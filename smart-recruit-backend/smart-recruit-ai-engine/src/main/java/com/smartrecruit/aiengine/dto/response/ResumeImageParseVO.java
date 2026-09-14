package com.smartrecruit.aiengine.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 图片/扫描件简历 AI 解析结果。
 *
 * @param fileName         文件名
 * @param reviewRequired   是否需人工复核（AI 无法完整识别时置 true）
 * @param message          提示信息（如需人工复核的原因）
 * @param name             姓名
 * @param email            邮箱
 * @param phone            手机号
 * @param educationLevel   学历
 * @param school           毕业院校
 * @param major            专业
 * @param yearsOfExperience 工作年限
 * @param currentCompany   当前公司
 * @param currentPosition  当前职位
 * @param skills           技能列表
 * @param skillsText       技能板块原文（保留原始换行与分组）
 * @param education        教育经历列表
 * @param experience       工作经历列表
 * @param projects         项目经历列表
 * @param strengths        个人优势
 * @param summary          综合评语
 * @param gender           性别
 * @param household        籍贯
 * @param location         现居地
 * @param age              年龄
 * @param birthDate        出生日期
 * @param workYears        工作年限
 * @param politicalStatus  政治面貌
 * @param desiredPosition  期望职位
 */
public record ResumeImageParseVO(
        @JsonProperty("fileName") String fileName,
        @JsonProperty("reviewRequired") boolean reviewRequired,
        @JsonProperty("message") String message,
        @JsonProperty("name") String name,
        @JsonProperty("email") String email,
        @JsonProperty("phone") String phone,
        @JsonProperty("educationLevel") String educationLevel,
        @JsonProperty("school") String school,
        @JsonProperty("major") String major,
        @JsonProperty("yearsOfExperience") Integer yearsOfExperience,
        @JsonProperty("currentCompany") String currentCompany,
        @JsonProperty("currentPosition") String currentPosition,
        @JsonProperty("skills") List<String> skills,
        @JsonProperty("skillsText") String skillsText,
        @JsonProperty("education") List<EducationItem> education,
        @JsonProperty("experience") List<ExperienceItem> experience,
        @JsonProperty("projects") List<ProjectItem> projects,
        @JsonProperty("strengths") String strengths,
        @JsonProperty("summary") String summary,
        @JsonProperty("gender") String gender,
        @JsonProperty("household") String household,
        @JsonProperty("location") String location,
        @JsonProperty("age") String age,
        @JsonProperty("birthDate") String birthDate,
        @JsonProperty("workYears") String workYears,
        @JsonProperty("politicalStatus") String politicalStatus,
        @JsonProperty("desiredPosition") String desiredPosition
) {
    /** 教育经历。 */
    public record EducationItem(
            @JsonProperty("school") String school,
            @JsonProperty("major") String major,
            @JsonProperty("degree") String degree,
            @JsonProperty("start") String start,
            @JsonProperty("end") String end) {
    }

    /** 工作经历。 */
    public record ExperienceItem(
            @JsonProperty("company") String company,
            @JsonProperty("position") String position,
            @JsonProperty("start") String start,
            @JsonProperty("end") String end,
            @JsonProperty("description") String description) {
    }

    /** 项目经历。 */
    public record ProjectItem(
            @JsonProperty("name") String name,
            @JsonProperty("role") String role,
            @JsonProperty("start") String start,
            @JsonProperty("end") String end,
            @JsonProperty("description") String description) {
    }
}
