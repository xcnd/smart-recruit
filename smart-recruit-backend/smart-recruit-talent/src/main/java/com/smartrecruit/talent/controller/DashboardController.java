package com.smartrecruit.talent.controller;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.common.util.UserContextUtil;
import com.smartrecruit.talent.dto.response.*;
import com.smartrecruit.talent.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 仪表板 REST 控制器。
 *
 * <p>端点：KPI、漏斗、部门进度、最近活动、待办任务。</p>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 查询工作台核心 KPI 指标。
     */
    @GetMapping("/kpi")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public ApiResponse<DashboardKpiVO> kpi() {
        log.info("Dashboard KPI requested");
        DashboardKpiVO kpi = dashboardService.getKpi();
        return ApiResponse.success(kpi);
    }

    /**
     * 查询招聘漏斗数据。
     */
    @GetMapping("/funnel")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public ApiResponse<List<FunnelStageVO>> funnel() {
        log.info("Dashboard funnel requested");
        List<FunnelStageVO> funnel = dashboardService.getFunnel();
        return ApiResponse.success(funnel);
    }

    /**
     * 查询各部门招聘进度。
     */
    @GetMapping("/department-progress")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public ApiResponse<List<DepartmentProgressVO>> departmentProgress() {
        log.info("Department progress requested");
        List<DepartmentProgressVO> progress = dashboardService.getDepartmentProgress();
        return ApiResponse.success(progress);
    }

    /**
     * 查询最近动态列表。
     */
    @GetMapping("/recent-activities")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public ApiResponse<List<ActivityVO>> recentActivities() {
        log.info("Recent activities requested");
        List<ActivityVO> activities = dashboardService.getRecentActivities();
        return ApiResponse.success(activities);
    }

    /**
     * 查询当前用户的待办任务列表。
     */
    @GetMapping("/pending-tasks")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public ApiResponse<List<PendingTaskVO>> pendingTasks() {
        Long userId = UserContextUtil.getCurrentUserId();
        log.info("Pending tasks requested by userId={}", userId);
        List<PendingTaskVO> tasks = dashboardService.getPendingTasks(userId);
        return ApiResponse.success(tasks);
    }

    /** 完成待办任务。 */
    @PutMapping("/pending-tasks/{id}/complete")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public ApiResponse<Void> completeTask(@PathVariable Long id) {
        dashboardService.completeTask(id);
        return ApiResponse.success(null);
    }

    /** 忽略待办任务。 */
    @PutMapping("/pending-tasks/{id}/dismiss")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public ApiResponse<Void> dismissTask(@PathVariable Long id) {
        dashboardService.dismissTask(id);
        return ApiResponse.success(null);
    }

    /** 分页查询活动动态。 */
    @GetMapping("/activities")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public ApiResponse<PageResult<ActivityVO>> activitiesPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(dashboardService.getActivitiesPage(page, size, type, keyword));
    }

    /** 分页查询待办任务。 */
    @GetMapping("/tasks")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public ApiResponse<PageResult<PendingTaskVO>> tasksPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer priority,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        Long userId = UserContextUtil.getCurrentUserId();
        return ApiResponse.success(dashboardService.getTasksPage(userId, page, size, type, priority, status, keyword));
    }
}
