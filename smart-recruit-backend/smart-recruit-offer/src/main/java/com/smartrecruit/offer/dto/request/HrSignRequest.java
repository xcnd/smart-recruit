package com.smartrecruit.offer.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * HR 签署合同请求。
 *
 * @param signerName HR 签署人姓名
 * @since 2026-04-09
 */
public record HrSignRequest(@NotBlank(message = "签署人姓名不能为空") String signerName) {
}
