package com.smartrecruit.talent.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 认证过滤器（Talent 微服务）。
 *
 * <p>认证已由 API 网关完成，网关通过以下请求头向下游注入用户信息：
 * <ul>
 *   <li>{@code X-User-Id} — 用户 ID</li>
 *   <li>{@code X-Username} — 用户名</li>
 *   <li>{@code X-User-Role} — 角色</li>
 * </ul>
 *
 * <p>本过滤器将网关头中的信息写入 Spring Security 上下文，
 * 使业务代码可以通过 {@code SecurityContextHolder} 获取当前用户，
 * 与 Offer 模块的做法保持一致。
 *
 * @since 1.0.0
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USERNAME = "X-Username";
    private static final String HEADER_USER_ROLE = "X-User-Role";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String userId = request.getHeader(HEADER_USER_ID);
        String username = request.getHeader(HEADER_USERNAME);
        String role = request.getHeader(HEADER_USER_ROLE);

        if (userId != null && !userId.isEmpty()) {
            List<SimpleGrantedAuthority> authorities = role != null && !role.isEmpty()
                    ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    : List.of();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            username != null ? username : userId,  // principal
                            userId,                                 // credentials → getCurrentUserId() 读取
                            authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("网关认证成功: userId={}, username={}, role={}", userId, username, role);
        }

        filterChain.doFilter(request, response);
    }
}
