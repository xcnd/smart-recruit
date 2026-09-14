package com.smartrecruit.aiengine.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.smartrecruit.aiengine.domain.CandidateProfile;
import com.smartrecruit.aiengine.domain.InterviewReport;

import java.util.List;

/**
 * 全流程编排（简历 → Offer）执行结果 VO。
 *
 * @param pipelineName     流水线名称（resume-to-offer）
 * @param status           整体状态：COMPLETED / FAILED
 * @param durationMs       总耗时（毫秒）
 * @param candidate        阶段一输出的候选人画像（简历解析结果）
 * @param screening        阶段二输出的智能筛选结果
 * @param interviewEvaluation 阶段四输出的面试评估结果（未提供面试摘要时为 null）
 * @param offerPrediction  阶段五输出的 Offer 接受度预测
 * @param stages           各阶段执行明细
 * @since 2026-04-09
 */
public record PipelineResultVO(
        @JsonProperty("pipelineName") String pipelineName,
        @JsonProperty("status") String status,
        @JsonProperty("durationMs") Long durationMs,
        @JsonProperty("candidate") CandidateProfile candidate,
        @JsonProperty("screening") ScreenResultVO screening,
        @JsonProperty("interviewEvaluation") InterviewReport interviewEvaluation,
        @JsonProperty("offerPrediction") PredictOfferVO offerPrediction,
        @JsonProperty("stages") List<PipelineStageVO> stages) {
}
