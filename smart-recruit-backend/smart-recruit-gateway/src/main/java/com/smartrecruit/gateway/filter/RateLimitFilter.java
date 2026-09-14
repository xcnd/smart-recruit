package com.smartrecruit.gateway.filter;

import com.alibaba.fastjson2.JSON;
import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.common.util.DateUtils;
import com.smartrecruit.common.util.SecurityUtil;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于内存的简单限流过滤器。
 *
 * <p>使用固定窗口算法（每分钟）限制每个 IP 的请求数。
 * 超过限制时返回 HTTP 429 (Too Many Requests) 响应。
 *
 * <p>限流窗口以自然分钟为边界（如 12:00:00 到 12:00:59）。
 * 计数器在窗口边界自动重置。
 *
 * <p><b>注意：</b>此实现适用于单实例部署。分布式环境下应替换为
 * Redis 滑动窗口算法（结合 spring-boot-starter-data-redis-reactive）。
 *
 * @author xdh
 * @since 2026-04-26
 */
@Component
@Slf4j
public class RateLimitFilter implements GlobalFilter, Ordered {

    // ================================================================
    // 限流记录
    // ================================================================

    /**
     * 单 IP 的限流记录。
     *
     * @param windowStartMinute 窗口起始分钟（以 epoch minute 表示）
     * @param counter           当前窗口内的请求计数器
     */
    private record RateLimitRecord(long windowStartMinute, AtomicLong counter) {
        RateLimitRecord(long windowStartMinute) {
            this(windowStartMinute, new AtomicLong(1));
        }
    }

    // ================================================================
    // 配置
    // ================================================================

    /** 每个 IP 每分钟最大请求数 */
    private final int maxRequestsPerMinute;

    // ================================================================
    // 状态
    // ================================================================

    /** IP 到限流记录的映射 */
    private final ConcurrentMap<String, RateLimitRecord> rateLimitMap = new ConcurrentHashMap<>();

    // ================================================================
    // 构造方法
    // ================================================================

    /**
     * 构造限流过滤器。
     *
     * @param maxRequestsPerMinute 每分钟最大请求数，从配置文件注入
     */
    public RateLimitFilter(
            @Value("${smart-recruit.gateway.rate-limit.max-requests-per-minute:200}")
            int maxRequestsPerMinute) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
        log.info("限流过滤器初始化完成: maxRequestsPerMinute={}", maxRequestsPerMinute);
    }

    // ================================================================
    // GlobalFilter 实现
    // ================================================================

    /**
     * 执行限流检查。
     *
     * <p>以当前 epoch 分钟作为窗口标识，检查该 IP 在当前窗口内
     * 的请求数是否超过阈值。
     *
     * @param exchange 当前请求-响应交换
     * @param chain    过滤器链
     * @return 下一个过滤器的 Mono
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientIp = extractClientIp(exchange);
        long currentMinute = DateUtils.currentEpochMillis() / 60_000;

        RateLimitRecord record = rateLimitMap.compute(clientIp, (ip, existing) -> {
            if (existing == null || existing.windowStartMinute() != currentMinute) {
                // 新 IP 或进入新窗口，创建新记录
                return new RateLimitRecord(currentMinute);
            }
            // 同一窗口内增加计数
            long currentCount = existing.counter().incrementAndGet();
            return existing;
        });

        // 检查是否超过限制（首次创建时 counter 初始为 1，不会触发）
        long currentCount = record.counter().get();
        if (currentCount > maxRequestsPerMinute) {
            log.warn("请求频率超限: ip={}, count={}, max={}, path={}",
                    clientIp, currentCount, maxRequestsPerMinute,
                    exchange.getRequest().getURI().getPath());
            return writeRateLimitResponse(exchange);
        }

        log.debug("限流检查通过: ip={}, count={}/{}, path={}",
                clientIp, currentCount, maxRequestsPerMinute,
                exchange.getRequest().getURI().getPath());

        return chain.filter(exchange);
    }

    // ================================================================
    // Ordered 实现
    // ================================================================

    /**
     * 返回过滤器顺序。
     *
     * <p>限流过滤器在认证过滤器之后执行（HIGHEST_PRECEDENCE + 20），
     * 确保先认证再限流，避免未认证的请求消耗限流配额。
     *
     * @return 过滤器顺序值
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }

    // ================================================================
    // 私有方法
    // ================================================================

    /**
     * 从请求中提取客户端真实 IP。
     *
     * <p>优先从 X-Forwarded-For 代理头获取，其次从 X-Real-IP 获取，
     * 最后使用直接连接的远程地址。
     *
     * @param exchange 当前请求-响应交换
     * @return 客户端 IP 字符串
     */
    private String extractClientIp(ServerWebExchange exchange) {
        return SecurityUtil.getClientIpAddress(
                name -> exchange.getRequest().getHeaders().getFirst(name),
                () -> {
                    InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
                    return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : null;
                });
    }

    /**
     * 写入 429 限流响应。
     *
     * @param exchange 当前请求-响应交换
     * @return 空的 Mono，表示响应已完成
     */
    private Mono<Void> writeRateLimitResponse(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set("Retry-After", "60");

        ApiResponse<Void> errorResponse = ApiResponse.error(40029,
                "请求频率超限，请稍后重试。当前限制为每分钟 " + maxRequestsPerMinute + " 次请求。");

        byte[] responseBytes = JSON.toJSONBytes(errorResponse);

        DataBuffer buffer = response.bufferFactory().wrap(responseBytes);
        return response.writeWith(Mono.just(buffer));
    }
}
