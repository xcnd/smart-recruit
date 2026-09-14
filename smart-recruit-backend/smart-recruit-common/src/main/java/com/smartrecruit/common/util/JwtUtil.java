package com.smartrecruit.common.util;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.smartrecruit.common.constant.Constants;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT 工具类。
 *
 * @author xdh
 * @since 2026-04-26
 */
@Slf4j
public final class JwtUtil {

    private JwtUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 从 Base64 编码的密钥字符串生成 SecretKey。
     * 注意：生产环境密钥必须从配置中心获取，禁止硬编码。
     */
    public static SecretKey getSecretKey(String base64Secret) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Secret);
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    /**
     * 创建 Access Token。
     */
    public static String createAccessToken(SecretKey secretKey, Long userId,
                                           String username, String email,
                                           String role, String permissions) {
        return createToken(secretKey, userId, username, email, role, permissions,
                Constants.ACCESS_TOKEN_EXPIRE_SECONDS * 1000);
    }

    /**
     * 创建 Access Token（自定义有效期，支持会话超时配置）。
     *
     * @param expireSeconds 有效期（秒）
     */
    public static String createAccessToken(SecretKey secretKey, Long userId,
                                           String username, String email,
                                           String role, String permissions,
                                           long expireSeconds) {
        return createToken(secretKey, userId, username, email, role, permissions,
                expireSeconds * 1000);
    }

    /**
     * 创建 Refresh Token。
     */
    public static String createRefreshToken(SecretKey secretKey, Long userId,
                                            String username, String email) {
        return createToken(secretKey, userId, username, email, "REFRESH", "",
                Constants.REFRESH_TOKEN_EXPIRE_SECONDS * 1000);
    }

    /**
     * 创建 Token。
     */
    private static String createToken(SecretKey secretKey, Long userId,
                                       String username, String email,
                                       String role, String permissions, long expireMs) {
        Date now = DateUtils.toDate(DateUtils.now());
        Date expiration = DateUtils.toDate(DateUtils.now().plus(Duration.ofMillis(expireMs)));

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("username", username);
        extraClaims.put("email", email);
        extraClaims.put("role", role);
        extraClaims.put("permissions", permissions);

        return Jwts.builder()
                .claims(extraClaims)
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiration)
                .issuer(Constants.JWT_ISSUER)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析 Token 的 Claims。
     */
    public static Claims parseToken(SecretKey secretKey, String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Token 中提取用户 ID。
     */
    public static Long getUserId(SecretKey secretKey, String token) {
        try {
            Claims claims = parseToken(secretKey, token);
            return Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            log.warn("解析 JWT 用户 ID 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 Token 中提取用户名。
     */
    public static String getUsername(SecretKey secretKey, String token) {
        try {
            Claims claims = parseToken(secretKey, token);
            return claims.get("username", String.class);
        } catch (Exception e) {
            log.warn("解析 JWT 用户名失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 Token 中提取邮箱。
     */
    public static String getEmail(SecretKey secretKey, String token) {
        try {
            Claims claims = parseToken(secretKey, token);
            return claims.get("email", String.class);
        } catch (Exception e) {
            log.warn("解析 JWT 邮箱失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 Token 中提取角色。
     */
    public static String getRole(SecretKey secretKey, String token) {
        try {
            Claims claims = parseToken(secretKey, token);
            return claims.get("role", String.class);
        } catch (Exception e) {
            log.warn("解析 JWT 角色失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 Token 中提取权限码（逗号分隔的字符串）。
     */
    public static String getPermissions(SecretKey secretKey, String token) {
        try {
            Claims claims = parseToken(secretKey, token);
            return claims.get("permissions", String.class);
        } catch (Exception e) {
            log.warn("解析 JWT 权限失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 验证 Token 是否有效。
     */
    public static boolean isTokenValid(SecretKey secretKey, String token) {
        try {
            parseToken(secretKey, token);
            return true;
        } catch (Exception e) {
            log.warn("JWT Token 验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查 Token 是否即将过期（剩余时间不足指定秒数）。
     */
    public static boolean isTokenExpiringSoon(SecretKey secretKey, String token,
                                               long thresholdSeconds) {
        try {
            Claims claims = parseToken(secretKey, token);
            Date expiration = claims.getExpiration();
            long remainingMs = expiration.getTime() - DateUtils.currentEpochMillis();
            return remainingMs < thresholdSeconds * 1000;
        } catch (Exception e) {
            return true;
        }
    }
}
