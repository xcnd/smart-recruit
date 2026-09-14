package com.smartrecruit.interview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.mybatis.spring.annotation.MapperScan;
/**
 * 面试服务应用程序 - AI驱动的面试管理。
 *
 * <p>处理面试安排、AI问题生成、六维度评分的面试结果提交以及面试报告可视化。</p>
 *
 * @author xdh
 * @since 2026-04-26
 */
@SpringBootApplication(scanBasePackages = {"com.smartrecruit"})
@EnableDiscoveryClient
@EnableScheduling
@MapperScan("com.smartrecruit.interview.repository")
public class InterviewServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterviewServiceApplication.class, args);
    }
}
