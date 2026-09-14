package com.smartrecruit.offer.filter;

import com.smartrecruit.common.constant.Constants;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * JWT 认证过滤器（Offer 微服务）。
 * 从请求头提取 JWT Token，验证并设置 Spring Security 上下文。
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        // 内部微服务 Feign 调用：跳过 JWT 校验
        if ("true".equals(request.getHeader("INNER-REQUEST"))) {
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken("system", null,
                            List.of(new SimpleGrantedAuthority("ROLE_SYSTEM")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractToken(request);

        if (token != null) {
            try {
                SecretKey secretKey = JwtUtil.getSecretKey(jwtSecret);
                if (JwtUtil.isTokenValid(secretKey, token)) {
                    Claims claims = JwtUtil.parseToken(secretKey, token);
                    String userId = claims.getSubject();
                    String username = claims.get("username", String.class);
                    String email = claims.get("email", String.class);
                    String role = claims.get("role", String.class);
                    String permissions = claims.get("permissions", String.class);

                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    if (role != null && !role.isEmpty() && !"REFRESH".equals(role)) {
                        Arrays.stream(role.split(","))
                                .map(r -> new SimpleGrantedAuthority(Constants.ROLE_PREFIX + r))
                                .forEach(authorities::add);
                    }
                    if (permissions != null && !permissions.isEmpty()) {
                        Arrays.stream(permissions.split(","))
                                .filter(p -> !p.isEmpty())
                                .map(SimpleGrantedAuthority::new)
                                .forEach(authorities::add);
                    }

                    String principal = email != null ? email : username;
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(principal, userId, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                log.warn("JWT 解析失败: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(Constants.TOKEN_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(Constants.TOKEN_TYPE_BEARER + " ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
