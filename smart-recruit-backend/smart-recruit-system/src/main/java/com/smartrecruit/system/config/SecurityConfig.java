package com.smartrecruit.system.config;

import com.smartrecruit.common.config.CustomAccessDeniedHandler;
import com.smartrecruit.common.config.CustomAuthenticationEntryPoint;
import com.smartrecruit.system.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

/**
 * Spring Security 安全配置。
 *
 * <p>配置基于 JWT 的无状态认证，集成自定义的认证入口点和访问拒绝处理器，
 * 为未登录和权限不足的场景提供统一的 JSON 格式错误响应。
 *
 * @since 2026-04-26
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    /**
     * 安全过滤器链配置。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF（API 为无状态）
                .csrf(AbstractHttpConfigurer::disable)

                // 无状态会话
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 路由授权配置
                .authorizeHttpRequests(auth -> auth
                        // 公开端点
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/auth/register").permitAll()
                        .requestMatchers("/api/v1/auth/send-code").permitAll()
                        .requestMatchers("/api/v1/auth/reset-password").permitAll()
                        .requestMatchers("/api/v1/auth/refresh").permitAll()
                        .requestMatchers("/api/v1/auth/captcha").permitAll()
                        .requestMatchers("/api/v1/auth/send-phone-code").permitAll()
                        .requestMatchers("/api/v1/auth/phone-login").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/actuator/health").permitAll()

                        // 内部微服务 Feign 调用的文件接口（仅限服务间调用，不对外暴露）
                        .requestMatchers("/api/v1/files/download-bytes").permitAll()
                        .requestMatchers("/api/v1/files/upload").permitAll()
                        .requestMatchers("/api/v1/files/delete").permitAll()

                        // 内部微服务接口（仅限服务间调用）
                        .requestMatchers("/api/v1/internal/**").permitAll()

                        // 公开的系统配置（邮箱后缀等）
                        .requestMatchers("/api/v1/configs/public/**").permitAll()

                        // 静态资源
                        .requestMatchers("/static/**", "/public/**").permitAll()

                        // Swagger / Knife4j
                        .requestMatchers("/doc.html", "/webjars/**", "/v3/api-docs/**",
                                "/swagger-resources/**", "/swagger-ui/**").permitAll()

                        // 其他所有请求需要认证
                        .anyRequest().authenticated()
                )

                // 自定义未登录和权限不足的处理器
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))

                // 在 UsernamePasswordAuthenticationFilter 之前添加 JWT 过滤器
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 宽松的 HTTP 防火墙 — 允许请求头中包含非 ASCII 字符（如中文文件名）。
     * 内部微服务 Feign 调用通过 X-File-Name 头传递文件原始名称。
     */
    @Bean
    public StrictHttpFirewall strictHttpFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        // 允许包含中文等非 ASCII 字符的请求头值（默认 StrictHttpFirewall 会拒绝）
        firewall.setAllowedHeaderValues(value -> !value.contains("\r") && !value.contains("\n"));
        return firewall;
    }

    /**
     * 将自定义防火墙应用到 WebSecurity。
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer(StrictHttpFirewall firewall) {
        return web -> web.httpFirewall(firewall);
    }

    /**
     * BCrypt 密码编码器。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
