package com.smartrecruit.offer.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Offer 审批统计视图对象。
 *
 * @param pendingCount   待审批数量
 * @param todayApproved  今日审批通过数量
 * @param todayRejected  今日审批驳回数量
 * @param monthlyTotal   本月审批总数
 * @since 2026-04-07
 */
@JsonPropertyOrder({"pendingCount", "todayApproved", "todayRejected", "monthlyTotal"})
public record OfferApprovalStatsVO(
        @JsonProperty("pendingCount") long pendingCount,
        @JsonProperty("todayApproved") long todayApproved,
        @JsonProperty("todayRejected") long todayRejected,
        @JsonProperty("monthlyTotal") long monthlyTotal
) {
}
