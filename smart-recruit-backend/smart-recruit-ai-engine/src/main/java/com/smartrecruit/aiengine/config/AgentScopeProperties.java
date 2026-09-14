package com.smartrecruit.aiengine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AgentScope2 多智能体框架配置属性。
 *
 * <p>控制 AgentScope2 的启用/关闭及各个子 Agent 的独立开关，
 * 支持逐步迁移策略：未启用的 Agent 继续使用原有的模拟实现。</p>
 *
 * @since 2.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "agentscope")
public class AgentScopeProperties {

    /** 是否启用 AgentScope2 框架，默认 false 保持向后兼容。 */
    private boolean enabled = false;

    /** 工作区配置。 */
    private WorkspaceConfig workspace = new WorkspaceConfig();

    /** 子 Agent 开关配置，key 为 Agent 标识名。 */
    private Map<String, SubAgentConfig> subAgents = new LinkedHashMap<>();

    @Data
    public static class WorkspaceConfig {
        /** 工作区路径，相对于 user.dir。 */
        private String path = ".agentscope/workspace";
    }

    @Data
    public static class SubAgentConfig {
        /** 是否启用该子 Agent 的 AgentScope2 版本。 */
        private boolean enabled = false;
    }

    /**
     * 判断指定子 Agent 的 AgentScope2 版本是否启用。
     *
     * @param agentKey 子 Agent 标识名
     * @return 当框架全局启用且该子 Agent 单独启用时返回 true
     */
    public boolean isSubAgentEnabled(String agentKey) {
        if (!enabled) return false;
        SubAgentConfig config = subAgents.get(agentKey);
        return config != null && config.isEnabled();
    }
}
