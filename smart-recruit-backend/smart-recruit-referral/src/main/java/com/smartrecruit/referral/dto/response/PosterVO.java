package com.smartrecruit.referral.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * 内推分享海报视图对象。
 *
 * @param programId   内推计划 ID
 * @param jobTitle    计划职位标题
 * @param bonusAmount 内推奖金金额
 * @param posterUrl   海报图片 URL
 * @param shareLink   分享链接
 * @param qrCodeUrl   二维码图片 URL
 * @param generatedAt 海报生成时间
 * @since 2026-04-07
 */
@JsonPropertyOrder({"programId", "jobTitle", "bonusAmount", "posterUrl",
        "shareLink", "qrCodeUrl", "generatedAt"})
public record PosterVO(
        @JsonProperty("programId") String programId,
        @JsonProperty("jobTitle") String jobTitle,
        @JsonProperty("bonusAmount") String bonusAmount,
        @JsonProperty("posterUrl") String posterUrl,
        @JsonProperty("shareLink") String shareLink,
        @JsonProperty("qrCodeUrl") String qrCodeUrl,
        @JsonProperty("generatedAt") String generatedAt
) {
}
