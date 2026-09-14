package com.smartrecruit.aiengine.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/**
 * AI 生成的职位描述视图对象。
 *
 * @param responsibilities 岗位职责列表
 * @param requirements     任职要求列表
 * @param plusPoints       加分项列表
 * @since 2026-04-07
 */
@JsonPropertyOrder({"responsibilities", "requirements", "plusPoints"})
public record JdVO(
        @JsonProperty("responsibilities") List<String> responsibilities,
        @JsonProperty("requirements") List<String> requirements,
        @JsonProperty("plusPoints") List<String> plusPoints
) {
}
