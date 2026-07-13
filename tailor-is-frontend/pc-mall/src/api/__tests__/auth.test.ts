import { vi, describe, it, expect, beforeEach } from 'vitest';

vi.mock('../request', () => ({
  default: vi.fn().mockResolvedValue({}),
}));

vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn(),
    success: vi.fn(),
    warning: vi.fn(),
    info: vi.fn(),
  },
}));

import request from '../request';
import {
  login,
  register,
  registerByEmail,
  getUserInfo,
  logout,
  sendSmsCode,
  sendEmailCode,
  sendVerificationCode,
  loginByCode,
  sendResetCode,
  resetPassword,
} from '../auth';

describe('auth API', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('login', () => {
    it('should login successfully with valid credentials', async () => {
      const mockResponse = {
        token: 'test-token',
        userInfo: { id: 1, username: 'test', nickname: 'Test User' },
        csrfToken: 'csrf-token',
      };
      (request as vi.Mock).mockResolvedValue(mockResponse);

      const result = await login({ username: 'test', password: 'password' });
      expect(result).toEqual(mockResponse);
      expect(result.token).toBe('test-token');
    });

    it('should handle login failure - invalid credentials', async () => {
      (request as vi.Mock).mockRejectedValue(new Error('账号或密码错误'));

      await expect(login({ username: 'wrong', password: 'wrong' })).rejects.toThrow('账号或密码错误');
    });

    it('should handle login failure - account locked', async () => {
      (request as vi.Mock).mockRejectedValue(new Error('账号已被锁定'));

      await expect(login({ username: 'locked', password: 'password' })).rejects.toThrow('账号已被锁定');
    });

    it('should handle login failure - network error', async () => {
      (request as vi.Mock).mockRejectedValue(new Error('Network Error'));

      await expect(login({ username: 'test', password: 'password' })).rejects.toThrow('Network Error');
    });

    it('should handle login failure - request timeout', async () => {
      (request as vi.Mock).mockRejectedValue(new Error('ECONNABORTED'));

      await expect(login({ username: 'test', password: 'password' })).rejects.toThrow('ECONNABORTED');
    });
  });

  describe('register', () => {
    it('should register successfully with valid data', async () => {
      (request as vi.Mock).mockResolvedValue(true);

      const result = await register({ phone: '13800138000', smsCode: '1234', password: 'password' });
      expect(result).toBe(true);
    });

    it('should handle register failure - phone already exists', async () => {
      (request as vi.Mock).mockRejectedValue(new Error('手机号已被注册'));

      await expect(register({ phone: '13800138000', smsCode: '1234', password: 'password' })).rejects.toThrow(
        '手机号已被注册'
      );
    });

    it('should handle register failure - invalid sms code', async () => {
      (request as vi.Mock).mockRejectedValue(new Error('验证码错误'));

      await expect(register({ phone: '13800138000', smsCode: '9999', password: 'password' })).rejects.toThrow(
        '验证码错误'
      );
    });

    it('should handle register failure - password too weak', async () => {
      (request as vi.Mock).mockRejectedValue(new Error('密码强度不足'));

      await expect(register({ phone: '13800138000', smsCode: '1234', password: '123' })).rejects.toThrow(
        '密码强度不足'
      );
    });

    it('should handle register failure - missing required fields', async () => {
      (request as vi.Mock).mockRejectedValue(new Error('必填项缺失'));

      await expect(register({ phone: '', smsCode: '', password: '' } as any)).rejects.toThrow('必填项缺失');
    });
  });

  describe('registerByEmail', () => {
    it('should register by email successfully', async () => {
      (request as vi.Mock).mockResolvedValue(true);

      const result = await registerByEmail({ email: 'test@example.com', code: '1234', password: 'password' });
      expect(result).toBe(true);
    });

    it('should handle email registration failure - email already exists', async () => {
      (request as vi.Mock).mockRejectedValue(new Error('邮箱已被注册'));

      await expect(registerByEmail({ email: 'test@example.com', code: '1234', password: 'password' })).rejects.toThrow(
        '邮箱已被注册'
      );
    });
  });

  describe('getUserInfo', () => {
    it('should get user info successfully', async () => {
      const mockUser = { id: 1, username: 'test', nickname: 'Test User' };
      (request as vi.Mock).mockResolvedValue(mockUser);

      const result = await getUserInfo();
      expect(result).toEqual(mockUser);
    });

    it('should handle token expired error', async () => {
      (request as vi.Mock).mockRejectedValue(new Error('登录已过期'));

      await expect(getUserInfo()).rejects.toThrow('登录已过期');
    });

    it('should handle permission denied error', async () => {
      (request as vi.Mock).mockRejectedValue(new Error('没有权限访问'));

      await expect(getUserInfo()).rejects.toThrow('没有权限访问');
    });
  });

  describe('logout', () => {
    it('should logout successfully', async () => {
      (request as vi.Mock).mockResolvedValue(undefined);

      const result = await logout();
      expect(result).toBeUndefined();
    });

    it('should handle logout failure - session timeout', async () => {
      (request as vi.Mock).mockRejectedValue(new Error('会话已超时'));

      await expect(logout()).rejects.toThrow('会话已超时');
    });
  });

  describe('sendVerificationCode', () => {
    it('should send sms code successfully', async () => {
      (request as vi.Mock).mockResolvedValue(true);

      const result = await sendVerificationCode('13800138000', 'phone');
      expect(result).toBe(true);
    });

    it('should send email code successfully', async () => {
      (request as vi.Mock).mockResolvedValue(true);

      const result = await sendVerificationCode('test@example.com', 'email');
      expect(result).toBe(true);
    });

    it('should handle rate limit error', async () => {
      (request as vi.Mock).mockRejectedValue(new Error('请求过于频繁'));

      await expect(sendVerificationCode('13800138000', 'phone')).rejects.toThrow('请求过于频繁');
    });
  });

  describe('sendSmsCode', () => {
    it('should send sms code successfully', async () => {
      (request as vi.Mock).mockResolvedValue(true);

      const result = await sendSmsCode('13800138000');
      expect(result).toBe(true);
    });

    it('should handle invalid phone number', async () => {
      (request as vi.Mock).mockRejectedValue(new Error('手机号格式错误'));

      await expect(sendSmsCode('invalid-phone')).rejects.toThrow('手机号格式错误');
    });
  });

  describe('sendEmailCode', () => {
    it('should send email code successfully', async () => {
      (request as vi.Mock).mockResolvedValue(true);

      const result = await sendEmailCode('test@example.com');
      expect(result).toBe(true);
    });

    it('should handle invalid email format', async () => {
      (request as vi.Mock).mockRejectedValue(new Error('邮箱格式错误'));

      await expect(sendEmailCode('invalid-email')).rejects.toThrow('邮箱格式错误');
    });
  });

  describe('loginByCode', () => {
    it('should login by phone code successfully', async () => {
      const mockResponse = {
        token: 'test-token',
        userInfo: { id: 1, username: 'test', nickname: 'Test User' },
        csrfToken: 'csrf-token',
      };
      (request as vi.Mock).mockResolvedValue(mockResponse);

      const result = await loginByCode({ target: '13800138000', code: '1234', type: 'phone' });
      expect(result).toEqual(mockResponse);
    });

    it('should login by email code successfully', async () => {
      const mockResponse = {
        token: 'test-token',
        userInfo: { id: 1, username: 'test', nickname: 'Test User' },
        csrfToken: 'csrf-token',
      };
      (request as vi.Mock).mockResolvedValue(mockResponse);

      const result = await loginByCode({ target: 'test@example.com', code: '1234', type: 'email' });
      expect(result).toEqual(mockResponse);
    });

    it('should handle invalid verification code', async () => {
      (request as vi.Mock).mockRejectedValue(new Error('验证码错误'));

      await expect(loginByCode({ target: '13800138000', code: '9999', type: 'phone' })).rejects.toThrow('验证码错误');
    });
  });

  describe('sendResetCode', () => {
    it('should send reset code via phone successfully', async () => {
      (request as vi.Mock).mockResolvedValue(true);

      const result = await sendResetCode({ target: '13800138000', type: 'phone' });
      expect(result).toBe(true);
    });

    it('should send reset code via email successfully', async () => {
      (request as vi.Mock).mockResolvedValue(true);

      const result = await sendResetCode({ target: 'test@example.com', type: 'email' });
      expect(result).toBe(true);
    });
  });

  describe('resetPassword', () => {
    it('should reset password successfully', async () => {
      (request as vi.Mock).mockResolvedValue(true);

      const result = await resetPassword({
        target: '13800138000',
        code: '1234',
        newPassword: 'newpassword',
        type: 'phone',
      });
      expect(result).toBe(true);
    });

    it('should handle password reset failure - invalid code', async () => {
      (request as vi.Mock).mockRejectedValue(new Error('验证码错误'));

      await expect(
        resetPassword({ target: '13800138000', code: '9999', newPassword: 'newpassword', type: 'phone' })
      ).rejects.toThrow('验证码错误');
    });

    it('should handle password reset failure - password mismatch', async () => {
      (request as vi.Mock).mockRejectedValue(new Error('两次密码不一致'));

      await expect(
        resetPassword({ target: '13800138000', code: '1234', newPassword: 'short', type: 'phone' })
      ).rejects.toThrow('两次密码不一致');
    });
  });
});
