package com.smartrecruit.recruitment.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 批量将候选人转换到新阶段的请求DTO。
 *
 * @since 1.0.0
 */
@Data
public class BatchTransitionRequest {

    /** 候选人 ID 列表。 */
    @NotEmpty(message = "Candidate IDs must not be empty")
    private List<Long> candidateIds;

    /** 目标阶段：SCREENING、INTERVIEWING、OFFERED、HIRED 等。 */
    @NotNull(message = "Target stage must not be null")
    private Integer targetStage;
}
