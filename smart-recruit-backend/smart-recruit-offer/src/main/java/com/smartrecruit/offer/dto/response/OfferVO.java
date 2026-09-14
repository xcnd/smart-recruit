package com.smartrecruit.offer.dto.response;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Offer列表视图对象。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferVO {

    /** Offer ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    /** 求职申请 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long applicationId;
    /** 候选人 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long candidateId;
    /** 候选人姓名。 */
    private String candidateName;
    /** Offer 编号，唯一标识。 */
    private String offerNo;
    /** 职位 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long jobPositionId;
    /** 职位名称。 */
    private String positionTitle;
    /** 部门名称。 */
    private String departmentName;
    /** 录用职级。 */
    private String level;
    /** 基本月薪（元）。 */
    private BigDecimal baseSalary;
    /** 年薪总包（元）。 */
    private BigDecimal totalPackage;
    /** Offer 状态：DRAFT、PENDING_APPROVAL、APPROVED、SENT、ACCEPTED、DECLINED、EXPIRED。 */
    private Integer status;
    /** 期望入职日期。 */
    private LocalDate expectedOnboardDate;
    /** Offer 有效期截止日。 */
    private LocalDate validUntil;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 候选人回复时间。 */
    private LocalDateTime respondTime;
    /** 拒绝原因。 */
    private String declineReason;
    /** 更新时间。 */
    private LocalDateTime updateTime;
    /** 创建人姓名（由 createBy 用户ID解析而来）。 */
    private String creatorName;
}
