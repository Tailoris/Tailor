// ============================================================
// TypeScript 类型声明 shims - Tailor IS Mobile App
// ============================================================

// uni-app 全局变量类型
declare module '*.vue' {
  import type { DefineComponent } from 'vue';
  const component: DefineComponent<{}, {}, any>;
  export default component;
}

declare namespace UniApp {
  interface NavigateToOptions {
    url: string;
    success?: (res: any) => void;
    fail?: (err: any) => void;
    complete?: () => void;
  }

  interface ShowToastOptions {
    title: string;
    icon?: 'success' | 'error' | 'loading' | 'none';
    duration?: number;
    mask?: boolean;
  }

  interface RequestOptions {
    url: string;
    method?: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH' | 'HEAD' | 'OPTIONS';
    data?: any;
    header?: Record<string, string>;
    timeout?: number;
    success?: (res: any) => void;
    fail?: (err: any) => void;
  }

  interface StorageInfo {
    keys: string[];
    currentSize: number;
    limitSize: number;
  }

  interface SystemInfo {
    model: string;
    pixelRatio: number;
    screenWidth: number;
    screenHeight: number;
    statusBarHeight: number;
    platform: string;
    version: string;
    SDKVersion: string;
  }
}

// 全局类型
declare global {
  interface Window {
    __TAILOR_IS_CONFIG__: {
      apiBaseUrl: string;
      env: 'development' | 'staging' | 'production';
      version: string;
    };
  }
}

// Shared modules
declare module '@shared/plugins/a11y-directive' {
  import type { App, Directive } from 'vue'
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
  import type { Ref } from 'vue'
  export function useCountdown(targetTime: Date | number): {
    days: Ref<number>;
    hours: Ref<number>;
    minutes: Ref<number>;
    seconds: Ref<number>;
    isEnd: Ref<boolean>;
    stop: () => void;
  };
}

export {};
