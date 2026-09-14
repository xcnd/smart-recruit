package com.smartrecruit.interview.feign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.time.Duration;

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

    private static final Logger log = LoggerFactory.getLogger(HttpExchangeConfig.class);

    @Bean
    @LoadBalanced
    RestClient.Builder loadBalancedBuilder() {
        return RestClient.builder();
    }

    @Bean
    RecruitmentClient recruitmentClient(RestClient.Builder builder) {
        var httpClient = java.net.http.HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        RestClient client = builder
                .baseUrl("http://smart-recruit-recruitment")
                .defaultHeader("INNER-REQUEST", "true")
                .requestFactory(requestFactory)
                .build();
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(client))
                .build()
                .createClient(RecruitmentClient.class);
    }

    @Bean
    AiEngineClient aiEngineClient(RestClient.Builder builder) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofSeconds(300));
        RestClient client = builder
                .baseUrl("http://smart-recruit-ai-engine")
                .defaultHeader("INNER-REQUEST", "true")
                .requestFactory(requestFactory)
                .build();
        AiEngineClient proxy = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(client))
                .build()
                .createClient(AiEngineClient.class);
        log.info("AiEngineClient Bean 创建成功: baseUrl=http://smart-recruit-ai-engine, readTimeout=300s, proxyClass={}",
                proxy.getClass().getSimpleName());
        return proxy;
    }

    @Bean
    AiAgentScopeClient aiAgentScopeClient(RestClient.Builder builder) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofSeconds(300));
        RestClient client = builder
                .baseUrl("http://smart-recruit-ai-engine")
                .defaultHeader("INNER-REQUEST", "true")
                .requestFactory(requestFactory)
                .build();
        AiAgentScopeClient proxy = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(client))
                .build()
                .createClient(AiAgentScopeClient.class);
        log.info("AiAgentScopeClient Bean 创建成功: baseUrl=http://smart-recruit-ai-engine, readTimeout=300s");
        return proxy;
    }

    @Bean
    SystemClient systemClient(RestClient.Builder builder) {
        RestClient client = builder
                .baseUrl("http://smart-recruit-system")
                .defaultHeader("INNER-REQUEST", "true")
                .build();
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(client))
                .build()
                .createClient(SystemClient.class);
    }

    @Bean
    AiAgentCapabilityClient aiAgentCapabilityClient(RestClient.Builder builder) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        // 出题等能力内部已改异步任务，但同步兜底路径也需要足够超时，
        // 避免 LLM 生成（可达数十秒）时客户端提前取消
        requestFactory.setReadTimeout(Duration.ofSeconds(300));
        RestClient client = builder
                .baseUrl("http://smart-recruit-ai-engine")
                .defaultHeader("INNER-REQUEST", "true")
                .requestFactory(requestFactory)
                .build();
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(client))
                .build()
                .createClient(AiAgentCapabilityClient.class);
    }

    @Bean
    TalentClient talentClient(RestClient.Builder builder) {
        var httpClient = java.net.http.HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        RestClient client = builder
                .baseUrl("http://smart-recruit-talent")
                .defaultHeader("INNER-REQUEST", "true")
                .requestFactory(requestFactory)
                .build();
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(client))
                .build()
                .createClient(TalentClient.class);
    }
}
