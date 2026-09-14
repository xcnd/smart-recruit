package com.smartrecruit.referral.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建内推计划的请求DTO。
 *
 * @author xdh
 * @since 2026-05-31
 */
@Data
public class CreateProgramRequest {

    /** 计划标题。 */
    @NotBlank(message = "计划标题不能为空")
    private String title;

    /** 计划描述。 */
    private String description;

    /** 内推基础奖金金额。 */
    @NotNull(message = "奖金金额不能为空")
    private BigDecimal bonusAmount;

    /** 奖金发放结构（JSON格式）。 */
    private Object bonusStructure;

    /** 计划开始日期。 */
    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    /** 计划结束日期（null表示持续进行）。 */
    private LocalDate endDate;

    /** 符合条件的部门ID（JSON数组）。 */
    private Object eligibleDeptIds;

    /** 状态：0=禁用，1=启用。 */
    private Integer status;
}
