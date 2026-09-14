package com.smartrecruit.recruitment.dto.request;

import lombok.Data;

/**
 * 创建职位申请请求。
 *
 * @since 2026-04-07
 */
@Data
public class CreateApplicationRequest {

    /** 候选人 ID。 */
    private Long candidateId;

    /** 职位 ID。 */
    private Long jobId;
}
