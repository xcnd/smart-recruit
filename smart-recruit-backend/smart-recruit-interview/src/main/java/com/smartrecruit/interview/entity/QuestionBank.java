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
 * 面试题库-套题实体，映射 {@code rec_question_bank} 表。
 *
 * @since 2026-04-10
 */
@Data
@TableName("rec_question_bank")
public class QuestionBank implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 套题名称。 */
    private String bankName;

    /** 部门 ID。 */
    private Long departmentId;

    /** 部门名称。 */
    private String departmentName;

    /** 职位 ID。 */
    private Long jobPositionId;

    /** 职位名称。 */
    private String jobTitle;

    /** 套题类型：0=技术面,1=项目面,2=行为/HR面,3=综合面。 */
    private Integer questionType;

    /** 难度：1=简单,2=中等,3=困难。 */
    private Integer difficulty;

    /** 套题说明。 */
    private String description;

    /** 题目数量。 */
    private Integer questionCount;

    /** 状态：0=草稿,1=启用,2=停用。 */
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createBy;

    /** 创建人ID。 */
    private Long createUserId;

    private String updateBy;

    /** 更新人ID。 */
    private Long updateUserId;

    @TableLogic
    private Integer deleted = 0;
}
