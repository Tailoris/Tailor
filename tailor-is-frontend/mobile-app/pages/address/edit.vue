<template>
  <view class="address-edit-page">
    <view class="form-section">
      <view class="form-item">
        <text class="label">收货人</text>
        <input type="text" placeholder="请输入收货人姓名" v-model="form.name" class="input" maxlength="20"></input>
      </view>
      
      <view class="form-item">
        <text class="label">手机号码</text>
        <input type="number" placeholder="请输入手机号码" v-model="form.phone" class="input" maxlength="11"></input>
      </view>
      
      <view class="form-item" @click="showRegionPicker">
        <text class="label">所在地区</text>
        <view class="input right">
          <text :class="{ placeholder: !regionText }">{{ regionText || '请选择省/市/区' }}</text>
          <text class="arrow">></text>
        </view>
      </view>
      
      <view class="form-item textarea-item">
        <text class="label">详细地址</text>
        <textarea placeholder="街道、门牌号等详细地址" v-model="form.detailAddress" class="textarea" maxlength="200" auto-height></textarea>
      </view>
      
      <view class="form-item">
        <text class="label">设为默认地址</text>
        <switch :checked="form.isDefault" @change="form.isDefault = $event.detail.value" color="#FF4D4F" class="switch"></switch>
      </view>
    </view>
    
    <view class="save-btn-wrap">
      <button class="save-btn" @click="saveAddress" :loading="loading">保存地址</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { addAddress, updateAddress, getAddressDetail } from '@/api/user'

const loading = ref(false)
const addressId = ref(null)
const form = ref({
  name: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  detailAddress: '',
  isDefault: false
})

const regionText = computed(() => {
  const { province, city, district } = form.value
  return province && city && district ? `${province} ${city} ${district}` : ''
})

onMounted(() => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  if (currentPage.options.id) {
    addressId.value = currentPage.options.id
    loadAddress(addressId.value)
  }
})

async function loadAddress(id) {
  try {
    const res = await getAddressDetail(id)
    const data = res.data || {}
    form.value = {
      name: data.name || '',
      phone: data.phone || '',
      province: data.province || '',
      city: data.city || '',
      district: data.district || '',
      detailAddress: data.detailAddress || '',
      isDefault: data.isDefault || false
    }
  } catch (e) {
    console.error('加载地址失败', e)
  }
}

function showRegionPicker() {
  uni.showToast({ title: '地区选择器', icon: 'none' })
}

async function saveAddress() {
  if (!form.value.name) {
    uni.showToast({ title: '请输入收货人', icon: 'none' })
    return
  }
  
  if (!form.value.phone || form.value.phone.length !== 11) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return
  }
  
  if (!form.value.province) {
    uni.showToast({ title: '请选择所在地区', icon: 'none' })
    return
  }
  
  if (!form.value.detailAddress) {
    uni.showToast({ title: '请输入详细地址', icon: 'none' })
    return
  }
  
  loading.value = true
  
  try {
    if (addressId.value) {
      await updateAddress(addressId.value, form.value)
      uni.showToast({ title: '更新成功', icon: 'success' })
    } else {
      await addAddress(form.value)
      uni.showToast({ title: '添加成功', icon: 'success' })
    }
    setTimeout(() => uni.navigateBack(), 500)
  } catch (e) {
    uni.showToast({ title: e.message || '保存失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.address-edit-page {
  min-height: 100vh;
  background: #f8f8f8;
  padding-bottom: 140rpx;
}

.form-section {
  background: #fff;
  padding: 0 30rpx;
  
  .form-item {
    display: flex;
    align-items: center;
    min-height: 100rpx;
    border-bottom: 2rpx solid #f5f5f5;
    padding: 20rpx 0;
    
    .label {
      width: 160rpx;
      font-size: 28rpx;
      color: #333;
      flex-shrink: 0;
    }
    
    .input {
      flex: 1;
      font-size: 28rpx;
      
      &.right {
        display: flex;
        justify-content: space-between;
        align-items: center;
        color: #333;
        
        .placeholder {
          color: #999;
        }
        
        .arrow {
          color: #999;
          font-size: 28rpx;
        }
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
    
    .switch {
      margin-left: auto;
    }
  }
}

.save-btn-wrap {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 20rpx 30rpx;
  background: #fff;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  
  .save-btn {
    background: #FF4D4F;
    color: #fff;
    border: none;
    border-radius: 40rpx;
    height: 88rpx;
    line-height: 88rpx;
    font-size: 32rpx;
  }
}
</style>
