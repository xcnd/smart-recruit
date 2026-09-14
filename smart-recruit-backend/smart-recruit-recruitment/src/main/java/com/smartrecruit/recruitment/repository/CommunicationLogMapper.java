package com.smartrecruit.recruitment.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruit.recruitment.entity.CommunicationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@code rec_communication_log} 表的 Mapper 接口。
 *
 * @since 1.0.0
 */
@Mapper
public interface CommunicationLogMapper extends BaseMapper<CommunicationLog> {
}
