import type { App, Directive } from 'vue'

/**
 * 无障碍指令集 - 修复 F-M08
 *
 * <p>提供一组无障碍增强指令，简化组件无障碍属性管理。</p>
 *
 * <h3>支持指令</h3>
 * <ul>
 *   <li>v-a11y-focus: 自动聚焦元素</li>
 *   <li>v-a11y-label: 动态aria-label</li>
 *   <li>v-a11y-live: 动态aria-live</li>
 *   <li>v-a11y-key: 键盘事件增强</li>
 * </ul>
 */

interface FocusHTMLElement extends HTMLElement {
  focus(): void
}

/**
 * 自动聚焦指令
 */
const a11yFocus: Directive<FocusHTMLElement, boolean | undefined> = {
  mounted(el, binding) {
    if (binding.value !== false) {
      // 等待DOM更新完成后聚焦
      requestAnimationFrame(() => {
        el.focus()
      })
    }
  }
}

/**
 * 动态aria-label指令
 */
const a11yLabel: Directive<HTMLElement, string> = {
  mounted(el, binding) {
    el.setAttribute('aria-label', binding.value)
  },
  updated(el, binding) {
    if (binding.value !== binding.oldValue) {
      el.setAttribute('aria-label', binding.value)
    }
  }
}

/**
 * 动态aria-live指令（屏幕阅读器实时播报）
 */
const a11yLive: Directive<HTMLElement, 'polite' | 'assertive'> = {
  mounted(el, binding) {
    el.setAttribute('aria-live', binding.value)
    el.setAttribute('role', binding.value === 'assertive' ? 'alert' : 'status')
  }
}

/**
 * 键盘事件增强指令 - 为非交互元素添加键盘可访问性
 */
const a11yKey: Directive<HTMLElement, (e: KeyboardEvent) => void> = {
  mounted(el, binding) {
    el.setAttribute('tabindex', '0')
    el.setAttribute('role', el.getAttribute('role') || 'button')
    el.addEventListener('keydown', (e: KeyboardEvent) => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault()
        binding.value(e)
      }
    })
  }
}

/**
 * 全局注册
 */
export function installA11yDirectives(app: App): void {
  app.directive('a11y-focus', a11yFocus)
  app.directive('a11y-label', a11yLabel)
  app.directive('a11y-live', a11yLive)
  app.directive('a11y-key', a11yKey)
}

export { a11yFocus, a11yLabel, a11yLive, a11yKey }
