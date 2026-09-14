package com.smartrecruit.recruitment.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量简历筛选的请求DTO。
 *
 * @since 1.0.0
 */
@Data
public class BatchScreenRequest {

    /** 待筛选的简历 ID 列表（前端传字符串以避免 JS Long 精度丢失）。 */
    @NotEmpty(message = "Resume IDs must not be empty")
    private List<String> resumeIds;

    /** 可选的职位ID，用于基于上下文的评分。 */
    private Long jobId;
}
