package com.smartrecruit.interview.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 面试题目实体，映射 {@code rec_interview_question} 表。
 *
 * <p>存储面试题库条目，包括 AI 生成和手动创建的题目，按职位、类型和难度分类。</p>
 *
 * @since 1.0.0
 */
@Data
@TableName("rec_interview_question")
public class InterviewQuestion implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 岗位类型（如 Java、Frontend、DevOps）。 */
    private Integer positionType;

    /** 题目类别：TECHNICAL、BEHAVIORAL、SITUATIONAL、CODING。 */
    private Integer category;

    /** 难度级别：EASY、MEDIUM、HARD。 */
    private Integer difficulty;

    /** 题目文本内容。 */
    private String questionText;

    /** 参考答案或预期关键点。 */
    private String referenceAnswer;

    /** 是否为 AI 生成（1 = 是，0 = 否）。 */
    private Integer isAiGenerated;

    /** 状态：0 = 停用，1 = 启用。 */
    private Integer status;

    /** 面试中使用次数。 */
    private Integer useCount;

    /** 创建时间。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
