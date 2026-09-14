package com.smartrecruit.interview.dto.request;

import lombok.Data;

/**
 * 更新在线测评成绩请求。
 *
 * @since 2026-04-07
 */
@Data
public class UpdateAssessmentScoreRequest {

    /** 测评成绩（如 "85/100" 或 "A"）。 */
    private String score;
}
