<template>
  <view class="offline-aftersale-page">
    <!-- 网络状态提示 -->
    <view class="network-banner" :class="isOnline ? 'online' : 'offline'">
      <text class="banner-icon">{{ isOnline ? '🟢' : '🔴' }}</text>
      <text class="banner-text">{{ isOnline ? '在线 - 数据将实时同步' : '离线 - 数据已保存到本地，联网后自动同步' }}</text>
    </view>

    <scroll-view scroll-y class="form-container">
      <!-- 关联订单 -->
      <view class="form-section">
        <text class="section-title">关联订单</text>
        <view class="form-item">
          <text class="label">订单编号</text>
          <input
            v-model="formData.orderNo"
            class="input"
            placeholder="请输入订单编号"
            :disabled="isEditing"
          />
        </view>
      </view>

      <!-- 售后类型 -->
      <view class="form-section">
        <text class="section-title">售后类型</text>
        <view class="radio-group">
          <view
            v-for="item in afterSaleTypes"
            :key="item.value"
            class="radio-item"
            :class="{ selected: formData.type === item.value }"
            @click="formData.type = item.value"
          >
            <text class="radio-icon">{{ formData.type === item.value ? '🔘' : '⚪' }}</text>
            <text class="radio-label">{{ item.label }}</text>
          </view>
        </view>
      </view>

      <!-- 商品信息 -->
      <view class="form-section">
        <text class="section-title">商品信息</text>
        <view class="form-item">
          <text class="label">商品名称</text>
          <input v-model="formData.productName" class="input" placeholder="请输入商品名称" />
        </view>
        <view class="form-item">
          <text class="label">商品数量</text>
          <input v-model.number="formData.quantity" class="input" type="number" placeholder="请输入数量" />
        </view>
      </view>

      <!-- 售后原因 -->
      <view class="form-section">
        <text class="section-title">售后原因</text>
        <view class="radio-group">
          <view
            v-for="reason in afterSaleReasons"
            :key="reason.value"
            class="radio-item"
            :class="{ selected: formData.reason === reason.value }"
            @click="formData.reason = reason.value"
          >
            <text class="radio-icon">{{ formData.reason === reason.value ? '🔘' : '⚪' }}</text>
            <text class="radio-label">{{ reason.label }}</text>
          </view>
        </view>
      </view>

      <!-- 问题描述 -->
      <view class="form-section">
        <text class="section-title">问题描述</text>
        <textarea
          v-model="formData.description"
          class="textarea"
          placeholder="请详细描述您遇到的问题"
          maxlength="500"
          :show-count="true"
        />
      </view>

      <!-- 图片上传 -->
      <view class="form-section">
        <text class="section-title">凭证图片</text>
        <view class="image-upload">
          <view v-for="(img, index) in formData.images" :key="index" class="uploaded-img">
            <image :src="img" mode="aspectFill" class="img-preview" @click="previewImage(index)" />
            <view class="img-delete" @click="removeImage(index)">✕</view>
          </view>
          <view v-if="formData.images.length < 5" class="upload-btn" @click="chooseImage">
            <text class="upload-icon">+</text>
            <text class="upload-text">上传图片</text>
          </view>
        </view>
      </view>

      <!-- 联系方式 -->
      <view class="form-section">
        <text class="section-title">联系方式</text>
        <view class="form-item">
          <text class="label">联系电话</text>
          <input v-model="formData.phone" class="input" type="tel" placeholder="请输入联系电话" />
        </view>
      </view>

      <!-- 待同步队列 -->
      <view v-if="pendingSyncs.length > 0" class="form-section sync-section">
        <text class="section-title">待同步工单 ({{ pendingSyncs.length }})</text>
        <view v-for="item in pendingSyncs" :key="item.id" class="sync-item">
          <view class="sync-info">
            <text class="sync-title">{{ item.payload.productName || '未命名工单' }}</text>
            <text class="sync-time">{{ formatTime(item.timestamp) }}</text>
            <text v-if="item.retryCount > 0" class="sync-retry">重试 {{ item.retryCount }} 次</text>
          </view>
          <view class="sync-status">
            <text class="status-tag" :class="getSyncStatusClass(item)">
              {{ getSyncStatusText(item) }}
            </text>
          </view>
        </view>
      </view>
    </scroll-view>

    <!-- 底部操作栏 -->
    <view class="action-bar">
      <button class="btn btn-draft" @click="saveDraft" :loading="saving">保存草稿</button>
      <button class="btn btn-primary" @click="submitForm" :loading="submitting" :disabled="!isOnline && !canOfflineSubmit">
        {{ isOnline ? '提交售后' : '保存到本地' }}
      </button>
    </view>

    <!-- 弱网指示器 -->
    <weak-network-indicator />
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { saveOfflineData, getOfflineData, enqueueSync, getSyncQueueItems } from '@/utils/offlineStorage'
import { isOnline, isWeakNetwork, onNetworkChange, offNetworkChange } from '@/utils/networkMonitor'
import { triggerSync, registerSyncHandler } from '@/utils/autoSync'
import { createAfterSale } from '@/api/aftersale'

interface AfterSaleForm {
  orderNo: string
  type: string
  productName: string
  quantity: number
  reason: string
  description: string
  images: string[]
  phone: string
}

const afterSaleTypes = [
  { label: '退货退款', value: 'refund' },
  { label: '换货', value: 'exchange' },
  { label: '维修', value: 'repair' },
  { label: '仅退款', value: 'refund_only' }
]

const afterSaleReasons = [
  { label: '商品质量问题', value: 'quality' },
  { label: '商品与描述不符', value: 'mismatch' },
  { label: '发货错误', value: 'wrong_item' },
  { label: '商品损坏', value: 'damaged' },
  { label: '其他原因', value: 'other' }
]

const formData = ref<AfterSaleForm>({
  orderNo: '',
  type: 'refund',
  productName: '',
  quantity: 1,
  reason: 'quality',
  description: '',
  images: [],
  phone: ''
})

const saving = ref(false)
const submitting = ref(false)
const isEditing = ref(false)
const pendingSyncs = ref(getSyncQueueItems().filter(item => item.entity === 'aftersale'))

// 离线时也可以提交到本地队列
const canOfflineSubmit = computed(() => {
  return !!(formData.value.productName && formData.value.description)
})

let networkUnsubscribe: (() => void) | null = null

onMounted(() => {
  networkUnsubscribe = onNetworkChange(handleNetworkChange)
  loadDraft()
  registerSyncHandler('aftersale', async (item) => {
    const payload = item.payload as Record<string, unknown>
    await createAfterSale({
      orderNo: (payload.orderNo as string) || '',
      type: (payload.type as string) || 'refund',
      productName: (payload.productName as string) || '',
      quantity: (payload.quantity as number) || 1,
      reason: (payload.reason as string) || 'quality',
      description: (payload.description as string) || '',
      images: (payload.images as string[]) || [],
      phone: (payload.phone as string) || ''
    })
  })
})

onUnmounted(() => {
  if (networkUnsubscribe) networkUnsubscribe()
})

onShow(() => {
  pendingSyncs.value = getSyncQueueItems().filter(item => item.entity === 'aftersale')
})

function handleNetworkChange(status: { online: boolean; weak: boolean }) {
  if (status.online) {
    // 网络恢复时触发自动同步
    triggerSync()
    pendingSyncs.value = getSyncQueueItems().filter(item => item.entity === 'aftersale')
  }
}

function loadDraft() {
  getOfflineData('aftersale_draft').then((draft) => {
    if (draft) {
      formData.value = draft as AfterSaleForm
      isEditing.value = true
    }
  })
}

async function saveDraft() {
  saving.value = true
  try {
    await saveOfflineData('aftersale_draft', formData.value)
    uni.showToast({ title: '草稿已保存', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: '保存失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}

async function submitForm() {
  if (!formData.value.productName || !formData.value.description) {
    uni.showToast({ title: '请填写必要信息', icon: 'none' })
    return
  }

  submitting.value = true
  try {
    if (isOnline.value && !isWeakNetwork.value) {
      // 在线状态：直接提交
      await submitToServer()
    } else {
      // 离线或弱网：保存到本地队列
      await saveToQueue()
    }
  } catch (e) {
    // 提交失败时自动降级到离线保存
    await saveToQueue()
  } finally {
    submitting.value = false
  }
}

async function submitToServer() {
  await createAfterSale({
    orderNo: formData.value.orderNo,
    type: formData.value.type,
    productName: formData.value.productName,
    quantity: formData.value.quantity,
    reason: formData.value.reason,
    description: formData.value.description,
    images: formData.value.images,
    phone: formData.value.phone
  })
  await saveOfflineData('aftersale_draft', null)
  uni.showToast({ title: '提交成功', icon: 'success' })
  setTimeout(() => uni.navigateBack(), 1500)
}

async function saveToQueue() {
  enqueueSync({
    type: 'create',
    entity: 'aftersale',
    payload: { ...formData.value } as Record<string, unknown>
  })
  // 清除草稿
  await saveOfflineData('aftersale_draft', null)
  uni.showToast({ title: '已保存到本地，联网后自动同步', icon: 'success' })
  pendingSyncs.value = getSyncQueueItems().filter(item => item.entity === 'aftersale')
}

function chooseImage() {
  // #ifdef H5
  uni.chooseImage({
    count: 5 - formData.value.images.length,
    success: (res) => {
      formData.value.images.push(...res.tempFilePaths)
      // 离线模式下将图片转为base64缓存
      res.tempFilePaths.forEach(path => {
        saveOfflineData(`aftersale_img_${Date.now()}_${path.split('/').pop()}`, path)
      })
    }
  })
  // #endif
  // #ifndef H5
  uni.chooseImage({
    count: 5 - formData.value.images.length,
    success: (res) => {
      formData.value.images.push(...res.tempFilePaths)
    }
  })
  // #endif
}

function removeImage(index: number) {
  formData.value.images.splice(index, 1)
}

function previewImage(index: number) {
  uni.previewImage({
    urls: formData.value.images,
    current: index
  })
}

function formatTime(timestamp: number): string {
  const date = new Date(timestamp)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hour}:${minute}`
}

function getSyncStatusClass(item: { retryCount: number }): string {
  if (item.retryCount >= 3) return 'failed'
  if (item.retryCount > 0) return 'retrying'
  return 'pending'
}

function getSyncStatusText(item: { retryCount: number }): string {
  if (item.retryCount >= 3) return '同步失败'
  if (item.retryCount > 0) return '重试中'
  return '待同步'
}
</script>

<style lang="scss" scoped>
.offline-aftersale-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 140rpx;
}

.network-banner {
  display: flex;
  align-items: center;
  padding: 20rpx 30rpx;
  font-size: 24rpx;

  &.online {
    background: #f0f9ff;
    color: #1890ff;
  }

  &.offline {
    background: #fff7e6;
    color: #fa8c16;
  }

  .banner-icon {
    margin-right: 12rpx;
    font-size: 28rpx;
  }
}

.form-container {
  height: calc(100vh - 100rpx - 140rpx);
}

.form-section {
  background: #fff;
  margin: 20rpx;
  border-radius: 16rpx;
  padding: 30rpx;

  .section-title {
    font-size: 30rpx;
    font-weight: bold;
    color: #333;
    margin-bottom: 24rpx;
    display: block;
  }
}

.form-item {
  display: flex;
  align-items: center;
  padding: 16rpx 0;

  .label {
    width: 160rpx;
    font-size: 28rpx;
    color: #666;
  }

  .input {
    flex: 1;
    font-size: 28rpx;
    color: #333;
  }
}

.radio-group {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;

  .radio-item {
    display: flex;
    align-items: center;
    padding: 16rpx 24rpx;
    border: 2rpx solid #eee;
    border-radius: 12rpx;
    background: #fafafa;

    &.selected {
      border-color: #ff4d4f;
      background: #fff1f0;
    }

    .radio-icon {
      margin-right: 8rpx;
      font-size: 24rpx;
    }

    .radio-label {
      font-size: 26rpx;
      color: #333;
    }
  }
}

.textarea {
  width: 100%;
  height: 200rpx;
  padding: 20rpx;
  border: 2rpx solid #eee;
  border-radius: 12rpx;
  font-size: 28rpx;
  background: #fafafa;
  box-sizing: border-box;
}

.image-upload {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;

  .uploaded-img {
    position: relative;
    width: 200rpx;
    height: 200rpx;

    .img-preview {
      width: 100%;
      height: 100%;
      border-radius: 12rpx;
    }

    .img-delete {
      position: absolute;
      top: -10rpx;
      right: -10rpx;
      width: 40rpx;
      height: 40rpx;
      background: rgba(0, 0, 0, 0.6);
      color: #fff;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24rpx;
    }
  }

  .upload-btn {
    width: 200rpx;
    height: 200rpx;
    border: 2rpx dashed #ddd;
    border-radius: 12rpx;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;

    .upload-icon {
      font-size: 48rpx;
      color: #999;
    }

    .upload-text {
      font-size: 24rpx;
      color: #999;
      margin-top: 8rpx;
    }
  }
}

.sync-section {
  border: 2rpx solid #e6f7ff;
  background: #f0f9ff;

  .sync-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20rpx 0;
    border-bottom: 2rpx solid #e6f7ff;

    &:last-child {
      border-bottom: none;
    }

    .sync-info {
      flex: 1;

      .sync-title {
        font-size: 28rpx;
        color: #333;
        display: block;
      }

      .sync-time {
        font-size: 22rpx;
        color: #999;
        margin-top: 6rpx;
        display: block;
      }

      .sync-retry {
        font-size: 22rpx;
        color: #fa8c16;
        margin-top: 4rpx;
        display: block;
      }
    }

    .status-tag {
      padding: 6rpx 16rpx;
      border-radius: 8rpx;
      font-size: 22rpx;

      &.pending {
        background: #e6f7ff;
        color: #1890ff;
      }

      &.retrying {
        background: #fff7e6;
        color: #fa8c16;
      }

      &.failed {
        background: #fff1f0;
        color: #ff4d4f;
      }
    }
  }
}

.action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  padding: 20rpx 30rpx;
  background: #fff;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.05);
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));

  .btn {
    flex: 1;
    height: 80rpx;
    line-height: 80rpx;
    font-size: 30rpx;
    border-radius: 40rpx;
    margin: 0 10rpx;

    &.btn-draft {
      background: #fff;
      color: #666;
      border: 2rpx solid #ddd;
    }

    &.btn-primary {
      background: #ff4d4f;
      color: #fff;
      border: none;

      &:disabled {
        opacity: 0.5;
      }
    }
  }
}
</style>
