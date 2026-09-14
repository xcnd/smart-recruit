package com.smartrecruit.system.service.file;

import com.smartrecruit.system.service.file.FileStorageService.FileResource;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务接口 — 通用文件上传/下载/删除到 RustFS。
 *
 * @since 2026-05-09
 */
public interface FileStorageService {

    /**
     * 上传文件到 RustFS（MultipartFile 形式），返回公开访问 URL。
     *
     * @param file         上传的文件
     * @param relativePath RustFS 桶中的相对路径（如 avatars/u_200001_abc.jpg）
     * @return 文件访问 URL
     */
    String upload(MultipartFile file, String relativePath);

    /**
     * 上传文件到 RustFS（字节数组形式），返回公开访问 URL。
     *
     * @param fileBytes    文件字节数组
     * @param fileName     原始文件名
     * @param contentType  文件 MIME 类型
     * @param relativePath RustFS 桶中的相对路径
     * @return 文件访问 URL
     */
    String upload(byte[] fileBytes, String fileName, String contentType, String relativePath);

    /**
     * 从 RustFS 下载文件，返回文件数据流及元信息。
     *
     * @param relativePath RustFS 桶中的相对路径
     * @return 文件资源（包含输入流、Content-Type、文件大小、文件名）
     * @throws com.smartrecruit.common.exception.ResourceNotFoundException 文件不存在时抛出
     */
    FileResource download(String relativePath);

    /**
     * 删除 RustFS 中的文件。
     *
     * @param relativePath RustFS 桶中的相对路径
     */
    void delete(String relativePath);

    /**
     * 文件下载结果：包含输入流和元数据，Controller 负责关流与 HTTP 响应组装。
     */
    record FileResource(java.io.InputStream inputStream, String contentType,
                        long contentLength, String fileName) {}
}
