package com.smartrecruit.common.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * 基于 {@code java.time} API 的日期时间工具类。
 *
 * <p>所有与旧版 {@link Date} 的转换均以 {@code Instant} 作为桥接。</p>
 *
 * @since 1.0.0
 */
public final class DateUtils {

    /** 平台的默认上海时区。 */
    public static final ZoneId ZONE_SHANGHAI = ZoneId.of("Asia/Shanghai");

    /** 标准日期格式化器：yyyy-MM-dd */
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 标准日期时间格式化器：yyyy-MM-dd HH:mm:ss */
    public static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 标准日期时间（分钟精度）格式化器：yyyy-MM-dd HH:mm */
    public static final DateTimeFormatter DATETIME_MINUTE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 标准时间格式化器：HH:mm */
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /** 标准时间（秒精度）格式化器：HH:mm:ss */
    public static final DateTimeFormatter TIME_SECONDS_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    /** 紧凑日期时间格式化器：yyyyMMddHHmmss */
    public static final DateTimeFormatter COMPACT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 紧凑日期格式化器：yyyyMMdd */
    public static final DateTimeFormatter COMPACT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 紧凑月份格式化器：yyyyMM */
    public static final DateTimeFormatter COMPACT_MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMM");

    /** 斜杠月份格式化器：yyyy/MM */
    public static final DateTimeFormatter SLASH_MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy/MM");

    /** 斜杠日期格式化器：yyyy/MM/dd */
    public static final DateTimeFormatter SLASH_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /** 点号日期格式化器：yyyy.MM.dd */
    public static final DateTimeFormatter DOT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd");

    /** 中文日期格式化器：yyyy年MM月dd日 */
    public static final DateTimeFormatter CN_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy年MM月dd日");

    /** ISO 8601 格式化器 */
    public static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private DateUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ================================================================
    // 格式化
    // ================================================================

    /**
     * 将 LocalDateTime 格式化为 "yyyy-MM-dd HH:mm:ss"。
     */
    public static String format(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMATTER) : null;
    }

    /**
     * 将 LocalDate 格式化为 "yyyy-MM-dd"。
     */
    public static String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : null;
    }

    /**
     * 将 LocalDateTime 格式化为紧凑字符串 "yyyyMMddHHmmss"。
     */
    public static String formatCompact(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(COMPACT_FORMATTER) : null;
    }

    /**
     * 将 LocalDateTime 格式化为 "yyyy-MM-dd HH:mm"。
     */
    public static String formatDateTimeMinute(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_MINUTE_FORMATTER) : null;
    }

    /**
     * 将 LocalDateTime 格式化为 "HH:mm"。
     */
    public static String formatTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(TIME_FORMATTER) : null;
    }

    /**
     * 将 LocalTime 格式化为 "HH:mm"。
     */
    public static String formatTime(LocalTime time) {
        return time != null ? time.format(TIME_FORMATTER) : null;
    }

    /**
     * 将 LocalDate 格式化为 "yyyyMMdd"。
     */
    public static String formatCompactDate(LocalDate date) {
        return date != null ? date.format(COMPACT_DATE_FORMATTER) : null;
    }

    /**
     * 将 LocalDate 格式化为 "yyyyMM"。
     */
    public static String formatCompactMonth(LocalDate date) {
        return date != null ? date.format(COMPACT_MONTH_FORMATTER) : null;
    }

    /**
     * 将 LocalDateTime 格式化为 "yyyy/MM"。
     */
    public static String formatSlashMonth(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(SLASH_MONTH_FORMATTER) : null;
    }

    /**
     * 将 LocalDate 格式化为 "yyyy/MM"。
     */
    public static String formatSlashMonth(LocalDate date) {
        return date != null ? date.format(SLASH_MONTH_FORMATTER) : null;
    }

    /**
     * 将 LocalDate 格式化为 "yyyy/MM/dd"。
     */
    public static String formatSlashDate(LocalDate date) {
        return date != null ? date.format(SLASH_DATE_FORMATTER) : null;
    }

    /**
     * 将 LocalDate 格式化为 "yyyy年MM月dd日"。
     */
    public static String formatCnDate(LocalDate date) {
        return date != null ? date.format(CN_DATE_FORMATTER) : null;
    }

    // ================================================================
    // 解析
    // ================================================================

    /**
     * 将 "yyyy-MM-dd HH:mm:ss" 解析为 LocalDateTime。
     *
     * <p>入参两侧的空白字符会被忽略，空值或空白字符串返回 {@code null}。</p>
     */
    public static LocalDateTime parse(String dateTimeStr) {
        if (StringUtils.isBlank(dateTimeStr)) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr.trim(), DATETIME_FORMATTER);
    }

    /**
     * 将 "yyyy-MM-dd" 解析为 LocalDate。
     *
     * <p>入参两侧的空白字符会被忽略，空值或空白字符串返回 {@code null}。</p>
     */
    public static LocalDate parseDate(String dateStr) {
        if (StringUtils.isBlank(dateStr)) {
            return null;
        }
        return LocalDate.parse(dateStr.trim(), DATE_FORMATTER);
    }

    /**
     * 将 ISO 8601 字符串解析为 LocalDateTime。
     *
     * <p>入参两侧的空白字符会被忽略，空值或空白字符串返回 {@code null}。</p>
     */
    public static LocalDateTime parseIso(String isoStr) {
        if (StringUtils.isBlank(isoStr)) {
            return null;
        }
        return LocalDateTime.parse(isoStr.trim(), ISO_FORMATTER);
    }

    // ================================================================
    // 转换
    // ================================================================

    /**
     * 将旧版 {@link Date} 转换为 {@link LocalDateTime}（系统时区）。
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        if (date == null) return null;
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * 将旧版 {@link Date} 转换为 {@link LocalDateTime}（上海时区）。
     */
    public static LocalDateTime toLocalDateTimeShanghai(Date date) {
        if (date == null) return null;
        return date.toInstant().atZone(ZONE_SHANGHAI).toLocalDateTime();
    }

    /**
     * 将 {@link LocalDateTime} 转换为旧版 {@link Date}（系统时区）。
     */
    public static Date toDate(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 将 {@link LocalDateTime} 转换为纪元毫秒值。
     */
    public static long toEpochMillis(LocalDateTime dateTime) {
        if (dateTime == null) return 0;
        return dateTime.atZone(ZONE_SHANGHAI).toInstant().toEpochMilli();
    }

    /**
     * 将纪元毫秒值转换为 {@link LocalDateTime}（上海时区）。
     */
    public static LocalDateTime fromEpochMillis(long millis) {
        return Instant.ofEpochMilli(millis).atZone(ZONE_SHANGHAI).toLocalDateTime();
    }

    // ================================================================
    // 日期边界（上海时区）
    // ================================================================

    /**
     * 返回上海时区今天的起始时间。
     */
    public static LocalDateTime beginOfDay() {
        return LocalDate.now(ZONE_SHANGHAI).atStartOfDay();
    }

    /**
     * 返回上海时区今天的结束时间。
     */
    public static LocalDateTime endOfDay() {
        return LocalDate.now(ZONE_SHANGHAI).atTime(LocalTime.MAX);
    }

    /**
     * 返回指定日期的起始时间。
     */
    public static LocalDateTime beginOfDay(LocalDate date) {
        return date != null ? date.atStartOfDay() : null;
    }

    /**
     * 返回指定日期的结束时间。
     */
    public static LocalDateTime endOfDay(LocalDate date) {
        return date != null ? date.atTime(LocalTime.MAX) : null;
    }

    // ================================================================
    // 计算
    // ================================================================

    /**
     * 返回两个日期之间的年数。
     */
    public static long yearsBetween(LocalDate from, LocalDate to) {
        return ChronoUnit.YEARS.between(from, to);
    }

    /**
     * 返回两个日期之间的天数。
     */
    public static long daysBetween(LocalDate from, LocalDate to) {
        return ChronoUnit.DAYS.between(from, to);
    }

    /**
     * 返回两个日期时间之间的天数。
     */
    public static long daysBetween(LocalDateTime from, LocalDateTime to) {
        return ChronoUnit.DAYS.between(from, to);
    }

    /**
     * 返回两个日期时间之间的小时数。
     */
    public static long hoursBetween(LocalDateTime from, LocalDateTime to) {
        return ChronoUnit.HOURS.between(from, to);
    }

    /**
     * 根据出生日期计算年龄（上海时区）。
     */
    public static int calculateAge(LocalDate birthDate) {
        if (birthDate == null) return 0;
        return (int) ChronoUnit.YEARS.between(birthDate, LocalDate.now(ZONE_SHANGHAI));
    }

    // ================================================================
    // 比较
    // ================================================================

    /**
     * 判断一个日期时间是否早于另一个。
     */
    public static boolean isBefore(LocalDateTime first, LocalDateTime second) {
        return first != null && second != null && first.isBefore(second);
    }

    /**
     * 判断一个日期时间是否晚于另一个。
     */
    public static boolean isAfter(LocalDateTime first, LocalDateTime second) {
        return first != null && second != null && first.isAfter(second);
    }

    /**
     * 判断两个日期时间是否为同一天（上海时区）。
     */
    public static boolean isSameDay(LocalDateTime first, LocalDateTime second) {
        if (first == null || second == null) return false;
        ZonedDateTime z1 = first.atZone(ZONE_SHANGHAI);
        ZonedDateTime z2 = second.atZone(ZONE_SHANGHAI);
        return z1.toLocalDate().equals(z2.toLocalDate());
    }

    // ================================================================
    // 当前时间
    // ================================================================

    /**
     * 返回上海时区的当前 LocalDateTime。
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE_SHANGHAI);
    }

    /**
     * 返回上海时区的当前 LocalDate。
     */
    public static LocalDate today() {
        return LocalDate.now(ZONE_SHANGHAI);
    }

    /**
     * 返回上海时区当前的年月。
     */
    public static YearMonth currentYearMonth() {
        return YearMonth.now(ZONE_SHANGHAI);
    }

    /**
     * 返回当前时刻的 Instant（UTC 瞬时点）。
     */
    public static Instant currentInstant() {
        return Instant.now();
    }

    /**
     * 返回当前的纪元毫秒值。
     */
    public static long currentEpochMillis() {
        return Instant.now().toEpochMilli();
    }
}
