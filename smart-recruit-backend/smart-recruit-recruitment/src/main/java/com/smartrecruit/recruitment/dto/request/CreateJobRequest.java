package com.smartrecruit.recruitment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建新职位的请求 DTO。
 *
 * @since 1.0.0
 */
@Data
public class CreateJobRequest {

    /** 职位名称。 */
    @NotBlank(message = "职位名称不能为空")
    @Size(max = 128, message = "职位名称不能超过128个字符")
    private String title;

    /** 部门 ID。 */
    @NotNull(message = "部门 ID 不能为空")
    private Long departmentId;

    /** 职位级别：ENTRY、JUNIOR、MID、SENIOR、LEAD、EXECUTIVE。 */
    @NotNull(message = "经验等级不能为空")
    private Integer level;

    /** 计划招聘人数。 */
    @NotNull(message = "招聘人数不能为空")
    private Integer headCount;

    /** 最低薪资（元）。 */
    private Integer salaryMin;

    /** 最高薪资（元）。 */
    private Integer salaryMax;

    /** 紧急程度：LOW、MEDIUM、HIGH、CRITICAL。 */
    private Integer urgency;

    /** 职位类型：FULL_TIME、PART_TIME、INTERN、CONTRACT。 */
    @NotNull(message = "职位类型不能为空")
    private Integer type;

    /** 工作地点。 */
    @NotBlank(message = "工作地点不能为空")
    private String location;

    /** 职位描述（支持 Markdown）。 */
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
