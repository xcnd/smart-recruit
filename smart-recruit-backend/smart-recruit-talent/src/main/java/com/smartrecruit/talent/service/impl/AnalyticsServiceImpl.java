package com.smartrecruit.talent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.talent.dto.response.*;
import com.smartrecruit.talent.entity.AnalyticsCandidateDaily;
import com.smartrecruit.talent.entity.AnalyticsInterviewDaily;
import com.smartrecruit.talent.entity.AnalyticsOfferDaily;
import com.smartrecruit.talent.feign.InterviewClient;
import com.smartrecruit.talent.feign.OfferClient;
import com.smartrecruit.talent.feign.RecruitmentClient;
import com.smartrecruit.talent.feign.AiEngineClient;
import com.smartrecruit.talent.repository.AnalyticsCandidateDailyMapper;
import com.smartrecruit.talent.repository.AnalyticsInterviewDailyMapper;
import com.smartrecruit.talent.repository.AnalyticsOfferDailyMapper;
import com.smartrecruit.talent.dto.remote.AiAnalyticsDTO;
import com.smartrecruit.talent.cache.AnalyticsInsightCacheService;
import com.smartrecruit.talent.service.AnalyticsDailySyncService;
import com.smartrecruit.talent.service.AnalyticsService;
import com.smartrecruit.common.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * 招聘分析服务实现（定时任务预聚合版）。
 *
 * <p>招聘、面试、Offer、入职各模块由定时任务把明细数据聚合成「日汇总表」，
 * 本服务查询时只读取日汇总（每天最多几百行）并在内存中求和，
 * 避免对业务大表实时 GROUP BY，显著降低大数据量下的查询压力。</p>
 *
 * @since 1.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    /** 平均岗位年化价值（元），用于渠道 ROI 估算。 */
    private static final double AVERAGE_ANNUAL_VALUE = 400_000.0;

    /** 招聘周期目标（天）。 */
    private static final int CYCLE_TARGET_DAYS = 25;

    /** Offer 接受率行业基准（%）。 */
    private static final double ACCEPTANCE_BENCHMARK = 75.0;

    /** 候选人已入职阶段码（与招聘模块 CandidateStatus.HIRED 一致）。 */
    private static final int STAGE_HIRED = 5;

    /** 招聘漏斗正向转化链路（终止态「已淘汰/已放弃」不参与漏斗转化）。 */
    private static final List<String> FUNNEL_STAGE_NAMES = List.of(
            "新入库", "筛选中", "筛选通过", "面试中", "已发Offer", "已入职");

    /** 渠道人均投放成本估算（元/人）：0=主动投递,1=内推,2=官网,3=LinkedIn,4=BOSS,5=拉勾,6=猎聘,7=其他。 */
    private static final Map<Integer, Double> CHANNEL_COST_PER_HIRE = Map.of(
            0, 0.0, 1, 500.0, 2, 0.0, 3, 1500.0, 4, 800.0, 5, 600.0, 6, 2000.0, 7, 0.0);

    /** 渠道名称兜底映射。 */
    private static final Map<Integer, String> CHANNEL_NAMES = Map.of(
            0, "主动投递", 1, "内推", 2, "官网", 3, "LinkedIn",
            4, "BOSS直聘", 5, "拉勾", 6, "猎聘", 7, "其他");

    private final RecruitmentClient recruitmentClient;
    private final InterviewClient interviewClient;
    private final OfferClient offerClient;
    private final AiEngineClient aiEngineClient;
    private final AnalyticsCandidateDailyMapper candidateDailyMapper;
    private final AnalyticsOfferDailyMapper offerDailyMapper;
    private final AnalyticsInterviewDailyMapper interviewDailyMapper;
    private final AnalyticsDailySyncService analyticsDailySyncService;
    private final AnalyticsInsightCacheService analyticsInsightCache;

    /** AI 洞察异步生成执行器（虚拟线程，不阻塞数据分析请求）。 */
    private static final Executor INSIGHT_EXECUTOR =
            Executors.newVirtualThreadPerTaskExecutor();

    /** 查询聚合概览数据。 */
    @Override
    public AnalyticsOverviewVO getOverview(LocalDate startDate, LocalDate endDate) {
        LocalDate[] range = normalizeRange(startDate, endDate);
        LocalDate start = range[0];
        LocalDate endExclusive = range[1];
        long periodDays = DateUtils.daysBetween(start, endExclusive);
        LocalDate prevStart = start.minusDays(Math.max(1, periodDays));
        LocalDate prevEndExclusive = start;

        // 趋势周期：近 12 个月（含当月）
        LocalDate trendStart = DateUtils.currentYearMonth().minusMonths(11).atDay(1);
        LocalDate trendEndExclusive = DateUtils.today().plusDays(1);

        // 并行拉取日汇总（每天最多几百行）
        List<Map<String, Object>> candDaily = fetchCandidateDaily(start, endExclusive);
        List<Map<String, Object>> offerDaily = fetchOfferDaily(start, endExclusive);
        List<Map<String, Object>> interviewDaily = fetchInterviewDaily(start, endExclusive);
        List<Map<String, Object>> prevCandDaily = fetchCandidateDaily(prevStart, prevEndExclusive);
        List<Map<String, Object>> prevOfferDaily = fetchOfferDaily(prevStart, prevEndExclusive);
        List<Map<String, Object>> prevInterviewDaily = fetchInterviewDaily(prevStart, prevEndExclusive);
        List<Map<String, Object>> trendCandDaily = fetchCandidateDaily(trendStart, trendEndExclusive);
        List<Map<String, Object>> trendOfferDaily = fetchOfferDaily(trendStart, trendEndExclusive);

        FunnelData currentFunnel = new FunnelData(
                sumCandidate(candDaily), aggregateFunnelRows(candDaily));
        FunnelData prevFunnel = new FunnelData(
                sumCandidate(prevCandDaily), aggregateFunnelRows(prevCandDaily));
        List<Map<String, Object>> channels = aggregateChannelRows(candDaily);
        List<Map<String, Object>> candidateTrend = aggregateCandidateTrend(trendCandDaily);
        List<Map<String, Object>> onboardingTrend = aggregateOnboardingTrend(trendOfferDaily);
        List<Map<String, Object>> offerTrend = aggregateOfferTrend(trendOfferDaily);
        Map<String, Object> interviewStats = aggregateInterviewStats(interviewDaily);
        Map<String, Object> offerStats = aggregateOfferStats(offerDaily);
        Map<String, Object> prevInterviewStats = aggregateInterviewStats(prevInterviewDaily);
        Map<String, Object> prevOfferStats = aggregateOfferStats(prevOfferDaily);

        long candidates = currentFunnel.totalCount();
        long prevCandidates = prevFunnel.totalCount();
        long interviews = toLong(interviewStats.get("total"));
        long prevInterviews = toLong(prevInterviewStats.get("total"));
        long offersSent = toLong(offerStats.get("sentCount"));
        long prevOffersSent = toLong(prevOfferStats.get("sentCount"));
        long onboardings = toLong(offerStats.get("onboardCount"));
        long prevOnboardings = toLong(prevOfferStats.get("onboardCount"));
        double acceptanceRate = toDouble(offerStats.get("acceptanceRate"));
        double prevAcceptanceRate = toDouble(prevOfferStats.get("acceptanceRate"));
        double avgConfirmDays = toDouble(offerStats.get("avgConfirmDays"));
        double prevAvgConfirmDays = toDouble(prevOfferStats.get("avgConfirmDays"));

        List<AnalyticsKpiVO> kpis = List.of(
                kpi("candidates", "新增候选人", candidates, prevCandidates, "人"),
                kpi("interviews", "面试场次", interviews, prevInterviews, "场"),
                kpi("offers", "Offer 发送", offersSent, prevOffersSent, "份"),
                kpi("onboardings", "完成入职", onboardings, prevOnboardings, "人"),
                kpi("acceptanceRate", "Offer 接受率", acceptanceRate, prevAcceptanceRate, "%"),
                kpi("avgConfirmDays", "平均确认周期", avgConfirmDays, prevAvgConfirmDays, "天")
        );

        return AnalyticsOverviewVO.builder()
                .startDate(DateUtils.formatDate(start))
                .endDate(DateUtils.formatDate(endExclusive.minusDays(1)))
                .kpis(kpis)
                .funnel(buildFunnel(currentFunnel))
                .channels(buildChannels(channels))
                .candidateTrend(buildCandidateTrend(candidateTrend, onboardingTrend))
                .offerTrend(buildOfferTrend(offerTrend))
                .insights(buildOverviewInsights(kpis, currentFunnel, channels,
                        candidateTrend, onboardingTrend, start, endExclusive))
                .build();
    }

    /**
     * 洞察：AI（LLM）异步生成 + 缓存优先，页面不阻塞。
     *
     * <p>LLM 生成耗时较长，若同步调用会导致 overview 接口超时。因此：
     * 命中缓存直接返回 AI 洞察；未命中则后台异步触发 LLM 生成，
     * 当前请求先返回规则洞察，生成完成后写入缓存，后续请求自动升级为 AI 洞察。</p>
     */
    private List<InsightVO> buildOverviewInsights(List<AnalyticsKpiVO> kpis, FunnelData funnel,
                                                  List<Map<String, Object>> channels,
                                                  List<Map<String, Object>> candidateTrend,
                                                  List<Map<String, Object>> onboardingTrend,
                                                  LocalDate start, LocalDate endExclusive) {
        List<InsightVO> ruleInsights = buildInsights(kpis, funnel, channels,
                candidateTrend, onboardingTrend);
        String periodKey = start + "_" + endExclusive;
        try {
            // 1. 缓存命中：直接返回 AI 洞察（不触发 LLM）
            List<InsightVO> cached = analyticsInsightCache.get(periodKey);
            if (cached != null && !cached.isEmpty()) {
                return cached;
            }
            // 2. 未命中：异步触发 LLM 生成（同一周期只触发一次），当前请求先用规则洞察
            if (analyticsInsightCache.tryStartGenerate(periodKey)) {
                AiAnalyticsDTO.Request request = buildInsightRequest(
                        kpis, funnel, channels, candidateTrend, onboardingTrend,
                        start, endExclusive);
                INSIGHT_EXECUTOR.execute(() -> generateInsightsAsync(request, periodKey));
                log.info("AI 洞察异步生成已触发: periodKey={}", periodKey);
            }
        } catch (Exception e) {
            log.warn("AI 洞察处理异常，使用规则洞察: error={}", e.getMessage());
        }
        return ruleInsights;
    }

    /** 后台异步生成 AI 洞察并写入缓存。 */
    private void generateInsightsAsync(AiAnalyticsDTO.Request request, String periodKey) {
        try {
            List<InsightVO> aiInsights = requestAiInsights(request);
            if (aiInsights != null && !aiInsights.isEmpty()) {
                analyticsInsightCache.put(periodKey, aiInsights);
                log.info("AI 洞察异步生成完成: periodKey={}, count={}",
                        periodKey, aiInsights.size());
            } else {
                log.info("AI 洞察异步生成为空，保留规则洞察: periodKey={}", periodKey);
            }
        } catch (Exception e) {
            log.warn("AI 洞察异步生成失败（保留规则洞察）: periodKey={}, error={}",
                    periodKey, e.getMessage());
        } finally {
            analyticsInsightCache.finishGenerate(periodKey);
        }
    }

    /** 调用 AI 引擎生成洞察（仅异步路径调用，同步路径不阻塞页面）。 */
    private List<InsightVO> requestAiInsights(AiAnalyticsDTO.Request request) {
        ApiResponse<List<AiAnalyticsDTO.Result>> resp =
                aiEngineClient.analyticsInsights(request);
        List<AiAnalyticsDTO.Result> results = resp != null ? resp.data() : null;
        if (results == null || results.isEmpty()) {
            return List.of();
        }
        List<InsightVO> aiInsights = new ArrayList<>();
        int id = 1;
        for (AiAnalyticsDTO.Result r : results) {
            if (r.title() == null || r.title().isBlank()) {
                continue;
            }
            aiInsights.add(InsightVO.builder()
                    .id(id++)
                    .icon(resolveInsightIcon(r.title(), r.description()))
                    .title(r.title())
                    .description(r.description())
                    .trend(normalizeTrend(r.trend()))
                    .value(r.value() == null ? "" : r.value())
                    .aiGenerated(true)
                    .build());
        }
        return aiInsights;
    }

    /**
     * 根据洞察内容推断语义化图标（使每个洞察卡片图标各不相同、更专业）。
     */
    private String resolveInsightIcon(String title, String description) {
        String text = (title == null ? "" : title)
                + " " + (description == null ? "" : description);
        if (text.contains("渠道") || text.contains("投放") || text.contains("成本")
                || text.contains("roi")) {
            return "money";
        }
        if (text.contains("面试")) {
            return "connection";
        }
        if (text.contains("offer") || text.contains("薪酬") || text.contains("薪资")) {
            return "coin";
        }
        if (text.contains("入职")) {
            return "data";
        }
        if (text.contains("候选人") || text.contains("简历") || text.contains("人才")
                || text.contains("供给")) {
            return "user";
        }
        if (text.contains("周期") || text.contains("天数")) {
            return "timer";
        }
        if (text.contains("接受率") || text.contains("通过率") || text.contains("转化率")) {
            return "circle-check";
        }
        if (text.contains("风险") || text.contains("瓶颈") || text.contains("流失")) {
            return "warning";
        }
        if (text.contains("增长") || text.contains("提升") || text.contains("上升")) {
            return "trend";
        }
        return "data";
    }

    /** 组装 AI 洞察请求（KPI / 漏斗 / 渠道 / 趋势）。 */
    private AiAnalyticsDTO.Request buildInsightRequest(List<AnalyticsKpiVO> kpis, FunnelData funnel,
                                                       List<Map<String, Object>> channels,
                                                       List<Map<String, Object>> candidateTrend,
                                                       List<Map<String, Object>> onboardingTrend,
                                                       LocalDate start, LocalDate endExclusive) {
        return new AiAnalyticsDTO.Request(
                DateUtils.formatDate(start),
                DateUtils.formatDate(endExclusive.minusDays(1)),
                kpis.stream().map(k -> new AiAnalyticsDTO.Kpi(
                        k.getKey(), k.getLabel(),
                        k.getValue() != null ? k.getValue().doubleValue() : null,
                        k.getPrevValue() != null ? k.getPrevValue().doubleValue() : null,
                        k.getUnit())).toList(),
                buildFunnel(funnel).stream()
                        .map(s -> new AiAnalyticsDTO.Funnel(
                                String.valueOf(s.getStage()), s.getLabel(),
                                s.getCount() != null ? s.getCount().longValue() : 0L)).toList(),
                channels.stream().map(c -> new AiAnalyticsDTO.Channel(
                        toInt(c.get("source")), nameOfSource(toInt(c.get("source"))),
                        toLong(c.get("candidateCount")), toLong(c.get("hireCount")),
                        toDouble(c.get("hireRate")))).toList(),
                toTrendList(candidateTrend),
                toTrendList(onboardingTrend));
    }

    private List<AiAnalyticsDTO.Trend> toTrendList(List<Map<String, Object>> rows) {
        if (rows == null) return List.of();
        return rows.stream().map(r -> new AiAnalyticsDTO.Trend(
                String.valueOf(r.getOrDefault("date", "")), toLong(r.get("count")))).toList();
    }

    private String normalizeTrend(String trend) {
        if (trend == null) return "STABLE";
        return "DOWN".equalsIgnoreCase(trend) ? "DOWN"
                : "UP".equalsIgnoreCase(trend) ? "UP" : "STABLE";
    }

    /** 查询招聘漏斗数据。 */
    @Override
    public List<FunnelStageVO> getFunnel(LocalDate startDate, LocalDate endDate) {
        LocalDate[] range = normalizeRange(startDate, endDate);
        List<Map<String, Object>> daily = fetchCandidateDaily(range[0], range[1]);
        return buildFunnel(new FunnelData(sumCandidate(daily), aggregateFunnelRows(daily)));
    }

    /** 查询渠道分析数据。 */
    @Override
    public List<ChannelVO> getChannelAnalysis(LocalDate startDate, LocalDate endDate) {
        LocalDate[] range = normalizeRange(startDate, endDate);
        return buildChannels(aggregateChannelRows(fetchCandidateDaily(range[0], range[1])));
    }

    /** 查询 AI 生成的招聘洞察建议。 */
    @Override
    public List<InsightVO> getInsights() {
        return getOverview(null, null).getInsights();
    }

    /** 查询招聘周期（Offer 发送→确认）趋势数据。 */
    @Override
    public List<RecruitmentCycleVO> getRecruitmentCycle(String period) {
        LocalDate trendStart = DateUtils.currentYearMonth().minusMonths(11).atDay(1);
        LocalDate trendEndExclusive = DateUtils.today().plusDays(1);
        return buildOfferTrend(aggregateOfferTrend(
                fetchOfferDaily(trendStart, trendEndExclusive))).stream()
                .map(point -> RecruitmentCycleVO.builder()
                        .month(point.getMonth())
                        .cycle((int) Math.round(point.getAvgConfirmDays()))
                        .target(CYCLE_TARGET_DAYS)
                        .build())
                .toList();
    }

    /** 导出分析报表（CSV）。 */
    @Override
    public byte[] exportReport(LocalDate startDate, LocalDate endDate) {
        AnalyticsOverviewVO overview = getOverview(startDate, endDate);

        StringBuilder csv = new StringBuilder();
        csv.append("SmartRecruit 招聘数据分析报告\n");
        csv.append("统计周期,").append(overview.getStartDate()).append(" ~ ").append(overview.getEndDate()).append("\n\n");

        csv.append("一、核心指标\n");
        csv.append("指标,当前值,上一周期,环比变化\n");
        for (AnalyticsKpiVO kpi : overview.getKpis()) {
            csv.append(escapeCsv(kpi.getLabel())).append(',')
                    .append(formatNumber(kpi.getValue())).append(',')
                    .append(formatNumber(kpi.getPrevValue())).append(',')
                    .append(kpi.getChangePercent() != null
                            ? String.format("%+.1f%%", kpi.getChangePercent()) : "—")
                    .append('\n');
        }

        csv.append("\n二、转化漏斗\n");
        csv.append("阶段,人数,转化率\n");
        for (FunnelStageVO stage : overview.getFunnel()) {
            csv.append(escapeCsv(stage.getLabel())).append(',')
                    .append(stage.getCount()).append(',')
                    .append(stage.getConversionRate() != null
                            ? String.format("%.1f%%", stage.getConversionRate()) : "—")
                    .append('\n');
        }

        csv.append("\n三、渠道分析\n");
        csv.append("渠道,候选人数,占比,入职数,转化率,人均成本,总成本,ROI\n");
        for (ChannelVO channel : overview.getChannels()) {
            csv.append(escapeCsv(channel.getChannel())).append(',')
                    .append(channel.getCandidateCount()).append(',')
                    .append(channel.getPercentage() != null ? String.format("%.1f%%", channel.getPercentage()) : "—").append(',')
                    .append(channel.getHireCount()).append(',')
                    .append(channel.getConversionRate() != null ? String.format("%.1f%%", channel.getConversionRate()) : "—").append(',')
                    .append(channel.getCostPerHire() != null ? String.format("¥%.0f", channel.getCostPerHire()) : "—").append(',')
                    .append(channel.getTotalCost() != null ? String.format("¥%.0f", channel.getTotalCost()) : "—").append(',')
                    .append(channel.getRoi() != null ? String.format("%.2f", channel.getRoi()) : "∞")
                    .append('\n');
        }

        csv.append("\n四、月度趋势\n");
        csv.append("月份,新增候选人,完成入职\n");
        for (TrendPointVO point : overview.getCandidateTrend()) {
            csv.append(point.getMonth()).append(',')
                    .append(point.getCandidateCount()).append(',')
                    .append(point.getOnboardCount()).append('\n');
        }

        csv.append("\n五、Offer 趋势\n");
        csv.append("月份,发送,接受,平均确认周期(天)\n");
        for (OfferTrendPointVO point : overview.getOfferTrend()) {
            csv.append(point.getMonth()).append(',')
                    .append(point.getSentCount()).append(',')
                    .append(point.getAcceptedCount()).append(',')
                    .append(point.getAvgConfirmDays()).append('\n');
        }

        csv.append("\n六、AI 洞察\n");
        for (InsightVO insight : overview.getInsights()) {
            csv.append("【").append(insight.getTitle()).append("】").append(insight.getDescription()).append('\n');
        }

        // 添加 UTF-8 BOM，保证 Excel 直接打开不乱码
        return ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);
    }

    // ==================== 日汇总拉取（本地快照优先） ====================

    /**
     * 候选人日汇总：优先读本地快照（定时同步），快照为空（首次部署）时降级远程查询。
     */
    private List<Map<String, Object>> fetchCandidateDaily(LocalDate start, LocalDate endExclusive) {
        List<AnalyticsCandidateDaily> rows = candidateDailyMapper.selectList(
                new LambdaQueryWrapper<AnalyticsCandidateDaily>()
                        .ge(AnalyticsCandidateDaily::getStatDate, start)
                        .lt(AnalyticsCandidateDaily::getStatDate, endExclusive)
                        .orderByAsc(AnalyticsCandidateDaily::getStatDate));
        if (!rows.isEmpty()) {
            return rows.stream().map(r -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("statDate", r.getStatDate());
                map.put("stage", r.getStage());
                map.put("source", r.getSource());
                map.put("candidateCount", r.getCandidateCount());
                return map;
            }).toList();
        }
        // 快照从未初始化（首次部署）：降级远程查询一次，避免页面空白
        if (!analyticsDailySyncService.hasSnapshotData()) {
            return fetchList(() -> recruitmentClient.getCandidateDaily(
                    DateUtils.formatDate(start), DateUtils.formatDate(endExclusive.minusDays(1))));
        }
        return List.of();
    }

    /**
     * Offer/入职日汇总：本地快照优先，快照为空时降级远程查询（首次部署兜底）。
     */
    private List<Map<String, Object>> fetchOfferDaily(LocalDate start, LocalDate endExclusive) {
        List<AnalyticsOfferDaily> rows = offerDailyMapper.selectList(
                new LambdaQueryWrapper<AnalyticsOfferDaily>()
                        .ge(AnalyticsOfferDaily::getStatDate, start)
                        .lt(AnalyticsOfferDaily::getStatDate, endExclusive)
                        .orderByAsc(AnalyticsOfferDaily::getStatDate));
        if (!rows.isEmpty()) {
            return rows.stream().map(r -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("statDate", r.getStatDate());
                map.put("sentCount", r.getSentCount());
                map.put("acceptedCount", r.getAcceptedCount());
                map.put("declinedCount", r.getDeclinedCount());
                map.put("pendingCount", r.getPendingCount());
                map.put("onboardCount", r.getOnboardCount());
                map.put("confirmTotalDays", r.getConfirmTotalDays());
                map.put("confirmCount", r.getConfirmCount());
                return map;
            }).toList();
        }
        if (!analyticsDailySyncService.hasSnapshotData()) {
            return fetchList(() -> offerClient.getOfferDaily(
                    DateUtils.formatDate(start), DateUtils.formatDate(endExclusive.minusDays(1))));
        }
        return List.of();
    }

    /**
     * 面试日汇总：本地快照优先，快照为空时降级远程查询（首次部署兜底）。
     */
    private List<Map<String, Object>> fetchInterviewDaily(LocalDate start, LocalDate endExclusive) {
        List<AnalyticsInterviewDaily> rows = interviewDailyMapper.selectList(
                new LambdaQueryWrapper<AnalyticsInterviewDaily>()
                        .ge(AnalyticsInterviewDaily::getStatDate, start)
                        .lt(AnalyticsInterviewDaily::getStatDate, endExclusive)
                        .orderByAsc(AnalyticsInterviewDaily::getStatDate));
        if (!rows.isEmpty()) {
            return rows.stream().map(r -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("statDate", r.getStatDate());
                map.put("totalCount", r.getTotalCount());
                map.put("passedCount", r.getPassedCount());
                map.put("cancelledCount", r.getCancelledCount());
                return map;
            }).toList();
        }
        if (!analyticsDailySyncService.hasSnapshotData()) {
            return fetchList(() -> interviewClient.getInterviewDaily(
                    DateUtils.formatDate(start), DateUtils.formatDate(endExclusive.minusDays(1))));
        }
        return List.of();
    }

    // ==================== 日汇总 → 分析结构（内存聚合） ====================

    private record FunnelData(long totalCount, List<Map<String, Object>> rows) {}

    private long sumCandidate(List<Map<String, Object>> daily) {
        return daily.stream().mapToLong(r -> toLong(r.get("candidateCount"))).sum();
    }

    private List<Map<String, Object>> aggregateFunnelRows(List<Map<String, Object>> daily) {
        Map<Integer, Long> byStage = new HashMap<>();
        for (Map<String, Object> row : daily) {
            byStage.merge(toInt(row.get("stage")), toLong(row.get("candidateCount")), Long::sum);
        }
        return byStage.entrySet().stream()
                .map(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("stage", e.getKey());
                    row.put("count", e.getValue());
                    return row;
                })
                .toList();
    }

    private List<Map<String, Object>> aggregateChannelRows(List<Map<String, Object>> daily) {
        Map<Integer, Long> candidates = new HashMap<>();
        Map<Integer, Long> hires = new HashMap<>();
        for (Map<String, Object> row : daily) {
            int source = toInt(row.get("source"));
            long count = toLong(row.get("candidateCount"));
            candidates.merge(source, count, Long::sum);
            if (toInt(row.get("stage")) == STAGE_HIRED) {
                hires.merge(source, count, Long::sum);
            }
        }

        Set<Integer> sources = new TreeSet<>(candidates.keySet());
        sources.addAll(hires.keySet());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Integer source : sources) {
            long candidateCount = candidates.getOrDefault(source, 0L);
            long hireCount = hires.getOrDefault(source, 0L);
            double hireRate = candidateCount > 0
                    ? Math.round(hireCount * 10000.0 / candidateCount) / 100.0 : 0.0;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("source", source);
            row.put("candidateCount", candidateCount);
            row.put("hireCount", hireCount);
            row.put("hireRate", hireRate);
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, Object>> aggregateCandidateTrend(List<Map<String, Object>> daily) {
        Map<String, Long> byMonth = new TreeMap<>();
        for (Map<String, Object> row : daily) {
            byMonth.merge(monthOf(row.get("statDate")), toLong(row.get("candidateCount")), Long::sum);
        }
        return byMonth.entrySet().stream()
                .map(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("month", e.getKey());
                    row.put("candidateCount", e.getValue());
                    return row;
                })
                .toList();
    }

    private List<Map<String, Object>> aggregateOnboardingTrend(List<Map<String, Object>> offerDaily) {
        Map<String, Long> byMonth = new TreeMap<>();
        for (Map<String, Object> row : offerDaily) {
            byMonth.merge(monthOf(row.get("statDate")), toLong(row.get("onboardCount")), Long::sum);
        }
        return byMonth.entrySet().stream()
                .map(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("month", e.getKey());
                    row.put("onboardCount", e.getValue());
                    return row;
                })
                .toList();
    }

    private List<Map<String, Object>> aggregateOfferTrend(List<Map<String, Object>> offerDaily) {
        Map<String, long[]> byMonth = new TreeMap<>();
        for (Map<String, Object> row : offerDaily) {
            String month = monthOf(row.get("statDate"));
            long[] acc = byMonth.computeIfAbsent(month, k -> new long[4]);
            acc[0] += toLong(row.get("sentCount"));
            acc[1] += toLong(row.get("acceptedCount"));
            acc[2] += toLong(row.get("confirmTotalDays"));
            acc[3] += toLong(row.get("confirmCount"));
        }
        return byMonth.entrySet().stream()
                .map(e -> {
                    long[] acc = e.getValue();
                    double avgDays = acc[3] > 0
                            ? Math.round(acc[2] * 10.0 / acc[3]) / 10.0 : 0.0;
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("month", e.getKey());
                    row.put("sentCount", acc[0]);
                    row.put("acceptedCount", acc[1]);
                    row.put("avgConfirmDays", avgDays);
                    return row;
                })
                .toList();
    }

    private Map<String, Object> aggregateOfferStats(List<Map<String, Object>> offerDaily) {
        long sent = 0, accepted = 0, declined = 0, pending = 0, onboard = 0;
        long confirmTotalDays = 0, confirmCount = 0;
        for (Map<String, Object> row : offerDaily) {
            sent += toLong(row.get("sentCount"));
            accepted += toLong(row.get("acceptedCount"));
            declined += toLong(row.get("declinedCount"));
            pending += toLong(row.get("pendingCount"));
            onboard += toLong(row.get("onboardCount"));
            confirmTotalDays += toLong(row.get("confirmTotalDays"));
            confirmCount += toLong(row.get("confirmCount"));
        }
        double acceptanceRate = accepted + declined > 0
                ? Math.round(accepted * 10000.0 / (accepted + declined)) / 100.0 : 0.0;
        double avgConfirmDays = confirmCount > 0
                ? Math.round(confirmTotalDays * 10.0 / confirmCount) / 10.0 : 0.0;

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("sentCount", sent);
        stats.put("acceptedCount", accepted);
        stats.put("declinedCount", declined);
        stats.put("pendingCount", pending);
        stats.put("onboardCount", onboard);
        stats.put("acceptanceRate", acceptanceRate);
        stats.put("avgConfirmDays", avgConfirmDays);
        return stats;
    }

    private Map<String, Object> aggregateInterviewStats(List<Map<String, Object>> interviewDaily) {
        long total = interviewDaily.stream()
                .mapToLong(r -> toLong(r.get("totalCount")))
                .sum();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", total);
        return stats;
    }

    private String monthOf(Object statDate) {
        String s = String.valueOf(statDate);
        return s.length() >= 7 ? s.substring(0, 7) : s;
    }

    // ==================== VO 构建 ====================

    private List<FunnelStageVO> buildFunnel(FunnelData data) {
        Map<Integer, Long> counts = new LinkedHashMap<>();
        for (int i = 0; i < FUNNEL_STAGE_NAMES.size(); i++) {
            counts.put(i, 0L);
        }
        for (Map<String, Object> row : data.rows()) {
            int stage = toInt(row.get("stage"));
            if (stage >= 0 && stage < FUNNEL_STAGE_NAMES.size()) {
                counts.put(stage, counts.get(stage) + toLong(row.get("count")));
            }
        }

        List<FunnelStageVO> funnel = new ArrayList<>();
        for (int i = 0; i < FUNNEL_STAGE_NAMES.size(); i++) {
            long count = counts.get(i);
            long next = i + 1 < FUNNEL_STAGE_NAMES.size() ? counts.get(i + 1) : 0;
            double rate = count > 0 ? Math.round(next * 10000.0 / count) / 100.0 : 0.0;
            funnel.add(FunnelStageVO.builder()
                    .stage(i + 1)
                    .label(FUNNEL_STAGE_NAMES.get(i))
                    .count((int) count)
                    .conversionRate(i == FUNNEL_STAGE_NAMES.size() - 1 ? 0.0 : rate)
                    .sortOrder(i + 1)
                    .build());
        }
        return funnel;
    }

    private List<ChannelVO> buildChannels(List<Map<String, Object>> rows) {
        long totalCandidates = rows.stream().mapToLong(r -> toLong(r.get("candidateCount"))).sum();
        List<ChannelVO> channels = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            int source = toInt(row.get("source"));
            long candidates = toLong(row.get("candidateCount"));
            long hires = toLong(row.get("hireCount"));
            double hireRate = toDouble(row.get("hireRate"));
            double percentage = totalCandidates > 0
                    ? Math.round(candidates * 10000.0 / totalCandidates) / 100.0 : 0.0;
            double costPerHire = CHANNEL_COST_PER_HIRE.getOrDefault(source, 0.0);
            double totalCost = candidates * costPerHire;
            Double roi = totalCost > 0
                    ? Math.round(hires * AVERAGE_ANNUAL_VALUE * 100.0 / totalCost) / 100.0
                    : null;

            channels.add(ChannelVO.builder()
                    .channel(CHANNEL_NAMES.getOrDefault(source, "渠道" + source))
                    .candidateCount((int) candidates)
                    .percentage(percentage)
                    .hireCount((int) hires)
                    .conversionRate(hireRate)
                    .costPerHire(costPerHire)
                    .totalCost(totalCost)
                    .roi(roi)
                    .build());
        }
        return channels;
    }

    private List<TrendPointVO> buildCandidateTrend(
            List<Map<String, Object>> candidateTrend, List<Map<String, Object>> onboardingTrend) {
        Map<String, Long> candidates = toMonthMap(candidateTrend, "candidateCount");
        Map<String, Long> onboardings = toMonthMap(onboardingTrend, "onboardCount");
        Set<String> months = new TreeSet<>(candidates.keySet());
        months.addAll(onboardings.keySet());

        return months.stream().map(month -> TrendPointVO.builder()
                .month(month)
                .candidateCount(candidates.getOrDefault(month, 0L))
                .onboardCount(onboardings.getOrDefault(month, 0L))
                .build()).toList();
    }

    private List<OfferTrendPointVO> buildOfferTrend(List<Map<String, Object>> rows) {
        return rows.stream().map(row -> OfferTrendPointVO.builder()
                .month(String.valueOf(row.get("month")))
                .sentCount(toLong(row.get("sentCount")))
                .acceptedCount(toLong(row.get("acceptedCount")))
                .avgConfirmDays(toDouble(row.get("avgConfirmDays")))
                .build()).toList();
    }

    private Map<String, Long> toMonthMap(List<Map<String, Object>> rows, String valueKey) {
        Map<String, Long> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String month = String.valueOf(row.get("month"));
            map.put(month, toLong(row.get(valueKey)));
        }
        return map;
    }

    // ==================== 洞察生成（基于真实统计的规则） ====================

    private List<InsightVO> buildInsights(List<AnalyticsKpiVO> kpis, FunnelData funnel,
                                          List<Map<String, Object>> channels,
                                          List<Map<String, Object>> candidateTrend,
                                          List<Map<String, Object>> onboardingTrend) {
        List<InsightVO> insights = new ArrayList<>();

        // 1. 渠道：入职转化率最高的渠道
        channels.stream()
                .max(Comparator.comparingDouble(r -> toLong(r.get("candidateCount")) > 0
                        ? toDouble(r.get("hireRate")) : -1))
                .filter(r -> toLong(r.get("candidateCount")) > 0)
                .ifPresent(best -> insights.add(InsightVO.builder()
                        .id(1)
                        .icon("money")
                        .title("最佳渠道：" + nameOfSource(toInt(best.get("source"))))
                        .description(String.format("%s 渠道入职转化率 %.1f%%，共入职 %d 人，建议加大该渠道投入。",
                                nameOfSource(toInt(best.get("source"))),
                                toDouble(best.get("hireRate")), toLong(best.get("hireCount"))))
                        .trend("UP")
                        .value(String.format("%.1f%%", toDouble(best.get("hireRate"))))
                        .build()));

        // 2. 漏斗：流失最大的环节
        buildFunnel(funnel).stream()
                .filter(s -> s.getSortOrder() < FUNNEL_STAGE_NAMES.size() && s.getCount() > 0)
                .min(Comparator.comparingDouble(s -> s.getConversionRate() != null
                        ? s.getConversionRate() : 100.0))
                .ifPresent(bottleneck -> insights.add(InsightVO.builder()
                        .id(2)
                        .icon("connection")
                        .title("漏斗瓶颈：" + bottleneck.getLabel())
                        .description(String.format("「%s → %s」转化率仅 %.1f%%，%d 人在此环节流失，建议优化该环节筛选标准。",
                                bottleneck.getLabel(),
                                nextStageName(bottleneck.getSortOrder()),
                                bottleneck.getConversionRate(),
                                Math.round(bottleneck.getCount()
                                        * (1 - bottleneck.getConversionRate() / 100.0))))
                        .trend("DOWN")
                        .value(String.format("%.1f%%", bottleneck.getConversionRate()))
                        .build()));

        // 3. 招聘周期
        double avgDays = kpiValue(kpis, "avgConfirmDays");
        boolean cycleOk = avgDays <= CYCLE_TARGET_DAYS;
        insights.add(InsightVO.builder()
                .id(3)
                .icon("timer")
                .title(cycleOk ? "招聘周期处于目标线内" : "招聘周期超出目标线")
                .description(String.format("当前平均确认周期 %.1f 天（目标 %d 天），%s。",
                        avgDays, CYCLE_TARGET_DAYS,
                        cycleOk ? "流程运转健康" : "建议缩短审批与沟通环节"))
                .trend(cycleOk ? "STABLE" : "DOWN")
                .value(String.format("%.1f天", avgDays))
                .build());

        // 4. Offer 接受率
        double acceptanceRate = kpiValue(kpis, "acceptanceRate");
        boolean acceptanceOk = acceptanceRate >= ACCEPTANCE_BENCHMARK;
        insights.add(InsightVO.builder()
                .id(4)
                .icon("circle-check")
                .title(acceptanceOk ? "Offer 接受率良好" : "Offer 接受率偏低")
                .description(String.format("当前 Offer 接受率 %.1f%%（行业基准 %.0f%%），%s。",
                        acceptanceRate, ACCEPTANCE_BENCHMARK,
                        acceptanceOk ? "薪酬与岗位吸引力稳定" : "建议结合薪酬与岗位吸引力分析原因"))
                .trend(acceptanceOk ? "STABLE" : "DOWN")
                .value(String.format("%.1f%%", acceptanceRate))
                .build());

        // 5. 候选人增长
        List<Map<String, Object>> sorted = candidateTrend.stream()
                .sorted(Comparator.comparing(r -> String.valueOf(r.get("month"))))
                .toList();
        if (sorted.size() >= 2) {
            long last = toLong(sorted.get(sorted.size() - 1).get("candidateCount"));
            long prev = toLong(sorted.get(sorted.size() - 2).get("candidateCount"));
            double growth = prev > 0 ? Math.round((last - prev) * 10000.0 / prev) / 100.0 : 0.0;
            insights.add(InsightVO.builder()
                    .id(5)
                    .icon("user")
                    .title(growth >= 0 ? "候选人供给持续增长" : "候选人供给环比下降")
                    .description(String.format("最近一个月新增候选人 %d 人，环比%s %.1f%%。",
                            last, growth >= 0 ? "增长" : "下降", Math.abs(growth)))
                    .trend(growth >= 0 ? "UP" : "DOWN")
                    .value(String.format("%+.1f%%", growth))
                    .build());
        }

        // 6. 入职转化
        List<Map<String, Object>> onboardSorted = onboardingTrend.stream()
                .sorted(Comparator.comparing(r -> String.valueOf(r.get("month"))))
                .toList();
        if (!onboardSorted.isEmpty()) {
            long lastOnboard = toLong(onboardSorted.get(onboardSorted.size() - 1).get("onboardCount"));
            insights.add(InsightVO.builder()
                    .id(6)
                    .icon("data")
                    .title("入职转化")
                    .description(String.format("最近一个月完成入职 %d 人，请结合部门进度安排团队融入与培训资源。",
                            lastOnboard))
                    .trend(lastOnboard > 0 ? "STABLE" : "DOWN")
                    .value(lastOnboard + "人")
                    .build());
        }

        return insights;
    }

    private String nextStageName(int sortOrder) {
        int idx = sortOrder; // sortOrder 从 1 开始，下标 = sortOrder - 1
        return idx < FUNNEL_STAGE_NAMES.size() ? FUNNEL_STAGE_NAMES.get(idx) : "结束";
    }

    private double kpiValue(List<AnalyticsKpiVO> kpis, String key) {
        return kpis.stream()
                .filter(k -> key.equals(k.getKey()))
                .map(AnalyticsKpiVO::getValue)
                .findFirst()
                .orElse(0.0);
    }

    private String nameOfSource(int source) {
        return CHANNEL_NAMES.getOrDefault(source, "其他");
    }

    // ==================== KPI 工具 ====================

    private AnalyticsKpiVO kpi(String key, String label, double value,
                               double prevValue, String unit) {
        Double change = prevValue > 0
                ? Math.round((value - prevValue) * 10000.0 / prevValue) / 100.0
                : null;
        return AnalyticsKpiVO.builder()
                .key(key)
                .label(label)
                .value(Math.round(value * 100.0) / 100.0)
                .prevValue(Math.round(prevValue * 100.0) / 100.0)
                .changePercent(change)
                .unit(unit)
                .build();
    }

    // ==================== 通用工具 ====================

    private LocalDate[] normalizeRange(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : DateUtils.today().withDayOfMonth(1);
        LocalDate end = endDate != null ? endDate : DateUtils.today();
        if (end.isBefore(start)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }
        return new LocalDate[]{start, end.plusDays(1)};
    }

    private List<Map<String, Object>> fetchList(
            Supplier<ApiResponse<List<Map<String, Object>>>> supplier) {
        try {
            ApiResponse<List<Map<String, Object>>> resp = supplier.get();
            return resp != null && resp.data() != null ? resp.data() : List.of();
        } catch (Exception e) {
            log.warn("分析数据聚合失败（降级为空数据）: {}", e.getMessage());
            return List.of();
        }
    }

    private int toInt(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number n) return n.intValue();
        try { return Integer.parseInt(obj.toString()); } catch (NumberFormatException e) { return 0; }
    }

    private long toLong(Object obj) {
        if (obj == null) return 0L;
        if (obj instanceof Number n) return n.longValue();
        try { return Long.parseLong(obj.toString()); } catch (NumberFormatException e) { return 0L; }
    }

    private double toDouble(Object obj) {
        if (obj == null) return 0.0;
        if (obj instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(obj.toString()); } catch (NumberFormatException e) { return 0.0; }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String formatNumber(Double value) {
        return value != null ? String.format("%.1f", value) : "—";
    }
}
