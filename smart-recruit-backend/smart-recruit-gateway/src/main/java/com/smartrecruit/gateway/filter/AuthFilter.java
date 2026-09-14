package com.smartrecruit.gateway.filter;

import com.alibaba.fastjson2.JSON;
import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.common.util.JwtUtil;
import com.smartrecruit.gateway.config.AuthSkipProperties;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT 认证全局过滤器。
 *
 * <p>在网关层解析 JWT Token，将用户信息注入到下游请求头中，
 * 避免下游服务重复解析 Token。
 *
 * <p>跳过认证的路径由 {@link AuthSkipProperties} 配置
 * （application.yml 的 {@code gateway.auth-skip}），包括精确路径与路径前缀；
 * 另外 OPTIONS 预检请求始终放行。</p>
 *
 * <p>过滤器顺序：-200，在 GlobalLogFilter (-100) 之前执行。
 *
 * @author xdh
 * @since 2026-04-26
 */
@Component
@Slf4j
public class AuthFilter implements GlobalFilter, Ordered {

    /** Bearer Token 前缀 */
    private static final String BEARER_PREFIX = "Bearer ";

    /** 下游传递的用户信息请求头 */
    private static final int ERR_AUTH_FAILED = 40002;
    private static final int ERR_TOKEN_MISSING = 40004;
    private static final int ERR_TOKEN_EXPIRED = 40003;
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USERNAME = "X-Username";
    private static final String HEADER_USER_ROLE = "X-User-Role";

    private final SecretKey secretKey;
    private final AntPathMatcher pathMatcher;
    private final List<String> skipPathPrefixes;
    private final List<String> skipPaths;

    public AuthFilter(@Value("${smart-recruit.jwt.secret-key}") String jwtSecret,
                      AuthSkipProperties authSkipProperties) {
        this.secretKey = JwtUtil.getSecretKey(jwtSecret);
        this.pathMatcher = new AntPathMatcher();
        this.skipPathPrefixes = authSkipProperties.getPathPrefixes();
        this.skipPaths = authSkipProperties.getExactPaths();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod() != null ? request.getMethod().name() : "GET";

        // OPTIONS 预检请求直接放行
        if (HttpMethod.OPTIONS.matches(method)) {
            return chain.filter(exchange);
        }

        // 白名单路径跳过认证
        if (shouldSkip(path)) {
            log.debug("请求路径在白名单中，跳过 JWT 认证: path={}", path);
            return chain.filter(exchange);
        }

        // 提取 Token（优先 Authorization 头，回退到 Cookie）
        String token = extractToken(request);
        if (token == null) {
            log.debug("未提供 Token，直接放行: path={}", path);
            return chain.filter(exchange);
        }

        // 验证 Token 并注入用户信息
        try {
            Claims claims = JwtUtil.parseToken(secretKey, token);

            String userId = claims.getSubject();
            String username = claims.get("username", String.class);
            String role = claims.get("role", String.class);

            if (userId == null || username == null) {
                log.warn("JWT Claims 中缺少必要字段: path={}", path);
                return writeErrorResponse(exchange, ERR_AUTH_FAILED,
                        "无效的 Token Claims");
            }

            ServerHttpRequest mutatedRequest = request.mutate()
                    .header(HEADER_USER_ID, userId)
                    .header(HEADER_USERNAME, username)
                    .header(HEADER_USER_ROLE, role != null ? role : "")
                    .build();

            log.debug("JWT 认证成功: userId={}, username={}, role={}, path={}",
                    userId, username, role, path);

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            log.warn("JWT Token 验证失败: path={}, error={}", path, e.getMessage());
            return writeErrorResponse(exchange, ERR_TOKEN_EXPIRED,
                    "Token 无效或已过期");
        }
    }

    @Override
    public int getOrder() {
        return -200;
    }

    /**
     * 判断请求路径是否在白名单中。
     */
    private boolean shouldSkip(String path) {
        for (String skipPath : skipPaths) {
            if (pathMatcher.match(skipPath, path)) {
                return true;
            }
        }
        for (String prefix : skipPathPrefixes) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从请求中提取 JWT Token。
     *
     * <p>优先从 Authorization 请求头提取，回退到 access_token Cookie，
     * 最后回退到 {@code token} 查询参数（SSE/EventSource 无法自定义请求头，只能带查询参数）。
     */
    private String extractToken(ServerHttpRequest request) {
        // 优先从 Authorization header 读取
        String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length()).trim();
        }
        // 回退：从 Cookie 读取
        var cookies = request.getCookies();
        if (cookies.containsKey("access_token")) {
            var cookie = cookies.getFirst("access_token");
            if (cookie != null && StringUtils.hasText(cookie.getValue())) {
                return cookie.getValue();
            }
        }
        // 回退：从查询参数读取（EventSource/SSE 场景）
        String queryToken = request.getQueryParams().getFirst("token");
        if (StringUtils.hasText(queryToken)) {
            return queryToken;
        }
        return null;
    }

    /**
     * 向客户端写入 JSON 格式的业务错误响应（HTTP 200）。
     */
    private Mono<Void> writeErrorResponse(ServerWebExchange exchange,
                                           int businessErrorCode,
                                           String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ApiResponse<Void> errorResponse = ApiResponse.error(businessErrorCode, message);
        byte[] responseBytes = JSON.toJSONBytes(errorResponse);

        DataBuffer buffer = response.bufferFactory().wrap(responseBytes);
        return response.writeWith(Mono.just(buffer));
    }
}
