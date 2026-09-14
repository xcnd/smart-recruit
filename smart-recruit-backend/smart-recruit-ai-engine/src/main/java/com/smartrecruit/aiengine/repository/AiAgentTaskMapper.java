package com.smartrecruit.aiengine.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruit.aiengine.entity.AiAgentTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * {@code ai_agent_task}表的Mapper。
 *
 * @author xdh
 * @since 2026-04-26
 */
@Mapper
public interface AiAgentTaskMapper extends BaseMapper<AiAgentTask> {

    /**
     * 查询最近任务（执行中/排队优先，其次最近完成/失败），供任务队列展示真实数据。
     */
    List<AiAgentTask> selectRecentTasks();

    /**
     * 查找指定智能体的近期任务。
     */
    List<AiAgentTask> selectByAgentName(@Param("agentName") String agentName,
                                         @Param("limit") int limit);
}
