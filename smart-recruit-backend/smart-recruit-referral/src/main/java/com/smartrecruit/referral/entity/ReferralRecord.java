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
 * 内推记录实体，映射到 {@code ref_record} 表。
 *
 * <p>每条记录代表员工（推荐人）对候选人的一次内推，
 * 关联到某个计划和职位。</p>
 *
 * @author xdh
 * @since 2026-05-04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("ref_record")
public class ReferralRecord implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 关联的内推计划ID。 */
    private Long programId;

    /** 计划-职位关联ID。 */
    private Long programJobId;

    /** 推荐人（员工）用户ID。 */
    private Long referrerId;

    /** 候选人用户ID。 */
    private Long candidateId;

    /** 目标职位ID。 */
    private Long jobPositionId;

    /** 推荐人与候选人之间的关系。 */
    private String relationship;

    /** 内推备注 / 推荐理由。 */
    private String referralNote;

    /** 简历文件URL（RustFS 存储路径）。 */
    private String resumeUrl;

    /** 关联的分享token（通过公开链接提交时使用）。 */
    private String shareToken;

    /** 关联的内推码。 */
    private String referralCode;

    /**
     * 内推进度状态。
     * 取值：PENDING（待处理）、SCREENING（筛选中）、INTERVIEWING（面试中）、OFFERED（已发Offer）、HIRED（已入职）、REJECTED（已淘汰）。
     */
    private Integer status;

    /** 奖金发放状态：PENDING（待发放）、PARTIAL（部分发放）、FULL（全额发放）、PAID（已发放）、CANCELLED（已取消）。 */
    private Integer bonusStatus;

    /** 已核定的总奖金金额。 */
    private BigDecimal bonusAmount;

    /** 已发放的奖金金额。 */
    private BigDecimal bonusPaid;

    /** 候选人入职时间。 */
    private LocalDateTime hiredTime;

    /** 记录创建时间戳。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 最后更新时间戳。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
