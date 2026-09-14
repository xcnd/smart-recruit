package com.smartrecruit.interview.util;

import com.smartrecruit.common.constant.Constants;
import com.smartrecruit.common.util.DateUtils;
import com.smartrecruit.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 测评访问 Token 工具类。
 *
 * <p>为在线测评生成带时效的 JWT token，候选人通过链接中的 token 访问测评页面。</p>
 *
 * @since 1.0.0
 */
@Slf4j
public final class AssessmentTokenUtil {

    /** 测评 token 过期时间：7 天（秒）。 */
    private static final long ASSESSMENT_TOKEN_EXPIRE_SECONDS = 604800;

    private AssessmentTokenUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 创建测评访问 Token。
     *
     * @param secretKey    JWT 签名密钥
     * @param assessmentId 测评记录 ID
     * @param candidateId  候选人 ID
     * @param email        候选人邮箱
     * @return JWT token 字符串
     */
    public static String createAssessmentToken(SecretKey secretKey, Long assessmentId,
                                                Long candidateId, String email) {
        Date now = DateUtils.toDate(DateUtils.now());
        Date expiration = DateUtils.toDate(DateUtils.now().plusSeconds(ASSESSMENT_TOKEN_EXPIRE_SECONDS));

        Map<String, Object> claims = new HashMap<>();
        claims.put("assessmentId", String.valueOf(assessmentId));
        claims.put("candidateId", String.valueOf(candidateId));
        claims.put("email", email);
        claims.put("type", "ASSESSMENT");

        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(candidateId))
                .issuedAt(now)
                .expiration(expiration)
                .issuer(Constants.JWT_ISSUER)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 验证并解析测评 Token。
     *
     * @param secretKey JWT 签名密钥
     * @param token     测评 token
     * @return 解析后的 Claims，token 无效时返回 null
     */
    public static Claims parseAssessmentToken(SecretKey secretKey, String token) {
        try {
            Claims claims = JwtUtil.parseToken(secretKey, token);
            if (!"ASSESSMENT".equals(claims.get("type", String.class))) {
                log.warn("Token 类型不正确: expected=ASSESSMENT");
                return null;
            }
            return claims;
        } catch (Exception e) {
            log.warn("测评 Token 解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 构建完整的测评访问 URL。
     *
     * @param baseUrl 系统前端地址
     * @param token   测评 token
     * @return 完整测评链接
     */
    public static String buildAssessmentUrl(String baseUrl, String token) {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:3000";
        }
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/assessment?token=" + token;
    }
}
