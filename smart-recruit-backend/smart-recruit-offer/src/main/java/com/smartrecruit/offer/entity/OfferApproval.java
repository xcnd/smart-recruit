package com.smartrecruit.offer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Offer 审批记录实体，映射 {@code rec_offer_approval} 表。
 *
 * <p>记录多级审批链中的每一步审批操作。</p>
 *
 * @since 1.0.0
 */
@Data
@TableName("rec_offer_approval")
public class OfferApproval implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 关联的 Offer ID。 */
    private Long offerId;

    /** 审批人用户 ID。 */
    private Long approverId;

    /** 审批人姓名。 */
    private String approverName;

    /** 审批人角色，如"HR经理"、"部门总监"。 */
    private String approverRole;

    /** 审批级别（1、2、3...）。 */
    private Integer approvalLevel;

    /** 状态：PENDING、APPROVED、REJECTED。 */
    private Integer status;

    /** 审批意见。 */
    private String comment;

    /** 审批时间。 */
    private LocalDateTime approveTime;

    /** 创建时间。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
