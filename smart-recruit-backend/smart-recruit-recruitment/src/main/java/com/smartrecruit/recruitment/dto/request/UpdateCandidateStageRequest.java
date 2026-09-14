package com.smartrecruit.recruitment.dto.request;

import lombok.Data;

/**
 * 更新候选人阶段请求。
 *
 * @since 2026-04-07
 */
@Data
public class UpdateCandidateStageRequest {

    /** 目标阶段（支持阶段名称或阶段编码字符串）。 */
    private String stage;
}
