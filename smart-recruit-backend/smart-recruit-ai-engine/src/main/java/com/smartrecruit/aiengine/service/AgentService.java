package com.smartrecruit.aiengine.service;

import com.smartrecruit.aiengine.dto.response.AgentDetailVO;
import com.smartrecruit.aiengine.dto.response.AgentEventVO;
import com.smartrecruit.aiengine.dto.response.AgentInfoVO;
import com.smartrecruit.aiengine.dto.response.AgentLogVO;
import com.smartrecruit.aiengine.dto.response.AgentTaskVO;

import java.util.List;
import java.util.Map;

/**
 * AI Agent 管理服务接口。
 *
 * @author xdh
 * @since 2026-04-26
 */
public interface AgentService {

    /**
     * 列出所有已注册的 AI Agent 及其摘要信息。
     */
    List<AgentInfoVO> listAgents();

    /**
     * 获取特定 Agent 的详细信息。
     */
    AgentDetailVO getAgentDetail(String agentId);

    /**
     * 暂停一个 Agent（状态置为 PAUSED，能力调用将被拒绝）。
     */
    void pauseAgent(String agentId);

    /**
     * 恢复一个 Agent（状态置为 RUNNING，能力调用恢复正常）。
     */
    void resumeAgent(String agentId);

    /**
     * 重启一个 Agent（状态置为 RUNNING，并记录重启事件）。
     */
    void restartAgent(String agentId);

    /**
     * 注册一个新的 Agent 到注册表。
     *
     * @param displayName 展示名称
     * @param type        层级：0=编排层,1=执行层,2=复盘层
     * @param model       绑定模型
     * @param description 职责描述
     * @param config      运行配置
     * @return 注册后的智能体摘要信息
     */
    AgentInfoVO registerAgent(String displayName, int type, String model,
                              String description, Map<String, Object> config);

    /**
     * 判断 Agent 当前是否可被调用（启用且未暂停）。
     *
     * @param agentId 智能体唯一标识
     * @return true 表示可调用；注册表中不存在的 Agent 默认可调用
     */
    boolean isAgentAvailable(String agentId);

    /**
     * 使用自定义设置配置一个 Agent。
     */
    void configureAgent(String agentId, Map<String, Object> config);

    /**
     * 获取一个 Agent 最近的执行日志。
     */
    List<AgentLogVO> getAgentLogs(String agentId);

    /**
     * 获取当前活跃的任务队列。
     */
    List<AgentTaskVO> getTaskQueue();

    /**
     * 获取流式 Agent 事件（真实事件日志）。
     */
    List<AgentEventVO> getEvents();

    /**
     * 获取指定事件 ID 之后的新增 Agent 事件（SSE 增量推送）。
     *
     * @param lastEventId 已推送的最大事件 ID（null 表示首次连接，返回最近事件）
     */
    List<AgentEventVO> getEventsAfter(String lastEventId);
}
