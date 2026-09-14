import request from './request'
import type { PhoneLoginDTO, PhoneSendCodeDTO, LoginResultVO, UserVO } from '@/types/models'

/** 发送手机短信验证码。POST /api/v1/auth/send-phone-code */
export function sendPhoneCode(data: PhoneSendCodeDTO): Promise<void> {
  return request.post('/auth/send-phone-code', data)
}

/** 手机号验证码登录（若手机号未注册则自动注册）。POST /api/v1/auth/phone-login */
export function phoneLogin(data: PhoneLoginDTO): Promise<LoginResultVO> {
  return request.post('/auth/phone-login', data)
}

export function getMe(): Promise<UserVO> {
  return request.get('/auth/me')
}

export function logout(): Promise<void> {
  return request.post('/auth/logout')
}
