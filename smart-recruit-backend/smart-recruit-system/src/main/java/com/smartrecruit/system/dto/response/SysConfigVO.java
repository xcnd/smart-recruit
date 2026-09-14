package com.smartrecruit.system.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDateTime;

/**
 * 系统配置视图 VO。
 *
 * @since 2026-05-26
 */
@JsonPropertyOrder({"id", "configKey", "configValue", "description", "createTime", "updateTime", "createBy", "updateBy"})
public record SysConfigVO(
        @JsonProperty("id")
        Long id,

        @JsonProperty("configKey")
        String configKey,

        @JsonProperty("configValue")
        String configValue,

        @JsonProperty("description")
        String description,

        @JsonProperty("createTime")
        LocalDateTime createTime,

        @JsonProperty("updateTime")
        LocalDateTime updateTime,

        @JsonProperty("createBy")
        String createBy,

        @JsonProperty("updateBy")
        String updateBy
) {
}
