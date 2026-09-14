package com.smartrecruit.system.controller;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.system.dto.request.LoginRequest;
import com.smartrecruit.system.dto.request.PhoneLoginRequest;
import com.smartrecruit.system.dto.request.PhoneSendCodeRequest;
import com.smartrecruit.system.dto.request.RegisterRequest;
import com.smartrecruit.system.dto.request.ResetPasswordRequest;
import com.smartrecruit.system.dto.request.SendCodeRequest;
import com.smartrecruit.system.dto.response.CaptchaVO;
import com.smartrecruit.system.dto.response.LoginResponse;
import com.smartrecruit.system.dto.response.UserVO;
import com.smartrecruit.system.service.AuthService;
import com.smartrecruit.system.service.CaptchaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器。
 *
 * @since 2026-04-26
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    /**
     * 用户登录。
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("登录请求: account={}", request.account());
        LoginResponse response = authService.login(request);
        return ApiResponse.success(response);
    }

    /**
     * 发送邮箱验证码。
     */
    @PostMapping("/send-code")
    public ApiResponse<Void> sendCode(@Valid @RequestBody SendCodeRequest request) {
        log.info("发送验证码请求: email={}, purpose={}", request.email(), request.purpose());
        authService.sendVerificationCode(request.email(), request.purpose());
        return ApiResponse.success("验证码已发送", null);
    }

    /**
     * 重置密码。
     */
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("密码重置请求: email={}", request.email());
        authService.resetPassword(request);
        return ApiResponse.success("密码重置成功", null);
    }

    /**
     * 用户注册。
     */
    @PostMapping("/register")
    public ApiResponse<UserVO> register(@Valid @RequestBody RegisterRequest request) {
        log.info("注册请求: email={}, username={}", request.email(), request.username());
        UserVO user = authService.register(request);
        return ApiResponse.success("注册成功", user);
    }

    /**
     * 刷新 Token。
     */
    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@RequestParam String refreshToken) {
        log.info("刷新 Token 请求");
        LoginResponse response = authService.refreshToken(refreshToken);
        return ApiResponse.success(response);
    }

    /**
     * 获取图片验证码。
     */
    @GetMapping("/captcha")
    public ApiResponse<CaptchaVO> captcha() {
        CaptchaVO captcha = captchaService.generateCaptcha();
        return ApiResponse.success(captcha);
    }

    /**
     * 发送手机短信验证码。
     */
    @PostMapping("/send-phone-code")
    public ApiResponse<Void> sendPhoneCode(@Valid @RequestBody PhoneSendCodeRequest request) {
        log.info("发送手机验证码请求: mobile={}", request.mobile());
        authService.sendPhoneCode(request.mobile());
        return ApiResponse.success("验证码已发送", null);
    }

    /**
     * 手机号验证码登录（候选人用户，首次登录自动注册）。
     */
    @PostMapping("/phone-login")
    public ApiResponse<LoginResponse> phoneLogin(@Valid @RequestBody PhoneLoginRequest request) {
        log.info("手机验证码登录请求: mobile={}", request.mobile());
        LoginResponse response = authService.phoneLogin(request);
        return ApiResponse.success(response);
    }

    /**
     * 获取当前登录用户信息。
     */
    @GetMapping("/me")
    public ApiResponse<UserVO> me() {
        UserVO user = authService.getCurrentUser();
        return ApiResponse.success(user);
    }
}
