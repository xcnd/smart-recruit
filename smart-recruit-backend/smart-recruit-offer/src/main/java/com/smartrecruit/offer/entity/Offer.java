package com.smartrecruit.offer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Offer 实体，映射到 {@code rec_offer} 表。
 *
 * <p>表示一份正式的录用通知书，包含薪酬结构、审批流程和生命周期追踪。</p>
 *
 * @since 1.0.0
 */
@Data
@TableName(value = "rec_offer", autoResultMap = true)
public class Offer implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 申请 ID。 */
    private Long applicationId;

    /** 候选人 ID。 */
    private Long candidateId;

    /** 职位 ID。 */
    private Long jobPositionId;

    /** 候选人姓名。 */
    private String candidateName;

    /** 候选人邮箱（用于发送 Offer 邮件）。 */
    private String candidateEmail;

    /** 可读的 Offer 编号（例如 OFF-20260626-0001）。 */
    private String offerNo;

    /** 录用职位名称。 */
    private String positionTitle;

    /** 部门名称。 */
    private String departmentName;

    /** 录用职级（阿里体系：P4-P10, M1-M5）。 */
    private String level;

    /** 薪酬结构（JSON：base, bonus, allowance, stock）。 */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.Fastjson2TypeHandler.class)
    private Object salaryStructure;

    /** 试用期月数。 */
    private Integer probationMonths;

    /** 试用期薪资比例（例如 0.80）。 */
    private BigDecimal probationSalaryRatio;

    /** 预计入职日期。 */
    private LocalDate expectedOnboardDate;

    /** Offer 有效期截止日期。 */
    private LocalDate validUntil;

    /** 状态：DRAFT, PENDING_APPROVAL, APPROVED, SENT, ACCEPTED, DECLINED, EXPIRED。 */
    private Integer status;

    /** 发送时间。 */
    private LocalDateTime sendTime;

    /** 候选人回复时间。 */
    private LocalDateTime respondTime;

    /** 候选人确认 Token（用于 Offer 录用邮件中的一键接受/拒绝链接）。 */
    private String confirmToken;

    /** 拒绝原因。 */
    private String declineReason;

    /** AI 接受度预测（0-100，定时任务预预测后落库）。 */
    private BigDecimal aiAcceptProbability;

    /** AI 预测风险级别：LOW/MEDIUM/HIGH。 */
    private String aiRiskLevel;

    /** AI 预测建议。 */
    private String aiSuggestion;

    /** AI 预测时间（用于判断是否过期需重新预测）。 */
    private LocalDateTime aiPredictedAt;

    /** 已签署的 Offer 附件路径。 */
    private String attachmentPath;

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
