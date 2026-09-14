package com.smartrecruit.recruitment.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDate;

/**
 * 候选人日汇总视图对象（定时任务预聚合数据）。
 *
 * @since 2026-04-07
 */
@JsonPropertyOrder({"statDate", "stage", "source", "candidateCount"})
public record CandidateDailyStatVO(
        @JsonProperty("statDate") LocalDate statDate,
        @JsonProperty("stage") Integer stage,
        @JsonProperty("source") Integer source,
        @JsonProperty("candidateCount") Integer candidateCount
) {
}
