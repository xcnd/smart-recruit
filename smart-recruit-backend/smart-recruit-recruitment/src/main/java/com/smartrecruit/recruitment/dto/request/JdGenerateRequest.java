package com.smartrecruit.recruitment.dto.request;

import lombok.Data;

/**
 * 调用 AI Agent 生成 JD 请求（内部服务间调用）。
 *
 * @since 2026-04-07
 */
@Data
public class JdGenerateRequest {

    /** 岗位名称。 */
    private String jobTitle;

    /** 部门名称。 */
    private String department;

    /** 经验要求（如 "3-5年"）。 */
    private String experienceLevel;
}
