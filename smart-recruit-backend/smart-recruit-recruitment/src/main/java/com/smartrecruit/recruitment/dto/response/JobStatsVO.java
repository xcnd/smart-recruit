package com.smartrecruit.recruitment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 职位统计汇总 VO。
 *
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobStatsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long total;
    private Long published;
    private Long draft;
    private Long closed;
}
