package com.smartrecruit.interview.controller;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.interview.dto.request.SubmitAnswersRequest;
import com.smartrecruit.interview.dto.response.AssessmentPageVO;
import com.smartrecruit.interview.dto.response.AssessResultVO;
import com.smartrecruit.interview.service.PublicAssessmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 公开测评控制器 — 候选人通过邮件链接访问，无需登录。
 *
 * <p>所有端点不需要 JWT 认证，通过查询参数中的测评 token 进行身份和权限验证。</p>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/public/assessment")
@RequiredArgsConstructor
@Slf4j
public class PublicAssessmentController {

    private final PublicAssessmentService publicAssessmentService;

    /**
     * 通过测评 token 获取测评页面数据（题目 + 候选人信息）。
     */
    @GetMapping
    public ApiResponse<AssessmentPageVO> getAssessment(@RequestParam("token") String token) {
        log.info("公开接口 - 获取测评页面: token前8位={}", token.length() > 8 ? token.substring(0, 8) + "..." : token);
        AssessmentPageVO vo = publicAssessmentService.getAssessmentByToken(token);
        return ApiResponse.success(vo);
    }

    /**
     * 提交测评答案。
     */
    @PostMapping("/submit")
    public ApiResponse<AssessResultVO> submit(@RequestBody SubmitAnswersRequest request) {
        log.info("公开接口 - 提交测评答案");
        AssessResultVO result = publicAssessmentService.submitAssessment(request);
        return ApiResponse.success(result);
    }
}
