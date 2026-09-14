package com.smartrecruit.offer.controller;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.offer.dto.request.PublicSignRequest;
import com.smartrecruit.offer.dto.response.ContractDetailVO;
import com.smartrecruit.offer.service.ContractService;
import com.smartrecruit.common.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 合同候选人公开接口（无需登录）。
 *
 * <p>候选人通过邮件/链接中的签署 Token 访问，可预览合同并确认签署或拒绝。</p>
 *
 * @since 2026-04-09
 */
@RestController
@RequestMapping("/api/v1/public/contracts")
@RequiredArgsConstructor
@Slf4j
public class PublicContractController {

    private final ContractService contractService;

    /**
     * 通过签署 Token 获取合同详情。
     */
    @GetMapping("/{token}")
    public ApiResponse<ContractDetailVO> getByToken(@PathVariable String token) {
        return ApiResponse.success(contractService.getBySignToken(token));
    }

    /**
     * 候选人确认签署或拒绝。
     */
    @PostMapping("/{token}/sign")
    public ApiResponse<Void> sign(@PathVariable String token,
                                  @Valid @RequestBody PublicSignRequest request,
                                  HttpServletRequest httpRequest) {
        contractService.signByCandidate(token, request.name(), request.accept(),
                request.remark(), request.signature(), request.idCard(), request.phone(),
                request.address(), SecurityUtil.getClientIpAddress(httpRequest));
        return ApiResponse.success();
    }
}
