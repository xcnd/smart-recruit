package com.smartrecruit.aiengine.service.impl;

import com.smartrecruit.aiengine.dto.request.LlmChatRequest;
import com.smartrecruit.aiengine.enums.AiEnums;
import com.smartrecruit.aiengine.service.AgentTaskRecorder;
import com.smartrecruit.aiengine.service.LlmFacadeService;
import com.smartrecruit.aiengine.service.LlmGatewayService;
import com.smartrecruit.common.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link LlmFacadeService} 实现类。
 *
 * <p>封装通用 LLM 对话的完整调用链路：
 * 服务可用性校验 → 任务开始记录（执行中）→ 网关调用 → 任务完成/失败记录。
 * 直接走 LLM 网关的调用也会按 Agent 记录任务与耗时指标。</p>
 *
 * @since 2026-04-12
 */
@Service
@Slf4j
public class LlmFacadeServiceImpl implements LlmFacadeService {

    /** Agent ID → 任务类型映射（用于直接走 LLM 网关的调用也记录调用量/延迟指标）。 */
    private static final Map<String, Integer> AGENT_TASK_TYPES = new HashMap<>();

    static {
        AGENT_TASK_TYPES.put("resume-parser", AiEnums.TaskType.RESUME_PARSE.getCode());
        AGENT_TASK_TYPES.put("smart-screener", AiEnums.TaskType.SCREEN.getCode());
        AGENT_TASK_TYPES.put("interview-evaluator", AiEnums.TaskType.EVALUATE.getCode());
        AGENT_TASK_TYPES.put("interview-question", AiEnums.TaskType.QUESTION_GEN.getCode());
    }

    @Autowired(required = false)
    private LlmGatewayService llmGatewayService;

    @Autowired(required = false)
    private AgentTaskRecorder taskRecorder;

    @Override
    public Map<String, Object> chat(LlmChatRequest request) {
        if (llmGatewayService == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "LLM 服务未启用，请设置 ai.llm.enabled=true 并配置 API Key");
        }
        long start = DateUtils.currentEpochMillis();
        log.info("[LLM 服务] 接收请求: systemPrompt长度={}, userPrompt长度={}",
                request.getSystemPrompt() != null ? request.getSystemPrompt().length() : 0,
                request.getUserPrompt() != null ? request.getUserPrompt().length() : 0);
        String agentId = request.getAgentId();
        Integer taskType = agentId == null ? null : AGENT_TASK_TYPES.get(agentId);
        // 真实任务生命周期：开始（执行中）→ 完成/失败（记录耗时与指标）
        Long taskId = (taskType != null && taskRecorder != null)
                ? taskRecorder.startTask(agentId, taskType, request.getUserPrompt())
                : null;
        try {
            Map<String, Object> result = llmGatewayService.chat(
                    request.getSystemPrompt(), request.getUserPrompt(), agentId);
            long elapsed = DateUtils.currentEpochMillis() - start;
            log.info("[LLM 服务] 响应完成: agentId={}, elapsed={}ms, resultKeys={}, resultSize={}",
                    agentId, elapsed, result.keySet(),
                    result.toString().length());
            if (taskId != null && taskId > 0 && taskRecorder != null) {
                taskRecorder.completeTask(taskId, true, result.toString(), elapsed, null);
            }
            return result;
        } catch (Exception e) {
            long elapsed = DateUtils.currentEpochMillis() - start;
            if (taskId != null && taskId > 0 && taskRecorder != null) {
                taskRecorder.completeTask(taskId, false, null, elapsed, e.getMessage());
            }
            throw e;
        }
    }
}
