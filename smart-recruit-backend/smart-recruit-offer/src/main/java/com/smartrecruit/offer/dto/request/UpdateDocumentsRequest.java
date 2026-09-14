package com.smartrecruit.offer.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;

import java.util.Map;

/**
 * 批量更新入职资料提交状态请求。
 *
 * <p>请求体为 {@code {文档类型编码: 状态编码}} 的键值映射，
 * 通过 {@link JsonCreator} 直接反序列化为 {@code documents} 字段，保持线上 JSON 格式不变。</p>
 *
 * @since 2026-04-07
 */
@Data
public class UpdateDocumentsRequest {

    /** 文档类型编码 → 状态编码 映射。 */
    private final Map<String, String> documents;

    @JsonCreator
    public UpdateDocumentsRequest(Map<String, String> documents) {
        this.documents = documents;
    }
}
