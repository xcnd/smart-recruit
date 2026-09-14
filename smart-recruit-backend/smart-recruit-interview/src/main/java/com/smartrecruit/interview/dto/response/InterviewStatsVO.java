package com.smartrecruit.interview.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 面试统计数据视图对象。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewStatsVO {

    /** 总面试数。 */
    private long total;

    /** 今日面试数。 */
    private long today;

    /** 已通过数。 */
    private long passed;

    /** 已取消数。 */
    private long cancelled;
}
