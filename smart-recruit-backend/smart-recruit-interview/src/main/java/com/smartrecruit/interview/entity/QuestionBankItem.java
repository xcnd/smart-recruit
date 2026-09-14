package com.smartrecruit.interview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 面试题库-题目实体，映射 {@code rec_question_bank_item} 表。
 *
 * @since 2026-04-10
 */
@Data
@TableName(value = "rec_question_bank_item", autoResultMap = true)
public class QuestionBankItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 套题 ID。 */
    private Long bankId;

    /** 题型：0=单选,1=多选,2=问答。 */
    private Integer questionType;

    /** 题目内容。 */
    private String question;

    /** 选项（JSON 数组文本，如 ["A. xxx","B. xxx"]），问答题为 null。 */
    private String options;

    /** 答案（单选/多选为选项标识；问答为参考要点）。 */
    private String answer;

    /** 题目解读/解析。 */
    private String explanation;

    /** 难度：1=简单,2=中等,3=困难。 */
    private Integer difficulty;

    /** 排序。 */
    private Integer sortOrder;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 创建人ID。 */
    private Long createUserId;

    /** 创建人用户名。 */
    private String createBy;

    /** 更新人ID。 */
    private Long updateUserId;

    /** 更新人用户名。 */
    private String updateBy;

    @TableLogic
    private Integer deleted = 0;
}
