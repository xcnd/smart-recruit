package com.smartrecruit.offer.controller;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.offer.dto.request.CreateContractRequest;
import com.smartrecruit.offer.dto.request.HrSignRequest;
import com.smartrecruit.offer.dto.request.VoidContractRequest;
import com.smartrecruit.offer.dto.response.ContractDetailVO;
import com.smartrecruit.offer.dto.response.ContractStatsVO;
import com.smartrecruit.offer.dto.response.ContractVO;
import com.smartrecruit.offer.service.ContractService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 合同管理控制器（后台）。
 *
 * @since 2026-04-09
 */
@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
@Slf4j
public class ContractController {

    private final ContractService contractService;

    /**
     * 分页查询合同。
     */
    @GetMapping
    public ApiResponse<PageResult<ContractVO>> pageQuery(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ApiResponse.success(contractService.pageQuery(
                page, size, keyword, status, startDate, endDate));
    }

    /**
     * 合同统计概览。
     */
    @GetMapping("/stats")
    public ApiResponse<ContractStatsVO> stats() {
        return ApiResponse.success(contractService.getStats());
    }

    /**
     * 合同详情（含正文与签署记录）。
     */
    @GetMapping("/{id}")
    public ApiResponse<ContractDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.success(contractService.getDetail(id));
    }

    /**
     * 从 Offer 创建合同。
     */
    @PostMapping
    public ApiResponse<ContractVO> create(@Valid @RequestBody CreateContractRequest request) {
        return ApiResponse.success(contractService.createFromOffer(
                request.offerId(), operator()));
    }

    /**
     * 提交审批。
     */
    @PostMapping("/{id}/submit-approval")
    public ApiResponse<Void> submitApproval(@PathVariable Long id) {
        contractService.submitApproval(id, operator());
        return ApiResponse.success();
    }

    /**
     * 审批通过。
     */
    @PostMapping("/{id}/approve")
    public ApiResponse<Void> approve(@PathVariable Long id) {
        contractService.approve(id, operator());
        return ApiResponse.success();
    }

    /**
     * 发送给候选人签署（生成签署链接）。
     */
    @PostMapping("/{id}/send")
    public ApiResponse<Void> send(@PathVariable Long id) {
        contractService.send(id, operator());
        return ApiResponse.success();
    }

    /**
     * HR 签署合同。
     */
    @PostMapping("/{id}/hr-sign")
    public ApiResponse<Void> hrSign(@PathVariable Long id,
                                    @Valid @RequestBody HrSignRequest request) {
        contractService.hrSign(id, request.signerName(), operator());
        return ApiResponse.success();
    }

    /**
     * 合同生效（已签署后点击生效，状态变为生效中）。
     */
    @PostMapping("/{id}/effective")
    public ApiResponse<Void> makeEffective(@PathVariable Long id) {
        contractService.makeEffective(id, operator());
        return ApiResponse.success();
    }

    /**
     * 合同作废（已签署/生效中的合同作废后不可恢复）。
     */
    @PostMapping("/{id}/void")
    public ApiResponse<Void> voidContract(@PathVariable Long id,
                                          @Valid @RequestBody VoidContractRequest request) {
        contractService.voidContract(id, request.reason(), operator());
        return ApiResponse.success();
    }

    /**
     * 删除合同（仅草稿/已拒绝）。
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        contractService.delete(id, operator());
        return ApiResponse.success();
    }

    /** 当前操作人：优先取 X-Username 请求头（网关注入），否则取认证主体。 */
    private String operator() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getCredentials() != null
                && !auth.getCredentials().toString().isEmpty()) {
            return auth.getCredentials().toString();
        }
        return "0";
    }
}
