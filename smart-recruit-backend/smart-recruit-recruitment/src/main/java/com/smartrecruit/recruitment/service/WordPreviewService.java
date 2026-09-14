package com.smartrecruit.recruitment.service;

/**
 * 简历文件预览服务（Word → HTML）。
 *
 * @since 1.0.0
 */
public interface WordPreviewService {

    /**
     * 将 Word 文件字节流转为 HTML 预览内容。
     */
    String convertToHtml(byte[] fileBytes, String fileName);
}
