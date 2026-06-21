<template>
  <view class="merchant-apply-page">
    <view class="step-bar">
      <view class="step" v-for="(s, i) in steps" :key="i" :class="{ active: currentStep >= i, completed: currentStep > i }">
        <view class="step-icon">{{ currentStep > i ? '✓' : i + 1 }}</view>
        <text class="step-text">{{ s }}</text>
      </view>
    </view>
    
    <view class="form-content">
      <view class="form-section" v-if="currentStep === 0">
        <text class="section-title">基本信息</text>
        
        <view class="form-item">
          <text class="label">店铺名称</text>
          <input type="text" placeholder="请输入店铺名称" v-model="form.shopName" class="input"></input>
        </view>
        
        <view class="form-item">
          <text class="label">经营类目</text>
          <picker mode="selector" :range="categories" @change="categoryChange">
            <view class="input" :class="{ placeholder: !form.category }">
              {{ form.category || '请选择经营类目' }}
            </view>
          </picker>
        </view>
        
        <view class="form-item textarea-item">
          <text class="label">店铺简介</text>
          <textarea placeholder="请简要介绍您的店铺" v-model="form.description" class="textarea" maxlength="200"></textarea>
        </view>
      </view>
      
      <view class="form-section" v-if="currentStep === 1">
        <text class="section-title">资质信息</text>
        
        <view class="form-item">
          <text class="label">营业执照</text>
          <view class="upload-area" @click="uploadFile('license')">
            <image :src="form.license" mode="aspectFill" class="upload-img" v-if="form.license"></image>
            <view class="upload-placeholder" v-else>
              <text class="icon">📄</text>
              <text class="text">上传营业执照</text>
            </view>
          </view>
        </view>
        
        <view class="form-item">
          <text class="label">法人身份证</text>
          <view class="upload-area" @click="uploadFile('idCard')">
            <image :src="form.idCard" mode="aspectFill" class="upload-img" v-if="form.idCard"></image>
            <view class="upload-placeholder" v-else>
              <text class="icon">🪪</text>
              <text class="text">上传身份证正面</text>
            </view>
          </view>
        </view>
        
        <view class="form-item">
          <text class="label">联系人姓名</text>
          <input type="text" placeholder="请输入联系人姓名" v-model="form.contactName" class="input"></input>
        </view>
        
        <view class="form-item">
          <text class="label">联系电话</text>
          <input type="number" placeholder="请输入联系电话" v-model="form.contactPhone" class="input" maxlength="11"></input>
        </view>
      </view>
      
      <view class="form-section" v-if="currentStep === 2">
        <text class="section-title">结算信息</text>
        
        <view class="form-item">
          <text class="label">银行卡号</text>
          <input type="number" placeholder="请输入银行卡号" v-model="form.bankCard" class="input"></input>
        </view>
        
        <view class="form-item">
          <text class="label">开户银行</text>
          <input type="text" placeholder="请输入开户银行" v-model="form.bankName" class="input"></input>
        </view>
        
        <view class="form-item">
          <text class="label">开户人姓名</text>
          <input type="text" placeholder="请输入开户人姓名" v-model="form.bankHolder" class="input"></input>
        </view>
        
        <view class="agreement-item">
          <text class="check-icon" :class="{ checked: agreed }" @click="agreed = !agreed">{{ agreed ? '☑' : '☐' }}</text>
          <text class="text">我已阅读并同意</text>
          <text class="link-text">《商家入驻协议》</text>
        </view>
      </view>
    </view>
    
    <view class="action-bar">
      <button class="btn btn-prev" v-if="currentStep > 0" @click="prevStep">上一步</button>
      <button class="btn btn-next" @click="nextStep">{{ currentStep === 2 ? '提交申请' : '下一步' }}</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { applyMerchant, uploadFile } from '@/api/merchant'

const steps = ['基本信息', '资质信息', '结算信息']
const currentStep = ref(0)
const agreed = ref(false)

const categories = ['服装定制', '面料辅料', '配饰配饰', '鞋帽箱包', '其他']

const form = ref({
  shopName: '',
  category: '',
  description: '',
  license: '',
  idCard: '',
  contactName: '',
  contactPhone: '',
  bankCard: '',
  bankName: '',
  bankHolder: ''
})

function categoryChange(e) {
  form.value.category = categories[e.detail.value]
}

function uploadFile(type) {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: async (res) => {
      try {
        const result = await uploadFile(res.tempFilePaths[0])
        form.value[type] = result.data.url
        uni.showToast({ title: '上传成功', icon: 'success' })
      } catch (e) {
        uni.showToast({ title: e.message || '上传失败', icon: 'none' })
      }
    }
  })
}

function prevStep() {
  if (currentStep.value > 0) {
    currentStep.value--
  }
}

function nextStep() {
  if (currentStep.value === 0) {
    if (!form.value.shopName) {
      uni.showToast({ title: '请输入店铺名称', icon: 'none' })
      return
    }
    if (!form.value.category) {
      uni.showToast({ title: '请选择经营类目', icon: 'none' })
      return
    }
  } else if (currentStep.value === 1) {
    if (!form.value.contactName) {
      uni.showToast({ title: '请输入联系人姓名', icon: 'none' })
      return
    }
    if (!form.value.contactPhone || form.value.contactPhone.length !== 11) {
      uni.showToast({ title: '请输入正确的联系电话', icon: 'none' })
      return
    }
  } else if (currentStep.value === 2) {
    if (!form.value.bankCard) {
      uni.showToast({ title: '请输入银行卡号', icon: 'none' })
      return
    }
    if (!agreed.value) {
      uni.showToast({ title: '请先阅读并同意入驻协议', icon: 'none' })
      return
    }
    
    submitApplication()
    return
  }
  
  currentStep.value++
}

async function submitApplication() {
  try {
    await applyMerchant(form.value)
    uni.showModal({
      title: '提交成功',
      content: '您的入驻申请已提交，我们将在1-3个工作日内审核，请耐心等待。',
      showCancel: false,
      success: () => {
        uni.navigateBack()
      }
    })
  } catch (e) {
    uni.showToast({ title: e.message || '提交失败', icon: 'none' })
  }
}
</script>

<style lang="scss" scoped>
.merchant-apply-page {
  min-height: 100vh;
  background: #f8f8f8;
  padding-bottom: 140rpx;
}

.step-bar {
  display: flex;
  justify-content: space-around;
  padding: 40rpx 30rpx;
  background: #fff;
  margin-bottom: 20rpx;
  
  .step {
    display: flex;
    flex-direction: column;
    align-items: center;
    position: relative;
    
    .step-icon {
      width: 60rpx;
      height: 60rpx;
      border-radius: 50%;
      background: #ddd;
      color: #fff;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 28rpx;
      margin-bottom: 12rpx;
    }
    
    .step-text {
      font-size: 24rpx;
      color: #999;
    }
    
    &.active {
      .step-icon {
        background: #FF4D4F;
      }
      .step-text {
        color: #FF4D4F;
      }
    }
    
    &.completed {
      .step-icon {
        background: #52C41A;
      }
    }
    
    &:not(:last-child)::after {
      content: '';
      position: absolute;
      top: 30rpx;
      left: 100%;
      width: 100%;
      height: 2rpx;
      background: #eee;
      transform: translateX(-50%);
    }
  }
}

.form-content {
  padding: 0 30rpx;
}

.form-section {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  
  .section-title {
    font-size: 32rpx;
    font-weight: bold;
    margin-bottom: 30rpx;
    display: block;
  }
  
  .form-item {
    display: flex;
    align-items: center;
    min-height: 100rpx;
    border-bottom: 2rpx solid #f5f5f5;
    padding: 20rpx 0;
    
    .label {
      width: 180rpx;
      font-size: 28rpx;
      color: #333;
      flex-shrink: 0;
    }
    
    .input {
      flex: 1;
      font-size: 28rpx;
      
      &.placeholder {
        color: #999;
      }
    }
    
    &.textarea-item {
      align-items: flex-start;
      
      .textarea {
        flex: 1;
        font-size: 28rpx;
        min-height: 120rpx;
      }
    }
    
    .upload-area {
      flex: 1;
      height: 200rpx;
      border: 2rpx dashed #ddd;
      border-radius: 12rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      
      .upload-img {
        width: 100%;
        height: 100%;
        border-radius: 12rpx;
      }
      
      .upload-placeholder {
        display: flex;
        flex-direction: column;
        align-items: center;
        
        .icon {
          font-size: 60rpx;
          margin-bottom: 12rpx;
        }
        
        .text {
          font-size: 24rpx;
          color: #999;
        }
      }
    }
  }
  
  .agreement-item {
    display: flex;
    align-items: center;
    justify-content: center;
    margin-top: 40rpx;
    
    .check-icon {
      font-size: 36rpx;
      margin-right: 10rpx;
      color: #ddd;
      
      &.checked {
        color: #FF4D4F;
      }
    }
    
    .text {
      font-size: 24rpx;
      color: #999;
    }
    
    .link-text {
      font-size: 24rpx;
      color: #FF4D4F;
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
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  
  .btn {
    flex: 1;
    height: 88rpx;
    line-height: 88rpx;
    font-size: 32rpx;
    border: none;
    border-radius: 44rpx;
    margin: 0 10rpx;
    
    &.btn-prev {
      background: #f5f5f5;
      color: #666;
    }
    
    &.btn-next {
      background: #FF4D4F;
      color: #fff;
    }
  }
}
</style>
