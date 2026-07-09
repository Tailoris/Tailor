const ALLOWED_TAGS: Record<string, string[]> = {
  b: [],
  strong: [],
  i: [],
  em: [],
  u: [],
  s: [],
  p: [],
  br: [],
  ul: [],
  ol: [],
  li: [],
  a: ['href', 'target'],
  img: ['src', 'alt'],
  span: ['class'],
  div: ['class'],
}

export function sanitizeHtmlContent(html: string): string {
  if (!html) return ''

  const temp = document.createElement('div')
  temp.innerHTML = html

  const removeDisallowedElements = (parent: HTMLElement) => {
    const children = Array.from(parent.children)
    for (const child of children) {
      const tagName = child.tagName.toLowerCase()
      if (!ALLOWED_TAGS[tagName]) {
        child.remove()
        continue
      }

      const allowedAttrs = ALLOWED_TAGS[tagName]
      const attrs = Array.from(child.attributes)
      for (const attr of attrs) {
        if (!allowedAttrs.includes(attr.name)) {
          child.removeAttribute(attr.name)
        } else if (attr.name === 'href') {
          const href = attr.value
          if (!isValidHref(href)) {
            child.removeAttribute('href')
          }
        } else if (attr.name === 'src') {
          const src = attr.value
          if (!isValidSrc(src)) {
            child.removeAttribute('src')
          }
        }
      }

      removeDisallowedElements(child as HTMLElement)
    }
  }

  removeDisallowedElements(temp)
  return temp.innerHTML
}

function isValidHref(href: string): boolean {
  if (!href) return false
  if (href.startsWith('javascript:')) return false
  if (href.startsWith('vbscript:')) return false
  if (href.startsWith('data:')) return false
  if (href.startsWith('file:')) return false
  if (href.startsWith('mailto:') || href.startsWith('tel:')) return true
  if (href.startsWith('/')) return true
  if (href.startsWith('http://') || href.startsWith('https://')) return true
  return false
}

function isValidSrc(src: string): boolean {
  if (!src) return false
  if (src.startsWith('data:image/')) return true
  if (src.startsWith('/')) return true
  if (src.startsWith('http://') || src.startsWith('https://')) return true
  return false
}

export function sanitizeUserInput(input: string): string {
  if (!input) return ''
  let result = input.trim()
  result = result.replace(/<script[^>]*>[\s\S]*?<\/script>/gi, '')
  result = result.replace(/javascript:/gi, '')
  result = result.replace(/on\w+\s*=/gi, '')
  return result
}

export function sanitizeFilename(filename: string): string {
  if (!filename) return ''
  let result = filename.trim()
  result = result.replace(/[\\/:*?"<>|]/g, '_')
  result = result.replace(/\.\./g, '_')
  result = result.replace(/^\./g, '_')
  return result
}

export function sanitizePhone(phone: string): string {
  if (!phone) return ''
  return phone.replace(/[^0-9]/g, '')
}

export function sanitizeEmail(email: string): string {
  if (!email) return ''
  return email.trim().toLowerCase()
}

export function sanitizeHtmlAttributes(attrs: Record<string, string>): Record<string, string> {
  const result: Record<string, string> = {}
  for (const [key, value] of Object.entries(attrs)) {
    const sanitizedKey = key.toLowerCase()
    let sanitizedValue = value

    if (sanitizedKey === 'href') {
      if (!isValidHref(value)) continue
      sanitizedValue = value
    } else if (sanitizedKey === 'src') {
      if (!isValidSrc(value)) continue
      sanitizedValue = value
    } else if (sanitizedKey === 'class' || sanitizedKey === 'id') {
      sanitizedValue = value.replace(/[^a-zA-Z0-9_-]/g, '')
    } else if (sanitizedKey === 'style') {
      sanitizedValue = sanitizeStyle(value)
    } else {
      continue
    }

    result[sanitizedKey] = sanitizedValue
  }
  return result
}

function sanitizeStyle(style: string): string {
  const safeProperties = [
    'color', 'background-color', 'font-size', 'font-weight', 'text-align',
    'margin', 'padding', 'border', 'width', 'height', 'display',
    'flex', 'flex-direction', 'justify-content', 'align-items',
    'border-radius', 'box-shadow', 'opacity', 'visibility',
  ]

  const result: string[] = []
  const declarations = style.split(';')

  for (const declaration of declarations) {
    const [property, value] = declaration.split(':').map(s => s.trim())
    if (!property || !value) continue

    const lowerProperty = property.toLowerCase()
    if (!safeProperties.includes(lowerProperty)) continue

    if (lowerProperty === 'background-color' && value.includes('url')) continue

    result.push(`${property}: ${value}`)
  }

  return result.join('; ')
}
