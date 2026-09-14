package com.smartrecruit.aiengine.agents;

import com.smartrecruit.aiengine.domain.CandidateProfile;
import com.smartrecruit.aiengine.domain.JobRequirement;
import com.smartrecruit.aiengine.domain.ScreeningResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 智能筛选 Agent —— 模拟 AI 驱动的简历筛选。
 *
 * <p>从 5 个维度对候选人与职位要求进行评分：
 * <ul>
 *   <li>技能匹配 (40%)</li>
 *   <li>经验匹配 (25%)</li>
 *   <li>学历匹配 (15%)</li>
 *   <li>稳定性 (10%)</li>
 *   <li>综合匹配 (10%)</li>
 * </ul>
 *
 * @author xdh
 * @since 2026-04-26
 */
@Component
@Slf4j
public class SmartScreenerAgent {

    /**
     * 根据职位要求对候选人画像进行筛选。
     *
     * @param candidate   解析后的候选人画像
     * @param requirement 职位要求规格
     * @return 包含 5 维度评分的筛选结果
     */
    public ScreeningResult screen(CandidateProfile candidate, JobRequirement requirement) {
        log.info("SmartScreenerAgent: screening candidate={} against job={}",
                candidate.getName(), requirement.getJobTitle());
        simulateLatency(80, 350);

        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        // 计算技能重叠度
        double skillMatch = 0.0;
        if (candidate.getSkills() != null && requirement.getRequiredSkills() != null
                && !requirement.getRequiredSkills().isEmpty()) {
            Set<String> candidateSkills = new LinkedHashSet<>(candidate.getSkills());
            Set<String> required = new LinkedHashSet<>(requirement.getRequiredSkills());
            long overlapCount = candidateSkills.stream().filter(required::contains).count();
            skillMatch = Math.min(100.0, (double) overlapCount / required.size() * 100.0);
        }
        skillMatch = Math.max(40.0, Math.min(98.0, skillMatch + rnd.nextDouble(-5.0, 15.0)));

        // 基于年限的经验匹配
        double experienceMatch = 70.0;
        if (requirement.getMinYearsOfExperience() != null && candidate.getYearsOfExperience() != null) {
            int diff = candidate.getYearsOfExperience() - requirement.getMinYearsOfExperience();
            experienceMatch = Math.max(40.0, Math.min(98.0, 60.0 + diff * 8.0 + rnd.nextDouble(-8.0, 8.0)));
        }

        // 学历匹配
        double educationMatch = rnd.nextDouble(50.0, 95.0);

        // 稳定性因素
        double stability = rnd.nextDouble(55.0, 90.0);

        // 综合匹配
        double overallFit = rnd.nextDouble(50.0, 95.0);

        Map<String, Double> dimensionScores = new LinkedHashMap<>();
        dimensionScores.put("skillMatch", Math.round(skillMatch * 10.0) / 10.0);
        dimensionScores.put("experienceMatch", Math.round(experienceMatch * 10.0) / 10.0);
        dimensionScores.put("educationMatch", Math.round(educationMatch * 10.0) / 10.0);
        dimensionScores.put("stability", Math.round(stability * 10.0) / 10.0);
        dimensionScores.put("overallFit", Math.round(overallFit * 10.0) / 10.0);

        // 加权综合评分
        double overallScore = skillMatch * 0.40
                + experienceMatch * 0.25
                + educationMatch * 0.15
                + stability * 0.10
                + overallFit * 0.10;
        overallScore = Math.round(overallScore * 10.0) / 10.0;

        boolean passed = overallScore >= 70.0;

        Map<String, String> highlights = new LinkedHashMap<>();
        highlights.put("topSkill", candidate.getSkills() != null && !candidate.getSkills().isEmpty()
                ? candidate.getSkills().get(0) : "N/A");
        highlights.put("experienceYears", String.valueOf(candidate.getYearsOfExperience()));

        ScreeningResult result = ScreeningResult.builder()
                .candidateId(null)
                .jobId(requirement.getJobId())
                .overallScore(overallScore)
                .dimensionScores(dimensionScores)
                .passed(passed)
                .recommendation(passed ? "PROCEED_TO_INTERVIEW" : "ARCHIVE")
                .summary(passed
                        ? "Candidate meets screening threshold with overall score " + overallScore
                        : "Candidate does not meet the minimum screening threshold of 70.0")
                .highlights(highlights)
                .build();

        log.info("SmartScreenerAgent: screening complete, overallScore={}, passed={}",
                overallScore, passed);
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
