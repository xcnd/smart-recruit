package com.smartrecruit.system.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * 系统配置变更事件监听配置。
 *
 * @since 2026-04-05
 */
@Configuration
@RequiredArgsConstructor
public class SysConfigRedisConfig {

    private final SysConfigChangeSubscriber subscriber;

    /**
     * Redis 消息监听容器，订阅配置变更频道。
     */
    @Bean
    public RedisMessageListenerContainer sysConfigMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, new ChannelTopic(SysConfigChangePublisher.CHANNEL));
        return container;
    }
}
