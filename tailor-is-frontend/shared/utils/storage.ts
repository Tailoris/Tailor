/**
 * 本地存储工具
 * 通过参数化 key 支持不同项目的使用场景
 */

export function createStorage(key: string) {
  return {
    /** 保存账号到本地存储 */
    saveAccount(account: string): void {
      try {
        localStorage.setItem(key, account)
      } catch {
        // 静默失败
      }
    },

    /** 从本地存储获取账号 */
    getAccount(): string | null {
      try {
        return localStorage.getItem(key)
      } catch {
        return null
      }
    },

    /** 清除本地存储的账号 */
    clearAccount(): void {
      try {
        localStorage.removeItem(key)
      } catch {
        // 静默失败
      }
    }
  }
}
