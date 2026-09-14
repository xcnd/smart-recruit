package com.smartrecruit.system.dto.request;

import lombok.Data;

import java.util.List;

/**
 * 更新角色权限请求。
 *
 * @since 2026-04-07
 */
@Data
public class UpdateRolePermissionsRequest {

    /** 权限 ID 列表。 */
    private List<Long> permissionIds;
}
