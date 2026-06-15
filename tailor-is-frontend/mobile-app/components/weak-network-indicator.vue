<template>
  <view v-if="showIndicator" class="weak-network-indicator" :class="statusClass" @click="handleClick">
    <view class="indicator-content">
      <text class="status-icon">{{ statusIcon }}</text>
      <text class="status-text">{{ statusText }}</text>
      <view v-if="showActions" class="action-buttons">
        <text class="action-btn" @click.stop="onRetry">重试</text>
      </view>
    </view>

    <!-- 弱网降级设置面板 -->
    <view v-if="showSettings" class="settings-panel">
      <view class="settings-header">
        <text class="settings-title">网络优化设置</text>
        <text class="close-btn" @click="closeSettings">✕</text>
      </view>
      <view class="settings-body">
        <view class="setting-item">
          <text class="setting-label">低分辨率图片</text>
          <switch :checked="lowResEnabled" @change="toggleLowRes" color="#ff4d4f" />
        </view>
        <view class="setting-item">
          <text class="setting-label">减少数据请求</text>
          <switch :checked="degradeEnabled" @change="toggleDegrade" color="#ff4d4f" />
        </view>
        <view class="setting-item">
          <text class="setting-label">自动同步</text>
          <switch :checked="autoSyncEnabled" @change="toggleAutoSync" color="#ff4d4f" />
        </view>
        <view v-if="pendingCount > 0" class="sync-info">
          <text class="sync-text">待同步: {{ pendingCount }} 项</text>
          <text class="sync-btn" @click="triggerManualSync">立即同步</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import {
  isOnline,
  isWeakNetwork,
  onNetworkChange,
  getNetworkStatusText,
  getNetworkStatusIcon,
  shouldUseLowResImage,
  shouldDegradeRequests
} from '@/utils/networkMonitor'
import { getPendingSyncCount, triggerSync } from '@/utils/autoSync'

interface Props {
  alwaysShow?: boolean
  showActions?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  alwaysShow: false,
  showActions: false
})

const showSettings = ref(false)
const lowResEnabled = ref(false)
const degradeEnabled = ref(false)
const autoSyncEnabled = ref(true)
const pendingCount = ref(0)
const lastClickTime = ref(0)

let networkUnsubscribe: (() => void) | null = null

const showIndicator = computed(() => {
  if (props.alwaysShow) return true
  return !isOnline.value || isWeakNetwork.value
})

const statusClass = computed(() => {
  if (!isOnline.value) return 'offline'
  if (isWeakNetwork.value) return 'weak'
  return 'online'
})

const statusIcon = computed(() => getNetworkStatusIcon())
const statusText = computed(() => getNetworkStatusText())

onMounted(() => {
  networkUnsubscribe = onNetworkChange(() => {
    pendingCount.value = getPendingSyncCount()
    lowResEnabled.value = shouldUseLowResImage()
    degradeEnabled.value = shouldDegradeRequests()
  })
  pendingCount.value = getPendingSyncCount()
})

onUnmounted(() => {
  if (networkUnsubscribe) networkUnsubscribe()
})

function handleClick() {
  const now = Date.now()
  if (now - lastClickTime.value < 500) return
  lastClickTime.value = now

  if (showIndicator.value) {
    showSettings.value = !showSettings.value
  }
}

function closeSettings() {
  showSettings.value = false
}

function onRetry() {
  triggerManualSync()
}

function triggerManualSync() {
  triggerSync()
  uni.showToast({ title: '正在同步...', icon: 'loading', duration: 2000 })
}

function toggleLowRes(event: { detail: { value: boolean } }) {
  lowResEnabled.value = event.detail.value
  uni.setStorageSync('__low_res_image__', lowResEnabled.value)
  uni.showToast({ title: lowResEnabled.value ? '已启用低分辨率图片' : '已关闭低分辨率图片', icon: 'none' })
}

function toggleDegrade(event: { detail: { value: boolean } }) {
  degradeEnabled.value = event.detail.value
  uni.setStorageSync('__degrade_requests__', degradeEnabled.value)
  uni.showToast({ title: degradeEnabled.value ? '已启用请求降级' : '已关闭请求降级', icon: 'none' })
}

function toggleAutoSync(event: { detail: { value: boolean } }) {
  autoSyncEnabled.value = event.detail.value
  uni.setStorageSync('__auto_sync__', autoSyncEnabled.value)
  uni.showToast({ title: autoSyncEnabled.value ? '已启用自动同步' : '已关闭自动同步', icon: 'none' })
}

// 导出供外部使用
defineExpose({
  showSettings,
  triggerManualSync
})
</script>

<style lang="scss" scoped>
.weak-network-indicator {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 9999;

  &.online {
    display: none;
  }

  &.weak {
    background: #fff7e6;
    border-bottom: 2rpx solid #ffd591;
  }

  &.offline {
    background: #fff1f0;
    border-bottom: 2rpx solid #ffa39e;
  }
}

.indicator-content {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12rpx 30rpx;
  font-size: 24rpx;

  .status-icon {
    margin-right: 8rpx;
    font-size: 28rpx;
  }

  .status-text {
    color: #666;
  }

  .action-buttons {
    margin-left: 20rpx;

    .action-btn {
      color: #1890ff;
      font-size: 24rpx;
      padding: 4rpx 12rpx;
    }
  }
}

.settings-panel {
  background: #fff;
  margin: 16rpx;
  border-radius: 16rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.1);
  overflow: hidden;

  .settings-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 24rpx 30rpx;
    border-bottom: 2rpx solid #f5f5f5;

    .settings-title {
      font-size: 30rpx;
      font-weight: bold;
      color: #333;
    }

    .close-btn {
      font-size: 32rpx;
      color: #999;
      padding: 0 8rpx;
    }
  }

  .settings-body {
    padding: 20rpx 30rpx;

    .setting-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 20rpx 0;
      border-bottom: 2rpx solid #f5f5f5;

      &:last-child {
        border-bottom: none;
      }

      .setting-label {
        font-size: 28rpx;
        color: #333;
      }
    }

    .sync-info {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 20rpx 0;
      background: #e6f7ff;
      border-radius: 12rpx;
      margin-top: 16rpx;
      padding: 20rpx 24rpx;

      .sync-text {
        font-size: 26rpx;
        color: #1890ff;
      }

      .sync-btn {
        font-size: 26rpx;
        color: #1890ff;
        font-weight: bold;
        padding: 8rpx 20rpx;
        border: 2rpx solid #1890ff;
        border-radius: 24rpx;
      }
    }
  }
}
</style>
