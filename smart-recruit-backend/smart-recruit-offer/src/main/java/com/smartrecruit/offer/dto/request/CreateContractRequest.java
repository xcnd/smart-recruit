package com.smartrecruit.offer.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 从 Offer 创建合同请求。
 *
 * @param offerId Offer ID
 * @since 2026-04-09
 */
public record CreateContractRequest(@NotNull(message = "Offer ID 不能为空") Long offerId) {
}
