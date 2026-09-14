package com.smartrecruit.referral.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 奖金记录实体，映射到 {@code ref_bonus_record} 表。
 *
 * <p>记录与内推记录相关的每一次奖金发放或阶段付款。
 * 一条内推记录可能对应多条奖金记录（例如分阶段发放）。</p>
 *
 * @author xdh
 * @since 2026-05-04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("ref_bonus_record")
public class RefBonusRecord implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 关联的内推记录ID。 */
    private Long refRecordId;

    /** 推荐人（收款人）用户ID。 */
    private Long referrerId;

    /** 本次记录奖金金额。 */
    private BigDecimal amount;

    /** 发放阶段键值：ONBOARD（入职）、PROBATION_PASS（转正通过）、CONTRACT_FULL（合同期满）。 */
    private Integer stage;

    /** 发放阶段显示名称。 */
    private String stageName;

    /** 状态：PENDING（待处理）、APPROVED（已审批）、PAID（已发放）、CANCELLED（已取消）。 */
    private Integer status;

    /** 奖金实际发放时间。 */
    private LocalDateTime paidTime;

    /** 内部备注。 */
    private String remark;

    /** 记录创建时间戳。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 最后更新时间戳。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
