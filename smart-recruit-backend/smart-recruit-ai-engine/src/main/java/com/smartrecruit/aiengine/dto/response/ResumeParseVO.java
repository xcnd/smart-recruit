package com.smartrecruit.aiengine.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/**
 * AI 简历解析结果视图对象。
 *
 * @param fileName          文件名
 * @param name              候选人姓名
 * @param email             电子邮箱
 * @param phone             联系电话
 * @param educationLevel    最高学历
 * @param school            毕业院校
 * @param major             所学专业
 * @param yearsOfExperience 工作年限
 * @param currentCompany    当前任职公司
 * @param currentPosition   当前职位
 * @param skills            技能列表
 * @since 2026-04-07
 */
@JsonPropertyOrder({"fileName", "name", "email", "phone", "educationLevel", "school",
        "major", "yearsOfExperience", "currentCompany", "currentPosition", "skills"})
public record ResumeParseVO(
        @JsonProperty("fileName") String fileName,
        @JsonProperty("name") String name,
        @JsonProperty("email") String email,
        @JsonProperty("phone") String phone,
        @JsonProperty("educationLevel") String educationLevel,
        @JsonProperty("school") String school,
        @JsonProperty("major") String major,
        @JsonProperty("yearsOfExperience") Integer yearsOfExperience,
        @JsonProperty("currentCompany") String currentCompany,
        @JsonProperty("currentPosition") String currentPosition,
        @JsonProperty("skills") List<String> skills
) {
}
