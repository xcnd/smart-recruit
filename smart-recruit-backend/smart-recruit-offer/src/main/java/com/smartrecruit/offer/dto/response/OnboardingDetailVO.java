package com.smartrecruit.offer.dto.response;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 包含检查清单步骤和文档状态的入职详情视图。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingDetailVO {

    /** 入职记录 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    /** 关联 Offer ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long offerId;
    /** 候选人 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long candidateId;
    /** 员工姓名。 */
    private String employeeName;
    /** 员工工号。 */
    private String employeeNo;
    /** 职位名称。 */
    private String jobTitle;
    /** 职级（如 P7、M1）。 */
    private String level;
    /** 部门名称。 */
    private String departmentName;
    /** 入职日期。 */
    private LocalDate onboardDate;
    /** 当前步骤。 */
    private Integer currentStep;
    /** 总步骤数。 */
    private Integer totalSteps;
    /** 入职状态。 */
    private Integer status;
    /** 员工状态：0=待入职,1=试用期,2=正式,3=已离职。 */
    private Integer employeeStatus;
    private String employeeStatusLabel;
    /** 留任风险级别：0=LOW,1=MEDIUM,2=HIGH。 */
    private Integer riskLevel;
    /** 留任预测评分（0-100）。 */
    private Integer retentionScore;

    // ---- 步骤相关 ----
    /** 各文档提交状态映射（兼容旧版）。 */
    private Map<String, String> documentStatuses;
    /** 结构化文档列表。 */
    private List<OnboardingDocumentVO> documents;
    /** 结构化设备列表。 */
    private List<OnboardingEquipmentVO> equipments;
    /** 设备发放聚合状态。 */
    private Integer equipmentStatus;

    // ---- 导师/伙伴 ----
    /** 导师 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long mentorId;
    /** 导师姓名。 */
    private String mentorName;
    /** 伙伴 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long buddyId;
    /** 伙伴姓名。 */
    private String buddyName;

    // ---- 欢迎页 ----
    /** 欢迎页是否已发送：0=未发送,1=已发送。 */
    private Integer welcomeSent;

    // ---- 培训 ----
    /** 培训进度 0-100。 */
    private BigDecimal trainingProgress;

    // ---- 账号 ----
    /** 系统账号是否已创建。 */
    private Boolean accountCreated;
    /** 系统账号用户名。 */
    private String accountUsername;
    /** 系统账号邮箱。 */
    private String accountEmail;

    // ---- 其他 ----
    /** 入职检查清单（JSON）。 */
    private Object checklist;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;
}
