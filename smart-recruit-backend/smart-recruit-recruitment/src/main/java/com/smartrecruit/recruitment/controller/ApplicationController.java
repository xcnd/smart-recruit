package com.smartrecruit.recruitment.controller;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.recruitment.dto.response.ApplicationDTO;
import com.smartrecruit.recruitment.dto.request.CreateApplicationRequest;
import com.smartrecruit.recruitment.entity.Application;
import com.smartrecruit.recruitment.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 求职申请管理的 REST 控制器。
 *
 * <p>提供申请查询和创建接口，供内部模块（Offer、Interview）通过 Feign 调用。</p>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@Slf4j
public class ApplicationController {

    private final ApplicationService applicationService;

    /**
     * 根据候选人和职位查询申请记录。
     */
    @GetMapping
    public ApiResponse<ApplicationDTO> getApplication(
            @RequestParam Long candidateId,
            @RequestParam Long jobId) {
        Application app = applicationService.findByCandidateAndJob(candidateId, jobId);
        if (app == null) {
            return ApiResponse.success(null);
        }
        return ApiResponse.success(ApplicationDTO.builder()
                .id(app.getId())
                .candidateId(app.getCandidateId())
                .jobId(app.getJobId())
                .stage(app.getStage())
                .applyAt(app.getApplyAt())
                .build());
    }

    /**
     * 创建求职申请记录（幂等：已存在则返回已有记录）。
     */
    @PostMapping
    public ApiResponse<ApplicationDTO> createApplication(@RequestBody CreateApplicationRequest request) {
        Long candidateId = request.getCandidateId();
        Long jobId = request.getJobId();
        if (candidateId == null || jobId == null) {
            return ApiResponse.error(400, "candidateId 和 jobId 不能为空");
        }
        Application app = applicationService.findOrCreate(candidateId, jobId);
        return ApiResponse.success(ApplicationDTO.builder()
                .id(app.getId())
                .candidateId(app.getCandidateId())
                .jobId(app.getJobId())
                .stage(app.getStage())
                .applyAt(app.getApplyAt())
                .build());
    }
}
