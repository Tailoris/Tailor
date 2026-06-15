/**
 * 应用配置文件
 * 修复 B-C11/F-C01: BASE_URL 环境变量化
 *
 * 通过环境变量配置不同环境的API地址：
 * - dev: 本地开发
 * - staging: 测试环境
 * - prod: 生产环境
 */

export interface AppConfig {
  baseURL: string
  apiPrefix: string
  uploadURL: string
  tokenStorageKey: string
  userInfoStorageKey: string
  env: 'dev' | 'staging' | 'prod'
  requestTimeout: number
}

export interface GatewayConfig {
  coreGatewayUrl: string
  liteGatewayUrl: string
}

// #ifdef H5
// H5环境：使用相对路径或配置的域名
const DEV_BASE_URL = 'http://localhost:8080'
const STAGING_BASE_URL = 'https://staging-api.tailor-is.com'
const PROD_BASE_URL = 'https://api.tailor-is.com'

// Gateway split configuration
const DEV_CORE_GATEWAY = 'http://localhost:8080'
const DEV_LITE_GATEWAY = 'http://localhost:8081'
const STAGING_CORE_GATEWAY = 'https://staging-api.tailor-is.com'
const STAGING_LITE_GATEWAY = 'https://staging-api-lite.tailor-is.com'
const PROD_CORE_GATEWAY = 'https://api.tailor-is.com'
const PROD_LITE_GATEWAY = 'https://api-lite.tailor-is.com'
// #endif

// #ifdef MP-WEIXIN || MP-ALIPAY
// 小程序环境：使用配置的HTTPS地址
const DEV_BASE_URL = 'http://localhost:8080'
const STAGING_BASE_URL = 'https://staging-api.tailor-is.com'
const PROD_BASE_URL = 'https://api.tailor-is.com'

// Gateway split configuration
const DEV_CORE_GATEWAY = 'http://localhost:8080'
const DEV_LITE_GATEWAY = 'http://localhost:8081'
const STAGING_CORE_GATEWAY = 'https://staging-api.tailor-is.com'
const STAGING_LITE_GATEWAY = 'https://staging-api-lite.tailor-is.com'
const PROD_CORE_GATEWAY = 'https://api.tailor-is.com'
const PROD_LITE_GATEWAY = 'https://api-lite.tailor-is.com'
// #endif

// #ifdef APP-PLUS
// App环境：使用配置的HTTPS地址
const DEV_BASE_URL = 'http://dev-api.tailor-is.com:8080'
const STAGING_BASE_URL = 'https://staging-api.tailor-is.com'
const PROD_BASE_URL = 'https://api.tailor-is.com'

// Gateway split configuration
const DEV_CORE_GATEWAY = 'http://dev-api.tailor-is.com:8080'
const DEV_LITE_GATEWAY = 'http://dev-api.tailor-is.com:8081'
const STAGING_CORE_GATEWAY = 'https://staging-api.tailor-is.com'
const STAGING_LITE_GATEWAY = 'https://staging-api-lite.tailor-is.com'
const PROD_CORE_GATEWAY = 'https://api.tailor-is.com'
const PROD_LITE_GATEWAY = 'https://api-lite.tailor-is.com'
// #endif

const ENV = (process.env.NODE_ENV || 'dev') as 'dev' | 'staging' | 'prod'

function selectBaseURL(): string {
  // 优先从uni全局配置获取
  try {
    const customURL = uni.getStorageSync('__custom_base_url__')
    if (customURL) {
      return customURL
    }
  } catch {
    // ignore
  }
  switch (ENV) {
    case 'prod':
      return PROD_BASE_URL
    case 'staging':
      return STAGING_BASE_URL
    default:
      return DEV_BASE_URL
  }
}

export const gatewayConfig: GatewayConfig = {
  coreGatewayUrl: ENV === 'prod' ? PROD_CORE_GATEWAY : ENV === 'staging' ? STAGING_CORE_GATEWAY : DEV_CORE_GATEWAY,
  liteGatewayUrl: ENV === 'prod' ? PROD_LITE_GATEWAY : ENV === 'staging' ? STAGING_LITE_GATEWAY : DEV_LITE_GATEWAY,
}

/**
 * 根据API路径自动选择正确的网关
 * @param path API路径 (e.g. '/api/user/list')
 * @returns 完整的请求URL
 */
export function resolveGatewayUrl(path: string): string {
  const corePaths = [
    '/api/user', '/api/auth', '/api/product', '/api/favorite',
    '/api/order', '/api/cart', '/api/payment', '/api/settlement',
    '/api/account', '/api/sandbox', '/api/marketing', '/api/coupon',
    '/api/points', '/api/seckill', '/api/ai', '/api/body-size',
    '/api/copyright', '/api/merchant', '/api/shop', '/api/admin', '/api/pattern'
  ]
  const litePaths = [
    '/api/community', '/api/post', '/api/comment',
    '/api/academy', '/api/course',
    '/api/supply',
    '/api/message', '/api/notice',
    '/api/im', '/api/im-message',
    '/api/analytics', '/api/metrics', '/api/dashboard'
  ]

  const isCore = corePaths.some(p => path.startsWith(p))
  const isLite = litePaths.some(p => path.startsWith(p))

  if (isLite) {
    return `${gatewayConfig.liteGatewayUrl}${path}`
  }
  // 默认走核心网关
  return `${gatewayConfig.coreGatewayUrl}${path}`
}

export const appConfig: AppConfig = {
  baseURL: selectBaseURL(),
  apiPrefix: '/api/front',
  uploadURL: '/api/front/upload',
  tokenStorageKey: '__tkn__',
  userInfoStorageKey: '__usr__',
  env: ENV,
  requestTimeout: 30000
}

export default appConfig
