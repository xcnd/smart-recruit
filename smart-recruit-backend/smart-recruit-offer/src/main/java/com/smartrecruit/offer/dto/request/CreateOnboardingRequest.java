package com.smartrecruit.offer.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 创建入职记录的请求 DTO。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOnboardingRequest {

    /** 关联的 Offer ID。 */
    private Long offerId;

    /** 候选人 ID。 */
    private Long candidateId;

    /** 入职日期（默认使用 Offer 的 expectedOnboardDate）。 */
    private LocalDate onboardDate;

    /** 指定导师用户 ID（可选）。 */
    private Long mentorId;

    /** 指定伙伴用户 ID（可选）。 */
    private Long buddyId;
}
