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
 * 计划-职位关联实体，映射到 {@code ref_program_job} 表。
 *
 * <p>将内推计划与特定职位关联起来，支持可选的
 * 按职位奖金覆盖以及启用/禁用标记。</p>
 *
 * @author xdh
 * @since 2026-05-04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("ref_program_job")
public class RefProgramJob implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 内推计划ID。 */
    private Long programId;

    /** 职位ID。 */
    private Long jobPositionId;

    /** 该职位是否启用内推：0=禁用，1=启用。 */
    private Integer isEnabled;

    /** 职位级别的奖金金额（如果设置，则覆盖计划默认值）。 */
    private BigDecimal bonusAmount;

    /** 职位标签 / 显示用标记。 */
    private Integer tag;

    /** 职位标题快照（用于公开落地页展示，避免跨服务调用）。 */
    private String jobTitle;

    /** 最低薪资快照。 */
    private Integer minSalary;

    /** 最高薪资快照。 */
    private Integer maxSalary;

    /** 招聘人数。 */
    private Integer headCount;

    /** 记录创建时间戳。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 最后更新时间戳。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
