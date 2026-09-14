package com.smartrecruit.referral.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 内推智能匹配结果视图对象。
 *
 * @since 2026-04-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferralMatchVO {

    private Long id;

    private Long recordId;

    private Long candidateId;

    private String candidateName;

    private Long matchedJobId;

    private String matchedJobTitle;

    private BigDecimal matchScore;

    private String recommendation;

    private String suggestedApproach;

    private Object matchDimensions;

    private String source;

    private Integer rankNo;

    private LocalDateTime createTime;
}
