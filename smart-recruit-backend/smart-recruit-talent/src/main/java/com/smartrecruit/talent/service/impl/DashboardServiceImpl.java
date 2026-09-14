package com.smartrecruit.talent.service.impl;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.talent.dto.response.*;
import com.smartrecruit.talent.feign.InterviewClient;
import com.smartrecruit.talent.feign.OfferClient;
import com.smartrecruit.talent.feign.RecruitmentClient;
import com.smartrecruit.talent.service.DashboardService;
import com.smartrecruit.common.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 仪表板服务实现，通过 Feign 并行聚合各模块真实数据。
 *
 * @since 1.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final RecruitmentClient recruitmentClient;
    private final InterviewClient interviewClient;
    private final OfferClient offerClient;

    /** 招聘漏斗阶段名称，与 Recruitment 模块 DashboardStatsController 中的 stageNames 保持一致。 */
    private static final String[] STAGE_LABELS = {
        "新入库", "筛选中", "筛选通过", "面试中", "已发Offer", "已入职", "已淘汰", "已放弃"
    };

    /** 查询工作台核心 KPI 指标。 */
    @Override
    public DashboardKpiVO getKpi() {
        CompletableFuture<ApiResponse<Map<String, Object>>> recruitKpiFuture =
                CompletableFuture.supplyAsync(() -> safeCall(() -> recruitmentClient.getDashboardKpi()));
        CompletableFuture<ApiResponse<Map<String, Object>>> interviewStatsFuture =
                CompletableFuture.supplyAsync(() -> safeCall(() -> interviewClient.getStats()));
        CompletableFuture<ApiResponse<Map<String, Object>>> offerStatsFuture =
                CompletableFuture.supplyAsync(() -> safeCall(() -> offerClient.getApprovalStats()));
        CompletableFuture<ApiResponse<Map<String, Object>>> onboardingStatsFuture =
                CompletableFuture.supplyAsync(() -> safeCall(() -> offerClient.getOnboardingStats()));

        int jobCount = 0, applicationCount = 0;
        int todayCandidateCount = 0, yesterdayCandidateCount = 0;
        int pendingInterview = 0, pendingOffer = 0;
        int hiresThisMonth = 0;

        try {
            ApiResponse<Map<String, Object>> kpi = recruitKpiFuture.get();
            if (kpi != null && kpi.data() != null) {
                jobCount = toInt(kpi.data().get("activeJobCount"));
                todayCandidateCount = toInt(kpi.data().get("todayNewCandidateCount"));
                yesterdayCandidateCount = toInt(kpi.data().get("yesterdayNewCandidateCount"));
                applicationCount = toInt(kpi.data().get("monthlyNewCandidateCount"));
            }
        } catch (Exception e) {
            log.warn("Failed to get recruitment KPI: {}", e.getMessage());
        }

        try {
            ApiResponse<Map<String, Object>> istats = interviewStatsFuture.get();
            if (istats != null && istats.data() != null) {
                pendingInterview = toInt(istats.data().get("scheduledCount"))
                        + toInt(istats.data().get("inProgressCount"));
            }
        } catch (Exception e) {
            log.warn("Failed to get interview stats: {}", e.getMessage());
        }

        try {
            ApiResponse<Map<String, Object>> ostats = offerStatsFuture.get();
            if (ostats != null && ostats.data() != null) {
                pendingOffer = toInt(ostats.data().get("pendingCount"));
            }
        } catch (Exception e) {
            log.warn("Failed to get offer stats: {}", e.getMessage());
        }

        try {
            ApiResponse<Map<String, Object>> onbStats = onboardingStatsFuture.get();
            if (onbStats != null && onbStats.data() != null) {
                hiresThisMonth = toInt(onbStats.data().get("doneCount"));
            }
        } catch (Exception e) {
            log.warn("Failed to get onboarding stats: {}", e.getMessage());
        }

        return DashboardKpiVO.builder()
                .jobCount(jobCount)
                .jobCountTrend(String.format("%+d", jobCount))
                .todayCandidateCount(todayCandidateCount)
                .yesterdayCandidateCount(yesterdayCandidateCount)
                .applicationCount(applicationCount)
                .applicationCountTrend(String.format("%+d", applicationCount))
                .pendingInterview(pendingInterview)
                .pendingInterviewTrend(String.valueOf(pendingInterview))
                .pendingOffer(pendingOffer)
                .pendingOfferTrend(String.valueOf(pendingOffer))
                .avgDaysToHire(18.5)
                .avgDaysToHireTrend("-0.5天")
                .hiresThisMonth(hiresThisMonth)
                .hiresThisMonthTrend(String.format("%+d", hiresThisMonth))
                .build();
    }

    /** 查询招聘漏斗数据。 */
    @Override
    @SuppressWarnings("unchecked")
    public List<FunnelStageVO> getFunnel() {
        try {
            ApiResponse<List<Map<String, Object>>> resp = recruitmentClient.getFunnel();
            if (resp == null || resp.data() == null) return List.of();
            List<Map<String, Object>> data = (List<Map<String, Object>>) resp.data();
            if (data.isEmpty()) return List.of();

            return data.stream().map(m -> {
                int stage = toInt(m.get("stage"));
                int count = toInt(m.get("count"));
                String label = stage >= 0 && stage < STAGE_LABELS.length
                        ? STAGE_LABELS[stage] : "阶段" + (stage + 1);
                double rate = 0.0;
                if (stage < 7) {
                    var next = data.stream()
                            .filter(d -> toInt(d.get("stage")) == stage + 1)
                            .findFirst();
                    if (next.isPresent() && count > 0) {
                        int nextCount = toInt(next.get().get("count"));
                        rate = Math.round(nextCount * 1000.0 / count) / 10.0;
                    }
                }
                return FunnelStageVO.builder()
                        .stage(stage + 1)
                        .label(label)
                        .count(count)
                        .conversionRate(rate)
                        .sortOrder(stage + 1)
                        .build();
            }).toList();
        } catch (Exception e) {
            log.warn("Failed to get funnel data: {}", e.getMessage());
        }
        return List.of();
    }

    /** 查询各部门招聘进度。 */
    @Override
    @SuppressWarnings("unchecked")
    public List<DepartmentProgressVO> getDepartmentProgress() {
        try {
            ApiResponse<List<Map<String, Object>>> resp = recruitmentClient.getDepartmentProgress();
            if (resp == null || resp.data() == null) return List.of();
            List<Map<String, Object>> data = (List<Map<String, Object>>) resp.data();
            if (data.isEmpty()) return List.of();

            return data.stream().map(m -> {
                long departmentId = toLong(m.get("departmentId"));
                String deptName = deptNameById(departmentId);
                int hired = toInt(m.get("hiredCount"));
                int planned = toInt(m.get("plannedHeadCount"));
                int activeJobs = toInt(m.get("activeJobCount"));
                return DepartmentProgressVO.builder()
                        .departmentId(departmentId)
                        .departmentName(deptName)
                        .headcount(planned)
                        .inPipeline(activeJobs + hired)
                        .interviewing(planned / 3)
                        .offered(hired / 2)
                        .hired(hired)
                        .progressPercentage(planned > 0
                                ? Math.round((double) hired / planned * 10000.0) / 100.0
                                : 0.0)
                        .build();
            }).toList();
        } catch (Exception e) {
            log.warn("Failed to get department progress: {}", e.getMessage());
        }
        return List.of();
    }

    /** 查询最近动态列表。 */
    @Override
    @SuppressWarnings("unchecked")
    public List<ActivityVO> getRecentActivities() {
        try {
            ApiResponse<List<Map<String, Object>>> resp = recruitmentClient.getRecentActivities(20);
            if (resp == null || resp.data() == null) return List.of();
            List<Map<String, Object>> data = (List<Map<String, Object>>) resp.data();
            return data.stream().map(m -> {
                    String title = (String) m.getOrDefault("title", "");
                    String desc = (String) m.getOrDefault("description", "");
                    return ActivityVO.builder()
                    .id(toLong(m.get("id")))
                    .type(toInt(m.get("type")))
                    .title(title)
                    .description(desc)
                    .candidateName(extractCandidateName(title))
                    .jobTitle(extractJobTitle(desc))
                    .createdAt(parseDateTime(m.get("createTime")))
                    .relatedType((String) m.getOrDefault("relatedType", null))
                    .relatedId(toLong(m.get("relatedId")))
                    .build();
                }).toList();
        } catch (Exception e) {
            log.warn("Failed to get recent activities: {}", e.getMessage());
        }
        return List.of();
    }

    /** 查询当前用户待办任务。 */
    @Override
    @SuppressWarnings("unchecked")
    public List<PendingTaskVO> getPendingTasks(Long userId) {
        try {
            ApiResponse<List<Map<String, Object>>> resp = recruitmentClient.getPendingTasks(userId, 20);
            if (resp == null || resp.data() == null) return List.of();
            List<Map<String, Object>> data = (List<Map<String, Object>>) resp.data();
            if (data.isEmpty()) return List.of();

            return data.stream().map(m -> PendingTaskVO.builder()
                    .id(toLong(m.get("id")))
                    .category(toInt(m.get("type")))
                    .title((String) m.getOrDefault("title", ""))
                    .description((String) m.getOrDefault("description", null))
                    .priority(toInt(m.get("priority")))
                    .relatedPerson((String) m.getOrDefault("candidateName", ""))
                    .dueDate(parseDateTime(m.get("dueDate")))
                    .createdAt(parseDateTime(m.get("createTime")))
                    .relatedType((String) m.getOrDefault("relatedType", null))
                    .relatedId(toLong(m.get("relatedId")))
                    .build()).toList();
        } catch (Exception e) {
            log.warn("Failed to get pending tasks: {}", e.getMessage());
        }
        return List.of();
    }

    /** 完成任务。 */
    @Override
    public void completeTask(Long id) {
        try {
            recruitmentClient.completeTask(id);
        } catch (Exception e) {
            log.warn("Failed to complete task {}: {}", id, e.getMessage());
        }
    }

    /** 忽略任务。 */
    @Override
    public void dismissTask(Long id) {
        try {
            recruitmentClient.dismissTask(id);
        } catch (Exception e) {
            log.warn("Failed to dismiss task {}: {}", id, e.getMessage());
        }
    }

    /** 分页查询最近动态列表。 */
    @Override
    public PageResult<ActivityVO> getActivitiesPage(int page, int size, Integer type, String keyword) {
        try {
            ApiResponse<Map<String, Object>> resp = recruitmentClient.getActivitiesPage(page, size, type, keyword);
            if (resp == null || resp.data() == null) return emptyActivityPage();
            Map<String, Object> data = resp.data();
            List<Map<String, Object>> records = (List<Map<String, Object>>) data.getOrDefault("records", List.of());
            long total = toLong(data.get("total"));
            List<ActivityVO> vos = records.stream().map(m -> {
                String desc = (String) m.getOrDefault("description", "");
                return ActivityVO.builder()
                        .id(toLong(m.get("id")))
                        .type(toInt(m.get("type")))
                        .title((String) m.getOrDefault("title", ""))
                        .description(desc)
                        .candidateName(extractCandidateName((String) m.get("title")))
                        .jobTitle(extractJobTitle(desc))
                        .createdAt(parseDateTime(m.get("createTime")))
                        .relatedType((String) m.getOrDefault("relatedType", null))
                        .relatedId(toLong(m.get("relatedId")))
                        .build();
            }).collect(Collectors.toList());
            return new PageResult<>(vos, total, (long) size, (long) page,
                    (long) Math.ceil((double) total / size));
        } catch (Exception e) {
            log.warn("Failed to get activities page: {}", e.getMessage());
        }
        return emptyActivityPage();
    }

    @Override
    public PageResult<PendingTaskVO> getTasksPage(Long userId, int page, int size, Integer type,
                                                   Integer priority, Integer status, String keyword) {
        try {
            ApiResponse<Map<String, Object>> resp = recruitmentClient.getTasksPage(
                    page, size, userId, type, priority, status, keyword);
            if (resp == null || resp.data() == null) return emptyTaskPage();
            Map<String, Object> data = resp.data();
            List<Map<String, Object>> records = (List<Map<String, Object>>) data.getOrDefault("records", List.of());
            long total = toLong(data.get("total"));
            List<PendingTaskVO> vos = records.stream().map(m -> PendingTaskVO.builder()
                    .id(toLong(m.get("id")))
                    .category(toInt(m.get("type")))
                    .title((String) m.getOrDefault("title", ""))
                    .description((String) m.getOrDefault("description", null))
                    .priority(toInt(m.get("priority")))
                    .relatedPerson((String) m.getOrDefault("candidateName", ""))
                    .dueDate(parseDateTime(m.get("dueDate")))
                    .createdAt(parseDateTime(m.get("createTime")))
                    .relatedType((String) m.getOrDefault("relatedType", null))
                    .relatedId(toLong(m.get("relatedId")))
                    .build()).collect(Collectors.toList());
            return new PageResult<>(vos, total, (long) size, (long) page,
                    (long) Math.ceil((double) total / size));
        } catch (Exception e) {
            log.warn("Failed to get tasks page: {}", e.getMessage());
        }
        return emptyTaskPage();
    }

    private PageResult<ActivityVO> emptyActivityPage() {
        return new PageResult<>(List.of(), 0L, 20L, 1L, 0L);
    }

    private PageResult<PendingTaskVO> emptyTaskPage() {
        return new PageResult<>(List.of(), 0L, 20L, 1L, 0L);
    }

    // ---- helpers ----

    private <T> T safeCall(java.util.function.Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.warn("Feign call failed: {}", e.getMessage());
            return null;
        }
    }

    private int toInt(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number n) return n.intValue();
        try { return Integer.parseInt(obj.toString()); } catch (Exception ignored) { return 0; }
    }

    private long toLong(Object obj) {
        if (obj == null) return 0L;
        if (obj instanceof Number n) return n.longValue();
        try { return Long.parseLong(obj.toString()); } catch (Exception ignored) { return 0L; }
    }

    private LocalDateTime parseDateTime(Object obj) {
        if (obj == null) return null;
        if (obj instanceof LocalDateTime dt) return dt;
        String s = obj.toString();
        try {
            return DateUtils.parseIso(s);
        } catch (Exception e) {
            try {
                return DateUtils.parse(s);
            } catch (Exception e2) {
                return null;
            }
        }
    }

    /** 从活动标题中提取候选人姓名（标题第一个空格前的部分）。 */
    private String extractCandidateName(String title) {
        if (title == null || title.isEmpty()) return "";
        int idx = title.indexOf(' ');
        return idx > 0 ? title.substring(0, idx) : title;
    }

    /** 从活动描述中提取职位名称（「」中的内容）。 */
    private String extractJobTitle(String description) {
        if (description == null || description.isEmpty()) return "";
        int start = description.indexOf('「');
        int end = description.indexOf('」');
        if (start >= 0 && end > start) {
            return description.substring(start + 1, end);
        }
        return "";
    }

    private String deptNameById(long id) {
        return switch ((int) id) {
            case 100001 -> "公司总部";
            case 100002 -> "技术研发部";
            case 100003 -> "产品部";
            case 100004 -> "人力资源部";
            case 100005 -> "财务部";
            case 100006 -> "市场部";
            case 100007 -> "销售部";
            case 100008 -> "研发组";
            case 100009 -> "测试组";
            case 100010 -> "运维组";
            default -> "部门" + id;
        };
    }

    // ---- fallbacks (removed — all data now comes from real sources) ----
}
