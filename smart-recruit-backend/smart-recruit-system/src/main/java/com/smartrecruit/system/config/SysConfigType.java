package com.smartrecruit.system.config;

/**
 * 系统配置值类型。
 *
 * <p>用于配置项的强类型校验与类型化读取，
 * 避免字符串配置在业务代码中被随意解析导致运行时错误。</p>
 *
 * @since 2026-04-05
 */
public enum SysConfigType {

    /** 普通字符串。 */
    STRING,

    /** 整型（如密码长度、上传大小限制）。 */
    INT,

    /** 浮点型（如 AI 风险阈值）。 */
    DOUBLE,

    /** 布尔开关，存储 0/1。 */
    BOOLEAN,

    /** 逗号分隔的列表（如允许的文件类型、培训模块）。 */
    LIST
}
