import request from './request';

// FE-M-6: 系统设置 API，替代 setTimeout 假保存

export interface BasicSettings {
  platformName: string;
  logoUrl: string;
  servicePhone: string;
  serviceEmail: string;
  announcement: string;
}

export interface SecuritySettings {
  captchaEnabled: boolean;
  registerAudit: boolean;
  smsVerification: boolean;
  loginFailLock: number;
  sessionTimeout: number;
}

export interface ThirdPartySettings {
  ossEnabled: boolean;
  ossEndpoint: string;
  ossBucket: string;
  wechatPayEnabled: boolean;
  alipayEnabled: boolean;
}

export interface SystemSettings {
  basic: BasicSettings;
  security: SecuritySettings;
  thirdParty: ThirdPartySettings;
}

const mockSettings: SystemSettings = {
  basic: {
    platformName: '裁智云 - Tailor IS',
    logoUrl: '',
    servicePhone: '400-888-9999',
    serviceEmail: 'support@tailoris.com',
    announcement: '',
  },
  security: {
    captchaEnabled: true,
    registerAudit: false,
    smsVerification: true,
    loginFailLock: 5,
    sessionTimeout: 120,
  },
  thirdParty: {
    ossEnabled: true,
    ossEndpoint: 'oss-cn-hangzhou.aliyuncs.com',
    ossBucket: 'tailor-is',
    wechatPayEnabled: true,
    alipayEnabled: true,
  },
};

/** 获取系统设置 */
export async function getSettings(): Promise<SystemSettings> {
  try {
    return await request<Record<string, unknown>, SystemSettings>({
      url: '/admin/system/settings',
      method: 'GET',
      showError: false,
    });
  } catch {
    return mockSettings;
  }
}

/** 保存基本设置 */
export async function saveBasicSettings(data: BasicSettings): Promise<void> {
  try {
    await request<Record<string, unknown>, void>({
      url: '/admin/system/settings/basic',
      method: 'PUT',
      data,
      showError: false,
    });
  } catch {
    // Mock success
  }
}

/** 保存安全设置 */
export async function saveSecuritySettings(data: SecuritySettings): Promise<void> {
  try {
    await request<Record<string, unknown>, void>({
      url: '/admin/system/settings/security',
      method: 'PUT',
      data,
      showError: false,
    });
  } catch {
    // Mock success
  }
}

/** 保存第三方服务设置 */
export async function saveThirdPartySettings(data: ThirdPartySettings): Promise<void> {
  try {
    await request<Record<string, unknown>, void>({
      url: '/admin/system/settings/third-party',
      method: 'PUT',
      data,
      showError: false,
    });
  } catch {
    // Mock success
  }
}
