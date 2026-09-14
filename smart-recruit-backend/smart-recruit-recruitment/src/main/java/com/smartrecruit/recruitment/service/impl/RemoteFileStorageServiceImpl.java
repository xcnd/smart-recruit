package com.smartrecruit.recruitment.service.impl;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.recruitment.feign.SystemFileClient;
import com.smartrecruit.recruitment.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * 基于 @HttpExchange 的远程文件存储服务实现，将文件操作委托给系统服务（smart-recruit-system）。
 *
 * @since 1.0.0
 */
@Service
@ConditionalOnProperty(name = "file.storage.type", havingValue = "rustfs")
@Slf4j
public class RemoteFileStorageServiceImpl implements FileStorageService {

    private final SystemFileClient systemFileClient;

    public RemoteFileStorageServiceImpl(SystemFileClient systemFileClient) {
        this.systemFileClient = systemFileClient;
    }

    /** 上传文件。 */
    @Override
    public String upload(byte[] fileBytes, String fileName, String contentType, String relativePath) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException("文件不能为空");
        }

        ApiResponse<String> response = systemFileClient.upload(
                fileBytes,
                fileName != null ? fileName : "unknown",
                contentType != null ? contentType : "application/octet-stream",
                relativePath);

        if (response == null || !response.ok()) {
            String errMsg = response != null ? response.message() : "未知错误";
            throw new RuntimeException("文件上传失败: " + errMsg);
        }
        log.info("通过 @HttpExchange 上传文件成功: path={}, size={}", relativePath, fileBytes.length);
        return response.data();
    }

    /** 上传文件。 */
    @Override
    public String upload(InputStream inputStream, long contentLength, String contentType, String relativePath) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[8192];
            int nRead;
            long totalRead = 0;
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
                totalRead += nRead;
                if (totalRead > 15 * 1024 * 1024) {
                    throw new IllegalArgumentException("文件大小不能超过 15 MB");
                }
            }
            buffer.flush();
            return upload(buffer.toByteArray(), null, contentType, relativePath);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("读取文件流失败", e);
        }
    }

    /** 下载文件字节流。 */
    @Override
    public byte[] downloadBytes(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        try {
            ApiResponse<byte[]> response = systemFileClient.downloadBytes(relativePath);
            if (response != null && response.ok() && response.data() != null) {
                log.debug("通过 @HttpExchange 下载文件成功: path={}, size={}", relativePath, response.data().length);
                return response.data();
            }
            log.warn("通过 @HttpExchange 下载文件失败: path={}, msg={}",
                    relativePath, response != null ? response.message() : "未知错误");
            return null;
        } catch (Exception e) {
            log.warn("通过 @HttpExchange 下载文件异常: path={}", relativePath, e);
            return null;
        }
    }

    /** 删除存储的文件。 */
    @Override
    public void delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        try {
            systemFileClient.delete(relativePath);
            log.debug("通过 @HttpExchange 删除文件成功: path={}", relativePath);
        } catch (Exception e) {
            log.warn("通过 @HttpExchange 删除文件失败（非致命错误）: path={}", relativePath, e);
        }
    }
}
