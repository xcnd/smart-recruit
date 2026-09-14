package com.smartrecruit.system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 更新单个配置项的请求 DTO。
 *
 * @since 2026-05-26
 */
public record ConfigItemRequest(
        @NotBlank(message = "配置键不能为空")
        @Size(max = 64, message = "配置键最多64个字符")
        String configKey,

        @NotBlank(message = "配置值不能为空")
        @Size(max = 512, message = "配置值最多512个字符")
        String configValue
) {
}
