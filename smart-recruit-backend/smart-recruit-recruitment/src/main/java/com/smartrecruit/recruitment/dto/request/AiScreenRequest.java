package com.smartrecruit.recruitment.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * AI简历筛选的请求DTO。
 *
 * @since 1.0.0
 */
@Data
public class AiScreenRequest {

    /** 待 AI 筛选的简历 ID 列表。 */
    @NotEmpty(message = "Resume IDs must not be empty")
    private List<Long> resumeIds;

    /** 可选：用于匹配的职位ID。 */
    private Long jobId;

    /** 可选：用于评分的关键词。 */
    private List<String> keywords;
}
