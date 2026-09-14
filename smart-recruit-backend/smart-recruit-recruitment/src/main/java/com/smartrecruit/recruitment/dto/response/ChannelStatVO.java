package com.smartrecruit.recruitment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 招聘渠道统计视图对象。
 *
 * @since 2026-04-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelStatVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 渠道编码（对应 {@code rec_candidate.source}）。 */
    private Integer source;

    /** 渠道名称。 */
    private String sourceName;

    /** 候选人数。 */
    private Long candidateCount;

    /** 已入职人数。 */
    private Long hireCount;

    /** 渠道入职转化率（%）。 */
    private Double hireRate;
}
