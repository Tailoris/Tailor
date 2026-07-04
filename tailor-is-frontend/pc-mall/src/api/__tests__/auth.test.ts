import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import axios from 'axios'
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
  resetPassword
} from '../auth'

vi.mock('axios')

describe('auth API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should login successfully', async () => {
    const mockResponse = {
      token: 'test-token',
      userInfo: { id: 1, username: 'test', nickname: 'Test User' },
      csrfToken: 'csrf-token'
    }
    ;(axios.create as vi.Mock).mockReturnValue({
      interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
      request: vi.fn().mockResolvedValue({
        data: { code: 200, message: 'success', data: mockResponse }
      })
    })

    const result = await login({ username: 'test', password: 'password' })
    expect(result).toEqual(mockResponse)
    expect(result.token).toBe('test-token')
    expect(result.userInfo.id).toBe(1)
  })

  it('should register by phone successfully', async () => {
    ;(axios.create as vi.Mock).mockReturnValue({
      interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
      request: vi.fn().mockResolvedValue({
        data: { code: 200, message: 'success', data: true }
      })
    })

    const result = await register({ phone: '13800138000', smsCode: '1234', password: 'password' })
    expect(result).toBe(true)
  })

  it('should register by email successfully', async () => {
    ;(axios.create as vi.Mock).mockReturnValue({
      interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
      request: vi.fn().mockResolvedValue({
        data: { code: 200, message: 'success', data: true }
      })
    })

    const result = await registerByEmail({ email: 'test@example.com', code: '1234', password: 'password' })
    expect(result).toBe(true)
  })

  it('should get user info successfully', async () => {
    const mockUser = { id: 1, username: 'test', nickname: 'Test User', email: 'test@example.com' }
    ;(axios.create as vi.Mock).mockReturnValue({
      interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
      request: vi.fn().mockResolvedValue({
        data: { code: 200, message: 'success', data: mockUser }
      })
    })

    const result = await getUserInfo()
    expect(result).toEqual(mockUser)
    expect(result.username).toBe('test')
  })

  it('should logout successfully', async () => {
    ;(axios.create as vi.Mock).mockReturnValue({
      interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
      request: vi.fn().mockResolvedValue({
        data: { code: 200, message: 'success', data: undefined }
      })
    })

    const result = await logout()
    expect(result).toBeUndefined()
  })

  it('should send SMS code successfully', async () => {
    ;(axios.create as vi.Mock).mockReturnValue({
      interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
      request: vi.fn().mockResolvedValue({
        data: { code: 200, message: 'success', data: true }
      })
    })

    const result = await sendSmsCode('13800138000')
    expect(result).toBe(true)
  })

  it('should send email code successfully', async () => {
    ;(axios.create as vi.Mock).mockReturnValue({
      interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
      request: vi.fn().mockResolvedValue({
        data: { code: 200, message: 'success', data: true }
      })
    })

    const result = await sendEmailCode('test@example.com')
    expect(result).toBe(true)
  })

  it('should send verification code by phone', async () => {
    ;(axios.create as vi.Mock).mockReturnValue({
      interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
      request: vi.fn().mockResolvedValue({
        data: { code: 200, message: 'success', data: true }
      })
    })

    const result = await sendVerificationCode('13800138000', 'phone')
    expect(result).toBe(true)
  })

  it('should send verification code by email', async () => {
    ;(axios.create as vi.Mock).mockReturnValue({
      interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
      request: vi.fn().mockResolvedValue({
        data: { code: 200, message: 'success', data: true }
      })
    })

    const result = await sendVerificationCode('test@example.com', 'email')
    expect(result).toBe(true)
  })

  it('should login by code successfully', async () => {
    const mockResponse = {
      token: 'test-token',
      userInfo: { id: 1, username: 'test', nickname: 'Test User' }
    }
    ;(axios.create as vi.Mock).mockReturnValue({
      interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
      request: vi.fn().mockResolvedValue({
        data: { code: 200, message: 'success', data: mockResponse }
      })
    })

    const result = await loginByCode({ target: '13800138000', code: '1234', type: 'phone' })
    expect(result).toEqual(mockResponse)
    expect(result.token).toBe('test-token')
  })

  it('should send reset code by phone', async () => {
    ;(axios.create as vi.Mock).mockReturnValue({
      interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
      request: vi.fn().mockResolvedValue({
        data: { code: 200, message: 'success', data: true }
      })
    })

    const result = await sendResetCode({ target: '13800138000', type: 'phone' })
    expect(result).toBe(true)
  })

  it('should send reset code by email', async () => {
    ;(axios.create as vi.Mock).mockReturnValue({
      interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
      request: vi.fn().mockResolvedValue({
        data: { code: 200, message: 'success', data: true }
      })
    })

    const result = await sendResetCode({ target: 'test@example.com', type: 'email' })
    expect(result).toBe(true)
  })

  it('should reset password successfully', async () => {
    ;(axios.create as vi.Mock).mockReturnValue({
      interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
      request: vi.fn().mockResolvedValue({
        data: { code: 200, message: 'success', data: true }
      })
    })

    const result = await resetPassword({ target: '13800138000', code: '1234', newPassword: 'newpassword', type: 'phone' })
    expect(result).toBe(true)
  })
})