package com.smartrecruit.aiengine.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 全流程编排单阶段执行结果 VO。
 *
 * @param stage      阶段标识（resume-parse / smart-screen / human-review / interview-evaluate / offer-predict）
 * @param stageName  阶段中文名（简历解析 / 智能筛选 / 人工审核 / 面试评估 / Offer 预测）
 * @param agentId    执行智能体 ID
 * @param agentName  执行智能体展示名
 * @param status     阶段状态：COMPLETED / SKIPPED / FAILED
 * @param durationMs 阶段耗时（毫秒）
 * @param message    结果摘要（评分/结论/跳过原因等）
 * @since 2026-04-09
 */
public record PipelineStageVO(
        @JsonProperty("stage") String stage,
        @JsonProperty("stageName") String stageName,
        @JsonProperty("agentId") String agentId,
        @JsonProperty("agentName") String agentName,
        @JsonProperty("status") String status,
        @JsonProperty("durationMs") Long durationMs,
        @JsonProperty("message") String message) {
}
