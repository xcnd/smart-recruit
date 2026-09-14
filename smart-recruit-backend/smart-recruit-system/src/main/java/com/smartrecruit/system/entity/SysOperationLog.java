package com.smartrecruit.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志实体，映射 {@code sys_operation_log} 表。
 *
 * @since 2026-04-26
 */
@Data
@TableName("sys_operation_log")
public class SysOperationLog implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 操作用户 ID。 */
    @TableField("user_id")
    private Long userId;

    /** 操作用户名。 */
    @TableField("username")
    private String username;

    /** 操作所属模块，如"用户管理"、"角色管理"。 */
    @TableField("module")
    private Integer module;

    /** 操作类型：CREATE、UPDATE、DELETE、EXPORT 等。 */
    @TableField("action")
    private Integer action;

    /** 操作目标类型，如 USER、ROLE、PERMISSION。 */
    @TableField("target_type")
    private String targetType;

    /** 操作目标记录的主键 ID。 */
    @TableField("target_id")
    private Long targetId;

    /** 操作描述，如"创建用户张三"。 */
    @TableField("description")
    private String description;

    /** HTTP 请求方法：GET、POST、PUT、DELETE。 */
    @TableField("request_method")
    private String requestMethod;

    /** 请求 URI 路径。 */
    @TableField("request_uri")
    private String requestUri;

    /** 请求参数（JSON 格式，可能脱敏）。 */
    @TableField("request_params")
    private String requestParams;

    /** HTTP 响应状态码，如 200、403、500。 */
    @TableField("response_status")
    private Integer responseStatus;

    /** 客户端 IP 地址。 */
    @TableField("client_ip")
    private String clientIp;

    /** 客户端 User-Agent 信息。 */
    @TableField("user_agent")
    private String userAgent;

    /** 接口执行耗时（毫秒）。 */
    @TableField("duration_ms")
    private Long durationMs;

    /** 错误信息，操作成功时为空。 */
    @TableField("error_msg")
    private String errorMsg;

    /** 全链路追踪 ID。 */
    @TableField("trace_id")
    private String traceId;

    /** 创建时间。 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
