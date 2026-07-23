export interface CspDirectives {
  defaultSrc?: string[];
  scriptSrc?: string[];
  styleSrc?: string[];
  imgSrc?: string[];
  fontSrc?: string[];
  objectSrc?: string[];
  connectSrc?: string[];
  formAction?: string[];
  baseUri?: string[];
  frameAncestors?: string[];
  upgradeInsecureRequests?: boolean;
}

/**
 * 开发环境 CSP 策略.
 *
 * 注意: `unsafe-inline` 和 `unsafe-eval` 是 Vite HMR 的必要条件，
 * 移除会导致开发时热更新失效。生产环境使用 `strict-dynamic` 代替。
 * 建议: 生产环境 Nginx 也应配置 CSP 头作为双重防护。
 */
const developmentCsp: CspDirectives = {
  defaultSrc: ["'self'"],
  scriptSrc: ["'self'", "'unsafe-inline'", "'unsafe-eval'", 'http://localhost:*'],
  styleSrc: ["'self'", "'unsafe-inline'"],
  imgSrc: ["'self'", 'data:', 'http://localhost:*', 'https://images.unsplash.com'],
  fontSrc: ["'self'", 'data:'],
  objectSrc: ["'none'"],
  connectSrc: ["'self'", 'http://localhost:*'],
  formAction: ["'self'"],
  baseUri: ["'self'"],
  frameAncestors: ["'none'"],
};

const productionCsp: CspDirectives = {
  defaultSrc: ["'self'"],
  scriptSrc: ["'self'", "'strict-dynamic'"],
  styleSrc: ["'self'", "'unsafe-inline'"],
  imgSrc: ["'self'", 'data:', 'https://images.unsplash.com'],
  fontSrc: ["'self'", 'data:'],
  objectSrc: ["'none'"],
  connectSrc: ["'self'"],
  formAction: ["'self'"],
  baseUri: ["'self'"],
  frameAncestors: ["'none'"],
  upgradeInsecureRequests: true,
};

export function getCspHeader(): string {
  const isProduction = import.meta.env.NODE_ENV === 'production';
  const directives = isProduction ? productionCsp : developmentCsp;
  const parts: string[] = [];

  if (directives.defaultSrc) parts.push(`default-src ${directives.defaultSrc.join(' ')}`);
  if (directives.scriptSrc) parts.push(`script-src ${directives.scriptSrc.join(' ')}`);
  if (directives.styleSrc) parts.push(`style-src ${directives.styleSrc.join(' ')}`);
  if (directives.imgSrc) parts.push(`img-src ${directives.imgSrc.join(' ')}`);
  if (directives.fontSrc) parts.push(`font-src ${directives.fontSrc.join(' ')}`);
  if (directives.objectSrc) parts.push(`object-src ${directives.objectSrc.join(' ')}`);
  if (directives.connectSrc) parts.push(`connect-src ${directives.connectSrc.join(' ')}`);
  if (directives.formAction) parts.push(`form-action ${directives.formAction.join(' ')}`);
  if (directives.baseUri) parts.push(`base-uri ${directives.baseUri.join(' ')}`);
  if (directives.frameAncestors) parts.push(`frame-ancestors ${directives.frameAncestors.join(' ')}`);
  if (directives.upgradeInsecureRequests) parts.push('upgrade-insecure-requests');

  return parts.join('; ');
}

export function applyCspMetaTag(): void {
  const cspHeader = getCspHeader();
  const meta = document.createElement('meta');
  meta.httpEquiv = 'Content-Security-Policy';
  meta.content = cspHeader;
  document.head.appendChild(meta);
}

/**
 * 应用安全头部（通过 meta 标签 + 建议 Nginx 服务端双重设置）.
 *
 * 注意: 以下安全头通过前端 meta 标签设置作为客户端防护层。
 * 为保证最佳安全性，建议同时在 Nginx 服务端配置这些头:
 * ```
 * add_header X-Content-Type-Options "nosniff" always;
 * add_header X-Frame-Options "DENY" always;
 * add_header X-XSS-Protection "1; mode=block" always;
 * add_header Referrer-Policy "strict-origin-when-cross-origin" always;
 * add_header Permissions-Policy "camera=(), microphone=(), geolocation=(), payment=()" always;
 * ```
 */
export function applySecurityHeaders(): void {
  applyCspMetaTag();

  const headers: Record<string, string> = {
    'X-Content-Type-Options': 'nosniff',
    'X-Frame-Options': 'DENY',
    'X-XSS-Protection': '1; mode=block',
    'Referrer-Policy': 'strict-origin-when-cross-origin',
    'Permissions-Policy': 'camera=(), microphone=(), geolocation=(), payment=()',
  };

  Object.entries(headers).forEach(([key, value]) => {
    const meta = document.createElement('meta');
    meta.httpEquiv = key;
    meta.content = value;
    document.head.appendChild(meta);
  });
}
