# Tailor IS 离线能力指南

> 本文档描述 Tailor IS 移动应用的离线功能、自动同步机制和弱网适配策略。

## 目录

- [概述](#概述)
- [架构概览](#架构概览)
- [离线存储工具](#离线存储工具)
- [产品缓存](#产品缓存)
- [离线工单编辑](#离线工单编辑)
- [网络状态监控](#网络状态监控)
- [自动同步机制](#自动同步机制)
- [弱网 UI 降级](#弱网-ui-降级)
- [请求层离线/弱网处理](#请求层离线弱网处理)
- [测试指南](#测试指南)
- [最佳实践](#最佳实践)

---

## 概述

Task-12 为 Tailor IS 移动应用增加了完整的离线和弱网适配能力，使用户在网络不稳定或完全离线的情况下仍能正常使用核心功能。

### 核心能力

| 能力 | 描述 |
|------|------|
| 离线数据存储 | 基于 IndexedDB + localStorage 的双层存储 |
| 离线工单编辑 | 离线创建/编辑售后工单，联网后自动同步 |
| 离线产品浏览 | TTL 缓存产品数据，离线时可浏览已缓存内容 |
| 网络状态监控 | 实时监测在线/离线/弱网状态 |
| 自动同步 | 网络恢复时自动提交待同步操作 |
| 弱网降级 | 低分辨率图片、减少 API 请求、动态超时调整 |

---

## 架构概览

```
┌─────────────────────────────────────────────────────┐
│                    应用层 (Pages)                     │
│  offline-aftersale.vue │ product/detail.vue │ ...   │
└──────────┬──────────────────────────┬───────────────┘
           │                          │
┌──────────▼──────────┐   ┌───────────▼──────────────┐
│  弱网指示器组件      │   │  业务 API 层 (api/*.ts)   │
│  weak-network-       │   │  ↓                       │
│  indicator.vue       │   │  request.ts (增强版)     │
└─────────────────────┘   └───────────┬──────────────┘
                                      │
                    ┌─────────────────┼─────────────────┐
                    │                 │                 │
          ┌─────────▼──────┐ ┌───────▼───────┐ ┌───────▼───────┐
          │ networkMonitor │ │  autoSync     │ │ productCache  │
          │ .ts            │ │  .ts          │ │  .ts          │
          └─────────┬──────┘ └───────┬───────┘ └───────┬───────┘
                    │                │                 │
          ┌─────────▼────────────────▼─────────────────▼───────┐
          │              offlineStorage.ts                      │
          │         (IndexedDB + localStorage)                  │
          └─────────────────────────────────────────────────────┘
```

---

## 离线存储工具

**文件**: `utils/offlineStorage.ts`

基于 IndexedDB + localStorage 的双层存储方案：
- **大数据**（>5KB）自动使用 IndexedDB 存储
- **小数据** 使用 localStorage 存储
- 自动降级：IndexedDB 不可用时自动使用 localStorage

### API

```typescript
import { saveOfflineData, getOfflineData, deleteOfflineData } from '@/utils/offlineStorage'

// 保存离线数据
await saveOfflineData('user_preferences', { theme: 'dark', lang: 'zh' })

// 获取离线数据
const prefs = await getOfflineData('user_preferences')

// 删除离线数据
await deleteOfflineData('user_preferences')

// 同步队列管理
import { enqueueSync, getSyncQueueItems, dequeueSync } from '@/utils/offlineStorage'

// 将操作加入同步队列
enqueueSync({
  type: 'create',
  entity: 'aftersale',
  payload: { orderNo: 'ORD123', reason: 'quality' }
})

// 获取待同步队列
const pending = getSyncQueueItems()

// 同步成功后从队列移除
dequeueSync(itemId)
```

### SyncQueueItem 结构

```typescript
interface SyncQueueItem {
  id: string            // 唯一标识
  type: 'create' | 'update' | 'delete'
  entity: string        // 实体名称
  payload: Record<string, unknown>
  timestamp: number     // 创建时间戳
  retryCount: number    // 已重试次数
}
```

---

## 产品缓存

**文件**: `utils/productCache.ts`

TTL 驱动的离线产品数据缓存，支持多种数据类型和自动过期清理。

### 支持的缓存类型

| 类型 | 默认 TTL | 说明 |
|------|----------|------|
| 产品详情 | 30 分钟 | 单个产品的完整信息 |
| 产品列表 | 15 分钟 | 分页列表数据 |
| 分类数据 | 60 分钟 | 商品分类树 |
| 秒杀产品 | 5 分钟 | 限时秒杀数据 |
| 热门产品 | 10 分钟 | 热销商品 |
| 搜索结果 | 10 分钟 | 搜索关键词结果 |

### API

```typescript
import {
  cacheProductDetail,
  getCachedProductDetail,
  cacheProductList,
  getCachedProductList,
  hasProductCache,
  invalidateProductCache
} from '@/utils/productCache'

// 缓存产品详情
await cacheProductDetail(123, productData)

// 获取缓存（过期自动返回 null）
const cached = await getCachedProductDetail(123)
if (cached) {
  // 使用缓存数据
} else {
  // 从 API 获取
}

// 缓存搜索结果
await cacheSearchResults('连衣裙', { pageNum: 1 }, searchResults)

// 清理过期缓存（建议定时调用）
await cleanupProductCache()
```

### 使用示例：带缓存的产品详情页

```typescript
async function loadProduct(id: number) {
  // 1. 先尝试从缓存加载
  const cached = await getCachedProductDetail(id)
  if (cached) {
    product.value = cached
    loading.value = false
  }

  // 2. 同时发起 API 请求（刷新缓存）
  try {
    const res = await getProductDetail(id)
    product.value = res.data
    // 更新缓存
    await cacheProductDetail(id, res.data)
  } catch (e) {
    if (!cached) {
      // 无缓存且请求失败
      uni.showToast({ title: '加载失败', icon: 'none' })
    }
    // 有缓存时静默失败
  } finally {
    loading.value = false
  }
}
```

---

## 离线工单编辑

**文件**: `pages/order/offline-aftersale.vue`

允许用户在离线状态下创建和编辑售后工单。

### 功能特性

1. **网络状态实时显示** - 顶部横幅显示当前网络状态
2. **草稿自动保存** - 表单数据自动保存为草稿
3. **离线提交到队列** - 离线时提交操作加入同步队列
4. **自动同步** - 网络恢复后自动提交待同步工单
5. **同步状态展示** - 显示待同步工单列表及同步状态

### 页面路由

在 `pages.json` 中添加路由配置：

```json
{
  "path": "pages/order/offline-aftersale",
  "style": {
    "navigationBarTitleText": "离线售后工单"
  }
}
```

---

## 网络状态监控

**文件**: `utils/networkMonitor.ts`

实时监测网络状态变化，提供在线/离线/弱网检测。

### 网络状态

```typescript
interface NetworkStatus {
  online: boolean                    // 是否在线
  weak: boolean                      // 是否弱网
  type: 'wifi' | 'cellular' | ...    // 网络类型
  speed: number                      // 预估下载速度 (Mbps)
  rtt: number                        // 往返延迟 (ms)
  effectiveType: '4g' | '3g' | ...   // 有效网络类型
}
```

### 响应式状态导出

```typescript
import { isOnline, isWeakNetwork, networkType } from '@/utils/networkMonitor'

// Vue 响应式状态，可直接在模板中使用
console.log(isOnline.value)    // true/false
console.log(isWeakNetwork.value)  // true/false
```

### 订阅网络变化

```typescript
import { onNetworkChange } from '@/utils/networkMonitor'

const unsubscribe = onNetworkChange((status) => {
  console.log('网络状态变化:', status)
  if (status.online) {
    console.log('网络已恢复')
  } else {
    console.log('网络已断开')
  }
})

// 取消订阅
unsubscribe()
```

### 弱网判断标准

| 条件 | 阈值 |
|------|------|
| RTT（往返延迟） | > 500ms |
| 下载速度 | < 0.5 Mbps |
| 有效网络类型 | slow-2g, 2g |

### 工具方法

```typescript
import {
  shouldUseLowResImage,
  shouldDegradeRequests,
  getRecommendedTimeout,
  getNetworkStatusText
} from '@/utils/networkMonitor'

// 是否应该使用低分辨率图片
if (shouldUseLowResImage()) {
  imageUrl = imageUrl + '?w=300'
}

// 是否应该降级请求
if (shouldDegradeRequests()) {
  // 减少非必要的 API 调用
}

// 获取推荐的超时时间
const timeout = getRecommendedTimeout()  // 弱网 30s, 正常 15s
```

### 初始化

在应用入口（`main.js`）初始化网络监控：

```javascript
import { initNetworkMonitor } from '@/utils/networkMonitor'
import { initAutoSync } from '@/utils/autoSync'

initNetworkMonitor()
initAutoSync()
```

---

## 自动同步机制

**文件**: `utils/autoSync.ts`

管理离线操作队列，在网络恢复时自动同步。

### 同步策略

- **并发控制**：最多 3 个请求同时同步
- **指数退避**：重试延迟 2s → 4s → 8s → ... → 最大 60s
- **最大重试**：5 次重试，超过后标记为失败
- **随机抖动**：避免"雷击效应"（多客户端同时重试）

### 注册同步处理器

```typescript
import { registerSyncHandler, triggerSync } from '@/utils/autoSync'

// 注册售后工单同步处理器
registerSyncHandler('aftersale', async (item) => {
  const { url, method, data } = item.payload
  await request(url, method, data)
})

// 手动触发同步
triggerSync()
```

### 同步进度订阅

```typescript
import { onSyncProgress } from '@/utils/autoSync'

const unsubscribe = onSyncProgress((progress) => {
  console.log(`同步进度: ${progress.completed}/${progress.total}`)
  console.log('当前状态:', progress.status)  // idle | syncing | paused | error
})
```

### 冲突处理

支持 4 种冲突解决策略：

```typescript
type ConflictStrategy =
  | 'server'   // 以服务器数据为准（默认）
  | 'local'    // 以本地数据为准
  | 'newest'   // 以时间最新的数据为准
  | 'manual'   // 弹出对话框让用户选择
```

---

## 弱网 UI 降级

**文件**: `components/weak-network-indicator.vue`

显示网络状态指示器，提供网络优化设置面板。

### 使用方式

```vue
<template>
  <view class="page">
    <!-- 弱网指示器 -->
    <weak-network-indicator />
    <!-- 页面内容 -->
  </view>
</template>

<script setup>
import WeakNetworkIndicator from '@/components/weak-network-indicator.vue'
</script>
```

### Props

| Prop | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| alwaysShow | boolean | false | 是否始终显示（用于调试） |
| showActions | boolean | false | 是否显示操作按钮 |

### 设置面板功能

- **低分辨率图片**：启用后自动加载压缩版图片
- **减少数据请求**：启用后跳过非必要的 API 请求
- **自动同步**：启用/禁用网络恢复时的自动同步
- **手动同步**：一键触发待同步项提交

---

## 请求层离线/弱网处理

**文件**: `api/request.ts`（已增强）

### 新增功能

1. **离线请求队列**：离线时写请求自动加入队列
2. **弱网超时调整**：弱网时自动延长超时至 30s
3. **自动重试**：失败请求最多重试 3 次（指数退避）
4. **请求降级**：弱网时跳过非必要的 GET 请求

### RequestConfig 扩展

```typescript
interface RequestConfig {
  noAuth?: boolean
  showLoading?: boolean
  hideError?: boolean
  offlineQueue?: boolean     // 离线时是否加入队列（默认 true）
  retryCount?: number        // 自定义重试次数（默认 3）
  timeout?: number           // 自定义超时时间
  ignoreOffline?: boolean    // 忽略离线状态强制请求
}
```

### 使用示例

```typescript
import { post, get } from '@/api/request'

// 离线时不加入队列
await post('order/create', data, { offlineQueue: false })

// 自定义重试和超时
await get('product/list', { pageNum: 1 }, {
  retryCount: 5,
  timeout: 60000
})

// 忽略离线状态
await get('critical/data', {}, { ignoreOffline: true })
```

---

## 测试指南

### 弱网测试场景

| 场景 | 测试方法 | 预期结果 |
|------|----------|----------|
| 完全离线 | 关闭 Wi-Fi 和移动数据 | 显示离线横幅，写请求加入队列 |
| 弱网模拟 | Chrome DevTools → Network → Slow 3G | 显示弱网指示器，超时延长 |
| 网络恢复 | 离线 → 在线切换 | 3 秒后自动触发同步 |
| 频繁切换 | 快速切换在线/离线 | 不丢失数据，同步状态正确 |
| 弱网浏览 | 限制带宽至 100kbps | 加载低分辨率图片，跳过非必要请求 |

### Chrome DevTools 弱网模拟

1. 打开 DevTools (F12)
2. 切换到 Network 面板
3. 在 Throttling 下拉菜单中选择：
   - **Fast 3G**: 1.6Mbps / 150ms RTT
   - **Slow 3G**: 500kbps / 400ms RTT
   - **Offline**: 完全离线

### 离线功能测试清单

- [ ] 离线时能打开已缓存的产品详情页
- [ ] 离线时能创建售后工单并保存草稿
- [ ] 离线时能浏览已缓存的产品列表
- [ ] 网络恢复后离线工单自动同步
- [ ] 同步失败后自动重试（指数退避）
- [ ] 弱网时图片加载降级
- [ ] 弱网时请求超时自动延长
- [ ] 同步冲突正确处理

### 手动测试步骤

```bash
# 1. 启动开发服务器
cd tailor-is-frontend/mobile-app
npm run dev:h5

# 2. 打开浏览器，开启 DevTools 网络模拟

# 3. 测试离线产品浏览
# - 先在线访问产品详情（建立缓存）
# - 切换到 Offline 模式
# - 再次访问同一产品，应从缓存加载

# 4. 测试离线工单
# - 切换到 Offline 模式
# - 访问离线售后工单页面
# - 填写表单并提交
# - 切换到 Online 模式
# - 观察工单自动同步

# 5. 测试弱网降级
# - 切换到 Slow 3G
# - 观察弱网指示器出现
# - 检查图片是否降级加载
```

---

## 最佳实践

### 1. 缓存策略

```typescript
// ✅ 推荐：先展示缓存，再刷新
const cached = await getCachedProductDetail(id)
if (cached) {
  product.value = cached  // 立即展示
}
try {
  const res = await getProductDetail(id)
  product.value = res.data
  await cacheProductDetail(id, res.data)
} catch {
  // 静默失败，用户仍能看到缓存数据
}
```

### 2. 错误处理

```typescript
// ✅ 推荐：区分离线错误和网络错误
try {
  await submitOrder(data)
} catch (e) {
  if (e.message.includes('Offline')) {
    uni.showToast({ title: '已保存，联网后自动提交', icon: 'none' })
  } else {
    uni.showToast({ title: '提交失败', icon: 'none' })
  }
}
```

### 3. 同步处理器注册

```typescript
// ✅ 推荐：在应用启动时注册所有同步处理器
import { registerSyncHandler } from '@/utils/autoSync'

registerSyncHandler('aftersale', async (item) => {
  // 处理售后工单同步
})

registerSyncHandler('order', async (item) => {
  // 处理订单同步
})

registerSyncHandler('cart', async (item) => {
  // 处理购物车同步
})
```

### 4. 存储清理

```typescript
// ✅ 推荐：定期清理过期缓存
// 在 App.vue 的 onShow 中调用
import { cleanupProductCache } from '@/utils/productCache'

onShow(() => {
  cleanupProductCache()
})
```

---

## 文件清单

| 文件 | 用途 |
|------|------|
| `utils/offlineStorage.ts` | 离线数据存储（IndexedDB + localStorage） |
| `utils/productCache.ts` | TTL 产品数据缓存 |
| `utils/networkMonitor.ts` | 网络状态监控 |
| `utils/autoSync.ts` | 自动同步机制 |
| `components/weak-network-indicator.vue` | 弱网 UI 指示器 |
| `pages/order/offline-aftersale.vue` | 离线工单编辑页面 |
| `api/request.ts` | 增强的请求层（离线/弱网处理） |
| `api/types.ts` | 扩展的 RequestConfig 类型 |
