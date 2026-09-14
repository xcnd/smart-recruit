package com.smartrecruit.system.config;

/**
 * 系统配置分组，与后台「系统设置」页面分类一一对应。
 *
 * @since 2026-04-05
 */
public enum SysConfigGroup {

    /** 通用设置。 */
    GENERAL,

    /** 邮件配置。 */
    EMAIL,

    /** 安全策略。 */
    SECURITY,

    /** 文件管理。 */
    FILE,

    /** 入职管理。 */
    ONBOARDING,

    /** AI 引擎。 */
    AI,

    /** 招聘官网（独立配置页面维护）。 */
    CAREERS
}
