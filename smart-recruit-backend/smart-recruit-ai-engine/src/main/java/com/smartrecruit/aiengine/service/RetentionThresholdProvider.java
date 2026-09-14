package com.smartrecruit.aiengine.service;

/**
 * 留任风险等级阈值提供者。
 *
 * <p>由运行环境注入实现：独立部署 AI 引擎时使用内置默认阈值，
 * 嵌入 Offer 服务时由 Offer 模块提供基于系统配置的实现，保证
 * 系统设置中修改的 AI 风险阈值实时生效。</p>
 *
 * @since 2026-04-05
 */
public interface RetentionThresholdProvider {

    /**
     * 低风险阈值（低于此值为 LOW）。
     */
    double low();

    /**
     * 中风险阈值（低于此值为 MEDIUM）。
     */
    double medium();

    /**
     * 高风险阈值（低于此值为 HIGH，高于等于为 CRITICAL）。
     */
    double high();
}
