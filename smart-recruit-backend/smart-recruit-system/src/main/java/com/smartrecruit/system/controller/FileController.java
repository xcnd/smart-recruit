package com.smartrecruit.system.controller;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.common.exception.ResourceNotFoundException;
import com.smartrecruit.system.service.file.FileStorageService;
import com.smartrecruit.system.service.file.FileStorageService.FileResource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;

/**
 * 文件管理控制器 — 负责 HTTP 层参数提取、响应组装，文件存储逻辑委托给 {@link FileStorageService}。
 *
 * <p>访问路径：
 * <ul>
 *   <li>下载：GET /api/v1/files/download/**</li>
 *   <li>上传：POST /api/v1/files/upload</li>
 *   <li>删除：DELETE /api/v1/files/delete</li>
 * </ul>
 *
 * @since 2026-05-09
 */
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileStorageService fileStorageService;

    /**
     * 下载/预览 RustFS 文件。
     * 将 /api/v1/files/download/** 路径映射到 RustFS 的对应 key。
     */
    @GetMapping("/download/**")
    public void download(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String key = extractKeyFromPath(request);
        if (key.isEmpty()) {
            throw new ResourceNotFoundException("文件路径不能为空");
        }

        try {
            FileResource resource = fileStorageService.download(key);

            response.setContentType(resource.contentType() != null
                    ? resource.contentType() : "application/octet-stream");
            response.setContentLengthLong(resource.contentLength());
            response.setHeader("Content-Disposition",
                    "inline; filename=\"" + resource.fileName() + "\"");
            response.setHeader("Cache-Control", "public, max-age=86400");

            resource.inputStream().transferTo(response.getOutputStream());
            response.flushBuffer();
        } catch (ResourceNotFoundException e) {
            log.warn("文件不存在: key={}", key);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "文件不存在");
        } catch (Exception e) {
            log.error("文件下载失败: key={}", key, e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "文件下载失败");
        }
    }

    /**
     * 上传文件到 RustFS（内部微服务 Feign 调用）。
     *
     * <p>文件以原始字节流传输，元数据通过请求头传递：
     * <ul>
     *   <li>{@code X-File-Name} — 原始文件名</li>
     *   <li>{@code X-Content-Type} — 文件 MIME 类型</li>
     *   <li>{@code X-Relative-Path} — RustFS 桶中的相对路径</li>
     * </ul>
     */
    @PostMapping("/upload")
    public ApiResponse<String> upload(@RequestBody byte[] fileBytes,
                                       @RequestHeader("X-File-Name") String fileName,
                                       @RequestHeader("X-Content-Type") String contentType,
                                       @RequestHeader("X-Relative-Path") String relativePath) {
        if (fileBytes == null || fileBytes.length == 0) {
            return ApiResponse.error(400, "文件不能为空");
        }
        String url = fileStorageService.upload(fileBytes, fileName, contentType, relativePath);
        return ApiResponse.success(url);
    }

    /**
     * 以字节数组形式下载文件（供内部微服务定时任务使用）。
     *
     * @param path RustFS 桶中的相对路径
     * @return 文件字节数据
     */
    @GetMapping("/download-bytes")
    public ApiResponse<byte[]> downloadBytes(@RequestParam("path") String path) {
        if (path == null || path.isBlank()) {
            return ApiResponse.error(400, "文件路径不能为空");
        }
        try {
            FileResource resource = fileStorageService.download(path);
            byte[] bytes = resource.inputStream().readAllBytes();
            return ApiResponse.success(bytes);
        } catch (ResourceNotFoundException e) {
            log.warn("文件不存在: key={}", path);
            return ApiResponse.error(404, "文件不存在");
        } catch (Exception e) {
            log.error("文件下载失败: key={}", path, e);
            return ApiResponse.error(500, "文件下载失败");
        }
    }

    /**
     * 删除 RustFS 中的文件（内部微服务调用）。
     */
    @DeleteMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam("path") String path) {
        if (path == null || path.isBlank()) {
            return ApiResponse.error(400, "文件路径不能为空");
        }
        fileStorageService.delete(path);
        return ApiResponse.success();
    }

    /**
     * 从请求 URL 中提取下载路径对应的 RustFS key。
     * <p>例如 /api/v1/files/download/resumes/2026/07/xxx.pdf → key = resumes/2026/07/xxx.pdf</p>
     */
    private String extractKeyFromPath(HttpServletRequest request) {
        String fullPath = (String) request.getAttribute(
                HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatchingPattern = (String) request.getAttribute(
                HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

        if (bestMatchingPattern == null || fullPath == null) {
            return "";
        }
        String prefix = bestMatchingPattern.substring(0, bestMatchingPattern.indexOf("**"));
        return fullPath.substring(prefix.length());
    }
}
