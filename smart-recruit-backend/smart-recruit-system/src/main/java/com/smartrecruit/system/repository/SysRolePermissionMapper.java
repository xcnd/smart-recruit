package com.smartrecruit.system.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruit.system.entity.SysRolePermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色-权限关联 Mapper。
 *
 * @since 2026-04-26
 */
@Mapper
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {

    /**
     * 根据角色 ID 查询权限 ID 列表。
     */
    List<Long> selectPermissionIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据角色 ID 删除所有权限关联。
     */
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据权限 ID 删除所有角色关联。
     */
    int deleteByPermissionId(@Param("permId") Long permId);
}
