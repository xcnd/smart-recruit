package com.smartrecruit.aiengine.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 提供 Jackson 2 {@link ObjectMapper} Bean，供 {@code LlmGatewayService} 等组件使用。
 * 注意：Spring Boot 4.x 默认使用 Jackson 3 ({@code tools.jackson})，
 * 因此本配置与 common 模块的 Jackson 3 配置不冲突。
 *
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
public class AiEngineJacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
