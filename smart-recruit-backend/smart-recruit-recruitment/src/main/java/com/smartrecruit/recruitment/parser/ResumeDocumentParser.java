package com.smartrecruit.recruitment.parser;

/**
 * 简历文档解析器接口 — 每种文档格式（PDF、Word）各有一个实现。
 *
 * <p>每个实现类负责自己格式的多策略文本提取（主引擎+回退）、头像提取、
 * 以及结构诊断。{@link com.smartrecruit.recruitment.service.impl.DocumentParserServiceImpl}
 * 作为门面，根据内容类型匹配合适的解析器并委托调用。</p>
 *
 * @since 1.0.0
 */
public interface ResumeDocumentParser {

    /**
     * 判断是否能解析该文件。
     *
     * @param contentType Tika 检测到的内容类型（如 application/pdf）
     * @param fileName    原始文件名（用于扩展名兜底判断）
     * @return true 表示该解析器可以处理此文件
     */
    boolean supports(String contentType, String fileName);

    /**
     * 从文件字节数组中提取纯文本。
     *
     * @param fileBytes 文件字节数组
     * @param fileName  原始文件名（用于日志）
     * @return 提取的纯文本，失败返回空字符串
     */
    String extractText(byte[] fileBytes, String fileName);

    /**
     * 从文件中提取头像/照片。
     *
     * @param fileBytes 文件字节数组
     * @param fileName  原始文件名（用于日志）
     * @return JPEG 字节数组，未找到或失败返回 null
     */
    byte[] extractProfileImage(byte[] fileBytes, String fileName);
}
