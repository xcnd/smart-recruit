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
 * 内推排行榜实体，映射到 {@code ref_leaderboard} 表。
 *
 * <p>存储周期性的排行榜快照，用于对推荐人在内推计划中的表现进行排名。</p>
 *
 * @author xdh
 * @since 2026-05-04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("ref_leaderboard")
public class RefLeaderboard implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 关联的内推计划ID。 */
    private Long programId;

    /** 周期类型：MONTHLY（月度）、QUARTERLY（季度）、YEARLY（年度）、ALL_TIME（总累计）。 */
    private Integer periodType;

    /** 周期值，例如："2026-07"、"2026-Q3"、"2026"。 */
    private String periodValue;

    /** 推荐人用户ID。 */
    private Long referrerId;

    /** 推荐人显示名称。 */
    private String referrerName;

    /** 推荐人部门名称。 */
    private String referrerDeptName;

    /** 排行榜排名。 */
    private Integer rank;

    /** 内推总数。 */
    private Integer referralCount;

    /** 成功（已入职）内推数量。 */
    private Integer successCount;

    /** 已核定的总奖金金额。 */
    private BigDecimal bonusAmount;

    /** 记录创建时间戳。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
