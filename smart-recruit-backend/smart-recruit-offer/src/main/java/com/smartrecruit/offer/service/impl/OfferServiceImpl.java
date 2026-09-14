package com.smartrecruit.offer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruit.common.constant.NotificationConstants;
import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.aiengine.agents.OfferPredictorAgent;
import com.smartrecruit.aiengine.domain.CandidateProfile;
import com.smartrecruit.aiengine.domain.OfferDetail;
import com.smartrecruit.aiengine.domain.PredictionResult;
import com.smartrecruit.aiengine.dto.request.OfferPredictRequest;
import com.smartrecruit.aiengine.dto.response.PredictOfferVO;
import com.smartrecruit.offer.enums.OfferEnums;
import com.smartrecruit.common.exception.BusinessException;
import com.smartrecruit.common.exception.ForbiddenException;
import com.smartrecruit.common.exception.ResourceNotFoundException;
import com.smartrecruit.offer.dto.request.CreateOfferRequest;
import com.smartrecruit.offer.dto.request.CreateOnboardingRequest;
import com.smartrecruit.offer.dto.request.OfferApprovalRequest;
import com.smartrecruit.offer.dto.request.UpdateOfferRequest;
import com.smartrecruit.offer.dto.response.OfferApprovalListVO;
import com.smartrecruit.offer.dto.response.OfferDetailVO;
import com.smartrecruit.offer.dto.response.OfferOptionVO;
import com.smartrecruit.offer.dto.response.OfferPredictionVO;
import com.smartrecruit.offer.dto.response.OfferVO;
import com.smartrecruit.offer.dto.response.OfferApprovalStatsVO;
import com.smartrecruit.offer.entity.Offer;
import com.smartrecruit.offer.entity.OfferApproval;
import com.smartrecruit.offer.entity.Onboarding;
import com.smartrecruit.offer.dto.remote.ApplicationDTO;
import com.smartrecruit.offer.dto.remote.UserDTO;
import com.smartrecruit.offer.dto.response.ApprovalFlowConfigVO;
import com.smartrecruit.offer.dto.response.ApprovalFlowConfigVO.ApproverVO;
import com.smartrecruit.offer.dto.response.ApprovalFlowConfigVO.FlowNodeVO;
import com.smartrecruit.offer.dto.remote.ActivityRecordRequest;
import com.smartrecruit.offer.dto.remote.WorkbenchTaskRequest;
import com.smartrecruit.offer.dto.remote.NotificationRequest;
import com.smartrecruit.offer.feign.RecruitmentClient;
import com.smartrecruit.offer.feign.SystemClient;
import com.smartrecruit.offer.feign.AiEngineClient;
import com.smartrecruit.offer.repository.OfferApprovalMapper;
import com.smartrecruit.offer.repository.OfferMapper;
import com.smartrecruit.offer.repository.OnboardingMapper;
import com.smartrecruit.offer.service.ApprovalFlowConfigService;
import com.smartrecruit.offer.service.OfferMailService;
import com.smartrecruit.offer.service.OfferService;
import com.smartrecruit.offer.service.OnboardingService;
import com.smartrecruit.common.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Offer 管理服务实现。
 *
 * <p>处理 Offer 完整生命周期，包含自动计算薪酬以及 AI 接受度预测
 * （LLM 优先，AI 引擎不可用时降级本地启发式）。</p>
 *
 * @since 1.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OfferServiceImpl implements OfferService {

    /** AI 预测有效期：15 分钟，过期后由定时任务或按需重新预测。 */
    private static final Duration AI_PREDICTION_TTL = Duration.ofMinutes(15);

    /** 需要 AI 预测的 Offer 状态（未进入终态：草稿/审批/已批/已发/洽谈中）。 */
    private static final List<Integer> AI_PREDICT_STATUSES =
            List.of(0, 1, 2, 3, 6);

    private final OfferMapper offerMapper;
    private final OfferApprovalMapper approvalMapper;
    private final RecruitmentClient recruitmentClient;
    private final OfferMailService offerMailService;
    private final ApprovalFlowConfigService flowConfigService;
    private final SystemClient systemClient;
    private final OnboardingService onboardingService;
    private final OfferPredictorAgent offerPredictorAgent;
    private final OnboardingMapper onboardingMapper;
    private final AiEngineClient aiEngineClient;

    /** 分页查询记录列表，支持多条件筛选。 */
    @Override
    public PageResult<OfferVO> pageQuery(Page<?> page, Map<String, Object> params) {
        Page<Offer> mpPage = new Page<>(page.getCurrent(), page.getSize());
        IPage<Offer> result = offerMapper.selectPageWithFilters(mpPage, params);
        List<OfferVO> vos = result.getRecords().stream()
                .map(this::toVO)
                .toList();
        return new PageResult<>(vos, result.getTotal(), result.getSize(), result.getCurrent(), result.getPages());
    }

    /** 查询可创建合同的 Offer 下拉选项。 */
    @Override
    public List<OfferOptionVO> listContractOptions(String keyword) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Offer> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.in(Offer::getStatus,
                OfferEnums.OfferStatus.APPROVED.getCode(),
                OfferEnums.OfferStatus.SENT.getCode(),
                OfferEnums.OfferStatus.ACCEPTED.getCode());
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Offer::getOfferNo, kw)
                    .or().like(Offer::getCandidateName, kw)
                    .or().like(Offer::getPositionTitle, kw));
        }
        wrapper.orderByDesc(Offer::getCreateTime).last("LIMIT 50");
        return offerMapper.selectList(wrapper).stream()
                .map(o -> OfferOptionVO.builder()
                        .id(o.getId())
                        .offerNo(o.getOfferNo())
                        .candidateName(o.getCandidateName())
                        .positionTitle(o.getPositionTitle())
                        .status(o.getStatus())
                        .statusLabel(OfferEnums.OfferStatus.fromCode(o.getStatus()) != null
                                ? OfferEnums.OfferStatus.fromCode(o.getStatus()).getLabel() : "未知")
                        .build())
                .toList();
    }

    /** 根据主键查询详情。 */
    @Override
    public OfferDetailVO getById(Long id) {
        Offer offer = offerMapper.selectById(id);
        if (offer == null) {
            throw new ResourceNotFoundException("Offer", id);
        }

        List<OfferApproval> approvals = approvalMapper.selectByOfferId(id);
        List<OfferDetailVO.OfferApprovalVO> approvalVOs = approvals.stream()
                .map(a -> OfferDetailVO.OfferApprovalVO.builder()
                        .id(a.getId())
                        .approverId(a.getApproverId())
                        .approverName(a.getApproverName())
                        .approverRole(a.getApproverRole())
                        .approvalLevel(a.getApprovalLevel())
                        .status(a.getStatus())
                        .comment(a.getComment())
                        .approveTime(a.getApproveTime())
                        .build())
                .toList();

        BigDecimal totalPackage = calcTotalPackage(offer);

        return OfferDetailVO.builder()
                .id(offer.getId())
                .applicationId(offer.getApplicationId())
                .candidateId(offer.getCandidateId())
                .candidateName(offer.getCandidateName())
                .jobPositionId(offer.getJobPositionId())
                .offerNo(offer.getOfferNo())
                .positionTitle(offer.getPositionTitle())
                .departmentName(offer.getDepartmentName())
                .level(offer.getLevel())
                .baseSalary(getBaseSalary(offer))
                .bonusMonths(getBonusMonths(offer))
                .stockOptions(getStockOptions(offer))
                .signOnBonus(getSignOnBonus(offer))
                .totalPackage(totalPackage)
                .status(offer.getStatus())
                .expectedOnboardDate(offer.getExpectedOnboardDate())
                .validUntil(offer.getValidUntil())
                .sendTime(offer.getSendTime())
                .respondTime(offer.getRespondTime())
                .declineReason(offer.getDeclineReason())
                .salaryStructure(offer.getSalaryStructure())
                .approvals(approvalVOs)
                .createBy(offer.getCreateBy())
                .creatorName(resolveCreatorName(offer))
                .createTime(offer.getCreateTime())
                .updateTime(offer.getUpdateTime())
                .build();
    }

    /** 创建记录。 */
    @Override
    @Transactional
    public OfferVO create(CreateOfferRequest request) {
        // 自动计算年薪总包
        BigDecimal baseSalary = request.getBaseSalary() != null ? request.getBaseSalary() : BigDecimal.ZERO;
        int bonusMonths = request.getBonusMonths() != null ? request.getBonusMonths() : 0;
        BigDecimal stockOptions = request.getStockOptions() != null ? request.getStockOptions() : BigDecimal.ZERO;
        BigDecimal signOnBonus = request.getSignOnBonus() != null ? request.getSignOnBonus() : BigDecimal.ZERO;

        // totalPackage = baseSalary * 12 + baseSalary * bonusMonths + stockOptions + signOnBonus
        BigDecimal annualBase = baseSalary.multiply(BigDecimal.valueOf(12));
        BigDecimal annualBonus = baseSalary.multiply(BigDecimal.valueOf(bonusMonths));
        BigDecimal totalPackage = annualBase.add(annualBonus).add(stockOptions).add(signOnBonus);

        // 生成薪酬结构 JSON
        Map<String, Object> salaryStructure = Map.of(
                "baseSalary", baseSalary,
                "bonusMonths", bonusMonths,
                "stockOptions", stockOptions,
                "signOnBonus", signOnBonus,
                "annualBase", annualBase,
                "annualBonus", annualBonus,
                "totalPackage", totalPackage
        );

        String today = DateUtils.formatCompactDate(DateUtils.today());
        String offerNo = "OFF-" + today + "-" + String.format("%04d", ThreadLocalRandom.current().nextInt(1, 9999));

        Offer offer = new Offer();
        offer.setApplicationId(resolveApplicationId(request.getCandidateId(), request.getJobPositionId()));
        offer.setCandidateId(request.getCandidateId());
        offer.setCandidateName(request.getCandidateName());
        offer.setCandidateEmail(request.getCandidateEmail());
        offer.setJobPositionId(request.getJobPositionId());
        offer.setOfferNo(offerNo);
        offer.setPositionTitle(request.getJobTitle());
        offer.setDepartmentName(request.getDepartmentName());
        offer.setLevel(request.getLevel());
        offer.setSalaryStructure(salaryStructure);
        offer.setProbationMonths(3);
        offer.setProbationSalaryRatio(new BigDecimal("0.80"));
        offer.setExpectedOnboardDate(request.getExpectedOnboardDate());
        offer.setValidUntil(request.getValidUntil());
        offer.setStatus(OfferEnums.OfferStatus.DRAFT.getCode());
        // 记录创建人
        try {
            offer.setCreateBy(String.valueOf(getCurrentUserId()));
        } catch (Exception e) {
            log.warn("创建Offer时获取当前用户失败: {}", e.getMessage());
        }

        offerMapper.insert(offer);
        log.info("Offer created: id={}, offerNo={}, totalPackage={}", offer.getId(), offerNo, totalPackage);

        return toVO(offer);
    }

    /** 更新记录。 */
    @Override
    @Transactional
    public OfferVO update(Long id, UpdateOfferRequest request) {
        Offer offer = offerMapper.selectById(id);
        if (offer == null) {
            throw new ResourceNotFoundException("Offer", id);
        }

        if (OfferEnums.OfferStatus.DRAFT.getCode() != offer.getStatus()) {
            throw new BusinessException("OFFER_NOT_EDITABLE",
                    "Only offers in DRAFT status can be edited. Current status: " + offer.getStatus());
        }

        if (request.getBaseSalary() != null || request.getBonusMonths() != null
                || request.getStockOptions() != null || request.getSignOnBonus() != null) {

            BigDecimal baseSalary = request.getBaseSalary() != null
                    ? request.getBaseSalary() : getBaseSalary(offer);
            int bonusMonths = request.getBonusMonths() != null
                    ? request.getBonusMonths() : getBonusMonths(offer);
            BigDecimal stockOptions = request.getStockOptions() != null
                    ? request.getStockOptions() : getStockOptions(offer);
            BigDecimal signOnBonus = request.getSignOnBonus() != null
                    ? request.getSignOnBonus() : getSignOnBonus(offer);

            BigDecimal annualBase = baseSalary.multiply(BigDecimal.valueOf(12));
            BigDecimal annualBonus = baseSalary.multiply(BigDecimal.valueOf(bonusMonths));
            BigDecimal totalPackage = annualBase.add(annualBonus).add(stockOptions).add(signOnBonus);

            Map<String, Object> salaryStructure = Map.of(
                    "baseSalary", baseSalary,
                    "bonusMonths", bonusMonths,
                    "stockOptions", stockOptions,
                    "signOnBonus", signOnBonus,
                    "annualBase", annualBase,
                    "annualBonus", annualBonus,
                    "totalPackage", totalPackage
            );
            offer.setSalaryStructure(salaryStructure);
        }

        if (request.getPositionTitle() != null) {
            offer.setPositionTitle(request.getPositionTitle());
        }
        if (request.getDepartmentName() != null) {
            offer.setDepartmentName(request.getDepartmentName());
        }
        if (request.getLevel() != null) {
            offer.setLevel(request.getLevel());
        }

        offerMapper.updateById(offer);
        log.info("Offer updated: id={}", id);
        return toVO(offer);
    }

    /** 根据主键删除记录。 */
    @Override
    @Transactional
    public void delete(Long id) {
        Offer offer = offerMapper.selectById(id);
        if (offer == null) {
            throw new ResourceNotFoundException("Offer", id);
        }
        if (OfferEnums.OfferStatus.DRAFT.getCode() != offer.getStatus()) {
            throw new BusinessException("OFFER_NOT_DELETABLE",
                    "Only offers in DRAFT status can be deleted. Current status: " + offer.getStatus());
        }
        offerMapper.deleteById(id);
        log.info("Offer deleted: id={}, offerNo={}", id, offer.getOfferNo());
    }

    /** 提交审批。 */
    @Override
    @Transactional
    public void submitApproval(Long id) {
        Offer offer = offerMapper.selectById(id);
        if (offer == null) {
            throw new ResourceNotFoundException("Offer", id);
        }
        if (OfferEnums.OfferStatus.DRAFT.getCode() != offer.getStatus()) {
            throw new BusinessException("OFFER_STATUS_INVALID",
                    "Can only submit DRAFT offers for approval. Current status: " + offer.getStatus());
        }

        offer.setStatus(OfferEnums.OfferStatus.PENDING.getCode());
        offerMapper.updateById(offer);
        log.info("Offer submitted for approval: id={}, offerNo={}", id, offer.getOfferNo());

        // 生成待办：审批Offer
        createTask(offer,
                "审批Offer - " + offer.getCandidateName(),
                "候选人 " + offer.getCandidateName() + " 的「" + offer.getPositionTitle()
                        + "」Offer 已提交审批，请尽快处理。",
                2, 0, 2);

        // 查找部门对应的审批流程配置，通知第1级审批人
        ApprovalFlowConfigVO flowConfig = flowConfigService.getByDepartment(offer.getDepartmentName());
        if (flowConfig != null && flowConfig.getNodes() != null && !flowConfig.getNodes().isEmpty()) {
            flowConfig.getNodes().stream()
                    .filter(n -> n.getLevel() != null && n.getLevel() == 1)
                    .findFirst()
                    .ifPresent(node -> notifyLevelApprovers(offer, node, 1, flowConfig.getMaxLevels()));
        } else {
            log.info("部门 '{}' 未配置审批流程，跳过通知: offerId={}", offer.getDepartmentName(), id);
        }
    }

    /** 处理审批（通过/驳回）。 */
    @Override
    @Transactional
    public void approve(Long id, OfferApprovalRequest request) {
        Offer offer = offerMapper.selectById(id);
        if (offer == null) {
            throw new ResourceNotFoundException("Offer", id);
        }
        if (OfferEnums.OfferStatus.PENDING.getCode() != offer.getStatus()) {
            throw new BusinessException("OFFER_STATUS_INVALID",
                    "Can only approve offers in PENDING_APPROVAL status. Current: " + offer.getStatus());
        }

        // 权限校验：当前登录用户必须是该阶段配置的审批人
        Long currentUserId = getCurrentUserId();
        ApprovalFlowConfigVO flowConfig = flowConfigService.getByDepartment(offer.getDepartmentName());

        int currentLevel = request.getApprovalLevel() != null
                ? request.getApprovalLevel()
                : detectCurrentLevel(id, flowConfig);
        if (flowConfig != null && flowConfig.getNodes() != null) {
            FlowNodeVO currentNode = flowConfig.getNodes().stream()
                    .filter(n -> n.getLevel() != null && n.getLevel() == currentLevel)
                    .findFirst().orElse(null);
            if (currentNode != null && currentNode.getApprovers() != null
                    && !currentNode.getApprovers().isEmpty()) {
                boolean isAuthorized = currentNode.getApprovers().stream()
                        .anyMatch(a -> a.getApproverId() != null
                                && a.getApproverId().equals(currentUserId));
                if (!isAuthorized) {
                    String configuredIds = currentNode.getApprovers().stream()
                            .map(a -> a.getApproverName() + "(id=" + a.getApproverId() + ")")
                            .collect(Collectors.joining(", "));
                    log.warn("用户无权审批该Offer: userId={}, offerId={}, level={}, configuredApprovers=[{}]",
                            currentUserId, id, currentLevel, configuredIds);
                    String approverNames = currentNode.getApprovers().stream()
                            .map(a -> a.getApproverName() + "（" + (a.getApproverRole() != null ? a.getApproverRole() : "") + "）")
                            .collect(Collectors.joining("、"));
                    throw new ForbiddenException("审批权限不足，当前阶段审批人：" + approverNames);
                }
            }
        }

        // 创建审批记录
        OfferApproval approval = new OfferApproval();
        approval.setOfferId(id);
        approval.setApproverId(currentUserId);
        approval.setApproverName(request.getApproverName());
        approval.setApproverRole(request.getApproverRole());
        approval.setApprovalLevel(currentLevel);
        approval.setStatus(request.getStatus());
        approval.setComment(request.getComment());
        approval.setApproveTime(DateUtils.now());
        approvalMapper.insert(approval);

        // 查找审批流程配置
        int totalLevels = flowConfig != null ? flowConfig.getMaxLevels() : currentLevel;

        if (OfferEnums.ApprovalStatus.APPROVED.getCode() == request.getStatus()) {
            handleApprovalApproved(offer, flowConfig, currentLevel, totalLevels, request.getApproverName(),
                    request.getApproverRole());
        } else if (OfferEnums.ApprovalStatus.REJECTED.getCode() == request.getStatus()) {
            handleApprovalRejected(offer, request.getApproverName(), request.getApproverRole(),
                    request.getComment());
        } else {
            throw new BusinessException("INVALID_APPROVAL_STATUS",
                    "Approval status must be APPROVED or REJECTED.");
        }

        offerMapper.updateById(offer);
        log.info("Offer approval processed: offerId={}, level={}/{}, status={}, approverId={}",
                id, currentLevel, totalLevels, request.getStatus(), request.getApproverId());
    }

    /**
     * 处理审批通过：或签模式（任意一个审批人通过即可），通过后进入下一级。
     */
    private void handleApprovalApproved(Offer offer, ApprovalFlowConfigVO flowConfig,
                                        int currentLevel, int totalLevels,
                                        String approverName, String approverRole) {
        // 或签模式：任意一个审批人通过即流转，无需等待同级别其他人审批
        boolean isLastLevel = currentLevel >= totalLevels;

        if (isLastLevel) {
            // 最后一级全部通过 → 自动发送 Offer（生成确认Token + 状态变为 SENT）
            offer.setStatus(OfferEnums.OfferStatus.SENT.getCode());
            offer.setSendTime(DateUtils.now());
            offer.setConfirmToken(java.util.UUID.randomUUID().toString().replace("-", ""));
            log.info("Offer 全部审批通过并自动发送: offerId={}, candidateName={}", offer.getId(), offer.getCandidateName());

            // 发送录用通知邮件（此时 confirmToken 已存在，邮件中会包含接受/拒绝按钮）
            offerMailService.sendOfferLetter(offer, "审批已全部通过，Offer 正式生效");

            // 通知 Offer 创建人：全部审批完成
            sendNotification(creatorId(offer),
                    "Offer 审批完成并已发送 - " + offerPersonLabel(offer),
                    "「" + offer.getPositionTitle() + "」的 Offer 已通过全部审批并发送给候选人 "
                            + offerPersonLabel(offer) + "，请跟进候选人确认。",
                    NotificationConstants.TYPE_OFFER,
                    NotificationConstants.BIZ_OFFER_APPROVAL_COMPLETE,
                    offer.getId(),
                    "/offers/" + offer.getId());
            String creatorEmail = getCreatorEmail(offer);
            if (creatorEmail != null) {
                offerMailService.sendApprovalCompleteToCreator(offer, creatorEmail);
            } else {
                log.warn("Offer 创建人邮箱获取失败，跳过审批完成通知: offerId={}, createBy={}",
                        offer.getId(), offer.getCreateBy());
            }

            // 记录活动动态：Offer 审批通过并已发送
            try {
                recruitmentClient.recordActivity(new ActivityRecordRequest(
                        3,
                        "Offer 已发送给 " + offer.getCandidateName(),
                        "「" + offer.getPositionTitle() + "」的 Offer 已通过全部审批并发送给候选人 "
                                + offer.getCandidateName() + "，请等待候选人确认。",
                        0L, "system", "OFFER", offer.getId()));
            } catch (Exception e) {
                log.warn("Failed to record offer approval activity: {}", e.getMessage());
            }

            // 生成待办：等待候选人确认Offer
            createTask(offer,
                    "等待确认 - " + offer.getCandidateName(),
                    "「" + offer.getPositionTitle() + "」的 Offer 已通过全部审批并发送，请跟进候选人确认。",
                    5, 1, 5);
        } else {
            // 非最后一级 → 保持 PENDING，通知下一级审批人
            log.info("Offer第{}级审批通过，准备通知第{}级: offerId={}", currentLevel, currentLevel + 1, offer.getId());
            boolean nextLevelFound = false;
            if (flowConfig != null && flowConfig.getNodes() != null) {
                int nextLevel = currentLevel + 1;
                Optional<FlowNodeVO> nextNode = flowConfig.getNodes().stream()
                        .filter(n -> n.getLevel() != null && n.getLevel() == nextLevel)
                        .findFirst();
                if (nextNode.isPresent()) {
                    notifyLevelApprovers(offer, nextNode.get(), nextLevel, totalLevels);
                    nextLevelFound = true;
                }
            }
            if (!nextLevelFound) {
                log.warn("未找到第{}级审批节点配置: offerId={}, flowConfigNull={}",
                        currentLevel + 1, offer.getId(), flowConfig == null);
            }

            // 通知 Offer 创建人：进度更新
            FlowNodeVO currentNode = findNode(flowConfig, currentLevel);
            String nodeName = currentNode != null ? currentNode.getNodeName() : ("第" + currentLevel + "级");
            sendNotification(creatorId(offer),
                    "Offer 审批进度更新 - " + offerPersonLabel(offer),
                    approverName + "（" + approverRole + "）已完成第 " + currentLevel + "/" + totalLevels
                            + " 级审批（" + nodeName + "），Offer 已流转至下一级审批。",
                    NotificationConstants.TYPE_OFFER,
                    NotificationConstants.BIZ_OFFER_APPROVAL_PROGRESS,
                    offer.getId(),
                    "/offers/" + offer.getId() + "/approval-review");
            String creatorEmail = getCreatorEmail(offer);
            if (creatorEmail != null) {
                offerMailService.sendApprovalProgressToCreator(offer, creatorEmail,
                        approverName, nodeName, currentLevel, totalLevels);
            }
        }
    }

    /**
     * 处理审批驳回：Offer 退回草稿，通知创建人。
     */
    private void handleApprovalRejected(Offer offer, String approverName, String approverRole, String comment) {
        offer.setStatus(OfferEnums.OfferStatus.DRAFT.getCode());

        sendNotification(creatorId(offer),
                "Offer 审批被驳回 - " + offerPersonLabel(offer),
                approverName + "（" + approverRole + "）驳回了「" + offer.getPositionTitle()
                        + "」的 Offer 审批" + (comment != null && !comment.isBlank()
                        ? "，意见：" + comment : "") + "，请修改后重新提交。",
                NotificationConstants.TYPE_OFFER,
                NotificationConstants.BIZ_OFFER_REJECTED,
                offer.getId(),
                "/offers/" + offer.getId());
        String creatorEmail = getCreatorEmail(offer);
        if (creatorEmail != null) {
            offerMailService.sendOfferRejectedToCreator(offer, creatorEmail,
                    approverName, approverRole, comment);
        }
    }

    /**
     * 通知指定审批节点的所有审批人（best-effort）。
     */
    private void notifyLevelApprovers(Offer offer, FlowNodeVO node, int level, int totalLevels) {
        if (node.getApprovers() == null || node.getApprovers().isEmpty()) {
            log.warn("第{}级审批节点无审批人配置，无法发送通知邮件: offerId={}, nodeName={}",
                    level, offer.getId(), node.getNodeName());
            return;
        }
        log.info("开始通知第{}级审批人: offerId={}, nodeName={}, approversCount={}",
                level, offer.getId(), node.getNodeName(), node.getApprovers().size());
        int sentCount = 0;
        for (ApproverVO approver : node.getApprovers()) {
            sendNotification(approver.getApproverId(),
                    "待审批 Offer - " + offerPersonLabel(offer),
                    "候选人 " + offerPersonLabel(offer) + " 的「" + offer.getPositionTitle()
                            + "」Offer 已进入第 " + level + " 级审批（" + node.getNodeName()
                            + "），请及时处理。",
                    NotificationConstants.TYPE_OFFER,
                    NotificationConstants.BIZ_OFFER_APPROVAL_REQUEST,
                    offer.getId(),
                    "/offers/" + offer.getId() + "/approval-review");
            String email = fetchUserEmail(approver.getApproverId());
            if (email != null) {
                offerMailService.sendApprovalRequestEmail(offer, email,
                        approver.getApproverName(), node.getNodeName(), level, totalLevels);
                sentCount++;
            } else {
                log.warn("第{}级审批人邮箱获取失败，跳过通知: offerId={}, approverId={}, approverName={}",
                        level, offer.getId(), approver.getApproverId(), approver.getApproverName());
            }
        }
        log.info("第{}级审批通知完成: offerId={}, sentCount={}/approversCount={}",
                level, offer.getId(), sentCount, node.getApprovers().size());
    }

    /**
     * 从 SecurityContext 获取当前登录用户的 ID。
     */
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ForbiddenException("未登录，无法获取当前用户");
        }
        Object credentials = auth.getCredentials();
        if (credentials == null || credentials.toString().isEmpty()) {
            throw new ForbiddenException("用户凭证缺失");
        }
        return Long.parseLong(credentials.toString());
    }

    /**
     * 根据用户 ID 查询邮箱（best-effort，失败返回 null）。
     */
    /**
     * 根据用户 ID 查询真实姓名（best-effort，失败返回 null）。
     */
    private String fetchUserRealName(Long userId) {
        if (userId == null) return null;
        try {
            var response = systemClient.getUser(userId);
            if (response != null && response.ok() && response.data() != null) {
                UserDTO user = response.data();
                return user.getRealName() != null && !user.getRealName().isBlank() ? user.getRealName() : null;
            }
        } catch (Exception e) {
            log.warn("查询用户真实姓名失败: userId={}, error={}", userId, e.getMessage());
        }
        return null;
    }

    /**
     * 根据 offer.getCreateBy() 解析创建人姓名（best-effort）。
     */
    private String resolveCreatorName(Offer offer) {
        if (offer.getCreateBy() == null || offer.getCreateBy().isBlank()) return null;
        try {
            return fetchUserRealName(Long.parseLong(offer.getCreateBy()));
        } catch (NumberFormatException e) {
            log.warn("解析创建人ID失败: createBy={}", offer.getCreateBy());
            return null;
        }
    }

    private String fetchUserEmail(Long userId) {
        if (userId == null) return null;
        try {
            var response = systemClient.getUser(userId);
            if (response != null && response.ok() && response.data() != null) {
                UserDTO user = response.data();
                return user.getEmail() != null && !user.getEmail().isBlank() ? user.getEmail() : null;
            }
        } catch (Exception e) {
            log.warn("查询用户邮箱失败: userId={}, error={}", userId, e.getMessage());
        }
        return null;
    }

    /**
     * 获取 Offer 创建人的邮箱。
     */
    private String getCreatorEmail(Offer offer) {
        if (offer.getCreateBy() == null || offer.getCreateBy().isBlank()) {
            log.warn("Offer 创建人为空，无法获取邮箱: offerId={}", offer.getId());
            return null;
        }
        try {
            Long creatorId = Long.parseLong(offer.getCreateBy());
            String email = fetchUserEmail(creatorId);
            if (email == null) {
                log.warn("未查询到创建人邮箱: offerId={}, creatorId={}", offer.getId(), creatorId);
            }
            return email;
        } catch (NumberFormatException e) {
            log.warn("解析创建人ID失败: createBy={}", offer.getCreateBy());
            return null;
        }
    }

    /**
     * 获取 Offer 创建人用户 ID（best-effort）。
     */
    private Long creatorId(Offer offer) {
        if (offer.getCreateBy() == null || offer.getCreateBy().isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(offer.getCreateBy());
        } catch (NumberFormatException e) {
            log.warn("解析创建人ID失败: createBy={}", offer.getCreateBy());
            return null;
        }
    }

    /**
     * 发送站内通知（best-effort，失败不影响主流程）。
     */
    private String offerPersonLabel(Offer offer) {
        String dept = offer.getDepartmentName() != null && !offer.getDepartmentName().isBlank()
                ? offer.getDepartmentName() : null;
        String pos = offer.getPositionTitle() != null && !offer.getPositionTitle().isBlank()
                ? offer.getPositionTitle() : null;
        if (dept != null && pos != null) {
            return offer.getCandidateName() + "（" + dept + " · " + pos + "）";
        }
        if (dept != null) {
            return offer.getCandidateName() + "（" + dept + "）";
        }
        if (pos != null) {
            return offer.getCandidateName() + "（" + pos + "）";
        }
        return offer.getCandidateName();
    }

    private void sendNotification(Long userId, String title, String content, int type,
                                  String businessType, Long businessId, String actionUrl) {
        if (userId == null) {
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
            systemClient.sendNotification(request);
        } catch (Exception e) {
            log.warn("站内通知发送失败（可忽略）: userId={}, title={}, error={}",
                    userId, title, e.getMessage());
        }
    }

    /**
     * 从流程配置中查找指定级别的节点。
     */
    private FlowNodeVO findNode(ApprovalFlowConfigVO flowConfig, int level) {
        if (flowConfig == null || flowConfig.getNodes() == null) return null;
        return flowConfig.getNodes().stream()
                .filter(n -> n.getLevel() != null && n.getLevel() == level)
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据已有审批记录自动检测当前应处于哪个审批级别。
     * 或签模式：遍历流程配置的每一级，找到第一个尚无任何人审批通过的级别并返回。
     * 如果所有级别都已通过，返回最大级别。
     */
    private int detectCurrentLevel(Long offerId, ApprovalFlowConfigVO flowConfig) {
        if (flowConfig == null || flowConfig.getNodes() == null || flowConfig.getNodes().isEmpty()) {
            return 1;
        }
        List<OfferApproval> approvals = approvalMapper.selectByOfferId(offerId);
        for (FlowNodeVO node : flowConfig.getNodes()) {
            int level = node.getLevel();
            // 任意人审批通过即视为该级别已过
            boolean anyoneApproved = approvals.stream()
                    .anyMatch(a -> a.getApprovalLevel() != null && a.getApprovalLevel() == level
                            && OfferEnums.ApprovalStatus.APPROVED.getCode() == a.getStatus());
            if (!anyoneApproved) {
                return level;
            }
        }
        return flowConfig.getMaxLevels();
    }

    /** 发送通知。 */
    @Override
    @Transactional
    public void send(Long id) {
        Offer offer = offerMapper.selectById(id);
        if (offer == null) {
            throw new ResourceNotFoundException("Offer", id);
        }
        if (OfferEnums.OfferStatus.APPROVED.getCode() != offer.getStatus()) {
            throw new BusinessException("OFFER_NOT_APPROVED",
                    "Can only send approved offers. Current status: " + offer.getStatus());
        }

        offer.setStatus(OfferEnums.OfferStatus.SENT.getCode());
        offer.setSendTime(DateUtils.now());
        if (offer.getConfirmToken() == null || offer.getConfirmToken().isBlank()) {
            offer.setConfirmToken(java.util.UUID.randomUUID().toString().replace("-", ""));
        }
        offerMapper.updateById(offer);

        // 发送 Offer 录用邮件（重新发送，confirmToken 确保邮件中有确认按钮）
        offerMailService.sendOfferLetter(offer, "Offer 已发送，请查收");

        // 记录活动动态
        try {
            recruitmentClient.recordActivity(new ActivityRecordRequest(
                    3,
                    "Offer 已发送给 " + offer.getCandidateName(),
                    "「" + offer.getPositionTitle() + "」的 Offer 已通过审批并发送给候选人 "
                            + offer.getCandidateName() + "，请等待候选人确认。",
                    0L, "system", "OFFER", offer.getId()));
        } catch (Exception e) {
            log.warn("Failed to record offer send activity: {}", e.getMessage());
        }

        // 生成待办：等待候选人确认Offer
        createTask(offer,
                "等待确认 - " + offer.getCandidateName(),
                "「" + offer.getPositionTitle() + "」的 Offer 已发送给候选人 "
                        + offer.getCandidateName() + "，请跟进候选人确认。",
                5, 1, 5);

        log.info("Offer sent: id={}, offerNo={}, sentAt={}", id, offer.getOfferNo(), offer.getSendTime());
    }

    /** 重发通知邮件。 */
    @Override
    public void sendEmail(Long id) {
        Offer offer = offerMapper.selectById(id);
        if (offer == null) {
            throw new ResourceNotFoundException("Offer", id);
        }
        offerMailService.sendOfferLetter(offer, "Offer 录用通知书（重新发送）");
        log.info("Offer email resent: id={}, candidateEmail={}", id, offer.getCandidateEmail());
    }

    /** 根据候选人确认令牌查询 Offer。 */
    @Override
    public OfferDetailVO getByConfirmToken(String token) {
        LambdaQueryWrapper<Offer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Offer::getConfirmToken, token)
                .eq(Offer::getDeleted, 0);
        Offer offer = offerMapper.selectOne(wrapper);
        if (offer == null) {
            throw new ResourceNotFoundException("Offer", token);
        }
        return getById(offer.getId());
    }

    /** 处理候选人通过令牌确认 Offer（接受/拒绝）。 */
    @Override
    @Transactional
    public void confirmOffer(String token, boolean accept, String declineReason) {
        LambdaQueryWrapper<Offer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Offer::getConfirmToken, token)
                .eq(Offer::getDeleted, 0);
        Offer offer = offerMapper.selectOne(wrapper);
        if (offer == null) {
            throw new ResourceNotFoundException("Offer", token);
        }
        if (OfferEnums.OfferStatus.SENT.getCode() != offer.getStatus()) {
            throw new BusinessException("OFFER_NOT_SENT",
                    "该 Offer 当前不可确认，当前状态：" + offer.getStatus());
        }
        if (accept) {
            offer.setStatus(OfferEnums.OfferStatus.ACCEPTED.getCode());

            // 自动创建入职记录
            try {
                onboardingService.create(CreateOnboardingRequest.builder()
                        .offerId(offer.getId())
                        .candidateId(offer.getCandidateId())
                        .onboardDate(offer.getExpectedOnboardDate())
                        .build());
                log.info("Auto-created onboarding for accepted offer: offerId={}, candidateId={}",
                        offer.getId(), offer.getCandidateId());
            } catch (Exception e) {
                log.error("Failed to auto-create onboarding for offerId={}: {}",
                        offer.getId(), e.getMessage(), e);
                // 不阻断 Offer 确认流程，入职记录可后续手动创建
            }
        } else {
            offer.setStatus(OfferEnums.OfferStatus.REJECTED.getCode());
            offer.setDeclineReason(declineReason);
        }
        offer.setRespondTime(DateUtils.now());
        offerMapper.updateById(offer);
        log.info("Offer confirmed by candidate: offerId={}, candidateName={}, accepted={}",
                offer.getId(), offer.getCandidateName(), accept);

        // 记录活动动态
        recordOfferActivity(offer, accept);

        // 生成待办：接受 → 办理入职；拒绝 → 无后续任务
        if (accept) {
            createTask(offer,
                    "办理入职 - " + offer.getCandidateName(),
                    "候选人 " + offer.getCandidateName() + " 已接受「" + offer.getPositionTitle()
                            + "」的Offer，请尽快办理入职手续。",
                    3, 0, 3);
        }
    }

    /** 手动确认 Offer 结果（后台代操作）。 */
    @Override
    public void manualConfirm(Long id, boolean accept, String declineReason) {
        Offer offer = offerMapper.selectById(id);
        if (offer == null) {
            throw new ResourceNotFoundException("Offer", id);
        }

        int currentStatus = offer.getStatus() != null ? offer.getStatus() : -1;
        // 允许在已审批、已发送、已接受、已拒绝状态下进行手动确认
        if (currentStatus != OfferEnums.OfferStatus.APPROVED.getCode()
                && currentStatus != OfferEnums.OfferStatus.SENT.getCode()
                && currentStatus != OfferEnums.OfferStatus.ACCEPTED.getCode()
                && currentStatus != OfferEnums.OfferStatus.REJECTED.getCode()) {
            throw new BusinessException("OFFER_CANNOT_CONFIRM",
                    "当前 Offer 状态不允许手动确认，当前状态：" + currentStatus + "，仅已发送/已接受/已拒绝状态的 Offer 可手动确认");
        }

        int previousStatus = currentStatus;
        if (accept) {
            offer.setStatus(OfferEnums.OfferStatus.ACCEPTED.getCode());
            offer.setDeclineReason(null);

            // 自动创建入职记录（如果之前未创建）
            LambdaQueryWrapper<Onboarding> existCheck = new LambdaQueryWrapper<>();
            existCheck.eq(Onboarding::getOfferId, offer.getId());
            if (onboardingMapper.selectCount(existCheck) == 0) {
                try {
                    onboardingService.create(CreateOnboardingRequest.builder()
                            .offerId(offer.getId())
                            .candidateId(offer.getCandidateId())
                            .onboardDate(offer.getExpectedOnboardDate())
                            .build());
                    log.info("Auto-created onboarding for manually confirmed offer: offerId={}, candidateId={}",
                            offer.getId(), offer.getCandidateId());
                } catch (Exception e) {
                    log.error("Failed to auto-create onboarding for offerId={}: {}", offer.getId(), e.getMessage(), e);
                }
            } else {
                log.info("Onboarding already exists for offerId={}, skipping", offer.getId());
            }
        } else {
            offer.setStatus(OfferEnums.OfferStatus.REJECTED.getCode());
            if (declineReason != null && !declineReason.isBlank()) {
                offer.setDeclineReason(declineReason);
            }
        }
        offer.setRespondTime(DateUtils.now());
        offerMapper.updateById(offer);
        log.info("Offer manually confirmed by HR: offerId={}, candidateName={}, accepted={}, previousStatus={}",
                offer.getId(), offer.getCandidateName(), accept, previousStatus);

        // 记录活动动态
        recordOfferActivity(offer, accept);

        // 生成待办：接受 → 办理入职；拒绝 → 无后续任务
        if (accept) {
            createTask(offer,
                    "办理入职 - " + offer.getCandidateName(),
                    "候选人 " + offer.getCandidateName() + " 已接受「" + offer.getPositionTitle()
                            + "」的Offer，请尽快办理入职手续。",
                    3, 0, 3);
        }
    }

    /**
     * 查询 AI 预测结果（定时任务预预测落库优先）。
     *
     * <p>优先读取 15 分钟内由定时任务写入的预测结果，避免实时调用 LLM；
     * 未预测或已过期时按需实时预测并回写落库，保证新 Offer 页面也有数据。</p>
     */
    @Override
    public OfferPredictionVO getPrediction(Long id) {
        Offer offer = offerMapper.selectById(id);
        if (offer == null) {
            throw new ResourceNotFoundException("Offer", id);
        }

        // 1. 定时任务预预测结果在有效期内：直接读取落库数据
        if (isPredictionFresh(offer)) {
            return buildPredictionVO(id, offer.getAiAcceptProbability().intValue(),
                    offer.getAiRiskLevel(), offer.getAiSuggestion());
        }

        // 2. 未预测或已过期：按需实时预测（LLM 优先）并回写落库
        OfferPredictionResult result = predictAcceptanceProbability(offer,
                getBaseSalary(offer), calcTotalPackage(offer));
        persistPrediction(offer, result);
        return buildPredictionVO(id, result.probability(),
                result.riskLevel(), result.recommendation());
    }

    /**
     * 批量刷新 Offer AI 接受度预测（定时任务调用）。
     *
     * <p>只处理「从未预测过」且未进入终态的 Offer：已预测过的一律跳过，
     * 避免定时任务每次运行重复调用 LLM；预测过期后的更新由按需路径
     * （页面访问时 {@link #getPrediction(Long)} 重新预测）完成。
     * 单条失败不影响其它 Offer。</p>
     */
    @Override
    public void refreshAiPredictions() {
        List<Offer> offers = offerMapper.selectList(
                new LambdaQueryWrapper<Offer>()
                        .eq(Offer::getDeleted, 0)
                        .in(Offer::getStatus, AI_PREDICT_STATUSES)
                        .isNull(Offer::getAiPredictedAt));
        long skipped = offerMapper.selectCount(
                new LambdaQueryWrapper<Offer>()
                        .eq(Offer::getDeleted, 0)
                        .in(Offer::getStatus, AI_PREDICT_STATUSES)
                        .isNotNull(Offer::getAiPredictedAt));
        int updated = 0;
        for (Offer offer : offers) {
            try {
                OfferPredictionResult result = predictAcceptanceProbability(offer,
                        getBaseSalary(offer), calcTotalPackage(offer));
                persistPrediction(offer, result);
                updated++;
            } catch (Exception e) {
                log.error("Offer AI 预测失败（跳过）: offerId={}, error={}",
                        offer.getId(), e.getMessage());
            }
        }
        log.info("Offer AI 接受度预预测完成: pending={}, updated={}, skipped={}",
                offers.size(), updated, skipped);
    }

    /** 预测结果是否在有效期内（15 分钟）。 */
    private boolean isPredictionFresh(Offer offer) {
        return offer.getAiAcceptProbability() != null
                && offer.getAiRiskLevel() != null
                && offer.getAiPredictedAt() != null
                && offer.getAiPredictedAt().isAfter(DateUtils.now().minus(AI_PREDICTION_TTL));
    }

    /** 将预测结果回写 Offer 落库。 */
    private void persistPrediction(Offer offer, OfferPredictionResult result) {
        offer.setAiAcceptProbability(BigDecimal.valueOf(result.probability()));
        offer.setAiRiskLevel(result.riskLevel());
        offer.setAiSuggestion(result.recommendation());
        offer.setAiPredictedAt(DateUtils.now());
        offerMapper.updateById(offer);
    }

    /** 构建预测 VO（概率 + 风险级别 + 影响因素 + 建议）。 */
    private OfferPredictionVO buildPredictionVO(Long id, int probability,
                                                String riskLevel, String suggestion) {
        int confidenceLevel = probability >= 80 ? 2 : probability >= 65 ? 1 : 0;

        List<OfferPredictionVO.Factor> factors = List.of(
                OfferPredictionVO.Factor.builder()
                        .name("薪资竞争力")
                        .impact(probability >= 75 ? "正面影响" : "负面影响")
                        .weight(new BigDecimal("0.35"))
                        .build(),
                OfferPredictionVO.Factor.builder()
                        .name("职位匹配度")
                        .impact(probability >= 70 ? "正面影响" : "负面影响")
                        .weight(new BigDecimal("0.25"))
                        .build(),
                OfferPredictionVO.Factor.builder()
                        .name("市场行情")
                        .impact("正面影响")
                        .weight(new BigDecimal("0.20"))
                        .build(),
                OfferPredictionVO.Factor.builder()
                        .name("候选人活跃度")
                        .impact(probability >= 80 ? "正面影响" : "中性")
                        .weight(new BigDecimal("0.12"))
                        .build(),
                OfferPredictionVO.Factor.builder()
                        .name("公司品牌吸引力")
                        .impact("正面影响")
                        .weight(new BigDecimal("0.08"))
                        .build()
        );

        return OfferPredictionVO.builder()
                .offerId(id)
                .acceptanceProbability(probability)
                .confidenceLevel(confidenceLevel)
                .riskLevel(riskLevel)
                .factors(factors)
                .suggestion(suggestion)
                .build();
    }

    /**
     * 预测候选人接受 Offer 的概率（LLM 优先，本地 Agent 启发式兜底）。
     *
     * <p>优先调用 AI 引擎的 {@code /offer/predict} 能力（LLM 生成真实预测），
     * 调用失败或 LLM 不可用时降级到本地 {@link OfferPredictorAgent}，
     * 最终兜底默认概率，保证任何情况下页面都有数据。</p>
     */
    private OfferPredictionResult predictAcceptanceProbability(Offer offer, BigDecimal baseSalary,
                                                               BigDecimal totalPackage) {
        try {
            CandidateProfile candidate = new CandidateProfile();
            candidate.setName(offer.getCandidateName() != null ? offer.getCandidateName() : "候选人");
            candidate.setEmail(offer.getCandidateEmail());

            OfferDetail detail = new OfferDetail();
            detail.setOfferId(offer.getId());
            detail.setCandidateId(offer.getCandidateId());
            detail.setJobId(offer.getJobPositionId());
            detail.setJobTitle(offer.getPositionTitle());
            detail.setDepartment(offer.getDepartmentName());
            detail.setBaseSalary(baseSalary != null ? baseSalary.doubleValue() : null);
            detail.setTotalPackage(totalPackage != null ? totalPackage.doubleValue() : null);

            // 1. LLM 优先：调用 AI 引擎
            ApiResponse<PredictOfferVO> resp = aiEngineClient.predictOffer(
                    new OfferPredictRequest(candidate, detail));
            if (resp != null && resp.data() != null
                    && resp.data().acceptanceProbability() != null) {
                // 防御性规范化：LLM 可能返回 0-1 小数（如 0.85），统一转为 0-100 百分比
                double raw = resp.data().acceptanceProbability();
                double normalized = raw <= 1.0 ? raw * 100.0 : raw;
                int probability = (int) Math.round(
                        Math.max(0, Math.min(100, normalized)));
                String riskLevel = normalizeRiskLevel(resp.data().riskLevel());
                String recommendation = resp.data().recommendation();
                if (recommendation == null || recommendation.isBlank()) {
                    recommendation = buildSuggestion(probability, totalPackage);
                }
                log.info("Offer 接受度预测完成(LLM): offerId={}, probability={}, riskLevel={}",
                        offer.getId(), probability, riskLevel);
                return new OfferPredictionResult(probability, riskLevel, recommendation);
            }

            // 2. 兜底：本地 Agent 启发式预测
            PredictionResult prediction = offerPredictorAgent.predict(candidate, detail);
            if (prediction.getAcceptanceProbability() != null) {
                int probability = (int) Math.round(
                        Math.max(0, Math.min(100, prediction.getAcceptanceProbability())));
                log.info("Offer 接受度预测完成(Agent兜底): offerId={}, probability={}",
                        offer.getId(), probability);
                return new OfferPredictionResult(probability,
                        normalizeRiskLevel(prediction.getRiskLevel()),
                        buildSuggestion(probability, totalPackage));
            }
        } catch (Exception e) {
            log.warn("Offer 预测调用失败，回退默认概率: offerId={}, error={}",
                    offer.getId(), e.getMessage());
        }
        return new OfferPredictionResult(75, "MEDIUM", buildSuggestion(75, totalPackage));
    }

    /** 根据接受概率生成默认建议文案。 */
    private String buildSuggestion(int probability, BigDecimal totalPackage) {
        if (probability >= 80) {
            return "候选人接受概率较高，建议及时跟进确认入职时间。";
        }
        if (probability >= 65) {
            return "候选人可能正在比较多个Offer，建议强调公司成长机会和文化优势。";
        }
        return "接受概率偏低，建议考虑提升薪资包或增加签约奖金以增强吸引力。当前年薪总包: "
                + (totalPackage == null ? "-" : totalPackage) + "元";
    }

    /** 风险级别规范化：仅允许 LOW/MEDIUM/HIGH。 */
    private String normalizeRiskLevel(String level) {
        String upper = level == null ? "" : level.trim().toUpperCase();
        return switch (upper) {
            case "LOW", "MEDIUM", "HIGH" -> upper;
            default -> "MEDIUM";
        };
    }

    /** Offer 预测结果内部对象。 */
    private record OfferPredictionResult(int probability, String riskLevel, String recommendation) {
    }

    // ---- 审批工作台 ----

    /** 查询待审批的 Offer 列表。 */
    @Override
    public PageResult<OfferApprovalListVO> getPendingApprovals(Page<?> page, Map<String, Object> params) {
        // 强制查询待审批状态的 Offer
        params = new HashMap<>(params);
        params.put("status", String.valueOf(OfferEnums.OfferStatus.PENDING.getCode()));

        Page<Offer> mpPage = new Page<>(page.getCurrent(), page.getSize());
        IPage<Offer> result = offerMapper.selectPageWithFilters(mpPage, params);

        List<OfferApprovalListVO> vos = result.getRecords().stream()
                .map(offer -> {
                    OfferApprovalListVO vo = toApprovalListVO(offer);
                    ApprovalFlowConfigVO fc = flowConfigService.getByDepartment(offer.getDepartmentName());
                    vo.setCurrentLevel(detectCurrentLevel(offer.getId(), fc));
                    return vo;
                })
                .toList();
        return new PageResult<>(vos, result.getTotal(), result.getSize(), result.getCurrent(), result.getPages());
    }    @Override
    /** 分页查询审批历史记录。 */
    public PageResult<OfferApprovalListVO> getApprovalHistory(Page<?> page, Map<String, Object> params) {
        // 先从审批记录表查出已处理的审批，按时间倒序
        LambdaQueryWrapper<OfferApproval> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(OfferApproval::getStatus,
                        OfferEnums.ApprovalStatus.APPROVED.getCode(),
                        OfferEnums.ApprovalStatus.REJECTED.getCode())
                .orderByDesc(OfferApproval::getCreateTime);
        List<OfferApproval> allApprovals = approvalMapper.selectList(wrapper);

        // 按 offerId 去重取最新审批
        Map<Long, OfferApproval> latestByOfferId = allApprovals.stream()
                .collect(Collectors.toMap(
                        OfferApproval::getOfferId,
                        a -> a,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));

        // 获取对应的 Offer 列表
        List<Long> offerIds = new ArrayList<>(latestByOfferId.keySet());
        List<OfferApprovalListVO> vos = new ArrayList<>();
        if (!offerIds.isEmpty()) {
            List<Offer> offers = offerMapper.selectBatchIds(offerIds);
            Map<Long, Offer> offerMap = offers.stream()
                    .collect(Collectors.toMap(Offer::getId, o -> o));

            for (Map.Entry<Long, OfferApproval> entry : latestByOfferId.entrySet()) {
                Offer offer = offerMap.get(entry.getKey());
                if (offer != null) {
                    // 排除仍处于待审批状态的 Offer
                    if (OfferEnums.OfferStatus.PENDING.getCode() == offer.getStatus()) {
                        continue;
                    }
                    OfferApprovalListVO vo = toApprovalListVO(offer, entry.getValue());
                    // 历史记录：通过→显示最后一级，驳回→显示驳回那一级
                    if (OfferEnums.ApprovalStatus.REJECTED.getCode() == entry.getValue().getStatus()) {
                        vo.setCurrentLevel(entry.getValue().getApprovalLevel());
                    } else {
                        ApprovalFlowConfigVO fc = flowConfigService.getByDepartment(offer.getDepartmentName());
                        vo.setCurrentLevel(fc != null ? fc.getMaxLevels() : entry.getValue().getApprovalLevel());
                    }
                    vos.add(vo);
                }
            }
        }

        // 手动分页
        int total = vos.size();
        int fromIndex = (int) ((page.getCurrent() - 1) * page.getSize());
        int toIndex = Math.min(fromIndex + (int) page.getSize(), total);
        List<OfferApprovalListVO> pagedVos = fromIndex < total
                ? vos.subList(fromIndex, toIndex)
                : List.of();
        int pages = (int) Math.ceil((double) total / page.getSize());

        return new PageResult<>(pagedVos, (long) total, (int) page.getSize(), page.getCurrent(), pages);
    }

    /** 查询审批统计。 */
    @Override
    public OfferApprovalStatsVO getApprovalStats() {
        LocalDate today = DateUtils.today();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();

        // 待审批数量
        LambdaQueryWrapper<Offer> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(Offer::getStatus, OfferEnums.OfferStatus.PENDING.getCode())
                .eq(Offer::getDeleted, 0);
        long pendingCount = offerMapper.selectCount(pendingWrapper);

        // 今日通过数
        LambdaQueryWrapper<OfferApproval> todayApprovedWrapper = new LambdaQueryWrapper<>();
        todayApprovedWrapper.eq(OfferApproval::getStatus, OfferEnums.ApprovalStatus.APPROVED.getCode())
                .ge(OfferApproval::getApproveTime, todayStart)
                .lt(OfferApproval::getApproveTime, todayEnd);
        long todayApproved = approvalMapper.selectCount(todayApprovedWrapper);

        // 今日驳回数
        LambdaQueryWrapper<OfferApproval> todayRejectedWrapper = new LambdaQueryWrapper<>();
        todayRejectedWrapper.eq(OfferApproval::getStatus, OfferEnums.ApprovalStatus.REJECTED.getCode())
                .ge(OfferApproval::getApproveTime, todayStart)
                .lt(OfferApproval::getApproveTime, todayEnd);
        long todayRejected = approvalMapper.selectCount(todayRejectedWrapper);

        // 本月审批总数
        LocalDate firstOfMonth = today.withDayOfMonth(1);
        LocalDateTime monthStart = firstOfMonth.atStartOfDay();
        LambdaQueryWrapper<OfferApproval> monthWrapper = new LambdaQueryWrapper<>();
        monthWrapper.in(OfferApproval::getStatus,
                        OfferEnums.ApprovalStatus.APPROVED.getCode(),
                        OfferEnums.ApprovalStatus.REJECTED.getCode())
                .ge(OfferApproval::getCreateTime, monthStart);
        long monthlyTotal = approvalMapper.selectCount(monthWrapper);

        return new OfferApprovalStatsVO(pendingCount, todayApproved, todayRejected, monthlyTotal);
    }

    // ---- 私有辅助方法 ----

    private OfferVO toVO(Offer offer) {
        return OfferVO.builder()
                .id(offer.getId())
                .applicationId(offer.getApplicationId())
                .candidateId(offer.getCandidateId())
                .candidateName(offer.getCandidateName())
                .jobPositionId(offer.getJobPositionId())
                .offerNo(offer.getOfferNo())
                .positionTitle(offer.getPositionTitle())
                .departmentName(offer.getDepartmentName())
                .level(offer.getLevel())
                .baseSalary(getBaseSalary(offer))
                .totalPackage(calcTotalPackage(offer))
                .status(offer.getStatus())
                .expectedOnboardDate(offer.getExpectedOnboardDate())
                .validUntil(offer.getValidUntil())
                .respondTime(offer.getRespondTime())
                .declineReason(offer.getDeclineReason())
                .creatorName(resolveCreatorName(offer))
                .createTime(offer.getCreateTime())
                .updateTime(offer.getUpdateTime())
                .build();
    }

    @SuppressWarnings("unchecked")
    private BigDecimal calcTotalPackage(Offer offer) {
        if (offer.getSalaryStructure() instanceof Map<?, ?> ss) {
            Object tp = ss.get("totalPackage");
            if (tp instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
            if (tp instanceof String s) return new BigDecimal(s);
        }
        // 兜底计算
        return getBaseSalary(offer).multiply(BigDecimal.valueOf(12 + getBonusMonths(offer)))
                .add(getStockOptions(offer))
                .add(getSignOnBonus(offer));
    }

    @SuppressWarnings("unchecked")
    private BigDecimal getBaseSalary(Offer offer) {
        return getSalaryField(offer, "baseSalary", BigDecimal.ZERO);
    }

    @SuppressWarnings("unchecked")
    private int getBonusMonths(Offer offer) {
        if (offer.getSalaryStructure() instanceof Map<?, ?> ss) {
            Object bm = ss.get("bonusMonths");
            if (bm instanceof Number n) return n.intValue();
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    private BigDecimal getStockOptions(Offer offer) {
        return getSalaryField(offer, "stockOptions", BigDecimal.ZERO);
    }

    @SuppressWarnings("unchecked")
    private BigDecimal getSignOnBonus(Offer offer) {
        return getSalaryField(offer, "signOnBonus", BigDecimal.ZERO);
    }

    @SuppressWarnings("unchecked")
    private BigDecimal getSalaryField(Offer offer, String field, BigDecimal defaultValue) {
        if (offer.getSalaryStructure() instanceof Map<?, ?> ss) {
            Object val = ss.get(field);
            if (val instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
            if (val instanceof String s) return new BigDecimal(s);
        }
        return defaultValue;
    }

    private OfferApprovalListVO toApprovalListVO(Offer offer) {
        OfferApproval latestApproval = approvalMapper.selectLatestByOfferId(offer.getId());
        return toApprovalListVO(offer, latestApproval);
    }

    private OfferApprovalListVO toApprovalListVO(Offer offer, OfferApproval approval) {
        OfferEnums.OfferStatus offerStatus = OfferEnums.OfferStatus.fromCode(offer.getStatus());
        OfferEnums.ApprovalStatus approvalStatus = approval != null
                ? OfferEnums.ApprovalStatus.fromCode(approval.getStatus())
                : null;

        OfferApprovalListVO.ApprovalInfo approvalInfo = null;
        if (approval != null) {
            approvalInfo = OfferApprovalListVO.ApprovalInfo.builder()
                    .id(approval.getId())
                    .approverId(approval.getApproverId())
                    .approverName(approval.getApproverName())
                    .approverRole(approval.getApproverRole())
                    .approvalLevel(approval.getApprovalLevel())
                    .status(approval.getStatus())
                    .statusLabel(approvalStatus != null ? approvalStatus.getLabel() : "待审批")
                    .comment(approval.getComment())
                    .approveTime(approval.getApproveTime())
                    .build();
        }

        return OfferApprovalListVO.builder()
                .offerId(offer.getId())
                .offerNo(offer.getOfferNo())
                .candidateId(offer.getCandidateId())
                .candidateName(offer.getCandidateName())
                .positionTitle(offer.getPositionTitle())
                .departmentName(offer.getDepartmentName())
                .level(offer.getLevel())
                .baseSalary(getBaseSalary(offer))
                .totalPackage(calcTotalPackage(offer))
                .expectedOnboardDate(offer.getExpectedOnboardDate())
                .status(offer.getStatus())
                .statusLabel(offerStatus != null ? offerStatus.getLabel() : "未知")
                .submitTime(offer.getUpdateTime())
                .latestApproval(approvalInfo)
                .creatorName(resolveCreatorName(offer))
                .createTime(offer.getCreateTime())
                .build();
    }

    /**
     * 通过 Feign 查询真实的求职申请 ID。
     * 如果远程调用失败或申请不存在，返回 {@code null}（可被 UNIQUE KEY 视为不同值）。
     */
    private Long resolveApplicationId(Long candidateId, Long jobPositionId) {
        try {
            var response = recruitmentClient.findApplication(candidateId, jobPositionId);
            if (response != null && response.code() == 0 && response.data() != null) {
                ApplicationDTO app = response.data();
                log.info("找到关联申请: applicationId={}, candidateId={}, jobId={}",
                        app.getId(), candidateId, jobPositionId);
                return app.getId();
            }
        } catch (Exception e) {
            log.warn("查询求职申请失败 (candidateId={}, jobId={})，applicationId 设为 null: {}",
                    candidateId, jobPositionId, e.getMessage());
        }
        return null;
    }

    /**
     * 记录 Offer 确认/拒绝的活动动态到招聘服务。
     */
    private void recordOfferActivity(Offer offer, boolean accept) {
        try {
            String title = accept
                    ? offer.getCandidateName() + " 已接受 Offer"
                    : offer.getCandidateName() + " 已拒绝 Offer";
            String description = accept
                    ? "候选人 " + offer.getCandidateName() + " 已接受「" + offer.getPositionTitle() + "」的Offer，将进入入职流程。"
                    : "候选人 " + offer.getCandidateName() + " 已拒绝「" + offer.getPositionTitle() + "」的Offer。";
            recruitmentClient.recordActivity(new ActivityRecordRequest(
                    3, title, description, 0L, "system", "OFFER", offer.getId()));
            log.info("Offer activity recorded: offerId={}, accepted={}", offer.getId(), accept);
        } catch (Exception e) {
            log.warn("Failed to record offer activity feed: {}", e.getMessage());
        }
    }

    /**
     * 创建待办任务到招聘服务（通过 Feign）。
     */
    private void createTask(Offer offer, String title, String description,
                            int taskType, int priority, long daysUntilDue) {
        try {
            WorkbenchTaskRequest task = new WorkbenchTaskRequest();
            task.setUserId(0L);
            task.setTitle(title);
            task.setDescription(description);
            task.setType(taskType);
            task.setPriority(priority);
            task.setRelatedType("OFFER");
            task.setRelatedId(offer.getId());
            task.setCandidateName(offer.getCandidateName());
            if (daysUntilDue > 0) {
                task.setDueDate(DateUtils.now().plusDays(daysUntilDue));
            }
            recruitmentClient.createTask(task);
            log.info("Task created: offerId={}, title={}", offer.getId(), title);
        } catch (Exception e) {
            log.warn("Failed to create workbench task for offer: {}", e.getMessage());
        }
    }
}
