package com.smartrecruit.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统权限实体，映射 {@code sys_permission} 表。
 *
 * @since 2026-04-26
 */
@Data
@TableName("sys_permission")
public class SysPermission implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 父级权限 ID，0 表示顶级菜单。 */
    @TableField("parent_id")
    private Long parentId;

    /** 权限名称，用于前端展示。 */
    @TableField("perm_name")
    private String name;

    /** 权限编码，用于 Spring Security 鉴权，如 user:create。 */
    @TableField("perm_code")
    private String code;

    /** 权限类型：MENU = 菜单，BUTTON = 按钮，API = 接口。 */
    @TableField("perm_type")
    private Integer permType;

    /** 前端路由路径，仅菜单类型有效。 */
    @TableField("path")
    private String path;

    /** 前端组件路径，仅菜单类型有效。 */
    @TableField("component")
    private String component;

    /** 菜单图标名称或 class。 */
    @TableField("icon")
    private String icon;

    /** HTTP 请求方法：GET、POST、PUT、DELETE 等，仅 API 类型有效。 */
    @TableField("api_method")
    private Integer method;

    /** 所属功能模块名称。 */
    @TableField("module")
    private Integer module;

    /** API 接口路径，仅 API 类型有效。 */
    @TableField("api_path")
    private String apiPath;

    /** 排序序号，数值越小越靠前。 */
    @TableField("sort_order")
    private Integer sortOrder;

    /** 权限状态：0 = 禁用，1 = 启用。 */
    @TableField("status")
    private Integer status;

    /** 是否在菜单中可见：0 = 隐藏，1 = 显示。 */
    @TableField("visible")
    private Integer visible;

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
