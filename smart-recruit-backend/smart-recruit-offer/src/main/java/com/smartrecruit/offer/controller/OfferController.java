package com.smartrecruit.offer.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.offer.dto.request.CreateOfferRequest;
import com.smartrecruit.offer.dto.request.OfferApprovalRequest;
import com.smartrecruit.offer.dto.request.OfferConfirmRequest;
import com.smartrecruit.offer.dto.request.UpdateOfferRequest;
import com.smartrecruit.offer.dto.response.OfferApprovalListVO;
import com.smartrecruit.offer.dto.response.OfferDetailVO;
import com.smartrecruit.offer.dto.response.OfferOptionVO;
import com.smartrecruit.offer.dto.response.OfferPredictionVO;
import com.smartrecruit.offer.dto.response.OfferVO;
import com.smartrecruit.offer.dto.response.OfferApprovalStatsVO;
import com.smartrecruit.offer.service.OfferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Offer 管理 REST 控制器。
 *
 * <p>接口：列表、详情、创建、更新、提交审批、审批、发送、预测。</p>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/offers")
@RequiredArgsConstructor
@Slf4j
public class OfferController {

    private final OfferService offerService;

    /**
     * 分页查询 Offer 列表，支持候选人、状态、部门与时间范围筛选。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('offer:view')")
    public ApiResponse<PageResult<OfferVO>> list(
            @RequestParam(required = false) Long candidateId,
            @RequestParam(required = false) String candidateName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String departmentName,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        Map<String, Object> params = new HashMap<>();
        if (candidateId != null) params.put("candidateId", candidateId);
        if (candidateName != null && !candidateName.isBlank()) params.put("candidateName", candidateName);
        if (status != null) params.put("status", status);
        if (departmentName != null) params.put("departmentName", departmentName);
        if (startDate != null && !startDate.isBlank()) params.put("startDate", startDate);
        if (endDate != null && !endDate.isBlank()) params.put("endDate", endDate);

        Page<?> pageQuery = new Page<>(page, size);
        PageResult<OfferVO> result = offerService.pageQuery(pageQuery, params);
        return ApiResponse.success(result);
    }

    /**
     * 查询可创建合同的 Offer 下拉选项（支持按编号/候选人/职位关键字搜索）。
     */
    @GetMapping("/options")
    public ApiResponse<List<OfferOptionVO>> contractOptions(
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(offerService.listContractOptions(keyword));
    }

    /**
     * 分页查询待审批的 Offer 列表。
     */
    @GetMapping("/approvals/pending")
    @PreAuthorize("hasAuthority('offer:approve')")
    public ApiResponse<PageResult<OfferApprovalListVO>> pendingApprovals(
            @RequestParam(required = false) String candidateName,
            @RequestParam(required = false) String departmentName,
            @RequestParam(required = false) String offerNo,
            @RequestParam(required = false) String expectedOnboardDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        Map<String, Object> params = new HashMap<>();
        if (candidateName != null && !candidateName.isBlank()) params.put("candidateName", candidateName);
        if (departmentName != null) params.put("departmentName", departmentName);
        if (offerNo != null && !offerNo.isBlank()) params.put("offerNo", offerNo);
        if (expectedOnboardDate != null && !expectedOnboardDate.isBlank()) params.put("expectedOnboardDate", expectedOnboardDate);

        Page<?> pageQuery = new Page<>(page, size);
        PageResult<OfferApprovalListVO> result = offerService.getPendingApprovals(pageQuery, params);
        return ApiResponse.success(result);
    }

    /**
     * 分页查询 Offer 审批历史。
     */
    @GetMapping("/approvals/history")
    @PreAuthorize("hasAuthority('offer:approve')")
    public ApiResponse<PageResult<OfferApprovalListVO>> approvalHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<?> pageQuery = new Page<>(page, size);
        PageResult<OfferApprovalListVO> result = offerService.getApprovalHistory(pageQuery, Map.of());
        return ApiResponse.success(result);
    }

    /**
     * 查询 Offer 审批统计（各状态数量）。
     */
    @GetMapping("/approvals/stats")
    @PreAuthorize("hasAuthority('offer:approve')")
    public ApiResponse<OfferApprovalStatsVO> approvalStats() {
        OfferApprovalStatsVO stats = offerService.getApprovalStats();
        return ApiResponse.success(stats);
    }

    /**
     * 查询 Offer 详情。
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('offer:view')")
    public ApiResponse<OfferDetailVO> detail(@PathVariable Long id) {
        OfferDetailVO vo = offerService.getById(id);
        return ApiResponse.success(vo);
    }

    /**
     * 创建 Offer（草稿状态）。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('offer:create')")
    public ApiResponse<OfferVO> create(@Valid @RequestBody CreateOfferRequest request) {
        log.info("Creating offer: candidateId={}, jobTitle={}", request.getCandidateId(), request.getJobTitle());
        OfferVO vo = offerService.create(request);
        return ApiResponse.success(vo);
    }

    /**
     * 更新 Offer 信息。
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('offer:edit')")
    public ApiResponse<OfferVO> update(@PathVariable Long id,
                                        @Valid @RequestBody UpdateOfferRequest request) {
        log.info("Updating offer: id={}", id);
        OfferVO vo = offerService.update(id, request);
        return ApiResponse.success(vo);
    }

    /**
     * 提交 Offer 进入审批流程。
     */
    @PostMapping("/{id}/submit-approval")
    @PreAuthorize("hasAuthority('offer:approve')")
    public ApiResponse<Void> submitApproval(@PathVariable Long id) {
        log.info("Submitting offer for approval: id={}", id);
        offerService.submitApproval(id);
        return ApiResponse.success();
    }

    /**
     * 处理 Offer 审批（通过/驳回）。
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('offer:approve')")
    public ApiResponse<Void> approve(@PathVariable Long id,
                                      @Valid @RequestBody OfferApprovalRequest request) {
        log.info("Processing offer approval: id={}, status={}", id, request.getStatus());
        offerService.approve(id, request);
        return ApiResponse.success();
    }

    /**
     * 发送 Offer 给候选人（状态流转为已发送）。
     */
    @PostMapping("/{id}/send")
    @PreAuthorize("hasAuthority('offer:send')")
    public ApiResponse<Void> send(@PathVariable Long id) {
        log.info("Sending offer: id={}", id);
        offerService.send(id);
        return ApiResponse.success();
    }

    /**
     * 重新发送 Offer 通知邮件。
     */
    @PostMapping("/{id}/send-email")
    @PreAuthorize("hasAuthority('offer:send')")
    public ApiResponse<Void> sendEmail(@PathVariable Long id) {
        log.info("Resending offer email: id={}", id);
        offerService.sendEmail(id);
        return ApiResponse.success();
    }

    /**
     * 查询 Offer 的 AI 预测结果（接受概率等）。
     */
    @GetMapping("/{id}/prediction")
    @PreAuthorize("hasAuthority('offer:view')")
    public ApiResponse<OfferPredictionVO> prediction(@PathVariable Long id) {
        OfferPredictionVO vo = offerService.getPrediction(id);
        return ApiResponse.success(vo);
    }

    /**
     * 删除 Offer 记录。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('offer:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        log.info("Deleting offer: id={}", id);
        offerService.delete(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/manual-confirm")
    @PreAuthorize("hasAuthority('offer:edit')")
    public ApiResponse<Void> manualConfirm(@PathVariable Long id,
                                            @RequestBody OfferConfirmRequest request) {
        boolean accept = "accept".equalsIgnoreCase(request.getAction());
        log.info("Manual confirming offer: id={}, accepted={}, reason={}", id, accept, request.getDeclineReason());
        offerService.manualConfirm(id, accept, request.getDeclineReason());
        return ApiResponse.success();
    }
}
