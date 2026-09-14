package com.smartrecruit.offer.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * 文件上传结果视图对象。
 *
 * @param url  文件访问 URL
 * @param name 原始文件名
 * @param size 文件大小（字节）
 * @since 2026-04-07
 */
@JsonPropertyOrder({"url", "name", "size"})
public record FileUploadVO(
        @JsonProperty("url") String url,
        @JsonProperty("name") String name,
        @JsonProperty("size") String size
) {
}
