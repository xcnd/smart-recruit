package com.smartrecruit.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关认证跳过路径配置。
 *
 * <p>由 {@link com.smartrecruit.gateway.filter.AuthFilter} 读取，
 * 用于配置无需 JWT 认证的路径（精确路径 + 路径前缀），
 * 全部在 application.yml 的 {@code gateway.auth-skip} 下维护，
 * 调整后无需修改代码。</p>
 *
 * @since 2026-04-12
 */
@Component
@ConfigurationProperties(prefix = "gateway.auth-skip")
@Data
public class AuthSkipProperties {

    /**
     * 无需认证的精确路径（支持 Ant 通配符，如 {@code /api/v1/configs/public/**}）。
     */
    private List<String> exactPaths = new ArrayList<>(List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/configs/public/**",
            "/api/v1/careers/jobs/public/**",
            "/files/download/**",
            "/api/v1/files/download/**",
            "/actuator/health",
            "/doc.html",
            "/swagger-resources",
            "/v3/api-docs"
    ));

    /**
     * 无需认证的路径前缀（路径以任一前缀开头即跳过认证）。
     */
    private List<String> pathPrefixes = new ArrayList<>(List.of(
            "/api/v1/auth/",
            "/api/v1/public/",
            "/api/v1/referrals/public/",
            "/api/v2/agents/",
            "/actuator/",
            "/doc.html",
            "/swagger-resources/",
            "/swagger-ui/",
            "/v3/api-docs/",
            "/webjars/"
    ));
}
