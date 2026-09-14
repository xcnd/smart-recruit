package com.smartrecruit.referral.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 内推计划的VO。
 *
 * @author xdh
 * @since 2026-04-26
 */
@Data
public class ReferralProgramVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 内推计划 ID。 */
    private Long id;
    /** 计划标题。 */
    private String title;
    /** 计划描述。 */
    private String description;
    /** 内推基础奖金金额。 */
    private BigDecimal bonusAmount;
    /** 奖金结构（JSON）。 */
    private Object bonusStructure;
    /** 计划开始日期。 */
    private LocalDate startDate;
    /** 计划结束日期。 */
    private LocalDate endDate;
    /** 符合条件的部门ID（JSON）。 */
    private Object eligibleDeptIds;
    /** 状态：0=停用, 1=启用。 */
    private Integer status;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;
    /** 创建人。 */
    private String createBy;
    /** 更新人。 */
    private String updateBy;
}
