package com.smartrecruit.referral;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 员工内推服务应用程序。
 *
 * <p>提供内推计划管理、内推记录跟踪、
 * 排行榜和内推策略接口。
 *
 * @author xdh
 * @since 2026-04-26
 */
@SpringBootApplication(scanBasePackages = {"com.smartrecruit.referral", "com.smartrecruit.common"})
@EnableDiscoveryClient
@MapperScan("com.smartrecruit.referral.repository")
public class ReferralServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReferralServiceApplication.class, args);
    }
}
