package com.smartrecruit.system.controller;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.system.dto.request.ChangeEmailRequest;
import com.smartrecruit.system.dto.request.ChangePasswordRequest;
import com.smartrecruit.system.dto.request.CreateUserRequest;
import com.smartrecruit.system.dto.request.UpdateProfileRequest;
import com.smartrecruit.system.dto.request.UpdateUserRequest;
import com.smartrecruit.system.dto.request.AdminResetPasswordRequest;
import com.smartrecruit.system.dto.request.UserPageQuery;
import com.smartrecruit.system.dto.response.UserDetailVO;
import com.smartrecruit.system.dto.response.UserStatsVO;
import com.smartrecruit.system.dto.response.UserVO;
import com.smartrecruit.system.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 用户管理控制器。
 *
 * @since 2026-04-26
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * 获取当前登录用户的个人信息详情。
     */
    @GetMapping("/me/profile")
    public ApiResponse<UserDetailVO> getMyProfile() {
        UserDetailVO vo = userService.getCurrentUserProfile();
        return ApiResponse.success(vo);
    }

    /**
     * 当前登录用户更新个人信息。
     */
    @PutMapping("/me/profile")
    public ApiResponse<UserVO> updateMyProfile(@Valid @RequestBody UpdateProfileRequest request) {
        UserVO vo = userService.updateCurrentUserProfile(request);
        return ApiResponse.success("个人信息更新成功", vo);
    }

    /**
     * 当前登录用户修改密码。
     */
    @PutMapping("/me/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request.oldPassword(), request.newPassword());
        return ApiResponse.<Void>success("密码修改成功", null);
    }

    /**
     * 当前登录用户修改邮箱。
     */
    @PutMapping("/me/email")
    public ApiResponse<Void> changeEmail(@Valid @RequestBody ChangeEmailRequest request) {
        userService.changeEmail(request.newEmail(), request.verificationCode());
        return ApiResponse.<Void>success("邮箱修改成功", null);
    }

    /**
     * 当前登录用户上传头像。
     * 支持 JPEG/PNG/GIF/WebP，最大 2 MB。
     */
    @PutMapping("/me/avatar")
    public ApiResponse<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String avatarUrl = userService.uploadAvatar(file);
        return ApiResponse.success("头像更新成功", avatarUrl);
    }

    /**
     * 分页查询用户列表。
     */
    @GetMapping
    @PreAuthorize("!hasRole(T(com.smartrecruit.common.constant.Constants).CANDIDATE_ROLE)")
    public ApiResponse<PageResult<UserVO>> list(@Valid UserPageQuery query) {
        PageResult<UserVO> result = userService.pageQuery(query);
        return ApiResponse.success(result);
    }

    /**
     * 获取用户统计数据（总数、正常、冻结、禁用）。
     */
    @GetMapping("/stats")
    @PreAuthorize("!hasRole(T(com.smartrecruit.common.constant.Constants).CANDIDATE_ROLE)")
    public ApiResponse<UserStatsVO> stats() {
        UserStatsVO vo = userService.getUserStats();
        return ApiResponse.success(vo);
    }

    /**
     * 查询拥有「面试官」角色的用户列表（面试安排时选择面试官）。
     */
    @GetMapping("/interviewers")
    @PreAuthorize("!hasRole(T(com.smartrecruit.common.constant.Constants).CANDIDATE_ROLE)")
    public ApiResponse<List<UserVO>> interviewers() {
        return ApiResponse.success(userService.listInterviewers());
    }

    /**
     * 查询用户详情。
     */
    @GetMapping("/{id}")
    @PreAuthorize("!hasRole(T(com.smartrecruit.common.constant.Constants).CANDIDATE_ROLE)")
    public ApiResponse<UserDetailVO> detail(@PathVariable Long id) {
        UserDetailVO vo = userService.getById(id);
        return ApiResponse.success(vo);
    }

    /**
     * 创建用户。
     */
    @PostMapping
    @PreAuthorize("!hasRole(T(com.smartrecruit.common.constant.Constants).CANDIDATE_ROLE)")
    public ApiResponse<UserVO> create(@Valid @RequestBody CreateUserRequest request) {
        UserVO vo = userService.create(request);
        return ApiResponse.success(vo);
    }

    /**
     * 更新用户信息。
     */
    @PutMapping("/{id}")
    @PreAuthorize("!hasRole(T(com.smartrecruit.common.constant.Constants).CANDIDATE_ROLE)")
    public ApiResponse<UserVO> update(@PathVariable Long id,
                                       @Valid @RequestBody UpdateUserRequest request) {
        UserVO vo = userService.update(id, request);
        return ApiResponse.success(vo);
    }

    /**
     * 更新用户状态。
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("!hasRole(T(com.smartrecruit.common.constant.Constants).CANDIDATE_ROLE)")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
                                           @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return ApiResponse.success();
    }

    /**
     * 删除用户（逻辑删除）。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("!hasRole(T(com.smartrecruit.common.constant.Constants).CANDIDATE_ROLE)")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.success();
    }

    /**
     * 管理员重置指定用户的密码。
     */
    @PutMapping("/{id}/reset-password")
    @PreAuthorize("!hasRole(T(com.smartrecruit.common.constant.Constants).CANDIDATE_ROLE)")
    public ApiResponse<Void> resetPassword(@PathVariable Long id,
                                            @RequestBody AdminResetPasswordRequest request) {
        String password = request.getPassword();
        userService.resetPassword(id, password);
        return ApiResponse.success("密码重置成功", null);
    }
}
