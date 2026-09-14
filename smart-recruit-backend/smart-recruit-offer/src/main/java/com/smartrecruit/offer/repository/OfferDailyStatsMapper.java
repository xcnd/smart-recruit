package com.smartrecruit.offer.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruit.offer.entity.OfferDailyStats;
import org.apache.ibatis.annotations.Mapper;

/**
 * Offer 日汇总 Mapper。
 *
 * @since 2026-04-06
 */
@Mapper
public interface OfferDailyStatsMapper extends BaseMapper<OfferDailyStats> {
}
