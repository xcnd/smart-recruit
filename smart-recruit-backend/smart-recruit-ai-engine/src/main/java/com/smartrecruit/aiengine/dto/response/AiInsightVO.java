package com.smartrecruit.aiengine.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * AI 数据分析洞察 VO。
 *
 * @param title       洞察标题
 * @param description 洞察描述与业务建议
 * @param trend       趋势方向：UP/DOWN/STABLE
 * @param value       关键数值
 * @since 2026-04-09
 */
public record AiInsightVO(
        @JsonProperty("title") String title,
        @JsonProperty("description") String description,
        @JsonProperty("trend") String trend,
        @JsonProperty("value") String value) {
}
