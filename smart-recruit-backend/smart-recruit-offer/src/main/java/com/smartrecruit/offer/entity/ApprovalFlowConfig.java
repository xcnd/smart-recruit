package com.smartrecruit.offer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审批流程配置实体，映射 {@code rec_approval_flow_config} 表。
 *
 * <p>支持按部门配置多级审批流程，每级可指定多名审批人。</p>
 *
 * @since 1.0.0
 */
@Data
@TableName(value = "rec_approval_flow_config", autoResultMap = true)
public class ApprovalFlowConfig implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 流程名称。 */
    private String flowName;

    /** 适用部门名称。 */
    private String departmentName;

    /** 是否启用：0=停用，1=启用。 */
    private Integer isActive;

    /** 审批层级总数。 */
    private Integer maxLevels;

    /** 审批节点配置 JSON。 */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.Fastjson2TypeHandler.class)
    private Object nodes;

    /** 流程说明。 */
    private String description;

    /** 创建时间。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建人ID。 */
    private Long createUserId;

    /** 创建人用户名。 */
    private String createBy;

    /** 更新人ID。 */
    private Long updateUserId;

    /** 更新人用户名。 */
    private String updateBy;
}
