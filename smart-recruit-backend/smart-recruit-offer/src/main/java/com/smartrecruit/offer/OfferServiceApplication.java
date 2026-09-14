package com.smartrecruit.offer;

import com.smartrecruit.aiengine.agents.RetentionPredictorAgent;
import com.smartrecruit.aiengine.agents.OfferPredictorAgent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.mybatis.spring.annotation.MapperScan;

/**
 * Offer Service Application - Offer管理和入职。
 *
 * <p>管理Offer全生命周期，从创建、多级审批、发送、洽谈、
 * 接受度预测，一直到入职进度跟踪。</p>
 *
 * @author xdh
 * @since 2026-04-26
 */
@SpringBootApplication(scanBasePackages = {
    "com.smartrecruit.common",
    "com.smartrecruit.offer"
})
@EnableDiscoveryClient
@EnableScheduling
@Import({RetentionPredictorAgent.class, OfferPredictorAgent.class})
@MapperScan("com.smartrecruit.offer.repository")
public class OfferServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OfferServiceApplication.class, args);
    }
}
