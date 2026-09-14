package com.smartrecruit.gateway.filter;

import com.smartrecruit.common.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 全局日志过滤器。
 *
 * <p>记录所有请求的方法、URI、来源地址以及响应的状态码和耗时，
 * 便于问题排查和性能监控。
 *
 * @author xdh
 * @since 1.0.0
 */
@Slf4j
@Component
public class GlobalLogFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        log.info("请求 => Method: {}, URI: {}, RemoteAddress: {}",
                request.getMethod(),
                request.getURI(),
                request.getRemoteAddress());

        long startTime = DateUtils.currentEpochMillis();

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long endTime = DateUtils.currentEpochMillis();
            log.info("响应 => StatusCode: {}, Time: {}ms",
                    exchange.getResponse().getStatusCode(),
                    endTime - startTime);
        }));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
