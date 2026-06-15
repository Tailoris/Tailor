import request from './request'
import type { User } from '@/types'

interface LoginParams {
  username: string
  password: string
}

interface RegisterParams {
  phone: string
  code: string
  password: string
}

interface EmailRegisterParams {
  email: string
  code: string
  password: string
}

interface SendCodeParams {
  target: string
  type: 'phone' | 'email'
}

export function login(data: LoginParams) {
  return request<any, { token: string; user: User }>({
    url: '/auth/login',
    method: 'post',
    data
  })
}

/** 手机号注册 */
export function register(data: RegisterParams) {
  return request<any, boolean>({
    url: '/auth/register',
    method: 'post',
    data
  })
}

/** 邮箱注册 */
export function registerByEmail(data: EmailRegisterParams) {
  return request<any, boolean>({
    url: '/auth/register/email',
    method: 'post',
    data
  })
}

export function getUserInfo() {
  return request<any, User>({
    url: '/auth/userinfo',
    method: 'get'
  })
}

export function logout() {
  return request<any, void>({
    url: '/auth/logout',
    method: 'post'
  })
}

/** 发送验证码（自动区分手机/邮箱） */
export function sendSmsCode(phone: string) {
  return request<any, boolean>({
    url: '/auth/sms-code',
    method: 'post',
    data: { phone }
  })
}

/** 发送邮箱验证码 */
export function sendEmailCode(email: string) {
  return request<any, boolean>({
    url: '/auth/email-code',
    method: 'post',
    data: { email }
  })
}

/** 统一验证码发送 - 根据类型自动分发 */
export function sendVerificationCode(target: string, type: 'phone' | 'email') {
  return request<any, boolean>({
    url: type === 'phone' ? '/auth/sms-code' : '/auth/email-code',
    method: 'post',
    data: type === 'phone' ? { phone: target } : { email: target }
  })
}

/** 验证码免密登录 */
export function loginByCode(data: { target: string; code: string; type: 'phone' | 'email' }) {
  return request<any, { token: string; user: User }>({
    url: '/auth/login/code',
    method: 'post',
    data
  })
}

/** 找回密码 - 发送验证码 */
export function sendResetCode(data: SendCodeParams) {
  return request<any, boolean>({
    url: '/auth/reset/code',
    method: 'post',
    data
  })
}

/** 重置密码 */
export function resetPassword(data: { target: string; code: string; newPassword: string; type: 'phone' | 'email' }) {
  return request<any, boolean>({
    url: '/auth/reset-password',
    method: 'post',
    data
  })
}
