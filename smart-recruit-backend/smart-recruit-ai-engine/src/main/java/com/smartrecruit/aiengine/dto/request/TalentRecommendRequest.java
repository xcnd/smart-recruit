package com.smartrecruit.aiengine.dto.request;

import com.smartrecruit.aiengine.domain.JobRequirement;

import java.util.List;

/**
 * AI 人才推荐请求。
 *
 * @param job        目标职位要求
 * @param candidates 待匹配的候选人列表
 * @since 2026-04-09
 */
public record TalentRecommendRequest(
        JobRequirement job,
        List<TalentRecommendCandidate> candidates) {
}
