package com.smartrecruit.system.service;

import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.system.dto.request.CreateUserRequest;
import com.smartrecruit.system.dto.request.UpdateProfileRequest;
import com.smartrecruit.system.dto.request.UpdateUserRequest;
import com.smartrecruit.system.dto.request.UserPageQuery;
import com.smartrecruit.system.dto.response.UserDetailVO;
import com.smartrecruit.system.dto.response.UserStatsVO;
import com.smartrecruit.system.dto.response.UserVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 用户管理服务接口。
 *
 * @since 2026-04-26
 */
public interface UserService {

    /**
     * 分页查询用户列表。
     */
    PageResult<UserVO> pageQuery(UserPageQuery query);

    /**
     * 查询拥有「面试官」角色的用户列表（面试安排时选择面试官）。
     *
     * @return 启用状态的面试官用户
     */
    List<UserVO> listInterviewers();

    /**
     * 根据 ID 查询用户详情。
     */
    UserDetailVO getById(Long id);

    /**
     * 创建用户（BCrypt 加密密码）。
     */
    UserVO create(CreateUserRequest request);

    /**
     * 更新用户信息。
     */
    UserVO update(Long id, UpdateUserRequest request);

    /**
     * 更新用户状态。
     */
    void updateStatus(Long id, Integer status);

    /**
     * 删除用户（逻辑删除）。
     */
    void delete(Long id);

    /**
     * 获取当前登录用户的详细信息（自主查看）。
     */
    UserDetailVO getCurrentUserProfile();

    /**
     * 当前登录用户更新自己的个人信息。
     */
    UserVO updateCurrentUserProfile(UpdateProfileRequest request);

    /**
     * 当前登录用户修改密码。
     */
    void changePassword(String oldPassword, String newPassword);

    /**
     * 当前登录用户修改邮箱。
     */
    void changeEmail(String newEmail, String verificationCode);

    /**
     * 当前登录用户上传头像。
     */
    String uploadAvatar(MultipartFile file);

    /**
     * 管理员重置指定用户的密码。
     */
    void resetPassword(Long id, String newPassword);

    /**
     * 获取用户统计数据（总数、正常、冻结、禁用）。
     */
    UserStatsVO getUserStats();
}
