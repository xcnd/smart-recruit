package com.smartrecruit.recruitment.controller;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.recruitment.service.ResumeParseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 简历同步解析接口（供「全流程编排」等场景复用）。
 *
 * <p>与上传流程不同，本接口只解析不落库，直接返回提取文本与结构化结果，
 * 便于前端将解析结果回填到编排表单。</p>
 *
 * @since 2026-04-11
 */
@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
@Slf4j
public class PipelineParseController {

    private final ResumeParseService resumeParseService;

    /**
     * 同步解析简历文件。
     *
     * @param file 简历文件（PDF/DOCX/图片等）
     * @return {@code {"rawText": 提取文本, "parsed": ParsedResume}}
     */
    @PostMapping("/parse-file")
    public ApiResponse<Map<String, Object>> parseFile(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> result = resumeParseService.parseFileForPipeline(
                    file.getBytes(), file.getOriginalFilename());
            return ApiResponse.success(result);
        } catch (IllegalStateException e) {
            log.warn("简历文件解析失败（AI 无法识别）: file={}, error={}",
                    file.getOriginalFilename(), e.getMessage());
            return ApiResponse.error(40001, e.getMessage());
        } catch (Exception e) {
            log.error("简历文件解析失败: file={}", file.getOriginalFilename(), e);
            return ApiResponse.error(50001, "简历文件解析失败：" + e.getMessage());
        }
    }
}
