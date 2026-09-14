package com.smartrecruit.aiengine.service.impl;

import com.smartrecruit.aiengine.service.RetentionThresholdProvider;
import org.springframework.stereotype.Component;

/**
 * 默认留任风险阈值实现（AI 引擎独立部署时使用）。
 *
 * @since 2026-04-05
 */
@Component
public class DefaultRetentionThresholdProviderImpl implements RetentionThresholdProvider {

    private static final double LOW = 0.25;
    private static final double MEDIUM = 0.50;
    private static final double HIGH = 0.75;

    /** 返回低风险阈值。 */
    @Override
    public double low() {
        return LOW;
    }

    /** 返回中风险阈值。 */
    @Override
    public double medium() {
        return MEDIUM;
    }

    /** 返回高风险阈值。 */
    @Override
    public double high() {
        return HIGH;
    }
}
