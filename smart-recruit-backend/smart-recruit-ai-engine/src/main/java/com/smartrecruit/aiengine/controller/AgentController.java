package com.smartrecruit.aiengine.controller;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.aiengine.dto.response.*;
import com.smartrecruit.aiengine.dto.request.RegisterAgentRequest;
import com.smartrecruit.aiengine.dto.request.ConfigureAgentRequest;
import com.smartrecruit.aiengine.service.AgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * AI Agent 管理 REST 控制器。
 *
 * <p>暴露 Agent 列表、查看详情、暂停/恢复、
 * 配置、检查日志、任务队列和 SSE 事件流的端点。
 *
 * @author xdh
 * @since 2026-04-26
 */
@RestController
@RequestMapping({"/api/v1/agents", "/api/v2/agents"})
@RequiredArgsConstructor
@Slf4j
public class AgentController {

    private final AgentService agentService;

    /**
     * 注册一个新的 AI Agent 到注册表。
     *
     * @param request 注册信息（展示名称/层级/模型/描述/配置）
     * @return 注册后的智能体摘要信息
     */
    @PostMapping
    @PreAuthorize("hasAuthority('agent:manage')")
    public ApiResponse<AgentInfoVO> registerAgent(@Valid @RequestBody RegisterAgentRequest request) {
        log.info("POST /api/v1/agents (register): displayName={}, type={}",
                request.displayName(), request.type());
        return ApiResponse.success(agentService.registerAgent(
                request.displayName(),
                request.type(),
                request.model(),
                request.description(),
                request.config()));
    }

    /**
     * 列出所有已注册的 AI Agent。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('agent:view')")
    public ApiResponse<List<AgentInfoVO>> listAgents() {
        log.info("GET /api/v1/agents");
        return ApiResponse.success(agentService.listAgents());
    }

    /**
     * 获取特定 Agent 的详细信息。
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('agent:view')")
    public ApiResponse<AgentDetailVO> getAgentDetail(@PathVariable("id") String agentId) {
        log.info("GET /api/v1/agents/{}", agentId);
        return ApiResponse.success(agentService.getAgentDetail(agentId));
    }

    /**
     * 暂停一个 Agent（状态置为 PAUSED，能力调用将被拒绝）。
     */
    @PostMapping("/{id}/pause")
    @PreAuthorize("hasAuthority('agent:manage')")
    public ApiResponse<Void> pauseAgent(@PathVariable("id") String agentId) {
        log.info("POST /api/v1/agents/{}/pause", agentId);
        agentService.pauseAgent(agentId);
        return ApiResponse.success();
    }

    /**
     * 恢复一个已暂停的 Agent。
     *
     * @param agentId 智能体唯一标识
     * @return 操作结果
     */
    @PostMapping("/{id}/resume")
    @PreAuthorize("hasAuthority('agent:manage')")
    public ApiResponse<Void> resumeAgent(@PathVariable("id") String agentId) {
        log.info("POST /api/v1/agents/{}/resume", agentId);
        agentService.resumeAgent(agentId);
        return ApiResponse.success();
    }

    /**
     * 重启一个 Agent（状态恢复 RUNNING，并记录重启事件）。
     *
     * @param agentId 智能体唯一标识
     * @return 操作结果
     */
    @PostMapping("/{id}/restart")
    @PreAuthorize("hasAuthority('agent:manage')")
    public ApiResponse<Void> restartAgent(@PathVariable("id") String agentId) {
        log.info("POST /api/v1/agents/{}/restart", agentId);
        agentService.restartAgent(agentId);
        return ApiResponse.success();
    }

    /**
     * 使用自定义设置配置 Agent。
     *
     * @param agentId 智能体唯一标识
     * @param request 配置键值映射
     * @return 操作结果
     */
    @PutMapping("/{id}/config")
    @PreAuthorize("hasAuthority('agent:manage')")
    public ApiResponse<Void> updateConfig(@PathVariable("id") String agentId,
                                          @RequestBody ConfigureAgentRequest request) {
        log.info("PUT /api/v1/agents/{}/config", agentId);
        agentService.configureAgent(agentId, request.getConfig());
        return ApiResponse.success();
    }

    /**
     * 使用自定义设置配置 Agent（兼容旧路径 POST /configure）。
     */
    @PostMapping("/{id}/configure")
    @PreAuthorize("hasAuthority('agent:manage')")
    public ApiResponse<Void> configureAgent(@PathVariable("id") String agentId,
                                             @RequestBody ConfigureAgentRequest request) {
        log.info("POST /api/v1/agents/{}/configure", agentId);
        agentService.configureAgent(agentId, request.getConfig());
        return ApiResponse.success();
    }

    /**
     * 获取 Agent 最近的执行日志。
     */
    @GetMapping("/{id}/logs")
    @PreAuthorize("hasAuthority('agent:view')")
    public ApiResponse<List<AgentLogVO>> getAgentLogs(@PathVariable("id") String agentId) {
        log.info("GET /api/v1/agents/{}/logs", agentId);
        return ApiResponse.success(agentService.getAgentLogs(agentId));
    }

    /**
     * 获取当前活跃的任务队列。
     */
    @GetMapping({"/tasks", "/task-queue"})
    @PreAuthorize("hasAuthority('agent:view')")
    public ApiResponse<List<AgentTaskVO>> getTaskQueue() {
        log.info("GET /api/v1/agents/tasks");
        return ApiResponse.success(agentService.getTaskQueue());
    }

    /**
     * 用于实时 Agent 事件的 SSE 流式端点。
     *
     * <p>每 2 秒增量推送真实 Agent 事件（读取 ai_event_log），持续 60 秒。
     */
    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAuthority('agent:view')")
    public SseEmitter streamEvents() {
        log.info("GET /api/v1/agents/events (SSE stream started)");
        // 30 分钟超时：避免浏览器 EventSource 频繁断开重连；
        // 连接自然超时/断开由全局异常处理器静默处理，浏览器会自动重连
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        String[] lastEventId = {null};
        executor.scheduleAtFixedRate(() -> {
            try {
                List<AgentEventVO> events = agentService.getEventsAfter(lastEventId[0]);
                for (AgentEventVO event : events) {
                    emitter.send(SseEmitter.event()
                            .id(event.getEventId())
                            .name("agent-event")
                            .data(event));
                    lastEventId[0] = event.getEventId();
                }
            } catch (IOException e) {
                log.warn("SSE send error: {}", e.getMessage());
                emitter.completeWithError(e);
                executor.shutdown();
            }
        }, 0, 2, TimeUnit.SECONDS);

        emitter.onCompletion(() -> {
            log.info("SSE stream completed");
            executor.shutdown();
        });
        emitter.onTimeout(() -> {
            log.info("SSE stream timed out");
            executor.shutdown();
        });
        emitter.onError((e) -> {
            log.error("SSE stream error: {}", e.getMessage());
            executor.shutdown();
        });

        return emitter;
    }
}
