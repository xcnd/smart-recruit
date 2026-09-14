package com.smartrecruit.common.util;

import java.util.regex.Pattern;

/**
 * 扩展的字符串工具方法，作为 {@link cn.hutool.core.util.StrUtil} 的补充。
 *
 * <p>使用 Java 25 模块导入声明来引入常用依赖。</p>
 *
 * @since 1.0.0
 */
public final class StringUtils {

    /** 中国手机号码正则。 */
    private static final Pattern MOBILE_PATTERN =
            Pattern.compile("^1[3-9]\\d{9}$");

    /** 电子邮件正则（RFC 5322 简化版）。 */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    /** 中国身份证号码正则（18位）。 */
    private static final Pattern ID_CARD_PATTERN =
            Pattern.compile("^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$");

    private StringUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ================================================================
    // Null 安全的操作
    // ================================================================

    /**
     * 如果给定的 CharSequence 为 null 或长度为 0，则返回 {@code true}。
     */
    public static boolean isEmpty(CharSequence cs) {
        return cs == null || cs.isEmpty();
    }

    /**
     * 如果 CharSequence 不为 null 且长度大于 0，则返回 {@code true}。
     */
    public static boolean isNotEmpty(CharSequence cs) {
        return !isEmpty(cs);
    }

    /**
     * 如果 CharSequence 为 null、空字符串或仅包含空白字符，则返回 {@code true}。
     */
    public static boolean isBlank(CharSequence cs) {
        if (cs == null || cs.isEmpty()) {
            return true;
        }
        for (int i = 0; i < cs.length(); i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 如果 CharSequence 不为 null 且包含非空白内容，则返回 {@code true}。
     */
    public static boolean isNotBlank(CharSequence cs) {
        return !isBlank(cs);
    }

    // ================================================================
    // 验证
    // ================================================================

    /**
     * 验证中国手机号码。
     */
    public static boolean isMobile(String mobile) {
        return isNotBlank(mobile) && MOBILE_PATTERN.matcher(mobile).matches();
    }

    /**
     * 验证电子邮件地址。
     */
    public static boolean isEmail(String email) {
        return isNotBlank(email) && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * 验证中国 18 位身份证号码。
     */
    public static boolean isIdCard(String idCard) {
        return isNotBlank(idCard) && ID_CARD_PATTERN.matcher(idCard).matches();
    }

    // ================================================================
    // 脱敏
    // ================================================================

    /**
     * 对手机号码进行脱敏处理：138****5678
     */
    public static String maskMobile(String mobile) {
        if (isEmpty(mobile) || mobile.length() < 7) {
            return mobile;
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(7);
    }

    /**
     * 对电子邮件地址进行脱敏处理：t***@example.com
     */
    public static String maskEmail(String email) {
        if (isEmpty(email) || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf('@');
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (localPart.length() <= 2) {
            return localPart.charAt(0) + "***" + domain;
        }
        return localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1) + domain;
    }

    /**
     * 对身份证号码进行脱敏处理：3201**********1234
     */
    public static String maskIdCard(String idCard) {
        if (isEmpty(idCard) || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 4);
    }

    // ================================================================
    // 大小写转换
    // ================================================================

    /**
     * 将 camelCase 转换为 snake_case。
     */
    public static String camelToSnake(String camelCase) {
        if (isEmpty(camelCase)) {
            return camelCase;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 将 snake_case 转换为 camelCase。
     */
    public static String snakeToCamel(String snakeCase) {
        if (isEmpty(snakeCase)) {
            return snakeCase;
        }
        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = false;
        for (int i = 0; i < snakeCase.length(); i++) {
            char c = snakeCase.charAt(i);
            if (c == '_') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    /**
     * 将字符串的首字符转换为大写。
     */
    public static String capitalize(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
