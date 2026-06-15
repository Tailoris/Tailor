/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<object, object, unknown>
  export default component
}

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string
  readonly VITE_APP_TITLE: string
  readonly VITE_ENV: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
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
