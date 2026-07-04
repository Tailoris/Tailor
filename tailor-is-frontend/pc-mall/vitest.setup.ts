import { vi } from 'vitest'

vi.mock('axios', () => {
  const mockInterceptors = {
    request: { use: vi.fn(), eject: vi.fn() },
    response: { use: vi.fn(), eject: vi.fn() }
  }
  const mockAxiosInstance = {
    interceptors: mockInterceptors,
    request: vi.fn(),
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
  return {
    default: {
      create: vi.fn(() => mockAxiosInstance),
      get: vi.fn(),
      post: vi.fn(),
      put: vi.fn(),
      delete: vi.fn()
    },
    AxiosInstance: {},
    AxiosResponse: {},
    AxiosError: {},
    AxiosRequestConfig: {},
    InternalAxiosRequestConfig: {}
  }
})