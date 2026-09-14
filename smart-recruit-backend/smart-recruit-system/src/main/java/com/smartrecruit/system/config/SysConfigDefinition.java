package com.smartrecruit.system.config;

/**
 * 系统配置项定义。
 *
 * <p>集中描述每个配置键的元信息：分组、值类型、是否公开、
 * 默认值、取值范围等，作为配置读写、校验和前端渲染的唯一事实来源。</p>
 *
 * @param key            配置键
 * @param group          配置分组
 * @param type           值类型
 * @param publicVisible  是否通过公开接口暴露（无需认证）
 * @param defaultValue   默认值（配置缺失或为空时兜底）
 * @param description    配置说明
 * @param min            数值最小值 / 列表最小项数（非数值类型忽略）
 * @param max            数值最大值 / 列表最大项数（非数值类型忽略）
 * @param maxLength      字符串最大长度
 * @since 2026-04-05
 */
public record SysConfigDefinition(
        String key,
        SysConfigGroup group,
        SysConfigType type,
        boolean publicVisible,
        String defaultValue,
        String description,
        long min,
        long max,
        int maxLength
) {

    /**
     * 便捷构造：无数值上下限的配置项。
     */
    public static SysConfigDefinition of(String key, SysConfigGroup group, SysConfigType type,
                                         boolean publicVisible, String defaultValue,
                                         String description, int maxLength) {
        return new SysConfigDefinition(key, group, type, publicVisible, defaultValue,
                description, Long.MIN_VALUE, Long.MAX_VALUE, maxLength);
    }

    /**
     * 便捷构造：带数值上下限的配置项。
     */
    public static SysConfigDefinition ranged(String key, SysConfigGroup group, SysConfigType type,
                                             boolean publicVisible, String defaultValue,
                                             String description, long min, long max) {
        return new SysConfigDefinition(key, group, type, publicVisible, defaultValue,
                description, min, max, 0);
    }
}
