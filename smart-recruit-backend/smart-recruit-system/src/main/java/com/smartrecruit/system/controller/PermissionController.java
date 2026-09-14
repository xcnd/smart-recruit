package com.smartrecruit.system.controller;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.system.dto.request.CreatePermissionRequest;
import com.smartrecruit.system.dto.request.UpdatePermissionRequest;
import com.smartrecruit.system.dto.response.PermissionTreeVO;
import com.smartrecruit.system.dto.response.PermissionVO;
import com.smartrecruit.system.dto.response.PermissionGroupVO;
import com.smartrecruit.system.service.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 权限管理控制器。
 *
 * @since 2026-04-26
 */
@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("!hasRole(T(com.smartrecruit.common.constant.Constants).CANDIDATE_ROLE)")
public class PermissionController {

    private final PermissionService permissionService;

    /**
     * 查询所有权限（按模块分组）。
     */
    @GetMapping
    public ApiResponse<List<PermissionGroupVO>> list() {
        List<PermissionGroupVO> grouped = permissionService.listAllGrouped();
        return ApiResponse.success(grouped);
    }

    /**
     * 查询所有权限（树形结构）。
     */
    @GetMapping("/tree")
    public ApiResponse<List<PermissionTreeVO>> listTree() {
        List<PermissionTreeVO> tree = permissionService.listAllTree();
        return ApiResponse.success(tree);
    }

    /**
     * 查询权限详情。
     */
    @GetMapping("/detail/{id}")
    public ApiResponse<PermissionVO> detail(@PathVariable Long id) {
        PermissionVO vo = permissionService.getById(id);
        return ApiResponse.success(vo);
    }

    /**
     * 查询角色关联的权限列表。
     */
    @GetMapping("/{roleId}")
    public ApiResponse<List<PermissionVO>> getByRole(@PathVariable Long roleId) {
        List<PermissionVO> permissions = permissionService.getByRoleId(roleId);
        return ApiResponse.success(permissions);
    }

    /**
     * 创建权限。
     */
    @PostMapping
    public ApiResponse<PermissionVO> create(@Valid @RequestBody CreatePermissionRequest request) {
        PermissionVO vo = permissionService.create(request);
        return ApiResponse.success(vo);
    }

    /**
     * 更新权限。
     */
    @PutMapping("/update/{id}")
    public ApiResponse<PermissionVO> update(@PathVariable Long id,
                                             @Valid @RequestBody UpdatePermissionRequest request) {
        PermissionVO vo = permissionService.update(id, request);
        return ApiResponse.success(vo);
    }

    /**
     * 删除权限。
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        permissionService.delete(id);
        return ApiResponse.success();
    }

    /**
     * 更新角色权限（全量替换）。
     */
    @PutMapping("/{roleId}")
    public ApiResponse<Void> updateRolePermissions(@PathVariable Long roleId,
                                                    @RequestBody List<Long> permIds) {
        permissionService.updateRolePermissions(roleId, permIds);
        return ApiResponse.success();
    }
}
