package com.smartrecruit.aiengine.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * 注册新 AI 智能体请求。
 *
 * @param displayName 展示名称（如「技能抽取」），用于生成唯一 agentId 与类名
 * @param type        层级：0=编排层,1=执行层,2=复盘层
 * @param model       绑定模型（可选，默认 deepseek-v4）
 * @param description 职责描述（可选）
 * @param config      运行配置（可选，键值映射）
 * @since 2026-04-08
 */
public record RegisterAgentRequest(
        @NotBlank(message = "Agent 名称不能为空") String displayName,
        @NotNull(message = "Agent 层级不能为空") Integer type,
        String model,
        String description,
        Map<String, Object> config) {
}
