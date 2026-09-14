package com.smartrecruit.aiengine.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;

import java.util.Map;

/**
 * 配置 AI 智能体参数请求。
 *
 * <p>各智能体的可配置项不同，请求体为键值映射，
 * 通过 {@link JsonCreator} 直接反序列化为 {@code config} 字段，保持线上 JSON 格式不变。</p>
 *
 * @since 2026-04-07
 */
@Data
public class ConfigureAgentRequest {

    /** 智能体配置项（键值映射，键为配置名）。 */
    private final Map<String, Object> config;

    @JsonCreator
    public ConfigureAgentRequest(Map<String, Object> config) {
        this.config = config;
    }
}
