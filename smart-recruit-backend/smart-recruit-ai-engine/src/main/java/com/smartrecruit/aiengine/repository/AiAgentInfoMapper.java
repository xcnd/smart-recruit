package com.smartrecruit.aiengine.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruit.aiengine.entity.AiAgentInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@code ai_agent_info} 表的 Mapper 接口。
 *
 * <p>提供智能体注册信息的增删改查，供 Agent 运维与监控使用。</p>
 *
 * @since 2026-04-08
 */
@Mapper
public interface AiAgentInfoMapper extends BaseMapper<AiAgentInfo> {
}
