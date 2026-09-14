package com.smartrecruit.offer.feign;

import com.smartrecruit.common.dto.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * 系统服务文件存储 HTTP 接口客户端。
 *
 * <p>使用 Spring 6 / Boot 4 原生 {@code @HttpExchange} 调用 smart-recruit-system 的文件上传/删除接口。
 * 文件以原始字节流传输，元数据通过请求头传递。</p>
 *
 * @since 1.0.0
 */
@HttpExchange("/api/v1/files")
public interface SystemFileClient {

    @PostExchange(value = "/upload", contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    ApiResponse<String> upload(@RequestBody byte[] fileBytes,
                                @RequestHeader("X-File-Name") String fileName,
                                @RequestHeader("X-Content-Type") String contentType,
                                @RequestHeader("X-Relative-Path") String relativePath);

    @GetExchange("/download-bytes")
    ApiResponse<byte[]> downloadBytes(@RequestParam("path") String path);

    @DeleteExchange("/delete")
    ApiResponse<Void> delete(@RequestParam("path") String path);
}
