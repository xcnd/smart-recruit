package com.smartrecruit.offer.dto.response;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 入职列表视图对象。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingVO {

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
    /** 当前入职步骤序号。 */
    private Integer currentStep;
    /** 入职步骤总数。 */
    private Integer totalSteps;
    /** 入职状态：0=PENDING,1=ACTIVE,2=DONE,3=AT_RISK。 */
    private Integer status;
    /** 员工状态：0=待入职,1=试用期,2=正式,3=已离职。 */
    private Integer employeeStatus;
    private String employeeStatusLabel;
    /** 风险等级：0=LOW,1=MEDIUM,2=HIGH。 */
    private Integer riskLevel;
    /** 导师 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long mentorId;
    /** 已完成文档数。 */
    private Integer completedDocumentsCount;
    /** 文档总数。 */
    private Integer totalDocumentsCount;
    /** 创建时间。 */
    private LocalDateTime createTime;
}
