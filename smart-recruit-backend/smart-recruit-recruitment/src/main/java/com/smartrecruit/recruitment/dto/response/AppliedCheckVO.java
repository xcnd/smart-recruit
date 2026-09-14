package com.smartrecruit.recruitment.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * 职位投递状态检查视图对象。
 *
 * @param applied 当前候选人是否已投递该职位
 * @since 2026-04-07
 */
@JsonPropertyOrder({"applied"})
public record AppliedCheckVO(
        @JsonProperty("applied") boolean applied
) {
}
