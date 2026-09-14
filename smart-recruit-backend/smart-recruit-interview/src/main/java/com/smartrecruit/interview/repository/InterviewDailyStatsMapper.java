package com.smartrecruit.interview.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruit.interview.entity.InterviewDailyStats;
import org.apache.ibatis.annotations.Mapper;

/**
 * 面试日汇总 Mapper。
 *
 * @since 2026-04-06
 */
@Mapper
public interface InterviewDailyStatsMapper extends BaseMapper<InterviewDailyStats> {
}
