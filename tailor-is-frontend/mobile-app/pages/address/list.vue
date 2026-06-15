<template>
  <view class="address-list-page">
    <scroll-view scroll-y class="address-list" v-if="addressList.length > 0">
      <view class="address-card" v-for="addr in addressList" :key="addr.id" :class="{ default: addr.isDefault }">
        <view class="address-info" @click="selectAddress(addr)">
          <view class="name-phone">
            <text class="name">{{ addr.name }}</text>
            <text class="phone">{{ addr.phone }}</text>
            <text class="default-tag" v-if="addr.isDefault">默认</text>
          </view>
          <text class="address">{{ addr.fullAddress }} {{ addr.detailAddress }}</text>
        </view>
        <view class="address-actions">
          <view class="action-btn" @click="goEdit(addr)">编辑</view>
          <view class="action-btn delete" @click="deleteAddress(addr)">删除</view>
          <view class="action-btn" @click="setDefault(addr)" v-if="!addr.isDefault">设为默认</view>
        </view>
      </view>
    </scroll-view>
    
    <view class="empty" v-else>
      <text class="icon">📍</text>
      <text class="text">暂无收货地址</text>
    </view>
    
    <view class="add-btn-wrap">
      <button class="add-btn" @click="goEdit()">+ 新增地址</button>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAddresses, deleteAddress as deleteAddressApi, setDefaultAddress } from '@/api/user'

const addressList = ref([])
const isSelectMode = ref(false)

onMounted(() => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  isSelectMode.value = currentPage.options.select === '1'
  loadAddresses()
})

async function loadAddresses() {
  try {
    const res = await getAddresses()
    addressList.value = res.data || []
  } catch (e) {
    console.error('加载地址失败', e)
  }
}

function selectAddress(addr) {
  if (isSelectMode.value) {
    const pages = getCurrentPages()
    const prevPage = pages[pages.length - 2]
    if (prevPage) {
      prevPage.$vm.selectedAddress = addr
    }
    uni.navigateBack()
  }
}

function goEdit(addr) {
  uni.navigateTo({ url: `/pages/address/edit${addr ? `?id=${addr.id}` : ''}` })
}

function deleteAddress(addr) {
  uni.showModal({
    title: '确认删除',
    content: '确定要删除该地址吗?',
    success: async (res) => {
      if (res.confirm) {
        try {
          await deleteAddressApi(addr.id)
          uni.showToast({ title: '删除成功', icon: 'success' })
          loadAddresses()
        } catch (e) {
          uni.showToast({ title: e.message || '删除失败', icon: 'none' })
        }
      }
    }
  })
}

async function setDefault(addr) {
  try {
    await setDefaultAddress(addr.id)
    uni.showToast({ title: '设置成功', icon: 'success' })
    loadAddresses()
  } catch (e) {
    uni.showToast({ title: e.message || '设置失败', icon: 'none' })
  }
}
</script>

<style lang="scss" scoped>
.address-list-page {
  min-height: 100vh;
  background: #f8f8f8;
  padding-bottom: 140rpx;
}

.address-list {
  padding: 20rpx;
  
  .address-card {
    background: #fff;
    border-radius: 16rpx;
    padding: 30rpx;
    margin-bottom: 20rpx;
    
    &.default {
      border: 2rpx solid #FF4D4F;
    }
    
    .address-info {
      margin-bottom: 20rpx;
      
      .name-phone {
        display: flex;
        align-items: center;
        margin-bottom: 12rpx;
        
        .name {
          font-size: 32rpx;
          font-weight: bold;
          margin-right: 20rpx;
        }
        
        .phone {
          font-size: 28rpx;
          color: #666;
          margin-right: 16rpx;
        }
        
        .default-tag {
          background: #FF4D4F;
          color: #fff;
          font-size: 22rpx;
          padding: 4rpx 12rpx;
          border-radius: 4rpx;
        }
      }
      
      .address {
        font-size: 26rpx;
        color: #666;
      }
    }
    
    .address-actions {
      display: flex;
      justify-content: flex-end;
      border-top: 2rpx solid #f5f5f5;
      padding-top: 20rpx;
      
      .action-btn {
        font-size: 26rpx;
        color: #666;
        padding: 10rpx 20rpx;
        margin-left: 20rpx;
        
        &.delete {
          color: #FF4D4F;
        }
      }
    }
  }
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 200rpx;
  
  .icon {
    font-size: 120rpx;
    margin-bottom: 20rpx;
  }
  
  .text {
    font-size: 28rpx;
    color: #999;
  }
}

.add-btn-wrap {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 20rpx 30rpx;
  background: #fff;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  
  .add-btn {
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
