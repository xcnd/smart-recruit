package com.smartrecruit.referral.dto.request;

import lombok.Data;

/**
 * 生成内推分享海报请求。
 *
 * @since 2026-04-07
 */
@Data
public class GeneratePosterRequest {

    /** 内推计划 ID。 */
    private Long programId;
}
