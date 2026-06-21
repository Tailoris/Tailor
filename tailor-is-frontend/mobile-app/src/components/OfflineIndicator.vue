<template>
  <view
    v-if="visible"
    class="offline-indicator"
    :class="{
      'offline-indicator--offline': !isOnline,
      'offline-indicator--online': isOnline && wasOffline,
      'offline-indicator--dismissing': isDismissing,
    }"
    @touchstart="handleTouchStart"
    @touchend="handleTouchEnd"
  >
    <view class="offline-indicator__content">
      <view class="offline-indicator__icon">
        <text v-if="!isOnline">📡</text>
        <text v-else-if="wasOffline">✅</text>
        <text v-else>🔄</text>
      </view>

      <view class="offline-indicator__text">
        <text class="offline-indicator__title">
          {{ statusTitle }}
        </text>
        <text v-if="statusMessage" class="offline-indicator__message">
          {{ statusMessage }}
        </text>
      </view>

      <view class="offline-indicator__actions">
        <!-- 同步中 -->
        <view v-if="isSyncing" class="offline-indicator__sync-progress">
          <text class="offline-indicator__sync-text">同步中...</text>
        </view>

        <!-- 待同步数量 -->
        <view v-else-if="pendingCount > 0" class="offline-indicator__badge" @click.stop="handleSyncClick">
          <text class="offline-indicator__badge-text">{{ pendingCount }}</text>
        </view>

        <!-- 关闭按钮 -->
        <view
          v-if="isOnline && wasOffline"
          class="offline-indicator__close"
          @click.stop="dismiss"
        >
          <text class="offline-indicator__close-icon">✕</text>
        </view>
      </view>
    </view>

    <!-- 展开的同步详情 -->
    <view v-if="showSyncDetails" class="offline-indicator__details">
      <view class="offline-indicator__details-header">
        <text class="offline-indicator__details-title">离线操作</text>
        <text class="offline-indicator__details-close" @click="showSyncDetails = false">✕</text>
      </view>
      <view class="offline-indicator__details-body">
        <view v-if="pendingCount === 0" class="offline-indicator__empty">
          <text>暂无待同步的离线操作</text>
        </view>
        <view v-else class="offline-indicator__sync-list">
          <view
            v-for="item in pendingActions"
            :key="item.id"
            class="offline-indicator__sync-item"
          >
            <text class="offline-indicator__sync-item-type">
              {{ getActionLabel(item.type) }}
            </text>
            <text class="offline-indicator__sync-item-time">
              {{ formatTime(item.timestamp) }}
            </text>
          </view>
        </view>
        <view v-if="pendingCount > 0" class="offline-indicator__sync-now">
          <text class="offline-indicator__sync-now-btn" @click="handleSyncNow">
            立即同步全部
          </text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { isOnline } from '@/utils/networkMonitor'
import { getPendingSyncCount, triggerSync } from '@/utils/autoSync'
import { getPendingSyncActions, type SyncQueueItem } from '@/src/service-worker/offline-db'

interface Props {
  /** 是否始终显示 */
  alwaysShow?: boolean
  /** 是否允许滑动关闭 */
  swipeToDismiss?: boolean
  /** 自动关闭延迟（毫秒），0 表示不自动关闭 */
  autoDismissDelay?: number
}

const props = withDefaults(defineProps<Props>(), {
  alwaysShow: false,
  swipeToDismiss: true,
  autoDismissDelay: 5000,
})

const emit = defineEmits<{
  (e: 'dismiss'): void
  (e: 'sync'): void
}>()

// 状态
const wasOffline = ref(false)
const isDismissing = ref(false)
const isSyncing = ref(false)
const showSyncDetails = ref(false)
const pendingCount = ref(0)
const pendingActions = ref<SyncQueueItem[]>([])
const visible = ref(false)

// 定时器
let autoDismissTimer: ReturnType<typeof setTimeout> | null = null
let syncPollTimer: ReturnType<typeof setInterval> | null = null
let networkUnsubscribe: (() => void) | null = null

// 触摸滑动相关
let touchStartY = 0
let touchStartX = 0

// 计算属性
const statusTitle = computed(() => {
  if (!isOnline.value) return '当前处于离线模式'
  if (isSyncing.value) return '正在同步离线操作...'
  if (wasOffline.value && pendingCount.value > 0) return '网络已恢复'
  if (wasOffline.value) return '网络已恢复'
  return '网络连接正常'
})

const statusMessage = computed(() => {
  if (!isOnline.value) {
    if (pendingCount.value > 0) {
      return `网络断开，${pendingCount.value} 项操作将在恢复后自动同步`
    }
    return '请检查网络连接后重试'
  }
  if (wasOffline.value && pendingCount.value > 0) {
    return `正在同步 ${pendingCount.value} 项离线操作...`
  }
  return ''
})

// 方法
function getActionLabel(type: SyncQueueItem['type']): string {
  const labels: Record<SyncQueueItem['type'], string> = {
    addToCart: '加入购物车',
    removeFromCart: '移除购物车',
    updateCart: '更新购物车',
    addToWishlist: '加入收藏',
    removeFromWishlist: '取消收藏',
  }
  return labels[type] || type
}

function formatTime(timestamp: number): string {
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now.getTime() - timestamp

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`

  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hours}:${minutes}`
}

function dismiss(): void {
  isDismissing.value = true
  setTimeout(() => {
    visible.value = false
    wasOffline.value = false
    isDismissing.value = false
    emit('dismiss')
  }, 300)
}

function handleSyncClick(): void {
  showSyncDetails.value = !showSyncDetails.value
}

async function handleSyncNow(): Promise<void> {
  if (isSyncing.value) return

  isSyncing.value = true
  showSyncDetails.value = false

  try {
    triggerSync()
    emit('sync')
  } finally {
    isSyncing.value = false
  }
}

function handleTouchStart(e: TouchEvent): void {
  if (!props.swipeToDismiss) return
  touchStartY = e.touches[0].clientY
  touchStartX = e.touches[0].clientX
}

function handleTouchEnd(e: TouchEvent): void {
  if (!props.swipeToDismiss) return
  const touchEndY = e.changedTouches[0].clientY
  const touchEndX = e.changedTouches[0].clientX

  // 向上滑动超过 50px 且水平位移小于 30px 时关闭
  if (
    touchStartY - touchEndY > 50 &&
    Math.abs(touchStartX - touchEndX) < 30
  ) {
    dismiss()
  }
}

async function updatePendingCount(): Promise<void> {
  try {
    pendingCount.value = getPendingSyncCount()
    if (pendingCount.value > 0) {
      pendingActions.value = await getPendingSyncActions()
    }
  } catch {
    pendingCount.value = 0
  }
}

// 网络状态变化处理
function handleNetworkChange(): void {
  if (!isOnline.value) {
    // 进入离线
    visible.value = true
    wasOffline.value = false
    if (autoDismissTimer) {
      clearTimeout(autoDismissTimer)
      autoDismissTimer = null
    }
  } else if (wasOffline.value || visible.value) {
    // 恢复在线
    visible.value = true
    updatePendingCount()
    // 自动同步
    handleSyncNow()
    // 自动关闭
    if (props.autoDismissDelay > 0 && pendingCount.value === 0) {
      autoDismissTimer = setTimeout(() => {
        dismiss()
      }, props.autoDismissDelay)
    }
  }
  wasOffline.value = !isOnline.value
}

// 生命周期
onMounted(async () => {
  // 初始化网络状态
  if (!isOnline.value) {
    visible.value = true
    wasOffline.value = true
  } else if (props.alwaysShow) {
    visible.value = true
  }

  // 监听网络变化
  const { onNetworkChange } = await import('@/utils/networkMonitor')
  networkUnsubscribe = onNetworkChange(() => {
    handleNetworkChange()
  })

  // 初始检查待同步队列
  await updatePendingCount()

  // 定期轮询待同步数量
  syncPollTimer = setInterval(() => {
    updatePendingCount()
  }, 5000)
})

onUnmounted(() => {
  if (autoDismissTimer) {
    clearTimeout(autoDismissTimer)
  }
  if (syncPollTimer) {
    clearInterval(syncPollTimer)
  }
  if (networkUnsubscribe) {
    networkUnsubscribe()
  }
})

defineExpose({
  dismiss,
  handleSyncNow,
})
</script>

<style lang="scss" scoped>
.offline-indicator {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 10000;
  background: #fff1f0;
  border-bottom: 2rpx solid #ffa39e;
  transition: transform 0.3s ease, opacity 0.3s ease;
  transform: translateY(0);
  opacity: 1;

  &--online {
    background: #f6ffed;
    border-bottom-color: #b7eb8f;
  }

  &--dismissing {
    transform: translateY(-100%);
    opacity: 0;
  }
}

.offline-indicator__content {
  display: flex;
  align-items: center;
  padding: 16rpx 24rpx;
  gap: 16rpx;
}

.offline-indicator__icon {
  flex-shrink: 0;
  font-size: 36rpx;
  line-height: 1;
}

.offline-indicator__text {
  flex: 1;
  min-width: 0;
}

.offline-indicator__title {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
  display: block;
}

.offline-indicator__message {
  font-size: 24rpx;
  color: #999;
  display: block;
  margin-top: 4rpx;
}

.offline-indicator__actions {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.offline-indicator__sync-progress {
  .offline-indicator__sync-text {
    font-size: 24rpx;
    color: #1890ff;
  }
}

.offline-indicator__badge {
  background: #ff4d4f;
  color: #fff;
  border-radius: 50%;
  min-width: 40rpx;
  height: 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 8rpx;

  .offline-indicator__badge-text {
    font-size: 22rpx;
    font-weight: 600;
  }
}

.offline-indicator__close {
  padding: 8rpx;

  .offline-indicator__close-icon {
    font-size: 28rpx;
    color: #999;
  }
}

// 展开的同步详情
.offline-indicator__details {
  background: #fff;
  border-top: 2rpx solid #f5f5f5;
  max-height: 400rpx;
  overflow: hidden;
}

.offline-indicator__details-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 24rpx;
  border-bottom: 2rpx solid #f5f5f5;
}

.offline-indicator__details-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
}

.offline-indicator__details-close {
  font-size: 28rpx;
  color: #999;
  padding: 8rpx;
}

.offline-indicator__details-body {
  padding: 16rpx 24rpx;
}

.offline-indicator__empty {
  padding: 40rpx 0;
  text-align: center;
  color: #999;
  font-size: 26rpx;
}

.offline-indicator__sync-list {
  max-height: 240rpx;
  overflow-y: auto;
}

.offline-indicator__sync-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 0;
  border-bottom: 2rpx solid #f5f5f5;

  &:last-child {
    border-bottom: none;
  }
}

.offline-indicator__sync-item-type {
  font-size: 26rpx;
  color: #333;
}

.offline-indicator__sync-item-time {
  font-size: 22rpx;
  color: #999;
}

.offline-indicator__sync-now {
  margin-top: 16rpx;
  text-align: center;
}

.offline-indicator__sync-now-btn {
  font-size: 26rpx;
  color: #1890ff;
  font-weight: 600;
  padding: 12rpx 32rpx;
  border: 2rpx solid #1890ff;
  border-radius: 24rpx;
  display: inline-block;
}
</style>