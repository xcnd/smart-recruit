package com.smartrecruit.interview.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 提交面试反馈和结果的请求DTO。
 *
 * @since 1.0.0
 */
@Data
public class SubmitInterviewRequest {

    /** 面试反馈内容（JSON 格式，包含各维度评分与评价）。 */
    @NotBlank(message = "反馈内容不能为空")
    @Size(max = 5000, message = "反馈内容最多5000字")
    private String feedback;
}
