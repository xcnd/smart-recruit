package com.smartrecruit.referral.filter;

import com.smartrecruit.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.List;

/**
 * 认证过滤器（Referral 微服务）。
 *
 * <p>认证由 API 网关完成，网关通过以下请求头向下游注入用户信息：
 * <ul>
 *   <li>{@code X-User-Id} — 用户 ID</li>
 *   <li>{@code X-Username} — 用户名</li>
 *   <li>{@code X-User-Role} — 角色</li>
 * </ul>
 *
 * <p>当网关白名单路径（如 {@code /api/v1/referrals/public/**}）未注入这些请求头时，
 * 本过滤器会尝试直接从 {@code Authorization} 请求头中解析 JWT Token，以确保
 * 登录候选人也能在公开端点中获得有效的 SecurityContext。</p>
 *
 * @since 1.0.0
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USERNAME = "X-Username";
    private static final String HEADER_USER_ROLE = "X-User-Role";
    private static final String BEARER_PREFIX = "Bearer ";

    private final SecretKey secretKey;

    public JwtAuthenticationFilter(@Value("${jwt.secret}") String jwtSecret) {
        this.secretKey = JwtUtil.getSecretKey(jwtSecret);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String userId = request.getHeader(HEADER_USER_ID);
        String username = request.getHeader(HEADER_USERNAME);
        String role = request.getHeader(HEADER_USER_ROLE);

        if (userId != null && !userId.isEmpty()) {
            // Gateway headers are present — use them directly
            setAuthentication(userId, username, role);
        } else {
            // Gateway skipped auth (e.g., public paths) — try parsing JWT directly
            String token = extractBearerToken(request);
            if (token != null) {
                try {
                    Claims claims = JwtUtil.parseToken(secretKey, token);
                    String sub = claims.getSubject();
                    String tokenUsername = claims.get("username", String.class);
                    String tokenRole = claims.get("role", String.class);

                    if (sub != null) {
                        setAuthentication(sub, tokenUsername, tokenRole);
                        log.debug("JWT directly parsed for public endpoint: userId={}, path={}",
                                sub, request.getRequestURI());
                    }
                } catch (Exception e) {
                    log.debug("Failed to parse JWT on public endpoint (non-fatal): path={}",
                            request.getRequestURI());
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void setAuthentication(String userId, String username, String role) {
        List<SimpleGrantedAuthority> authorities = role != null && !role.isEmpty()
                ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                : List.of();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        username != null ? username : userId,
                        userId,
                        authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * 从请求中提取 Bearer Token。
     */
    private String extractBearerToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}
