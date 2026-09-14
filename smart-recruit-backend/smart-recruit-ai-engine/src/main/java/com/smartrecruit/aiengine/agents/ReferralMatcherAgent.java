package com.smartrecruit.aiengine.agents;

import com.smartrecruit.aiengine.domain.CandidateProfile;
import com.smartrecruit.aiengine.domain.JobMatchTarget;
import com.smartrecruit.aiengine.domain.ReferralMatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 内推匹配智能体——模拟AI驱动的内推职位匹配。
 *
 * <p>根据可用职位分析候选人画像，寻找最佳匹配，
 * 生成带有评分和建议的排名列表。
 *
 * @author xdh
 * @since 2026-04-26
 */
@Component
@Slf4j
public class ReferralMatcherAgent {

    /**
     * 将候选人画像与职位列表进行匹配。
     *
     * @param candidate    被推荐候选人的画像
     * @param jobPositions 可用职位列表
     * @return 最佳内推匹配结果
     */
    public ReferralMatch match(CandidateProfile candidate, List<JobMatchTarget> jobPositions) {
        log.info("ReferralMatcherAgent: matching candidate={} against {} jobs",
                candidate.getName(), jobPositions.size());
        simulateLatency(60, 250);

        if (jobPositions == null || jobPositions.isEmpty()) {
            log.warn("ReferralMatcherAgent: no job positions to match against");
            return ReferralMatch.builder()
                    .candidateId(null)
                    .matchScore(0.0)
                    .recommendation("No available positions to match")
                    .build();
        }

        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        // 通过选择最佳匹配（最高随机分数）来模拟匹配
        JobMatchTarget bestJob = null;
        double bestScore = 0.0;
        Map<String, Double> bestDimensions = new LinkedHashMap<>();

        for (JobMatchTarget job : jobPositions) {
            double skillMatch = 0.0;
            if (candidate.getSkills() != null && job.getRequiredSkills() != null
                    && !job.getRequiredSkills().isEmpty()) {
                Set<String> candidateSkills = new LinkedHashSet<>(candidate.getSkills());
                Set<String> required = new LinkedHashSet<>(job.getRequiredSkills());
                long overlap = candidateSkills.stream().filter(required::contains).count();
                skillMatch = Math.min(100.0, (double) overlap / required.size() * 100.0);
            }
            double score = skillMatch * 0.45 + rnd.nextDouble(50.0, 80.0) * 0.55;

            if (score > bestScore) {
                bestScore = score;
                bestJob = job;
                bestDimensions = new LinkedHashMap<>();
                bestDimensions.put("skillMatch", Math.round(skillMatch * 10.0) / 10.0);
                bestDimensions.put("experienceMatch", Math.round(rnd.nextDouble(50.0, 90.0) * 10.0) / 10.0);
                bestDimensions.put("locationMatch", Math.round(rnd.nextDouble(50.0, 95.0) * 10.0) / 10.0);
                bestDimensions.put("salaryMatch", Math.round(rnd.nextDouble(45.0, 92.0) * 10.0) / 10.0);
                bestDimensions.put("cultureMatch", Math.round(rnd.nextDouble(55.0, 95.0) * 10.0) / 10.0);
            }
        }

        bestScore = Math.round(bestScore * 10.0) / 10.0;

        String recommendation;
        String suggestedApproach;
        if (bestScore >= 80.0) {
            recommendation = "STRONG_MATCH — Highly recommend reaching out";
            suggestedApproach = "Personal referral message highlighting company culture and role fit";
        } else if (bestScore >= 65.0) {
            recommendation = "GOOD_MATCH — Worth pursuing";
            suggestedApproach = "Emphasize growth opportunities and team environment";
        } else {
            recommendation = "MODERATE_MATCH — Consider other positions or skill development";
            suggestedApproach = "Share job description and ask about transferable skills";
        }

        ReferralMatch result = ReferralMatch.builder()
                .candidateId(null)
                .matchedJobId(bestJob != null ? bestJob.getJobId() : null)
                .matchedJobTitle(bestJob != null ? bestJob.getTitle() : "N/A")
                .matchScore(bestScore)
                .matchDimensions(bestDimensions)
                .recommendation(recommendation)
                .suggestedApproach(suggestedApproach)
                .build();

        log.info("ReferralMatcherAgent: matching complete, bestJob={}, score={}",
                result.getMatchedJobTitle(), bestScore);
        return result;
    }

    private void simulateLatency(int minMs, int maxMs) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(minMs, maxMs));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
