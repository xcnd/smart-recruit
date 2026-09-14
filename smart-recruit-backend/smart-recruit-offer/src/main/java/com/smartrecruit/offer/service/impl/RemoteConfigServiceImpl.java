package com.smartrecruit.offer.service.impl;

import com.smartrecruit.common.constant.ConfigKeys;
import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.offer.feign.SystemConfigClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.smartrecruit.offer.service.RemoteConfigService;

/**
 * 远程系统配置读取服务（Offer 模块）。
 *
 * <p>每次读取实时向系统服务拉取最新配置，保证后台修改后立即生效；
 * 系统服务不可用时优雅降级为默认值，不影响核心业务。</p>
 *
 * @since 2026-04-05
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RemoteConfigServiceImpl implements RemoteConfigService {

    private final SystemConfigClient systemConfigClient;

    /**
     * 获取字符串配置值，缺失或远程不可用时返回默认值。
     */
    public String getString(String key, String defaultValue) {
        String value = fetch(key).get(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    /**
     * 获取浮点型配置值，解析失败或远程不可用时返回默认值。
     */
    public double getDouble(String key, double defaultValue) {
        String value = fetch(key).get(key);
        try {
            return value != null && !value.isBlank() ? Double.parseDouble(value) : defaultValue;
        } catch (NumberFormatException e) {
            log.warn("远程配置 {} 不是合法数字: {}", key, value);
            return defaultValue;
        }
    }

    /**
     * 获取逗号分隔的列表配置值，缺失时返回空列表。
     */
    public List<String> getList(String key) {
        String value = fetch(key).get(key);
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * 批量拉取配置（单次远程调用）。
     */
    private Map<String, String> fetch(String key) {
        return fetchAll(key);
    }

    /**
     * 批量拉取配置（单次远程调用），远程不可用时返回空 Map。
     */
    public Map<String, String> fetchAll(String... keys) {
        if (keys == null || keys.length == 0) {
            return Map.of();
        }
        String joined = String.join(",", keys);
        try {
            ApiResponse<Map<String, String>> response = systemConfigClient.getConfigs(joined);
            if (response.ok() && response.data() != null) {
                return response.data();
            }
            log.warn("拉取远程配置失败: key={}, code={}, message={}",
                    joined, response.code(), response.message());
        } catch (Exception e) {
            log.warn("拉取远程配置异常（降级为默认值）: key={}, error={}", joined, e.getMessage());
        }
        return Map.of();
    }
}
