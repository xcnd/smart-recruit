package com.smartrecruit.system.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/**
 * 按模块分组的权限视图对象。
 *
 * @param module      权限所属模块标识（由权限编码前缀推导）
 * @param permissions 该模块下的权限列表
 * @since 2026-04-07
 */
@JsonPropertyOrder({"module", "permissions"})
public record PermissionGroupVO(
        @JsonProperty("module") String module,
        @JsonProperty("permissions") List<PermissionVO> permissions
) {
}
