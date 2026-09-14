package com.smartrecruit.referral.feign;

import com.smartrecruit.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.Map;

/**
 * AI 引擎 Agent 能力网关客户端（内推模块）。
 *
 * @since 2026-04-06
 */
@HttpExchange("/api/v1/agent-capabilities")
public interface AiAgentCapabilityClient {

    /** 内推职位匹配推荐。 */
    @PostExchange("/referral/match")
    ApiResponse<Map<String, Object>> matchReferral(@RequestBody Map<String, Object> request);
}
