package com.smartrecruit.recruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 招聘官网职位实体，映射到 {@code careers_job_position} 表。
 *
 * @since 1.0.0
 */
@Data
@TableName(value = "careers_job_position", autoResultMap = true)
public class CareersJobPosition implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 招聘类型：SOCIAL/CAMPUS/HOT。 */
    @TableField("rec_type")
    private String recType;

    /** 职位名称。 */
    private String title;

    /** 部门/团队。 */
    private String dept;

    /** 工作地点。 */
    private String location;

    /** 经验要求。 */
    private String exp;

    /** 薪资范围。 */
    private String salary;

    /** 分类：tech/product/market/data/operation。 */
    private String category;

    /** 标签数组 [{text, cls}]。 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, String>> tags;

    /** 岗位职责。 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> responsibilities;

    /** 任职要求。 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> requirements;

    /** 加分项。 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> bonus;

    /** 排序。 */
    @TableField("sort_order")
    private Integer sortOrder;

    /** 状态：1=已发布, 0=草稿。 */
    private Integer status;

    /** 创建时间。 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间。 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建人ID。 */
    private Long createUserId;

    /** 创建人用户名。 */
    private String createBy;

    /** 更新人ID。 */
    private Long updateUserId;

    /** 更新人用户名。 */
    private String updateBy;

    /** 逻辑删除标记：0=正常，1=已删除。 */
    @TableLogic
    private Integer deleted = 0;
}
