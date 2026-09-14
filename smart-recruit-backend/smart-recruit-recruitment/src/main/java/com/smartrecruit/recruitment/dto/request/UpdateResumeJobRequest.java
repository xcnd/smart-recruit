package com.smartrecruit.recruitment.dto.request;

import lombok.Data;

/**
 * 更新简历关联职位请求。
 *
 * @since 2026-04-07
 */
@Data
public class UpdateResumeJobRequest {

    /** 目标职位 ID，设为 null 可取消关联。 */
    private Long jobPositionId;
}
