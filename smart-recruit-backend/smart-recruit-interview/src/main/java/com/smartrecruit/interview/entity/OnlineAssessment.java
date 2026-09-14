package com.smartrecruit.interview.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 在线测评实体，映射 {@code rec_online_assessment} 表。
 *
 * <p>管理发送给候选人的在线测评记录，包括编程测试、性格测试和智商测试。</p>
 *
 * @since 1.0.0
 */
@Data
@TableName("rec_online_assessment")
public class OnlineAssessment implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 关联面试 ID（可选）。 */
    private Long interviewId;

    /** 候选人 ID。 */
    private Long candidateId;

    /** 候选人姓名。 */
    private String candidateName;

    /** 应聘职位。 */
    private String jobTitle;

    /** 测评类型：0=编程测试,1=性格测试,2=智商测试。 */
    private Integer type;

    /** 类型标签。 */
    private String typeLabel;

    /** 发送时间。 */
    private LocalDateTime sentTime;

    /** 状态：0=未发送,1=待完成,2=已完成。 */
    private Integer status;

    /** 成绩（如 "85/100" 或 "A"）。 */
    private String score;

    /** 测评访问令牌（JWT，有效期7天）。 */
    private String accessToken;

    /** 候选人邮箱。 */
    private String candidateEmail;

    /** AI生成的测评题目 JSON（AssessmentQuestionItem 数组序列化，含 correctAnswer 供评分）。 */
    private String questionsJson;

    /** 创建时间。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建人ID。 */
    private Long createUserId;

    /** 创建人用户名。 */
    private String createBy;

    /** 更新人ID。 */
    private Long updateUserId;

    /** 更新人用户名。 */
    private String updateBy;

    /** 逻辑删除标记。 */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted = 0;
}
