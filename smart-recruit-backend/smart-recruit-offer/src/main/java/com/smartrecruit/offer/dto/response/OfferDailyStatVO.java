package com.smartrecruit.offer.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDate;

/**
 * Offer/入职日汇总视图对象（定时任务预聚合数据）。
 *
 * @since 2026-04-07
 */
@JsonPropertyOrder({"statDate", "sentCount", "acceptedCount", "declinedCount",
        "pendingCount", "onboardCount", "confirmTotalDays", "confirmCount"})
public record OfferDailyStatVO(
        @JsonProperty("statDate") LocalDate statDate,
        @JsonProperty("sentCount") Integer sentCount,
        @JsonProperty("acceptedCount") Integer acceptedCount,
        @JsonProperty("declinedCount") Integer declinedCount,
        @JsonProperty("pendingCount") Integer pendingCount,
        @JsonProperty("onboardCount") Integer onboardCount,
        @JsonProperty("confirmTotalDays") Long confirmTotalDays,
        @JsonProperty("confirmCount") Integer confirmCount
) {
}
