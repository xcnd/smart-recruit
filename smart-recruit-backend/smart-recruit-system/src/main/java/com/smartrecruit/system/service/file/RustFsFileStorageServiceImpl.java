package com.smartrecruit.system.service.file;

import com.smartrecruit.common.constant.ConfigKeys;
import com.smartrecruit.common.exception.ResourceNotFoundException;
import com.smartrecruit.system.config.FileStorageProperties;
import com.smartrecruit.system.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * RustFS S3 通用文件存储服务实现。
 *
 * <p>通过 AWS S3 SDK 将文件上传到 RustFS 对象存储。
 * 统一执行系统配置的大小与类型限制，配置修改后立即生效。</p>
 *
 * @since 2026-05-09
 */
@Service
@ConditionalOnProperty(name = "file.storage.type", havingValue = "rustfs")
@RequiredArgsConstructor
@Slf4j
public class RustFsFileStorageServiceImpl implements FileStorageService {

    private final S3Client s3Client;
    private final FileStorageProperties storageProperties;
    private final SysConfigService sysConfigService;

    /** 上传文件。 */
    @Override
    public String upload(MultipartFile file, String relativePath) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        try {
            return upload(file.getBytes(), file.getOriginalFilename(),
                    file.getContentType(), relativePath);
        } catch (IOException e) {
            log.error("读取上传文件失败", e);
            throw new RuntimeException("文件读取失败", e);
        }
    }

    /** 上传文件。 */
    @Override
    public String upload(byte[] fileBytes, String fileName, String contentType, String relativePath) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException("文件不能为空");
        }
        validateFile(fileBytes.length, fileName);

        String bucketName = storageProperties.getRustfs().getBucketName();

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(relativePath)
                            .contentType(contentType != null ? contentType : "application/octet-stream")
                            .build(),
                    RequestBody.fromInputStream(new ByteArrayInputStream(fileBytes), fileBytes.length));

            // 构建 RustFS 直接访问 URL
            FileStorageProperties.Rustfs rustfs = storageProperties.getRustfs();
            String protocol = rustfs.isSecure() ? "https" : "http";
            String fileUrl = String.format("%s://%s:%d/%s/%s",
                    protocol, rustfs.getEndpoint(), rustfs.getPort(),
                    bucketName, relativePath);
            log.info("文件上传成功: bucket={}, key={}, size={}", bucketName, relativePath, fileBytes.length);
            return fileUrl;
        } catch (S3Exception e) {
            log.error("文件上传到 RustFS 失败: bucket={}, key={}", bucketName, relativePath, e);
            throw new RuntimeException("文件上传失败，请稍后重试", e);
        }
    }

    /**
     * 按系统配置校验文件大小与扩展名（配置修改后立即生效）。
     */
    private void validateFile(long size, String fileName) {
        int maxSizeMb = sysConfigService.getInt(ConfigKeys.UPLOAD_MAX_SIZE_MB, 10);
        long maxSizeBytes = (long) maxSizeMb * 1024 * 1024;
        if (size > maxSizeBytes) {
            throw new IllegalArgumentException("文件大小不能超过 " + maxSizeMb + " MB");
        }

        List<String> allowedExtensions =
                sysConfigService.getList(ConfigKeys.UPLOAD_ALLOWED_EXTENSIONS);
        if (allowedExtensions.isEmpty()) {
            return;
        }
        String lowerName = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        boolean allowed = allowedExtensions.stream()
                .map(ext -> ext.startsWith(".") ? ext : "." + ext)
                .map(ext -> ext.toLowerCase(Locale.ROOT))
                .anyMatch(lowerName::endsWith);
        if (!allowed) {
            throw new IllegalArgumentException("不支持的文件类型：" + fileName
                    + "，允许的类型：" + String.join(", ", allowedExtensions));
        }
    }

    /** 下载文件。 */
    @Override
    public FileResource download(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("文件路径不能为空");
        }

        String bucketName = storageProperties.getRustfs().getBucketName();

        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(relativePath)
                    .build();
            ResponseInputStream<GetObjectResponse> s3Response = s3Client.getObject(getRequest);

            String fileName = relativePath.substring(relativePath.lastIndexOf('/') + 1);
            return new FileResource(
                    s3Response,
                    s3Response.response().contentType(),
                    s3Response.response().contentLength(),
                    fileName);
        } catch (NoSuchKeyException e) {
            throw new ResourceNotFoundException("文件不存在: " + relativePath);
        } catch (S3Exception e) {
            log.error("从 RustFS 下载文件失败: bucket={}, key={}", bucketName, relativePath, e);
            throw new RuntimeException("文件下载失败", e);
        }
    }

    /** 删除存储的文件。 */
    @Override
    public void delete(String relativePath) {
        String bucketName = storageProperties.getRustfs().getBucketName();

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(relativePath)
                    .build());
            log.debug("文件删除成功: bucket={}, key={}", bucketName, relativePath);
        } catch (S3Exception e) {
            log.warn("从 RustFS 删除文件失败（可忽略）: bucket={}, key={}", bucketName, relativePath, e);
        }
    }
}
