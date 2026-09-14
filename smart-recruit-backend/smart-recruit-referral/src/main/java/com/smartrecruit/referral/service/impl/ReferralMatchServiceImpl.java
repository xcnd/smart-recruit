package com.smartrecruit.referral.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.common.exception.ResourceNotFoundException;
import com.smartrecruit.referral.dto.response.ReferralMatchVO;
import com.smartrecruit.referral.entity.RefProgramJob;
import com.smartrecruit.referral.entity.ReferralMatchResult;
import com.smartrecruit.referral.entity.ReferralRecord;
import com.smartrecruit.referral.feign.AiAgentCapabilityClient;
import com.smartrecruit.referral.feign.RecruitmentCandidateClient;
import com.smartrecruit.referral.repository.RefProgramJobMapper;
import com.smartrecruit.referral.repository.ReferralMatchResultMapper;
import com.smartrecruit.referral.repository.ReferralRecordMapper;
import com.smartrecruit.referral.service.ReferralMatchService;
import com.smartrecruit.common.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 内推智能匹配服务实现。
 *
 * <p>业务接入点：内推记录创建后自动匹配；详情可手动重新匹配。
 * 匹配优先调用 AI 引擎 Agent 能力网关，失败降级为本地启发式
 * （候选人技能 × 职位标题关键词），结果保存 Top 3 供前端展示。</p>
 *
 * @since 2026-04-06
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReferralMatchServiceImpl implements ReferralMatchService {

    /** 推荐数量上限。 */
    private static final int TOP_N = 3;

    private final ReferralRecordMapper recordMapper;
    private final RefProgramJobMapper refProgramJobMapper;
    private final ReferralMatchResultMapper matchMapper;
    private final RecruitmentCandidateClient recruitmentCandidateClient;
    private final AiAgentCapabilityClient aiAgentCapabilityClient;

    /** 查询内推匹配结果列表。 */
    @Override
    public List<ReferralMatchVO> listMatches(Long recordId) {
        return matchMapper.selectList(
                        new LambdaQueryWrapper<ReferralMatchResult>()
                                .eq(ReferralMatchResult::getRecordId, recordId)
                                .orderByAsc(ReferralMatchResult::getRankNo))
                .stream()
                .map(this::toVO)
                .toList();
    }

    /** 刷新内推匹配结果。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ReferralMatchVO> refreshMatches(Long recordId) {
        ReferralRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new ResourceNotFoundException("内推记录不存在: id=" + recordId);
        }

        // 1. 候选人画像（技能、年限来自招聘模块）
        Map<String, Object> candidate = fetchCandidate(record.getCandidateId());
        String candidateName = candidate.get("name") != null
                ? String.valueOf(candidate.get("name")) : "候选人";

        // 2. 匹配池：所有启用的内推职位
        List<RefProgramJob> jobs = refProgramJobMapper.selectList(
                new LambdaQueryWrapper<RefProgramJob>().eq(RefProgramJob::getIsEnabled, 1));
        if (jobs.isEmpty()) {
            matchMapper.delete(new LambdaQueryWrapper<ReferralMatchResult>()
                    .eq(ReferralMatchResult::getRecordId, recordId));
            return List.of();
        }

        // 3. AI 引擎匹配（best-effort）
        Map<String, Object> aiBest = callAiMatch(candidate, jobs);

        // 4. 本地启发式打分（技能 × 职位标题）
        List<ScoredJob> scored = jobs.stream()
                .map(job -> new ScoredJob(job, heuristicScore(skillsOf(candidate), job.getJobTitle())))
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .toList();

        // 5. 合并 Top N：AI 最佳置顶，其余按本地分补足（按职位去重）
        Map<Long, ReferralMatchResult> merged = new LinkedHashMap<>();
        if (aiBest != null) {
            Long aiJobId = toLong(aiBest.get("matchedJobId"));
            if (aiJobId != null && aiJobId > 0) {
                merged.put(aiJobId, buildRow(record, candidateName, aiJobId,
                        str(aiBest.get("matchedJobTitle")),
                        toDouble(aiBest.get("matchScore")),
                        str(aiBest.get("recommendation")),
                        str(aiBest.get("suggestedApproach")),
                        aiBest.get("matchDimensions"),
                        "AI"));
            }
        }
        for (ScoredJob s : scored) {
            Long jobId = s.job.getJobPositionId();
            if (merged.containsKey(jobId)) {
                continue;
            }
            merged.put(jobId, buildRow(record, candidateName, jobId,
                    s.job.getJobTitle(), s.score, null, null, null, "HEURISTIC"));
            if (merged.size() >= TOP_N) {
                break;
            }
        }

        // 6. 落库（先清后插，幂等）
        matchMapper.delete(new LambdaQueryWrapper<ReferralMatchResult>()
                .eq(ReferralMatchResult::getRecordId, recordId));
        int rank = 1;
        for (ReferralMatchResult row : merged.values()) {
            row.setRankNo(rank++);
            row.setCreateTime(DateUtils.now());
            matchMapper.insert(row);
        }
        log.info("内推智能匹配完成: recordId={}, candidate={}, results={}",
                recordId, candidateName, merged.size());
        return listMatches(recordId);
    }

    // ==================== 数据组装 ====================

    private Map<String, Object> fetchCandidate(Long candidateId) {
        try {
            ApiResponse<Map<String, Object>> resp =
                    recruitmentCandidateClient.getCandidate(candidateId);
            if (resp != null && resp.ok() && resp.data() != null) {
                return resp.data();
            }
        } catch (Exception e) {
            log.warn("拉取候选人信息失败（按空画像匹配）: candidateId={}, error={}",
                    candidateId, e.getMessage());
        }
        return Map.of("id", candidateId, "name", "候选人");
    }

    private Map<String, Object> callAiMatch(Map<String, Object> candidate,
                                            List<RefProgramJob> jobs) {
        try {
            Map<String, Object> request = new LinkedHashMap<>();
            Map<String, Object> profile = new LinkedHashMap<>();
            profile.put("name", candidate.get("name"));
            profile.put("email", candidate.get("email"));
            profile.put("yearsOfExperience", candidate.get("yearsOfExperience"));
            profile.put("skills", candidate.get("skills"));
            request.put("candidate", profile);
            request.put("jobs", jobs.stream().map(job -> {
                Map<String, Object> t = new LinkedHashMap<>();
                t.put("jobId", job.getJobPositionId());
                t.put("title", job.getJobTitle());
                t.put("salaryMax", job.getMaxSalary());
                return t;
            }).toList());

            ApiResponse<Map<String, Object>> resp = aiAgentCapabilityClient.matchReferral(request);
            if (resp != null && resp.ok() && resp.data() != null && !resp.data().isEmpty()) {
                return resp.data();
            }
        } catch (Exception e) {
            log.warn("AI 内推匹配调用失败，降级本地启发式: error={}", e.getMessage());
        }
        return null;
    }

    private ReferralMatchResult buildRow(ReferralRecord record, String candidateName,
                                         Long jobId, String jobTitle, double score,
                                         String recommendation, String approach,
                                         Object dimensions, String source) {
        ReferralMatchResult row = new ReferralMatchResult();
        row.setRecordId(record.getId());
        row.setCandidateId(record.getCandidateId());
        row.setCandidateName(candidateName);
        row.setMatchedJobId(jobId);
        row.setMatchedJobTitle(jobTitle);
        row.setMatchScore(BigDecimal.valueOf(Math.round(score * 100.0) / 100.0));
        row.setRecommendation(recommendation);
        row.setSuggestedApproach(approach);
        row.setMatchDimensions(dimensions);
        row.setSource(source);
        return row;
    }

    private double heuristicScore(List<String> skills, String title) {
        if (title == null || title.isBlank()) {
            return 0;
        }
        String t = title.toLowerCase(Locale.ROOT);
        double score = 40;
        if (skills != null) {
            for (String skill : skills) {
                if (skill != null && !skill.isBlank() && t.contains(skill.toLowerCase(Locale.ROOT))) {
                    score += 12;
                }
            }
        }
        if (t.contains("高级") || t.contains("资深")) score += 5;
        if (t.contains("工程师") || t.contains("开发")) score += 5;
        return Math.min(100, score);
    }

    @SuppressWarnings("unchecked")
    private List<String> skillsOf(Map<String, Object> candidate) {
        Object skills = candidate.get("skills");
        if (skills instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }

    private ReferralMatchVO toVO(ReferralMatchResult entity) {
        return ReferralMatchVO.builder()
                .id(entity.getId())
                .recordId(entity.getRecordId())
                .candidateId(entity.getCandidateId())
                .candidateName(entity.getCandidateName())
                .matchedJobId(entity.getMatchedJobId())
                .matchedJobTitle(entity.getMatchedJobTitle())
                .matchScore(entity.getMatchScore())
                .recommendation(entity.getRecommendation())
                .suggestedApproach(entity.getSuggestedApproach())
                .matchDimensions(entity.getMatchDimensions())
                .source(entity.getSource())
                .rankNo(entity.getRankNo())
                .createTime(entity.getCreateTime())
                .build();
    }

    private String str(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        try { return Long.parseLong(value.toString()); } catch (NumberFormatException e) { return null; }
    }

    private double toDouble(Object value) {
        if (value == null) return 0;
        if (value instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(value.toString()); } catch (NumberFormatException e) { return 0; }
    }

    private record ScoredJob(RefProgramJob job, double score) {}
}
