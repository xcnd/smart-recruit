package com.smartrecruit.recruitment.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruit.recruitment.entity.ActivityFeed;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@code rec_activity_feed} 表的 Mapper 接口。
 *
 * @since 1.0.0
 */
@Mapper
public interface ActivityFeedMapper extends BaseMapper<ActivityFeed> {
}
