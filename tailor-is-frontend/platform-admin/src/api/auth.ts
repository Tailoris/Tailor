import request from './request';

interface LoginParams {
  username: string;
  password: string;
}

interface SendCodeParams {
  target: string;
  type: 'phone' | 'email';
}

interface AdminUserInfo {
  id: number;
  username: string;
  phone: string;
  email: string;
  realName: string;
  roles: string[];
  permissions: string[];
}

interface LoginResult {
  token: string;
  userInfo: AdminUserInfo;
}

export function apiLogin(data: LoginParams) {
  // 🔒 M-23修复: 替换 any 为 unknown，保持类型安全
  return request<unknown, LoginResult>({
    url: '/auth/login',
    method: 'post',
    data,
  });
}

export function sendVerificationCode(target: string, type: 'phone' | 'email') {
  // 🔒 M-23修复: 替换 any 为 unknown，保持类型安全
  return request<unknown, void>({
    url: type === 'phone' ? '/auth/sms-code' : '/auth/email-code',
    method: 'post',
    data: type === 'phone' ? { phone: target } : { email: target },
  });
}

export function loginByCode(data: { target: string; code: string; type: 'phone' | 'email' }) {
  // 🔒 M-23修复: 替换 any 为 unknown，保持类型安全
  return request<unknown, LoginResult>({
    url: '/auth/login/code',
    method: 'post',
    data,
  });
}

export function sendResetCode(data: SendCodeParams) {
  // 🔒 M-23修复: 替换 any 为 unknown，保持类型安全
  const { target, type } = data;
  if (type === 'phone') {
    return request<unknown, void>({
      url: '/auth/sms-code',
      method: 'post',
      data: { phone: target },
    });
  } else {
    return request<unknown, void>({
      url: '/auth/email-code',
      method: 'post',
      data: { email: target },
    });
  }
}

export function resetPassword(data: { target: string; code: string; newPassword: string; type: 'phone' | 'email' }) {
  // 🔒 M-23修复: 替换 any 为 unknown，保持类型安全
  return request<unknown, void>({
    url: '/auth/reset-password',
    method: 'post',
    data,
  });
}
