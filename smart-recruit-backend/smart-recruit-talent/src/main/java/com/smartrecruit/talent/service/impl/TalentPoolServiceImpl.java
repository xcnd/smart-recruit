package com.smartrecruit.talent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.common.exception.ResourceNotFoundException;
import com.smartrecruit.talent.cache.TalentRecommendLocalCacheService;
import com.smartrecruit.talent.cache.TalentRecommendRedisCacheService;
import com.smartrecruit.talent.dto.remote.CandidateDTO;
import com.smartrecruit.talent.dto.remote.AiTalentRecommendDTO;
import com.smartrecruit.talent.dto.remote.JobDetailDTO;
import com.smartrecruit.talent.dto.response.TalentPoolVO;
import com.smartrecruit.talent.dto.response.RecommendationTaskVO;
import com.smartrecruit.talent.entity.TalentPool;
import com.smartrecruit.talent.feign.RecruitmentClient;
import com.smartrecruit.talent.feign.AiEngineClient;
import com.smartrecruit.talent.search.ElasticsearchTalentSearch;
import com.smartrecruit.talent.repository.TalentPoolMapper;
import com.smartrecruit.talent.service.TalentPoolService;
import com.smartrecruit.common.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 人才库管理服务实现。
 *
 * <p>通过 RecruitmentClient 跨服务获取候选人真实信息，
 * 消除 MVP 阶段的模拟数据生成逻辑。</p>
 *
 * @since 1.0.0
 */
@Service
@Slf4j
public class TalentPoolServiceImpl implements TalentPoolService {

    /** 异步 AI 推荐执行器（虚拟线程，不阻塞请求线程）。 */
    private static final Executor RECOMMEND_EXECUTOR =
            Executors.newVirtualThreadPerTaskExecutor();

    /** 异步推荐任务存储：taskId → 任务。 */
    private final ConcurrentHashMap<String, RecommendationTask> recommendationTasks =
            new ConcurrentHashMap<>();

    private final TalentPoolMapper talentPoolMapper;
    private final RecruitmentClient recruitmentClient;
    private final AiEngineClient aiEngineClient;
    private final ElasticsearchTalentSearch elasticsearchTalentSearch;
    private final TalentRecommendLocalCacheService recommendLocalCache;
    private final TalentRecommendRedisCacheService recommendRedisCache;

    /** 岗位推荐计算锁（防缓存击穿：同一岗位并发时只计算一次）。 */
    private final ConcurrentHashMap<Long, Object> recommendLocks = new ConcurrentHashMap<>();

    /** 预筛相关度阈值（0-100），低于该值的人才不进入推荐候选池。 */
    private static final int PRE_FILTER_THRESHOLD = 45;

    /** 预筛候选池上限：避免过多候选人进入 AI 匹配。 */
    private static final int MAX_PRE_FILTER_POOL = 30;

    /** 推荐人数下限：预筛不足时放宽阈值兜底，保证至少一批（8 人）。 */
    private static final int MIN_RECOMMEND_POOL = 8;

    public TalentPoolServiceImpl(TalentPoolMapper talentPoolMapper,
                                  RecruitmentClient recruitmentClient,
                                  AiEngineClient aiEngineClient,
                                  ElasticsearchTalentSearch elasticsearchTalentSearch,
                                  TalentRecommendLocalCacheService recommendLocalCache,
                                  TalentRecommendRedisCacheService recommendRedisCache) {
        this.talentPoolMapper = talentPoolMapper;
        this.recruitmentClient = recruitmentClient;
        this.aiEngineClient = aiEngineClient;
        this.elasticsearchTalentSearch = elasticsearchTalentSearch;
        this.recommendLocalCache = recommendLocalCache;
        this.recommendRedisCache = recommendRedisCache;
    }

    /** 分页查询记录列表，支持多条件筛选。 */
    @Override
    public PageResult<TalentPoolVO> pageQuery(Page<?> page, Map<String, Object> params) {
        Page<TalentPool> mpPage = new Page<>(page.getCurrent(), page.getSize());
        IPage<TalentPool> result = talentPoolMapper.selectPageWithFilters(mpPage, params);

        List<TalentPoolVO> vos = result.getRecords().stream()
                .map(this::toVO)
                .toList();

        return new PageResult<>(vos, result.getTotal(), result.getSize(), result.getCurrent(),
                (result.getTotal() + result.getSize() - 1) / result.getSize());
    }

    /** 按关键词搜索记录。 */
    @Override
    public List<TalentPoolVO> search(String keyword) {
        // ES 语义检索优先，失败自动降级本地语义排序
        try {
            List<TalentPool> esResults = elasticsearchTalentSearch.search(keyword, 50);
            if (esResults != null && !esResults.isEmpty()) {
                List<TalentPoolVO> esVos = esResults.stream().map(this::toVO).toList();
                log.info("ES 人才搜索: keyword='{}', results={}", keyword, esVos.size());
                return esVos;
            }
        } catch (Exception e) {
            log.warn("ES 人才搜索异常，降级本地语义搜索: keyword={}, error={}",
                    keyword, e.getMessage());
        }
        List<TalentPool> results = talentPoolMapper.searchByKeyword(keyword);
        List<TalentPoolVO> vos = results.stream()
                .sorted(Comparator.comparingInt(t -> -semanticScore(t, keyword)))
                .map(this::toVO)
                .toList();
        log.info("Talent search: keyword='{}', results={}", keyword, vos.size());
        return vos;
    }

    /** 语义相关性评分：字段命中 + 2-gram 重合 + 技能同义词加权。 */
    private int semanticScore(TalentPool talent, String keyword) {
        if (keyword == null || keyword.isBlank()) return 0;
        String kw = keyword.trim().toLowerCase();
        String[] fields = {
                talent.getTags(), talent.getAiTags(), talent.getExpectedPosition(),
                talent.getExpectedLocation(), talent.getNote(),
        };
        int score = 0;
        for (String field : fields) {
            if (field == null) continue;
            String lower = field.toLowerCase();
            if (lower.contains(kw)) score += 12;
            score += ngramOverlap(lower, kw) * 2;
            if (kw.length() >= 3) {
                for (Map.Entry<String, List<String>> entry : SYNONYMS.entrySet()) {
                    if (kw.contains(entry.getKey()) || entry.getKey().contains(kw)) {
                        for (String syn : entry.getValue()) {
                            if (lower.contains(syn)) {
                                score += 6;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return score;
    }

    /** 2-gram 重合度（对中文短词做近似匹配）。 */
    private int ngramOverlap(String text, String keyword) {
        int hits = 0;
        for (int i = 0; i + 1 < keyword.length(); i++) {
            if (text.contains(keyword.substring(i, i + 2))) hits++;
        }
        return hits;
    }

    /** 技能/领域同义词表，用于语义扩展。 */
    private static final Map<String, List<String>> SYNONYMS = Map.of(
            "java", List.of("j2ee", "spring", "jvm", "spring boot", "微服务"),
            "python", List.of("django", "flask", "数据分析"),
            "前端", List.of("vue", "react", "javascript", "typescript", "html", "css"),
            "后端", List.of("java", "go", "spring", "微服务"),
            "大数据", List.of("spark", "flink", "hadoop", "hive"),
            "运维", List.of("linux", "docker", "kubernetes", "k8s", "ci/cd"),
            "数据库", List.of("mysql", "redis", "oracle", "postgresql"),
            "算法", List.of("机器学习", "深度学习", "nlp", "推荐系统"));

    /**
     * 查询推荐结果（两级缓存优先）。
     *
     * <p>读取顺序：本地缓存（Caffeine，5 分钟）→ Redis（15 分钟）→ 实时计算；
     * 实时计算结果回写两级缓存。同一岗位并发请求通过按岗位加锁，
     * 防止缓存击穿导致重复调用大模型。</p>
     */
    @Override
    public List<TalentPoolVO> getRecommendations(Long jobId) {
        Object lock = recommendLocks.computeIfAbsent(jobId, k -> new Object());
        synchronized (lock) {
            // L1：本地缓存（5 分钟）
            List<TalentPoolVO> local = recommendLocalCache.get(jobId);
            if (local != null) {
                log.info("命中人才推荐本地缓存: jobId={}, matched={}", jobId, local.size());
                return local;
            }
            // L2：Redis 缓存（15 分钟），命中后回填本地缓存
            List<TalentPoolVO> redis = recommendRedisCache.get(jobId);
            if (redis != null) {
                log.info("命中人才推荐 Redis 缓存: jobId={}, matched={}", jobId, redis.size());
                recommendLocalCache.put(jobId, redis);
                return redis;
            }
            // L3：实时计算，并回写两级缓存
            List<TalentPoolVO> results = computeRecommendations(jobId);
            recommendLocalCache.put(jobId, results);
            recommendRedisCache.put(jobId, results);
            return results;
        }
    }

    /** 实时计算推荐结果（内部方法，缓存由 {@link #getRecommendations} 统一管理）。 */
    private List<TalentPoolVO> computeRecommendations(Long jobId) {
        // 1. 拉取岗位要求
        JobDetailDTO job = fetchJob(jobId);
        if (job == null || job.getSkills() == null || job.getSkills().isEmpty()) {
            log.warn("岗位不存在或无技能要求: jobId={}", jobId);
            return List.of();
        }

        // 2. 拉取人才库活跃记录，并预筛「同部门方向 + 类似岗位」的候选人，
        //    避免把全部人才（含大量无关岗位）送入 AI 匹配
        LambdaQueryWrapper<TalentPool> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TalentPool::getDeleted, 0);
        List<TalentPool> allTalents = talentPoolMapper.selectList(wrapper);

        if (allTalents.isEmpty()) {
            return List.of();
        }
        List<TalentPoolScored> prePool = preFilterCandidates(job, allTalents);
        if (prePool.isEmpty()) {
            log.warn("预筛后无相关人才: jobId={}, jobTitle={}", jobId, job.getTitle());
            return List.of();
        }
        log.info("人才预筛完成: jobId={}, all={}, relevant={}",
                jobId, allTalents.size(), prePool.size());

        // AI 推荐优先（LLM 排序），失败时回退规则算法
        List<TalentPoolVO> aiResults = recommendWithAi(job, prePool);
        if (aiResults != null && !aiResults.isEmpty()) {
            log.info("AI 人才推荐完成: jobId={}, jobTitle={}, matched={}",
                    jobId, job.getTitle(), aiResults.size());
            return aiResults;
        }
        log.info("AI 推荐不可用，回退规则算法: jobId={}", jobId);

        // 3. 对预筛后的候选池计算规则匹配分并更新实体
        List<TalentPoolScored> scored = new ArrayList<>();
        for (TalentPoolScored pre : prePool) {
            TalentPool talent = pre.talent;
            CandidateDTO candidate = pre.candidate;
            int score = computeMatchScore(job, talent, candidate);
            talent.setMatchScore(score);
            talent.setMatchedPositionId(jobId);
            talentPoolMapper.updateById(talent);
            scored.add(new TalentPoolScored(talent, candidate, score));
        }

        // 4. 排序取 Top 24（前端按每批 8 人分页「换一批」）
        scored.sort((a, b) -> Integer.compare(b.score, a.score));
        List<TalentPoolVO> vos = scored.stream()
                .limit(24)
                .map(s -> toVO(s.talent, s.candidate))
                .collect(Collectors.toList());

        log.info("推荐完成: jobId={}, jobTitle={}, matched={}", jobId, job.getTitle(), vos.size());
        return vos;
    }

    /** 启动异步 AI 人才推荐。 */
    @Override
    public String startAsyncRecommendation(Long jobId) {
        if (recommendationTasks.size() > 500) {
            recommendationTasks.clear();
        }
        String taskId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        RecommendationTask task = new RecommendationTask();
        recommendationTasks.put(taskId, task);
        RECOMMEND_EXECUTOR.execute(() -> {
            try {
                task.status = "RUNNING";
                List<TalentPoolVO> results = getRecommendations(jobId);
                task.results = results == null ? List.of() : results;
                task.status = "COMPLETED";
                log.info("异步 AI 推荐完成: taskId={}, jobId={}, matched={}",
                        taskId, jobId, task.results.size());
            } catch (Exception e) {
                task.status = "FAILED";
                task.message = e.getMessage() == null ? "推荐失败" : e.getMessage();
                log.error("异步 AI 推荐失败: taskId={}, jobId={}", taskId, jobId, e);
            }
        });
        return taskId;
    }

    /**
     * 查询异步推荐任务状态与结果（分页）。
     *
     * <p>匹配完成后按每批人数切分推荐结果，供前端「换一批」分页展示。</p>
     */
    @Override
    public RecommendationTaskVO getAsyncRecommendation(String taskId, int page, int size) {
        RecommendationTask task = recommendationTasks.get(taskId);
        if (task == null) {
            throw new ResourceNotFoundException("推荐任务不存在: taskId=" + taskId);
        }
        int pageSize = Math.max(1, Math.min(size, 50));
        int pageNum = Math.max(1, page);
        List<TalentPoolVO> all = task.results == null ? List.of() : task.results;
        int from = (pageNum - 1) * pageSize;
        List<TalentPoolVO> pageResults = from >= all.size()
                ? List.of()
                : all.subList(from, Math.min(from + pageSize, all.size()));
        return RecommendationTaskVO.builder()
                .taskId(taskId)
                .status(task.status)
                .message(task.message)
                .results(pageResults)
                .total(all.size())
                .page(pageNum)
                .size(pageSize)
                .build();
    }

    /** 异步推荐任务内部对象。 */
    private static class RecommendationTask {
        private volatile String status = "PENDING";
        private volatile String message = "";
        private volatile List<TalentPoolVO> results = List.of();
    }

    /**
     * 推荐候选池预筛：仅保留与目标岗位「同部门方向 + 类似岗位」的人才。
     *
     * <p>预筛维度（满分 100）：部门/岗位方向 40 分、职位关键词相似度 25 分、
     * 技能重叠 25 分、工作地点 10 分。过滤后不足 {@link #MIN_RECOMMEND_POOL} 人时，
     * 放宽阈值取预筛分最高的兜底人数，避免极端数据下无推荐结果。</p>
     *
     * @param job        目标岗位
     * @param allTalents 人才库全部活跃记录
     * @return 预筛后的候选池（按预筛分降序，携带候选人信息）
     */
    private List<TalentPoolScored> preFilterCandidates(JobDetailDTO job, List<TalentPool> allTalents) {
        List<TalentPoolScored> pool = new ArrayList<>();
        for (TalentPool talent : allTalents) {
            CandidateDTO candidate = fetchCandidate(talent.getCandidateId());
            if (candidate == null) {
                continue;
            }
            pool.add(new TalentPoolScored(talent, candidate,
                    computePreFilterScore(job, talent, candidate)));
        }
        pool.sort((a, b) -> Integer.compare(b.score, a.score));

        List<TalentPoolScored> relevant = pool.stream()
                .filter(p -> p.score >= PRE_FILTER_THRESHOLD)
                .limit(MAX_PRE_FILTER_POOL)
                .toList();
        if (relevant.size() < MIN_RECOMMEND_POOL && pool.size() >= MIN_RECOMMEND_POOL) {
            log.info("预筛相关人才不足 {} 人，放宽阈值兜底: jobId={}, keep={}",
                    MIN_RECOMMEND_POOL, job.getId(), MIN_RECOMMEND_POOL);
            return new ArrayList<>(pool.subList(0, MIN_RECOMMEND_POOL));
        }
        return relevant;
    }

    /**
     * 预筛相关度评分（0-100）。
     *
     * <p>通过岗位方向类别、职位关键词、技能、地点四个维度
     * 快速判断候选人与目标岗位是否属于同一部门/相近岗位。</p>
     */
    private int computePreFilterScore(JobDetailDTO job, TalentPool talent, CandidateDTO candidate) {
        String currentPosition = currentPositionOf(candidate);

        // 1. 部门/岗位方向（技术、产品、设计、HR、市场、财务等）40 分
        int directionScore = departmentDirectionScore(job.getTitle(), currentPosition);

        // 2. 职位关键词相似度 25 分：期望职位与当前职位取较高值
        int posExpected = computePositionScore(job.getTitle(), talent.getExpectedPosition());
        int posCurrent = computePositionScore(job.getTitle(), currentPosition);
        int posScore = Math.max(posExpected, posCurrent);

        // 3. 技能重叠 25 分
        int skillScore = computeSkillScore(job.getSkills(), candidate.getSkills());

        // 4. 工作地点 10 分：候选人在库期望地点与岗位地点
        int locScore = computeLocationScore(job.getLocation(), talent.getExpectedLocation());

        return (int) Math.round(directionScore * 0.40
                + posScore * 0.25
                + skillScore * 0.25
                + locScore * 0.10);
    }

    /**
     * 部门/岗位方向匹配分（0-100）：岗位与候选人同属一个职能大类时高分，
     * 明显跨方向（如技术岗候选人 vs 产品岗）时低分，避免误推荐。
     */
    private int departmentDirectionScore(String jobTitle, String candidatePosition) {
        String jobCategory = categoryOf(jobTitle);
        String candCategory = categoryOf(candidatePosition);
        if (jobCategory.equals(candCategory)) {
            return 100;
        }
        // 数据/技术同属技术体系，允许互通
        if (isTechCategory(jobCategory) && isTechCategory(candCategory)) {
            return 90;
        }
        return switch (jobCategory) {
            case "TECH" -> candCategory.equals("PRODUCT") ? 15 : 35;
            case "PRODUCT" -> candCategory.equals("TECH") ? 15 : 35;
            case "DESIGN", "HR", "MARKET", "FINANCE" -> 35;
            default -> 50;
        };
    }

    /** 是否属于技术职能大类（技术/数据/运维等）。 */
    private boolean isTechCategory(String category) {
        return "TECH".equals(category) || "DATA".equals(category);
    }

    /**
     * 职能大类归类：技术、数据、产品、设计、HR、市场、财务。
     */
    private String categoryOf(String text) {
        String t = text == null ? "" : text.toLowerCase();
        if (containsAny(t,
                "开发", "工程师", "java", "后端", "前端", "算法", "测试", "运维",
                "架构", "移动", "android", "ios", "devops", "sre", "dev", "engineer")) {
            return "TECH";
        }
        if (containsAny(t, "数据", "etl", "数仓", "大数据", "数据分析")) {
            return "DATA";
        }
        if (containsAny(t, "产品", "运营", "pm", "product")) {
            return "PRODUCT";
        }
        if (containsAny(t, "设计", "ui", "ux", "视觉", "交互")) {
            return "DESIGN";
        }
        if (containsAny(t, "hr", "招聘", "人事", "人力资源")) {
            return "HR";
        }
        if (containsAny(t, "市场", "销售", "营销", "品牌", "商务")) {
            return "MARKET";
        }
        if (containsAny(t, "财务", "会计", "审计")) {
            return "FINANCE";
        }
        return "OTHER";
    }

    /**
     * 调用 AI 引擎做人才推荐（LLM 优先、Agent 兜底），失败返回 null。
     *
     * @param job 目标岗位
     * @param pool 预筛后的候选池（已携带候选人信息，避免重复跨服务拉取）
     */
    private List<TalentPoolVO> recommendWithAi(JobDetailDTO job, List<TalentPoolScored> pool) {
        try {
            AiTalentRecommendDTO.JobReq jobReq = new AiTalentRecommendDTO.JobReq(
                    job.getTitle(),
                    job.getSkills(),
                    minYearsOfExperience(job.getLevel()),
                    educationLevelOf(job.getEducationRequired()));

            List<AiTalentRecommendDTO.Candidate> candidates = new ArrayList<>();
            Map<Long, CandidateDTO> candidateMap = new LinkedHashMap<>();
            for (TalentPoolScored pre : pool) {
                TalentPool talent = pre.talent;
                CandidateDTO candidate = pre.candidate;
                if (candidate == null || candidate.getId() == null) {
                    continue;
                }
                candidateMap.put(candidate.getId(), candidate);
                candidates.add(new AiTalentRecommendDTO.Candidate(
                        candidate.getId(),
                        candidate.getName(),
                        candidate.getSkills(),
                        candidate.getYearsOfExperience(),
                        educationLevelOf(candidate.getEducation()),
                        candidate.getCurrentCompany(),
                        currentPositionOf(candidate),
                        null));
            }
            if (candidates.isEmpty()) {
                return null;
            }

            ApiResponse<List<AiTalentRecommendDTO.Result>> resp =
                    aiEngineClient.recommendTalent(
                            new AiTalentRecommendDTO.Request(jobReq, candidates));
            List<AiTalentRecommendDTO.Result> results =
                    resp != null ? resp.data() : null;
            if (results == null || results.isEmpty()) {
                return null;
            }

            List<TalentPoolVO> vos = new ArrayList<>();
            for (AiTalentRecommendDTO.Result r : results) {
                TalentPool talent = pool.stream()
                        .map(TalentPoolScored::talent)
                        .filter(t -> t.getCandidateId() != null
                                && t.getCandidateId().equals(r.candidateId()))
                        .findFirst()
                        .orElse(null);
                CandidateDTO candidate = candidateMap.get(r.candidateId());
                if (talent == null || candidate == null) {
                    continue;
                }
                int score = r.matchScore() != null
                        ? (int) Math.round(r.matchScore()) : 0;
                talent.setMatchScore(score);
                talent.setMatchedPositionId(job.getId());
                try {
                    talentPoolMapper.updateById(talent);
                } catch (Exception e) {
                    log.warn("更新人才匹配分失败（可忽略）: talentId={}", talent.getId());
                }
                TalentPoolVO vo = toVO(talent, candidate);
                vo.setMatchDimensions(r.matchDimensions());
                vos.add(vo);
            }
            return vos;
        } catch (Exception e) {
            log.warn("AI 人才推荐调用失败: jobId={}, error={}",
                    job.getId(), e.getMessage());
            return null;
        }
    }

    /** 学历编码 → 字符串（ASSOCIATE/BACHELOR/MASTER/PHD）。 */
    private String educationLevelOf(Integer education) {
        if (education == null) return "BACHELOR";
        return switch (education) {
            case 1 -> "ASSOCIATE";
            case 3 -> "MASTER";
            case 4 -> "PHD";
            default -> "BACHELOR";
        };
    }

    /** 经验等级编码 → 最低年限。 */
    private Integer minYearsOfExperience(Integer level) {
        if (level == null) return null;
        return switch (level) {
            case 0 -> 0;
            case 1 -> 1;
            case 2 -> 3;
            case 3 -> 5;
            case 4 -> 8;
            case 5 -> 10;
            default -> null;
        };
    }

    // ---- 匹配算法 ----

    /**
     * 五维匹配度计算。
     */
    private int computeMatchScore(JobDetailDTO job, TalentPool talent, CandidateDTO candidate) {
        if (candidate == null) return 0;

        int skillScore = computeSkillScore(job.getSkills(), candidate.getSkills());
        int eduScore = computeEducationScore(job.getEducationRequired(), candidate.getEducation());
        int expScore = computeExperienceScore(job.getLevel(), candidate.getYearsOfExperience());
        int posScore = computePositionScore(job.getTitle(), talent.getExpectedPosition());
        int locScore = computeLocationScore(job.getLocation(), talent.getExpectedLocation());
        int roleScore = computeRoleScore(job.getTitle(), currentPositionOf(candidate));

        return (int) Math.round(skillScore * 0.40 + roleScore * 0.20
                + eduScore * 0.15 + expScore * 0.15 + posScore * 0.05 + locScore * 0.05);
    }

    /**
     * 解析候选人的当前职位：优先取 currentPosition（最近任职岗位），
     * 为空时回退投递岗位 jobTitle，避免人才库候选人无简历导致职位为空。
     */
    private String currentPositionOf(CandidateDTO candidate) {
        if (candidate == null) return null;
        String currentPosition = candidate.getCurrentPosition();
        return currentPosition != null && !currentPosition.isBlank()
                ? currentPosition
                : candidate.getJobTitle();
    }

    /** 岗位角色匹配：区分技术岗与产品/运营岗，避免方向不符被误推荐。 */
    private int computeRoleScore(String jobTitle, String candidatePosition) {
        String job = jobTitle == null ? "" : jobTitle.toLowerCase();
        String pos = candidatePosition == null ? "" : candidatePosition.toLowerCase();
        boolean jobIsProduct = containsAny(job,
                "产品经理", "产品", "pm", "product", "运营", "市场", "销售", "设计", "hr");
        boolean jobIsTech = containsAny(job,
                "开发", "工程师", "后端", "前端", "java", "算法", "测试", "运维", "数据", "架构");
        boolean candIsProduct = containsAny(pos,
                "产品经理", "产品", "pm", "产品运营", "用户研究", "需求分析");
        boolean candIsTech = containsAny(pos,
                "开发", "工程师", "后端", "前端", "java", "算法", "测试", "运维", "架构");
        if (jobIsProduct && !jobIsTech) {
            return candIsProduct ? 100 : (candIsTech ? 20 : 55);
        }
        if (jobIsTech && !jobIsProduct) {
            return candIsTech ? 100 : 45;
        }
        return 80;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    /** 技能匹配：Jaccard 相似度 (50%) */
    private int computeSkillScore(List<String> jobSkills, List<String> candidateSkills) {
        if (jobSkills == null || jobSkills.isEmpty()) return 0;
        if (candidateSkills == null || candidateSkills.isEmpty()) return 0;

        Set<String> jobSet = jobSkills.stream()
                .map(String::toLowerCase).collect(Collectors.toSet());
        long overlap = candidateSkills.stream()
                .map(String::toLowerCase)
                .filter(jobSet::contains)
                .count();

        return (int) Math.round((double) overlap / jobSkills.size() * 100);
    }

    /** 学历匹配 (15%) */
    private int computeEducationScore(Integer required, Integer actual) {
        if (required == null) return 100;
        if (actual == null) return 50;
        if (actual >= required) return 100;
        int diff = required - actual;
        if (diff == 1) return 70;
        if (diff == 2) return 40;
        return 10;
    }

    /** 经验匹配 (15%) — 岗位 level 映射年限要求 */
    private int computeExperienceScore(Integer jobLevel, Integer yearsOfExperience) {
        if (jobLevel == null) return 100;
        int requiredYears = switch (jobLevel) {
            case 0 -> 0;   // ENTRY
            case 1 -> 1;   // JUNIOR
            case 2 -> 3;   // MID
            case 3 -> 5;   // SENIOR
            case 4 -> 8;   // LEAD
            default -> 0;
        };
        if (yearsOfExperience == null) return 30;
        if (yearsOfExperience >= requiredYears) return 100;
        int diff = requiredYears - yearsOfExperience;
        if (diff <= 1) return 70;
        if (diff <= 3) return 40;
        return 10;
    }

    /** 职位匹配 (10%) — 关键词重叠 */
    private int computePositionScore(String jobTitle, String expectedPosition) {
        if (jobTitle == null || expectedPosition == null) return 50;
        Set<String> jobWords = extractKeywords(jobTitle);
        Set<String> posWords = extractKeywords(expectedPosition);
        if (jobWords.isEmpty()) return 50;
        long overlap = posWords.stream().filter(jobWords::contains).count();
        return (int) Math.round((double) overlap / jobWords.size() * 100);
    }

    /** 地点匹配 (10%) */
    private int computeLocationScore(String jobLocation, String expectedLocation) {
        if (jobLocation == null || expectedLocation == null) return 50;
        String jl = jobLocation.toLowerCase();
        String el = expectedLocation.toLowerCase();
        if (jl.equals(el)) return 100;
        if (jl.contains(el) || el.contains(jl)) return 80;
        return 20;
    }

    private Set<String> extractKeywords(String text) {
        return Arrays.stream(text.toLowerCase()
                        .replaceAll("[()（）\\-、/]", " ")
                        .split("\\s+"))
                .filter(w -> w.length() >= 2)
                .collect(Collectors.toSet());
    }

    // ---- Feign 辅助方法 ----

    private JobDetailDTO fetchJob(Long jobId) {
        try {
            ApiResponse<JobDetailDTO> resp = recruitmentClient.getJob(jobId);
            return resp != null ? resp.data() : null;
        } catch (Exception e) {
            log.warn("获取岗位信息失败: jobId={}, error={}", jobId, e.getMessage());
            return null;
        }
    }

    private CandidateDTO fetchCandidate(Long candidateId) {
        try {
            ApiResponse<CandidateDTO> resp = recruitmentClient.getCandidate(candidateId);
            return resp != null ? resp.data() : null;
        } catch (Exception e) {
            log.debug("获取候选人信息失败: candidateId={}", candidateId);
            return null;
        }
    }

    /** 更新人才库候选人标签。 */
    @Override
    @Transactional
    public void updateTags(Long id, List<String> tags) {
        TalentPool talent = talentPoolMapper.selectById(id);
        if (talent == null) {
            throw new ResourceNotFoundException("TalentPool", id);
        }
        talent.setTags(toJson(tags));
        talentPoolMapper.updateById(talent);
        log.info("Talent tags updated: id={}, tags={}", id, tags);
    }

    /** 将候选人加入人才库。 */
    @Override
    @Transactional
    public TalentPoolVO addToPool(Long candidateId, List<String> tags) {
        // 检查候选人是否已在人才库中
        LambdaQueryWrapper<TalentPool> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TalentPool::getCandidateId, candidateId)
               .eq(TalentPool::getDeleted, 0);
        TalentPool existing = talentPoolMapper.selectOne(wrapper);
        if (existing != null) {
            TalentPoolVO existingVO = toVO(existing);
            log.info("候选人已在人才库中: candidateId={}, talentPoolId={}", candidateId, existingVO.getId());
            return existingVO;
        }

        // 从招聘服务获取候选人信息
        CandidateDTO candidate = null;
        try {
            ApiResponse<CandidateDTO> resp = recruitmentClient.getCandidate(candidateId);
            if (resp != null && resp.data() != null) {
                candidate = resp.data();
            }
        } catch (Exception e) {
            log.warn("获取候选人信息失败，使用最小化数据: candidateId={}, error={}", candidateId, e.getMessage());
        }

        TalentPool entity = new TalentPool();
        entity.setCandidateId(candidateId);
        entity.setPoolType(0); // 默认为通用人才池
        entity.setSkillLevel(skillLevelFromExperience(candidate));
        entity.setAvailability(0); // 默认 ACTIVE
        entity.setStatus(0); // AVAILABLE
        entity.setTags(toJson(tags != null ? tags : List.of()));
        entity.setLastActiveTime(DateUtils.now());

        if (candidate != null) {
            entity.setMatchScore(candidate.getAiMatchScore());
            entity.setExpectedPosition(currentPositionOf(candidate));
        }

        talentPoolMapper.insert(entity);
        log.info("候选人已添加到人才库: candidateId={}, talentPoolId={}", candidateId, entity.getId());
        return toVO(entity);
    }

    /** 标记候选人为已联系。 */
    @Override
    @Transactional
    public void contactCandidate(Long id) {
        TalentPool talent = talentPoolMapper.selectById(id);
        if (talent == null) {
            throw new ResourceNotFoundException("TalentPool", id);
        }
        talent.setLastContactTime(DateUtils.now());
        talent.setStatus(1); // CONTACTED
        talentPoolMapper.updateById(talent);
        log.info("已联系候选人: talentPoolId={}", id);
    }

    // ---- 私有辅助方法 ----

    /**
     * 将实体转换为 VO，会跨服务获取候选人真实信息。
     */
    private TalentPoolVO toVO(TalentPool entity) {
        CandidateDTO candidate = fetchCandidate(entity.getCandidateId());
        return buildVO(entity, candidate);
    }

    /** 使用预取好的候选人数据构建 VO，避免重复 HTTP 调用。 */
    private TalentPoolVO toVO(TalentPool entity, CandidateDTO candidate) {
        return buildVO(entity, candidate);
    }

    private TalentPoolVO buildVO(TalentPool entity, CandidateDTO candidate) {

        String candidateName = "";
        String email = "";
        String phone = "";
        String education = "";
        Integer experience = null;
        String currentCompany = "";
        String lastPosition = "";
        List<String> skills = List.of();
        List<String> aiTags = List.of();
        Integer matchScore = entity.getMatchScore();

        if (candidate != null) {
            candidateName = candidate.getName() != null ? candidate.getName() : "";
            email = candidate.getEmail() != null ? candidate.getEmail() : "";
            phone = candidate.getPhone() != null ? candidate.getPhone() : "";
            education = mapEducation(candidate.getEducation());
            experience = candidate.getYearsOfExperience();
            currentCompany = candidate.getCurrentCompany() != null ? candidate.getCurrentCompany() : "";
            lastPosition = currentPositionOf(candidate);
            skills = candidate.getSkills() != null ? candidate.getSkills() : List.of();
            aiTags = candidate.getTags() != null ? candidate.getTags() : List.of();
            if (matchScore == null) {
                matchScore = candidate.getAiMatchScore();
            }
        }

        return TalentPoolVO.builder()
                .id(entity.getId())
                .candidateId(entity.getCandidateId())
                .candidateName(candidateName)
                .email(email)
                .phone(phone)
                .education(education)
                .experience(experience)
                .lastPosition(lastPosition)
                .currentCompany(currentCompany)
                .matchScore(matchScore)
                .skills(skills)
                .tags(toList(entity.getTags()))
                .aiTags(aiTags)
                .source(candidate != null ? candidate.getSource() : null)
                .poolType(entity.getPoolType())
                .skillLevel(entity.getSkillLevel())
                .availability(entity.getAvailability())
                .expectedPosition(entity.getExpectedPosition())
                .expectedLocation(entity.getExpectedLocation())
                .expectedSalaryMin(entity.getExpectedSalaryMin())
                .expectedSalaryMax(entity.getExpectedSalaryMax())
                .status(entity.getStatus())
                .aiScore(matchScore)
                .lastActiveAt(entity.getLastActiveTime() != null
                        ? entity.getLastActiveTime()
                        : entity.getLastContactTime())
                .lastContactAt(entity.getLastContactTime())
                .createdAt(entity.getCreateTime())
                .build();
    }

    /**
     * 学历 Integer → String 映射。
     */
    private String mapEducation(Integer edu) {
        if (edu == null) return "";
        return switch (edu) {
            case 1 -> "大专";
            case 2 -> "本科";
            case 3 -> "硕士";
            case 4 -> "博士";
            default -> "";
        };
    }

    /**
     * 根据候选人工作年限推导技能等级。
     */
    private Integer skillLevelFromExperience(CandidateDTO candidate) {
        if (candidate == null || candidate.getYearsOfExperience() == null) return 1;
        int years = candidate.getYearsOfExperience();
        if (years < 2) return 0;   // BASIC
        if (years < 5) return 1;   // INTERMEDIATE
        if (years < 8) return 2;   // ADVANCED
        return 3;                   // EXPERT
    }

    @SuppressWarnings("unchecked")
    private List<String> toList(Object obj) {
        if (obj instanceof List<?> list) {
            return (List<String>) list.stream().map(Object::toString).toList();
        }
        if (obj instanceof String str && !str.isBlank()) {
            return parseTags(str.trim());
        }
        return List.of();
    }

    /** 从可能的 JSON/转义字符串中提取标签：激进清洗后逗号分割。 */
    private static List<String> parseTags(String s) {
        // 去除所有 JSON/转义噪声字符：\ " [ ]
        String cleaned = s.replace("\\", "").replace("\"", "")
                .replace("[", "").replace("]", "");
        if (cleaned.isBlank()) return List.of();
        return Arrays.stream(cleaned.split(","))
                .map(String::trim)
                .filter(x -> !x.isEmpty())
                .toList();
    }

    /** 标签列表序列化为逗号分隔字符串。 */
    private static String toJson(List<String> list) {
        if (list == null || list.isEmpty()) return "";
        return String.join(",", list);
    }

    /** 推荐排序辅助记录 —— 携匹配分和预取的候选人数据进行排序。 */
    private record TalentPoolScored(TalentPool talent, CandidateDTO candidate, int score) {}
}
