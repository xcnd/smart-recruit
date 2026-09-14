package com.smartrecruit.recruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 沟通记录实体，映射 {@code rec_communication_log} 表。
 *
 * <p>记录所有与候选人的外发沟通（邮件、短信、
 * 电话等），包括内容和发送操作人。</p>
 *
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "rec_communication_log", autoResultMap = true)
public class CommunicationLog implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 关联 {@code rec_candidate.id}。 */
    private Long candidateId;

    /** 沟通类型：EMAIL、SMS、PHONE、WECHAT、IN_APP。 */
    private Integer type;

    /** 沟通主题或标题。 */
    private String title;

    /** 沟通完整内容/正文。 */
    private String content;

    /** 发送沟通的用户 ID。 */
    private Long operatorId;

    /** 发送沟通的用户名称。 */
    private String operatorName;

    /** 记录创建时间。 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
