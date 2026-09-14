package com.smartrecruit.common.config;

import com.smartrecruit.common.util.DateUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalTimeSerializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * Jackson 3 JSON 配置（Spring Boot 4.x 默认使用 {@code tools.jackson}）。
 *
 * <p>通过 {@link JsonMapperBuilderCustomizer} Hook Spring Boot 自动配置的
 * JsonMapper，该 Mapper 同时用于 HTTP 消息转换器和手动注入点
 * （如 {@code CustomAuthenticationEntryPoint}、{@code CustomAccessDeniedHandler}）。</p>
 *
 * @since 1.0.0
 */
@AutoConfiguration
public class JacksonConfig {

    private static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String ASIA_SHANGHAI = "Asia/Shanghai";

    @Bean
    public JsonMapperBuilderCustomizer jsonMapperBuilderCustomizer() {
        return this::configureBuilder;
    }

    private void configureBuilder(MapperBuilder<?, ?> builder) {
        // Long/BigInteger → String（防止 JS Number 精度丢失）
        // 注意：仅序列化包装类型 Long.class，不序列化基本类型 long.class。
        // 基本类型 long 用于分页计数等场景，不存在精度问题且前端期望 Number 类型。
        SimpleModule longModule = new SimpleModule("LongToStringModule");
        longModule.addSerializer(Long.class, ToStringSerializer.instance);
        longModule.addSerializer(BigInteger.class, ToStringSerializer.instance);
        builder.addModule(longModule);

        // Java 8 时间模块 — 项目统一日期格式 yyyy-MM-dd HH:mm:ss
        DateTimeFormatter dtf = DateUtils.DATETIME_FORMATTER;
        SimpleModule jtm = new SimpleModule("JavaTimeModule");
        jtm.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dtf));
        jtm.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dtf));
        jtm.addSerializer(LocalDate.class,
                new LocalDateSerializer(DateUtils.DATE_FORMATTER));
        jtm.addDeserializer(LocalDate.class,
                new LocalDateDeserializer(DateUtils.DATE_FORMATTER));
        jtm.addSerializer(LocalTime.class,
                new LocalTimeSerializer(DateUtils.TIME_SECONDS_FORMATTER));
        jtm.addDeserializer(LocalTime.class,
                new LocalTimeDeserializer(DateUtils.TIME_SECONDS_FORMATTER));
        builder.addModule(jtm);

        // 全局配置
        builder.changeDefaultPropertyInclusion(
                inc -> inc.withValueInclusion(JsonInclude.Include.NON_NULL));
        builder.defaultDateFormat(new SimpleDateFormat(DATETIME_FORMAT));
        builder.defaultTimeZone(TimeZone.getTimeZone(ASIA_SHANGHAI));
        builder.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
