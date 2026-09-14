package com.smartrecruit.common.util;

import jakarta.servlet.http.HttpServletRequest;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 安全工具类。
 *
 * <p>提供客户端 IP 提取、User-Agent 解析、浏览器和操作系统检测、
 * HTML 转义、SQL 注入/XSS 攻击检测、输入清理等安全相关的工具方法。
 *
 * @author xdh
 * @since 1.0.0
 */
public final class SecurityUtil {

    private SecurityUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 获取客户端真实 IP 地址。
     *
     * <p>按优先级尝试从代理头获取：X-Forwarded-For → X-Real-IP → 常见代理头 →
     * 直接连接的远程地址。X-Forwarded-For 可能包含代理链，取第一个 IP；
     * 值为空或 "unknown" 时跳过，适用于 Nginx/网关代理场景。</p>
     *
     * @param request 当前 HTTP 请求，可为 {@code null}
     * @return 客户端真实 IP；无法获取时返回 "unknown"
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        return getClientIpAddress(request::getHeader, request::getRemoteAddr);
    }

    /**
     * 获取客户端真实 IP 地址（适配非 Servlet 场景，如 WebFlux 网关过滤器）。
     *
     * <p>解析规则与 {@link #getClientIpAddress(HttpServletRequest)} 完全一致：
     * X-Forwarded-For → X-Real-IP → 常见代理头 → 远程地址。</p>
     *
     * @param headerGetter       按 header 名取值（如 {@code name -> exchange.getRequest().getHeaders().getFirst(name)}）
     * @param remoteAddrSupplier 提供直接连接的远程地址，可为 {@code null}
     * @return 客户端真实 IP；无法获取时返回 "unknown"
     */
    public static String getClientIpAddress(Function<String, String> headerGetter,
                                            Supplier<String> remoteAddrSupplier) {
        String ip = firstValid(headerGetter, "X-Forwarded-For");
        if (invalidIp(ip)) {
            ip = firstValid(headerGetter, "X-Real-IP");
        }
        if (invalidIp(ip)) {
            ip = firstValid(headerGetter, "Proxy-Client-IP");
        }
        if (invalidIp(ip)) {
            ip = firstValid(headerGetter, "WL-Proxy-Client-IP");
        }
        if (invalidIp(ip)) {
            ip = firstValid(headerGetter, "HTTP_CLIENT_IP");
        }
        if (invalidIp(ip)) {
            ip = firstValid(headerGetter, "HTTP_X_FORWARDED_FOR");
        }
        if (invalidIp(ip) && remoteAddrSupplier != null) {
            ip = remoteAddrSupplier.get();
        }
        return ip != null && !ip.isBlank() ? ip.trim() : "unknown";
    }

    /**
     * 从指定 header 取值，代理链（逗号分隔）取第一个 IP，并过滤空值/unknown。
     */
    private static String firstValid(Function<String, String> headerGetter, String name) {
        if (headerGetter == null) {
            return null;
        }
        String value = headerGetter.apply(name);
        if (invalidIp(value)) {
            return null;
        }
        if (value.contains(",")) {
            return value.split(",")[0].trim();
        }
        return value.trim();
    }

    /** IP 值是否无效（空、空白或 "unknown"）。 */
    private static boolean invalidIp(String ip) {
        return ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip);
    }

    public static String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    /**
     * 获取浏览器类型。
     */
    public static String getBrowserType(HttpServletRequest request) {
        String userAgent = getUserAgent(request);
        if (userAgent == null) {
            return "Unknown";
        }

        if (userAgent.contains("Chrome")) return "Chrome";
        if (userAgent.contains("Firefox")) return "Firefox";
        if (userAgent.contains("Safari")) return "Safari";
        if (userAgent.contains("Edge")) return "Edge";
        if (userAgent.contains("Opera")) return "Opera";
        if (userAgent.contains("MSIE") || userAgent.contains("Trident")) return "Internet Explorer";
        return "Unknown";
    }

    /**
     * 获取操作系统类型。
     */
    public static String getOperatingSystem(HttpServletRequest request) {
        String userAgent = getUserAgent(request);
        if (userAgent == null) {
            return "Unknown";
        }

        if (userAgent.contains("Windows")) return "Windows";
        if (userAgent.contains("Mac")) return "MacOS";
        if (userAgent.contains("Linux")) return "Linux";
        if (userAgent.contains("Android")) return "Android";
        if (userAgent.contains("iPhone") || userAgent.contains("iPad") || userAgent.contains("iPod")) return "iOS";
        return "Unknown";
    }

    /**
     * HTML 字符转义，防止 XSS 攻击。
     */
    public static String escapeHtml(String input) {
        if (input == null) return null;
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    /**
     * 检查输入是否包含 SQL 注入关键词。
     */
    public static boolean containsSqlInjection(String input) {
        if (input == null) return false;

        String lowerInput = input.toLowerCase();
        String[] sqlKeywords = {
                "select ", "insert ", "update ", "delete ", "drop ",
                "union ", "exec ", "execute ", "script", "javascript",
                "--", "/*", "*/"
        };

        for (String keyword : sqlKeywords) {
            if (lowerInput.contains(keyword)) return true;
        }
        return false;
    }

    /**
     * 检查输入是否包含 XSS 攻击模式。
     */
    public static boolean containsXssAttack(String input) {
        if (input == null) return false;

        String lowerInput = input.toLowerCase();
        String[] xssPatterns = {
                "<script", "</script>", "javascript:", "onerror=", "onload=",
                "onclick=", "<iframe", "</iframe>", "<object", "</object>", "<embed"
        };

        for (String pattern : xssPatterns) {
            if (lowerInput.contains(pattern)) return true;
        }
        return false;
    }

    /**
     * 清理输入字符串，移除危险字符。
     */
    public static String sanitizeInput(String input) {
        if (input == null) return null;
        return input.replaceAll("[<>\"'']", "")
                .replaceAll("[/\\\\*]", "")
                .trim();
    }
}
