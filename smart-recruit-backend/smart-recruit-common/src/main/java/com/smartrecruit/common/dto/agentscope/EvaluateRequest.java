package com.smartrecruit.common.dto.agentscope;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AgentScope2 面试评估请求。
 *
 * @since 2.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluateRequest {
    @JsonSetter(nulls = Nulls.SKIP)
    private String candidateName = "未知";
    @JsonSetter(nulls = Nulls.SKIP)
    private String jobTitle = "未知";
    @JsonSetter(nulls = Nulls.SKIP)
    private String interviewContent = "";
    @JsonSetter(nulls = Nulls.SKIP)
    private String sessionId = "default";
}
