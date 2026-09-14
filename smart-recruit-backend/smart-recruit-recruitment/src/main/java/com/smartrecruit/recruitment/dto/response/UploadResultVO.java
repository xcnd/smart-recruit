package com.smartrecruit.recruitment.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * 文件上传结果视图对象。
 *
 * @param url 文件访问 URL
 * @since 2026-04-07
 */
@JsonPropertyOrder({"url"})
public record UploadResultVO(
        @JsonProperty("url") String url
) {
}
