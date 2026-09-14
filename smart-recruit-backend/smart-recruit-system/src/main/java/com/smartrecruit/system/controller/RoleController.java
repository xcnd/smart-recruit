package com.smartrecruit.system.controller;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.system.dto.request.CreateRoleRequest;
import com.smartrecruit.system.dto.request.UpdateRoleRequest;
import com.smartrecruit.system.dto.request.UpdateRolePermissionsRequest;
import com.smartrecruit.system.dto.response.RoleDetailVO;
import com.smartrecruit.system.dto.response.RoleStatsVO;
import com.smartrecruit.system.dto.response.RoleVO;
import com.smartrecruit.system.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 角色管理控制器。
 *
 * @since 2026-04-26
 */
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("!hasRole(T(com.smartrecruit.common.constant.Constants).CANDIDATE_ROLE)")
public class RoleController {

    private final RoleService roleService;

    /**
     * 角色统计。
     */
    @GetMapping("/stats")
    public ApiResponse<RoleStatsVO> stats() {
        RoleStatsVO vo = roleService.getRoleStats();
        return ApiResponse.success(vo);
    }

    /**
     * 查询所有角色。
     */
    @GetMapping
    public ApiResponse<List<RoleVO>> list() {
        List<RoleVO> roles = roleService.listAll();
        return ApiResponse.success(roles);
    }

    /**
     * 查询角色详情。
     */
    @GetMapping("/{id}")
    public ApiResponse<RoleDetailVO> detail(@PathVariable Long id) {
        RoleDetailVO vo = roleService.getById(id);
        return ApiResponse.success(vo);
    }

    /**
     * 创建角色。
     */
    @PostMapping
    public ApiResponse<RoleVO> create(@Valid @RequestBody CreateRoleRequest request) {
        RoleVO vo = roleService.create(request);
        return ApiResponse.success(vo);
    }

    /**
     * 更新角色。
     */
    @PutMapping("/{id}")
    public ApiResponse<RoleVO> update(@PathVariable Long id,
                                       @Valid @RequestBody UpdateRoleRequest request) {
        RoleVO vo = roleService.update(id, request);
        return ApiResponse.success(vo);
    }

    /**
     * 删除角色。
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ApiResponse.success();
    }

    /**
     * 查询角色权限 ID 列表。
     */
    @GetMapping("/{id}/permissions")
    public ApiResponse<List<Long>> getPermissions(@PathVariable Long id) {
        List<Long> permIds = roleService.getPermissionIds(id);
        return ApiResponse.success(permIds);
    }

    /**
     * 更新角色权限。
     */
    @PutMapping("/{id}/permissions")
    public ApiResponse<Void> updatePermissions(@PathVariable Long id,
                                                @RequestBody UpdateRolePermissionsRequest request) {
        roleService.updatePermissions(id, request.getPermissionIds());
        return ApiResponse.success();
    }
}
