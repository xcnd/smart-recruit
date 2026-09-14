package com.smartrecruit.interview.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 智能面试页面的统计卡片数据 VO。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiStatsVO {

    /** 今日面试数。 */
    private long todayInterviews;

    /** 已完成面试数。 */
    private long completed;

    /** AI 评估完成数。 */
    private long aiAssessed;

    /** 通过率（百分比数值，如 62.5）。 */
    private double passRate;
}
