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

/**
 * 原生组件注册表.
 *
 * TODO: 以下组件需要实现原生渲染以提升性能
 *   - ProductCard: 商品卡片组件（小程序端需原生渲染，解决复杂布局性能问题）
 *   - ImChat: IM 聊天组件（需 WebSocket 长连接 + 原生消息列表渲染）
 *   - SignaturePad: 签名板组件（需 Canvas 原生绑定）
 *   - MapView: 地图组件（需原生地图 SDK 集成）
 *   - PaymentKeyboard: 支付密码键盘（需原生键盘实现，安全要求）
 *   - ImageCropper: 图片裁剪组件（需原生 Canvas 操作）
 *   - LivePlayer: 直播播放器组件（需原生播放器 SDK）
 * 参考: UniApp 原生组件开发文档
 */
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