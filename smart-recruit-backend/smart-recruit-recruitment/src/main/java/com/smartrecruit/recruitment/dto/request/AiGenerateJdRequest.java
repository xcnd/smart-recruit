package com.smartrecruit.recruitment.dto.request;

import lombok.Data;

import java.util.List;

/**
 * AI 生成职位描述请求。
 *
 * @since 2026-04-07
 */
@Data
public class AiGenerateJdRequest {

    /** 职位标题（缺省为 Software Engineer）。 */
    private String title;

    /** 所属部门名称。 */
    private String department;

    /** 经验要求文案（如 "3-5年"）。 */
    private String experience;

    /** 工作地点（如 "北京"）。 */
    private String location;

    /** 技能关键词列表。 */
    private List<String> skills;

    /** 生成关键词，用于约束 JD 内容。 */
    private String keywords;
}
