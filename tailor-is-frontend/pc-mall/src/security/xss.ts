const HTML_ESCAPE_MAP: Record<string, string> = {
  '&': '&amp;',
  '<': '&lt;',
  '>': '&gt;',
  '"': '&quot;',
  "'": '&#39;',
  '/': '&#x2F;',
  '`': '&#x60;',
  '=': '&#x3D;',
}

export function escapeHtml(str: string): string {
  return str.replace(/[&<>"'`=\/]/g, char => HTML_ESCAPE_MAP[char] || char)
}

export function sanitizeHtml(str: string): string {
  const temp = document.createElement('div')
  temp.textContent = str
  return temp.innerHTML
}

export function stripHtmlTags(str: string): string {
  return str.replace(/<[^>]*>/g, '')
}

export function isSafeUrl(url: string): boolean {
  try {
    const parsed = new URL(url)
    const allowedProtocols = ['http:', 'https:', 'mailto:', 'tel:']
    return allowedProtocols.includes(parsed.protocol)
  } catch {
    return false
  }
}

export function validateInput(input: string): { isValid: boolean; errors: string[] } {
  const errors: string[] = []

  if (/<script[^>]*>[\s\S]*?<\/script>/gi.test(input)) {
    errors.push('不允许包含脚本标签')
  }

  if (/javascript:/gi.test(input)) {
    errors.push('不允许包含javascript协议')
  }

  if (/on\w+\s*=/gi.test(input)) {
    errors.push('不允许包含事件处理器')
  }

  if (/data:image\/[^,]+,.*<script/gi.test(input)) {
    errors.push('不允许在图片数据中包含脚本')
  }

  return {
    isValid: errors.length === 0,
    errors,
  }
}

export function sanitizeUrl(url: string): string {
  if (!url) return ''
  if (!isSafeUrl(url)) return ''
  return url
}

export function sanitizeText(text: string): string {
  if (!text) return ''
  return stripHtmlTags(text).trim()
}
