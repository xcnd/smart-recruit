package com.smartrecruit.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色-权限关联实体，映射 {@code sys_role_permission} 表。
 *
 * @since 2026-04-26
 */
@Data
@TableName("sys_role_permission")
public class SysRolePermission implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 角色 ID，关联 sys_role.id。 */
    @TableField("role_id")
    private Long roleId;

    /** 权限 ID，关联 sys_permission.id。 */
    @TableField("permission_id")
    private Long permissionId;

    /** 创建时间。 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
