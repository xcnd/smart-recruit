package com.smartrecruit.offer.dto.request;

import lombok.Data;

/**
 * 候选人确认 Offer 请求。
 *
 * @since 1.0.0
 */
@Data
public class OfferConfirmRequest {

    /** 操作：accept / reject。 */
    private String action;

    /** 拒绝原因（仅 reject 时需要）。 */
    private String declineReason;
}
