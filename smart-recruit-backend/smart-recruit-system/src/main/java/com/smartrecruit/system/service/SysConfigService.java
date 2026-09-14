package com.smartrecruit.system.service;

import com.smartrecruit.system.dto.request.ConfigItemRequest;
import com.smartrecruit.system.dto.response.SysConfigVO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 系统配置服务接口。
 *
 * @since 2026-05-26
 */
public interface SysConfigService {

    /**
     * 查询所有配置项列表。
     */
    List<SysConfigVO> listAll();

    /**
     * 根据配置键查询单个配置值。
     *
     * @param configKey 配置键
     * @return 配置值，如果配置不存在则返回 null
     */
    String getByKey(String configKey);

    /**
     * 获取配置值，缺失时返回默认值。
     *
     * @param configKey    配置键
     * @param defaultValue 默认值
     */
    String getString(String configKey, String defaultValue);

    /**
     * 获取整型配置值，解析失败或缺失时返回默认值。
     */
    int getInt(String configKey, int defaultValue);

    /**
     * 获取长整型配置值，解析失败或缺失时返回默认值。
     */
    long getLong(String configKey, long defaultValue);

    /**
     * 获取浮点型配置值，解析失败或缺失时返回默认值。
     */
    double getDouble(String configKey, double defaultValue);

    /**
     * 获取布尔开关配置值（0/1），解析失败或缺失时返回默认值。
     */
    boolean getBoolean(String configKey, boolean defaultValue);

    /**
     * 获取逗号分隔的列表配置值，缺失时返回空列表。
     */
    List<String> getList(String configKey);

    /**
     * 根据配置键前缀查询配置集合。
     *
     * @param prefix 配置键前缀
     * @return key=configKey, value=configValue 的 Map
     */
    Map<String, String> getByPrefix(String prefix);

    /**
     * 按配置键集合批量查询。
     *
     * @param keys 配置键集合
     * @return key=configKey, value=configValue 的 Map
     */
    Map<String, String> getByKeys(Collection<String> keys);

    /**
     * 获取面向管理后台的公开配置（无需认证，供前端即时生效）。
     *
     * <p>仅包含注册表中标记为公开的配置项，缺失项自动以默认值兜底。</p>
     */
    Map<String, String> getPublicConfigs();

    /**
     * 失效指定配置项的本地缓存。
     *
     * <p>供配置变更事件订阅方调用，保证多实例部署下
     * 任一实例的修改都能立即在其他实例生效。</p>
     *
     * @param keys 配置键集合
     */
    void evictCache(Collection<String> keys);

    /**
     * 批量更新配置项。
     */
    List<SysConfigVO> batchUpdate(List<ConfigItemRequest> items);
}
