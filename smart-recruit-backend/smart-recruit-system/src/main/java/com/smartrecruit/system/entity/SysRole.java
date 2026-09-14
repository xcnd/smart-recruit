package com.smartrecruit.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统角色实体，映射 {@code sys_role} 表。
 *
 * @since 2026-04-26
 */
@Data
@TableName("sys_role")
public class SysRole implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 角色名称，用于前端展示。 */
    @TableField("role_name")
    private String name;

    /** 角色编码，用于权限判断，如 ADMIN、HR_MANAGER。 */
    @TableField("role_code")
    private String code;

    /** 角色描述说明。 */
    @TableField("description")
    private String description;

    /** 角色状态：0 = 禁用，1 = 启用。 */
    @TableField("status")
    private Integer status;

    /** 排序序号，数值越小越靠前。 */
    @TableField("sort_order")
    private Integer sortOrder;

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
