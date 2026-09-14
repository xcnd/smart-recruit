package com.smartrecruit.recruitment.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 招聘官网投递申请请求 DTO。
 *
 * @since 1.0.0
 */
@Data
public class CareersApplyRequest {

    /** careers_job_position.id */
    @NotNull(message = "职位ID不能为空")
    private Long jobId;

    /** 候选人姓名。 */
    @NotBlank(message = "姓名不能为空")
    private String candidateName;

    /** 候选人手机号。 */
    @NotBlank(message = "手机号不能为空")
    private String candidatePhone;

    /** 候选人邮箱。 */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String candidateEmail;

    /** 上传的简历文件 URL（可选）。 */
    private String resumeUrl;

    /** 登录用户 ID（可选，用于关联系统用户）。 */
    private Long candidateId;
}
