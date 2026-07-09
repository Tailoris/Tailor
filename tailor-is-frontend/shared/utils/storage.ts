export interface StorageInstance {
  set(value: string): void
  get(): string | null
  clear(): void
}

/**
 * 创建通用 localStorage 存储实例.
 *
 * @param key - localStorage 键名
 * @returns StorageInstance 包含 set/get/clear 操作
 */
export function createStorage(key: string): StorageInstance {
  return {
    set(value: string): void {
      try {
        localStorage.setItem(key, value)
      } catch {
        // localStorage 不可用或已满时静默失败
      }
    },

    get(): string | null {
      try {
        return localStorage.getItem(key)
      } catch {
        return null
      }
    },

    clear(): void {
      try {
        localStorage.removeItem(key)
      } catch {
      }
    }
  }
}
