package com.smartrecruit.aiengine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartrecruit.aiengine.config.AgentScopeLifecycleManager;
import com.smartrecruit.aiengine.dto.response.AgentDetailVO;
import com.smartrecruit.aiengine.dto.response.AgentEventVO;
import com.smartrecruit.aiengine.dto.response.AgentInfoVO;
import com.smartrecruit.aiengine.dto.response.AgentLogVO;
import com.smartrecruit.aiengine.dto.response.AgentTaskVO;
import com.smartrecruit.aiengine.entity.AiAgentInfo;
import com.smartrecruit.aiengine.entity.AiAgentMetric;
import com.smartrecruit.aiengine.entity.AiAgentTask;
import com.smartrecruit.aiengine.entity.AiEventLog;
import com.smartrecruit.aiengine.enums.AiEnums;
import com.smartrecruit.aiengine.repository.AiAgentInfoMapper;
import com.smartrecruit.aiengine.repository.AiAgentMetricMapper;
import com.smartrecruit.aiengine.repository.AiAgentTaskMapper;
import com.smartrecruit.aiengine.repository.AiEventLogMapper;
import com.smartrecruit.aiengine.service.AgentService;
import com.smartrecruit.common.exception.ResourceNotFoundException;
import com.smartrecruit.common.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI Agent 管理服务实现 —— 基于数据库的真实 Agent 运维与监控。
 *
 * <p>Agent 注册信息来自 {@code ai_agent_info} 表，运行指标来自
 * {@code ai_agent_metric} 表，任务与事件分别来自 {@code ai_agent_task}、
 * {@code ai_event_log} 表。暂停/恢复/重启/注册/配置等运维操作均落库生效，
 * 并同步写入真实事件日志（SSE 事件流数据源）。</p>
 *
 * @since 2026-04-08
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    /** 日志查询条数上限。 */
    private static final int LOG_LIMIT = 50;

    /** 运行状态常量（与 ai_agent_info.status 对齐）。 */
    private static final int STATUS_RUNNING = 0;
    private static final int STATUS_IDLE = 1;
    private static final int STATUS_PAUSED = 2;
    private static final int STATUS_ERROR = 3;

    private final AiAgentInfoMapper agentInfoMapper;
    private final AiAgentTaskMapper taskMapper;
    private final AiAgentMetricMapper metricMapper;
    private final AiEventLogMapper eventLogMapper;
    private final ObjectProvider<AgentScopeLifecycleManager> lifecycleManagerProvider;

    /** agentId → 展示名称 的内存缓存，避免事件流逐条查库。 */
    private final Map<String, String> displayNameCache = new ConcurrentHashMap<>();

    /** 查询 AI 智能体列表（真实注册表 + 最近指标聚合）。 */
    @Override
    public List<AgentInfoVO> listAgents() {
        List<AiAgentInfo> infos = agentInfoMapper.selectList(
                new LambdaQueryWrapper<AiAgentInfo>()
                        .eq(AiAgentInfo::getEnabled, 1)
                        .orderByAsc(AiAgentInfo::getId));
        return infos.stream().map(this::toInfoVO).toList();
    }

    /** 查询智能体详情与运行指标。 */
    @Override
    public AgentDetailVO getAgentDetail(String agentId) {
        AiAgentInfo info = requireAgent(agentId);
        AiAgentMetric metric = latestMetric(agentId);

        long totalTasks = countTasks(agentId, null);
        long activeTasks = countActiveTasks(agentId);

        return AgentDetailVO.builder()
                .agentId(info.getAgentId())
                .name(info.getDisplayName())
                .description(info.getDescription())
                .model(info.getModel())
                .health(String.valueOf(metric != null ? num(metric.getHealthPct()) : 0.0))
                .status(info.getStatus())
                .config(buildConfig(info, metric))
                .callsPerHour(metric != null ? callsPerHour(metric) : 0)
                .latencyMs(metric != null ? (int) Math.round(num(metric.getAvgLatencyMs())) : 0)
                .successRate(metric != null ? num(metric.getAccuracyPct()) : 0.0)
                .totalTasks(totalTasks)
                .activeTasks(activeTasks)
                .build();
    }

    /** 暂停智能体（状态落库，能力调用将被拒绝）。 */
    @Override
    public void pauseAgent(String agentId) {
        AiAgentInfo info = requireAgent(agentId);
        updateStatus(info, STATUS_PAUSED);
        recordEvent(info, AiEnums.EventType.WARNING.getCode(), "Agent 已暂停",
                "智能体 " + info.getDisplayName() + " 已被管理员暂停，后续能力调用将直接拒绝");
        log.info("Agent 已暂停: agentId={}", agentId);
    }

    /** 恢复智能体（状态置为 RUNNING）。 */
    @Override
    public void resumeAgent(String agentId) {
        AiAgentInfo info = requireAgent(agentId);
        updateStatus(info, STATUS_RUNNING);
        recordEvent(info, AiEnums.EventType.SUCCESS.getCode(), "Agent 已恢复",
                "智能体 " + info.getDisplayName() + " 已恢复运行，能力调用恢复正常");
        log.info("Agent 已恢复: agentId={}", agentId);
    }

    /** 重启智能体（状态置为 RUNNING，并记录重启事件）。 */
    @Override
    public void restartAgent(String agentId) {
        AiAgentInfo info = requireAgent(agentId);
        updateStatus(info, STATUS_RUNNING);

        // 真正重建 AgentScope 运行时实例（interview-evaluator / interview-orchestrator），
        // 失败时标记异常并记录失败事件，而不是静默假装重启成功
        AgentScopeLifecycleManager lifecycle = lifecycleManagerProvider.getIfAvailable();
        if (lifecycle != null) {
            try {
                lifecycle.rebuild(agentId);
            } catch (Exception e) {
                log.error("AgentScope 运行时重建失败，标记异常: agentId={}", agentId, e);
                updateStatus(info, STATUS_ERROR);
                recordEvent(info, AiEnums.EventType.ERROR.getCode(), "Agent 重启失败",
                        "智能体 " + info.getDisplayName()
                                + " 重启时 AgentScope 运行时重建失败：" + e.getMessage());
                throw new IllegalStateException("Agent 重启失败：" + e.getMessage(), e);
            }
        }

        recordEvent(info, AiEnums.EventType.INFO.getCode(), "Agent 已重启",
                "智能体 " + info.getDisplayName() + " 完成重启，运行时与配置已重新加载");
        log.info("Agent 已重启: agentId={}", agentId);
    }

    /** 注册新智能体到注册表。 */
    @Override
    public AgentInfoVO registerAgent(String displayName, int type, String model,
                                     String description, Map<String, Object> config) {
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Agent 名称不能为空");
        }
        String agentId = toAgentId(displayName);
        Long exists = agentInfoMapper.selectCount(
                new LambdaQueryWrapper<AiAgentInfo>()
                        .eq(AiAgentInfo::getAgentId, agentId));
        if (exists != null && exists > 0) {
            throw new IllegalArgumentException("Agent 已存在: agentId=" + agentId);
        }

        LocalDateTime now = DateUtils.now();
        AiAgentInfo info = new AiAgentInfo();
        info.setAgentId(agentId);
        info.setAgentName(toClassName(displayName));
        info.setDisplayName(displayName);
        info.setModel(model == null || model.isBlank() ? "deepseek-v4" : model);
        info.setDescription(description);
        info.setType(type);
        info.setStatus(STATUS_RUNNING);
        info.setConfig(config == null ? Map.of() : config);
        info.setEnabled(1);
        info.setCreatedAt(now);
        info.setUpdatedAt(now);
        agentInfoMapper.insert(info);
        displayNameCache.put(agentId, displayName);

        recordEvent(info, AiEnums.EventType.SUCCESS.getCode(), "Agent 已注册",
                "智能体 " + displayName + " 已注册到 AgentScope Runtime，绑定模型: " + info.getModel());
        log.info("Agent 注册成功: agentId={}, displayName={}, type={}", agentId, displayName, type);
        return toInfoVO(info);
    }

    /** 判断 Agent 是否可调用（启用且未暂停/未故障）。 */
    @Override
    public boolean isAgentAvailable(String agentId) {
        AiAgentInfo info = agentInfoMapper.selectOne(
                new LambdaQueryWrapper<AiAgentInfo>()
                        .eq(AiAgentInfo::getAgentId, agentId));
        // 注册表中不存在的 Agent 视为可调用，避免影响未纳入监控的存量能力
        if (info == null) {
            return true;
        }
        return Integer.valueOf(1).equals(info.getEnabled())
                && info.getStatus() != null
                && info.getStatus() != STATUS_PAUSED
                && info.getStatus() != STATUS_ERROR;
    }

    /** 配置智能体参数（配置落库，立即生效）。 */
    @Override
    @SuppressWarnings("unchecked")
    public void configureAgent(String agentId, Map<String, Object> config) {
        AiAgentInfo info = requireAgent(agentId);
        Map<String, Object> merged = new LinkedHashMap<>();
        if (info.getConfig() instanceof Map<?, ?> existing) {
            merged.putAll((Map<String, Object>) existing);
        }
        if (config != null) {
            merged.putAll(config);
        }
        info.setConfig(merged);
        info.setUpdatedAt(DateUtils.now());
        agentInfoMapper.updateById(info);
        recordEvent(info, AiEnums.EventType.INFO.getCode(), "Agent 配置已更新",
                "智能体 " + info.getDisplayName() + " 的配置已更新: " + config);
        log.info("Agent 配置成功: agentId={}, config={}", agentId, config);
    }

    /** 查询智能体日志列表（真实任务 + 真实事件日志，无模拟填充）。 */
    @Override
    public List<AgentLogVO> getAgentLogs(String agentId) {
        AiAgentInfo info = requireAgent(agentId);
        List<AgentLogVO> logs = new ArrayList<>();

        // 1. 最近任务记录
        List<AiAgentTask> recentTasks = taskMapper.selectByAgentName(info.getAgentId(), LOG_LIMIT);
        for (AiAgentTask task : recentTasks) {
            logs.add(AgentLogVO.builder()
                    .id(task.getId())
                    .agentId(agentId)
                    .agentName(task.getAgentName())
                    .taskType(task.getTaskType())
                    .status(task.getStatus())
                    .durationMs(task.getDurationMs())
                    .inputSummary(truncate(task.getInputData(), 160))
                    .outputSummary(truncate(task.getOutputData(), 160))
                    .startedAt(task.getStartedAt())
                    .completedAt(task.getCompletedAt())
                    .build());
        }

        // 2. 最近事件日志（含运维操作：暂停/恢复/重启/配置）
        List<AiEventLog> events = eventLogMapper.selectList(
                new LambdaQueryWrapper<AiEventLog>()
                        .eq(AiEventLog::getAgentName, info.getAgentId())
                        .orderByDesc(AiEventLog::getCreatedAt)
                        .last("LIMIT " + LOG_LIMIT));
        for (AiEventLog event : events) {
            logs.add(AgentLogVO.builder()
                    .id(event.getId())
                    .agentId(agentId)
                    .agentName(event.getAgentName())
                    .status(mapEventToTaskStatus(event.getEventType()))
                    .durationMs(event.getData() instanceof Map<?, ?> data
                            && data.get("durationMs") instanceof Number n ? n.longValue() : null)
                    .outputSummary(event.getMessage())
                    .startedAt(event.getCreatedAt())
                    .build());
        }

        // 按开始时间倒序（事件优先展示最新）
        logs.sort((a, b) -> {
            LocalDateTime ta = a.getStartedAt() != null ? a.getStartedAt() : LocalDateTime.MIN;
            LocalDateTime tb = b.getStartedAt() != null ? b.getStartedAt() : LocalDateTime.MIN;
            return tb.compareTo(ta);
        });
        return logs.size() > LOG_LIMIT ? logs.subList(0, LOG_LIMIT) : logs;
    }

    /** 查询智能体任务队列（真实活跃任务，无模拟填充）。 */
    @Override
    public List<AgentTaskVO> getTaskQueue() {
        List<AiAgentTask> recentTasks = taskMapper.selectRecentTasks();
        if (recentTasks == null || recentTasks.isEmpty()) {
            return Collections.emptyList();
        }
        return recentTasks.stream().map(task -> AgentTaskVO.builder()
                .id(String.valueOf(task.getId()))
                .agentId(task.getAgentName())
                .agentName(displayNameOf(task.getAgentName()))
                .type(mapTaskType(task.getTaskType()))
                .priority(mapPriority(task.getPriority()))
                .status(mapTaskStatus(task.getStatus()))
                .createdAt(task.getCreatedAt())
                .startedAt(task.getStartedAt())
                .durationMs(task.getDurationMs())
                .build()).toList();
    }

    /** 查询智能体事件流（SSE 数据源，真实事件日志）。 */
    @Override
    public List<AgentEventVO> getEvents() {
        return getEventsAfter(null);
    }

    /** 查询指定事件 ID 之后的增量事件流。 */
    @Override
    public List<AgentEventVO> getEventsAfter(String lastEventId) {
        LambdaQueryWrapper<AiEventLog> wrapper = new LambdaQueryWrapper<>();
        if (lastEventId != null && !lastEventId.isBlank()) {
            try {
                wrapper.gt(AiEventLog::getId, Long.parseLong(lastEventId));
            } catch (NumberFormatException e) {
                // 非法 ID 忽略，走首次全量
            }
            wrapper.orderByAsc(AiEventLog::getId).last("LIMIT 100");
        } else {
            List<AiEventLog> latest = eventLogMapper.selectList(
                    new LambdaQueryWrapper<AiEventLog>()
                            .orderByDesc(AiEventLog::getId)
                            .last("LIMIT 50"));
            Collections.reverse(latest);
            return latest.stream().map(this::toEventVO).toList();
        }
        return eventLogMapper.selectList(wrapper).stream()
                .map(this::toEventVO)
                .toList();
    }

    // ================================================================
    // 私有辅助方法
    // ================================================================

    /** 将注册信息 + 最近指标聚合为列表 VO。 */
    private AgentInfoVO toInfoVO(AiAgentInfo info) {
        AiAgentMetric metric = latestMetric(info.getAgentId());
        long tasksCompleted = countTasks(info.getAgentId(), AiEnums.TaskStatus.COMPLETED.getCode());
        long activeTasks = countActiveTasks(info.getAgentId());

        return AgentInfoVO.builder()
                .id(info.getAgentId())
                .name(info.getDisplayName())
                .description(info.getDescription())
                .type(info.getType())
                .status(info.getStatus())
                .health(metric != null ? round1(num(metric.getHealthPct())) : 0.0)
                .metrics(AgentInfoVO.AgentMetricsVO.builder()
                        .tasksCompleted(tasksCompleted)
                        .avgResponseTime(metric != null ? (int) Math.round(num(metric.getAvgLatencyMs())) : 0)
                        .successRate(metric != null ? round1(num(metric.getAccuracyPct())) : 0.0)
                        .uptime(metric != null ? uptimeHours(metric) : 0)
                        .build())
                .config(buildConfig(info, metric))
                .updatedAt(metric != null
                        ? DateUtils.format(metric.getMetricTime())
                        : "暂无调用")
                .build();
    }

    /** 构建返回给前端的配置快照（默认参数 + 已持久化配置 + 实时指标）。 */
    private Map<String, Object> buildConfig(AiAgentInfo info, AiAgentMetric metric) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("model", info.getModel());
        config.put("displayName", info.getDisplayName());
        config.put("agentId", info.getAgentId());
        config.put("timeoutSeconds", 300);
        config.put("maxRetries", 3);
        config.put("callsPerHour", metric != null ? callsPerHour(metric) : 0);
        config.put("latencyMs", metric != null ? (int) Math.round(num(metric.getAvgLatencyMs())) : 0);
        config.put("tokens24h", metric != null && metric.getTokens24h() != null
                ? metric.getTokens24h() : 0L);
        config.put("totalCalls", metric != null && metric.getTotalCalls() != null
                ? metric.getTotalCalls() : 0);
        config.put("successCount", metric != null && metric.getSuccessCount() != null
                ? metric.getSuccessCount() : 0);
        config.put("failCount", metric != null && metric.getFailCount() != null
                ? metric.getFailCount() : 0);
        if (info.getConfig() instanceof Map<?, ?> saved) {
            config.put("saved", saved);
        }
        return config;
    }

    /** 查询指定智能体最近一条真实指标记录。 */
    private AiAgentMetric latestMetric(String agentId) {
        try {
            List<AiAgentMetric> rows = metricMapper.selectList(
                    new LambdaQueryWrapper<AiAgentMetric>()
                            .eq(AiAgentMetric::getAgentName, agentId)
                            .orderByDesc(AiAgentMetric::getMetricTime)
                            .last("LIMIT 1"));
            return rows.isEmpty() ? null : rows.get(0);
        } catch (Exception e) {
            log.warn("查询智能体指标失败（降级为空）: agentId={}, error={}", agentId, e.getMessage());
            return null;
        }
    }

    private long countTasks(String agentId, Integer status) {
        LambdaQueryWrapper<AiAgentTask> wrapper = new LambdaQueryWrapper<AiAgentTask>()
                .eq(AiAgentTask::getAgentName, agentId);
        if (status != null) {
            wrapper.eq(AiAgentTask::getStatus, status);
        }
        Long count = taskMapper.selectCount(wrapper);
        return count != null ? count : 0L;
    }

    private long countActiveTasks(String agentId) {
        Long count = taskMapper.selectCount(new LambdaQueryWrapper<AiAgentTask>()
                .eq(AiAgentTask::getAgentName, agentId)
                .in(AiAgentTask::getStatus,
                        AiEnums.TaskStatus.QUEUED.getCode(),
                        AiEnums.TaskStatus.RUNNING.getCode()));
        return count != null ? count : 0L;
    }

    private AiAgentInfo requireAgent(String agentId) {
        AiAgentInfo info = agentInfoMapper.selectOne(
                new LambdaQueryWrapper<AiAgentInfo>()
                        .eq(AiAgentInfo::getAgentId, agentId));
        if (info == null) {
            throw new ResourceNotFoundException("Agent not found: agentId=" + agentId);
        }
        return info;
    }

    private void updateStatus(AiAgentInfo info, int status) {
        info.setStatus(status);
        info.setUpdatedAt(DateUtils.now());
        agentInfoMapper.updateById(info);
    }

    /** 记录一条真实事件日志（SSE 事件流 / 日志弹窗共用数据源）。 */
    private void recordEvent(AiAgentInfo info, int eventType, String title, String message) {
        try {
            AiEventLog event = new AiEventLog();
            event.setAgentName(info.getAgentId());
            event.setEventType(eventType);
            event.setEventSource(0);
            event.setTitle(title);
            event.setMessage(message);
            event.setData(Map.of(
                    "agentId", info.getAgentId(),
                    "status", info.getStatus()));
            event.setTraceId(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            event.setCreatedAt(DateUtils.now());
            eventLogMapper.insert(event);
        } catch (Exception e) {
            log.warn("Agent 运维事件记录失败（可忽略）: agentId={}, error={}",
                    info.getAgentId(), e.getMessage());
        }
    }

    private AgentEventVO toEventVO(AiEventLog log) {
        Integer eventType = log.getEventType() != null ? log.getEventType()
                : AiEnums.EventType.INFO.getCode();
        @SuppressWarnings("unchecked")
        Map<String, Object> data = log.getData() instanceof Map<?, ?> raw
                ? (Map<String, Object>) raw
                : Map.of("taskId", log.getTaskId() != null ? log.getTaskId() : 0L);

        return AgentEventVO.builder()
                .eventId(String.valueOf(log.getId()))
                .agentId(log.getAgentName())
                .agentName(displayNameOf(log.getAgentName()))
                .eventType(eventType)
                .message(log.getMessage() != null ? log.getMessage()
                        : (log.getTitle() != null ? log.getTitle() : ""))
                .status(mapEventToTaskStatus(eventType))
                .timestamp(log.getCreatedAt() != null
                        ? log.getCreatedAt().atZone(DateUtils.ZONE_SHANGHAI).toInstant()
                        : DateUtils.currentInstant())
                .type(eventType)
                .data(data)
                .build();
    }

    /** 根据 Agent ID 查询展示名称（用于事件/任务列表）。 */
    private String displayNameOf(String agentId) {
        if (agentId == null) {
            return "未知";
        }
        String cached = displayNameCache.get(agentId);
        if (cached != null) {
            return cached;
        }
        AiAgentInfo info = agentInfoMapper.selectOne(
                new LambdaQueryWrapper<AiAgentInfo>()
                        .eq(AiAgentInfo::getAgentId, agentId));
        String name = info != null ? info.getDisplayName() : agentId;
        displayNameCache.put(agentId, name);
        return name;
    }

    private Integer mapEventToTaskStatus(Integer eventType) {
        if (eventType == null) return AiEnums.TaskStatus.RUNNING.getCode();
        return switch (eventType) {
            case 3 -> AiEnums.TaskStatus.COMPLETED.getCode();  // SUCCESS
            case 2 -> AiEnums.TaskStatus.FAILED.getCode();     // ERROR
            default -> AiEnums.TaskStatus.RUNNING.getCode();   // INFO/WARNING
        };
    }

    /** 展示名 → kebab-case 唯一标识。 */
    private String toAgentId(String displayName) {
        return displayName.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    /** 展示名 → 类名风格。 */
    private String toClassName(String displayName) {
        String[] parts = displayName.trim().split("[^a-zA-Z0-9]+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        String name = sb.toString();
        return name.isEmpty() ? "CustomAgent" : name + "Agent";
    }

    private String truncate(String text, int maxLen) {
        if (text == null || text.isBlank()) return null;
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }

    private int callsPerHour(AiAgentMetric metric) {
        if (metric.getCallsPerHour() != null) {
            return (int) Math.round(num(metric.getCallsPerHour()));
        }
        int total = metric.getTotalCalls() != null ? metric.getTotalCalls() : 0;
        return (int) Math.round(total / 24.0);
    }

    private static String mapTaskType(Integer taskType) {
        AiEnums.TaskType type = AiEnums.TaskType.fromCode(taskType);
        return type != null ? type.getLabel() : "未知";
    }

    private static String mapPriority(Integer priority) {
        AiEnums.Priority p = AiEnums.Priority.fromCode(priority);
        if (p == null) return "MEDIUM";
        return switch (p) {
            case LOW -> "LOW";
            case MEDIUM -> "MEDIUM";
            case HIGH -> "HIGH";
        };
    }

    private static String mapTaskStatus(Integer status) {
        AiEnums.TaskStatus s = AiEnums.TaskStatus.fromCode(status);
        if (s == null) return "QUEUED";
        return switch (s) {
            case QUEUED -> "QUEUED";
            case RUNNING -> "RUNNING";
            case COMPLETED -> "COMPLETED";
            case FAILED -> "FAILED";
            case RETRYING -> "RETRYING";
        };
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static double num(java.math.BigDecimal value) {
        return value != null ? value.doubleValue() : 0.0;
    }

    private static int uptimeHours(AiAgentMetric metric) {
        if (metric.getMetricTime() == null) return 0;
        return (int) Math.max(0, Duration.between(metric.getMetricTime(), DateUtils.now()).toHours());
    }
}
