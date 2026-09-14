package com.smartrecruit.system.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.StringJoiner;

/**
 * 系统配置变更事件发布器。
 *
 * <p>配置更新后向 Redis 频道广播变更的配置键，
 * 其他服务实例订阅后失效本地缓存，实现跨实例立即生效。</p>
 *
 * @since 2026-04-05
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SysConfigChangePublisher {

    /** 配置变更广播频道。 */
    public static final String CHANNEL = "sys:config:changed";

    private final StringRedisTemplate redisTemplate;

    /**
     * 广播配置变更（best-effort，广播失败不影响主流程）。
     */
    public void publish(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        try {
            StringJoiner joiner = new StringJoiner(",");
            keys.forEach(joiner::add);
            redisTemplate.convertAndSend(CHANNEL, joiner.toString());
            log.debug("系统配置变更已广播: keys={}", keys);
        } catch (Exception e) {
            log.warn("系统配置变更广播失败（可忽略）: error={}", e.getMessage());
        }
    }
}
