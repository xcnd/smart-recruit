package com.smartrecruit.talent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Talent Service Application - 人才库、数据分析与仪表盘。
 *
 * <p>提供人才库管理（智能搜索与推荐）、招聘漏斗分析、
 * 渠道分析、AI生成的数据洞察，以及面向HR管理层的KPI仪表盘。</p>
 *
 * @author xdh
 * @since 2026-04-26
 */
@SpringBootApplication(scanBasePackages = {"com.smartrecruit"})
@EnableDiscoveryClient
@EnableScheduling
@MapperScan("com.smartrecruit.talent.repository")
public class TalentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TalentServiceApplication.class, args);
    }
}
