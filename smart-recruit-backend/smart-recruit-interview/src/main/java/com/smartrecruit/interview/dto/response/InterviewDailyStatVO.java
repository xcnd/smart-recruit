package com.smartrecruit.interview.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDate;

/**
 * 面试日汇总视图对象（定时任务预聚合数据）。
 *
 * @since 2026-04-07
 */
@JsonPropertyOrder({"statDate", "totalCount", "passedCount", "cancelledCount"})
public record InterviewDailyStatVO(
        @JsonProperty("statDate") LocalDate statDate,
        @JsonProperty("totalCount") Integer totalCount,
        @JsonProperty("passedCount") Integer passedCount,
        @JsonProperty("cancelledCount") Integer cancelledCount
) {
}
