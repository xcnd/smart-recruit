package com.smartrecruit.offer.dto.request;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新培训进度请求。
 *
 * @since 1.0.0
 */
@Data
public class UpdateTrainingRequest {

    /** 培训总体进度 0-100。 */
    private BigDecimal trainingProgress;
}
