package com.smartrecruit.offer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruit.aiengine.agents.RetentionPredictorAgent;
import com.smartrecruit.aiengine.domain.OnboardingCandidate;
import com.smartrecruit.aiengine.domain.RetentionRisk;
import com.smartrecruit.aiengine.dto.request.RetentionPredictRequest;
import com.smartrecruit.aiengine.dto.response.RetentionPredictVO;
import com.smartrecruit.common.constant.ConfigKeys;
import com.smartrecruit.common.constant.NotificationConstants;
import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.common.exception.BusinessException;
import com.smartrecruit.common.exception.ResourceNotFoundException;
import com.smartrecruit.common.exception.ValidationException;
import com.smartrecruit.offer.dto.request.CreateOnboardingRequest;
import com.smartrecruit.offer.dto.request.UpdateAccountRequest;
import com.smartrecruit.offer.dto.request.UpdateEquipmentRequest;
import com.smartrecruit.offer.dto.request.UpdateMentorRequest;
import com.smartrecruit.offer.dto.request.UpdateTrainingRequest;
import com.smartrecruit.offer.dto.request.UpdateSingleDocumentRequest;
import com.smartrecruit.offer.dto.remote.CreateUserRequest;
import com.smartrecruit.offer.dto.remote.CandidateProfileDTO;
import com.smartrecruit.offer.dto.remote.EmailRequest;
import com.smartrecruit.offer.dto.remote.NotificationRequest;
import com.smartrecruit.offer.dto.remote.ActivityRecordRequest;
import com.smartrecruit.offer.dto.remote.WorkbenchTaskRequest;
import com.smartrecruit.offer.dto.response.OnboardingDetailVO;
import com.smartrecruit.offer.dto.response.OnboardingDocumentVO;
import com.smartrecruit.offer.dto.response.OnboardingEquipmentVO;
import com.smartrecruit.offer.dto.response.OnboardingStatsVO;
import com.smartrecruit.offer.dto.response.OnboardingVO;
import com.smartrecruit.offer.dto.response.RetentionPredictionVO;
import com.smartrecruit.offer.dto.response.FileUploadVO;
import com.smartrecruit.offer.entity.*;
import com.smartrecruit.offer.enums.OfferEnums.*;
import com.smartrecruit.offer.repository.*;
import com.smartrecruit.offer.service.OnboardingService;
import com.smartrecruit.offer.service.RemoteConfigService;
import com.smartrecruit.offer.feign.AiEngineClient;
import com.smartrecruit.offer.feign.SystemFileClient;
import com.smartrecruit.common.util.DateUtils;
import com.smartrecruit.common.util.UserContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 入职管理服务实现。
 *
 * <p>管理六步骤的入职进度，集成 AI 引擎进行留任预测。</p>
 *
 * @since 1.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OnboardingServiceImpl implements OnboardingService {

    private final OnboardingMapper onboardingMapper;
    private final OnboardingDocumentMapper documentMapper;
    private final OnboardingEquipmentMapper equipmentMapper;
    private final RetentionPredictorAgent retentionPredictor;
    private final AiEngineClient aiEngineClient;
    private final OfferMapper offerMapper;
    private final com.smartrecruit.offer.feign.SystemClient systemClient;
    private final com.smartrecruit.offer.feign.RecruitmentClient recruitmentClient;
    private final RemoteConfigService remoteConfigService;
    private final SystemFileClient systemFileClient;

    /** 允许上传的文件扩展名。 */
    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("pdf", "jpg", "jpeg", "png", "gif", "doc", "docx", "xls", "xlsx");

    /** 上传文件大小上限（10MB）。 */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /** 正在异步生成的记录集合（防止并发重复触发 LLM）。 */
    private final java.util.Set<Long> retentionGenerating = ConcurrentHashMap.newKeySet();

    /** 留任预测异步执行器（虚拟线程，不阻塞页面加载）。 */
    private static final Executor RETENTION_EXECUTOR =
            Executors.newVirtualThreadPerTaskExecutor();

    private static final int TOTAL_STEPS = 6;
    private static final List<String> STEP_NAMES = List.of(
            "资料收集", "设备发放", "账号开通", "欢迎页", "导师分配", "入职培训"
    );

    /** 风险因素英文 → 中文 */
    private static final Map<String, String> RISK_FACTOR_CN = Map.of(
            "jobHoppingHistory", "跳槽历史",
            "compensationAlignment", "薪酬匹配度",
            "commuteDistance", "通勤距离",
            "careerGrowth", "职业发展空间",
            "culturalFit", "文化契合度",
            "unknown", "未知因素"
    );

    /** 干预建议英文 → 中文 */
    private static final Map<String, String> INTERVENTION_CN = Map.of(
            "Standard onboarding and check-in schedule", "标准入职流程和定期签到",
            "Assign mentor and schedule monthly 1:1 for first 6 months", "分配导师并在前6个月安排每月一对一沟通",
            "Implement retention plan: mentor + career path planning + quarterly review", "实施留任计划：导师制 + 职业路径规划 + 季度回顾",
            "Immediate intervention: executive check-in, personalized growth plan, compensation review", "立即干预：管理层沟通、个性化成长计划、薪酬评估",
            "Monitor regularly", "定期跟踪观察"
    );

    /** 上传入职文档附件：校验 → 上传对象存储 → 返回可访问 URL。 */
    @Override
    public FileUploadVO uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("文件不能为空");
        }

        // 校验扩展名
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new ValidationException(
                    "不支持的文件类型: " + ext + "，支持: " + String.join(", ", ALLOWED_EXTENSIONS));
        }

        // 限制 10MB
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ValidationException("文件大小不能超过 10MB");
        }

        try {
            // 通过系统服务上传到 RustFS
            String dateDir = DateUtils.formatSlashDate(DateUtils.today());
            String storedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            String relativePath = "onboarding/" + dateDir + "/" + storedName;

            String contentType = file.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            byte[] fileBytes = file.getBytes();

            // URL-encode 文件名以兼容 HTTP Header 的 ASCII 限制（中文文件名需转义）
            String safeFileName = URLEncoder.encode(
                    originalName != null ? originalName : "file", StandardCharsets.UTF_8);

            ApiResponse<String> uploadResult =
                    systemFileClient.upload(fileBytes, safeFileName, contentType, relativePath);

            if (uploadResult == null || !uploadResult.ok()) {
                log.error("System file upload failed: result={}", uploadResult);
                throw new ValidationException("文件上传失败");
            }

            String fileUrl = uploadResult.data();
            log.info("File uploaded via system service: {} -> {}", originalName, fileUrl);
            return new FileUploadVO(fileUrl, originalName, String.valueOf(file.getSize()));
        } catch (Exception e) {
            log.error("File upload failed", e);
            if (e instanceof ValidationException ve) {
                throw ve;
            }
            throw new ValidationException("文件上传失败: " + e.getMessage());
        }
    }

    /** 分页查询记录列表，支持多条件筛选。 */
    @Override
    public PageResult<OnboardingVO> pageQuery(Page<?> page, Map<String, Object> params) {
        Page<Onboarding> mpPage = new Page<>(page.getCurrent(), page.getSize());
        IPage<Onboarding> result = onboardingMapper.selectPageWithFilters(mpPage, params);
        List<OnboardingVO> vos = result.getRecords().stream()
                .map(this::toVO)
                .toList();
        return new PageResult<>(vos, result.getTotal(), result.getSize(), result.getCurrent(), result.getPages());
    }

    /** 根据主键查询详情。 */
    @Override
    public OnboardingDetailVO getById(Long id) {
        Onboarding entity = onboardingMapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("Onboarding", id);
        }
        return toDetailVO(entity);
    }

    /** 创建记录。 */
    @Override
    @Transactional
    public OnboardingVO create(CreateOnboardingRequest request) {
        Offer offer = offerMapper.selectById(request.getOfferId());
        if (offer == null) {
            throw new ResourceNotFoundException("Offer", request.getOfferId());
        }

        // 检查是否已存在入职记录（offer_id 有唯一约束）
        LambdaQueryWrapper<Onboarding> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(Onboarding::getOfferId, request.getOfferId());
        if (onboardingMapper.selectCount(existWrapper) > 0) {
            throw new BusinessException("ONBOARDING_EXISTS",
                    "该 Offer 已创建入职记录，不可重复创建");
        }

        Onboarding onboarding = new Onboarding();
        onboarding.setOfferId(request.getOfferId());
        onboarding.setCandidateId(request.getCandidateId());
        onboarding.setDepartmentId(offer.getJobPositionId()); // 通过 Offer 的职位关联部门
        onboarding.setDepartmentName(offer.getDepartmentName());
        onboarding.setEmployeeName(offer.getCandidateName());
        onboarding.setPositionTitle(offer.getPositionTitle());
        onboarding.setLevel(offer.getLevel());
        onboarding.setExpectedOnboardDate(
                request.getOnboardDate() != null ? request.getOnboardDate() : offer.getExpectedOnboardDate());
        onboarding.setCurrentStep(1);
        onboarding.setStatus(OnboardingStatus.PENDING.getCode());
        onboarding.setWelcomeSent(0);
        onboarding.setTrainingProgress(BigDecimal.ZERO);
        onboarding.setMentorId(request.getMentorId());
        onboarding.setBuddyId(request.getBuddyId());

        // 生成员工编号：EMP-YYYY-NNNN
        onboarding.setEmployeeNo(generateEmployeeNo());

        onboardingMapper.insert(onboarding);
        log.info("Onboarding created: id={}, employeeNo={}, candidateId={}",
                onboarding.getId(), onboarding.getEmployeeNo(), request.getCandidateId());

        // 批量创建 6 条默认文档记录
        createDefaultDocuments(onboarding.getId());
        // 批量创建 5 条默认设备记录
        createDefaultEquipments(onboarding.getId());

        return toVO(onboarding);
    }

    /** 根据主键删除记录。 */
    @Override
    @Transactional
    public void delete(Long id) {
        Onboarding entity = onboardingMapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("Onboarding", id);
        }
        int status = entity.getStatus() != null ? entity.getStatus() : 0;
        if (status != OnboardingStatus.PENDING.getCode() && status != OnboardingStatus.AT_RISK.getCode()) {
            throw new BusinessException("ONBOARDING_CANNOT_DELETE",
                    "仅待入职或有风险状态的记录可以删除，当前状态：" + status);
        }
        onboardingMapper.deleteById(id);
        log.info("Onboarding deleted: id={}", id);
    }

    /** 推进入职流程到下一步。 */
    @Override
    @Transactional
    public OnboardingVO advanceStep(Long id) {
        Onboarding entity = onboardingMapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("Onboarding", id);
        }

        if (OnboardingStatus.DONE.getCode() == entity.getStatus()) {
            throw new BusinessException("ONBOARDING_COMPLETED",
                    "Onboarding process is already completed.");
        }

        int currentStep = entity.getCurrentStep() != null ? entity.getCurrentStep() : 1;
        int nextStep = currentStep + 1;

        if (nextStep > TOTAL_STEPS) {
            // 仅当所有文档已上传时才自动完成
            if (areAllDocumentsCompleted(id)) {
                entity.setStatus(OnboardingStatus.DONE.getCode());
                entity.setTrainingProgress(new BigDecimal("100.00"));
                log.info("Onboarding auto-completed: id={}, all documents verified", id);
            } else {
                log.info("Onboarding last step reached but documents incomplete: id={}", id);
            }
        } else {
            if (OnboardingStatus.PENDING.getCode() == entity.getStatus()) {
                entity.setStatus(OnboardingStatus.ACTIVE.getCode());
            }
            // 步骤 4（欢迎页）自动标记为已发送
            if (nextStep == 4) {
                entity.setWelcomeSent(1);
            }
        }
        entity.setCurrentStep(Math.min(nextStep, TOTAL_STEPS));
        onboardingMapper.updateById(entity);

        boolean isDone = entity.getStatus() != null && entity.getStatus() == OnboardingStatus.DONE.getCode();
        log.info("Onboarding step advanced: id={}, currentStep={}, status={}",
                id, entity.getCurrentStep(), entity.getStatus());

        // 记录活动动态：完成时记录完成，中间步骤记录推进
        if (isDone) {
            recordOnboardingActivity(entity, "入职流程已完成",
                    "员工 " + entity.getEmployeeName() + " 的入职手续已全部完成。", true);
            notifyCreator(entity,
                    "入职流程已完成 - " + entity.getEmployeeName(),
                    "员工 " + entity.getEmployeeName() + " 的入职手续已全部完成，请做好后续跟进安排。",
                    NotificationConstants.BIZ_ONBOARDING_COMPLETED,
                    "/onboarding/" + entity.getId());
        } else {
            int step = entity.getCurrentStep() != null ? entity.getCurrentStep() : 1;
            recordOnboardingActivity(entity,
                    entity.getEmployeeName() + " 入职步骤已推进至第 " + step + " 步",
                    "员工 " + entity.getEmployeeName() + " 的入职手续已推进至第 " + step + "/" + TOTAL_STEPS + " 步。",
                    false);

            // 生成待办：根据当前步骤创建下一步任务
            String stepName = step <= STEP_NAMES.size() ? STEP_NAMES.get(step - 1) : "入职";
            createTask(entity,
                    stepName + " - " + entity.getEmployeeName(),
                    "员工 " + entity.getEmployeeName() + " 的入职进度至「" + stepName
                            + "」（" + step + "/" + TOTAL_STEPS + "），请继续推进。",
                    3, step >= TOTAL_STEPS - 1 ? 0 : 1, 3);
        }
        return toVO(entity);
    }

    /** 完成入职流程。 */
    @Override
    @Transactional
    public void completeOnboarding(Long id) {
        Onboarding entity = onboardingMapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("Onboarding", id);
        }
        if (OnboardingStatus.DONE.getCode() == entity.getStatus()) {
            throw new BusinessException("ONBOARDING_COMPLETED",
                    "Onboarding process is already completed.");
        }
        entity.setStatus(OnboardingStatus.DONE.getCode());
        entity.setTrainingProgress(new BigDecimal("100.00"));
        // 员工状态联动：入职完成时若仍为「待入职」，自动改为「试用期」，
        // 保证入职管理列表的「员工状态」列与「状态」列保持一致
        if (entity.getEmployeeStatus() == null || entity.getEmployeeStatus() == 0) {
            entity.setEmployeeStatus(1);
        }
        onboardingMapper.updateById(entity);
        log.info("Onboarding manually completed: id={}", id);

        notifyCreator(entity,
                "入职流程已完成 - " + entity.getEmployeeName(),
                "员工 " + entity.getEmployeeName() + " 的入职手续已全部完成，请做好后续跟进安排。",
                NotificationConstants.BIZ_ONBOARDING_COMPLETED,
                "/onboarding/" + entity.getId());

        // 记录活动动态
        recordOnboardingActivity(entity, "入职流程已完成",
                "员工 " + entity.getEmployeeName() + " 的入职手续已全部完成。", true);
        // 无待办任务：入职已完成
    }

    /** 批量更新入职资料提交状态。 */
    @Override
    @Transactional
    public void updateDocuments(Long id, Map<String, String> documentStatuses) {
        Onboarding entity = onboardingMapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("Onboarding", id);
        }

        // 通过 documentMapper 更新文档记录
        for (Map.Entry<String, String> entry : documentStatuses.entrySet()) {
            String docTypeStr = entry.getKey();
            String statusStr = entry.getValue();
            try {
                int docType = Integer.parseInt(docTypeStr);
                int status = Integer.parseInt(statusStr);
                LambdaQueryWrapper<OnboardingDocument> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(OnboardingDocument::getOnboardingId, id)
                        .eq(OnboardingDocument::getDocType, docType);
                OnboardingDocument doc = documentMapper.selectOne(wrapper);
                if (doc != null) {
                    doc.setStatus(status);
                    if (status == DocumentStatus.VERIFIED.getCode()) {
                        doc.setVerifiedTime(DateUtils.now());
                    }
                    doc.setUpdateUserId(currentUserId());
                    doc.setUpdateBy(currentUsername());
                    documentMapper.updateById(doc);
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid document update: docType={}, status={}", docTypeStr, statusStr);
            }
        }
        log.info("Onboarding documents updated: id={}", id);
    }

    /** 更新设备发放状态。 */
    @Override
    @Transactional
    public void updateEquipment(Long onboardingId, Long equipmentId, UpdateEquipmentRequest request) {
        OnboardingEquipment equip = equipmentMapper.selectById(equipmentId);
        if (equip == null || !equip.getOnboardingId().equals(onboardingId)) {
            throw new ResourceNotFoundException("Equipment", equipmentId);
        }
        if (request.getStatus() != null) {
            equip.setStatus(request.getStatus());
        }
        if (request.getAssetNo() != null) {
            equip.setAssetNo(request.getAssetNo());
        }
        if (request.getRemark() != null) {
            equip.setRemark(request.getRemark());
        }
        if (request.getStatus() != null
                && request.getStatus() == EquipmentStatus.DELIVERED.getCode()
                && equip.getDeliveredTime() == null) {
            equip.setDeliveredTime(DateUtils.now());
        }
        equip.setUpdateUserId(currentUserId());
        equip.setUpdateBy(currentUsername());
        equipmentMapper.updateById(equip);
        log.info("Equipment updated: onboardingId={}, equipmentId={}, status={}",
                onboardingId, equipmentId, request.getStatus());
    }

    /**
     * 查询留任预测（直接返回保存的数据）。
     *
     * <p>预测结果由定时任务提前计算并落库；查询时若保存结果对应的入职数据指纹
     * 未变化，直接返回保存的数据，不调用 AI；数据有变化或尚无预测时，
     * 异步触发生成（hash 校验后执行），页面先返回「评估中」。</p>
     */
    @Override
    public RetentionPredictionVO getRetentionPrediction(Long id) {
        Onboarding entity = onboardingMapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("Onboarding", id);
        }

        String hash = computeRetentionInputHash(entity);
        // 1. 已保存预测且入职数据未变化：直接返回保存的数据
        if (entity.getRetentionPredictedAt() != null
                && hash.equals(entity.getRetentionInputHash())) {
            return toSavedPredictionVO(entity);
        }

        // 2. 数据变化或无预测：异步触发（同一记录只触发一次），先返回「评估中」
        if (retentionGenerating.add(id)) {
            RETENTION_EXECUTOR.execute(() -> generateRetentionAsync(id));
            log.info("留任预测异步生成已触发: onboardingId={}", id);
        }
        return pendingVO(id);
    }

    /** 后台异步生成留任预测（hash 校验：入职数据未变化则跳过，不重复调用 AI）。 */
    private void generateRetentionAsync(Long onboardingId) {
        try {
            Onboarding entity = onboardingMapper.selectById(onboardingId);
            if (entity == null) {
                return;
            }
            String hash = computeRetentionInputHash(entity);
            if (entity.getRetentionPredictedAt() != null
                    && hash.equals(entity.getRetentionInputHash())) {
                log.info("留任预测跳过（入职数据未变化）: onboardingId={}", onboardingId);
                return;
            }
            RetentionPredictionVO vo = computeRetentionPrediction(entity);
            persistRetentionPrediction(entity, vo, hash);
            log.info("留任预测异步生成完成: onboardingId={}, score6M={}",
                    onboardingId, vo.getRetentionScore6M());
        } catch (Exception e) {
            log.warn("留任预测异步生成失败（可重试）: onboardingId={}, error={}",
                    onboardingId, e.getMessage());
        } finally {
            retentionGenerating.remove(onboardingId);
        }
    }

    /**
     * 批量刷新留任预测（定时任务调用）。
     *
     * <p>入职 5 步数据指纹（hash）未变化则跳过，不重复调用 AI；
     * 预测结果保存到数据库，查询时直接返回保存的数据。</p>
     */
    @Override
    public void refreshRetentionPredictions() {
        // 数据修复：历史被旧逻辑改为「有风险(AT_RISK)」的状态统一恢复为「入职中(ACTIVE)」，
        // 独立于 AI 预测执行，保证状态列只保留流程状态、不再出现「有风险」
        int repaired = repairLegacyAtRiskStatus();
        if (repaired > 0) {
            log.info("历史「有风险」状态已修复为「入职中」: count={}", repaired);
        }
        List<Onboarding> all = onboardingMapper.selectList(
                new LambdaQueryWrapper<Onboarding>()
                        .eq(Onboarding::getDeleted, 0));
        int submitted = 0;
        int skipped = 0;
        for (Onboarding entity : all) {
            String hash = computeRetentionInputHash(entity);
            if (entity.getRetentionPredictedAt() != null
                    && hash.equals(entity.getRetentionInputHash())) {
                skipped++;
                continue;
            }
            if (retentionGenerating.add(entity.getId())) {
                RETENTION_EXECUTOR.execute(() -> generateRetentionAsync(entity.getId()));
                submitted++;
            }
        }
        log.info("留任预测预预测完成: scanned={}, submitted={}, skipped={}",
                all.size(), submitted, skipped);
    }

    /** 将历史遗留的 AT_RISK(3) 状态批量恢复为 ACTIVE(1)（幂等）。 */
    private int repairLegacyAtRiskStatus() {
        return onboardingMapper.update(null,
                new LambdaUpdateWrapper<Onboarding>()
                        .eq(Onboarding::getDeleted, 0)
                        .eq(Onboarding::getStatus, OnboardingStatus.AT_RISK.getCode())
                        .set(Onboarding::getStatus, OnboardingStatus.ACTIVE.getCode()));
    }

    /**
     * 计算入职 5 步操作数据的 SHA-256 指纹。
     *
     * <p>资料收集（文档明细）、设备发放（设备明细）、账号开通、导师分配、入职培训
     * 拼接各自关键字段；欢迎页仅记录是否发生（welcomeSent）。</p>
     */
    private String computeRetentionInputHash(Onboarding entity) {
        StringBuilder sb = new StringBuilder();
        // 1. 资料收集
        List<OnboardingDocument> docs = documentMapper.selectList(
                new LambdaQueryWrapper<OnboardingDocument>()
                        .eq(OnboardingDocument::getOnboardingId, entity.getId())
                        .orderByAsc(OnboardingDocument::getId));
        for (OnboardingDocument doc : docs) {
            sb.append("DOC|").append(doc.getDocType()).append('|').append(doc.getDocName())
                    .append('|').append(doc.getStatus()).append('|').append(doc.getVerifiedTime())
                    .append('|').append(doc.getRemark()).append(';');
        }
        // 2. 设备发放
        List<OnboardingEquipment> equips = equipmentMapper.selectList(
                new LambdaQueryWrapper<OnboardingEquipment>()
                        .eq(OnboardingEquipment::getOnboardingId, entity.getId())
                        .orderByAsc(OnboardingEquipment::getId));
        for (OnboardingEquipment equip : equips) {
            sb.append("EQUIP|").append(equip.getEquipmentType()).append('|')
                    .append(equip.getEquipmentName()).append('|').append(equip.getStatus())
                    .append('|').append(equip.getAssetNo()).append('|').append(equip.getAssignedTime())
                    .append('|').append(equip.getDeliveredTime()).append('|').append(equip.getRemark())
                    .append(';');
        }
        // 3. 账号开通
        sb.append("ACCOUNT|").append(entity.getAccountUsername()).append('|')
                .append(entity.getAccountEmail()).append('|').append(entity.getAccountCreatedAt())
                .append(';');
        // 4. 欢迎页：仅记录是否发生
        sb.append("WELCOME|").append(entity.getWelcomeSent()).append(';');
        // 5. 导师分配
        sb.append("MENTOR|").append(entity.getMentorId()).append('|')
                .append(entity.getMentorName()).append('|').append(entity.getBuddyId()).append(';');
        // 6. 入职培训
        sb.append("TRAINING|").append(entity.getTrainingProgress()).append(';');
        // 版本前缀：画像字段扩展后整体失效重算，避免旧结果继续命中
        return sha256("retention-v3|" + sb);
    }

    /** SHA-256 十六进制摘要。 */
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    /** 实时计算留任预测（LLM 优先，本地启发式兜底），不落库。 */
    private RetentionPredictionVO computeRetentionPrediction(Onboarding entity) {
        // 构建 AI 引擎输入（补充真实薪酬/学历/年限/是否内推，让预测有区分度）
        OnboardingCandidate candidate = buildRetentionCandidate(entity);

        // 调用 AI 引擎（LLM 优先，本地启发式兜底）
        RetentionRisk risk = predictRisk(candidate);

        // 转换为 RetentionPredictionVO（风险分数反转为留任分数）
        int retentionScore6M = (int) Math.round((1.0 - risk.getRiskScore()) * 100);
        int retentionScore12M = Math.max(0, retentionScore6M - 10);
        int riskLevel = mapRiskLevel(risk.getRiskLevel());

        List<String> riskFactorNames = risk.getRiskFactors().entrySet().stream()
                .filter(e -> e.getValue() > 0.1)
                .map(e -> RISK_FACTOR_CN.getOrDefault(e.getKey(), e.getKey()))
                .collect(Collectors.toList());
        if (riskFactorNames.isEmpty() && risk.getPrimaryRiskFactor() != null) {
            riskFactorNames = List.of(
                    RISK_FACTOR_CN.getOrDefault(risk.getPrimaryRiskFactor(), risk.getPrimaryRiskFactor()));
        }

        List<String> interventions = List.of(
                INTERVENTION_CN.getOrDefault(risk.getRecommendation(), risk.getRecommendation()));

        return RetentionPredictionVO.builder()
                .onboardingId(entity.getId())
                .status("COMPLETED")
                .retentionScore6M(retentionScore6M)
                .retentionScore12M(retentionScore12M)
                .riskLevel(riskLevel)
                .riskFactors(riskFactorNames)
                .interventions(interventions)
                .build();
    }

    /**
     * 构建留任预测候选人画像：
     * 基础信息来自入职记录，薪酬来自关联 Offer，学历/年限/是否内推来自招聘服务。
     */
    private OnboardingCandidate buildRetentionCandidate(Onboarding entity) {
        OnboardingCandidate.OnboardingCandidateBuilder builder = OnboardingCandidate.builder()
                .employeeId(entity.getId())
                .name(entity.getEmployeeName())
                .department(entity.getDepartmentName() != null
                        ? entity.getDepartmentName() : entity.getPositionTitle())
                .position(entity.getPositionTitle())
                .onboardDate(entity.getExpectedOnboardDate() != null
                        ? entity.getExpectedOnboardDate() : entity.getActualOnboardDate())
                // 入职流程完成度：流程未完成时留任风险更高
                .onboardingCompletion(computeOnboardingCompletion(entity));

        // 薪酬：关联 Offer 的年度总包（元）
        if (entity.getOfferId() != null) {
            try {
                Offer offer = offerMapper.selectById(entity.getOfferId());
                if (offer != null && offer.getSalaryStructure() instanceof Map<?, ?> ss) {
                    Object totalPackage = ss.get("totalPackage");
                    if (totalPackage instanceof Number n) {
                        builder.salary(n.doubleValue());
                    }
                }
            } catch (Exception e) {
                log.warn("读取 Offer 薪酬失败（跳过）: onboardingId={}, error={}",
                        entity.getId(), e.getMessage());
            }
        }

        // 学历/年限/是否内推：招聘服务候选人详情
        if (entity.getCandidateId() != null) {
            try {
                ApiResponse<CandidateProfileDTO> resp =
                        recruitmentClient.getCandidate(entity.getCandidateId());
                if (resp != null && resp.data() != null) {
                    CandidateProfileDTO candidate = resp.data();
                    builder.educationLevel(educationLevelOf(candidate.getEducation()));
                    builder.yearsOfExperience(candidate.getYearsOfExperience());
                    builder.previousJobCount(candidate.getPreviousJobCount());
                    builder.avgTenureMonths(candidate.getAvgTenureMonths());
                    builder.isReferral(candidate.getSource() != null
                            && candidate.getSource() == 1);
                }
            } catch (Exception e) {
                log.warn("获取候选人画像失败（使用基础信息预测）: candidateId={}, error={}",
                        entity.getCandidateId(), e.getMessage());
            }
        }
        return builder.build();
    }

    /** 计算入职流程完成度（0-100）：步骤进度 + 培训进度，已入职记为 100。 */
    private int computeOnboardingCompletion(Onboarding entity) {
        if (entity.getStatus() != null && entity.getStatus() == OnboardingStatus.DONE.getCode()) {
            return 100;
        }
        int step = entity.getCurrentStep() != null ? Math.max(1, entity.getCurrentStep()) : 1;
        double training = entity.getTrainingProgress() != null
                ? entity.getTrainingProgress().doubleValue() / 100.0 : 0.0;
        return (int) Math.round(Math.min(100, ((step - 1) + training) / 6.0 * 100));
    }

    /** 学历编码 → 文案（LLM 可读）。 */
    private String educationLevelOf(Integer education) {
        if (education == null) {
            return null;
        }
        return switch (education) {
            case 0 -> "高中";
            case 1 -> "大专";
            case 2 -> "本科";
            case 3 -> "硕士";
            case 4 -> "博士";
            default -> null;
        };
    }

    /** 保存留任预测结果到实体并更新（含兼容字段与高风险预警）。 */
    private void persistRetentionPrediction(Onboarding entity, RetentionPredictionVO vo, String hash) {
        entity.setRetentionScore6M(vo.getRetentionScore6M());
        entity.setRetentionScore12M(vo.getRetentionScore12M());
        entity.setRetentionRiskFactors(joinList(vo.getRiskFactors()));
        entity.setRetentionInterventions(joinList(vo.getInterventions()));
        entity.setRetentionPredictedAt(DateUtils.now());
        entity.setRetentionInputHash(hash);
        // 兼容字段：风险评分/风险等级（列表展示用）
        if (vo.getRetentionScore6M() != null) {
            double riskScore = Math.max(0.0, Math.min(100.0, 100.0 - vo.getRetentionScore6M()));
            entity.setRiskScore(BigDecimal.valueOf(riskScore).setScale(2, RoundingMode.HALF_UP));
            entity.setRiskLevel(vo.getRiskLevel());
        }
        // 状态字段只表示入职流程进度；风险情况统一由风险等级（riskLevel）字段展示，
        // 不再把状态改为「有风险」。历史被旧逻辑改为 AT_RISK 的记录恢复为「入职中」。
        if (entity.getStatus() != null
                && entity.getStatus() == OnboardingStatus.AT_RISK.getCode()) {
            entity.setStatus(OnboardingStatus.ACTIVE.getCode());
        }
        if (vo.getRiskLevel() != null && vo.getRiskLevel() == RiskLevel.HIGH.getCode()) {
            notifyCreator(entity,
                    "入职风险预警 - " + entity.getEmployeeName(),
                    "员工 " + entity.getEmployeeName() + "（" + entity.getPositionTitle()
                            + "）被评估为高留任风险，请及时介入留任计划。",
                    NotificationConstants.BIZ_ONBOARDING_RISK,
                    "/onboarding/" + entity.getId());
        }
        onboardingMapper.updateById(entity);
    }

    /** 从已保存的实体构建预测 VO（COMPLETED）。 */
    private RetentionPredictionVO toSavedPredictionVO(Onboarding entity) {
        return RetentionPredictionVO.builder()
                .onboardingId(entity.getId())
                .status("COMPLETED")
                .retentionScore6M(entity.getRetentionScore6M())
                .retentionScore12M(entity.getRetentionScore12M())
                .riskLevel(entity.getRiskLevel())
                .riskFactors(splitList(entity.getRetentionRiskFactors()))
                .interventions(splitList(entity.getRetentionInterventions()))
                .build();
    }

    /** 「评估中」占位 VO。 */
    private RetentionPredictionVO pendingVO(Long id) {
        return RetentionPredictionVO.builder()
                .onboardingId(id)
                .status("PENDING")
                .retentionScore6M(null)
                .retentionScore12M(null)
                .riskLevel(null)
                .riskFactors(List.of())
                .interventions(List.of())
                .build();
    }

    /** 列表 → ";;" 分隔字符串。 */
    private String joinList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return String.join(";;", list);
    }

    /** ";;" 分隔字符串 → 列表。 */
    private List<String> splitList(String joined) {
        if (joined == null || joined.isBlank()) {
            return List.of();
        }
        return Arrays.stream(joined.split(";;"))
                .filter(s -> !s.isBlank())
                .toList();
    }

    /**
     * 调用 AI 引擎预测留任风险（LLM 优先，本地 Agent 启发式兜底）。
     *
     * <p>AI 引擎返回 6/12 个月留任概率后转换为 {@link RetentionRisk}，
     * 与本地 Agent 的输出结构对齐，复用统一的转换与预警逻辑。</p>
     */
    private RetentionRisk predictRisk(OnboardingCandidate candidate) {
        try {
            ApiResponse<RetentionPredictVO> resp = aiEngineClient.predictRetention(
                    new RetentionPredictRequest(candidate));
            if (resp != null && resp.data() != null) {
                return toRisk(candidate, resp.data());
            }
        } catch (Exception e) {
            log.warn("AI 留任预测调用失败，降级本地启发式: employeeId={}, error={}",
                    candidate.getEmployeeId(), e.getMessage());
        }
        return retentionPredictor.predict(candidate);
    }

    /** AI 引擎留任预测结果 → 本地风险对象（结构对齐）。 */
    private RetentionRisk toRisk(OnboardingCandidate candidate, RetentionPredictVO vo) {
        double score6M = vo.retentionScore6M() == null
                ? 70.0 : Math.max(0.0, Math.min(100.0, vo.retentionScore6M()));
        double riskScore = Math.round((1.0 - score6M / 100.0) * 100.0) / 100.0;
        String riskLevel = vo.riskLevel() == null || vo.riskLevel().isBlank()
                ? "MEDIUM" : vo.riskLevel().trim().toUpperCase();
        Map<String, Double> riskFactors = vo.riskFactors() == null
                ? Map.of() : new LinkedHashMap<>(vo.riskFactors());
        String primaryFactor = riskFactors.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        String recommendation = vo.recommendation() == null || vo.recommendation().isBlank()
                ? "定期跟踪观察" : vo.recommendation();
        log.info("留任预测完成(LLM): employeeId={}, score6M={}, riskLevel={}",
                candidate.getEmployeeId(), score6M, riskLevel);
        return RetentionRisk.builder()
                .employeeId(candidate.getEmployeeId())
                .riskScore(riskScore)
                .riskLevel(riskLevel)
                .riskFactors(riskFactors)
                .primaryRiskFactor(primaryFactor)
                .recommendation(recommendation)
                .build();
    }

    /** 查询统计信息。 */
    @Override
    public OnboardingStatsVO getStats() {
        List<Map<String, Object>> statusCounts = onboardingMapper.countByStatus();
        List<Map<String, Object>> riskCounts = onboardingMapper.countByRiskLevel();

        long pending = 0, active = 0, done = 0, atRisk = 0;
        for (Map<String, Object> row : statusCounts) {
            int code = toInt(row.get("status"));
            long cnt = toLong(row.get("cnt"));
            switch (code) {
                case 0 -> pending = cnt;
                case 1 -> active = cnt;
                case 2 -> done = cnt;
                case 3 -> atRisk = cnt;
            }
        }

        long low = 0, medium = 0, high = 0;
        for (Map<String, Object> row : riskCounts) {
            int code = toInt(row.get("risk_level"));
            long cnt = toLong(row.get("cnt"));
            switch (code) {
                case 0 -> low = cnt;
                case 1 -> medium = cnt;
                case 2 -> high = cnt;
            }
        }

        return OnboardingStatsVO.builder()
                .totalCount(pending + active + done + atRisk)
                .pendingCount(pending)
                .activeCount(active)
                .doneCount(done)
                .atRiskCount(atRisk)
                .lowRiskCount(low)
                .mediumRiskCount(medium)
                .highRiskCount(high)
                .build();
    }

    // ================================================================
    //  Step Management - 步骤管理
    // ================================================================

    /** 指定/更新导师与伙伴。 */
    @Override
    @Transactional
    public void updateMentor(Long id, UpdateMentorRequest request) {
        Onboarding entity = onboardingMapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("Onboarding", id);
        }
        entity.setMentorId(request.getMentorId());
        entity.setBuddyId(request.getBuddyId());
        // 如果传了 mentorId 但没有 name，可以后续通过 sys_user 查询补充
        onboardingMapper.updateById(entity);
        log.info("Mentor updated: onboardingId={}, mentorId={}, buddyId={}",
                id, request.getMentorId(), request.getBuddyId());
    }

    /** 更新培训进度。 */
    @Override
    @Transactional
    public void updateTraining(Long id, UpdateTrainingRequest request) {
        Onboarding entity = onboardingMapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("Onboarding", id);
        }
        entity.setTrainingProgress(request.getTrainingProgress());
        onboardingMapper.updateById(entity);
        log.info("Training progress updated: onboardingId={}, progress={}",
                id, request.getTrainingProgress());
    }

    /** 发送欢迎页。 */
    @Override
    @Transactional
    public void sendWelcome(Long id) {
        Onboarding entity = onboardingMapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("Onboarding", id);
        }

        sendWelcomeEmail(entity);

        entity.setWelcomeSent(1);
        onboardingMapper.updateById(entity);
        log.info("Welcome sent: onboardingId={}", id);
    }

    /** 开通/更新系统账号。 */
    @Override
    @Transactional
    public void updateAccount(Long id, UpdateAccountRequest request) {
        Onboarding entity = onboardingMapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("Onboarding", id);
        }

        // 在系统用户表中创建真实用户
        CreateUserRequest createParams = new CreateUserRequest();
        createParams.setUsername(request.getUsername());
        createParams.setRealName(entity.getEmployeeName());
        createParams.setEmail(request.getEmail());
        // 未指定初始密码时，使用系统配置的入职默认密码（修改后立即生效）
        String initialPassword = request.getInitialPassword();
        if (initialPassword == null || initialPassword.isBlank()) {
            initialPassword = remoteConfigService.getString(
                    ConfigKeys.ONBOARDING_DEFAULT_PASSWORD, "123456");
        }
        createParams.setPassword(initialPassword);
        createParams.setDeptId(entity.getDepartmentId() != null ? entity.getDepartmentId() : 1L);
        createParams.setRoleId(com.smartrecruit.common.constant.Constants.DEFAULT_ROLE_EMPLOYEE_ID);
        try {
            systemClient.createUser(createParams);
            log.info("System user created: username={}, email={}", request.getUsername(), request.getEmail());
        } catch (Exception e) {
            log.error("Failed to create system user for onboarding {}: {}", id, e.getMessage());
            throw new BusinessException("ACCOUNT_CREATE_FAILED", "创建系统用户失败：" + e.getMessage());
        }

        entity.setAccountUsername(request.getUsername());
        entity.setAccountEmail(request.getEmail());
        entity.setAccountCreatedAt(DateUtils.now());
        onboardingMapper.updateById(entity);
        log.info("Account created: onboardingId={}, username={}", id, request.getUsername());
    }

    /**
     * 发送入职欢迎邮件（基于系统配置的欢迎模板，best-effort 不阻断流程）。
     */
    private void sendWelcomeEmail(Onboarding entity) {
        if (entity.getAccountEmail() == null || entity.getAccountEmail().isBlank()) {
            log.warn("入职账号邮箱为空，跳过欢迎邮件: onboardingId={}", entity.getId());
            return;
        }
        try {
            String template = remoteConfigService.getString(
                    ConfigKeys.ONBOARDING_WELCOME_TEMPLATE,
                    "亲爱的 {{employeeName}}：\n\n欢迎加入 {{departmentName}} 部门！"
                            + "\n您的职位：{{jobTitle}}（{{level}}）\n入职日期：{{onboardDate}}"
                            + "\n\n我们为您准备了完善的入职培训计划，您的导师和伙伴将协助您快速融入团队。"
                            + "\n\n期待与您共同成长！\n\n—— 人力资源部");
            String systemName = remoteConfigService.getString(ConfigKeys.SYSTEM_NAME, "SmartRecruit");
            String subject = "【" + systemName + "】入职欢迎 — " + entity.getEmployeeName();

            EmailRequest request = new EmailRequest(
                    entity.getAccountEmail(), subject, renderWelcomeTemplate(template, entity), false);

            ApiResponse<Void> response = systemClient.sendEmail(request);
            if (response.ok()) {
                log.info("入职欢迎邮件已发送: onboardingId={}, to={}",
                        entity.getId(), entity.getAccountEmail());
            } else {
                log.warn("入职欢迎邮件发送失败: onboardingId={}, code={}, message={}",
                        entity.getId(), response.code(), response.message());
            }
        } catch (Exception e) {
            log.warn("入职欢迎邮件发送异常（不影响流程）: onboardingId={}, error={}",
                    entity.getId(), e.getMessage());
        }
    }

    /**
     * 渲染欢迎模板，替换 {{变量}} 占位符。
     */
    private String renderWelcomeTemplate(String template, Onboarding entity) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("employeeName", nullToEmpty(entity.getEmployeeName()));
        variables.put("departmentName", nullToEmpty(entity.getDepartmentName()));
        variables.put("jobTitle", nullToEmpty(entity.getPositionTitle()));
        variables.put("level", nullToEmpty(entity.getLevel()));
        variables.put("onboardDate", entity.getExpectedOnboardDate() != null
                ? DateUtils.formatDate(entity.getExpectedOnboardDate()) : "");

        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * 站内通知入职记录创建人（best-effort，失败不影响主流程）。
     */
    private void notifyCreator(Onboarding entity, String title, String content,
                               String businessType, String actionUrl) {
        Long creatorId = parseCreatorId(entity.getCreateBy());
        if (creatorId == null) {
            log.debug("入职记录创建人为空，跳过通知: onboardingId={}", entity.getId());
            return;
        }
        try {
            NotificationRequest request = new NotificationRequest();
            request.setUserId(creatorId);
            request.setTitle(title);
            request.setContent(content);
            request.setType(NotificationConstants.TYPE_ONBOARDING);
            request.setBusinessType(businessType);
            request.setBusinessId(entity.getId());
            request.setActionUrl(actionUrl);
            systemClient.sendNotification(request);
        } catch (Exception e) {
            log.warn("入职通知发送失败（可忽略）: onboardingId={}, error={}",
                    entity.getId(), e.getMessage());
        }
    }

    private Long parseCreatorId(String createBy) {
        if (createBy == null || createBy.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(createBy.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 更新单个入职文档状态。 */
    @Override
    @Transactional
    public void updateSingleDocument(Long onboardingId, Long documentId,
                                     UpdateSingleDocumentRequest request) {
        Onboarding entity = onboardingMapper.selectById(onboardingId);
        if (entity == null) {
            throw new ResourceNotFoundException("Onboarding", onboardingId);
        }
        OnboardingDocument doc = documentMapper.selectById(documentId);
        if (doc == null || !doc.getOnboardingId().equals(onboardingId)) {
            throw new ResourceNotFoundException("Document", documentId);
        }
        if (request.getFilePath() != null) {
            doc.setFilePath(request.getFilePath());
            // 上传文件后状态变为已上传，无需审核
            if (doc.getStatus() == null || doc.getStatus() == DocumentStatus.MISSING.getCode()) {
                doc.setStatus(DocumentStatus.PENDING.getCode());
            }
        }
        if (request.getStatus() != null) {
            int newStatus = request.getStatus();
            doc.setStatus(newStatus);
            if (newStatus == DocumentStatus.VERIFIED.getCode()) {
                doc.setVerifiedTime(DateUtils.now());
            }
        }
        if (request.getRemark() != null) {
            doc.setRemark(request.getRemark());
        }
        doc.setUpdateUserId(currentUserId());
        doc.setUpdateBy(currentUsername());
        documentMapper.updateById(doc);
        log.info("Document updated: onboardingId={}, documentId={}, fields={}",
                onboardingId, documentId, request);
    }

    /** 清除入职文档关联的文件。 */
    @Override
    @Transactional
    public void clearDocumentFile(Long onboardingId, Long documentId) {
        Onboarding entity = onboardingMapper.selectById(onboardingId);
        if (entity == null) {
            throw new ResourceNotFoundException("Onboarding", onboardingId);
        }
        OnboardingDocument doc = documentMapper.selectById(documentId);
        if (doc == null || !doc.getOnboardingId().equals(onboardingId)) {
            throw new ResourceNotFoundException("Document", documentId);
        }
        doc.setFilePath(null);
        doc.setStatus(DocumentStatus.MISSING.getCode());
        doc.setVerifiedTime(null);
        doc.setVerifiedBy(null);
        doc.setUpdateUserId(currentUserId());
        doc.setUpdateBy(currentUsername());
        documentMapper.updateById(doc);
        log.info("Document file cleared: onboardingId={}, documentId={}", onboardingId, documentId);
    }

    /**
     * 检查某入职记录的所有文档是否都已上传（status >= 1）。
     */
    private boolean areAllDocumentsCompleted(Long onboardingId) {
        List<OnboardingDocument> docs = documentMapper.selectList(
                new LambdaQueryWrapper<OnboardingDocument>()
                        .eq(OnboardingDocument::getOnboardingId, onboardingId));
        if (docs.isEmpty()) return false;
        return docs.stream().allMatch(d -> d.getStatus() != null && d.getStatus() >= 1);
    }

    /** 更新员工状态。 */
    @Override
    public void updateEmployeeStatus(Long id, Integer status) {
        Onboarding entity = onboardingMapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("Onboarding", id);
        }
        if (status == null || status < 0 || status > 3) {
            throw new ValidationException("无效的员工状态: " + status);
        }
        entity.setEmployeeStatus(status);
        entity.setUpdateTime(DateUtils.now());
        onboardingMapper.updateById(entity);
        log.info("员工状态已更新: onboardingId={}, employeeStatus={}", id, status);
    }

    /** 员工状态中文名。 */
    private String employeeStatusLabel(Integer status) {
        if (status == null) return "待入职";
        return switch (status) {
            case 1 -> "试用期";
            case 2 -> "正式";
            case 3 -> "已离职";
            default -> "待入职";
        };
    }

    // ---- 私有辅助方法 ----

    private OnboardingVO toVO(Onboarding entity) {
        List<OnboardingDocument> docs = documentMapper.selectList(
                new LambdaQueryWrapper<OnboardingDocument>()
                        .eq(OnboardingDocument::getOnboardingId, entity.getId()));
        int completedDocs = 0;
        for (OnboardingDocument doc : docs) {
            if (doc.getStatus() != null && doc.getStatus() >= 1) {
                completedDocs++;
            }
        }
        return toVO(entity, completedDocs, DocumentType.values().length);
    }

    private OnboardingVO toVO(Onboarding entity, int completedDocumentsCount, int totalDocumentsCount) {
        int currentStep = entity.getCurrentStep() != null ? entity.getCurrentStep() : 1;
        return OnboardingVO.builder()
                .id(entity.getId())
                .offerId(entity.getOfferId())
                .candidateId(entity.getCandidateId())
                .employeeName(entity.getEmployeeName())
                .employeeNo(entity.getEmployeeNo())
                .jobTitle(entity.getPositionTitle())
                .level(entity.getLevel())
                .departmentName(entity.getDepartmentName())
                .onboardDate(entity.getExpectedOnboardDate() != null
                        ? entity.getExpectedOnboardDate() : entity.getActualOnboardDate())
                .currentStep(currentStep)
                .totalSteps(TOTAL_STEPS)
                .completedDocumentsCount(completedDocumentsCount)
                .totalDocumentsCount(totalDocumentsCount)
                .status(entity.getStatus())
                .employeeStatus(entity.getEmployeeStatus())
                .employeeStatusLabel(employeeStatusLabel(entity.getEmployeeStatus()))
                .riskLevel(entity.getRiskLevel())
                .mentorId(entity.getMentorId())
                .createTime(entity.getCreateTime())
                .build();
    }

    private OnboardingDetailVO toDetailVO(Onboarding entity) {
        int currentStep = entity.getCurrentStep() != null ? entity.getCurrentStep() : 1;

        // 查询文档列表
        LambdaQueryWrapper<OnboardingDocument> docWrapper = new LambdaQueryWrapper<>();
        docWrapper.eq(OnboardingDocument::getOnboardingId, entity.getId());
        List<OnboardingDocument> docs = documentMapper.selectList(docWrapper);

        Map<String, String> docStatuses = new LinkedHashMap<>();
        List<OnboardingDocumentVO> docVOs = new ArrayList<>();
        for (OnboardingDocument doc : docs) {
            DocumentType docType = DocumentType.fromCode(doc.getDocType());
            DocumentStatus docStatus = DocumentStatus.fromCode(doc.getStatus());
            String name = docType != null ? docType.getLabel() : doc.getDocName();
            String statusLabel = docStatus != null ? docStatus.getLabel() : String.valueOf(doc.getStatus());
            docStatuses.put(name, statusLabel);
            docVOs.add(OnboardingDocumentVO.builder()
                    .id(doc.getId())
                    .docType(doc.getDocType())
                    .docName(name)
                    .status(doc.getStatus())
                    .statusLabel(statusLabel)
                    .filePath(doc.getFilePath())
                    .verifiedBy(doc.getVerifiedBy())
                    .verifiedTime(doc.getVerifiedTime())
                    .remark(doc.getRemark())
                    .build());
        }

        // 查询设备列表
        LambdaQueryWrapper<OnboardingEquipment> equipWrapper = new LambdaQueryWrapper<>();
        equipWrapper.eq(OnboardingEquipment::getOnboardingId, entity.getId());
        List<OnboardingEquipment> equipments = equipmentMapper.selectList(equipWrapper);
        int equipmentAggStatus = 0;
        List<OnboardingEquipmentVO> equipVOs = new ArrayList<>();
        if (!equipments.isEmpty()) {
            int delivered = 0;
            for (OnboardingEquipment e : equipments) {
                if (e.getStatus() != null && e.getStatus() >= EquipmentStatus.DELIVERED.getCode()) {
                    delivered++;
                }
                EquipmentType equiType = EquipmentType.fromCode(e.getEquipmentType());
                EquipmentStatus equiStatus = EquipmentStatus.fromCode(e.getStatus());
                equipVOs.add(OnboardingEquipmentVO.builder()
                        .id(e.getId())
                        .equipmentType(e.getEquipmentType())
                        .equipmentName(equiType != null ? equiType.getLabel() : e.getEquipmentName())
                        .status(e.getStatus())
                        .statusLabel(equiStatus != null ? equiStatus.getLabel() : String.valueOf(e.getStatus()))
                        .assetNo(e.getAssetNo())
                        .assignedBy(e.getAssignedBy())
                        .assignedTime(e.getAssignedTime())
                        .deliveredTime(e.getDeliveredTime())
                        .remark(e.getRemark())
                        .build());
            }
            if (delivered == equipments.size()) {
                equipmentAggStatus = 3;
            } else if (delivered > 0) {
                equipmentAggStatus = 1;
            }
        }

        return OnboardingDetailVO.builder()
                .id(entity.getId())
                .offerId(entity.getOfferId())
                .candidateId(entity.getCandidateId())
                .employeeName(entity.getEmployeeName())
                .employeeNo(entity.getEmployeeNo())
                .jobTitle(entity.getPositionTitle())
                .level(entity.getLevel())
                .departmentName(entity.getDepartmentName())
                .onboardDate(entity.getExpectedOnboardDate() != null
                        ? entity.getExpectedOnboardDate() : entity.getActualOnboardDate())
                .currentStep(currentStep)
                .totalSteps(TOTAL_STEPS)
                .status(entity.getStatus())
                .employeeStatus(entity.getEmployeeStatus())
                .employeeStatusLabel(employeeStatusLabel(entity.getEmployeeStatus()))
                .riskLevel(entity.getRiskLevel())
                .retentionScore(entity.getRiskScore() != null ? entity.getRiskScore().intValue() : null)
                .documentStatuses(docStatuses)
                .documents(docVOs)
                .equipments(equipVOs)
                .equipmentStatus(equipmentAggStatus)
                .mentorId(entity.getMentorId())
                .mentorName(entity.getMentorName())
                .buddyId(entity.getBuddyId())
                .welcomeSent(entity.getWelcomeSent())
                .trainingProgress(entity.getTrainingProgress())
                .accountCreated(entity.getAccountUsername() != null)
                .accountUsername(entity.getAccountUsername())
                .accountEmail(entity.getAccountEmail())
                .checklist(entity.getDocumentStatus())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    private void createDefaultDocuments(Long onboardingId) {
        for (DocumentType docType : DocumentType.values()) {
            OnboardingDocument doc = new OnboardingDocument();
            doc.setOnboardingId(onboardingId);
            doc.setDocType(docType.getCode());
            doc.setDocName(docType.getLabel());
            doc.setStatus(DocumentStatus.MISSING.getCode());
            doc.setCreateUserId(currentUserId());
            doc.setCreateBy(currentUsername());
            doc.setUpdateUserId(currentUserId());
            doc.setUpdateBy(currentUsername());
            documentMapper.insert(doc);
        }
        log.info("Created {} default documents for onboarding: id={}",
                DocumentType.values().length, onboardingId);
    }

    private void createDefaultEquipments(Long onboardingId) {
        for (EquipmentType equipType : EquipmentType.values()) {
            OnboardingEquipment equip = new OnboardingEquipment();
            equip.setOnboardingId(onboardingId);
            equip.setEquipmentType(equipType.getCode());
            equip.setEquipmentName(equipType.getLabel());
            equip.setStatus(EquipmentStatus.PENDING.getCode());
            equip.setCreateUserId(currentUserId());
            equip.setCreateBy(currentUsername());
            equip.setUpdateUserId(currentUserId());
            equip.setUpdateBy(currentUsername());
            equipmentMapper.insert(equip);
        }
        log.info("Created {} default equipment records for onboarding: id={}",
                EquipmentType.values().length, onboardingId);
    }

    private String generateEmployeeNo() {
        String yearMonth = DateUtils.formatCompactMonth(DateUtils.today());
        LambdaQueryWrapper<Onboarding> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(Onboarding::getEmployeeNo, "EMP-" + yearMonth + "-")
                .orderByDesc(Onboarding::getEmployeeNo)
                .last("LIMIT 1");
        Onboarding latest = onboardingMapper.selectOne(wrapper);
        int seq = 1;
        if (latest != null && latest.getEmployeeNo() != null) {
            String[] parts = latest.getEmployeeNo().split("-");
            if (parts.length == 3) {
                try {
                    seq = Integer.parseInt(parts[2]) + 1;
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return String.format("EMP-%s-%04d", yearMonth, seq);
    }

    private int mapRiskLevel(String riskLevel) {
        return switch (riskLevel) {
            case "LOW" -> RiskLevel.LOW.getCode();
            case "MEDIUM" -> RiskLevel.MEDIUM.getCode();
            case "HIGH", "CRITICAL" -> RiskLevel.HIGH.getCode();
            default -> RiskLevel.MEDIUM.getCode();
        };
    }

    private long toLong(Object obj) {
        if (obj instanceof Number n) return n.longValue();
        if (obj instanceof String s) return Long.parseLong(s);
        return 0;
    }

    private int toInt(Object obj) {
        if (obj instanceof Number n) return n.intValue();
        if (obj instanceof String s) return Integer.parseInt(s);
        return 0;
    }

    /**
     * 记录入职相关的活动动态到招聘服务。
     */
    private void recordOnboardingActivity(Onboarding entity, String title, String description, boolean isComplete) {
        try {
            recruitmentClient.recordActivity(new ActivityRecordRequest(
                    4, title, description, 0L, "system", "ONBOARDING", entity.getId()));
            log.info("Onboarding activity recorded: onboardingId={}, complete={}", entity.getId(), isComplete);
        } catch (Exception e) {
            log.warn("Failed to record onboarding activity feed: {}", e.getMessage());
        }
    }

    /**
     * 创建待办任务到招聘服务（通过 Feign）。
     */
    private void createTask(Onboarding entity, String title, String description,
                            int taskType, int priority, long daysUntilDue) {
        try {
            WorkbenchTaskRequest task = new WorkbenchTaskRequest();
            task.setUserId(0L);
            task.setTitle(title);
            task.setDescription(description);
            task.setType(taskType);
            task.setPriority(priority);
            task.setRelatedType("ONBOARDING");
            task.setRelatedId(entity.getId());
            task.setCandidateName(entity.getEmployeeName());
            if (daysUntilDue > 0) {
                task.setDueDate(DateUtils.now().plusDays(daysUntilDue));
            }
            recruitmentClient.createTask(task);
            log.info("Task created: onboardingId={}, title={}", entity.getId(), title);
        } catch (Exception e) {
            log.warn("Failed to create workbench task for onboarding: {}", e.getMessage());
        }
    }

    /** 当前登录用户 ID（内部调用或未登录时为 null）。 */
    private Long currentUserId() {
        return UserContextUtil.getCurrentUserId();
    }

    /** 当前登录用户名（内部调用时为 system）。 */
    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof String p
                && !p.isBlank() && !"anonymousUser".equals(p)) {
            return p;
        }
        return null;
    }
}
