package com.smartrecruit.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统配置实体，映射 {@code sys_config} 表。
 *
 * @since 2026-05-26
 */
@Data
@TableName("sys_config")
public class SysConfig implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 配置键，如 email_suffix。 */
    @TableField("config_key")
    private String configKey;

    /** 配置值。 */
    @TableField("config_value")
    private String configValue;

    /** 配置项描述说明。 */
    @TableField("description")
    private String description;

    /** 创建时间。 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间。 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建人姓名。 */
    @TableField("create_by")
    private String createBy;

    /** 创建人ID。 */
    @TableField("create_user_id")
    private Long createUserId;

    /** 更新人姓名。 */
    @TableField("update_by")
    private String updateBy;

    /** 更新人ID。 */
    @TableField("update_user_id")
    private Long updateUserId;
}
