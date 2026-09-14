import request from './request'
import axios from 'axios'
import type { CaptchaVO, LoginDTO, LoginResultVO, PhoneLoginDTO, PhoneSendCodeDTO, RegisterDTO, ResetPasswordDTO, SendCodeDTO, UserVO } from '@/types/models'

export function getCaptcha(): Promise<CaptchaVO> {
  return request.get('/auth/captcha')
}

export function login(data: LoginDTO): Promise<LoginResultVO> {
  return request.post('/auth/login', data)
}

export function sendVerificationCode(data: SendCodeDTO): Promise<void> {
  return request.post('/auth/send-code', data)
}

export function register(data: RegisterDTO): Promise<UserVO> {
  return request.post('/auth/register', data)
}

export function resetPassword(data: ResetPasswordDTO): Promise<void> {
  return request.post('/auth/reset-password', data)
}

/**
 * 静默刷新 token（使用原始 axios 实例，避免拦截器循环）。
 * 后端期望: POST /api/v1/auth/refresh?refreshToken=xxx
 */
export function refresh(refreshToken: string): Promise<LoginResultVO> {
  return axios
    .post('/api/v1/auth/refresh', null, { params: { refreshToken } })
    .then((res) => res.data.data as LoginResultVO)
}

export function getMe(): Promise<UserVO> {
  return request.get('/auth/me')
}

export function logout(): Promise<void> {
  return request.post('/auth/logout')
}

/**
 * 发送手机短信验证码。
 * POST /api/v1/auth/send-phone-code
 */
export function sendPhoneCode(data: PhoneSendCodeDTO): Promise<void> {
  return request.post('/auth/send-phone-code', data)
}

/**
 * 手机号验证码登录（若手机号未注册则自动注册）。
 * POST /api/v1/auth/phone-login
 */
export function phoneLogin(data: PhoneLoginDTO): Promise<LoginResultVO> {
  return request.post('/auth/phone-login', data)
}
