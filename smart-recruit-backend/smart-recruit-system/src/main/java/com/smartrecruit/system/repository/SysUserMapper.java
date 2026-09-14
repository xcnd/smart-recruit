package com.smartrecruit.system.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruit.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统用户 Mapper。
 *
 * @since 2026-04-26
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 根据邮箱查询用户。
     */
    SysUser findByEmail(@Param("email") String email);

    /**
     * 根据用户名查询用户。
     */
    SysUser findByUsername(@Param("username") String username);

    /**
     * 根据手机号查询用户。
     */
    SysUser findByMobile(@Param("mobile") String mobile);

    /**
     * 查询所有启用状态的管理员用户 ID（超级管理员 + HR 管理员）。
     */
    @Select("SELECT DISTINCT u.id FROM sys_user u "
            + "JOIN sys_user_role ur ON u.id = ur.user_id "
            + "JOIN sys_role r ON ur.role_id = r.id "
            + "WHERE r.role_code IN ('ROLE_SUPER_ADMIN', 'ROLE_HR_ADMIN') "
            + "AND u.status = 1 AND u.deleted = 0")
    List<Long> selectAdminUserIds();

    /**
     * 分页查询用户列表（含部门、角色信息）。
     */
    IPage<com.smartrecruit.system.dto.response.UserVO> pageQuery(
            Page<SysUser> page,
            @Param("username") String username,
            @Param("email") String email,
            @Param("userId") Long userId,
            @Param("status") Integer status,
            @Param("deptId") Long deptId,
            @Param("roleIds") String roleIds);
}
