package com.smartrecruit.recruitment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 简历筛选统计视图对象。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeStatsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 总简历数。 */
    private Long total;
    /** 待处理数。 */
    private Long pending;
    /** 已通过数。 */
    private Long passed;
    /** 已淘汰数。 */
    private Long rejected;
}
