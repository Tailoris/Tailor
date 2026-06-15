/**
 * 全局表单校验工具
 */

export const validate = {
  /** 11位中国大陆手机号校验 */
  isPhone(value: string): boolean {
    return /^1[3-9]\d{9}$/.test(value)
  },

  /** 通用邮箱格式校验 */
  isEmail(value: string): boolean {
    return /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(value)
  },

  /** 6位数字验证码校验 */
  isSixCode(value: string): boolean {
    return /^\d{6}$/.test(value)
  },

  /** 密码长度≥6位校验 */
  isValidPwd(value: string): boolean {
    return value.length >= 6
  },

  /** 两次密码一致性对比 */
  isEqualPwd(password: string, confirmPassword: string): boolean {
    return password === confirmPassword && password.length > 0
  },

  /** 识别账号类型：phone | email | username | unknown */
  identifyAccount(value: string): 'phone' | 'email' | 'username' | 'unknown' {
    if (!value) return 'unknown'
    if (/^1[3-9]\d{9}$/.test(value)) return 'phone'
    if (value.includes('@')) return 'email'
    return 'username'
  }
}
