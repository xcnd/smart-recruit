package com.smartrecruit.referral.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 更新内推计划的请求DTO。所有字段均为可选，只更新非null字段。
 *
 * @author xdh
 * @since 2026-05-31
 */
@Data
public class UpdateProgramRequest {

    /** 计划标题。 */
    private String title;

    /** 计划描述。 */
    private String description;

    /** 内推基础奖金金额。 */
    private BigDecimal bonusAmount;

    /** 奖金发放结构（JSON格式）。 */
    private Object bonusStructure;

    /** 计划开始日期。 */
    private LocalDate startDate;

    /** 计划结束日期。 */
    private LocalDate endDate;

    /** 符合条件的部门ID（JSON数组）。 */
    private Object eligibleDeptIds;

    /** 状态：0=禁用，1=启用。 */
    private Integer status;
}
