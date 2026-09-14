package com.smartrecruit.recruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 职位实体，映射到 {@code rec_job_position} 表。
 *
 * <p>表示一个开放的招聘职位需求，包含职位要求、薪酬和发布状态。</p>
 *
 * @since 1.0.0
 */
@Data
@TableName(value = "rec_job_position", autoResultMap = true)
public class JobPosition implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 职位名称，例如 "Senior Java Developer"。 */
    private String title;

    /** 关联的 {@code sys_department.id}。 */
    private Long departmentId;

    /** 经验等级：ENTRY, JUNIOR, MID, SENIOR, LEAD, EXECUTIVE。 */
    @TableField("experience_level")
    private Integer level;

    /** 招聘人数（编制）。 */
    @TableField("hc_total")
    private Integer headCount;

    /** 最低月薪（人民币）。 */
    @TableField("min_salary")
    private Integer salaryMin;

    /** 最高月薪（人民币）。 */
    @TableField("max_salary")
    private Integer salaryMax;

    /** 紧急程度：0=普通，1=紧急，2=非常紧急。 */
    @TableField("is_urgent")
    private Integer urgency;

    /** 状态：DRAFT, PUBLISHED, CLOSED, CANCELLED。 */
    private Integer status;

    /** 职位类型：FULL_TIME, PART_TIME, INTERN, CONTRACT。 */
    @TableField("position_type")
    private Integer type;

    /** 工作地点城市，例如 "Beijing"。 */
    @TableField("work_location")
    private String location;

    /** 学历要求：0=高中, 1=大专, 2=本科, 3=硕士, 4=博士。 */
    @TableField("education_required")
    private Integer educationRequired;

    /** 最低年龄要求。 */
    @TableField("age_min")
    private Integer ageMin;

    /** 最高年龄要求。 */
    @TableField("age_max")
    private Integer ageMax;

    /** 职位描述，Markdown 格式。 */
    @TableField("job_description")
    private String description;

    /** 所需技能，以 JSON 数组形式存储。 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> skills;

    /** 申请人数（缓存同步）。 */
    @TableField("application_count")
    private Integer applicationCount;

    /** 招聘负责人 ID（关联 sys_user.id），用于站内通知等场景。 */
    @TableField("responsible_id")
    private Long responsibleId;

    /** 创建人 ID（关联 sys_user.id）。 */
    @TableField("create_user_id")
    private Long createUserId;

    /** 职位发布时间。 */
    @TableField("publish_time")
    private LocalDateTime publishedAt;

    /** 创建者用户名。 */
    @TableField("create_by")
    private String createdBy;

    /** 记录创建时间戳。 */
    @TableField("create_time")
    private LocalDateTime createdAt;

    /** 最后更新时间戳。 */
    @TableField("update_time")
    private LocalDateTime updatedAt;

    /** 逻辑删除标记：0=正常，1=已删除。 */
    @TableLogic
    private Integer deleted = 0;

}
