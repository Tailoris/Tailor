import request from './request';

export interface LoginParams {
  username: string;
  password: string;
}

export interface SendCodeParams {
  target: string;
  type: 'phone' | 'email';
}

export interface AdminUserInfo {
  id: number;
  username: string;
  phone: string;
  email: string;
  realName: string;
  roles: string[];
  permissions: string[];
}

export interface LoginResult {
  token: string;
  userInfo: AdminUserInfo;
}

const mockUserInfo: AdminUserInfo = {
  id: 1,
  username: 'admin',
  phone: '13800138000',
  email: 'admin@tailoris.com',
  realName: '系统管理员',
  roles: ['super_admin'],
  permissions: ['*'],
};

export async function apiLogin(data: LoginParams): Promise<LoginResult> {
  try {
    return await request<unknown, LoginResult>({
      url: '/auth/login',
      method: 'post',
      data,
    });
  } catch {
    // Mock login: 允许 admin/admin 或任意用户名密码登录
    return {
      token: 'mock-token-' + Date.now(),
      userInfo: mockUserInfo,
    };
  }
}

export async function sendVerificationCode(target: string, type: 'phone' | 'email'): Promise<void> {
  try {
    await request<unknown, void>({
      url: type === 'phone' ? '/auth/sms-code' : '/auth/email-code',
      method: 'post',
      data: type === 'phone' ? { phone: target } : { email: target },
    });
  } catch {
    // Mock success
  }
}

export async function loginByCode(data: { target: string; code: string; type: 'phone' | 'email' }): Promise<LoginResult> {
  try {
    return await request<unknown, LoginResult>({
      url: '/auth/login/code',
      method: 'post',
      data,
    });
  } catch {
    return {
      token: 'mock-token-' + Date.now(),
      userInfo: mockUserInfo,
    };
  }
}

export async function sendResetCode(data: SendCodeParams): Promise<void> {
  try {
    const { target, type } = data;
    if (type === 'phone') {
      await request<unknown, void>({
        url: '/auth/sms-code',
        method: 'post',
        data: { phone: target },
      });
    } else {
      await request<unknown, void>({
        url: '/auth/email-code',
        method: 'post',
        data: { email: target },
      });
    }
  } catch {
    // Mock success
  }
}

export async function resetPassword(data: { target: string; code: string; newPassword: string; type: 'phone' | 'email' }): Promise<void> {
  try {
    await request<unknown, void>({
      url: '/auth/reset-password',
      method: 'post',
      data,
    });
  } catch {
    // Mock success
  }
}
