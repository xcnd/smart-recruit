package com.smartrecruit.recruitment.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.smartrecruit.recruitment.domain.ParsedResume;
import com.smartrecruit.recruitment.dto.response.AiScreeningResultVO;
import com.smartrecruit.recruitment.dto.response.AiScreeningResultVO.DimensionVO;
import com.smartrecruit.recruitment.dto.response.BatchProgressVO;
import com.smartrecruit.recruitment.dto.request.LlmChatRequest;
import com.smartrecruit.recruitment.dto.request.TaskReport;
import com.smartrecruit.recruitment.entity.AiScreeningResult;
import com.smartrecruit.recruitment.entity.AiScreeningResult.DimensionRecord;
import com.smartrecruit.recruitment.entity.Candidate;
import com.smartrecruit.recruitment.entity.JobPosition;
import com.smartrecruit.recruitment.entity.Resume;
import com.smartrecruit.recruitment.enums.RecruitmentEnums;
import com.smartrecruit.recruitment.repository.AiScreeningResultMapper;
import com.smartrecruit.recruitment.repository.CandidateMapper;
import com.smartrecruit.recruitment.repository.JobPositionMapper;
import com.smartrecruit.recruitment.repository.ResumeMapper;
import com.smartrecruit.recruitment.service.AiScreeningService;
import com.smartrecruit.recruitment.feign.AiAgentLlmClient;
import com.smartrecruit.recruitment.feign.AiAgentTaskClient;
import com.smartrecruit.recruitment.feign.SystemConfigClient;
import com.smartrecruit.common.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;

/**
 * {@link AiScreeningService} 实现类。
 *
 * <p>双引擎架构：
 * <ul>
 *   <li>AI 引擎 — 通过 {@link AiAgentLlmClient} 调用 AI 引擎的 LLM 网关评估</li>
 *   <li>启发式引擎 — 基于规则和解析数据的本地评分</li>
 * </ul>
 * AI 引擎可用时优先走 AI，不可用时自动降级为启发式。</p>
 *
 * @since 1.1.0
 */
@Service
@Slf4j
public class AiScreeningServiceImpl implements AiScreeningService {

    private final ResumeMapper resumeMapper;
    private final CandidateMapper candidateMapper;
    private final JobPositionMapper jobPositionMapper;
    private final AiScreeningResultMapper resultMapper;
    private final SystemConfigClient systemConfigClient;

    /** AI 筛选配置缓存（TTL 60 秒，配置修改后最多延迟 1 分钟生效）。 */
    private static final long CONFIG_TTL_MS = 60_000;
    private volatile ScreenConfig screenConfig;
    private volatile long configFetchedAt;

    @Autowired(required = false)
    private AiAgentLlmClient aiAgentLlmClient;

    @Autowired(required = false)
    private AiAgentTaskClient aiAgentTaskClient;

    @Autowired(required = false)
    @Qualifier("aiEngineExecutor")
    private Executor aiEngineExecutor;

    /** 批量筛选进度：taskId → BatchProgress */
    private final Map<String, BatchProgress> batchProgressMap = new ConcurrentHashMap<>();

    public AiScreeningServiceImpl(ResumeMapper resumeMapper,
                                   CandidateMapper candidateMapper,
                                   JobPositionMapper jobPositionMapper,
                                   AiScreeningResultMapper resultMapper,
                                   SystemConfigClient systemConfigClient) {
        this.resumeMapper = resumeMapper;
        this.candidateMapper = candidateMapper;
        this.jobPositionMapper = jobPositionMapper;
        this.resultMapper = resultMapper;
        this.systemConfigClient = systemConfigClient;
    }

    /** AI 简历筛选维度权重与最低通过分数线。 */
    private record ScreenConfig(int weightEducation, int weightSkill, int weightExperience,
                                int weightBehavior, int weightSemantic, int passScore) {
        static ScreenConfig defaults() {
            return new ScreenConfig(20, 35, 20, 20, 15, 70);
        }
    }

    // ─── 单份异步筛选 ───

    /** 异步执行 AI 简历筛选。 */
    @Override
    @Async("aiEngineExecutor")
    public void screenAsync(Long resumeId) {
        long start = DateUtils.currentEpochMillis();
        log.info("[计时] AI筛选启动: resumeId={}, thread={}", resumeId, Thread.currentThread().getName());

        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null || resume.getParsedContent() == null) {
            log.warn("简历不存在或未解析，跳过AI筛选: resumeId={}", resumeId);
            return;
        }

        // 设置筛选中状态
        resume.setScreeningStatus(RecruitmentEnums.ScreeningStatus.SCREENING.getCode());
        resumeMapper.updateById(resume);

        try {
            AiScreeningResult result = doScreen(resume);
            persistResult(resume, result);

            log.info("[计时] AI筛选完成: resumeId={}, score={}, source={}, 总耗时{}ms",
                    resumeId, result.getOverallScore(), result.getSource(),
                    DateUtils.currentEpochMillis() - start);
        } catch (Exception e) {
            log.error("AI筛选失败: resumeId={}", resumeId, e);
            resume.setScreeningStatus(RecruitmentEnums.ScreeningStatus.SCREEN_FAILED.getCode());
            resumeMapper.updateById(resume);
        }
    }

    // ─── 核心评分逻辑 ───

    private AiScreeningResult doScreen(Resume resume) {
        long start = DateUtils.currentEpochMillis();
        try {
            return screenWithLlm(resume);
        } catch (Exception e) {
            log.warn("AI 引擎筛选失败，降级为启发式评分: resumeId={}, error={}",
                    resume.getId(), e.getMessage());
            reportHeuristicScreeningTask(resume, DateUtils.currentEpochMillis() - start, e.getMessage());
        }
        return screenWithHeuristic(resume);
    }

    /**
     * 上报兜底任务（LLM 失败降级为启发式评分时）。
     *
     * <p>保证 AI 智能编排监控页「Agent 任务队列」始终能看到真实的简历筛选任务，
     * 即使 LLM 调用失败降级为启发式评分也不会丢任务。</p>
     *
     * @param resume    被筛选的简历
     * @param durationMs LLM 调用耗时（毫秒）
     * @param llmError  LLM 失败原因
     */
    private void reportHeuristicScreeningTask(Resume resume, long durationMs, String llmError) {
        if (aiAgentTaskClient == null) {
            log.warn("AiAgentTaskClient 未注入，跳过兜底任务上报: resumeId={}", resume.getId());
            return;
        }
        try {
            JobPosition position = jobPositionMapper.selectById(resume.getJobPositionId());
            String jobTitle = position != null ? position.getTitle() : "未知职位";
            aiAgentTaskClient.reportTask(new TaskReport(
                    "smart-screener",
                    1, // AiEnums.TaskType.SCREEN
                    true,
                    durationMs,
                    "简历筛选（LLM 降级为启发式评分）: resumeId=" + resume.getId() + ", 职位=" + jobTitle,
                    "启发式评分结果；LLM 失败原因：" + llmError,
                    null));
            log.info("已上报启发式简历筛选任务: resumeId={}", resume.getId());
        } catch (Exception e) {
            log.warn("上报启发式简历筛选任务失败: resumeId={}, error={}", resume.getId(), e.getMessage());
        }
    }

    // ─── LLM 评分 ───

    private AiScreeningResult screenWithLlm(Resume resume) {
        Candidate candidate = candidateMapper.selectById(resume.getCandidateId());

        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(resume, candidate);
        // 统一走 AI 引擎 LLM 网关，按 smart-screener 路由模型并统计 Token
        Map<String, Object> llmResponse = aiAgentLlmClient.chat(
                new LlmChatRequest(systemPrompt, userPrompt, "smart-screener"));

        return parseLlmResponse(llmResponse, resume.getId());
    }

    private String buildSystemPrompt() {
        ScreenConfig cfg = loadScreenConfig();
        return "你是一位拥有15年经验的资深技术招聘专家。你的任务是根据职位要求，客观、专业、全面地评估候选人的简历，生成结构化的评估结果。\n\n" +
               "评估原则：\n" +
               "1. 基于事实数据进行评估，避免主观臆断\n" +
               String.format("2. 综合评分按5个维度加权计算：学历背景×%d%% + 技能匹配×%d%% + 经验匹配×%d%% + 行为表现×%d%% + 语义匹配×%d%%\n",
                       cfg.weightEducation(), cfg.weightSkill(), cfg.weightExperience(),
                       cfg.weightBehavior(), cfg.weightSemantic()) +
               String.format("3. 最低通过分数线为 %d 分，达到或超过即建议推荐面试\n", cfg.passScore()) +
               "3. 优点和不足必须具体、可操作，避免空泛表述\n" +
               "4. 综合评语需在150字以内，简洁有力\n" +
               "5. 各维度评分（0-100）须与简历事实一致，overallScore 须等于5个维度得分的加权结果\n\n" +
               "请严格按照JSON格式输出，不要包含任何markdown标记或其他解释文字。";
    }

    @SuppressWarnings("unchecked")
    private String buildUserPrompt(Resume resume, Candidate candidate) {
        StringBuilder sb = new StringBuilder();
        sb.append("请评估以下候选人与职位的匹配度。\n\n");

        // 候选人信息
        sb.append("## 候选人信息\n");
        sb.append("- 姓名：").append(candidate != null ? candidate.getName() : "未知").append("\n");
        if (candidate != null && candidate.getCurrentCompany() != null) {
            sb.append("- 当前/最近公司：").append(candidate.getCurrentCompany()).append("\n");
        }
        if (candidate != null && candidate.getYearsOfExperience() != null) {
            sb.append("- 工作年限：").append(candidate.getYearsOfExperience()).append("年\n");
        }
        if (candidate != null && candidate.getSkills() != null && !candidate.getSkills().isEmpty()) {
            sb.append("- 技能列表：").append(String.join("、", candidate.getSkills())).append("\n");
        }

        // 解析内容摘要
        ParsedResume parsed = resume.getParsedContent();
        if (parsed != null) {
            List<ParsedResume.ExperienceEntry> experience = parsed.getExperience();
            if (!experience.isEmpty()) {
                sb.append("- 工作经历：\n");
                for (ParsedResume.ExperienceEntry exp : experience) {
                    sb.append("  - ").append(exp.getCompany()).append(" | ")
                            .append(exp.getPosition()).append("\n");
                }
            }
            List<ParsedResume.EducationEntry> education = parsed.getEducation();
            if (!education.isEmpty()) {
                sb.append("- 教育背景：\n");
                for (ParsedResume.EducationEntry edu : education) {
                    sb.append("  - ").append(edu.getSchool()).append(" | ")
                            .append(edu.getDegree()).append(" | ")
                            .append(edu.getMajor()).append("\n");
                }
            }
        }

        // 职位要求
        sb.append("\n## 职位要求\n");
        sb.append(buildJobContext(resume));

        // 输出格式
        sb.append("\n## 评估要求\n");
        sb.append("请从以下5个维度进行评分（0-100分）：\n");
        sb.append("1. 技能匹配：候选人技能与职位要求技能的匹配程度\n");
        sb.append("2. 经验匹配：相关工作经验的深度和广度\n");
        sb.append("3. 学历背景：教育背景与职位要求的匹配度\n");
        sb.append("4. 行为表现：团队协作、学习能力、职业稳定性等综合表现\n");
        sb.append("5. 语义匹配：简历内容与职位描述在语义上的整体契合度\n\n");
        sb.append("## 输出JSON格式\n");
        sb.append("{\n");
        sb.append("  \"overallScore\": <综合加权评分 0-100>,\n");
        sb.append("  \"dimensions\": [\n");
        sb.append("    {\"name\": \"技能匹配\", \"score\": <0-100>},\n");
        sb.append("    {\"name\": \"经验匹配\", \"score\": <0-100>},\n");
        sb.append("    {\"name\": \"学历背景\", \"score\": <0-100>},\n");
        sb.append("    {\"name\": \"行为表现\", \"score\": <0-100>},\n");
        sb.append("    {\"name\": \"语义匹配\", \"score\": <0-100>}\n");
        sb.append("  ],\n");
        sb.append("  \"matchedKeywords\": [\"匹配的技能1\", \"匹配的技能2\"],\n");
        sb.append("  \"missingKeywords\": [\"缺失的技能1\", \"缺失的技能2\"],\n");
        sb.append("  \"suggestion\": \"STRONG_HIRE|HIRE|CONSIDER|REJECT\",\n");
        sb.append("  \"summary\": \"<150字综合评语>\",\n");
        sb.append("  \"strengths\": [\"具体优势1\", \"具体优势2\"],\n");
        sb.append("  \"weaknesses\": [\"具体不足1\", \"具体不足2\"]\n");
        sb.append("}\n");

        return sb.toString();
    }

    // ─── AI 筛选配置（系统设置 → AI 筛选配置） ───

    /**
     * 读取 AI 筛选维度权重与最低通过分数线（带 60 秒本地缓存）。
     */
    private ScreenConfig loadScreenConfig() {
        long now = DateUtils.currentEpochMillis();
        if (screenConfig != null && now - configFetchedAt < CONFIG_TTL_MS) {
            return screenConfig;
        }
        try {
            var resp = systemConfigClient.getByKeys(
                    "ai_screen_weight_education,ai_screen_weight_skill,ai_screen_weight_experience,"
                            + "ai_screen_weight_behavior,ai_screen_weight_semantic,ai_screen_pass_score");
            Map<String, String> values = resp != null ? resp.data() : null;
            if (values != null && !values.isEmpty()) {
                screenConfig = new ScreenConfig(
                        intOf(values.get("ai_screen_weight_education"), 20),
                        intOf(values.get("ai_screen_weight_skill"), 35),
                        intOf(values.get("ai_screen_weight_experience"), 20),
                        intOf(values.get("ai_screen_weight_behavior"), 20),
                        intOf(values.get("ai_screen_weight_semantic"), 15),
                        intOf(values.get("ai_screen_pass_score"), 70));
                configFetchedAt = now;
                log.info("已加载 AI 筛选配置: {}", screenConfig);
            }
        } catch (Exception e) {
            log.warn("读取 AI 筛选配置失败，使用默认配置: error={}", e.getMessage());
        }
        if (screenConfig == null) {
            screenConfig = ScreenConfig.defaults();
        }
        return screenConfig;
    }

    /** 配置字符串 → int，异常或缺失时返回默认值。 */
    private static int intOf(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** 按配置权重计算加权总分。 */
    private double weightedScore(ScreenConfig cfg, double skill, double experience,
                                 double education, double behavior, double semantic) {
        return (skill * cfg.weightSkill()
                + experience * cfg.weightExperience()
                + education * cfg.weightEducation()
                + behavior * cfg.weightBehavior()
                + semantic * cfg.weightSemantic()) / 100.0;
    }

    /** 按最低通过分数线生成录用建议。 */
    private static String suggestionFor(double score, int passScore) {
        if (score >= passScore + 15) return "STRONG_HIRE";
        if (score >= passScore) return "HIRE";
        if (score >= passScore - 15) return "CONSIDER";
        return "REJECT";
    }

    private String buildJobContext(Resume resume) {
        if (resume.getJobPositionId() == null) {
            return "（无具体职位要求，请进行通用能力评估）\n";
        }
        JobPosition job = jobPositionMapper.selectById(resume.getJobPositionId());
        if (job == null) {
            return "（无具体职位要求，请进行通用能力评估）\n";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("- 职位名称：").append(job.getTitle()).append("\n");
        if (job.getDescription() != null && !job.getDescription().isEmpty()) {
            sb.append("- 职位描述：").append(truncateText(job.getDescription(), 1500)).append("\n");
        }
        if (job.getSkills() != null && !job.getSkills().isEmpty()) {
            sb.append("- 要求技能：").append(String.join("、", job.getSkills())).append("\n");
        }
        if (job.getLocation() != null && !job.getLocation().isEmpty()) {
            sb.append("- 工作地点：").append(job.getLocation()).append("\n");
        }
        RecruitmentEnums.ExperienceLevel level = RecruitmentEnums.ExperienceLevel.fromCode(job.getLevel());
        if (level != null) {
            sb.append("- 经验要求：").append(level.getLabel()).append("\n");
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private AiScreeningResult parseLlmResponse(Map<String, Object> response, Long resumeId) {
        AiScreeningResult result = new AiScreeningResult();
        result.setId(IdWorker.getId());
        result.setResumeId(resumeId);
        result.setSource("LLM");
        result.setCreatedAt(DateUtils.now());

        Object score = response.get("overallScore");
        if (score instanceof Number n) {
            result.setOverallScore(BigDecimal.valueOf(n.doubleValue()).setScale(2, RoundingMode.HALF_UP));
        } else if (score instanceof String s) {
            result.setOverallScore(new BigDecimal(s).setScale(2, RoundingMode.HALF_UP));
        } else {
            result.setOverallScore(BigDecimal.ZERO);
        }

        Object dims = response.get("dimensions");
        Map<String, Integer> dimScoreMap = new LinkedHashMap<>();
        if (dims instanceof List<?> list) {
            List<DimensionRecord> records = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    DimensionRecord dr = new DimensionRecord();
                    Object nameVal = m.get("name");
                    dr.setName(nameVal != null ? nameVal.toString() : "");
                    Object s = m.get("score");
                    if (s instanceof Number n) dr.setScore(n.intValue());
                    else if (s instanceof String str) dr.setScore(Integer.parseInt(str));
                    records.add(dr);
                    if (dr.getName() != null && dr.getScore() != null) {
                        dimScoreMap.put(dr.getName(), dr.getScore());
                    }
                }
            }
            result.setDimensions(records);

            // 按系统配置的 5 维权重重新计算综合评分，保证权重配置真实生效
            ScreenConfig cfg = loadScreenConfig();
            Double weighted = weightedScoreOrNull(cfg, dimScoreMap);
            if (weighted != null) {
                result.setOverallScore(BigDecimal.valueOf(weighted)
                        .setScale(2, RoundingMode.HALF_UP));
                result.setSuggestion(suggestionFor(weighted, cfg.passScore()));
            }
        }

        result.setMatchedKeywords(extractStringListFromResponse(response, "matchedKeywords"));
        result.setMissingKeywords(extractStringListFromResponse(response, "missingKeywords"));
        if (result.getSuggestion() == null) {
            result.setSuggestion(String.valueOf(response.getOrDefault("suggestion", "CONSIDER")));
        }
        result.setSummary(String.valueOf(response.getOrDefault("summary", "")));
        result.setStrengths(extractStringListFromResponse(response, "strengths"));
        result.setWeaknesses(extractStringListFromResponse(response, "weaknesses"));

        return result;
    }

    /** 从维度评分映射中计算加权总分；维度缺失时返回 null（回退 LLM 原始评分）。 */
    private Double weightedScoreOrNull(ScreenConfig cfg, Map<String, Integer> dimScoreMap) {
        Double skill = dimOf(dimScoreMap, "技能匹配");
        Double experience = dimOf(dimScoreMap, "经验匹配");
        Double education = dimOf(dimScoreMap, "学历背景");
        Double behavior = dimOf(dimScoreMap, "行为表现");
        Double semantic = dimOf(dimScoreMap, "语义匹配");
        if (skill == null || experience == null || education == null
                || behavior == null || semantic == null) {
            return null;
        }
        return weightedScore(cfg, skill, experience, education, behavior, semantic);
    }

    private static Double dimOf(Map<String, Integer> dimScoreMap, String name) {
        Integer score = dimScoreMap.get(name);
        return score != null ? score.doubleValue() : null;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractStringListFromResponse(Map<String, Object> response, String key) {
        Object obj = response.get(key);
        if (obj instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    // ─── 启发式评分 ───

    private AiScreeningResult screenWithHeuristic(Resume resume) {
        Candidate candidate = candidateMapper.selectById(resume.getCandidateId());
        String candidateName = candidate != null ? candidate.getName() : "未知";
        List<String> candidateSkills = candidate != null && candidate.getSkills() != null
                ? candidate.getSkills() : List.of();
        int eduLevel = candidate != null && candidate.getEducation() != null
                ? candidate.getEducation() : 0;
        int workYears = candidate != null && candidate.getYearsOfExperience() != null
                ? candidate.getYearsOfExperience() : 0;
        String company = candidate != null && candidate.getCurrentCompany() != null
                ? candidate.getCurrentCompany() : "";

        // 从简历解析内容中提取更丰富的数据
        List<String> parsedSkills = new ArrayList<>();
        List<ParsedResume.ExperienceEntry> parsedExperience = new ArrayList<>();
        List<ParsedResume.ProjectEntry> parsedProjects = new ArrayList<>();
        if (resume.getParsedContent() != null) {
            ParsedResume parsed = resume.getParsedContent();
            parsedSkills = new ArrayList<>(parsed.getSkills());
            parsedExperience = parsed.getExperience();
            parsedProjects = parsed.getProjects();

            // 从 skillsText 补充技能（解析后存储的完整技能文本）
            if (parsedSkills.size() < 8) {
                List<String> textSkills = extractSkillsFromText(parsed);
                if (!textSkills.isEmpty()) {
                    Set<String> merged = new LinkedHashSet<>(parsedSkills);
                    merged.addAll(textSkills);
                    parsedSkills = new ArrayList<>(merged);
                }
            }

            // 从解析数据补全候选人表中缺失的字段
            if (workYears <= 0) {
                String parsedWorkYears = parsed.getWorkYears();
                if (parsedWorkYears != null && !parsedWorkYears.isEmpty()) {
                    try {
                        workYears = Integer.parseInt(parsedWorkYears);
                    } catch (NumberFormatException ignored) {}
                }
            }
            if (eduLevel <= 0) {
                eduLevel = parseEducationLevel(parsed.getEducation());
            }
        }

        // 合并技能来源
        List<String> allSkills;
        if (!parsedSkills.isEmpty()) {
            Set<String> merged = new LinkedHashSet<>(parsedSkills);
            merged.addAll(candidateSkills);
            allSkills = new ArrayList<>(merged);
        } else {
            allSkills = new ArrayList<>(candidateSkills);
        }
        int skillCount = allSkills.size();
        int projectsCount = parsedProjects.size();
        // 经验计数 = max(工作经历数, 项目数) + 工作年限（项目经验更能体现能力）
        int expCount = Math.max(parsedExperience.size(), projectsCount) + workYears;

        ThreadLocalRandom rand = ThreadLocalRandom.current();

        // 技能匹配度
        int skillMatch = skillCount >= 20 ? 88 + rand.nextInt(10)
                : skillCount >= 12 ? 80 + rand.nextInt(13)
                : skillCount >= 8 ? 68 + rand.nextInt(15)
                : skillCount >= 5 ? 55 + rand.nextInt(18)
                : skillCount >= 2 ? 35 + rand.nextInt(21)
                : 20 + rand.nextInt(16);

        // 项目经验
        int experienceMatch = expCount >= 12 ? 90 + rand.nextInt(9)
                : expCount >= 8 ? 80 + rand.nextInt(13)
                : expCount >= 5 ? 65 + rand.nextInt(18)
                : expCount >= 3 ? 50 + rand.nextInt(21)
                : expCount >= 1 ? 30 + rand.nextInt(21)
                : 15 + rand.nextInt(16);

        // 学历背景
        int educationMatch = eduLevel >= 4 ? 88 + rand.nextInt(9)
                : eduLevel >= 3 ? 80 + rand.nextInt(13)
                : eduLevel >= 2 ? 65 + rand.nextInt(16)
                : eduLevel >= 1 ? 45 + rand.nextInt(21)
                : 30 + rand.nextInt(16);

        // 主观维度
        int stabilityScore = expCount >= 5 ? 65 + rand.nextInt(26) : 45 + rand.nextInt(31);
        int teamScore = 50 + rand.nextInt(41);
        int learningScore = skillCount >= 10 ? 65 + rand.nextInt(26) : 45 + rand.nextInt(31);
        int behaviorScore = (int) Math.round((stabilityScore + teamScore + learningScore) / 3.0);

        // 语义匹配：候选技能与职位要求技能的重合度
        int semanticScore;
        JobPosition job = resume.getJobPositionId() != null
                ? jobPositionMapper.selectById(resume.getJobPositionId()) : null;
        if (job != null && job.getSkills() != null && !job.getSkills().isEmpty()) {
            Set<String> requiredSkills = new LinkedHashSet<>(job.getSkills());
            long overlap = allSkills.stream().filter(requiredSkills::contains).count();
            semanticScore = (int) Math.round(overlap * 100.0 / requiredSkills.size());
            semanticScore = Math.max(35, Math.min(95, semanticScore + rand.nextInt(-5, 6)));
        } else {
            semanticScore = 60 + rand.nextInt(21);
        }

        // 按系统配置的 5 维权重计算综合评分
        ScreenConfig cfg = loadScreenConfig();
        int overall = (int) Math.round(weightedScore(cfg, skillMatch, experienceMatch,
                educationMatch, behaviorScore, semanticScore));

        // 录用建议
        String suggestion = suggestionFor(overall, cfg.passScore());
        int passScore = cfg.passScore();

        // 缺失关键词
        List<String> commonMissing = new ArrayList<>(List.of(
                "CI/CD", "性能优化", "系统架构设计", "项目管理", "跨部门协作"));
        commonMissing.removeAll(allSkills);
        List<String> missingKeywords;
        if (commonMissing.isEmpty()) {
            missingKeywords = List.of("CI/CD", "性能优化");
        } else if (commonMissing.size() <= 3) {
            missingKeywords = commonMissing;
        } else {
            missingKeywords = commonMissing.subList(0, 3);
        }

        // 优势
        List<String> strengths = new ArrayList<>();
        if (skillCount >= 12) strengths.add("技术栈丰富，掌握" + skillCount + "项专业技能");
        else if (skillCount >= 5) strengths.add("具备一定技术广度，掌握" + String.join("、",
                allSkills.subList(0, Math.min(3, allSkills.size()))) + "等核心技能");
        if (projectsCount >= 5) strengths.add("项目经验丰富，参与过" + projectsCount + "个完整项目");
        else if (projectsCount >= 2) strengths.add("具备" + projectsCount + "个项目实战经验");
        if (workYears >= 5) strengths.add("具备" + workYears + "年行业经验，项目积累丰富");
        else if (workYears >= 2) strengths.add("具备" + workYears + "年相关工作经验");
        if (eduLevel >= 3) strengths.add("学历背景优秀，具备扎实的理论基础");
        else if (eduLevel >= 2) strengths.add("具备良好的教育背景");
        if (!company.isEmpty() && !"待解析".equals(company)) strengths.add("有" + company + "等知名企业工作背景");
        if (strengths.isEmpty()) {
            strengths.add("简历信息完整，具备基本工作能力");
            strengths.add("具备持续学习意愿和成长潜力");
        }

        // 不足
        List<String> weaknesses = new ArrayList<>();
        if (skillCount < 5) weaknesses.add("技能覆盖面较窄，建议拓展技术栈");
        if (workYears < 2 && projectsCount < 3) weaknesses.add("经验相对较少，独立承担复杂任务能力待验证");
        if (eduLevel < 2) weaknesses.add("学历背景与岗位要求有一定差距");
        if (!allSkills.contains("Docker") && !allSkills.contains("Kubernetes")) {
            weaknesses.add("缺乏容器化和云原生相关经验");
        }
        if (weaknesses.isEmpty()) {
            weaknesses.add("简历中未详细描述项目成果和量化指标");
            weaknesses.add("建议补充开源贡献或个人项目链接");
        }

        // 综合评语
        StringBuilder summaryBuilder = new StringBuilder();
        summaryBuilder.append("该候选人综合评分").append(overall).append("分。");
        if (!candidateName.equals("待解析") && !candidateName.equals("未知")) {
            summaryBuilder.append(candidateName);
        }
        if (!company.isEmpty() && !"待解析".equals(company)) {
            summaryBuilder.append("曾在").append(company).append("任职");
        }
        if (skillCount > 0) summaryBuilder.append("，掌握").append(Math.min(skillCount, 8)).append("项专业技能");
        if (workYears > 0) summaryBuilder.append("，拥有").append(workYears).append("年工作经验");
        if (projectsCount > 0) summaryBuilder.append("，参与过").append(projectsCount).append("个项目");
        summaryBuilder.append("。");
        if (overall >= passScore + 15) summaryBuilder.append("技术栈、项目经验与职位高度匹配。建议尽快安排技术面试，进一步考察架构设计和问题解决能力。综合评估建议强烈推荐录用。");
        else if (overall >= passScore) summaryBuilder.append("核心技能与职位要求较为匹配。可安排初试进一步了解项目深度和业务理解。综合评估建议推荐录用。");
        else if (overall >= passScore - 15) summaryBuilder.append("部分技能符合要求但整体匹配度一般。可放入人才库备选，或在候选人池不足时考虑。");
        else summaryBuilder.append("与职位要求差距较大，建议不推荐录用。");

        // 构建结果
        AiScreeningResult result = new AiScreeningResult();
        result.setId(IdWorker.getId());
        result.setResumeId(resume.getId());
        result.setOverallScore(BigDecimal.valueOf(overall).setScale(2, RoundingMode.HALF_UP));
        result.setSuggestion(suggestion);
        result.setSummary(summaryBuilder.toString());
        result.setStrengths(strengths);
        result.setWeaknesses(weaknesses);
        result.setMatchedKeywords(new ArrayList<>(allSkills));
        result.setMissingKeywords(missingKeywords);
        result.setSource("HEURISTIC");
        result.setCreatedAt(DateUtils.now());

        List<DimensionRecord> dims = new ArrayList<>();
        dims.add(buildDim("技能匹配", skillMatch));
        dims.add(buildDim("经验匹配", experienceMatch));
        dims.add(buildDim("学历背景", educationMatch));
        dims.add(buildDim("行为表现", behaviorScore));
        dims.add(buildDim("语义匹配", semanticScore));
        result.setDimensions(dims);

        return result;
    }

    private DimensionRecord buildDim(String name, int score) {
        DimensionRecord dr = new DimensionRecord();
        dr.setName(name);
        dr.setScore(score);
        dr.setMaxScore(100);
        return dr;
    }

    // ─── 持久化 ───

    private void persistResult(Resume resume, AiScreeningResult result) {
        AiScreeningResult existing = resultMapper.selectLatestByResumeId(resume.getId());
        if (existing != null) {
            result.setId(existing.getId());
            resultMapper.updateById(result);
        } else {
            resultMapper.insert(result);
        }
        resume.setAiMatchScore(result.getOverallScore());
        // 筛选完成：按最低通过分数线自动落最终状态（已通过/已淘汰），
        // 避免状态一直停留在「筛选中」
        ScreenConfig cfg = loadScreenConfig();
        boolean passed = result.getOverallScore() != null
                && result.getOverallScore().doubleValue() >= cfg.passScore();
        resume.setScreeningStatus(passed
                ? RecruitmentEnums.ScreeningStatus.PASSED.getCode()
                : RecruitmentEnums.ScreeningStatus.REJECTED.getCode());
        resumeMapper.updateById(resume);
    }

    // ─── 批量筛选 ───

    /** 异步批量执行 AI 简历筛选。 */
    @Override
    public String batchScreenAsync(List<Long> resumeIds, Long jobId) {
        String taskId = UUID.randomUUID().toString().substring(0, 8);
        BatchProgress progress = new BatchProgress(taskId, resumeIds.size());
        batchProgressMap.put(taskId, progress);

        log.info("批量AI筛选已提交: taskId={}, count={}, jobId={}", taskId, resumeIds.size(), jobId);

        for (Long resumeId : resumeIds) {
            if (aiEngineExecutor != null) {
                aiEngineExecutor.execute(() -> executeScreeningTask(resumeId, progress));
            } else {
                // 无线程池时同步执行（测试环境）
                executeScreeningTask(resumeId, progress);
            }
        }

        return taskId;
    }

    private void executeScreeningTask(Long resumeId, BatchProgress progress) {
        try {
            Resume resume = resumeMapper.selectById(resumeId);
            if (resume == null) {
                progress.incrementFailed(String.valueOf(resumeId), "-", "简历不存在");
                return;
            }
            if (resume.getParsedContent() == null) {
                progress.incrementFailed(String.valueOf(resumeId), resume.getFileName(), "简历尚未解析完成");
                return;
            }
            resume.setScreeningStatus(RecruitmentEnums.ScreeningStatus.SCREENING.getCode());
            resumeMapper.updateById(resume);

            AiScreeningResult result = doScreen(resume);
            persistResult(resume, result);
            progress.incrementCompleted();
        } catch (Exception e) {
            log.error("批量筛选单份失败: resumeId={}", resumeId, e);
            String error = e.getMessage() != null
                    && e.getMessage().length() < 80 ? e.getMessage() : e.getClass().getSimpleName();
            progress.incrementFailed(String.valueOf(resumeId), "-", error);
        }
    }

    /** 查询批量 AI 筛选的进度。 */
    @Override
    public BatchProgressVO getBatchProgress(String taskId) {
        BatchProgress progress = batchProgressMap.get(taskId);
        if (progress == null) {
            BatchProgressVO notFound = new BatchProgressVO();
            notFound.setTaskId(taskId);
            notFound.setStatus("NOT_FOUND");
            return notFound;
        }
        BatchProgressVO result = new BatchProgressVO();
        result.setTaskId(taskId);
        result.setTotal(progress.total);
        result.setCompleted(progress.completed.get());
        result.setFailed(progress.failed.get());
        result.setFailedDetails(new ArrayList<>(progress.failedDetails));
        result.setStatus(progress.completed.get() + progress.failed.get() >= progress.total
                ? "COMPLETED" : "PROCESSING");

        // 完成后保留一段时间再清理
        if ("COMPLETED".equals(result.getStatus())) {
            new Thread(() -> {
                try { Thread.sleep(60000); } catch (InterruptedException ignored) {}
                batchProgressMap.remove(taskId);
            }).start();
        }
        return result;
    }

    // ─── 获取结果 ───

    /** 查询单个简历的 AI 筛选结果。 */
    @Override
    public AiScreeningResultVO getResult(Long resumeId) {
        AiScreeningResult entity = resultMapper.selectLatestByResumeId(resumeId);
        if (entity == null) return null;
        return toVO(entity);
    }

    private AiScreeningResultVO toVO(AiScreeningResult entity) {
        Resume resume = resumeMapper.selectById(entity.getResumeId());
        Candidate candidate = resume != null && resume.getCandidateId() != null
                ? candidateMapper.selectById(resume.getCandidateId()) : null;

        List<DimensionVO> dimensions = entity.getDimensions() != null
                ? entity.getDimensions().stream()
                .map(d -> DimensionVO.builder()
                        .name(d.getName())
                        .score(d.getScore())
                        .maxScore(d.getMaxScore() != null ? d.getMaxScore() : 100)
                        .build())
                .toList()
                : List.of();

        Map<String, Integer> dimensionScores = new LinkedHashMap<>();
        if (entity.getDimensions() != null) {
            for (DimensionRecord d : entity.getDimensions()) {
                dimensionScores.put(d.getName(), d.getScore());
            }
        }

        return AiScreeningResultVO.builder()
                .resumeId(entity.getResumeId())
                .candidateId(resume != null ? resume.getCandidateId() : null)
                .candidateName(candidate != null ? candidate.getName() : "未知")
                .overallScore(entity.getOverallScore() != null
                        ? entity.getOverallScore().intValue() : 0)
                .dimensionScores(dimensionScores)
                .matchedKeywords(entity.getMatchedKeywords())
                .missingKeywords(entity.getMissingKeywords())
                .suggestion(entity.getSuggestion())
                .summary(entity.getSummary())
                .strengths(entity.getStrengths())
                .weaknesses(entity.getWeaknesses())
                .recommendation(
                        "STRONG_HIRE".equals(entity.getSuggestion()) ? 1
                                : "HIRE".equals(entity.getSuggestion()) ? 2
                                : "CONSIDER".equals(entity.getSuggestion()) ? 3
                                : 4)
                .dimensions(dimensions)
                .build();
    }

    // ─── 工具方法 ───

    private static String truncateText(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }

    /**
     * 从解析内容的 skillsText 字段中提取技能列表。
     * 支持逗号、顿号、换行、分号分隔。
     */
    private List<String> extractSkillsFromText(ParsedResume content) {
        String text = content.getSkillsText();
        if (text == null || text.isEmpty()) return List.of();
        String[] parts = text.split("[,，、;；\\n\\r]+");
        List<String> skills = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty() && trimmed.length() < 50) {
                skills.add(trimmed);
            }
        }
        return skills;
    }

    /**
     * 从解析的学历信息中推断学历等级，结合学位、学校名、就读年限动态推断。
     * 0=高中, 1=大专, 2=本科, 3=硕士, 4=博士
     */
    private int parseEducationLevel(List<ParsedResume.EducationEntry> eduList) {
        if (eduList == null || eduList.isEmpty()) return 0;
        ParsedResume.EducationEntry first = eduList.get(0);
        String degree = first.getDegree() != null ? first.getDegree().toLowerCase() : "";
        String major = first.getMajor() != null ? first.getMajor() : "";
        String school = first.getSchool() != null ? first.getSchool() : "";

        // 博士
        if (degree.contains("博士") || degree.contains("phd") || degree.contains("doctor")) return 4;
        // 硕士
        if (degree.contains("硕士") || degree.contains("master") || degree.contains("研究生")) return 3;
        // 本科 (有专业也视为本科)
        if (degree.contains("本科") || degree.contains("学士") || degree.contains("bachelor")
                || !major.isEmpty()) return 2;
        // 大专
        if (degree.contains("大专") || degree.contains("专科") || degree.contains("associate")) return 1;
        // 高中
        if (degree.contains("高中") || degree.contains("中专")) return 0;

        // 无显式学历声明 → 从学校名和时间范围动态推断
        int duration = calcEduDuration(first.getStart(), first.getEnd());
        if (school.contains("研究生院")) return 3;
        if (school.contains("大学")) {
            return (duration >= 2 && duration <= 3) ? 3 : 2;
        }
        if (school.contains("学院")) {
            return duration >= 4 ? 2 : 1;
        }
        if (school.contains("专科") || school.contains("职业技术") || school.contains("高职")) return 1;
        if (school.contains("高中") || school.contains("中专") || school.contains("中学")) return 0;
        if (!school.isEmpty()) return 2; // 有学校名但无法归类 → 默认本科
        return 0;
    }

    private static int calcEduDuration(String start, String end) {
        int sy = extractYearFromDate(start);
        if (sy <= 0) return 0;
        int ey = extractYearFromDate(end);
        if (ey <= 0) ey = DateUtils.today().getYear();
        return Math.max(0, ey - sy);
    }

    private static int extractYearFromDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return 0;
        try {
            String cleaned = dateStr.replaceAll("[^0-9]", "");
            if (cleaned.length() >= 4) {
                return Integer.parseInt(cleaned.substring(0, 4));
            }
        } catch (NumberFormatException ignored) {}
        return 0;
    }

    /**
     * 批量筛选进度。
     */
    private static class BatchProgress {
        final String taskId;
        final int total;
        final java.util.concurrent.atomic.AtomicInteger completed = new java.util.concurrent.atomic.AtomicInteger(0);
        final java.util.concurrent.atomic.AtomicInteger failed = new java.util.concurrent.atomic.AtomicInteger(0);
        final List<String> failedDetails = new java.util.concurrent.CopyOnWriteArrayList<>();

        BatchProgress(String taskId, int total) {
            this.taskId = taskId;
            this.total = total;
        }

        void incrementCompleted() { completed.incrementAndGet(); }

        void incrementFailed(String resumeId, String candidateName, String error) {
            failed.incrementAndGet();
            failedDetails.add(String.format("简历 %s (%s): %s", resumeId, candidateName, error));
        }
    }
}
