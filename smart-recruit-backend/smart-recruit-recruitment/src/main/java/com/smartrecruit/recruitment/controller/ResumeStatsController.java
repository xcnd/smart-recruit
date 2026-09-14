package com.smartrecruit.recruitment.controller;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.recruitment.dto.response.ResumeStatsVO;
import com.smartrecruit.recruitment.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 简历统计独立控制器。
 *
 * <p>使用独立的基础路径避免与 {@code /api/v1/resumes/{id}} 路径变量冲突。</p>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/resume-statistics")
@RequiredArgsConstructor
@Slf4j
public class ResumeStatsController {

    private final ResumeService resumeService;

    /**
     * 查询简历整体统计（总量、解析状态分布等）。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('resume:view')")
    public ApiResponse<ResumeStatsVO> stats() {
        return ApiResponse.success(resumeService.getStats());
    }
}
