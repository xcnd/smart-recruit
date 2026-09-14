package com.smartrecruit.interview.dto.remote;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 同步候选人阶段请求（内部服务间调用）。
 *
 * @since 2026-04-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCandidateStageRequest {

    /** 目标阶段名称。 */
    private String stage;
}
