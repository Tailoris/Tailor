import axios, { AxiosInstance, AxiosResponse, AxiosError, AxiosRequestConfig, InternalAxiosRequestConfig } from 'axios';
import { ElMessage } from 'element-plus';
import router from '@/router';
import { decryptSync, encryptSync } from '@/utils/crypto';

export interface ApiResponse<T = unknown> {
  code: number;
  message: string;
  data?: T | null;
}

interface CustomAxiosRequestConfig extends AxiosRequestConfig {
  showError?: boolean;
}

interface RefreshResponse {
  token: string;
  refreshToken?: string;
  expiresIn?: number;
}

interface PendingRequest {
  resolve: (_: unknown) => void;
  reject: (_?: unknown) => void;
}

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

function generateSecureToken(): string {
  if (typeof crypto !== 'undefined' && crypto.getRandomValues) {
    const array = new Uint8Array(32);
    crypto.getRandomValues(array);
    return Array.from(array, (b) => b.toString(16).padStart(2, '0')).join('');
  }
  return '';
}

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000,
  responseEncoding: 'utf8',
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
    Accept: 'application/json;charset=UTF-8',
  },
});

let isRefreshing = false;
const pendingRequests: PendingRequest[] = [];

function isPublicPage(): boolean {
  const currentPath = router.currentRoute.value.path;
  return currentPath === '/login' || currentPath === '/register';
}

function addPendingRequest(request: PendingRequest): void {
  pendingRequests.push(request);
}

function processPendingRequests(token: string | null, error: unknown = null): void {
  pendingRequests.forEach(({ resolve, reject }) => {
    if (error) {
      reject(error);
    } else if (token) {
      resolve(token);
    }
  });
  pendingRequests.length = 0;
}

async function refreshToken(): Promise<string | null> {
  try {
    const refreshTokenValue = sessionStorage.getItem('refresh_token');
    if (!refreshTokenValue) {
      return null;
    }
    const response = await service.post<ApiResponse<RefreshResponse>>(
      '/api/v1/auth/refresh',
      { refreshToken: refreshTokenValue },
      { showError: false } as CustomAxiosRequestConfig
    );
    if (response.data.code === 200 && response.data.data?.token) {
      const newToken = response.data.data.token;
      localStorage.setItem('token', encryptSync(newToken));
      if (response.data.data.refreshToken) {
        sessionStorage.setItem('refresh_token', response.data.data.refreshToken);
      }
      return newToken;
    }
    return null;
  } catch {
    return null;
  }
}

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = decryptSync(localStorage.getItem('token') || '');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    config.headers['X-CSRF-Token'] = generateCsrfToken();
    return config;
  },
  (error: AxiosError) => Promise.reject(error)
);

service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse<unknown>>) => {
    const apiResponse = response.data;
    const showError = (response.config as CustomAxiosRequestConfig).showError !== false;
    if (!apiResponse || typeof apiResponse !== 'object') {
      if (showError && shouldShowError('响应格式错误')) {
        ElMessage.error('响应格式错误');
      }
      return Promise.reject(new Error('Invalid response format'));
    }
    const { code, message } = apiResponse;
    if (code !== 200 && code !== 0) {
      const errorMsg = message || '请求失败';
      if (showError && shouldShowError(errorMsg)) {
        ElMessage.error(errorMsg);
      }
      return Promise.reject(new Error(message));
    }
    return response;
  },
  async (error: AxiosError<ApiResponse>) => {
    const showError = (error.config as CustomAxiosRequestConfig).showError !== false;
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

      if (status === 401 && !error.config?.url?.includes('/auth/refresh')) {
        if (isRefreshing) {
          return new Promise((resolve, reject) => {
            addPendingRequest({ resolve, reject });
          }).then((token) => {
            if (error.config && token) {
              error.config.headers.Authorization = `Bearer ${token}`;
            }
            return service.request(error.config as AxiosRequestConfig);
          });
        }

        isRefreshing = true;
        const newToken = await refreshToken();
        isRefreshing = false;

        if (newToken) {
          processPendingRequests(newToken);
          if (error.config) {
            error.config.headers.Authorization = `Bearer ${newToken}`;
          }
          return service.request(error.config as AxiosRequestConfig);
        } else {
          processPendingRequests(null, new Error('Token refresh failed'));
          localStorage.removeItem('token');
          sessionStorage.removeItem('refresh_token');
          if (!isPublicPage()) {
            if (showError && shouldShowError('登录已过期，请重新登录')) {
              ElMessage.error('登录已过期，请重新登录');
            }
            router.push('/login');
          }
          return Promise.reject(error);
        }
      }

      switch (status) {
        case 401:
          localStorage.removeItem('token');
          sessionStorage.removeItem('refresh_token');
          if (!isPublicPage()) {
            if (showError && shouldShowError('登录已过期，请重新登录')) {
              ElMessage.error('登录已过期，请重新登录');
            }
            router.push('/login');
          }
          break;
        case 403:
          if (showError && shouldShowError('没有权限访问')) {
            ElMessage.error('没有权限访问');
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
        case 500: {
          const serverErrorMsg = errMessage || '服务器内部错误，请稍后重试';
          if (showError && shouldShowError(serverErrorMsg)) {
            ElMessage.error(serverErrorMsg);
          }
          break;
        }
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
        default: {
          const defaultErrorMsg = errMessage || `请求失败 (${status})`;
          if (showError && shouldShowError(defaultErrorMsg)) {
            ElMessage.error(defaultErrorMsg);
          }
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

export default async function request<T = unknown>(config: CustomAxiosRequestConfig): Promise<T> {
  const response = await service.request<ApiResponse<T>>(config);
  const apiData = response.data;
  // API 响应格式: { code, message, data }
  if (apiData && typeof apiData === 'object' && 'data' in apiData) {
    const data = apiData.data;
    if (data !== undefined && data !== null) {
      return data as T;
    }
  }
  throw new Error('Response data is null or undefined');
}
