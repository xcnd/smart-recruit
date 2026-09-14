package com.smartrecruit.gateway.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * 网关基础配置类。
 *
 * <p>路由规则全部由 application.yml 的 {@code spring.cloud.gateway.routes}
 * 声明式配置：服务地址（{@code lb://服务名}）与匹配路径（{@code Path=...}）
 * 都直接在配置文件中维护，改配置即可生效，无需修改代码；
 * 也可以进一步迁移到 Nacos 配置中心实现动态刷新。CORS 策略同样在
 * application.yml 的 {@code spring.cloud.gateway.globalcors} 中配置。
 *
 * <p>注意：不使用 @EnableWebFlux 注解，因为 Spring Cloud Gateway 依赖
 * Spring Boot 的 WebFlux 自动配置来注册路由处理器映射。使用 @EnableWebFlux
 * 会导致 WebFluxAutoConfiguration 回退，路由匹配失败。
 *
 * <p>本类仅保留请求体大小限制和启动时的路由加载日志检查。
 *
 * @author xdh
 * @since 2026-04-26
 */
@Configuration
public class RouteConfig implements WebFluxConfigurer {

    private static final Logger log = LoggerFactory.getLogger(RouteConfig.class);

    private final RouteDefinitionLocator routeDefinitionLocator;
    private final GatewayProperties gatewayProperties;

    public RouteConfig(RouteDefinitionLocator routeDefinitionLocator,
                       GatewayProperties gatewayProperties) {
        this.routeDefinitionLocator = routeDefinitionLocator;
        this.gatewayProperties = gatewayProperties;
    }

    // ================================================================
    // Request Size Limits
    // ================================================================

    @Override
    public void configureHttpMessageCodecs(
            org.springframework.http.codec.ServerCodecConfigurer configurer) {
        configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024); // 10MB
    }

    @PostConstruct
    public void logRoutes() {
        log.info("=== GatewayProperties.routes.size = {} ===", gatewayProperties.getRoutes().size());
        gatewayProperties.getRoutes().forEach(rd ->
                log.info("GatewayProperties 路由: id={}, uri={}, predicates={}, order={}",
                        rd.getId(), rd.getUri(), rd.getPredicates(), rd.getOrder()));
        log.info("=== RouteDefinitionLocator 中的路由 ===");
        routeDefinitionLocator.getRouteDefinitions()
                .doOnNext(rd -> log.info("RouteDefinitionLocator 路由: id={}, uri={}, predicates={}",
                        rd.getId(), rd.getUri(), rd.getPredicates()))
                .doOnComplete(() -> log.info("路由加载检查完成"))
                .doOnError(e -> log.error("路由加载失败: {}", e.getMessage(), e))
                .subscribe();
    }
}
