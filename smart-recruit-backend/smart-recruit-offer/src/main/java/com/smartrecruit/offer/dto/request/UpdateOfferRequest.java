package com.smartrecruit.offer.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新已有Offer的请求DTO（仅限草稿状态可编辑）。
 *
 * @since 1.0.0
 */
@Data
public class UpdateOfferRequest {

    /** 基本月薪（元）。 */
    @Min(value = 0, message = "月基本工资不能为负数")
    private BigDecimal baseSalary;

    /** 年终奖月数。 */
    @Min(value = 0, message = "奖金月数不能为负数")
    private Integer bonusMonths;

    /** 股票期权数量。 */
    private BigDecimal stockOptions;

    /** 签约奖金（元）。 */
    private BigDecimal signOnBonus;

    /** 职位名称。 */
    private String positionTitle;

    /** 部门名称。 */
    private String departmentName;

    /** 录用职级。 */
    private String level;
}
