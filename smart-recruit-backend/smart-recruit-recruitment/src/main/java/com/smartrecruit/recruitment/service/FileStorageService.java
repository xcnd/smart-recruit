package com.smartrecruit.recruitment.service;

import java.io.InputStream;

/**
 * 文件存储服务接口 — 上传简历文件到 RustFS。
 *
 * @since 1.0.0
 */
public interface FileStorageService {

    /**
     * 上传字节数组到 RustFS，返回公开访问 URL。
     *
     * @param fileBytes    文件字节数组
     * @param fileName     原始文件名
     * @param contentType  文件 MIME 类型
     * @param relativePath RustFS 桶中的相对路径（如 resumes/2026/07/uuid_resume.pdf）
     * @return 文件访问 URL
     */
    String upload(byte[] fileBytes, String fileName, String contentType, String relativePath);

    /**
     * 上传输入流到 RustFS，返回公开访问 URL。
     *
     * @param inputStream  文件输入流
     * @param contentLength 文件大小
     * @param contentType  文件 MIME 类型
     * @param relativePath RustFS 桶中的相对路径
     * @return 文件访问 URL
     */
    String upload(InputStream inputStream, long contentLength, String contentType, String relativePath);

    /**
     * 从 RustFS 下载文件字节数据（供定时任务解析使用）。
     *
     * @param relativePath RustFS 桶中的相对路径
     * @return 文件字节数组，下载失败返回 null
     */
    byte[] downloadBytes(String relativePath);

    /**
     * 删除 RustFS 中的文件。
     *
     * @param relativePath RustFS 桶中的相对路径
     */
    void delete(String relativePath);
}
