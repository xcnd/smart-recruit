package com.smartrecruit.talent.service;

import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.talent.dto.response.*;

import java.util.List;

/**
 * 仪表板服务接口，为 HR 仪表板提供 KPI 汇总、漏斗、部门进度、
 * 最近活动和待办任务数据。
 *
 * @since 1.0.0
 */
public interface DashboardService {

    /**
     * 获取带趋势指标的 KPI 汇总。
     */
    DashboardKpiVO getKpi();

    /**
     * 获取用于仪表板可视化的招聘漏斗数据。
     */
    List<FunnelStageVO> getFunnel();

    /**
     * 获取部门级别的招聘进度。
     */
    List<DepartmentProgressVO> getDepartmentProgress();

    /**
     * 获取最近活动动态条目。
     */
    List<ActivityVO> getRecentActivities();

    /**
     * 获取需要关注的待办任务（按当前用户过滤）。
     */
    List<PendingTaskVO> getPendingTasks(Long userId);

    /**
     * 完成待办任务。
     */
    void completeTask(Long id);

    /**
     * 忽略待办任务。
     */
    void dismissTask(Long id);

    /** 分页查询活动动态。 */
    PageResult<ActivityVO> getActivitiesPage(int page, int size, Integer type, String keyword);

    /** 分页查询待办任务（按当前用户过滤）。 */
    PageResult<PendingTaskVO> getTasksPage(Long userId, int page, int size, Integer type,
                                           Integer priority, Integer status, String keyword);
}
