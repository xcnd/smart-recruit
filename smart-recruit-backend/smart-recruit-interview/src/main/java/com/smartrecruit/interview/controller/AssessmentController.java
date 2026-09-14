package com.smartrecruit.interview.controller;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.interview.dto.request.AssessmentRequest;
import com.smartrecruit.interview.dto.response.AssessmentResponse;
import com.smartrecruit.interview.service.AssessmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * AI 评估报告 REST 控制器。
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/interviews")
@RequiredArgsConstructor
@Slf4j
public class AssessmentController {

    private final AssessmentService assessmentService;

    /**
     * 获取面试的 AI 评估报告。
     */
    @GetMapping("/{id}/assessment")
    @PreAuthorize("hasAuthority('interview:view')")
    public ApiResponse<AssessmentResponse> get(@PathVariable Long id) {
        log.info("查询 AI 评估报告: interviewId={}", id);
        AssessmentResponse response = assessmentService.getByInterviewId(id);
        return ApiResponse.success(response);
    }

    /**
     * 创建或更新 AI 评估报告。
     */
    @PostMapping("/{id}/assessment")
    @PreAuthorize("hasAuthority('interview:edit')")
    public ApiResponse<AssessmentResponse> save(@PathVariable Long id,
                                                 @Valid @RequestBody AssessmentRequest request) {
        log.info("保存 AI 评估报告: interviewId={}, candidateId={}", id, request.getCandidateId());
        AssessmentResponse response = assessmentService.save(id, request);
        return ApiResponse.success(response);
    }
}
