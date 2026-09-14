package com.smartrecruit.recruitment.controller;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.recruitment.dto.response.JobStatsVO;
import com.smartrecruit.recruitment.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 职位统计独立控制器。
 *
 * <p>使用独立的基础路径避免与 {@code /api/v1/jobs/{id}} 路径变量冲突。</p>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/job-statistics")
@RequiredArgsConstructor
@Slf4j
public class JobStatsController {

    private final JobService jobService;

    /**
     * 查询职位整体统计（总数、发布中、草稿等）。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('job:view')")
    public ApiResponse<JobStatsVO> stats() {
        return ApiResponse.success(jobService.getStats());
    }
}
