package com.smartrecruit.offer.service.impl;

import com.smartrecruit.aiengine.service.RetentionThresholdProvider;
import com.smartrecruit.common.constant.ConfigKeys;
import com.smartrecruit.offer.service.RemoteConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 基于系统配置的留任风险阈值实现（Offer 服务运行时生效）。
 *
 * <p>每次评估实时读取系统设置中的 AI 风险阈值，修改后立即生效；
 * 远程不可用时回退到内置默认值。</p>
 *
 * @since 2026-04-05
 */
@Component
@RequiredArgsConstructor
public class ConfiguredRetentionThresholdProviderImpl implements RetentionThresholdProvider {

    private final RemoteConfigService remoteConfigService;

    /** 返回低风险阈值。 */
    @Override
    public double low() {
        return thresholds().getOrDefault(ConfigKeys.AI_RISK_THRESHOLD_LOW, 0.25);
    }

    /** 返回中风险阈值。 */
    @Override
    public double medium() {
        return thresholds().getOrDefault(ConfigKeys.AI_RISK_THRESHOLD_MEDIUM, 0.50);
    }

    /** 返回高风险阈值。 */
    @Override
    public double high() {
        return thresholds().getOrDefault(ConfigKeys.AI_RISK_THRESHOLD_HIGH, 0.75);
    }

    /** 单次远程调用拉取全部阈值。 */
    private Map<String, Double> thresholds() {
        return remoteConfigService.fetchAll(
                ConfigKeys.AI_RISK_THRESHOLD_LOW,
                ConfigKeys.AI_RISK_THRESHOLD_MEDIUM,
                ConfigKeys.AI_RISK_THRESHOLD_HIGH
        ).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> parseDouble(e.getValue(), 0.0)));
    }

    private double parseDouble(String value, double defaultValue) {
        try {
            return value != null && !value.isBlank() ? Double.parseDouble(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
