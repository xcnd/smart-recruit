package com.smartrecruit.aiengine.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruit.aiengine.entity.AiEventLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * {@code ai_event_log}表的Mapper。
 *
 * @author xdh
 * @since 2026-05-04
 */
@Mapper
public interface AiEventLogMapper extends BaseMapper<AiEventLog> {

    /**
     * 查找指定任务的事件日志，按创建时间降序排列。
     */
    List<AiEventLog> selectByTaskId(@Param("taskId") Long taskId);

    /**
     * 查找指定智能体的事件日志。
     */
    List<AiEventLog> selectByAgentName(@Param("agentName") String agentName);
}
