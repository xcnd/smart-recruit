package com.smartrecruit.system.service;

import com.smartrecruit.system.dto.request.CreatePermissionRequest;
import com.smartrecruit.system.dto.request.UpdatePermissionRequest;
import com.smartrecruit.system.dto.response.PermissionTreeVO;
import com.smartrecruit.system.dto.response.PermissionVO;
import com.smartrecruit.system.dto.response.PermissionGroupVO;

import java.util.List;
import java.util.Map;

/**
 * 权限管理服务接口。
 *
 * @since 2026-04-26
 */
public interface PermissionService {

    /**
     * 查询所有权限，按模块分组。
     */
    List<PermissionGroupVO> listAllGrouped();

    /**
     * 查询所有权限（树形结构）。
     */
    List<PermissionTreeVO> listAllTree();

    /**
     * 根据 ID 查询权限详情。
     */
    PermissionVO getById(Long id);

    /**
     * 根据角色 ID 查询权限列表。
     */
    List<PermissionVO> getByRoleId(Long roleId);

    /**
     * 创建权限。
     */
    PermissionVO create(CreatePermissionRequest request);

    /**
     * 更新权限。
     */
    PermissionVO update(Long id, UpdatePermissionRequest request);

    /**
     * 删除权限（逻辑删除 + 清理角色关联）。
     */
    void delete(Long id);

    /**
     * 更新角色的权限列表（全量替换）。
     */
    void updateRolePermissions(Long roleId, List<Long> permIds);
}
