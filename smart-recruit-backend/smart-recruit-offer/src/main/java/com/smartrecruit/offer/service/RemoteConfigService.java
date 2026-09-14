package com.smartrecruit.offer.service;

import java.util.List;
import java.util.Map;

/**
 * 远程系统配置服务，通过 Feign 拉取 system 模块的配置并带缓存。
 *
 * @since 1.0.0
 */
public interface RemoteConfigService {

    /** 按配置键获取字符串类型配置值。 */
    String getString(String key, String defaultValue);

    /** 按配置键获取浮点类型配置值。 */
    double getDouble(String key, double defaultValue);

    /** 按配置键获取 JSON 数组配置值。 */
    List<String> getList(String key);

    /** 按多个配置键批量拉取配置（key → value）。 */
    Map<String, String> fetchAll(String... keys);
}
