<template>
  <view class="aftersale-form">
    <form @submit="handleSubmit">
      <!-- 售后类型 -->
      <view class="form-section">
        <view class="section-title">售后类型</view>
        <view class="type-options">
          <view
            v-for="item in typeOptions"
            :key="item.value"
            class="type-option"
            :class="{ active: formData.type === item.value }"
            @tap="formData.type = item.value"
          >
            <text class="type-icon">{{ item.icon }}</text>
            <text class="type-label">{{ item.label }}</text>
          </view>
        </view>
      </view>

      <!-- 订单信息 -->
      <view class="form-section">
        <view class="section-title">订单信息</view>
        <view class="info-row">
          <text class="info-label">订单编号</text>
          <text class="info-value">{{ orderInfo.orderNo }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">商品名称</text>
          <text class="info-value text-ellipsis">{{ orderInfo.productName }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">购买数量</text>
          <text class="info-value">{{ orderInfo.quantity }}</text>
        </view>
      </view>

      <!-- 售后原因 -->
      <view class="form-section">
        <view class="section-title">售后原因</view>
        <view class="reason-options">
          <view
            v-for="item in reasonOptions"
            :key="item.value"
            class="reason-option"
            :class="{ active: formData.reason === item.value }"
            @tap="formData.reason = item.value"
          >
            {{ item.label }}
          </view>
        </view>
      </view>

      <!-- 退款金额 -->
      <view class="form-section" v-if="formData.type === 'refund' || formData.type === 'return'">
        <view class="section-title">退款金额</view>
        <view class="refund-amount">
          <view class="amount-row">
            <text class="amount-label">商品金额</text>
            <text class="amount-value">¥{{ productPrice.toFixed(2) }}</text>
          </view>
          <view class="amount-row">
            <text class="amount-label">退款数量</text>
            <view class="quantity-stepper">
              <view
                class="stepper-btn"
                :class="{ disabled: refundQuantity <= 1 }"
                @tap="decreaseQuantity"
              >-</view>
              <input
                class="stepper-input"
                type="number"
                v-model="refundQuantity"
                :max="orderInfo.quantity"
                :min="1"
              />
              <view
                class="stepper-btn"
                :class="{ disabled: refundQuantity >= orderInfo.quantity }"
                @tap="increaseQuantity"
              >+</view>
            </view>
          </view>
          <view class="amount-row total">
            <text class="amount-label">预计退款</text>
            <text class="amount-value refund-total">¥{{ refundAmount.toFixed(2) }}</text>
          </view>
        </view>
      </view>

      <!-- 问题描述 -->
      <view class="form-section">
        <view class="section-title">问题描述</view>
        <textarea
          class="desc-input"
          v-model="formData.description"
          placeholder="请详细描述问题，以便我们更快处理..."
          :maxlength="500"
          :auto-height="true"
        />
        <view class="char-count">{{ formData.description.length }}/500</view>
      </view>

      <!-- 图片上传 -->
      <view class="form-section">
        <view class="section-title">上传凭证（选填，最多9张）</view>
        <view class="image-uploader">
          <view
            v-for="(img, index) in formData.images"
            :key="index"
            class="image-item"
          >
            <image :src="img" mode="aspectFill" class="uploaded-image" />
            <view class="image-delete" @tap="removeImage(index)">
              <text>×</text>
            </view>
          </view>
          <view
            v-if="formData.images.length < 9"
            class="image-add"
            @tap="chooseImage"
          >
            <text class="add-icon">+</text>
            <text class="add-text">{{ formData.images.length }}/9</text>
          </view>
        </view>
        <view class="upload-tip" v-if="compressing">
          <text>图片压缩中...</text>
        </view>
      </view>

      <!-- 联系方式 -->
      <view class="form-section">
        <view class="section-title">联系方式</view>
        <input
          class="contact-input"
          v-model="formData.phone"
          type="number"
          placeholder="请输入联系电话"
          :maxlength="11"
        />
      </view>

      <!-- 提交按钮 -->
      <view class="submit-section">
        <button
          class="submit-btn"
          :class="{ disabled: !isValid }"
          :disabled="!isValid || submitting"
          form-type="submit"
          :loading="submitting"
        >
          {{ submitting ? '提交中...' : '提交申请' }}
        </button>
      </view>
    </form>
  </view>
</template>

<script lang="ts">
import { defineComponent, PropType, ref, reactive, computed, watch } from 'vue'

interface OrderInfo {
  orderNo: string
  productName: string
  productImage: string
  productPrice: number
  quantity: number
}

interface AfterSaleFormData {
  type: string
  reason: string
  description: string
  images: string[]
  phone: string
}

export default defineComponent({
  name: 'AfterSaleForm',
  props: {
    /** 订单信息 */
    orderInfo: {
      type: Object as PropType<OrderInfo>,
      required: true
    },
    /** 默认售后类型 */
    defaultType: {
      type: String,
      default: 'refund'
    }
  },
  emits: ['submit', 'cancel'],
  setup(props, { emit }) {
    const formData = reactive<AfterSaleFormData>({
      type: props.defaultType,
      reason: '',
      description: '',
      images: [],
      phone: ''
    })

    const refundQuantity = ref(1)
    const submitting = ref(false)
    const compressing = ref(false)

    const typeOptions = [
      { value: 'refund', label: '仅退款', icon: '💰' },
      { value: 'return', label: '退货退款', icon: '📦' },
      { value: 'exchange', label: '换货', icon: '🔄' }
    ]

    const reasonOptions = [
      { value: 'damaged', label: '商品破损' },
      { value: 'wrong_size', label: '尺码不合适' },
      { value: 'quality_issue', label: '质量问题' },
      { value: 'not_as_described', label: '与描述不符' },
      { value: 'wrong_item', label: '发错货' },
      { value: 'missing_item', label: '少件/漏发' },
      { value: 'no_longer_needed', label: '不想要了' },
      { value: 'other', label: '其他原因' }
    ]

    const productPrice = computed(() => {
      return props.orderInfo.productPrice || 0
    })

    const refundAmount = computed(() => {
      return productPrice.value * refundQuantity.value
    })

    const isValid = computed(() => {
      if (!formData.type) return false
      if (!formData.reason) return false
      if (!formData.description || formData.description.length < 5) return false
      if (!formData.phone || formData.phone.length !== 11) return false
      return true
    })

    function decreaseQuantity() {
      if (refundQuantity.value > 1) {
        refundQuantity.value--
      }
    }

    function increaseQuantity() {
      if (refundQuantity.value < props.orderInfo.quantity) {
        refundQuantity.value++
      }
    }

    async function chooseImage() {
      try {
        const remaining = 9 - formData.images.length
        const res = await uni.chooseImage({
          count: remaining,
          sizeType: ['compressed'],
          sourceType: ['album', 'camera']
        })

        compressing.value = true

        // 压缩图片
        const compressedPaths = await Promise.all(
          res.tempFilePaths.map(async (path) => {
            return await compressImage(path)
          })
        )

        formData.images.push(...compressedPaths)
        compressing.value = false
      } catch (e) {
        compressing.value = false
        console.error('选择图片失败:', e)
      }
    }

    function compressImage(filePath: string): Promise<string> {
      return new Promise((resolve, reject) => {
        // #ifdef MP-WEIXIN
        uni.compressImage({
          src: filePath,
          quality: 80,
          success: (result) => resolve(result.tempFilePath),
          fail: () => resolve(filePath) // 压缩失败返回原图
        })
        // #endif

        // #ifndef MP-WEIXIN
        resolve(filePath)
        // #endif
      })
    }

    function removeImage(index: number) {
      formData.images.splice(index, 1)
    }

    async function handleSubmit() {
      if (!isValid.value || submitting.value) return

      submitting.value = true
      try {
        emit('submit', {
          ...formData,
          quantity: refundQuantity.value,
          refundAmount: refundAmount.value
        })
      } finally {
        submitting.value = false
      }
    }

    return {
      formData,
      refundQuantity,
      submitting,
      compressing,
      typeOptions,
      reasonOptions,
      productPrice,
      refundAmount,
      isValid,
      decreaseQuantity,
      increaseQuantity,
      chooseImage,
      removeImage,
      handleSubmit
    }
  }
})
</script>

<style lang="scss" scoped>
.aftersale-form {
  background: #f8f8f8;
  min-height: 100vh;
  padding-bottom: 40rpx;

  .form-section {
    background: #fff;
    margin: 20rpx;
    padding: 28rpx;
    border-radius: 16rpx;

    .section-title {
      font-size: 28rpx;
      font-weight: 600;
      color: #333;
      margin-bottom: 20rpx;
    }
  }

  .type-options {
    display: flex;
    gap: 20rpx;

    .type-option {
      flex: 1;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 24rpx;
      border: 2rpx solid #eee;
      border-radius: 12rpx;
      transition: all 0.2s;

      &.active {
        border-color: #ff4d4f;
        background: #fff2f0;
      }

      .type-icon {
        font-size: 36rpx;
        margin-bottom: 8rpx;
      }

      .type-label {
        font-size: 24rpx;
        color: #666;
      }
    }
  }

  .info-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12rpx 0;
    border-bottom: 1rpx solid #f5f5f5;

    &:last-child {
      border-bottom: none;
    }

    .info-label {
      font-size: 26rpx;
      color: #999;
    }

    .info-value {
      font-size: 26rpx;
      color: #333;
      max-width: 400rpx;
    }
  }

  .reason-options {
    display: flex;
    flex-wrap: wrap;
    gap: 16rpx;

    .reason-option {
      padding: 12rpx 24rpx;
      font-size: 24rpx;
      color: #666;
      background: #f5f5f5;
      border-radius: 8rpx;
      border: 1rpx solid transparent;
      transition: all 0.2s;

      &.active {
        color: #ff4d4f;
        background: #fff2f0;
        border-color: #ff4d4f;
      }
    }
  }

  .refund-amount {
    .amount-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12rpx 0;

      .amount-label {
        font-size: 26rpx;
        color: #666;
      }

      .amount-value {
        font-size: 26rpx;
        color: #333;
      }

      &.total {
        border-top: 1rpx solid #f5f5f5;
        margin-top: 8rpx;
        padding-top: 20rpx;

        .refund-total {
          font-size: 32rpx;
          font-weight: 600;
          color: #ff4d4f;
        }
      }
    }

    .quantity-stepper {
      display: flex;
      align-items: center;
      gap: 0;

      .stepper-btn {
        width: 56rpx;
        height: 56rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        background: #f5f5f5;
        font-size: 32rpx;
        color: #333;
        border-radius: 4rpx;

        &.disabled {
          color: #ccc;
        }
      }

      .stepper-input {
        width: 80rpx;
        height: 56rpx;
        text-align: center;
        font-size: 28rpx;
        background: #fff;
        border: 1rpx solid #eee;
        border-left: none;
        border-right: none;
      }
    }
  }

  .desc-input {
    width: 100%;
    min-height: 160rpx;
    font-size: 26rpx;
    color: #333;
    padding: 16rpx;
    background: #f8f8f8;
    border-radius: 8rpx;
    box-sizing: border-box;
  }

  .char-count {
    text-align: right;
    font-size: 22rpx;
    color: #ccc;
    margin-top: 8rpx;
  }

  .image-uploader {
    display: flex;
    flex-wrap: wrap;
    gap: 16rpx;

    .image-item {
      position: relative;
      width: 156rpx;
      height: 156rpx;
      border-radius: 8rpx;
      overflow: hidden;

      .uploaded-image {
        width: 100%;
        height: 100%;
      }

      .image-delete {
        position: absolute;
        top: 0;
        right: 0;
        width: 36rpx;
        height: 36rpx;
        background: rgba(0, 0, 0, 0.5);
        border-radius: 0 8rpx 0 8rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #fff;
        font-size: 28rpx;
      }
    }

    .image-add {
      width: 156rpx;
      height: 156rpx;
      border: 2rpx dashed #ddd;
      border-radius: 8rpx;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      background: #f8f8f8;

      .add-icon {
        font-size: 48rpx;
        color: #ccc;
      }

      .add-text {
        font-size: 20rpx;
        color: #ccc;
        margin-top: 4rpx;
      }
    }
  }

  .upload-tip {
    margin-top: 12rpx;
    font-size: 22rpx;
    color: #1890ff;
    text-align: center;
  }

  .contact-input {
    width: 100%;
    height: 80rpx;
    font-size: 28rpx;
    padding: 0 16rpx;
    background: #f8f8f8;
    border-radius: 8rpx;
    box-sizing: border-box;
  }

  .submit-section {
    padding: 40rpx 20rpx;

    .submit-btn {
      width: 100%;
      height: 88rpx;
      line-height: 88rpx;
      background: #ff4d4f;
      color: #fff;
      font-size: 30rpx;
      font-weight: 500;
      border-radius: 44rpx;
      text-align: center;
      border: none;

      &.disabled {
        background: #ffb3b3;
      }
    }
  }
}
</style>