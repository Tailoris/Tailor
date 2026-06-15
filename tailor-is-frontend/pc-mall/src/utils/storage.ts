/**
 * 本地存储工具
 * 仅存储账号字符串，不保存敏感密码信息
 */
import { createStorage } from '@shared/utils/storage'

export const storage = createStorage('tailor_is_remember_account')
