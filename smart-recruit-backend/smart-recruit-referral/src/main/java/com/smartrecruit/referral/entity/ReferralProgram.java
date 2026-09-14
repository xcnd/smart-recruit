package com.smartrecruit.referral.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 内推计划实体，映射到 {@code ref_program} 表。
 *
 * <p>定义一个独立的内推计划，包含奖金结构、日期范围
 * 和部门资格。</p>
 *
 * @author xdh
 * @since 2026-05-04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "ref_program", autoResultMap = true)
public class ReferralProgram implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 计划标题 / 名称。 */
    private String title;

    /** 计划描述。 */
    private String description;

    /** 内推基础奖金金额。 */
    private BigDecimal bonusAmount;

    /** 奖金发放结构（JSON格式）。 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object bonusStructure;

    /** 计划开始日期。 */
    private LocalDate startDate;

    /** 计划结束日期（null表示持续进行）。 */
    private LocalDate endDate;

    /** 符合条件的部门ID（JSON数组）。 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object eligibleDeptIds;

    /** 状态：0=禁用，1=启用。 */
    private Integer status;

    /** 记录创建时间戳。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 最后更新时间戳。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建人用户ID。 */
    private Long createUserId;

    /** 创建人显示名称。 */
    private String createBy;

    /** 最后更新人用户ID。 */
    private Long updateUserId;

    /** 最后更新人显示名称。 */
    private String updateBy;

    /** 逻辑删除标记：0=正常，1=已删除。 */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted = 0;
}
