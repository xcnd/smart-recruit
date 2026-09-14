package com.smartrecruit.referral.dto.request;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新内推奖金金额请求。
 *
 * @since 2026-04-07
 */
@Data
public class UpdateBonusRequest {

    /** 奖金金额。 */
    private BigDecimal bonusAmount;
}
