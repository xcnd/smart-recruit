package com.smartrecruit.system.config;

import com.smartrecruit.system.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 系统配置变更事件订阅器。
 *
 * <p>收到其他实例广播的配置变更后，失效本地缓存，
 * 保证多实例部署下配置修改立即全局生效。</p>
 *
 * @since 2026-04-05
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SysConfigChangeSubscriber implements MessageListener {

    private final SysConfigService sysConfigService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            List<String> keys = Arrays.stream(payload.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
            sysConfigService.evictCache(keys);
        } catch (Exception e) {
            log.warn("处理系统配置变更消息失败: error={}", e.getMessage());
        }
    }
}
