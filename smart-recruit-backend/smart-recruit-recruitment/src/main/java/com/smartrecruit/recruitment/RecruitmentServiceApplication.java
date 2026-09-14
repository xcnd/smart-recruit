package com.smartrecruit.recruitment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.mybatis.spring.annotation.MapperScan;

/**
 * 招聘服务应用。
 *
 * <p>SmartRecruit 平台的第 5 阶段微服务，负责职位、简历、候选人
 * 以及求职申请管理。</p>
 *
 * @since 1.0.0
 */
@SpringBootApplication(scanBasePackages = {"com.smartrecruit.recruitment", "com.smartrecruit.common"})
@EnableDiscoveryClient
@EnableScheduling
@MapperScan("com.smartrecruit.recruitment.repository")
public class RecruitmentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecruitmentServiceApplication.class, args);
    }
}
