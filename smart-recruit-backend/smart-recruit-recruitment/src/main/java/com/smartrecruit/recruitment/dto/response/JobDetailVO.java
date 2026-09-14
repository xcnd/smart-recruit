package com.smartrecruit.recruitment.dto.response;

import lombok.Data;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 单个职位的详细视图对象。
 *
 * @since 1.0.0
 */
@Data
public class JobDetailVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 职位 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    /** 职位名称。 */
    private String title;
    /** 部门 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long departmentId;
    /** 部门名称。 */
    private String departmentName;
    /** 职位级别。 */
    private Integer level;
    /** 招聘人数。 */
    private Integer headCount;
    /** 最低薪资（元）。 */
    private Integer salaryMin;
    /** 最高薪资（元）。 */
    private Integer salaryMax;
    /** 紧急程度。 */
    private Integer urgency;
    /** 状态。 */
    private Integer status;
    /** 职位类型。 */
    private Integer type;
    /** 工作地点。 */
    private String location;
    /** 职位描述。 */
    private String description;
    /** 技能要求。 */
    private List<String> skills;
    /** 学历要求：0=高中, 1=大专, 2=本科, 3=硕士, 4=博士。 */
    private Integer educationRequired;
    /** 最低年龄要求。 */
    private Integer ageMin;
    /** 最高年龄要求。 */
    private Integer ageMax;
    /** 申请数量。 */
    private Integer applicationCount;
    /** 发布时间。 */
    private LocalDateTime publishedAt;
    /** 创建人 ID。 */
    private String createdBy;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 更新时间。 */
    private LocalDateTime updatedAt;
}
