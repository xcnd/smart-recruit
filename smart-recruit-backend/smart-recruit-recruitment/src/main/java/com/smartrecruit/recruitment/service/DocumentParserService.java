package com.smartrecruit.recruitment.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文档解析服务，将上传的简历文件（PDF、DOCX、TXT）提取为纯文本。
 *
 * <p>封装 Apache Tika 的统一接口，自动检测文件类型并选择合适的解析器。</p>
 *
 * @since 1.0.0
 */
public interface DocumentParserService {

    /**
     * 从上传的简历文件中提取纯文本内容。
     *
     * @param file 上传的简历文件
     * @return 提取的纯文本，若提取失败返回空字符串
     * @throws com.smartrecruit.recruitment.exception.FileParseException 解析异常时抛出
     */
    String extractText(MultipartFile file);

    /**
     * 从字节数组中提取纯文本内容（用于异步线程安全）。
     *
     * @param fileBytes 文件字节数组
     * @param fileName  原始文件名（用于日志和类型检测）
     * @return 提取的纯文本，若提取失败返回空字符串
     * @throws com.smartrecruit.recruitment.exception.FileParseException 解析异常时抛出
     */
    String extractText(byte[] fileBytes, String fileName);

    /**
     * 判断文件是否为图片类型（需要走 OCR 流程）。
     *
     * @param file 上传的文件
     * @return true 表示该文件是图片（扫描版简历），需要 OCR 处理
     */
    boolean isImageBased(MultipartFile file);

    /**
     * 从 PDF 简历中提取头像/照片。
     * 查找第一页中最大的人像照片图片，返回 JPEG 字节数组。
     *
     * @param fileBytes PDF 文件字节数组
     * @param fileName  原始文件名（用于日志）
     * @return JPEG 格式的头像字节数组，未找到时返回 null
     */
    byte[] extractProfileImage(byte[] fileBytes, String fileName);
}
