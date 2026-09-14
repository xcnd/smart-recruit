package com.smartrecruit.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.common.exception.ResourceNotFoundException;
import com.smartrecruit.system.entity.SysUser;
import com.smartrecruit.system.repository.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统用户内部接口（服务间调用）。
 *
 * @since 2026-04-08
 */
@RestController
@RequestMapping("/api/v1/internal/users")
@RequiredArgsConstructor
@Slf4j
public class InternalUserController {

    private final SysUserMapper sysUserMapper;

    /**
     * 查询所有启用状态的管理员用户 ID（超级管理员 + HR 管理员）。
     *
     * <p>供面试/Offer 等业务服务在关键操作（安排面试、提交反馈等）时
     * 向管理员发送站内通知。</p>
     */
    @GetMapping("/admins")
    public ApiResponse<List<Long>> adminIds() {
        List<Long> ids = sysUserMapper.selectAdminUserIds();
        log.info("查询管理员用户 ID 列表: count={}", ids.size());
        return ApiResponse.success(ids);
    }

    /**
     * 根据用户名查询用户 ID（供业务服务定位创建人/负责人）。
     */
    @GetMapping("/by-username")
    public ApiResponse<Long> userIdByUsername(@RequestParam String username) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
                        .last("LIMIT 1"));
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在: username=" + username);
        }
        return ApiResponse.success(user.getId());
    }
}
