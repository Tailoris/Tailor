import axios, { AxiosError, AxiosInstance, AxiosRequestConfig, InternalAxiosRequestConfig } from 'axios';
import { ElMessage } from 'element-plus';
import type { ApiResponse } from '@/types';
import { decryptSync } from '@/utils/crypto';

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

// 🔒 F-H05: 重试配置
const MAX_RETRY_COUNT = 3;
const RETRY_DELAY_MS = 1000;

// 🔒 F-H05: 不可重试的状态码
const NON_RETRYABLE_STATUS = new Set([400, 401, 403, 404, 422]);

// 🔒 F-H05: 不可重试的请求方法
const NON_RETRYABLE_METHODS = new Set(['post', 'put', 'delete', 'patch']);

interface RetryConfig extends InternalAxiosRequestConfig {
  __retryCount?: number;
}

interface CustomAxiosRequestConfig extends AxiosRequestConfig {
  showError?: boolean;
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

/**
 * 🔒 F-H05: 延迟函数
 */
function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 30000,
  responseEncoding: 'utf8',
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
    Accept: 'application/json;charset=UTF-8',
  },
});

request.interceptors.request.use(
  (config) => {
    const token = decryptSync(localStorage.getItem('token') || '');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    config.headers['X-CSRF-Token'] = generateCsrfToken();
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

request.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResponse<unknown>;
    const showError = (response.config as CustomAxiosRequestConfig).showError !== false;
    if (res.code !== 200) {
      const errorMsg = res.message || '请求失败';
      if (showError && shouldShowError(errorMsg)) {
        ElMessage.error(errorMsg);
      }
      if (res.code === 401) {
        localStorage.removeItem('token');
        window.location.href = '/login';
      }
      return Promise.reject(new Error(res.message || '请求失败'));
    }
    return res.data as any;
  },
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const showError = (error.config as CustomAxiosRequestConfig).showError !== false;
    // 🔒 F-H05: 自动重试逻辑
    const config = error.config as RetryConfig | undefined;
    if (config && shouldRetry(error, config)) {
      config.__retryCount = (config.__retryCount || 0) + 1;
      const delay = RETRY_DELAY_MS * config.__retryCount;
      await sleep(delay);
      return request.request(config);
    }

    if (error.response) {
      const { status, data } = error.response;
      const message = data?.message || '';

      if (message.toLowerCase().includes('csrf')) {
        if (showError && shouldShowError('安全验证失败，请刷新页面重试')) {
          ElMessage.error('安全验证失败，请刷新页面重试');
        }
        return Promise.reject(error);
      }

      switch (status) {
        case 401:
          if (showError && shouldShowError('登录已过期，请重新登录')) {
            ElMessage.error('登录已过期，请重新登录');
          }
          localStorage.removeItem('token');
          window.location.href = '/login';
          break;
        case 403:
          if (showError && shouldShowError('权限不足，请联系管理员')) {
            ElMessage.error('权限不足，请联系管理员');
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
          if (showError && shouldShowError('服务器内部错误，请稍后重试')) {
            ElMessage.error('服务器内部错误，请稍后重试');
          }
          break;
        case 502:
          if (showError && shouldShowError('网关错误，服务暂时不可用')) {
            ElMessage.error('网关错误，服务暂时不可用');
          }
          break;
        case 503:
          if (showError && shouldShowError('服务暂时不可用，请检查网络或稍后重试')) {
            ElMessage.error('服务暂时不可用，请检查网络或稍后重试');
          }
          break;
        case 504:
          if (showError && shouldShowError('网关超时，请稍后重试')) {
            ElMessage.error('网关超时，请稍后重试');
          }
          break;
        default:
          if (showError && shouldShowError(data?.message || `请求失败 (${status})`)) {
            ElMessage.error(data?.message || `请求失败 (${status})`);
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

/**
 * 🔒 F-H05: 判断是否应该重试
 */
function shouldRetry(error: AxiosError, config: RetryConfig): boolean {
  if ((config.__retryCount || 0) >= MAX_RETRY_COUNT) {
    return false;
  }
  const method = (config.method || 'get').toLowerCase();
  if (NON_RETRYABLE_METHODS.has(method)) {
    return false;
  }
  if (!error.response) {
    return true;
  }
  const status = error.response.status;
  if (NON_RETRYABLE_STATUS.has(status)) {
    return false;
  }
  return status >= 500;
}

export default request;
