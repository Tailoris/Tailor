/**
 * 原生组件入口.
 *
 * <p>导出所有原生组件并注册为全局组件，供 UniApp 项目中直接使用。
 *
 * <h3>使用方式</h3>
 * <pre>
 * // main.js 中导入
 * import { registerNativeComponents } from '@/native-components'
 * registerNativeComponents(app)
 * </pre>
 */

import type { App } from 'vue'
import TransactionCard from './transaction-card/transaction-card.vue'
import AfterSaleForm from './aftersale-form/aftersale-form.vue'
import AfterSaleProgress from './aftersale-progress/aftersale-progress.vue'

/** 原生组件注册表 */
const nativeComponents = {
  TransactionCard,
  AfterSaleForm,
  AfterSaleProgress
}

/**
 * 注册所有原生组件为全局组件.
 *
 * @param app Vue 应用实例
 */
export function registerNativeComponents(app: App): void {
  for (const [name, component] of Object.entries(nativeComponents)) {
    app.component(name, component)
  }
}

/** 单独导出组件 */
export { TransactionCard }
export { AfterSaleForm }
export { AfterSaleProgress }

/** 默认导出注册表 */
export default nativeComponents