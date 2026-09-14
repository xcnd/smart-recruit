package com.smartrecruit.system.service;

import com.smartrecruit.system.dto.request.LoginRequest;
import com.smartrecruit.system.dto.request.PhoneLoginRequest;
import com.smartrecruit.system.dto.request.RegisterRequest;
import com.smartrecruit.system.dto.request.ResetPasswordRequest;
import com.smartrecruit.system.dto.response.LoginResponse;
import com.smartrecruit.system.dto.response.UserVO;

/**
 * 认证服务接口。
 *
 * @since 2026-04-26
 */
public interface AuthService {

    /**
     * 用户登录，验证密码并生成 JWT。
     */
    LoginResponse login(LoginRequest request);

    /**
     * 发送邮箱验证码。
     */
    void sendVerificationCode(String email);

    /**
     * 发送邮箱验证码（支持指定用途：register / reset）。
     */
    void sendVerificationCode(String email, String purpose);

    /**
     * 用户注册，校验验证码、用户名和邮箱唯一性后创建用户。
     */
    UserVO register(RegisterRequest request);

    /**
     * 重置密码（验证码通过后更新用户密码）。
     */
    void resetPassword(ResetPasswordRequest request);

    /**
     * 刷新 Token。
     */
    LoginResponse refreshToken(String refreshToken);

    /**
     * 获取当前登录用户信息。
     */
    UserVO getCurrentUser();

    /**
     * 发送手机短信验证码。
     */
    void sendPhoneCode(String mobile);

    /**
     * 手机号验证码登录（若手机号未注册则自动注册并返回 JWT）。
     */
    LoginResponse phoneLogin(PhoneLoginRequest request);
}
