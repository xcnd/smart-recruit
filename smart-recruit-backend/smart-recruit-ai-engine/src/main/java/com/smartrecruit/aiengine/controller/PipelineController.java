package com.smartrecruit.aiengine.controller;

import com.smartrecruit.aiengine.dto.request.RunPipelineRequest;
import com.smartrecruit.aiengine.dto.response.PipelineResultVO;
import com.smartrecruit.aiengine.pipeline.ResumeToOfferPipeline;
import com.smartrecruit.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 全流程编排控制器。
 *
 * <p>提供「简历 → Offer」全流程编排入口，串联简历解析、智能筛选、
 * 人工审核、面试评估、Offer 预测五个阶段，每个阶段都会记录真实
 * Agent 任务与事件，运行结果同步呈现在智能体监控页。</p>
 *
 * @since 2026-04-09
 */
@RestController
@RequestMapping("/api/v1/agents/pipeline")
@RequiredArgsConstructor
@Slf4j
public class PipelineController {

    private final ResumeToOfferPipeline pipeline;

    /**
     * 运行全流程编排（简历 → Offer）。
     *
     * @param request 候选人、简历文本、职位要求与 Offer 信息
     * @return 各阶段执行明细与关键输出
     */
    @PostMapping("/run")
    @PreAuthorize("hasAnyAuthority('agent:manage', 'ROLE_300001')")
    public ApiResponse<PipelineResultVO> run(@Valid @RequestBody RunPipelineRequest request) {
        log.info("POST /api/v1/agents/pipeline/run: candidate={}, job={}",
                request.candidateName(), request.jobTitle());
        PipelineResultVO result = pipeline.execute(request);
        return ApiResponse.success(result);
    }
}
