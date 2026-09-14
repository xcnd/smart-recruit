package com.smartrecruit.recruitment.feign;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * HTTP Interface 客户端代理配置。
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
    SystemUserClient systemUserClient(RestClient.Builder builder) {
        RestClient client = builder
                .baseUrl("http://smart-recruit-system")
                .defaultHeader("INNER-REQUEST", "true")
                .build();
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(client))
                .build()
                .createClient(SystemUserClient.class);
    }

    @Bean
    SystemNotificationClient systemNotificationClient(RestClient.Builder builder) {
        RestClient client = builder
                .baseUrl("http://smart-recruit-system")
                .defaultHeader("INNER-REQUEST", "true")
                .build();
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(client))
                .build()
                .createClient(SystemNotificationClient.class);
    }

    @Bean
    SystemConfigClient systemConfigClient(RestClient.Builder builder) {
        RestClient client = builder
                .baseUrl("http://smart-recruit-system")
                .defaultHeader("INNER-REQUEST", "true")
                .build();
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(client))
                .build()
                .createClient(SystemConfigClient.class);
    }

    @Bean
    AiAgentCapabilityClient aiAgentCapabilityClient(RestClient.Builder builder) {
        RestClient client = builder
                .baseUrl("http://smart-recruit-ai-engine")
                .defaultHeader("INNER-REQUEST", "true")
                .build();
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(client))
                .build()
                .createClient(AiAgentCapabilityClient.class);
    }

    @Bean
    AiAgentLlmClient aiAgentLlmClient(RestClient.Builder builder) {
        RestClient client = builder
                .baseUrl("http://smart-recruit-ai-engine")
                .defaultHeader("INNER-REQUEST", "true")
                .build();
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(client))
                .build()
                .createClient(AiAgentLlmClient.class);
    }

    @Bean
    AiAgentTaskClient aiAgentTaskClient(RestClient.Builder builder) {
        RestClient client = builder
                .baseUrl("http://smart-recruit-ai-engine")
                .defaultHeader("INNER-REQUEST", "true")
                .build();
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(client))
                .build()
                .createClient(AiAgentTaskClient.class);
    }
}
