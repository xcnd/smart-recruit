package com.smartrecruit.interview.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 大模型开关配置。
 *
 * <p>LLM 调用已迁移至 smart-recruit-ai-engine 服务，
 * 此处仅保留 {@code enabled} 开关用于本模块降级控制。</p>
 *
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.llm")
public class LlmProperties {

    /** 是否启用大模型，{@code false} 时降级为启发式评分/仅查库。 */
    private boolean enabled;
}
