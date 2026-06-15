import { post, get } from './request'
import type { LoginRequest, RegisterRequest, RegisterByEmailRequest, SmsCodeRequest, EmailCodeRequest, SendCodeRequest, LoginByCodeRequest, UserInfo } from './types'

interface LoginResponse {
  token: string
  userInfo: UserInfo
}

export function login(data: LoginRequest) {
  return post<LoginResponse>('auth/login', data)
}

export function loginByCode(data: LoginByCodeRequest) {
  return post<LoginResponse>('auth/login/code', data)
}

export function register(data: RegisterRequest) {
  return post<void>('auth/register/phone', data)
}

export function registerByEmail(data: RegisterByEmailRequest) {
  return post<void>('auth/register/email', data)
}

export function sendSmsCode(data: SmsCodeRequest) {
  return post<void>('auth/sms-code', data)
}

export function sendEmailCode(data: EmailCodeRequest) {
  return post<void>('auth/email-code', data)
}

export function sendVerificationCode(data: SendCodeRequest) {
  return post<void>(
    data.type === 'phone' ? 'auth/sms-code' : 'auth/email-code',
    data.type === 'phone' ? { phone: data.target } : { email: data.target }
  )
}

export function getUserInfo() {
  return get<UserInfo>('user/info')
}

export function logout() {
  return post<void>('auth/logout')
}
