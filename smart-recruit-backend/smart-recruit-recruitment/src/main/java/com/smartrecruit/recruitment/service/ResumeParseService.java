package com.smartrecruit.recruitment.service;

import java.util.Map;

/**
 * 异步简历解析编排服务。
 *
 * <p>负责协调文本提取 → LLM 结构化 → 落库的完整流水线，以异步方式执行。
 * 接收字节数组而非 {@code MultipartFile}，确保异步线程安全。</p>
 *
 * @since 1.0.0
 */
public interface ResumeParseService {

    /**
     * 异步触发简历解析流水线。
     *
     * <p>方法立即返回，实际解析在后台线程中执行。
     * 状态流转：PENDING → PARSING → SUCCESS/FAILED。</p>
     *
     * @param resumeId 简历 ID
     * @param fileBytes 文件字节数组（已从上传线程拷贝，异步安全）
     * @param fileName  原始文件名（用于 Tika 类型检测和日志）
     */
    void parseAsync(Long resumeId, byte[] fileBytes, String fileName);

    /**
     * 同步解析简历文件（供「全流程编排」等场景复用，不创建简历/候选人记录）。
     *
     * <p>文本型走文档解析 + 结构化（含 AI 增强），图片/扫描件走 AI 视觉解析；
     * 返回提取的原始文本与结构化结果。</p>
     *
     * @param fileBytes 文件字节
     * @param fileName  文件名（用于判断类型）
     * @return {@code {"rawText": 提取文本, "parsed": ParsedResume}}
     */
    Map<String, Object> parseFileForPipeline(byte[] fileBytes, String fileName);
}
