package com.smartrecruit.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * SmartRecruit API Gateway 启动类。
 *
 * <p>作为平台的统一入口，负责请求路由、认证鉴权、限流熔断和跨域处理。
 * 基于 Spring Cloud Gateway 响应式架构（WebFlux），集成 Nacos 服务发现与 Reactive Redis。
 *
 * @author xdh
 * @since 2026-04-26
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
