package com.smartrecruit.offer.feign;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.time.Duration;

/**
 * HTTP Interface 客户端代理配置（Offer 模块）。
 *
 * <p>使用 Spring 6 / Boot 4 原生的 {@code @HttpExchange} 替代 OpenFeign。
 * 通过 {@code @LoadBalanced} RestClient 结合 Nacos 实现服务发现与负载均衡。</p>
 *
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
public class HttpExchangeConfig {

    @Bean
    @LoadBalanced
    RestClient.Builder loadBalancedBuilder() {
        return RestClient.builder();
    }

    @Bean
    RecruitmentClient recruitmentClient(RestClient.Builder builder) {
        RestClient client = builder
                .baseUrl("http://smart-recruit-recruitment")
                .defaultHeader("INNER-REQUEST", "true")
                .build();
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(client))
                .build()
                .createClient(RecruitmentClient.class);
    }

    @Bean
    SystemClient systemClient(RestClient.Builder builder) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofSeconds(30));
        RestClient client = builder
                .baseUrl("http://smart-recruit-system")
                .defaultHeader("INNER-REQUEST", "true")
                .requestFactory(requestFactory)
                .build();
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(client))
                .build()
                .createClient(SystemClient.class);
    }

    @Bean
    SystemConfigClient systemConfigClient(RestClient.Builder builder) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        RestClient client = builder
                .baseUrl("http://smart-recruit-system")
                .defaultHeader("INNER-REQUEST", "true")
                .requestFactory(requestFactory)
                .build();
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(client))
                .build()
                .createClient(SystemConfigClient.class);
    }

    @Bean
    SystemFileClient systemFileClient(RestClient.Builder builder) {
        RestClient client = builder
                .baseUrl("http://smart-recruit-system")
                .defaultHeader("INNER-REQUEST", "true")
                .build();
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(client))
                .build()
                .createClient(SystemFileClient.class);
    }

    @Bean
    AiEngineClient aiEngineClient(RestClient.Builder builder) {
        RestClient client = builder
                .baseUrl("http://smart-recruit-ai-engine")
                .defaultHeader("INNER-REQUEST", "true")
                .build();
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(client))
                .build()
                .createClient(AiEngineClient.class);
    }
}
