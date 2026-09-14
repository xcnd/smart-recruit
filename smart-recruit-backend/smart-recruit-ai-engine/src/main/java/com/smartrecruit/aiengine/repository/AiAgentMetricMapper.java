package com.smartrecruit.aiengine.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruit.aiengine.entity.AiAgentMetric;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * {@code ai_agent_metric}表的Mapper。
 *
 * @author xdh
 * @since 2026-05-04
 */
@Mapper
public interface AiAgentMetricMapper extends BaseMapper<AiAgentMetric> {

    /**
     * 查找指定智能体的指标数据，按指标时间降序排列。
     */
    List<AiAgentMetric> selectByAgentName(@Param("agentName") String agentName);
}
