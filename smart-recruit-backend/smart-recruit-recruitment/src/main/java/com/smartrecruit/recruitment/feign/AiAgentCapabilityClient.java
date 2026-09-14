package com.smartrecruit.recruitment.feign;

import com.smartrecruit.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.Map;

/**
 * AI 引擎 Agent 能力网关客户端（招聘模块）。
 *
 * @since 2026-04-06
 */
@HttpExchange("/api/v1/agent-capabilities")
public interface AiAgentCapabilityClient {

    /** 生成结构化 JD。 */
    @PostExchange("/jd/generate")
    ApiResponse<Map<String, Object>> generateJd(@RequestBody com.smartrecruit.recruitment.dto.request.JdGenerateRequest request);

    /**
     * 解析图片/扫描件简历（AI 视觉模型）。
     *
     * @param request 含 fileName 与 base64Image（base64 Data URL）
     * @return 解析结果 Map；reviewRequired=true 表示需人工复核
     */
    @PostExchange("/resume/parse-image")
    ApiResponse<Map<String, Object>> parseResumeImage(@RequestBody Map<String, Object> request);
}
