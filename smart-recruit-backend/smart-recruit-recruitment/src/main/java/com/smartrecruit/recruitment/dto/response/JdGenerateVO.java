package com.smartrecruit.recruitment.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * AI 职位描述生成结果视图对象。
 *
 * @param title       职位标题
 * @param description 生成的职位描述文本
 * @since 2026-04-07
 */
@JsonPropertyOrder({"title", "description"})
public record JdGenerateVO(
        @JsonProperty("title") String title,
        @JsonProperty("description") String description
) {
}
