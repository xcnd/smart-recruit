package com.smartrecruit.talent.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruit.talent.entity.TalentCampaign;
import org.apache.ibatis.annotations.Mapper;

/**
 * 人才活动 Mapper，提供 CRUD 操作。
 *
 * @since 1.0.0
 */
@Mapper
public interface TalentCampaignMapper extends BaseMapper<TalentCampaign> {
}
