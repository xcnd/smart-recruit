package com.smartrecruit.referral.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 提交新内推的请求DTO。
 *
 * @author xdh
 * @since 2026-04-26
 */
@Data
public class CreateReferralRequest {

    /** 目标计划ID。 */
    @NotNull(message = "Referral program ID is required")
    private Long programId;

    /** 计划-职位关联ID。 */
    @NotNull(message = "Program job ID is required")
    private Long programJobId;

    /** 推荐人的员工ID。 */
    @NotNull(message = "Referrer ID is required")
    private Long referrerId;

    /** 推荐人显示名称。 */
    private String referrerName;

    /** 推荐人部门。 */
    private String referrerDept;

    /** 候选人ID。 */
    @NotNull(message = "Candidate ID is required")
    private Long candidateId;

    /** 候选人姓名。 */
    private String candidateName;

    /** 候选人电话。 */
    private String candidatePhone;

    /** 候选人邮箱。 */
    private String candidateEmail;

    /** 简历文件URL。 */
    private String candidateResumeUrl;

    /** 目标职位ID。 */
    @NotNull(message = "Job ID is required")
    private Long jobId;

    /** 目标职位名称。 */
    private String jobTitle;

    /** 推荐人与候选人关系。 */
    private String relationship;

    /** 推荐备注。 */
    private String referralNote;

    /** 预期奖金金额。 */
    private BigDecimal bonus;
}
