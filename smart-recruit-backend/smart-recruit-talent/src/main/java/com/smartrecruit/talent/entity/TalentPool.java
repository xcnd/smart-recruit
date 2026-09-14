package com.smartrecruit.talent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 人才库实体，映射 {@code rec_talent_pool} 表。
 *
 * <p>存储已表现出意向或被识别为潜在候选人的信息，按人才库类型和技能等级进行组织。</p>
 *
 * @since 1.0.0
 */
@Data
@TableName("rec_talent_pool")
public class TalentPool implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 候选人 ID。 */
    private Long candidateId;

    /** 人才库类型：GENERAL（通用）、TECHNICAL（技术）、MANAGEMENT（管理）、INTERN（实习）、EXECUTIVE（高管）。 */
    private Integer poolType;

    /** 标签（JSON 数组字符串）。 */
    private String tags;

    /** 技能等级：JUNIOR（初级）、MID（中级）、SENIOR（高级）、EXPERT（专家）。 */
    private Integer skillLevel;

    /** 可用状态：ACTIVE（活跃）、PASSIVE（被动）、NOT_AVAILABLE（不可用）。 */
    private Integer availability;

    /** 期望职位。 */
    private String expectedPosition;

    /** 期望工作地点。 */
    private String expectedLocation;

    /** 最低期望薪资。 */
    private Integer expectedSalaryMin;

    /** 最高期望薪资。 */
    private Integer expectedSalaryMax;

    /** 最近联系时间。 */
    private LocalDateTime lastContactTime;

    /** 匹配度评分（0-100）。 */
    private Integer matchScore;

    /** 匹配的职位 ID。 */
    private Long matchedPositionId;

    /** 人才状态：0=AVAILABLE（可联系）、1=CONTACTED（已联系）、2=ENGAGED（已沟通）。 */
    private Integer status;

    /** 最近活跃时间。 */
    private LocalDateTime lastActiveTime;

    /** AI 生成的标签（JSON 数组字符串）。 */
    private String aiTags;

    /** 内部备注。 */
    private String note;

    /** 创建时间。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建人 ID。 */
    private String createBy;

    /** 更新人 ID。 */
    private String updateBy;

    /** 逻辑删除标记：0 = 未删除，1 = 已删除。 */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted = 0;
}
