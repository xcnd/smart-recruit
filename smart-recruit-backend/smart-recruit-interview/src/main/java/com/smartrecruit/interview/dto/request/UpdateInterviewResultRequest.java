package com.smartrecruit.interview.dto.request;

import lombok.Data;

/**
 * 更新面试结果请求。
 *
 * @since 2026-04-07
 */
@Data
public class UpdateInterviewResultRequest {

    /** 面试结果编码：0=通过, 1=待定, 2=不通过。 */
    private Integer result;

    /** 面试评分（0-100）。 */
    private Integer score;

    /** 面试官评语。 */
    private String comment;
}
