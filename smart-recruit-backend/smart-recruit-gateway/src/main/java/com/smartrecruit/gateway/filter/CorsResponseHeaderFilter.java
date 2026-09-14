package com.smartrecruit.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.NettyWriteResponseFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CORS 响应头去重过滤器。
 *
 * <p>当网关和后端服务同时设置了 CORS 响应头时，浏览器可能因为重复的响应头而拒绝请求。
 * 此过滤器在所有响应写入完成后对 CORS 相关响应头进行去重，确保仅保留一套 CORS 配置。
 *
 * @author xdh
 * @since 1.0.0
 */
@Slf4j
@Component
public class CorsResponseHeaderFilter implements GlobalFilter, Ordered {

    private static final String ANY = "*";

    @Override
    public int getOrder() {
        return NettyWriteResponseFilter.WRITE_RESPONSE_FILTER_ORDER + 1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            try {
                if (exchange.getResponse().isCommitted()) {
                    return;
                }
                HttpHeaders headers = exchange.getResponse().getHeaders();
                if (headers == null || headers.isEmpty()) {
                    return;
                }

                deduplicateCorsHeader(headers, HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
                deduplicateCorsHeader(headers, HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS);
                deduplicateCorsHeader(headers, HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS);
                deduplicateCorsHeader(headers, HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS);
                deduplicateVaryHeader(headers);

            } catch (Exception e) {
                log.error("CORS 响应头去重处理发生异常", e);
            }
        }));
    }

    private void deduplicateCorsHeader(HttpHeaders headers, String headerName) {
        try {
            List<String> values = headers.get(headerName);
            if (values == null || values.size() <= 1) {
                return;
            }

            List<String> deduplicatedValues = new ArrayList<>();
            if (headerName.equals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)) {
                String nonAny = values.stream().filter(v -> !ANY.equals(v)).findFirst().orElse(ANY);
                deduplicatedValues.add(nonAny);
            } else if (values.contains(ANY)) {
                deduplicatedValues.add(ANY);
            } else {
                deduplicatedValues.add(values.get(0));
            }

            headers.put(headerName, deduplicatedValues);
            log.debug("CORS 头去重处理: {} -> {}", headerName, deduplicatedValues);
        } catch (Exception e) {
            log.warn("处理 CORS 头时发生异常: headerName={}, error={}", headerName, e.getMessage());
        }
    }

    private void deduplicateVaryHeader(HttpHeaders headers) {
        try {
            List<String> varyValues = headers.get(HttpHeaders.VARY);
            if (varyValues == null || varyValues.size() <= 1) {
                return;
            }

            List<String> deduplicatedValues = varyValues.stream()
                    .distinct()
                    .collect(Collectors.toList());

            headers.put(HttpHeaders.VARY, deduplicatedValues);
            log.debug("Vary 头去重处理: {} -> {}", HttpHeaders.VARY, deduplicatedValues);
        } catch (Exception e) {
            log.warn("处理 Vary 头时发生异常: error={}", e.getMessage());
        }
    }
}
