package com.smartrecruit.referral.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/**
 * 公开落地页计划详情视图对象。
 *
 * @param program      内推计划信息
 * @param jobs         计划下已启用的职位列表
 * @param referrerName 分享人姓名
 * @param source       分享来源渠道
 * @param referralCode 人类友好的内推码
 * @since 2026-04-07
 */
@JsonPropertyOrder({"program", "jobs", "referrerName", "source", "referralCode"})
public record PublicProgramDetailVO(
        @JsonProperty("program") ReferralProgramVO program,
        @JsonProperty("jobs") List<RefProgramJobVO> jobs,
        @JsonProperty("referrerName") String referrerName,
        @JsonProperty("source") String source,
        @JsonProperty("referralCode") String referralCode
) {
}
