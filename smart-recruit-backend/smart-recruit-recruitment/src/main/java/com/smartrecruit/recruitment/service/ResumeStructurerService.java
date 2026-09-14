package com.smartrecruit.recruitment.service;

import com.smartrecruit.recruitment.domain.ParsedResume;

/**
 * 简历结构化服务，将提取的纯文本转换为结构化简历数据。
 *
 * @since 1.0.0
 */
public interface ResumeStructurerService {

    /**
     * 将简历纯文本提取为结构化信息。
     *
     * @param rawText 从简历文件中提取的原始文本
     * @return 结构化简历数据
     */
    ParsedResume structure(String rawText);
}
