package com.smartrecruit.common.util;

import java.util.Collection;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 通用的输入校验工具类。
 *
 * @since 1.0.0
 */
public final class ValidationUtils {

    private static final Pattern SAFE_SQL_PATTERN =
            Pattern.compile("(?i)(drop\\s+table|truncate\\s+table|delete\\s+from|insert\\s+into|update\\s+.*set|exec\\s|execute\\s|script)", Pattern.CASE_INSENSITIVE);

    private static final Pattern XSS_PATTERN =
            Pattern.compile("(?i)(<script>|</script>|<iframe|javascript:|onerror=|onload=)", Pattern.CASE_INSENSITIVE);

    private ValidationUtils() {
        throw new UnsupportedOperationException("工具类不能被实例化");
    }

    // ================================================================
    // 空值检查
    // ================================================================

    /**
     * 检查指定的对象引用不为 null。
     *
     * @throws IllegalArgumentException 如果 obj 为 null
     */
    public static <T> T requireNonNull(T obj, String message) {
        if (obj == null) {
            throw new IllegalArgumentException(message);
        }
        return obj;
    }

    /**
     * 检查指定的集合不为 null 且不为空。
     *
     * @throws IllegalArgumentException 如果集合为 null 或为空
     */
    public static <T extends Collection<?>> T requireNotEmpty(T collection, String message) {
        if (collection == null || collection.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return collection;
    }

    // ================================================================
    // 范围检查
    // ================================================================

    /**
     * 检查值是否在 [min, max] 闭区间内。
     */
    public static int requireInRange(int value, int min, int max, String fieldName) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                    fieldName + " 必须在 " + min + " 到 " + max + " 之间，但当前值为 " + value);
        }
        return value;
    }

    /**
     * 检查字符串长度是否在 [min, max] 闭区间内。
     */
    public static String requireLength(String value, int min, int max, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " 不能为 null");
        }
        if (value.length() < min || value.length() > max) {
            throw new IllegalArgumentException(
                    fieldName + " 长度必须在 " + min + " 到 " + max + " 之间，但当前长度为 " + value.length());
        }
        return value;
    }

    // ================================================================
    // 分页参数规范化
    // ================================================================

    /**
     * 规范化页码（最小值为 1）。
     */
    public static long normalizePage(long page) {
        return page < 1 ? 1 : page;
    }

    /**
     * 规范化每页大小（1-100）。
     */
    public static long normalizePageSize(long size) {
        if (size < 1) return 10;
        if (size > 100) return 100;
        return size;
    }

    // ================================================================
    // 安全检查
    // ================================================================

    /**
     * 检查输入是否包含潜在的 SQL 注入模式。
     */
    public static boolean containsSqlInjection(String input) {
        if (StringUtils.isBlank(input)) {
            return false;
        }
        return SAFE_SQL_PATTERN.matcher(input).find();
    }

    /**
     * 检查输入是否包含潜在的 XSS 攻击模式。
     */
    public static boolean containsXss(String input) {
        if (StringUtils.isBlank(input)) {
            return false;
        }
        return XSS_PATTERN.matcher(input).find();
    }

    /**
     * 净化用户输入，去除潜在的 XSS 内容。
     */
    public static String sanitize(String input) {
        if (StringUtils.isBlank(input)) {
            return input;
        }
        return input
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    // ================================================================
    // ID 检查
    // ================================================================

    /**
     * 检查给定的 ID 是否为有效的正整数（Long 类型）。
     */
    public static boolean isValidId(Long id) {
        return id != null && id > 0;
    }

    /**
     * 检查给定的 ID 字符串是否为数字且为正数。
     */
    public static boolean isValidIdString(String id) {
        if (StringUtils.isBlank(id)) {
            return false;
        }
        try {
            return Long.parseLong(id.trim()) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 验证两个对象是否相等。
     */
    public static boolean isEqual(Object a, Object b) {
        return Objects.equals(a, b);
    }
}
