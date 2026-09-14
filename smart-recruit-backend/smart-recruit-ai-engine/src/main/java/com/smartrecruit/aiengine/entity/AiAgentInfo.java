package com.smartrecruit.aiengine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 智能体注册信息实体，映射 {@code ai_agent_info} 表。
 *
 * <p>持久化智能体的元数据、运行状态与运行配置，
 * 供智能体监控页展示真实运行状态，并支持暂停/恢复/重启/注册等运维操作。</p>
 *
 * @since 2026-04-08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "ai_agent_info", autoResultMap = true)
public class AiAgentInfo implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 智能体唯一标识（kebab-case，如 resume-parser）。 */
    private String agentId;

    /** 智能体类名，如 ResumeParserAgent。 */
    private String agentName;

    /** 前端展示名称，如 简历解析。 */
    private String displayName;

    /** 绑定模型。 */
    private String model;

    /** 职责描述。 */
    private String description;

    /** 层级：0=编排层,1=执行层,2=复盘层。 */
    private Integer type;

    /** 运行状态：0=RUNNING,1=IDLE,2=PAUSED,3=ERROR。 */
    private Integer status;

    /** 运行配置（JSON 映射为 Map）。 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object config;

    /** 是否启用：1=启用,0=停用。 */
    private Integer enabled;

    /** 创建时间。 */
    private LocalDateTime createdAt;

    /** 更新时间。 */
    private LocalDateTime updatedAt;
}
