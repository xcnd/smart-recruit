package com.smartrecruit.aiengine;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI引擎服务应用。
 *
 * <p>为招聘流程提供模拟AI智能体能力：
 * 简历解析、智能筛选、面试评估、Offer预测、
 * 留任风险评估和内推匹配。
 *
 * @author xdh
 * @since 2026-04-26
 */
@SpringBootApplication(scanBasePackages = {"com.smartrecruit.aiengine", "com.smartrecruit.common"})
@EnableDiscoveryClient
@EnableScheduling
@MapperScan("com.smartrecruit.aiengine.repository")
public class AiEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiEngineApplication.class, args);
    }
}
