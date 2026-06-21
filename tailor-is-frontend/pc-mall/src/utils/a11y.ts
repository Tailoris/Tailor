/**
 * 无障碍辅助函数 - UX-P3-01 WCAG 2.1 AA 无障碍合规
 *
 * 提供屏幕阅读器播报、焦点陷阱、跳过导航等辅助函数。
 */

/** 创建或获取屏幕阅读器专用 live region 元素 */
let liveRegionEl: HTMLElement | null = null

function getLiveRegion(): HTMLElement {
  if (!liveRegionEl) {
    liveRegionEl = document.createElement('div')
    liveRegionEl.setAttribute('aria-live', 'polite')
    liveRegionEl.setAttribute('aria-atomic', 'true')
    liveRegionEl.className = 'sr-only'
    document.body.appendChild(liveRegionEl)
  }
  return liveRegionEl
}

/**
 * 为屏幕阅读器动态播报消息
 * @param message - 播报文本
 * @param priority - 'polite'（不打断）| 'assertive'（立即打断）
 */
export function announceToScreenReader(message: string, priority: 'polite' | 'assertive' = 'polite'): void {
  const region = getLiveRegion()
  region.setAttribute('aria-live', priority)
  if (priority === 'assertive') {
    region.setAttribute('role', 'alert')
  } else {
    region.setAttribute('role', 'status')
  }
  // 先清空再设置，确保每次都能触发播报
  region.textContent = ''
  requestAnimationFrame(() => {
    region.textContent = message
  })
}

/**
 * 焦点陷阱 - 限制 Tab 焦点在指定元素内循环
 * 用于模态框、对话框等弹出层
 * @param element - 焦点陷阱容器元素
 * @returns 清理函数，调用后解除焦点陷阱
 */
export function focusTrap(element: HTMLElement): () => void {
  const focusableSelector = [
    'a[href]',
    'button:not([disabled])',
    'input:not([disabled])',
    'select:not([disabled])',
    'textarea:not([disabled])',
    '[tabindex]:not([tabindex="-1"])',
  ].join(', ')

  function getFocusableElements(): HTMLElement[] {
    return Array.from(element.querySelectorAll<HTMLElement>(focusableSelector))
  }

  const handleKeyDown = (e: KeyboardEvent): void => {
    if (e.key !== 'Tab') return

    const focusable = getFocusableElements()
    if (focusable.length === 0) return

    const first = focusable[0]
    const last = focusable[focusable.length - 1]

    if (e.shiftKey) {
      if (document.activeElement === first) {
        e.preventDefault()
        last.focus()
      }
    } else {
      if (document.activeElement === last) {
        e.preventDefault()
        first.focus()
      }
    }
  }

  // 将焦点移至第一个可聚焦元素
  const focusable = getFocusableElements()
  if (focusable.length > 0) {
    focusable[0].focus()
  }

  element.addEventListener('keydown', handleKeyDown)
  return () => {
    element.removeEventListener('keydown', handleKeyDown)
  }
}

/**
 * 跳转到主内容区域
 * @param targetId - 主内容元素的 ID，默认 'main-content'
 */
export function skipToContent(targetId: string = 'main-content'): void {
  const target = document.getElementById(targetId)
  if (target) {
    target.setAttribute('tabindex', '-1')
    target.focus()
    // 移除 tabindex 避免后续 Tab 导航异常
    target.addEventListener('blur', () => {
      target.removeAttribute('tabindex')
    }, { once: true })
  }
}

/**
 * 获取元素的 ARIA 标签
 * 按优先级：aria-label > aria-labelledby > 内部文本
 * @param element - 目标元素
 * @returns ARIA 标签文本
 */
export function getAriaLabel(element: HTMLElement): string {
  const ariaLabel = element.getAttribute('aria-label')
  if (ariaLabel && ariaLabel.trim()) {
    return ariaLabel.trim()
  }

  const labelledBy = element.getAttribute('aria-labelledby')
  if (labelledBy) {
    const labelEl = document.getElementById(labelledBy)
    if (labelEl) {
      return labelEl.textContent?.trim() || ''
    }
  }

  return element.textContent?.trim() || ''
}

/**
 * 生成唯一 ID，用于关联 label 和表单元素
 * @param prefix - ID 前缀
 */
let idCounter = 0
export function generateA11yId(prefix: string = 'a11y'): string {
  return `${prefix}-${++idCounter}-${Date.now().toString(36)}`
}