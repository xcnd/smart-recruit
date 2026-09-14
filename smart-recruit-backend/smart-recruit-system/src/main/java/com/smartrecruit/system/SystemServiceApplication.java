package com.smartrecruit.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.mybatis.spring.annotation.MapperScan;

/**
 * System Service (RBAC) — 系统管理微服务。
 *
 * <p>提供认证、用户、角色、部门、权限、审计日志等核心能力。</p>
 *
 * @author xdh
 * @since 2026-04-26
 */
@SpringBootApplication(scanBasePackages = {"com.smartrecruit.system", "com.smartrecruit.common"})
@EnableDiscoveryClient
@MapperScan("com.smartrecruit.system.repository")
public class SystemServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SystemServiceApplication.class, args);
    }
}
