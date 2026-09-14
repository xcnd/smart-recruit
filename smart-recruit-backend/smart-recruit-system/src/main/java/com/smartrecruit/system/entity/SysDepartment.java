package com.smartrecruit.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统部门实体，映射 {@code sys_department} 表。
 *
 * @since 2026-04-26
 */
@Data
@TableName("sys_department")
public class SysDepartment implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 父级部门 ID，0 表示顶级部门。 */
    @TableField("parent_id")
    private Long parentId;

    /** 部门名称。 */
    @TableField("dept_name")
    private String name;

    /** 部门编码，全局唯一。 */
    @TableField("dept_code")
    private String code;

    /** 部门编制人数（预算编制）。 */
    @TableField("headcount")
    private Integer headcount;

    /** 部门当前在职人数。 */
    @TableField("staff_count")
    private Integer staffCount;

    /** 部门层级：1 = 一级部门，2 = 二级部门，以此类推。 */
    @TableField("level")
    private Integer level;

    /** 部门负责人用户 ID，关联 sys_user.id。 */
    @TableField("leader_id")
    private Long leaderId;

    /** 排序序号，数值越小越靠前。 */
    @TableField("sort_order")
    private Integer sortOrder;

    /** 部门状态：0 = 禁用，1 = 启用。 */
    @TableField("status")
    private Integer status;

    /** 部门描述说明。 */
    @TableField("description")
    private String description;

    /** 创建时间。 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间。 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建人 ID。 */
    @TableField("create_user_id")
    private Long createUserId;

    /** 创建人姓名。 */
    @TableField("create_by")
    private String createBy;

    /** 更新人 ID。 */
    @TableField("update_user_id")
    private Long updateUserId;

    /** 更新人姓名。 */
    @TableField("update_by")
    private String updateBy;

    /** 逻辑删除标记：0 = 未删除，1 = 已删除。 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted = 0;
}
