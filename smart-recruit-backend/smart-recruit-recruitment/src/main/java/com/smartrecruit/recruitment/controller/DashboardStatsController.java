package com.smartrecruit.recruitment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.recruitment.dto.response.DashboardKpiVO;
import com.smartrecruit.recruitment.dto.response.DepartmentProgressVO;
import com.smartrecruit.recruitment.dto.response.FunnelStageVO;
import com.smartrecruit.recruitment.entity.Candidate;
import com.smartrecruit.recruitment.entity.JobPosition;
import com.smartrecruit.recruitment.repository.CandidateMapper;
import com.smartrecruit.recruitment.repository.JobPositionMapper;
import com.smartrecruit.common.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 工作台数据统计控制器，提供KPI、漏斗、部门进度等聚合数据。
 * 供 Talent 模块的 DashboardServiceImpl 通过 Feign 调用。
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class DashboardStatsController {

    private final JobPositionMapper jobPositionMapper;
    private final CandidateMapper candidateMapper;

    /** 工作台 KPI 统计。 */
    @GetMapping("/dashboard/kpi")
    public ApiResponse<DashboardKpiVO> getKpi() {
        // 在招职位数
        long activeJobCount = jobPositionMapper.selectCount(
                new LambdaQueryWrapper<JobPosition>()
                        .eq(JobPosition::getDeleted, 0)
                        .eq(JobPosition::getStatus, 1));

        // 时间范围：今日、昨日、本月
        LocalDate today = DateUtils.today();
        LocalDateTime todayStart = DateUtils.beginOfDay(today);
        LocalDateTime yesterdayStart = todayStart.minusDays(1);
        LocalDateTime monthStart = DateUtils.beginOfDay(today.withDayOfMonth(1));

        long todayNewCandidateCount = candidateMapper.selectCount(
                new LambdaQueryWrapper<Candidate>()
                        .eq(Candidate::getDeleted, 0)
                        .ge(Candidate::getCreatedAt, todayStart));
        long yesterdayNewCandidateCount = candidateMapper.selectCount(
                new LambdaQueryWrapper<Candidate>()
                        .eq(Candidate::getDeleted, 0)
                        .ge(Candidate::getCreatedAt, yesterdayStart)
                        .lt(Candidate::getCreatedAt, todayStart));
        long monthlyNewCandidateCount = candidateMapper.selectCount(
                new LambdaQueryWrapper<Candidate>()
                        .eq(Candidate::getDeleted, 0)
                        .ge(Candidate::getCreatedAt, monthStart));

        return ApiResponse.success(DashboardKpiVO.builder()
                .activeJobCount(activeJobCount)
                .todayNewCandidateCount(todayNewCandidateCount)
                .yesterdayNewCandidateCount(yesterdayNewCandidateCount)
                .monthlyNewCandidateCount(monthlyNewCandidateCount)
                .build());
    }

    /** 招聘漏斗数据：按候选人阶段分布。 */
    @GetMapping("/dashboard/funnel")
    public ApiResponse<List<FunnelStageVO>> getFunnel() {
        List<Map<String, Object>> dist = candidateMapper.stageDistribution();
        Map<Integer, Long> stageCounts = new HashMap<>();
        for (Map<String, Object> row : dist) {
            Object stageObj = row.get("stage");
            Object countObj = row.get("count");
            if (stageObj == null || countObj == null) continue;
            int stage = Integer.parseInt(stageObj.toString());
            long count = Long.parseLong(countObj.toString());
            stageCounts.put(stage, stageCounts.getOrDefault(stage, 0L) + count);
        }

        String[] stageNames = {"新入库", "筛选中", "筛选通过", "面试中", "已发Offer", "已入职", "已淘汰", "已放弃"};
        List<FunnelStageVO> funnel = new ArrayList<>();
        for (int i = 0; i < stageNames.length; i++) {
            funnel.add(FunnelStageVO.builder()
                    .name(stageNames[i])
                    .stage(i)
                    .count(stageCounts.getOrDefault(i, 0L))
                    .build());
        }
        return ApiResponse.success(funnel);
    }

    /** 部门招聘进度：各部门在招职位、计划人数、已入职人数、完成率。 */
    @GetMapping("/dashboard/department-progress")
    public ApiResponse<List<DepartmentProgressVO>> getDepartmentProgress() {
        List<DepartmentProgressVO> list = jobPositionMapper.departmentProgress();
        // Calculate progress percentage
        for (DepartmentProgressVO vo : list) {
            if (vo.getPlannedHeadCount() > 0) {
                double pct = vo.getHiredCount() * 100.0 / vo.getPlannedHeadCount();
                vo.setProgressPercent(Math.round(pct * 10.0) / 10.0);
            } else {
                vo.setProgressPercent(0.0);
            }
        }
        return ApiResponse.success(list);
    }
}
