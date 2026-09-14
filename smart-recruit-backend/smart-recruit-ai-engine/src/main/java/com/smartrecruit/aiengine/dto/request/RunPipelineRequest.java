package com.smartrecruit.aiengine.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * 全流程编排（简历 → Offer）运行请求。
 *
 * @param candidateName       候选人姓名（必填）
 * @param resumeText          简历文本（可解析出技能/年限/学历，可选）
 * @param jobTitle            目标职位（必填）
 * @param requiredSkills      职位必备技能（可选）
 * @param minYearsOfExperience 最低工作年限要求（可选）
 * @param offerTotalPackage   Offer 年薪总包（元，可选）
 * @param interviewSummary    面试摘要（可选，提供则执行面试评估阶段）
 * @since 2026-04-09
 */
public record RunPipelineRequest(
        @NotBlank(message = "候选人姓名不能为空") String candidateName,
        String resumeText,
        @NotBlank(message = "目标职位不能为空") String jobTitle,
        List<String> requiredSkills,
        Integer minYearsOfExperience,
        Double offerTotalPackage,
        String interviewSummary) {
}
