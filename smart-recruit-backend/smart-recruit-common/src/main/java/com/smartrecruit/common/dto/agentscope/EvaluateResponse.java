package com.smartrecruit.common.dto.agentscope;

import lombok.Builder;
import lombok.Data;

/**
 * AgentScope2 面试评估响应。
 *
 * @since 2.0.0
 */
@Data
@Builder
public class EvaluateResponse {
    private String sessionId;
    private String content;
    private String error;

    public static EvaluateResponse ok(String sessionId, String content) {
        return EvaluateResponse.builder().sessionId(sessionId).content(content).build();
    }

    public static EvaluateResponse error(String sessionId, String errorMessage) {
        return EvaluateResponse.builder().sessionId(sessionId).error(errorMessage).build();
    }
}
