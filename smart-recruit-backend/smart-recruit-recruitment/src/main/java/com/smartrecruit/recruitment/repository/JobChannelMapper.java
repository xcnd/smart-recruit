package com.smartrecruit.recruitment.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruit.recruitment.entity.JobChannel;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@code rec_job_channel} 表的 Mapper 接口。
 *
 * @since 1.0.0
 */
@Mapper
public interface JobChannelMapper extends BaseMapper<JobChannel> {
}
