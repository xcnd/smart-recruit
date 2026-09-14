package com.smartrecruit.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * 跨域资源共享（CORS）配置。
 *
 * <p>在开发环境默认启用。生产环境应使用指定的允许来源。</p>
 *
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(prefix = "smart-recruit.cors", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CorsConfig {

    private static final Logger log = LoggerFactory.getLogger(CorsConfig.class);

    /** 预检缓存的最大有效时间（1小时）。 */
    private static final long MAX_AGE = 3600L;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 开发环境允许所有来源；生产环境需限制
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of(
                "Authorization",
                "X-Trace-Id",
                "X-Request-Id",
                "Content-Disposition"
        ));
        config.setAllowCredentials(true);
        config.setMaxAge(MAX_AGE);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        log.info("CORS 过滤器已配置: allow-credentials: true, max-age: {}s", MAX_AGE);
        return new CorsFilter(source);
    }
}
