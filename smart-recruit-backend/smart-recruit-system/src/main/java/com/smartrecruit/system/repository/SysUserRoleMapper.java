package com.smartrecruit.system.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruit.system.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户-角色关联 Mapper。
 *
 * @since 2026-04-26
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    /**
     * 根据用户 ID 查询角色 ID 列表。
     */
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);
}
