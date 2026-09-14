package com.smartrecruit.referral.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruit.referral.entity.ReferralMatchResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * 内推智能匹配结果 Mapper。
 *
 * @since 2026-04-06
 */
@Mapper
public interface ReferralMatchResultMapper extends BaseMapper<ReferralMatchResult> {
}
