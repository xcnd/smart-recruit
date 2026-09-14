package com.smartrecruit.interview.dto.request;

import com.smartrecruit.interview.dto.response.QuestionGenerateResult.QuestionItem;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 发送面试题邮件请求 DTO。
 *
 * @since 1.0.0
 */
@Data
public class SendQuestionsEmailRequest {

    /** 面试官邮箱地址。 */
    @NotBlank(message = "邮箱地址不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 面试官姓名（可选，用于邮件问候语）。 */
    private String interviewerName;

    /** 应聘职位名称。 */
    @NotBlank(message = "职位名称不能为空")
    private String positionLabel;

    /** 技术基础题。 */
    @NotEmpty(message = "题目列表不能为空")
    private List<QuestionItem> techQuestions;

    /** 项目经验题。 */
    private List<QuestionItem> projectQuestions;

    /** 行为面试题。 */
    private List<QuestionItem> behavioralQuestions;
}
