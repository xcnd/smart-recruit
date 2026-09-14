package com.smartrecruit.interview.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/**
 * 候选人下一轮面试建议视图对象。
 *
 * @param nextRound      建议的下一轮面试轮次（1-7）
 * @param existingRounds 近一年内已完成的面试轮次列表
 * @since 2026-04-07
 */
@JsonPropertyOrder({"nextRound", "existingRounds"})
public record NextRoundVO(
        @JsonProperty("nextRound") Integer nextRound,
        @JsonProperty("existingRounds") List<Integer> existingRounds
) {
}
