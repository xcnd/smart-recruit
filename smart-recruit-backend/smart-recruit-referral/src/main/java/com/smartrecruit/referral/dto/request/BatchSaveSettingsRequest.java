package com.smartrecruit.referral.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 批量保存内推计划设置的请求DTO。
 *
 * @author xdh
 * @since 2026-04-26
 */
@Data
public class BatchSaveSettingsRequest {

    /** 计划ID（新增时为null）。 */
    private Long id;

    /** 与该计划关联的职位ID。 */
    @NotNull(message = "Job ID is required")
    private Long jobId;

    /** 显示用的职位名称。 */
    @NotBlank(message = "Job title is required")
    private String jobTitle;

    /** 该计划是否启用。 */
    private Integer enabled;

    /** 基础奖金金额。 */
    @NotNull(message = "Bonus amount is required")
    private BigDecimal bonusAmount;

    /** 第一档奖金金额。 */
    private BigDecimal bonusTier1;

    /** 第二档奖金金额。 */
    private BigDecimal bonusTier2;

    /** 第三档奖金金额。 */
    private BigDecimal bonusTier3;

    /** 计划描述。 */
    private String description;
}
