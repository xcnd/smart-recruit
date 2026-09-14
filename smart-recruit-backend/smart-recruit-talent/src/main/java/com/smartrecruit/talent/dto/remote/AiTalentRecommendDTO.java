package com.smartrecruit.talent.dto.remote;

import java.util.List;
import java.util.Map;

/**
 * AI 人才推荐请求/响应 DTO（对应 ai-engine 的 talent/recommend 能力）。
 *
 * @since 2026-04-09
 */
public final class AiTalentRecommendDTO {

    private AiTalentRecommendDTO() {
    }

    /** 职位要求。 */
    public record JobReq(
            String jobTitle,
            List<String> requiredSkills,
            Integer minYearsOfExperience,
            String educationLevel) {
    }

    /** 候选人信息。 */
    public record Candidate(
            Long candidateId,
            String name,
            List<String> skills,
            Integer yearsOfExperience,
            String educationLevel,
            String currentCompany,
            String currentPosition,
            String summary) {
    }

    /** 请求体。 */
    public record Request(JobReq job, List<Candidate> candidates) {
    }

    /** 响应条目。 */
    public record Result(
            Long candidateId,
            String candidateName,
            Double matchScore,
            String recommendation,
            Map<String, Double> matchDimensions) {
    }
}
