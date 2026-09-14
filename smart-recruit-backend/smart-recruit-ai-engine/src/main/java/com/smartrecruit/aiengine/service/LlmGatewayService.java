package com.smartrecruit.aiengine.service;

import java.util.List;
import java.util.Map;

/**
 * AI Engine 大模型网关服务，基于 LangChain4j 实现多厂商路由与自动降级。
 *
 * @since 1.0.0
 */
public interface LlmGatewayService {
    /**
     * 发起一次 LLM 对话：优先调用主提供商，失败自动降级到备用提供商。
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @return 解析后的 JSON 响应（Map）
     */
    Map<String, Object> chat(String systemPrompt, String userPrompt);

    /**
     * 发起一次 LLM 对话，按 Agent 路由模型（未配置路由时回退全局主/备链路）。
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @param agentId      Agent ID（如 offer-predictor），可为 {@code null}
     * @return 解析后的 JSON 响应（Map）
     */
    Map<String, Object> chat(String systemPrompt, String userPrompt, String agentId);

    /**
     * 发起一次 LLM 对话并返回模型原始输出文本（不做 JSON 解析）。
     *
     * <p>供 AgentScope2 等编排框架使用：模型可能返回工具调用、Markdown 或
     * 自由文本等非 JSON 内容，JSON 化解析会破坏这类场景。</p>
     */
    String chatRaw(String systemPrompt, String userPrompt);

    /**
     * 发起一次 LLM 对话并返回原始输出文本，按 Agent 路由模型。
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @param agentId      Agent ID（如 interview-evaluator），可为 {@code null}
     * @return 模型原始输出文本
     */
    String chatRaw(String systemPrompt, String userPrompt, String agentId);

    /**
     * 发起一次多模态（图片）LLM 对话，按 Agent 路由视觉模型。
     *
     * <p>用于图片/扫描件简历解析：图片以 base64 Data URL 传入，
     * 模型需支持图像输入（如 qwen-vl-max）。</p>
     *
     * @param systemPrompt 系统提示词
     * @param textPrompt   文本提示词
     * @param base64Images 图片列表（每项为 base64 Data URL，如 {@code data:image/png;base64,...}），
     *                     用于 PDF 多页渲染或多图场景
     * @param agentId          统计归属的 Agent ID（如 resume-parser）
     * @param routingAgentId   模型路由的 Agent ID（如 resume-parser-image）
     * @return 解析后的 JSON 响应（Map）
     */
    Map<String, Object> chatWithImage(String systemPrompt, String textPrompt,
                                      List<String> base64Images, String agentId, String routingAgentId);
}
