package com.smartrecruit.recruitment.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 更新现有职位的请求DTO。
 *
 * @since 1.0.0
 */
@Data
public class UpdateJobRequest {

    /** 职位名称。 */
    @Size(max = 128, message = "Title must not exceed 128 characters")
    private String title;

    /** 部门 ID。 */
    private Long departmentId;

    /** 职位级别：ENTRY、JUNIOR、MID、SENIOR、LEAD、EXECUTIVE。 */
    private Integer level;

    /** 计划招聘人数。 */
    private Integer headCount;

    /** 最低薪资（元）。 */
    private Integer salaryMin;

    /** 最高薪资（元）。 */
    private Integer salaryMax;

    /** 紧急程度：LOW、MEDIUM、HIGH、CRITICAL。 */
    private Integer urgency;

    /** 职位类型：FULL_TIME、PART_TIME、INTERN、CONTRACT。 */
    private Integer type;

    /** 工作地点。 */
    private String location;

    /** 职位描述。 */
    private String description;

    /** 技能要求（JSON 数组）。 */
    private List<String> skills;

    /** 学历要求：0=高中, 1=大专, 2=本科, 3=硕士, 4=博士。 */
    private Integer educationRequired;

    /** 最低年龄要求。 */
    private Integer ageMin;

    /** 最高年龄要求。 */
    private Integer ageMax;
}
