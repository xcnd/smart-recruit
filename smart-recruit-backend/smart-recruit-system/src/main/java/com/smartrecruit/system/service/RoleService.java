package com.smartrecruit.system.service;

import com.smartrecruit.system.dto.request.CreateRoleRequest;
import com.smartrecruit.system.dto.request.UpdateRoleRequest;
import com.smartrecruit.system.dto.response.RoleDetailVO;
import com.smartrecruit.system.dto.response.RoleStatsVO;
import com.smartrecruit.system.dto.response.RoleVO;

import java.util.List;

/**
 * 角色管理服务接口。
 *
 * @since 2026-04-26
 */
public interface RoleService {

    /**
     * 查询所有角色列表。
     */
    List<RoleVO> listAll();

    /**
     * 根据 ID 查询角色详情。
     */
    RoleDetailVO getById(Long id);

    /**
     * 创建角色。
     */
    RoleVO create(CreateRoleRequest request);

    /**
     * 更新角色信息。
     */
    RoleVO update(Long id, UpdateRoleRequest request);

    /**
     * 删除角色。
     */
    void delete(Long id);

    /**
     * 角色统计。
     */
    RoleStatsVO getRoleStats();

    /**
     * 查询角色的权限 ID 列表。
     */
    List<Long> getPermissionIds(Long roleId);

    /**
     * 更新角色权限。
     */
    void updatePermissions(Long roleId, List<Long> permissionIds);
}
