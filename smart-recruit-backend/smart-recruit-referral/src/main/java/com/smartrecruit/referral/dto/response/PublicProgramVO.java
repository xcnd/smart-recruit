package com.smartrecruit.referral.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/**
 * 公开内推计划列表项视图对象。
 *
 * @param program    内推计划信息
 * @param jobs       计划下已启用的职位列表
 * @param shareToken 最新分享令牌（可能为空）
 * @since 2026-04-07
 */
@JsonPropertyOrder({"program", "jobs", "shareToken"})
public record PublicProgramVO(
        @JsonProperty("program") ReferralProgramVO program,
        @JsonProperty("jobs") List<RefProgramJobVO> jobs,
        @JsonProperty("shareToken") String shareToken
) {
}
