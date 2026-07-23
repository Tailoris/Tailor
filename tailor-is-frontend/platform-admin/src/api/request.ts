import axios, { AxiosInstance, AxiosResponse, AxiosError, AxiosRequestConfig, InternalAxiosRequestConfig } from 'axios';
import { ElMessage } from 'element-plus';
import { decryptSync, encryptSync } from '@/utils/crypto';

// 扩展 AxiosRequestConfig 类型，添加 showError 属性
declare module 'axios' {
  interface AxiosRequestConfig {
    showError?: boolean;
  }
}

// 延迟引用 router 以避免循环依赖（request.ts → router → LoginView → auth.ts → request.ts）
// eslint-disable-next-line @typescript-eslint/no-explicit-any
let _router: any = null;
export function injectRouter(router: any) {
  _router = router;
}

export interface ApiResponse<T = unknown> {
  code: number;
  message: string;
  data?: T | null;
}

// 错误消息节流配置
const ERROR_MESSAGE_CACHE = new Map<string, number>();
const ERROR_THROTTLE_MS = 3000;

function shouldShowError(message: string): boolean {
  const now = Date.now();
  const lastShown = ERROR_MESSAGE_CACHE.get(message);
  if (lastShown && now - lastShown < ERROR_THROTTLE_MS) {
    return false;
  }
  ERROR_MESSAGE_CACHE.set(message, now);
  setTimeout(() => {
    ERROR_MESSAGE_CACHE.delete(message);
  }, ERROR_THROTTLE_MS);
  return true;
}

function generateCsrfToken(): string {
  const stored = localStorage.getItem('csrf_token');
  if (stored) return stored;
  const token = generateSecureToken();
  if (token) {
    localStorage.setItem('csrf_token', token);
  }
  return token;
}

/**
 * 使用 crypto.getRandomValues 生成安全的 CSRF Token
 */
function generateSecureToken(): string {
  if (typeof crypto !== 'undefined' && crypto.getRandomValues) {
    const array = new Uint8Array(32);
    crypto.getRandomValues(array);
    return Array.from(array, (b) => b.toString(16).padStart(2, '0')).join('');
  }
  return '';
}

const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 15000,
  responseEncoding: 'utf8',
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
    Accept: 'application/json;charset=UTF-8',
  },
});

request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 🔒 使用加密存储读取Token
    const token = decryptSync(localStorage.getItem('token') || '');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    config.headers['X-CSRF-Token'] = generateCsrfToken();
    return config;
  },
  (error: AxiosError) => Promise.reject(error)
);

request.interceptors.response.use(
  (response: AxiosResponse): any => {
    const apiResponse = response.data as Record<string, unknown> | null;
    const showError = response.config.showError !== false;
    if (apiResponse && typeof apiResponse === 'object' && 'code' in apiResponse) {
      const { code, message, data } = apiResponse as unknown as ApiResponse;
      if (code === 200 || code === 0) {
        return data;
      }
      if (showError && shouldShowError(message || '请求失败')) {
        ElMessage.error(message || '请求失败');
      }
      return Promise.reject(new Error(message));
    }
    return response.data;
  },
  (error: AxiosError<ApiResponse>) => {
    const showError = error.config?.showError !== false;
    if (error.response) {
      const { status } = error.response;
      const responseData = error.response.data;
      const errMessage = responseData?.message || '';

      if (errMessage.toLowerCase().includes('csrf')) {
        if (showError && shouldShowError('安全验证失败，请刷新页面重试')) {
          ElMessage.error('安全验证失败，请刷新页面重试');
        }
        return Promise.reject(error);
      }

      switch (status) {
        case 401:
          localStorage.removeItem('token');
          if (showError && shouldShowError('请先登录')) {
            ElMessage.error('请先登录');
          }
          _router?.push('/login');
          break;
        case 403:
          if (showError && shouldShowError('权限不足，无法访问此资源')) {
            ElMessage.error('权限不足，无法访问此资源');
          }
          break;
        case 404:
          if (showError && shouldShowError('请求的资源不存在')) {
            ElMessage.error('请求的资源不存在');
          }
          break;
        case 429:
          if (showError && shouldShowError('请求过于频繁，请稍后再试')) {
            ElMessage.error('请求过于频繁，请稍后再试');
          }
          break;
        case 500:
          if (showError && shouldShowError('服务器内部错误')) {
            ElMessage.error('服务器内部错误');
          }
          break;
        case 502:
          if (showError && shouldShowError('网关错误，服务暂时不可用')) {
            ElMessage.error('网关错误，服务暂时不可用');
          }
          break;
        case 503:
          if (showError && shouldShowError('服务暂时不可用')) {
            ElMessage.error('服务暂时不可用');
          }
          break;
        case 504:
          if (showError && shouldShowError('网关超时，请稍后重试')) {
            ElMessage.error('网关超时，请稍后重试');
          }
          break;
        default:
          if (showError && shouldShowError(errMessage || `请求失败 (${status})`)) {
            ElMessage.error(errMessage || `请求失败 (${status})`);
          }
      }
    } else if (error.code === 'ECONNABORTED') {
      if (showError && shouldShowError('请求超时，请稍后重试')) {
        ElMessage.error('请求超时，请稍后重试');
      }
    } else {
      if (showError && shouldShowError('网络连接异常，请检查网络')) {
        ElMessage.error('网络连接异常，请检查网络');
      }
    }
    return Promise.reject(error);
  }
);

export default request;
