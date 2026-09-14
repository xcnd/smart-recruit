package com.smartrecruit.referral.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 公开页面的匿名内推请求 DTO。
 * 与 {@link CreateReferralRequest} 不同，{@code referrerId} 和 {@code candidateId} 非必填，
 * 因为公开访问的访客没有系统账号。
 *
 * @author xdh
 * @since 2026-04-01
 */
@Data
public class PublicReferralRequest {

    /** 目标计划ID。 */
    @NotNull(message = "Referral program ID is required")
    private Long programId;

    /** 计划-职位关联ID。 */
    @NotNull(message = "Program job ID is required")
    private Long programJobId;

    /** 推荐人姓名（访客填写）。 */
    private String referrerName;

    /** 推荐人手机号。 */
    private String referrerPhone;

    /** 候选人姓名。 */
    @NotNull(message = "Candidate name is required")
    private String candidateName;

    /** 候选人电话。 */
    private String candidatePhone;

    /** 候选人邮箱。 */
    private String candidateEmail;

    /** 目标职位ID。 */
    @NotNull(message = "Job ID is required")
    private Long jobId;

    /** 推荐人与候选人关系。 */
    private String relationship;

    /** 推荐备注。 */
    private String referralNote;

    /** 简历文件URL（先上传简历获取）。 */
    private String resumeUrl;

    /** 分享token，用于将公开提交关联到具体推荐人。 */
    private String shareToken;

    /** 预期奖金金额。 */
    private BigDecimal bonus;
}
