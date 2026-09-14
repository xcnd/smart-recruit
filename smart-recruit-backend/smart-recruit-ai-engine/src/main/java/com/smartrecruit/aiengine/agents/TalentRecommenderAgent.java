package com.smartrecruit.aiengine.agents;

import com.smartrecruit.aiengine.domain.CandidateProfile;
import com.smartrecruit.aiengine.domain.JobRequirement;
import com.smartrecruit.aiengine.domain.TalentMatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 人才推荐智能体——为一批候选人匹配目标职位并排序。
 *
 * <p>五维匹配：技能匹配(30%)、岗位角色匹配(30%)、经验匹配(20%)、学历匹配(10%)、语义匹配(10%)。
 * 「岗位角色匹配」用于区分技术岗与产品/运营岗，避免纯技术背景候选人在产品岗被误推荐。</p>
 * 由 {@code AgentCapabilityService.recommendTalent} 的 LLM 优先策略包裹。</p>
 *
 * @since 2026-04-09
 */
@Component
@Slf4j
public class TalentRecommenderAgent {

    /**
     * 将候选人列表与目标职位进行匹配，返回按匹配分降序排列的结果。
     */
    public List<TalentMatch> recommend(JobRequirement job, List<CandidateProfile> candidates) {
        if (job == null || candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        List<TalentMatch> results = new ArrayList<>();
        for (CandidateProfile candidate : candidates) {
            results.add(score(job, candidate));
        }
        results.sort(Comparator.comparing(TalentMatch::getMatchScore,
                Comparator.nullsFirst(Double::compareTo)).reversed());
        log.info("TalentRecommenderAgent: matched {} candidates for job={}",
                results.size(), job.getJobTitle());
        return results;
    }

    /** 计算单个候选人的四维匹配。 */
    private TalentMatch score(JobRequirement job, CandidateProfile candidate) {
        Set<String> required = new LinkedHashSet<>(job.getRequiredSkills() == null
                ? List.of() : job.getRequiredSkills());
        Set<String> skills = new LinkedHashSet<>(candidate.getSkills() == null
                ? List.of() : candidate.getSkills());

        // 技能匹配：命中率
        double skillMatch = 0.0;
        if (!required.isEmpty()) {
            long overlap = skills.stream().filter(required::contains).count();
            skillMatch = Math.min(100.0, overlap * 100.0 / required.size());
        }

        // 岗位角色匹配：技术岗 vs 产品/运营岗
        double roleMatch = roleScore(job.getJobTitle(), candidate);

        // 经验匹配：相对最低年限
        double experienceMatch = 60.0;
        if (job.getMinYearsOfExperience() != null && candidate.getYearsOfExperience() != null) {
            int diff = candidate.getYearsOfExperience() - job.getMinYearsOfExperience();
            experienceMatch = Math.max(35.0, Math.min(98.0, 65.0 + diff * 8.0));
        }

        // 学历匹配
        double educationMatch = educationScore(
                job.getEducationLevel(), candidate.getEducationLevel());

        // 语义匹配：职位技能关键词在候选人技能/简介中的覆盖率
        String candidateText = String.join(",", skills)
                + (candidate.getCurrentPosition() == null ? "" : "," + candidate.getCurrentPosition())
                + (candidate.getSummary() == null ? "" : "," + candidate.getSummary());
        double semanticMatch = 55.0;
        if (!required.isEmpty()) {
            long hit = required.stream()
                    .filter(s -> candidateText.toLowerCase().contains(s.toLowerCase()))
                    .count();
            semanticMatch = Math.min(95.0, 50.0 + hit * 100.0 / required.size());
        }

        double overall = Math.round(
                (skillMatch * 0.30 + roleMatch * 0.30
                        + experienceMatch * 0.20 + educationMatch * 0.10
                        + semanticMatch * 0.10) * 10.0) / 10.0;

        Map<String, Double> dimensions = new LinkedHashMap<>();
        dimensions.put("skillMatch", Math.round(skillMatch * 10.0) / 10.0);
        dimensions.put("roleMatch", Math.round(roleMatch * 10.0) / 10.0);
        dimensions.put("experienceMatch", Math.round(experienceMatch * 10.0) / 10.0);
        dimensions.put("educationMatch", Math.round(educationMatch * 10.0) / 10.0);
        dimensions.put("semanticMatch", Math.round(semanticMatch * 10.0) / 10.0);

        String recommendation;
        if (roleMatch < 50) {
            recommendation = "岗位角色不匹配（技术/产品方向不符），不建议推荐";
        } else if (overall >= 80) {
            recommendation = "高度匹配，建议主动联系并推进面试";
        } else if (overall >= 65) {
            recommendation = "较匹配，建议联系并重点考察";
        } else {
            recommendation = "匹配一般，建议纳入观察池";
        }

        return TalentMatch.builder()
                .candidateId(candidate.getCandidateId())
                .candidateName(candidate.getName())
                .matchScore(overall)
                .recommendation(recommendation)
                .matchDimensions(dimensions)
                .build();
    }

    /**
     * 岗位角色匹配：根据职位名称与候选人岗位/技能判断技术岗 vs 产品/运营岗。
     * 角色一致返回高分，明显反向返回低分。
     */
    private double roleScore(String jobTitle, CandidateProfile candidate) {
        String job = jobTitle == null ? "" : jobTitle.toLowerCase();
        String candidatePos = candidate.getCurrentPosition() == null
                ? "" : candidate.getCurrentPosition().toLowerCase();
        String skills = String.join(",", candidate.getSkills() == null
                ? List.of() : candidate.getSkills()).toLowerCase();
        String text = candidatePos + "," + skills;

        boolean jobIsProduct = containsAny(job,
                "产品经理", "产品", "pm", "product", "运营", "市场", "销售", "设计", "hr");
        boolean jobIsTech = containsAny(job,
                "开发", "工程师", "后端", "前端", "java", "算法", "测试", "运维",
                "数据", "架构", "dev", "engineer", "saas 技术");

        if (jobIsProduct && !jobIsTech) {
            // 产品/运营类岗位：要求候选人具备产品/业务方向背景
            boolean candIsProduct = containsAny(text,
                    "产品经理", "产品", "pm", "产品运营", "用户研究", "需求分析", "prd");
            boolean candIsTech = containsAny(text,
                    "开发", "工程师", "后端", "前端", "java", "算法", "测试", "运维",
                    "架构", "dev", "engineer", "spring", "mysql", "redis");
            if (candIsProduct) {
                return 95.0;
            }
            return candIsTech ? 25.0 : 60.0;
        }
        if (jobIsTech && !jobIsProduct) {
            boolean candIsTech = containsAny(text,
                    "开发", "工程师", "后端", "前端", "java", "算法", "测试", "运维",
                    "架构", "dev", "engineer", "spring", "mysql", "redis");
            return candIsTech ? 95.0 : 50.0;
        }
        return 80.0;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /** 学历字符串 → 分（0-100）。 */
    private double educationScore(String jobLevel, String candidateLevel) {
        int job = levelOf(jobLevel);
        int cand = levelOf(candidateLevel);
        if (job <= 0) return 80.0;
        if (cand >= job) return 90.0;
        if (cand == job - 1) return 70.0;
        return 50.0;
    }

    private int levelOf(String level) {
        if (level == null) return 0;
        return switch (level.toUpperCase()) {
            case "PHD" -> 4;
            case "MASTER" -> 3;
            case "BACHELOR" -> 2;
            case "ASSOCIATE" -> 1;
            default -> 0;
        };
    }
}
