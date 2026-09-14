package com.smartrecruit.recruitment.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 更新招聘官网职位的请求 DTO。
 *
 * @since 1.0.0
 */
@Data
public class UpdateCareersJobRequest {

    /** 招聘类型。 */
    private String recType;

    /** 职位名称。 */
    @Size(max = 128, message = "职位名称不能超过128个字符")
    private String title;

    /** 部门/团队。 */
    @Size(max = 128, message = "部门不能超过128个字符")
    private String dept;

    /** 工作地点。 */
    @Size(max = 64, message = "工作地点不能超过64个字符")
    private String location;

    /** 经验要求。 */
    private String exp;

    /** 薪资范围。 */
    private String salary;

    /** 分类。 */
    private String category;

    /** 标签数组。 */
    private List<Map<String, String>> tags;

    /** 岗位职责。 */
    private List<String> responsibilities;

    /** 任职要求。 */
    private List<String> requirements;

    /** 加分项。 */
    private List<String> bonus;

    /** 排序。 */
    private Integer sortOrder;

    /** 状态。 */
    private Integer status;
}
