package com.smartrecruit.aiengine.service;

/**
 * 简历解析调用日志记录器，落库 {@code ai_resume_parse_log}。
 *
 * <p>LLM 网关每完成一次简历解析调用（文本增强或视觉识别）即写入一行，
 * 记录实际使用的厂商/模型、真实 Token 用量与耗时，用于解析质量与成本分析。</p>
 *
 * @since 2026-09-21
 */
public interface ResumeParseLogRecorder {

    /**
     * 记录一次简历解析调用。
     *
     * @param resumeId     简历 ID，为 {@code null} 时不记录（无法归属到具体简历）
     * @param engine       提供商代号，如 qwen、deepseek
     * @param model        实际调用的模型名，如 qwen-vl-max
     * @param inputTokens  输入 Token 数，可为 {@code null}
     * @param outputTokens 输出 Token 数，可为 {@code null}
     * @param durationMs   调用耗时（毫秒）
     * @param success      本次调用是否成功
     * @param errorMsg     失败原因，成功时为 {@code null}
     */
    void record(Long resumeId, String engine, String model,
                Integer inputTokens, Integer outputTokens,
                long durationMs, boolean success, String errorMsg);
}
