package com.smartrecruit.recruitment.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruit.recruitment.entity.CandidateDailyStats;
import org.apache.ibatis.annotations.Mapper;

/**
 * 候选人日汇总 Mapper。
 *
 * @since 2026-04-06
 */
@Mapper
public interface CandidateDailyStatsMapper extends BaseMapper<CandidateDailyStats> {
}
