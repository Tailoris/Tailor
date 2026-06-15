/// <reference types="vite/client" />

// ============================================================
// 🔒 F-L02修复: Vite 环境变量与自定义变量类型声明
// 裁智云 PC 商城 Vite 类型扩展
// ============================================================

interface ImportMetaEnv {
  // ==================== 应用基础 ====================
  /** 应用模式: development | production | staging */
  readonly VITE_APP_MODE: 'development' | 'production' | 'staging';
  /** 应用标题 */
  readonly VITE_APP_TITLE: string;
  /** 应用版本 */
  readonly VITE_APP_VERSION: string;

  // ==================== API 配置 ====================
  /** 后端 API 基础地址 */
  readonly VITE_API_BASE_URL: string;
  /** 网关地址 */
  readonly VITE_GATEWAY_URL: string;
  /** WebSocket 地址 */
  readonly VITE_WS_URL: string;
  /** API 请求超时（毫秒） */
  readonly VITE_API_TIMEOUT: string;
  /** API 前缀 */
  readonly VITE_API_PREFIX: string;

  // ==================== 第三方服务 ====================
  /** 阿里云 OSS 访问域名 */
  readonly VITE_OSS_DOMAIN: string;
  /** 阿里云 OSS 上传路径前缀 */
  readonly VITE_OSS_UPLOAD_PREFIX: string;
  /** 腾讯地图 Key */
  readonly VITE_TX_MAP_KEY: string;
  /** 百度统计 Key */
  readonly VITE_BAIDU_ANALYTICS_KEY: string;
  /** 神策分析数据地址 */
  readonly VITE_SA_URL: string;
  /** 极光推送 AppKey */
  readonly VITE_JPUSH_APP_KEY: string;

  // ==================== 支付配置 ====================
  /** 微信支付 AppID */
  readonly VITE_WECHAT_APP_ID: string;
  /** 支付宝 AppID */
  readonly VITE_ALIPAY_APP_ID: string;

  // ==================== 功能开关 ====================
  /** 是否启用 Mock */
  readonly VITE_USE_MOCK: string;
  /** 是否启用调试模式（控制台日志） */
  readonly VITE_ENABLE_DEBUG: string;
  /** 是否启用性能监控 */
  readonly VITE_ENABLE_PERFORMANCE: string;
  /** 是否启用错误监控（Sentry） */
  readonly VITE_ENABLE_SENTRY: string;
  /** Sentry DSN */
  readonly VITE_SENTRY_DSN: string;

  // ==================== CDN ====================
  /** CDN 基础地址 */
  readonly VITE_CDN_BASE_URL: string;

  // ==================== 业务开关 ====================
  /** 是否显示版权信息（备案号） */
  readonly VITE_SHOW_COPYRIGHT: string;
  /** 客服电话 */
  readonly VITE_SERVICE_PHONE: string;
  /** 公司全称 */
  readonly VITE_COMPANY_NAME: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

// ============================================================
// 模块声明
// ============================================================

// Vue 单文件组件类型
declare module '*.vue' {
  import type { DefineComponent } from 'vue';
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const component: DefineComponent<Record<string, unknown>, Record<string, unknown>, any>;
  export default component;
}

// SVG 文件（用作 URL）
declare module '*.svg' {
  const src: string;
  export default src;
}

// 静态资源
declare module '*.png' {
  const src: string;
  export default src;
}
declare module '*.jpg' {
  const src: string;
  export default src;
}
declare module '*.jpeg' {
  const src: string;
  export default src;
}
declare module '*.gif' {
  const src: string;
  export default src;
}
declare module '*.webp' {
  const src: string;
  export default src;
}

// CSS Modules
declare module '*.module.css' {
  const classes: Readonly<Record<string, string>>;
  export default classes;
}
declare module '*.module.scss' {
  const classes: Readonly<Record<string, string>>;
  export default classes;
}
declare module '*.module.sass' {
  const classes: Readonly<Record<string, string>>;
  export default classes;
}

// JSON 文件
declare module '*.json' {
  const value: Record<string, unknown>;
  export default value;
}

// Element Plus locale modules
declare module 'element-plus/dist/locale/*.mjs' {
  import type { Language } from 'element-plus/es/locale'
  const locale: Language
  export default locale
}

// Shared modules
declare module '@shared/plugins/a11y-directive' {
  import type { App, Directive, DirectiveBinding } from 'vue'
  export function installA11yDirectives(app: App): void
  export const a11yDirective: Directive<HTMLElement, string>
}
declare module '@shared/utils/storage' {
  export function createStorage(key: string): {
    get: <T>(key: string) => T | null
    set: <T>(key: string, value: T) => void
    remove: (key: string) => void
  }
}
declare module '@shared/utils/validate' {
  export const validate: {
    isPhone(value: string): boolean;
    isEmail(value: string): boolean;
    isSixCode(value: string): boolean;
    isValidPwd(value: string): boolean;
    isEqualPwd(password: string, confirmPassword: string): boolean;
    identifyAccount(value: string): 'phone' | 'email' | 'username' | 'unknown';
  };
}
declare module '@shared/composables/useCountdown' {
  export function useCountdown(targetTime: Date | number): {
    days: import('vue').Ref<number>;
    hours: import('vue').Ref<number>;
    minutes: import('vue').Ref<number>;
    seconds: import('vue').Ref<number>;
    isEnd: import('vue').Ref<boolean>;
    stop: () => void;
  };
}

// ==================== 全局类型 ====================

/** 统一 API 响应 */
interface ApiResponse<T = unknown> {
  code: number;
  message: string;
  data: T;
  traceId?: string;
  timestamp?: number;
}

/** 分页请求 */
interface PageRequest {
  pageNum?: number;
  pageSize?: number;
  orderBy?: string;
  asc?: boolean;
}

/** 分页响应 */
interface PageResponse<T = unknown> {
  total: number;
  pageNum: number;
  pageSize: number;
  pages: number;
  list: T[];
}

/** 用户信息 */
interface UserInfo {
  id: string | number;
  username: string;
  nickname?: string;
  avatar?: string;
  phone?: string;
  email?: string;
  roles?: string[];
  permissions?: string[];
}

/** 通用键值对 */
interface KeyValue<K = string, V = unknown> {
  key: K;
  value: V;
  label?: string;
}

/** 树形结构 */
interface TreeNode<T = unknown> {
  id: string | number;
  parentId?: string | number | null;
  children?: TreeNode<T>[];
  data: T;
}
