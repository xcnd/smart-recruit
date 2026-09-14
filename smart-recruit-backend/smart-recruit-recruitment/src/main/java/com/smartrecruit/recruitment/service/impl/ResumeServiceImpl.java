package com.smartrecruit.recruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruit.common.constant.NotificationConstants;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.common.exception.ValidationException;
import com.smartrecruit.recruitment.enums.RecruitmentEnums;
import com.smartrecruit.common.exception.ResourceNotFoundException;
import com.smartrecruit.recruitment.converter.ResumeConverter;
import com.smartrecruit.recruitment.dto.request.BatchScreenRequest;
import com.smartrecruit.recruitment.dto.request.ResumePageQuery;
import com.smartrecruit.recruitment.dto.response.AiScreeningResultVO;
import com.smartrecruit.recruitment.dto.response.BatchScreenResultVO;
import com.smartrecruit.recruitment.dto.response.ResumeDetailVO;
import com.smartrecruit.recruitment.dto.response.ResumeParseStatusVO;
import com.smartrecruit.recruitment.dto.response.ResumePreviewVO;
import com.smartrecruit.recruitment.dto.response.ResumeStatsVO;
import com.smartrecruit.recruitment.dto.response.ResumeVO;
import com.smartrecruit.recruitment.repository.CandidateMapper;
import com.smartrecruit.recruitment.repository.JobPositionMapper;
import com.smartrecruit.recruitment.repository.ResumeMapper;
import com.smartrecruit.recruitment.dto.request.NotificationRequest;
import com.smartrecruit.recruitment.entity.Candidate;
import com.smartrecruit.recruitment.entity.JobPosition;
import com.smartrecruit.recruitment.entity.Resume;
import com.smartrecruit.recruitment.service.AiScreeningService;
import com.smartrecruit.recruitment.service.ActivityFeedService;
import com.smartrecruit.recruitment.service.ApplicationService;
import com.smartrecruit.recruitment.service.FileStorageService;
import com.smartrecruit.recruitment.service.ResumeParseService;
import com.smartrecruit.recruitment.service.ResumeService;
import com.smartrecruit.recruitment.service.WordPreviewService;
import com.smartrecruit.recruitment.feign.SystemUserClient;
import com.smartrecruit.recruitment.feign.SystemNotificationClient;
import com.smartrecruit.recruitment.service.WorkbenchTaskService;
import com.smartrecruit.recruitment.util.UserContextUtil;
import com.smartrecruit.common.util.DateUtils;
import com.smartrecruit.common.util.HttpUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * {@link ResumeService} 的实现类，负责简历上传、列表查询和AI筛选。
 *
 * @since 1.0.0
 */
@Service
@Slf4j
public class ResumeServiceImpl implements ResumeService {

    private final ResumeMapper resumeMapper;
    private final CandidateMapper candidateMapper;
    private final JobPositionMapper jobPositionMapper;
    private final ResumeConverter resumeConverter;
    private final ResumeParseService resumeParseService;
    private final FileStorageService fileStorageService;
    private final AiScreeningService aiScreeningService;
    private final ApplicationService applicationService;
    private final ActivityFeedService activityFeedService;
    private final WorkbenchTaskService workbenchTaskService;
    private final SystemUserClient systemUserClient;
    private final SystemNotificationClient systemNotificationClient;
    private final WordPreviewService wordPreviewService;

    public ResumeServiceImpl(ResumeMapper resumeMapper, CandidateMapper candidateMapper,
                             JobPositionMapper jobPositionMapper, ResumeConverter resumeConverter,
                             ResumeParseService resumeParseService,
                             AiScreeningService aiScreeningService,
                             ApplicationService applicationService,
                             ActivityFeedService activityFeedService,
                             WorkbenchTaskService workbenchTaskService,
                             SystemUserClient systemUserClient,
                             SystemNotificationClient systemNotificationClient,
                             WordPreviewService wordPreviewService,
                             @org.springframework.beans.factory.annotation.Autowired(required = false)
                             FileStorageService fileStorageService) {
        this.resumeMapper = resumeMapper;
        this.candidateMapper = candidateMapper;
        this.jobPositionMapper = jobPositionMapper;
        this.resumeConverter = resumeConverter;
        this.resumeParseService = resumeParseService;
        this.aiScreeningService = aiScreeningService;
        this.applicationService = applicationService;
        this.activityFeedService = activityFeedService;
        this.workbenchTaskService = workbenchTaskService;
        this.systemUserClient = systemUserClient;
        this.systemNotificationClient = systemNotificationClient;
        this.wordPreviewService = wordPreviewService;
        this.fileStorageService = fileStorageService;
    }

    /** 简历在线预览：下载原文件 → .docx 原样返回 / .doc 转 HTML。 */
    @Override
    public ResumePreviewVO preview(Long id) {
        Resume resume = resumeMapper.selectById(id);
        if (resume == null) {
            return ResumePreviewVO.notFound();
        }

        String fileUrl = resume.getFileUrl();
        String fileName = resume.getFileName();
        if (fileUrl == null || fileUrl.isEmpty()) {
            return ResumePreviewVO.badRequest("文件不存在");
        }

        try {
            byte[] fileBytes = HttpUtils.downloadBytes(fileUrl);
            if (fileBytes == null || fileBytes.length == 0) {
                return ResumePreviewVO.badRequest("文件下载失败");
            }

            String lowerName = fileName != null ? fileName.toLowerCase() : "";
            if (lowerName.endsWith(".docx")) {
                // .docx → 返回原始字节，前端 mammoth.js 渲染
                return ResumePreviewVO.ok(fileBytes,
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            }
            // .doc → 服务端 POI 转 HTML
            String html = wordPreviewService.convertToHtml(fileBytes, fileName);
            return ResumePreviewVO.ok(html.getBytes(StandardCharsets.UTF_8),
                    "text/html;charset=UTF-8");
        } catch (Exception e) {
            log.error("Word 预览失败: resumeId={}, fileName={}", id, fileName, e);
            return ResumePreviewVO.serverError("文档预览失败: " + e.getMessage());
        }
    }

    /** 上传文件。 */
    @Override
    @Transactional
    public ResumeVO upload(MultipartFile file, Long candidateId, Long jobPositionId,
                           Long referrerId, Boolean autoScreen) {
        String originalName = file.getOriginalFilename();

        // 读取文件字节
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            log.error("读取上传文件字节失败", e);
            throw new ValidationException("无法读取上传文件");
        }

        // RustFS 文件上传
        String fileUrl = uploadFileToStorage(fileBytes, originalName, file.getContentType());

        // 解析候选人（附带推荐人信息）
        Long resolvedCandidateId = resolveCandidate(candidateId, referrerId);

        // 保存 Resume 记录（状态：待解析）
        Resume resume = new Resume();
        resume.setCandidateId(resolvedCandidateId);
        resume.setFileName(originalName);
        resume.setFileSize(file.getSize());
        resume.setFileUrl(fileUrl);

        if (originalName != null) {
            String upper = originalName.toUpperCase();
            if (upper.endsWith(".PDF")) {
                resume.setFileType(RecruitmentEnums.FileType.PDF.getCode());
            } else if (upper.endsWith(".DOCX") || upper.endsWith(".DOC")) {
                resume.setFileType(RecruitmentEnums.FileType.DOCX.getCode());
            } else {
                resume.setFileType(RecruitmentEnums.FileType.PDF.getCode());
            }
        }

        // 验证并设置关联职位
        if (jobPositionId == null) {
            throw new ValidationException("jobPositionId", "关联职位不能为空");
        }
        JobPosition job = jobPositionMapper.selectById(jobPositionId);
        if (job == null) {
            throw new ValidationException("jobPositionId",
                    "职位不存在: " + jobPositionId);
        }
        int status = job.getStatus();
        if (status == RecruitmentEnums.JobStatus.DRAFT.getCode()) {
            throw new ValidationException("jobPositionId",
                    "草稿状态的职位不能关联简历");
        }
        if (status == RecruitmentEnums.JobStatus.CLOSED.getCode()) {
            throw new ValidationException("jobPositionId",
                    "已关闭的职位不能关联简历");
        }
        resume.setJobPositionId(jobPositionId);

        // 上传后是否自动执行 AI 筛选（默认开启）
        resume.setAutoScreen(autoScreen == null || autoScreen);
        resume.setParseStatus(RecruitmentEnums.ParseStatus.PENDING.getCode());
        resume.setCreatedAt(DateUtils.now());
        resume.setCreateUserId(UserContextUtil.getUserId());
        resume.setCreateBy(UserContextUtil.getUsername());
        resume.setUpdateUserId(UserContextUtil.getUserId());
        resume.setUpdateBy(UserContextUtil.getUsername());
        resumeMapper.insert(resume);

        // 自动创建/关联求职申请记录，激活 rec_application 表
        applicationService.findOrCreate(resolvedCandidateId, jobPositionId);

        // 记录简历上传活动动态
        try {
            Candidate candidate = candidateMapper.selectById(resolvedCandidateId);
            String candidateName = candidate != null ? candidate.getName() : "未知候选人";
            String jobTitle = job.getTitle();
            activityFeedService.recordActivity(
                    RecruitmentEnums.ActivityType.APPLY,
                    candidateName + " 上传了简历",
                    candidateName + " 上传了简历，投递职位「" + jobTitle + "」。",
                    0L, "system", "RESUME", resume.getId());
        } catch (Exception e) {
            log.warn("Failed to record resume upload activity: {}", e.getMessage());
        }

        // 生成待办：筛选简历
        try {
            Candidate candidate = candidateMapper.selectById(resolvedCandidateId);
            String candidateName = candidate != null ? candidate.getName() : "待解析";
            workbenchTaskService.createTask(
                    UserContextUtil.getUserIdOrDefault(0L),
                    "筛选简历 - " + candidateName,
                    candidateName + " 上传了简历，投递职位「" + job.getTitle() + "」，请尽快筛选。",
                    0, 1, "RESUME", resume.getId(),
                    candidateName,
                    DateUtils.now().plusDays(1));
        } catch (Exception e) {
            log.warn("Failed to create workbench task for resume upload: {}", e.getMessage());
        }

        // 站内通知职位负责人：新简历待筛选
        try {
            Candidate candidate = candidateMapper.selectById(resolvedCandidateId);
            String candidateName = candidate != null ? candidate.getName() : "待解析";
            sendNotification(resolveJobOwner(job),
                    "新简历待筛选 - " + candidateName,
                    candidateName + " 上传了简历，投递职位「" + job.getTitle() + "」，请尽快筛选。",
                    NotificationConstants.TYPE_RECRUITMENT,
                    NotificationConstants.BIZ_RESUME_UPLOADED,
                    resume.getId(),
                    "/resumes");
        } catch (Exception e) {
            log.warn("Failed to send resume upload notification: {}", e.getMessage());
        }

        // 事务提交后立即触发异步解析，消除定时任务延迟
        final Long finalResumeId = resume.getId();
        final byte[] capturedBytes = fileBytes;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
    /** 事务提交后执行回调处理。 */
            @Override
            public void afterCommit() {
                resumeParseService.parseAsync(finalResumeId, capturedBytes, originalName);
            }
        });

        log.info("简历上传完成（事务提交后自动触发解析）: id={}, file={}, candidateId={}, jobPositionId={}",
                resume.getId(), resume.getFileName(), resolvedCandidateId, jobPositionId);
        return resumeConverter.toVO(resume);
    }

    /**
     * 解析/验证候选人：给定 ID 则校验存在性，否则创建占位记录。
     */
    private Long resolveCandidate(Long candidateId, Long referrerId) {
        if (candidateId != null) {
            if (candidateMapper.selectById(candidateId) == null) {
                throw new ResourceNotFoundException("Candidate", candidateId);
            }
            return candidateId;
        }
        Candidate placeholder = new Candidate();
        placeholder.setName("待解析");
        placeholder.setEmail("pending_" + UUID.randomUUID().toString().substring(0, 8) + "@placeholder.local");
        placeholder.setCurrentStage(0);
        placeholder.setCreatedAt(DateUtils.now());
        // 指定了推荐人 → 来源标记为内推
        if (referrerId != null) {
            placeholder.setSource(RecruitmentEnums.CandidateSource.REFERRAL.getCode());
            placeholder.setReferrerId(referrerId);
        }
        candidateMapper.insert(placeholder);
        log.info("自动创建占位候选人: id={}, source={}, referrerId={}",
                placeholder.getId(), placeholder.getSource(), placeholder.getReferrerId());
        return placeholder.getId();
    }

    /**
     * 上传文件字节到 RustFS（通过 Feign）。
     */
    private String uploadFileToStorage(byte[] fileBytes, String originalName, String contentType) {
        if (fileStorageService == null) {
            throw new RuntimeException("FileStorageService 未就绪，无法上传文件");
        }
        String now = DateUtils.formatSlashMonth(DateUtils.now());
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        // 只保留 ASCII 字符作为 RustFS key（中文等非 ASCII 字符通过 Feign 头传递会乱码）
        String ext = ".pdf";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        String keyName = uuid + ext.toLowerCase();
        String relativePath = String.format("resumes/%s/%s", now, keyName);
        String rustFsUrl = fileStorageService.upload(fileBytes, originalName, contentType, relativePath);
        log.info("简历文件已上传至 RustFS: url={}", rustFsUrl);
        return rustFsUrl;
    }

    /** 批量执行 AI 简历筛选。 */
    @Override
    public BatchScreenResultVO batchScreen(BatchScreenRequest request) {
        List<Long> resumeIds = request.getResumeIds().stream()
                .map(Long::valueOf)
                .toList();

        log.info("批量 AI 筛选已提交: resumeCount={}, jobId={}",
                resumeIds.size(), request.getJobId());

        String taskId = aiScreeningService.batchScreenAsync(resumeIds, request.getJobId());

        BatchScreenResultVO result = new BatchScreenResultVO();
        result.setTaskId(taskId);
        result.setTotalCount(request.getResumeIds().size());
        return result;
    }

    /** 分页查询记录列表，支持多条件筛选。 */
    @Override
    public PageResult<ResumeVO> pageQuery(ResumePageQuery query) {
        Page<Resume> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<Resume> wrapper = new LambdaQueryWrapper<>();
        if (query.getCandidateId() != null) {
            wrapper.eq(Resume::getCandidateId, query.getCandidateId());
        }
        if (query.getParseStatus() != null) {
            wrapper.eq(Resume::getParseStatus, query.getParseStatus());
        }
        if (query.getScreeningStatus() != null) {
            wrapper.eq(Resume::getScreeningStatus, query.getScreeningStatus());
        }

        // 关键字搜索：按候选人姓名过滤
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            List<Long> matchedCandidateIds = candidateMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Candidate>()
                    .like(Candidate::getName, query.getKeyword())
            ).stream().map(Candidate::getId).toList();
            if (matchedCandidateIds.isEmpty()) {
                wrapper.eq(Resume::getId, -1L); // 强制返回空结果
            } else {
                wrapper.in(Resume::getCandidateId, matchedCandidateIds);
            }
        }

        // 按来源渠道过滤（source 在 Candidate 表上，需提前查候选 ID）
        if (query.getSource() != null) {
            List<Long> sourceCandidateIds = candidateMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Candidate>()
                    .eq(Candidate::getSource, query.getSource())
            ).stream().map(Candidate::getId).toList();
            if (sourceCandidateIds.isEmpty()) {
                wrapper.eq(Resume::getId, -1L);
            } else {
                wrapper.in(Resume::getCandidateId, sourceCandidateIds);
            }
        }

        // 按职位过滤
        if (query.getJobId() != null && !query.getJobId().isBlank()) {
            wrapper.eq(Resume::getJobPositionId, Long.valueOf(query.getJobId()));
        }

        // 最低 AI 匹配分过滤（0 表示不过滤）
        if (query.getMinScore() != null && query.getMinScore() > 0) {
            wrapper.ge(Resume::getAiMatchScore, new java.math.BigDecimal(query.getMinScore()));
        }

        wrapper.orderByDesc(Resume::getCreatedAt);

        IPage<Resume> result = resumeMapper.selectPage(page, wrapper);

        // 批量加载候选人信息
        List<Long> candidateIds = result.getRecords().stream()
                .map(Resume::getCandidateId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Candidate> candidateMap = candidateIds.isEmpty() ? Map.of() :
                candidateMapper.selectBatchIds(candidateIds).stream()
                        .collect(java.util.stream.Collectors.toMap(Candidate::getId, c -> c));

        // 批量加载职位信息
        List<Long> jobIds = result.getRecords().stream()
                .map(Resume::getJobPositionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, JobPosition> jobMap = jobIds.isEmpty() ? Map.of() :
                jobPositionMapper.selectBatchIds(jobIds).stream()
                        .collect(java.util.stream.Collectors.toMap(JobPosition::getId, j -> j));

        return PageResult.from(result).map(entity -> {
            ResumeVO vo = resumeConverter.toVO(entity);

            // 填充候选人姓名和技能
            Candidate candidate = candidateMap.get(entity.getCandidateId());
            if (candidate != null) {
                vo.setCandidateName(candidate.getName());
                vo.setSource(candidate.getSource());
                if (candidate.getSkills() != null) {
                    vo.setSkills(candidate.getSkills());
                }
            }

            // 填充职位信息
            if (entity.getJobPositionId() != null) {
                vo.setJobId(entity.getJobPositionId());
                JobPosition job = jobMap.get(entity.getJobPositionId());
                if (job != null) {
                    vo.setJobTitle(job.getTitle());
                }
            }

            // 映射 AI 匹配分
            if (entity.getAiMatchScore() != null) {
                vo.setMatchScore(entity.getAiMatchScore());
            }

            return vo;
        });
    }

    /** 根据主键查询详情。 */
    @Override
    public ResumeDetailVO getById(Long id) {
        Resume entity = resumeMapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("Resume", id);
        }
        ResumeDetailVO detailVO = resumeConverter.toDetailVO(entity);

        // 填充候选人姓名、技能、来源、推荐人信息
        if (entity.getCandidateId() != null) {
            Candidate candidate = candidateMapper.selectById(entity.getCandidateId());
            if (candidate != null) {
                detailVO.setCandidateName(candidate.getName());
                detailVO.setAvatarUrl(candidate.getAvatarUrl());
                if (candidate.getSkills() != null) {
                    detailVO.setSkills(candidate.getSkills());
                }
                detailVO.setSource(candidate.getSource());
                detailVO.setReferrerId(candidate.getReferrerId());

                // 内推类型：获取推荐人姓名、部门、职位
                if (candidate.getSource() != null
                        && candidate.getSource().equals(RecruitmentEnums.CandidateSource.REFERRAL.getCode())
                        && candidate.getReferrerId() != null) {
                    try {
                        var userResult = systemUserClient.getUserById(candidate.getReferrerId());
                        if (userResult != null && userResult.data() != null) {
                            var data = userResult.data();
                            detailVO.setReferrerName((String) data.get("realName"));
                            detailVO.setReferrerDepartment((String) data.get("departmentName"));
                            detailVO.setReferrerPosition((String) data.get("position"));
                        }
                    } catch (Exception e) {
                        log.warn("获取内推人信息失败，referrerId={}", candidate.getReferrerId(), e);
                    }
                }
            }
        }
        // 填充职位信息
        if (entity.getJobPositionId() != null) {
            detailVO.setJobId(entity.getJobPositionId());
            JobPosition job = jobPositionMapper.selectById(entity.getJobPositionId());
            if (job != null) {
                detailVO.setJobTitle(job.getTitle());
            }
        }
        // 填充 AI 匹配分
        if (entity.getAiMatchScore() != null) {
            detailVO.setMatchScore(entity.getAiMatchScore());
        }

        // 附加已存储的 AI 筛选结果
        AiScreeningResultVO aiResult = aiScreeningService.getResult(id);
        if (aiResult != null) {
            detailVO.setAiResult(aiResult);
        }

        return detailVO;
    }

    /** 查询简历的 AI 筛选结果。 */
    @Override
    public AiScreeningResultVO getAiResult(Long resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) {
            throw new ResourceNotFoundException("Resume", resumeId);
        }
        return aiScreeningService.getResult(resumeId);
    }

    /** 更新简历筛选状态。 */
    @Override
    @Transactional
    public void updateScreeningStatus(Long resumeId, Integer screeningStatus) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) {
            throw new ResourceNotFoundException("Resume", resumeId);
        }
        int oldStatus = resume.getScreeningStatus() != null ? resume.getScreeningStatus() : -1;
        resume.setScreeningStatus(screeningStatus);
        resumeMapper.updateById(resume);
        log.info("简历筛选状态已更新: id={}, status={}", resumeId, screeningStatus);

        // 记录筛选状态变更活动动态
        try {
            Candidate candidate = candidateMapper.selectById(resume.getCandidateId());
            String candidateName = candidate != null ? candidate.getName() : "未知候选人";
            String jobTitle = resume.getJobPositionId() != null
                    ? jobPositionMapper.selectById(resume.getJobPositionId()).getTitle() : "未知职位";
            String statusLabel = screeningStatus == 1 ? "通过" : screeningStatus == 2 ? "未通过" : "待筛选";
            activityFeedService.recordActivity(
                    RecruitmentEnums.ActivityType.SCREEN,
                    candidateName + " 简历筛选" + statusLabel,
                    "候选人 " + candidateName + " 投递「" + jobTitle + "」的简历筛选结果为：" + statusLabel + "。",
                    0L, "system", "RESUME", resume.getId());
        } catch (Exception e) {
            log.warn("Failed to record screening status activity: {}", e.getMessage());
        }

        // 生成待办：筛选通过 → 安排面试；筛选未通过 → 无后续任务
        try {
            Candidate candidate = candidateMapper.selectById(resume.getCandidateId());
            String candidateName = candidate != null ? candidate.getName() : "未知候选人";
            if (screeningStatus == 1) {
                workbenchTaskService.createTask(
                        UserContextUtil.getUserIdOrDefault(0L),
                        "安排面试 - " + candidateName,
                        candidateName + " 的简历筛选已通过，请尽快安排面试。",
                        1, 0, "CANDIDATE", resume.getCandidateId(),
                        candidateName,
                        DateUtils.now().plusDays(3));
            }
        } catch (Exception e) {
            log.warn("Failed to create workbench task for screening status: {}", e.getMessage());
        }

        // 站内通知职位负责人：筛选结果
        try {
            Candidate candidate = candidateMapper.selectById(resume.getCandidateId());
            String candidateName = candidate != null ? candidate.getName() : "未知候选人";
            String jobTitle = resume.getJobPositionId() != null
                    ? jobPositionMapper.selectById(resume.getJobPositionId()).getTitle() : "未知职位";
            boolean passed = screeningStatus != null
                    && screeningStatus == RecruitmentEnums.ScreeningStatus.PASSED.getCode();
            JobPosition job = resume.getJobPositionId() != null
                    ? jobPositionMapper.selectById(resume.getJobPositionId()) : null;
            String title = (passed ? "简历筛选通过 - " : "简历筛选未通过 - ")
                    + candidateName + "（" + jobTitle + "）";
            String content = "候选人 " + candidateName + " 投递「" + jobTitle + "」的简历筛选"
                    + (passed ? "已通过，请尽快安排面试。" : "未通过。");
            String bizType = passed ? NotificationConstants.BIZ_RESUME_SCREEN_PASSED
                    : NotificationConstants.BIZ_RESUME_SCREEN_REJECTED;
            // 职位负责人优先接收；若当前操作人不是负责人，则同时通知操作人，
            // 保证执行筛选的管理账号一定能看到消息。
            Long owner = resolveJobOwner(job);
            sendNotification(owner, title, content,
                    NotificationConstants.TYPE_RECRUITMENT, bizType, resume.getId(),
                    "/resumes/" + resume.getId());
            Long operator = UserContextUtil.getUserIdOrDefault(0L);
            if (operator != null && !operator.equals(owner)) {
                sendNotification(operator, title, content,
                        NotificationConstants.TYPE_RECRUITMENT, bizType, resume.getId(),
                        "/resumes/" + resume.getId());
            }
        } catch (Exception e) {
            log.warn("Failed to send screening status notification: {}", e.getMessage());
        }
    }

    /** 更新简历关联的职位。 */
    @Override
    @Transactional
    public void updateJobPosition(Long resumeId, Long jobPositionId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) {
            throw new ResourceNotFoundException("Resume", resumeId);
        }
        if (jobPositionId != null) {
            JobPosition job = jobPositionMapper.selectById(jobPositionId);
            if (job == null) {
                throw new ValidationException("jobPositionId",
                        "职位不存在: " + jobPositionId);
            }
            int status = job.getStatus();
            if (status == RecruitmentEnums.JobStatus.DRAFT.getCode()) {
                throw new ValidationException("jobPositionId",
                        "草稿状态的职位不能关联简历");
            }
            if (status == RecruitmentEnums.JobStatus.CLOSED.getCode()) {
                throw new ValidationException("jobPositionId",
                        "已关闭的职位不能关联简历");
            }
        }
        resume.setJobPositionId(jobPositionId);
        resumeMapper.updateById(resume);
        log.info("简历职位关联已更新: resumeId={}, jobPositionId={}", resumeId, jobPositionId);
    }

    /** 查询简历解析状态。 */
    @Override
    public ResumeParseStatusVO getParseStatus(Long resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) {
            throw new ResourceNotFoundException("Resume", resumeId);
        }
        ResumeParseStatusVO result = new ResumeParseStatusVO();
        result.setResumeId(resume.getId());
        result.setParseStatus(resume.getParseStatus());
        if (RecruitmentEnums.ParseStatus.FAILED.getCode() == resume.getParseStatus()
                && resume.getParseError() != null && !resume.getParseError().isEmpty()) {
            result.setParseError(resume.getParseError());
        }
        return result;
    }

    /** 根据主键删除记录。 */
    @Override
    @Transactional
    public void delete(Long id) {
        Resume resume = resumeMapper.selectById(id);
        if (resume == null) {
            throw new ResourceNotFoundException("Resume", id);
        }

        // 删除 RustFS 上的文件（失败不影响数据库删除）
        String fileUrl = resume.getFileUrl();
        if (fileUrl != null && fileStorageService != null) {
            String relativePath = extractRelativePath(fileUrl);
            if (relativePath != null) {
                fileStorageService.delete(relativePath);
            }
        }

        resumeMapper.deleteById(id);
        log.info("简历已删除: id={}, file={}", id, resume.getFileName());
    }

    /** 查询统计信息。 */
    @Override
    public ResumeStatsVO getStats() {
        LambdaQueryWrapper<Resume> wrapper = new LambdaQueryWrapper<>();
        Long total = resumeMapper.selectCount(wrapper);
        Long pending = resumeMapper.selectCount(
                new LambdaQueryWrapper<Resume>().eq(Resume::getScreeningStatus, 0));
        Long passed = resumeMapper.selectCount(
                new LambdaQueryWrapper<Resume>().eq(Resume::getScreeningStatus, 1));
        Long rejected = resumeMapper.selectCount(
                new LambdaQueryWrapper<Resume>().eq(Resume::getScreeningStatus, 2));
        return ResumeStatsVO.builder()
                .total(total).pending(pending).passed(passed).rejected(rejected)
                .build();
    }

    /**
     * 从 fileUrl 中提取 RustFS 桶中的相对路径（如 resumes/2026/07/xxx.pdf）。
     */
    private static String extractRelativePath(String fileUrl) {
        if (fileUrl == null) return null;
        int idx = fileUrl.indexOf("/resumes/");
        return idx >= 0 ? fileUrl.substring(idx + 1) : null;
    }

    /**
     * 解析职位负责人：优先招聘负责人，其次创建人，最后当前操作人。
     */
    private Long resolveJobOwner(JobPosition job) {
        if (job != null && job.getResponsibleId() != null) {
            return job.getResponsibleId();
        }
        if (job != null && job.getCreateUserId() != null) {
            return job.getCreateUserId();
        }
        return UserContextUtil.getUserIdOrDefault(0L);
    }

    /**
     * 发送站内通知（best-effort，失败不影响主流程）。
     */
    private void sendNotification(Long userId, String title, String content, int type,
                                  String businessType, Long businessId, String actionUrl) {
        if (userId == null || userId == 0L) {
            log.debug("通知接收人为空，跳过: title={}", title);
            return;
        }
        try {
            NotificationRequest request = new NotificationRequest();
            request.setUserId(userId);
            request.setTitle(title);
            request.setContent(content);
            request.setType(type);
            request.setBusinessType(businessType);
            request.setBusinessId(businessId);
            request.setActionUrl(actionUrl);
            systemNotificationClient.send(request);
        } catch (Exception e) {
            log.warn("站内通知发送失败（可忽略）: userId={}, title={}, error={}",
                    userId, title, e.getMessage());
        }
    }
}
