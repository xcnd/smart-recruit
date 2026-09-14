package com.smartrecruit.offer.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建新Offer的请求DTO。
 *
 * @since 1.0.0
 */
@Data
public class CreateOfferRequest {

    /** 求职申请 ID（非必填，业务直接从面试-OFFER流程创建时可为空）。 */
    private Long applicationId;

    /** 候选人 ID。 */
    @NotNull(message = "候选人ID不能为空")
    private Long candidateId;

    /** 职位 ID。 */
    @NotNull(message = "职位ID不能为空")
    private Long jobPositionId;

    /** 候选人姓名。 */
    @NotBlank(message = "候选人姓名不能为空")
    private String candidateName;

    /** 候选人邮箱（用于后续发送 Offer 通知邮件）。 */
    private String candidateEmail;

    /** 职位名称。 */
    @NotBlank(message = "职位名称不能为空")
    private String jobTitle;

    /** 部门 ID。 */
    @NotNull(message = "部门ID不能为空")
    private Long departmentId;

    /** 部门名称。 */
    @NotBlank(message = "部门名称不能为空")
    private String departmentName;

    /** 录用职级（阿里体系：P4-P10, M1-M5）。 */
    @NotBlank(message = "职级不能为空")
    private String level;

    /** 基本月薪（元）。 */
    @NotNull(message = "月基本工资不能为空")
    @Min(value = 0, message = "月基本工资不能为负数")
    private BigDecimal baseSalary;

    /** 年终奖月数。 */
    @Min(value = 0, message = "奖金月数不能为负数")
    private Integer bonusMonths;

    /** 股票期权数量。 */
    private BigDecimal stockOptions;

    /** 签约奖金（元）。 */
    private BigDecimal signOnBonus;

    /** 期望入职日期。 */
    @NotNull(message = "预计入职日期不能为空")
    private LocalDate expectedOnboardDate;

    /** Offer 有效期截止日。 */
    @NotNull(message = "Offer有效期不能为空")
    private LocalDate validUntil;
}
