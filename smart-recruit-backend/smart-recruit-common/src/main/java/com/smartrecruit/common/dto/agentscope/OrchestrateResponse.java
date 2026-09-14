package com.smartrecruit.common.dto.agentscope;

import lombok.Builder;
import lombok.Data;

/**
 * AgentScope2 面试编排响应。
 *
 * @since 2.0.0
 */
@Data
@Builder
public class OrchestrateResponse {
    private String sessionId;
    private String content;
    private String stages;
    private String error;

    public static OrchestrateResponse ok(String sessionId, String content, String stages) {
        return OrchestrateResponse.builder()
                .sessionId(sessionId).content(content).stages(stages).build();
    }

    public static OrchestrateResponse error(String sessionId, String errorMessage) {
        return OrchestrateResponse.builder().sessionId(sessionId).error(errorMessage).build();
    }
}
