/**
 * 统一共享请求核心模块 (request-core.ts)
 *
 * 功能: 为 pc-mall、merchant-admin、platform-admin 提供统一的 Axios 请求封装
 *   - 统一请求拦截器: 认证令牌管理、CSRF Token、请求头标准化
 *   - 统一响应拦截器: 分层错误处理(401/403/500/502/503)、数据格式转换
 *   - 请求取消: AbortController 支持
 *   - 超时处理: 可配置超时时间
 *   - 重试机制: 可配置重试次数、重试延迟、可重试条件
 *   - Token 刷新: 可配置刷新策略
 *   - 错误消息节流: 防止同一错误重复弹窗
 *
 * 使用方式:
 *   import { createRequest } from '@shared/utils/request-core';
 *   const request = createRequest({ baseURL: '/api/v1', router: myRouter });
 *
 * @module shared/utils/request-core
 * @since 2026-07-20
 */

import axios, {
  AxiosError,
  AxiosInstance,
  AxiosRequestConfig,
  AxiosResponse,
  InternalAxiosRequestConfig,
} from 'axios';

// ============================================================
// 类型定义
// ============================================================

/** 统一后端响应包装类型 */
export interface ApiResponse<T = unknown> {
  code: number;
  message: string;
  data?: T | null;
}

/** 扩展请求配置 */
export interface RequestConfig extends AxiosRequestConfig {
  /** 是否显示错误提示，默认 true */
  showError?: boolean;
  /** 跳过重试，默认 false */
  skipRetry?: boolean;
}

/** 重试配置 */
export interface RetryConfig {
  /** 最大重试次数，默认 0（不重试） */
  maxRetries?: number;
  /** 重试延迟基数(ms)，默认 1000 */
  retryDelay?: number;
  /** 不可重试的 HTTP 状态码集合 */
  nonRetryableStatus?: Set<number>;
  /** 不可重试的 HTTP 方法集合 */
  nonRetryableMethods?: Set<string>;
}

/** Token 刷新配置 */
export interface TokenRefreshConfig {
  /** Token 刷新接口地址 */
  refreshUrl?: string;
  /** 从存储中获取 refresh token */
  getRefreshToken?: () => string | null;
  /** 存储新 token 的回调 */
  onTokenRefreshed?: (token: string, refreshToken?: string) => void;
  /** 清除 token 的回调 */
  onTokenClear?: () => void;
}

/** 创建请求实例的配置选项 */
export interface CreateRequestOptions {
  /** API 基础路径，默认 '/api/v1' */
  baseURL?: string;
  /** 请求超时时间(ms)，默认 15000 */
  timeout?: number;
  /** 重试配置 */
  retry?: RetryConfig;
  /** Token 刷新配置 */
  tokenRefresh?: TokenRefreshConfig;
  /** 路由实例（用于 401 跳转，避免循环依赖） */
  router?: unknown;
  /** 获取 token 的函数 */
  getToken?: () => string | null;
  /** 自定义未授权处理 */
  onUnauthorized?: () => void;
  /** 自定义错误消息显示函数 */
  showMessage?: (type: 'error' | 'warning' | 'info', message: string) => void;
}

/** 挂起的请求 */
interface PendingRequest {
  resolve: (_: unknown) => void;
  reject: (_?: unknown) => void;
}

// ============================================================
// 内部工具函数
// ============================================================

/** 错误消息节流缓存 */
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

/** 生成 CSRF Token */
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

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/** 默认错误提示函数 */
function defaultShowMessage(type: 'error' | 'warning' | 'info', message: string): void {
  if (typeof window !== 'undefined' && typeof (window as unknown as Record<string, unknown>).ElMessage !== 'undefined') {
    const ElMessage = (window as unknown as Record<string, { error: (msg: string) => void }>).ElMessage;
    if (ElMessage && ElMessage[type]) {
      ElMessage[type](message);
    }
  }
}

// ============================================================
// 核心工厂函数
// ============================================================

/**
 * 创建统一的 Axios 请求实例
 *
 * @param options - 配置选项
 * @returns AxiosInstance 实例
 */
export function createRequest(options: CreateRequestOptions = {}): AxiosInstance {
  const {
    baseURL = '/api/v1',
    timeout = 15000,
    retry,
    tokenRefresh,
    router,
    getToken,
    onUnauthorized,
    showMessage = defaultShowMessage,
  } = options;

  // 重试默认值
  const maxRetries = retry?.maxRetries ?? 0;
  const retryDelay = retry?.retryDelay ?? 1000;
  const nonRetryableStatus = retry?.nonRetryableStatus ?? new Set([400, 401, 403, 404, 422]);
  const nonRetryableMethods = retry?.nonRetryableMethods ?? new Set(['post', 'put', 'delete', 'patch']);

  // Token 刷新状态
  let isRefreshing = false;
  const pendingRequests: PendingRequest[] = [];

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

  // 创建 Axios 实例
  const instance: AxiosInstance = axios.create({
    baseURL,
    timeout,
    responseEncoding: 'utf8',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
      Accept: 'application/json;charset=UTF-8',
    },
  });

  // ---------- 请求拦截器 ----------
  instance.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
      // 认证令牌
      if (getToken) {
        const token = getToken();
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
      }
      // CSRF Token
      config.headers['X-CSRF-Token'] = generateCsrfToken();
      return config;
    },
    (error: AxiosError) => Promise.reject(error),
  );

  // ---------- Token 刷新逻辑 ----------
  async function doRefreshToken(): Promise<string | null> {
    if (!tokenRefresh?.refreshUrl || !tokenRefresh?.getRefreshToken) {
      return null;
    }
    try {
      const refreshTokenValue = tokenRefresh.getRefreshToken();
      if (!refreshTokenValue) return null;

      const response = await axios.post<ApiResponse<{ token: string; refreshToken?: string }>>(
        tokenRefresh.refreshUrl,
        { refreshToken: refreshTokenValue },
      );

      if (response.data.code === 200 && response.data.data?.token) {
        const newToken = response.data.data.token;
        tokenRefresh.onTokenRefreshed?.(newToken, response.data.data.refreshToken);
        return newToken;
      }
      return null;
    } catch {
      return null;
    }
  }

  // ---------- 响应拦截器 ----------
  instance.interceptors.response.use(
    (response: AxiosResponse<ApiResponse<unknown>>) => {
      const apiResponse = response.data;
      const showError = (response.config as RequestConfig).showError !== false;

      if (!apiResponse || typeof apiResponse !== 'object') {
        if (showError && shouldShowError('响应格式错误')) {
          showMessage('error', '响应格式错误');
        }
        return Promise.reject(new Error('Invalid response format'));
      }

      const { code, message } = apiResponse;
      if (code !== 200 && code !== 0) {
        const errorMsg = message || '请求失败';
        if (showError && shouldShowError(errorMsg)) {
          showMessage('error', errorMsg);
        }
        return Promise.reject(new Error(message));
      }

      return response;
    },

    async (error: AxiosError<ApiResponse>) => {
      const config = error.config as (InternalAxiosRequestConfig & { __retryCount?: number }) | undefined;
      const showError = (error.config as RequestConfig).showError !== false;
      const skipRetry = (error.config as RequestConfig).skipRetry === true;

      // ---------- 重试逻辑 ----------
      if (!skipRetry && config && maxRetries > 0) {
        const retryCount = config.__retryCount || 0;
        if (retryCount < maxRetries && shouldRetry(error, config, nonRetryableStatus, nonRetryableMethods)) {
          config.__retryCount = retryCount + 1;
          const delay = retryDelay * (retryCount + 1);
          await sleep(delay);
          return instance.request(config);
        }
      }

      // ---------- 错误处理 ----------
      if (error.response) {
        const { status } = error.response;
        const responseData = error.response.data;
        const errMessage = responseData?.message || '';

        // CSRF 错误特殊处理
        if (errMessage.toLowerCase().includes('csrf')) {
          if (showError && shouldShowError('安全验证失败，请刷新页面重试')) {
            showMessage('error', '安全验证失败，请刷新页面重试');
          }
          return Promise.reject(error);
        }

        // 401 Token 刷新
        if (status === 401 && tokenRefresh && !error.config?.url?.includes('/auth/refresh')) {
          if (isRefreshing) {
            return new Promise((resolve, reject) => {
              addPendingRequest({ resolve, reject });
            }).then((token) => {
              if (error.config && token) {
                error.config.headers.Authorization = `Bearer ${token}`;
              }
              return instance.request(error.config as AxiosRequestConfig);
            });
          }

          isRefreshing = true;
          const newToken = await doRefreshToken();
          isRefreshing = false;

          if (newToken) {
            processPendingRequests(newToken);
            if (error.config) {
              error.config.headers.Authorization = `Bearer ${newToken}`;
            }
            return instance.request(error.config as AxiosRequestConfig);
          }

          processPendingRequests(null, new Error('Token refresh failed'));
          tokenRefresh?.onTokenClear?.();
        }

        // 分层错误处理
        switch (status) {
          case 401:
            if (showError && shouldShowError('登录已过期，请重新登录')) {
              showMessage('error', '登录已过期，请重新登录');
            }
            onUnauthorized?.();
            break;
          case 403:
            if (showError && shouldShowError('没有权限访问')) {
              showMessage('error', '没有权限访问');
            }
            break;
          case 404:
            if (showError && shouldShowError('请求的资源不存在')) {
              showMessage('error', '请求的资源不存在');
            }
            break;
          case 429:
            if (showError && shouldShowError('请求过于频繁，请稍后再试')) {
              showMessage('error', '请求过于频繁，请稍后再试');
            }
            break;
          case 500:
            if (showError && shouldShowError(errMessage || '服务器内部错误，请稍后重试')) {
              showMessage('error', errMessage || '服务器内部错误，请稍后重试');
            }
            break;
          case 502:
            if (showError && shouldShowError('网关错误，服务暂时不可用')) {
              showMessage('error', '网关错误，服务暂时不可用');
            }
            break;
          case 503:
            if (showError && shouldShowError('服务暂时不可用，请稍后重试')) {
              showMessage('error', '服务暂时不可用，请稍后重试');
            }
            break;
          case 504:
            if (showError && shouldShowError('网关超时，请稍后重试')) {
              showMessage('error', '网关超时，请稍后重试');
            }
            break;
          default:
            if (showError && shouldShowError(errMessage || `请求失败 (${status})`)) {
              showMessage('error', errMessage || `请求失败 (${status})`);
            }
        }
      } else if (error.code === 'ECONNABORTED') {
        if (showError && shouldShowError('请求超时，请稍后重试')) {
          showMessage('error', '请求超时，请稍后重试');
        }
      } else if (error.code === 'ERR_CANCELED') {
        // 请求被取消，静默处理
      } else {
        if (showError && shouldShowError('网络连接异常，请检查网络')) {
          showMessage('error', '网络连接异常，请检查网络');
        }
      }

      return Promise.reject(error);
    },
  );

  return instance;
}

/**
 * 判断是否应该重试请求
 */
function shouldRetry(
  error: AxiosError,
  config: InternalAxiosRequestConfig & { __retryCount?: number },
  nonRetryableStatus: Set<number>,
  nonRetryableMethods: Set<string>,
): boolean {
  const method = (config.method || 'get').toLowerCase();
  if (nonRetryableMethods.has(method)) {
    return false;
  }
  if (!error.response) {
    return true; // 网络错误可重试
  }
  const status = error.response.status;
  if (nonRetryableStatus.has(status)) {
    return false;
  }
  return status >= 500; // 服务端错误可重试
}

/**
 * 创建请求取消控制器
 * @returns AbortController 实例
 */
export function createAbortController(): AbortController {
  return new AbortController();
}

/**
 * 便捷的请求封装函数
 * 对 AxiosInstance 进行包装，自动提取 response.data.data
 *
 * @param instance - Axios 实例
 * @param config - 请求配置
 * @returns Promise<T>
 */
export async function request<T = unknown>(
  instance: AxiosInstance,
  config: RequestConfig,
): Promise<T> {
  const response = await instance.request<ApiResponse<T>>(config);
  return response.data.data as T;
}

export default createRequest;