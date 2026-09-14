package com.smartrecruit.system.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruit.system.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统权限 Mapper。
 *
 * @since 2026-04-26
 */
@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    /**
     * 根据角色 ID 查询权限列表。
     */
    List<SysPermission> findByRoleId(@Param("roleId") Long roleId);
}
