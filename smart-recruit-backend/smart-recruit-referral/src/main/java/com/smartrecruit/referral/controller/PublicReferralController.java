package com.smartrecruit.referral.controller;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.common.util.UserContextUtil;
import com.smartrecruit.referral.dto.request.PublicReferralRequest;
import com.smartrecruit.referral.dto.response.ProgramJobDetailVO;
import com.smartrecruit.referral.dto.response.PublicProgramVO;
import com.smartrecruit.referral.dto.response.PublicProgramDetailVO;
import com.smartrecruit.referral.dto.response.UploadResultVO;
import com.smartrecruit.referral.service.ReferralService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 公开内推控制器。
 *
 * <p>此类端点不需要身份认证，供外部访客访问内推计划详情和提交推荐。
 * 网关已在 AuthFilter 中将 {@code /api/v1/referrals/public/**} 加入白名单。</p>
 *
 * @since 2026-04-01
 */
@RestController
@RequestMapping("/api/v1/referrals/public")
@RequiredArgsConstructor
@Slf4j
public class PublicReferralController {

    private final ReferralService referralService;

    /**
     * 获取所有已启用内推计划及其职位的公开列表（无需认证，供候选人浏览）。
     */
    @GetMapping("/programs")
    public ApiResponse<List<PublicProgramVO>> listPublicPrograms() {
        return ApiResponse.success(referralService.getPublicPrograms());
    }

    /**
     * 通过分享令牌获取公开落地页数据。
     */
    @GetMapping("/programs/{token}")
    public ApiResponse<PublicProgramDetailVO> getProgramByToken(@PathVariable String token) {
        return ApiResponse.success(referralService.getPublicProgramByToken(token));
    }

    /**
     * 公开获取职位详情（含计划上下文和完整职位数据）。
     */
    @GetMapping("/positions/{programJobId}/detail")
    public ApiResponse<ProgramJobDetailVO> getJobDetail(@PathVariable Long programJobId) {
        log.info("GET /api/v1/referrals/public/positions/{}/detail", programJobId);
        return ApiResponse.success(referralService.getProgramJobDetail(programJobId));
    }

    /**
     * 上传候选人简历（PDF/DOCX/DOC），返回 RustFS 文件路径。
     */
    @PostMapping("/upload-resume")
    public ApiResponse<UploadResultVO> uploadResume(@RequestParam("file") MultipartFile file) {
        String fileUrl = referralService.uploadResume(file);
        return ApiResponse.success(new UploadResultVO(fileUrl));
    }

    /**
     * 匿名访客提交内推推荐。
     */
    @PostMapping("/submit")
    public ApiResponse<?> submitReferral(@Valid @RequestBody PublicReferralRequest request) {
        return ApiResponse.success(referralService.createPublicRecord(request));
    }

    /**
     * 检查当前登录用户是否已投递过指定计划职位。
     * 未登录时返回 false。
     */
    @GetMapping("/check-applied")
    public ApiResponse<Boolean> checkApplied(@RequestParam Long programJobId) {
        Long candidateId = UserContextUtil.getCurrentUserId();
        if (candidateId == null) {
            return ApiResponse.success(false);
        }
        boolean applied = referralService.hasApplied(candidateId, programJobId);
        return ApiResponse.success(applied);
    }
}
