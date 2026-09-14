package com.smartrecruit.recruitment.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruit.recruitment.entity.Candidate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * {@code rec_candidate} 表的 Mapper 接口。
 *
 * @since 1.0.0
 */
@Mapper
public interface CandidateMapper extends BaseMapper<Candidate> {

    /**
     * 分页查询，支持动态过滤条件。
     */
    IPage<Candidate> pageQuery(Page<Candidate> page,
                               @Param("name") String name,
                               @Param("keyword") String keyword,
                               @Param("stage") Integer stage,
                               @Param("source") Integer source,
                               @Param("education") Integer education,
                               @Param("aiMatchScoreMin") Integer aiMatchScoreMin,
                               @Param("aiMatchScoreMax") Integer aiMatchScoreMax,
                               @Param("experienceMin") Integer experienceMin,
                               @Param("experienceMax") Integer experienceMax,
                               @Param("city") String city,
                               @Param("applyDateStart") String applyDateStart,
                               @Param("applyDateEnd") String applyDateEnd,
                               @Param("screeningStatus") Integer screeningStatus);

    /**
     * 按当前阶段分组统计候选人数。
     */
    List<Map<String, Object>> stageDistribution();

    /**
     * 按来源渠道分组统计候选人数。
     */
    List<Map<String, Object>> sourceDistribution();

    /**
     * 按月统计新增候选人数（最近N个月）。
     */
    List<Map<String, Object>> monthlyTrend(@Param("months") int months);

    /**
     * 按当前阶段分组统计指定时间范围内新增的候选人数。
     */
    List<Map<String, Object>> stageDistributionRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * 按来源渠道分组统计指定时间范围内新增的候选人数。
     */
    List<Map<String, Object>> sourceDistributionRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * 按来源渠道分组统计指定时间范围内新增、且当前已入职的候选人数。
     */
    List<Map<String, Object>> sourceHireDistributionRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * 按自然月统计新增候选人数（最近 N 个月，含当月）。
     */
    List<Map<String, Object>> monthlyTrendRange(@Param("months") int months);

    /**
     * 按"日期范围 + 阶段 + 来源"统计新增候选人数（定时任务日汇总用）。
     */
    List<Map<String, Object>> dailyStageSourceCounts(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
