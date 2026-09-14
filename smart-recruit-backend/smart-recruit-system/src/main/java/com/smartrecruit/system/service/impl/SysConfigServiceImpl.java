package com.smartrecruit.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartrecruit.common.constant.ConfigKeys;
import com.smartrecruit.common.exception.BusinessException;
import com.smartrecruit.system.audit.Auditable;
import com.smartrecruit.system.config.SysConfigChangePublisher;
import com.smartrecruit.system.config.SysConfigDefinition;
import com.smartrecruit.system.config.SysConfigRegistry;
import com.smartrecruit.system.config.SysConfigType;
import com.smartrecruit.system.dto.request.ConfigItemRequest;
import com.smartrecruit.system.dto.response.SysConfigVO;
import com.smartrecruit.system.entity.SysConfig;
import com.smartrecruit.system.repository.SysConfigMapper;
import com.smartrecruit.system.service.SysConfigService;
import com.smartrecruit.common.util.UserContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统配置服务实现类。
 *
 * @since 2026-05-26
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SysConfigServiceImpl implements SysConfigService {

    private final SysConfigMapper sysConfigMapper;
    private final SysConfigChangePublisher changePublisher;

    /** 配置值内存缓存：更新时主动失效，保证修改后立即生效。 */
    private final Map<String, String> valueCache = new ConcurrentHashMap<>();

    /** 查询全部记录。 */
    @Override
    public List<SysConfigVO> listAll() {
        log.info("查询所有系统配置");

        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysConfig::getId);
        List<SysConfig> configs = sysConfigMapper.selectList(wrapper);

        List<SysConfigVO> result = new ArrayList<>();
        for (SysConfig config : configs) {
            result.add(toVO(config));
        }

        log.info("系统配置查询完成，共 {} 条", result.size());
        return result;
    }

    /** 根据配置键查询配置值。 */
    @Override
    public String getByKey(String configKey) {
        String cached = valueCache.get(configKey);
        if (cached != null) {
            return cached;
        }

        SysConfig config = selectByKey(configKey);
        if (config == null) {
            return null;
        }
        valueCache.put(configKey, config.getConfigValue());
        return config.getConfigValue();
    }

    /** 按配置键获取字符串类型配置值。 */
    @Override
    public String getString(String configKey, String defaultValue) {
        String value = getByKey(configKey);
        return value != null && !value.isBlank() ? value : defaultValue;
    }

    /** 按配置键获取整数类型配置值。 */
    @Override
    public int getInt(String configKey, int defaultValue) {
        String value = getByKey(configKey);
        try {
            return value != null && !value.isBlank() ? Integer.parseInt(value.trim()) : defaultValue;
        } catch (NumberFormatException e) {
            log.warn("配置项 {} 不是合法整数: {}", configKey, value);
            return defaultValue;
        }
    }

    /** 按配置键获取长整数类型配置值。 */
    @Override
    public long getLong(String configKey, long defaultValue) {
        String value = getByKey(configKey);
        try {
            return value != null && !value.isBlank() ? Long.parseLong(value.trim()) : defaultValue;
        } catch (NumberFormatException e) {
            log.warn("配置项 {} 不是合法长整数: {}", configKey, value);
            return defaultValue;
        }
    }

    /** 按配置键获取浮点类型配置值。 */
    @Override
    public double getDouble(String configKey, double defaultValue) {
        String value = getByKey(configKey);
        try {
            return value != null && !value.isBlank() ? Double.parseDouble(value.trim()) : defaultValue;
        } catch (NumberFormatException e) {
            log.warn("配置项 {} 不是合法浮点数: {}", configKey, value);
            return defaultValue;
        }
    }

    /** 按配置键获取布尔类型配置值。 */
    @Override
    public boolean getBoolean(String configKey, boolean defaultValue) {
        String value = getByKey(configKey);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return "1".equals(value.trim()) || "true".equalsIgnoreCase(value.trim());
    }

    /** 按配置键获取 JSON 数组配置值。 */
    @Override
    public List<String> getList(String configKey) {
        String value = getByKey(configKey);
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /** 根据配置键前缀查询配置列表。 */
    @Override
    public Map<String, String> getByPrefix(String prefix) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(SysConfig::getConfigKey, prefix);
        List<SysConfig> configs = sysConfigMapper.selectList(wrapper);
        return toConfigMap(configs);
    }

    /** 根据多个配置键批量查询配置。 */
    @Override
    public Map<String, String> getByKeys(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Map.of();
        }
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysConfig::getConfigKey, keys);
        return toConfigMap(sysConfigMapper.selectList(wrapper));
    }

    /** 查询公开的系统配置（供前端渲染）。 */
    @Override
    public Map<String, String> getPublicConfigs() {
        Map<String, String> configs = getByKeys(SysConfigRegistry.publicKeys());
        // 缺失配置用默认值兜底，保证前端始终拿到完整配置
        for (String key : SysConfigRegistry.publicKeys()) {
            configs.computeIfAbsent(key,
                    k -> SysConfigRegistry.find(k).map(SysConfigDefinition::defaultValue).orElse(""));
        }
        return configs;
    }

    /** 清除指定配置的缓存。 */
    @Override
    public void evictCache(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        keys.forEach(valueCache::remove);
        log.info("系统配置本地缓存已失效: keys={}", keys);
    }

    /** 批量更新记录。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @Auditable(action = "UPDATE", resourceType = "SYSTEM_CONFIG")
    public List<SysConfigVO> batchUpdate(List<ConfigItemRequest> items) {
        log.info("批量更新系统配置，共 {} 项", items.size());

        // 1. 逐项校验（类型 + 取值范围）
        for (ConfigItemRequest item : items) {
            validateItem(item);
        }
        // 2. 跨字段语义校验
        validateCrossFields(items);

        String operator = currentOperator();
        Long operatorId = UserContextUtil.getCurrentUserId();
        for (ConfigItemRequest item : items) {
            SysConfig config = selectByKey(item.configKey());
            if (config != null) {
                config.setConfigValue(item.configValue());
                config.setUpdateBy(operator);
                config.setUpdateUserId(operatorId);
                sysConfigMapper.updateById(config);
            } else {
                // 配置不存在时自动创建（幂等写入）
                config = new SysConfig();
                config.setConfigKey(item.configKey());
                config.setConfigValue(item.configValue());
                config.setDescription(SysConfigRegistry.find(item.configKey())
                        .map(SysConfigDefinition::description).orElse(null));
                config.setCreateBy(operator);
                config.setCreateUserId(operatorId);
                config.setUpdateBy(operator);
                config.setUpdateUserId(operatorId);
                sysConfigMapper.insert(config);
            }
            // 3. 更新内存缓存，立即生效
            valueCache.put(item.configKey(), item.configValue());
        }

        // 4. 广播配置变更（多实例缓存失效）
        changePublisher.publish(items.stream().map(ConfigItemRequest::configKey).toList());

        log.info("系统配置批量更新完成");
        return listAll();
    }

    /**
     * 校验单个配置项：键必须已注册、值必须符合类型与范围约束。
     */
    private void validateItem(ConfigItemRequest item) {
        SysConfigDefinition def = SysConfigRegistry.find(item.configKey())
                .orElseThrow(() -> new BusinessException(
                        "CONFIG_KEY_NOT_REGISTERED", "未知的配置项：" + item.configKey()));

        String value = item.configValue() == null ? "" : item.configValue().trim();
        if (value.isBlank()) {
            throw new BusinessException("CONFIG_VALUE_BLANK", "配置项 " + def.key() + " 的值不能为空");
        }
        if (def.maxLength() > 0 && value.length() > def.maxLength()) {
            throw new BusinessException("CONFIG_VALUE_TOO_LONG",
                    "配置项 " + def.key() + " 长度不能超过 " + def.maxLength() + " 个字符");
        }

        try {
            switch (def.type()) {
                case INT -> {
                    int num = Integer.parseInt(value);
                    if (num < def.min() || num > def.max()) {
                        throw new BusinessException("CONFIG_VALUE_OUT_OF_RANGE",
                                "配置项 " + def.key() + " 必须在 " + def.min() + " ~ " + def.max() + " 之间");
                    }
                }
                case DOUBLE -> {
                    double num = Double.parseDouble(value);
                    if (num < def.min() || num > def.max()) {
                        throw new BusinessException("CONFIG_VALUE_OUT_OF_RANGE",
                                "配置项 " + def.key() + " 必须在 " + def.min() + " ~ " + def.max() + " 之间");
                    }
                }
                case BOOLEAN -> {
                    if (!"0".equals(value) && !"1".equals(value)) {
                        throw new BusinessException("CONFIG_VALUE_INVALID",
                                "配置项 " + def.key() + " 只能为 0 或 1");
                    }
                }
                case LIST -> {
                    // 至少 1 个非空项
                    boolean hasItem = Arrays.stream(value.split(","))
                            .map(String::trim)
                            .anyMatch(s -> !s.isEmpty());
                    if (!hasItem) {
                        throw new BusinessException("CONFIG_VALUE_EMPTY",
                                "配置项 " + def.key() + " 至少需要一项内容");
                    }
                }
                default -> { /* STRING 无需额外校验 */ }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (NumberFormatException e) {
            throw new BusinessException("CONFIG_VALUE_TYPE_MISMATCH",
                    "配置项 " + def.key() + " 必须是数字");
        }
    }

    /**
     * 跨字段语义校验：密码长度区间、AI 阈值递增关系。
     */
    private void validateCrossFields(List<ConfigItemRequest> items) {
        Map<String, String> merged = new HashMap<>();
        for (ConfigItemRequest item : items) {
            merged.put(item.configKey(), item.configValue().trim());
        }

        // 密码最小长度 ≤ 最大长度
        if (merged.containsKey(ConfigKeys.PASSWORD_MIN_LENGTH)
                && merged.containsKey(ConfigKeys.PASSWORD_MAX_LENGTH)) {
            int min = Integer.parseInt(merged.get(ConfigKeys.PASSWORD_MIN_LENGTH));
            int max = Integer.parseInt(merged.get(ConfigKeys.PASSWORD_MAX_LENGTH));
            if (min > max) {
                throw new BusinessException("PASSWORD_LENGTH_INVALID",
                        "密码最小长度不能大于最大长度");
            }
        }

        // AI 阈值：低 ≤ 中 ≤ 高
        List<String> thresholdKeys = List.of(
                ConfigKeys.AI_RISK_THRESHOLD_LOW,
                ConfigKeys.AI_RISK_THRESHOLD_MEDIUM,
                ConfigKeys.AI_RISK_THRESHOLD_HIGH);
        if (merged.keySet().containsAll(thresholdKeys)) {
            double low = Double.parseDouble(merged.get(ConfigKeys.AI_RISK_THRESHOLD_LOW));
            double medium = Double.parseDouble(merged.get(ConfigKeys.AI_RISK_THRESHOLD_MEDIUM));
            double high = Double.parseDouble(merged.get(ConfigKeys.AI_RISK_THRESHOLD_HIGH));
            if (low > medium || medium > high) {
                throw new BusinessException("AI_THRESHOLD_INVALID",
                        "AI 风险阈值必须满足：低风险 ≤ 中风险 ≤ 高风险");
            }
        }
    }

    /**
     * 从 SecurityContext 解析当前操作人（邮箱或用户名）。
     */
    private String currentOperator() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof String principal
                && !"anonymousUser".equals(principal) && !principal.isBlank()) {
            return principal;
        }
        return "system";
    }

    private SysConfig selectByKey(String configKey) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, configKey);
        return sysConfigMapper.selectOne(wrapper);
    }

    private Map<String, String> toConfigMap(List<SysConfig> configs) {
        return configs.stream().collect(Collectors.toMap(
                SysConfig::getConfigKey,
                SysConfig::getConfigValue,
                (v1, v2) -> v2,
                LinkedHashMap::new
        ));
    }

    private SysConfigVO toVO(SysConfig entity) {
        return new SysConfigVO(
                entity.getId(),
                entity.getConfigKey(),
                entity.getConfigValue(),
                entity.getDescription(),
                entity.getCreateTime(),
                entity.getUpdateTime(),
                entity.getCreateBy(),
                entity.getUpdateBy()
        );
    }
}
