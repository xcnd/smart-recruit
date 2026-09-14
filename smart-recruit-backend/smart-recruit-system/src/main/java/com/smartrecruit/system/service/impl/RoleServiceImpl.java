package com.smartrecruit.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartrecruit.common.constant.Constants;
import com.smartrecruit.common.exception.DuplicateResourceException;
import com.smartrecruit.common.exception.ResourceNotFoundException;
import com.smartrecruit.system.audit.Auditable;
import com.smartrecruit.system.converter.RoleConverter;
import com.smartrecruit.system.dto.request.CreateRoleRequest;
import com.smartrecruit.system.dto.request.UpdateRoleRequest;
import com.smartrecruit.system.dto.response.RoleDetailVO;
import com.smartrecruit.system.dto.response.RoleStatsVO;
import com.smartrecruit.system.dto.response.RoleVO;
import com.smartrecruit.system.entity.SysRole;
import com.smartrecruit.system.entity.SysRolePermission;
import com.smartrecruit.system.repository.SysRoleMapper;
import com.smartrecruit.system.repository.SysRolePermissionMapper;
import com.smartrecruit.system.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色管理服务实现类。
 *
 * @since 2026-04-26
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final SysRoleMapper sysRoleMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final RoleConverter roleConverter;

    /** 查询全部记录。 */
    @Override
    public List<RoleVO> listAll() {
        log.info("查询所有角色");

        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getStatus, Constants.STATUS_ENABLED)
                .orderByAsc(SysRole::getSortOrder);
        List<SysRole> roles = sysRoleMapper.selectList(wrapper);

        return roleConverter.toVOList(roles);
    }

    /** 根据主键查询详情。 */
    @Override
    public RoleDetailVO getById(Long id) {
        log.info("查询角色详情: id={}", id);

        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            throw new ResourceNotFoundException("角色不存在: id=" + id);
        }

        RoleDetailVO vo = roleConverter.toDetailVO(role);
        List<Long> permIds = sysRolePermissionMapper.selectPermissionIdsByRoleId(id);

        return new RoleDetailVO(
                vo.id(), vo.name(), vo.code(), vo.description(),
                vo.status(), vo.sortOrder(), permIds,
                vo.createTime(), vo.updateTime());
    }

    /** 创建记录。 */
    @Override
    @Auditable(action = "CREATE", resourceType = "ROLE", module = 0)
    @Transactional(rollbackFor = Exception.class)
    public RoleVO create(CreateRoleRequest request) {
        log.info("创建角色: name={}, code={}", request.name(), request.code());

        // Check code uniqueness
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getCode, request.code());
        if (sysRoleMapper.selectCount(wrapper) > 0) {
            throw new DuplicateResourceException("角色", "code", request.code());
        }

        SysRole role = roleConverter.toEntity(request);
        role.setStatus(Constants.STATUS_ENABLED);
        if (request.sortOrder() != null) {
            role.setSortOrder(request.sortOrder());
        }
        sysRoleMapper.insert(role);

        log.info("角色创建成功: roleId={}, name={}", role.getId(), role.getName());
        return roleConverter.toVO(role);
    }

    /** 更新记录。 */
    @Override
    @Auditable(action = "UPDATE", resourceType = "ROLE", module = 0)
    @Transactional(rollbackFor = Exception.class)
    public RoleVO update(Long id, UpdateRoleRequest request) {
        log.info("更新角色: id={}", id);

        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            throw new ResourceNotFoundException("角色不存在: id=" + id);
        }

        if (request.name() != null) {
            role.setName(request.name());
        }
        if (request.description() != null) {
            role.setDescription(request.description());
        }
        if (request.sortOrder() != null) {
            role.setSortOrder(request.sortOrder());
        }

        sysRoleMapper.updateById(role);

        log.info("角色更新成功: roleId={}", id);
        return roleConverter.toVO(role);
    }

    /** 根据主键删除记录。 */
    @Override
    @Auditable(action = "DELETE", resourceType = "ROLE", module = 0)
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        log.info("删除角色: id={}", id);

        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            throw new ResourceNotFoundException("角色不存在: id=" + id);
        }

        // Logical delete
        sysRoleMapper.deleteById(id);

        // Clean up role-permission associations
        sysRolePermissionMapper.deleteByRoleId(id);

        log.info("角色删除成功: roleId={}", id);
    }

    /** 查询角色统计信息（数量、状态分布等）。 */
    @Override
    public RoleStatsVO getRoleStats() {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        long total = sysRoleMapper.selectCount(wrapper);
        long builtin = sysRoleMapper.selectCount(
                new LambdaQueryWrapper<SysRole>().likeRight(SysRole::getCode, "ROLE_"));
        long custom = total - builtin;
        return new RoleStatsVO(total, builtin, custom);
    }

    /** 查询角色关联的权限 ID 列表。 */
    @Override
    public List<Long> getPermissionIds(Long roleId) {
        return sysRolePermissionMapper.selectPermissionIdsByRoleId(roleId);
    }

    /** 更新角色关联的权限。 */
    @Override
    @Auditable(action = "UPDATE", resourceType = "ROLE_PERMISSION", module = 0)
    @Transactional(rollbackFor = Exception.class)
    public void updatePermissions(Long roleId, List<Long> permissionIds) {
        // Remove existing permissions
        sysRolePermissionMapper.deleteByRoleId(roleId);
        // Insert new permissions
        if (permissionIds != null && !permissionIds.isEmpty()) {
            List<SysRolePermission> list = new ArrayList<>();
            for (Long permId : permissionIds) {
                SysRolePermission rp = new SysRolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionId(permId);
                list.add(rp);
            }
            for (SysRolePermission rp : list) {
                sysRolePermissionMapper.insert(rp);
            }
        }
        log.info("角色权限更新成功: roleId={}, permCount={}", roleId,
                permissionIds != null ? permissionIds.size() : 0);
    }
}
