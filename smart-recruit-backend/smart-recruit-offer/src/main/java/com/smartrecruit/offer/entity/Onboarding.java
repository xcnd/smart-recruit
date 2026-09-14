package com.smartrecruit.offer.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 入职计划实体，映射 {@code rec_onboarding} 表。
 *
 * <p>跟踪员工入职进度，从入职前准备、文档收集、设备配置到完成全过程。</p>
 *
 * @since 1.0.0
 */
@Data
@TableName(value = "rec_onboarding", autoResultMap = true)
public class Onboarding implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 关联的 Offer ID。 */
    private Long offerId;

    /** 候选人（现员工）ID。 */
    private Long candidateId;

    /** 部门 ID，关联 sys_department。 */
    private Long departmentId;

    /** 部门名称（从 Offer 反范式化）。 */
    private String departmentName;

    /** 员工编号，格式 EMP-YYYY-NNNN。 */
    private String employeeNo;

    /** 员工姓名（从候选人/Offer 反范式化）。 */
    private String employeeName;

    /** 职位名称（从 Offer 反范式化）。 */
    private String positionTitle;

    /** 职级（从 Offer 反范式化，如 P7、M1）。 */
    private String level;

    /** 预计入职日期（从 Offer 复制）。 */
    private LocalDate expectedOnboardDate;

    /** 实际入职日期。 */
    private LocalDate actualOnboardDate;

    /** 当前入职步骤：1=资料收集,2=设备发放,3=账号开通,4=欢迎页,5=导师分配,6=入职培训。 */
    private Integer currentStep;

    /** 状态：0=PENDING,1=ACTIVE,2=DONE,3=AT_RISK。 */
    private Integer status;

    /** 员工状态：0=待入职,1=试用期,2=正式,3=已离职。 */
    private Integer employeeStatus;

    /** 留存风险评分 0-100。 */
    private BigDecimal riskScore;

    /** 风险等级：0=LOW,1=MEDIUM,2=HIGH。 */
    private Integer riskLevel;

    /** 6 个月留任概率（定时任务预测后落库）。 */
    private Integer retentionScore6M;

    /** 12 个月留任概率（定时任务预测后落库）。 */
    private Integer retentionScore12M;

    /** 风险因素（";;" 分隔）。 */
    private String retentionRiskFactors;

    /** 干预建议（";;" 分隔）。 */
    private String retentionInterventions;

    /** 留任预测时间。 */
    private LocalDateTime retentionPredictedAt;

    /** 入职 5 步数据 SHA-256 指纹（hash 未变化则不重复调用 AI）。 */
    private String retentionInputHash;

    /** 6 项资料提交状态，JSON 格式。 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object documentStatus;

    /** 5 项设备发放状态，JSON 格式。 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object equipmentStatus;

    /** 指定导师用户 ID。 */
    private Long mentorId;

    /** 导师姓名。 */
    private String mentorName;

    /** 指定伙伴用户 ID。 */
    private Long buddyId;

    /** 欢迎页是否已发送：0=未发送,1=已发送。 */
    private Integer welcomeSent;

    /** 培训进度 0-100。 */
    private BigDecimal trainingProgress;

    /** 系统账号用户名（步骤3-账号开通）。 */
    private String accountUsername;

    /** 系统账号邮箱（步骤3-账号开通）。 */
    private String accountEmail;

    /** 系统账号创建时间（步骤3-账号开通）。 */
    private LocalDateTime accountCreatedAt;

    /** 创建时间。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建人用户 ID。 */
    private Long createUserId;

    /** 创建人姓名。 */
    private String createBy;

    /** 更新人用户 ID。 */
    private Long updateUserId;

    /** 更新人姓名。 */
    private String updateBy;

    /** 逻辑删除标记：0 = 未删除，1 = 已删除。 */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted = 0;
}
