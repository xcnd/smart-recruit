package com.smartrecruit.referral.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/**
 * 内推政策视图对象，供内推政策页展示。
 *
 * @param bonusTiers 分阶段奖金阶梯
 * @param rules      内推参与规则
 * @param process    内推流程步骤
 * @param faqs       常见问题
 * @param contact    联系方式
 * @since 2026-04-07
 */
@JsonPropertyOrder({"bonusTiers", "rules", "process", "faqs", "contact"})
public record ReferralPolicyVO(
        @JsonProperty("bonusTiers") List<BonusTierVO> bonusTiers,
        @JsonProperty("rules") List<String> rules,
        @JsonProperty("process") List<PolicyStepVO> process,
        @JsonProperty("faqs") List<PolicyFaqVO> faqs,
        @JsonProperty("contact") ContactVO contact
) {

    /**
     * 奖金发放阶段信息。
     */
    @JsonPropertyOrder({"tier", "description", "payoutRatio", "trigger"})
    public record BonusTierVO(
            @JsonProperty("tier") String tier,
            @JsonProperty("description") String description,
            @JsonProperty("payoutRatio") String payoutRatio,
            @JsonProperty("trigger") String trigger
    ) {
    }

    /**
     * 内推流程步骤信息。
     */
    @JsonPropertyOrder({"step", "title", "desc"})
    public record PolicyStepVO(
            @JsonProperty("step") String step,
            @JsonProperty("title") String title,
            @JsonProperty("desc") String desc
    ) {
    }

    /**
     * 常见问题条目。
     */
    @JsonPropertyOrder({"question", "answer"})
    public record PolicyFaqVO(
            @JsonProperty("question") String question,
            @JsonProperty("answer") String answer
    ) {
    }

    /**
     * 内推政策联系方式。
     */
    @JsonPropertyOrder({"email", "phone", "wechat"})
    public record ContactVO(
            @JsonProperty("email") String email,
            @JsonProperty("phone") String phone,
            @JsonProperty("wechat") String wechat
    ) {
    }
}
