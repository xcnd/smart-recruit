package com.smartrecruit.recruitment.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 任务上报请求（招聘模块本地兜底逻辑中也将简历筛选任务上报给 AI 引擎）。
 *
 * @since 2026-04-12
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskReport {

    private String agentName;
    private Integer taskType;
    private Boolean success;
    private Long durationMs;
    private String input;
    private String output;
    private String error;
}
