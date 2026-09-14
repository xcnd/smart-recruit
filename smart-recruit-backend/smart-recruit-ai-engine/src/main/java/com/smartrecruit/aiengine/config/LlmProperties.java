package com.smartrecruit.aiengine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 大模型对接配置属性。
 *
 * <p>支持多厂商接入（阿里云千问 / DeepSeek），通过 {@code primary} 指定主模型，
 * 主模型调用失败时自动降级到备选模型。</p>
 *
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.llm")
public class LlmProperties {

    /** 是否启用大模型，{@code false} 时 LLM 网关不可用。 */
    private boolean enabled;

    /** 主模型名称，对应 {@code providers} 下的 key。 */
    private String primary;

    /** 多厂商配置映射，key 为厂商代号（如 qwen、deepseek）。 */
    private Map<String, ProviderConfig> providers = new LinkedHashMap<>();

    /** 按 Agent 的模型路由：key=Agent ID（如 offer-predictor），value=模型名。 */
    private Map<String, String> routing = new LinkedHashMap<>();

    @Data
    public static class ProviderConfig {
        private String apiUrl;
        private String apiKey;
        private String model;
        /** 该提供商支持的其他模型名（用于按 Agent 路由时按模型名定位提供商）。 */
        private List<String> models = new ArrayList<>();
        /** 最大 token 数，默认 8192（结构化 JSON 输出需要更大空间，避免被截断） */
        private int maxTokens = 8192;
        /** 温度参数，默认 0.1（低温度以获取结构化 JSON 响应） */
        private double temperature = 0.1;
    }

    public ProviderConfig getPrimaryConfig() {
        if (primary == null || providers.isEmpty()) return null;
        return providers.get(primary);
    }

    public ProviderConfig getFallbackConfig() {
        if (primary == null || providers.size() <= 1) return null;
        return providers.values().stream()
                .filter(config -> !config.equals(getPrimaryConfig()))
                .findFirst().orElse(null);
    }

    /**
     * 按 Agent ID 解析绑定的模型名。
     *
     * @param agentId Agent ID（如 offer-predictor）
     * @return 绑定的模型名；未配置或 Agent 不存在时返回 {@code null}
     */
    public String resolveModelForAgent(String agentId) {
        if (agentId == null || agentId.isBlank() || routing == null) {
            return null;
        }
        return routing.get(agentId.trim());
    }

    /**
     * 按模型名定位所属提供商。
     *
     * <p>先匹配各提供商的默认 {@code model}，再匹配 {@code models} 支持列表。</p>
     *
     * @param modelName 模型名（如 deepseek-v4-flash）
     * @return 匹配的提供商配置；未匹配到返回 {@code null}
     */
    public ProviderConfig findProviderByModel(String modelName) {
        if (modelName == null || modelName.isBlank() || providers == null) {
            return null;
        }
        String target = modelName.trim();
        for (ProviderConfig config : providers.values()) {
            if (target.equalsIgnoreCase(config.getModel())) {
                return config;
            }
        }
        for (ProviderConfig config : providers.values()) {
            if (config.getModels() != null && config.getModels().stream()
                    .anyMatch(m -> target.equalsIgnoreCase(m))) {
                return config;
            }
        }
        return null;
    }
}
