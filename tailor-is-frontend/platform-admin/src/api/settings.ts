import request from './request'

// FE-M-6: 系统设置 API，替代 setTimeout 假保存

export interface BasicSettings {
  platformName: string
  logoUrl: string
  servicePhone: string
  serviceEmail: string
  announcement: string
}

export interface SecuritySettings {
  captchaEnabled: boolean
  registerAudit: boolean
  smsVerification: boolean
  loginFailLock: number
  sessionTimeout: number
}

export interface ThirdPartySettings {
  ossEnabled: boolean
  ossEndpoint: string
  ossBucket: string
  wechatPayEnabled: boolean
  alipayEnabled: boolean
}

export interface SystemSettings {
  basic: BasicSettings
  security: SecuritySettings
  thirdParty: ThirdPartySettings
}

/** 获取系统设置 */
export const getSettings = () => {
  return request<Record<string, unknown>, SystemSettings>({
    url: '/system/settings',
    method: 'GET',
  })
}

/** 保存基本设置 */
export const saveBasicSettings = (data: BasicSettings) => {
  return request<Record<string, unknown>, void>({
    url: '/system/settings/basic',
    method: 'PUT',
    data,
  })
}

/** 保存安全设置 */
export const saveSecuritySettings = (data: SecuritySettings) => {
  return request<Record<string, unknown>, void>({
    url: '/system/settings/security',
    method: 'PUT',
    data,
  })
}

/** 保存第三方服务设置 */
export const saveThirdPartySettings = (data: ThirdPartySettings) => {
  return request<Record<string, unknown>, void>({
    url: '/system/settings/third-party',
    method: 'PUT',
    data,
  })
}
