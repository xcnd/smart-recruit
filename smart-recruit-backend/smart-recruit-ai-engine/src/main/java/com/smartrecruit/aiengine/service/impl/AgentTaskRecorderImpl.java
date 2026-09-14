package com.smartrecruit.aiengine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartrecruit.aiengine.entity.AiAgentMetric;
import com.smartrecruit.aiengine.entity.AiAgentTask;
import com.smartrecruit.aiengine.entity.AiEventLog;
import com.smartrecruit.aiengine.enums.AiEnums;
import com.smartrecruit.aiengine.repository.AiAgentMetricMapper;
import com.smartrecruit.aiengine.repository.AiAgentTaskMapper;
import com.smartrecruit.aiengine.repository.AiEventLogMapper;
import com.smartrecruit.aiengine.service.AgentTaskRecorder;
import com.smartrecruit.common.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Agent 任务与指标记录器实现。
 *
 * @since 2026-04-07
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AgentTaskRecorderImpl implements AgentTaskRecorder {

    /** 任务输入/输出摘要最大长度（JSON 列，超出后按合法 JSON 截断）。 */
    private static final int MAX_JSON_LENGTH = 4000;

    private final AiAgentTaskMapper taskMapper;
    private final AiAgentMetricMapper metricMapper;
    private final AiEventLogMapper eventLogMapper;
    private final ObjectMapper objectMapper;

    /**
     * 记录一次 Agent 能力调用。
     */
    @Override
    public void record(String agentName, int taskType, String input, String output,
                       boolean success, long durationMs, String error) {
        try {
            LocalDateTime now = DateUtils.now();
            AiAgentTask task = new AiAgentTask();
            task.setAgentName(agentName);
            task.setTaskType(taskType);
            task.setPriority(AiEnums.Priority.MEDIUM.getCode());
            task.setInputData(truncateToValidJson(input));
            task.setOutputData(truncateToValidJson(output));
            task.setStatus(success ? AiEnums.TaskStatus.COMPLETED.getCode()
                    : AiEnums.TaskStatus.FAILED.getCode());
            task.setStartedAt(now.minusNanos(durationMs * 1_000_000L));
            task.setCompletedAt(now);
            task.setDurationMs(durationMs);
            task.setErrorMessage(error);
            task.setCreatedAt(now);
            taskMapper.insert(task);

            recordEventLog(task, success, durationMs, error);
            updateMetric(agentName, success, durationMs, now);
        } catch (Exception e) {
            log.warn("Agent 任务记录失败（可忽略）: agent={}, error={}", agentName, e.getMessage());
        }
    }

    /**
     * 开始一个真实任务：写入「执行中」状态，实时出现在任务队列。
     */
    @Override
    public long startTask(String agentName, int taskType, String input) {
        try {
            LocalDateTime now = DateUtils.now();
            AiAgentTask task = new AiAgentTask();
            task.setAgentName(agentName);
            task.setTaskType(taskType);
            task.setPriority(AiEnums.Priority.MEDIUM.getCode());
            task.setInputData(truncateToValidJson(input));
            task.setStatus(AiEnums.TaskStatus.RUNNING.getCode());
            task.setStartedAt(now);
            task.setCreatedAt(now);
            taskMapper.insert(task);
            log.info("Agent 任务开始: agent={}, taskType={}, taskId={}",
                    agentName, taskType, task.getId());
            return task.getId();
        } catch (Exception e) {
            log.warn("Agent 任务开始记录失败（可忽略）: agent={}, error={}",
                    agentName, e.getMessage());
            return 0L;
        }
    }

    /**
     * 完成一个真实任务：更新状态与耗时，并滚动更新指标。
     */
    @Override
    public void completeTask(Long taskId, boolean success, String output,
                             long durationMs, String error) {
        if (taskId == null || taskId <= 0) {
            return;
        }
        try {
            AiAgentTask task = taskMapper.selectById(taskId);
            if (task == null) {
                log.warn("Agent 任务不存在，跳过完成记录: taskId={}", taskId);
                return;
            }
            LocalDateTime now = DateUtils.now();
            task.setOutputData(truncateToValidJson(output));
            task.setStatus(success ? AiEnums.TaskStatus.COMPLETED.getCode()
                    : AiEnums.TaskStatus.FAILED.getCode());
            task.setCompletedAt(now);
            task.setDurationMs(durationMs);
            task.setErrorMessage(error);
            taskMapper.updateById(task);

            recordEventLog(task, success, durationMs, error);
            updateMetric(task.getAgentName(), success, durationMs, now);
            log.info("Agent 任务完成: taskId={}, agent={}, success={}, durationMs={}",
                    taskId, task.getAgentName(), success, durationMs);
        } catch (Exception e) {
            log.warn("Agent 任务完成记录失败（可忽略）: taskId={}, error={}",
                    taskId, e.getMessage());
        }
    }

    /**
     * 落一条真实事件日志（SSE 事件流数据源）。
     */
    private void recordEventLog(AiAgentTask task, boolean success, long durationMs, String error) {
        try {
            AiEventLog log = new AiEventLog();
            log.setAgentName(task.getAgentName());
            log.setTaskId(task.getId());
            log.setEventType(success ? AiEnums.EventType.SUCCESS.getCode()
                    : AiEnums.EventType.ERROR.getCode());
            log.setEventSource(0); // 0=SSE
            log.setTitle(success ? "任务完成" : "任务失败");
            log.setMessage(success
                    ? String.format("智能体能力调用成功，耗时 %d ms", durationMs)
                    : (error == null ? "智能体能力调用失败" : error));
            log.setData(Map.of(
                    "taskId", task.getId(),
                    "durationMs", durationMs,
                    "status", task.getStatus()));
            log.setTraceId(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            log.setCreatedAt(DateUtils.now());
            eventLogMapper.insert(log);
        } catch (Exception e) {
            log.warn("Agent 事件日志记录失败（可忽略）: agent={}, error={}",
                    task.getAgentName(), e.getMessage());
        }
    }

    private void updateMetric(String agentName, boolean success, long durationMs, LocalDateTime now) {
        List<AiAgentMetric> rows = metricMapper.selectList(
                new LambdaQueryWrapper<AiAgentMetric>()
                        .eq(AiAgentMetric::getAgentName, agentName)
                        .orderByDesc(AiAgentMetric::getMetricTime)
                        .last("LIMIT 1"));

        AiAgentMetric metric = rows.isEmpty() ? null : rows.get(0);
        if (metric == null || metric.getMetricTime().toLocalDate().isBefore(now.toLocalDate())) {
            metric = new AiAgentMetric();
            metric.setAgentName(agentName);
            metric.setMetricTime(now);
            metric.setTotalCalls(0);
            metric.setSuccessCount(0);
            metric.setFailCount(0);
            metric.setAvgLatencyMs(BigDecimal.ZERO);
            metric.setAccuracyPct(BigDecimal.valueOf(100));
            metric.setHealthPct(BigDecimal.valueOf(100));
            metric.setTokens24h(0L);
            metric.setModelName("smartrecruit-llm-gateway");
            metric.setCreatedAt(now);
        }

        int total = (metric.getTotalCalls() == null ? 0 : metric.getTotalCalls()) + 1;
        int successCount = (metric.getSuccessCount() == null ? 0 : metric.getSuccessCount())
                + (success ? 1 : 0);
        int failCount = (metric.getFailCount() == null ? 0 : metric.getFailCount())
                + (success ? 0 : 1);
        int callsLastHour = countCallsLastHour(agentName, now);
        BigDecimal prevAvg = metric.getAvgLatencyMs() == null ? BigDecimal.ZERO : metric.getAvgLatencyMs();
        double newAvg = total == 1 ? durationMs
                : (prevAvg.doubleValue() * (total - 1) + durationMs) / total;
        double successRate = total > 0 ? successCount * 100.0 / total : 0.0;
        double health = Math.max(0, Math.min(100, successRate * 0.8 + (100 - newAvg / 100.0) * 0.2));

        metric.setTotalCalls(total);
        metric.setSuccessCount(successCount);
        metric.setFailCount(failCount);
        metric.setCallsPerHour(BigDecimal.valueOf(callsLastHour));
        metric.setAvgLatencyMs(BigDecimal.valueOf(Math.round(newAvg * 10.0) / 10.0));
        metric.setAccuracyPct(BigDecimal.valueOf(Math.round(successRate * 10.0) / 10.0));
        metric.setHealthPct(BigDecimal.valueOf(Math.round(health * 10.0) / 10.0));
        metric.setMetricTime(now);

        if (metric.getId() == null) {
            metricMapper.insert(metric);
        } else {
            metricMapper.updateById(metric);
        }
    }

    /**
     * 记录一次真实的 Token 消耗（按 Agent 累加到 24 小时指标）。
     *
     * <p>同一天内累加到当日指标行；跨天自动新开一行，保证
     * 「Token 消耗分布（近 24h）」为真实累计值。</p>
     */
    @Override
    public void recordTokenUsage(String agentName, String modelName, long tokens) {
        if (agentName == null || agentName.isBlank() || tokens <= 0) {
            return;
        }
        try {
            LocalDateTime now = DateUtils.now();
            List<AiAgentMetric> rows = metricMapper.selectList(
                    new LambdaQueryWrapper<AiAgentMetric>()
                            .eq(AiAgentMetric::getAgentName, agentName)
                            .orderByDesc(AiAgentMetric::getMetricTime)
                            .last("LIMIT 1"));
            AiAgentMetric metric = rows.isEmpty() ? null : rows.get(0);

            if (metric == null || metric.getMetricTime().toLocalDate().isBefore(now.toLocalDate())) {
                // 首次记录或已跨天：新开一行，tokens 从本次开始累计
                metric = new AiAgentMetric();
                metric.setAgentName(agentName);
                metric.setMetricTime(now);
                metric.setTotalCalls(0);
                metric.setSuccessCount(0);
                metric.setFailCount(0);
                metric.setAvgLatencyMs(BigDecimal.ZERO);
                metric.setAccuracyPct(BigDecimal.valueOf(100));
                metric.setHealthPct(BigDecimal.valueOf(100));
                metric.setTokens24h(tokens);
                metric.setModelName(modelName);
                metric.setCreatedAt(now);
                metricMapper.insert(metric);
            } else {
                long prev = metric.getTokens24h() == null ? 0L : metric.getTokens24h();
                metric.setTokens24h(prev + tokens);
                if (modelName != null && !modelName.isBlank()) {
                    metric.setModelName(modelName);
                }
                metricMapper.updateById(metric);
            }
            log.info("Agent Token 消耗已记录: agent={}, model={}, tokens={}",
                    agentName, modelName, tokens);
        } catch (Exception e) {
            log.warn("Agent Token 消耗记录失败（可忽略）: agent={}, error={}",
                    agentName, e.getMessage());
        }
    }

    /**
     * 统计最近 1 小时内的任务调用次数（用于 calls_per_hour 实时指标）。
     */
    private int countCallsLastHour(String agentName, LocalDateTime now) {
        try {
            Long count = taskMapper.selectCount(
                    new LambdaQueryWrapper<AiAgentTask>()
                            .eq(AiAgentTask::getAgentName, agentName)
                            .ge(AiAgentTask::getCreatedAt, now.minusHours(1)));
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 把输入/输出摘要截断为合法 JSON（MySQL JSON 列要求值必须是合法 JSON）。
     *
     * <p>先尝试完整保留（仅当本身就是合法 JSON 时）；
     * 过长时逐步缩短直到能解析出合法 JSON 前缀；
     * 普通文本统一降级为 JSON 字符串字面量，避免 Data truncation 报错。</p>
     */
    private String truncateToValidJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        // 本身就是合法 JSON：长度未超限时直接保留
        if (value.length() <= MAX_JSON_LENGTH) {
            try {
                objectMapper.readTree(value);
                return value;
            } catch (Exception ignored) {
                // 非 JSON 文本，走下方降级逻辑
            }
        }
        // 过长：从截断点向前逐步收缩，找到第一个可解析的 JSON 前缀
        for (int len = Math.min(MAX_JSON_LENGTH, value.length());
             len > Math.min(200, MAX_JSON_LENGTH / 2); len -= 50) {
            String candidate = value.substring(0, len);
            try {
                objectMapper.readTree(candidate);
                return candidate;
            } catch (Exception ignored) {
                // 继续收缩
            }
        }
        // 兜底：以 JSON 字符串形式保存截断摘要
        String truncated = value.length() > 200 ? value.substring(0, 200) + "..." : value;
        try {
            return objectMapper.writeValueAsString(truncated);
        } catch (Exception e) {
            return "\"truncated\"";
        }
    }
}
