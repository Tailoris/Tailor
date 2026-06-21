<template>
  <view class="user-page">
    <view class="user-header">
      <view class="user-info" v-if="isLogin" @click="goProfile">
        <image :src="userInfo.avatar || 'https://via.placeholder.com/150x150'" mode="aspectFill" class="avatar"></image>
        <view class="info">
          <text class="nickname">{{ userInfo.nickname || 'Tailor IS用户' }}</text>
          <view class="level-badge" v-if="userInfo.level">
            <text>{{ userInfo.level.name || '普通会员' }}</text>
          </view>
        </view>
      </view>
      <view class="user-info" v-else @click="goLogin">
        <view class="avatar-placeholder">👤</view>
        <view class="info">
          <text class="nickname">点击登录/注册</text>
          <text class="desc">登录后享受更多权益</text>
        </view>
      </view>
      <view class="setting-btn" @click="goSettings">⚙️</view>
    </view>
    
    <view class="order-section">
      <view class="section-header" @click="goOrderList('')">
        <text class="title">我的订单</text>
        <text class="more">全部订单 ></text>
      </view>
      <view class="order-grid">
        <view class="order-item" @click="goOrderList(0)">
          <view class="icon-wrap">
            <text class="icon">💰</text>
            <text class="badge" v-if="orderCount.unpaid > 0">{{ orderCount.unpaid }}</text>
          </view>
          <text>待付款</text>
        </view>
        <view class="order-item" @click="goOrderList(1)">
          <view class="icon-wrap">
            <text class="icon">📦</text>
            <text class="badge" v-if="orderCount.unshipped > 0">{{ orderCount.unshipped }}</text>
          </view>
          <text>待发货</text>
        </view>
        <view class="order-item" @click="goOrderList(2)">
          <view class="icon-wrap">
            <text class="icon">🚚</text>
            <text class="badge" v-if="orderCount.shipped > 0">{{ orderCount.shipped }}</text>
          </view>
          <text>待收货</text>
        </view>
        <view class="order-item" @click="goOrderList(3)">
          <view class="icon-wrap">
            <text class="icon">✅</text>
            <text class="badge" v-if="orderCount.completed > 0">{{ orderCount.completed }}</text>
          </view>
          <text>已完成</text>
        </view>
      </view>
    </view>
    
    <view class="tools-section">
      <view class="tools-grid">
        <view class="tool-item" @click="goAddresses">
          <text class="icon">📍</text>
          <text>地址管理</text>
        </view>
        <view class="tool-item" @click="goFavorites">
          <text class="icon">⭐</text>
          <text>我的收藏</text>
        </view>
        <view class="tool-item" @click="goCoupons">
          <text class="icon">🎫</text>
          <text>优惠券</text>
        </view>
        <view class="tool-item" @click="goPoints">
          <text class="icon">💎</text>
          <text>我的积分</text>
        </view>
        <view class="tool-item" @click="goCommunity">
          <text class="icon">💬</text>
          <text>社区动态</text>
        </view>
        <view class="tool-item" @click="goMerchantApply">
          <text class="icon">🏪</text>
          <text>商家入驻</text>
        </view>
        <view class="tool-item" @click="goCustomerService">
          <text class="icon">📞</text>
          <text>在线客服</text>
        </view>
        <view class="tool-item" @click="goHelp">
          <text class="icon">❓</text>
          <text>帮助中心</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getUserInfo } from '@/api/auth'
import { getOrders } from '@/api/order'

const isLogin = ref(false)
const userInfo = ref({})
const orderCount = ref({
  unpaid: 0,
  unshipped: 0,
  shipped: 0,
  completed: 0
})

onMounted(() => {
  const token = uni.getStorageSync('token')
  if (token) {
    isLogin.value = true
    loadUserInfo()
    loadOrderCount()
  }
})

async function loadUserInfo() {
  try {
    const res = await getUserInfo()
    userInfo.value = res.data || {}
  } catch (e) {
    console.error('获取用户信息失败', e)
  }
}

async function loadOrderCount() {
  try {
    const [unpaidRes, unshippedRes, shippedRes, completedRes] = await Promise.all([
      getOrders({ status: 0, page: 1, limit: 1 }),
      getOrders({ status: 1, page: 1, limit: 1 }),
      getOrders({ status: 2, page: 1, limit: 1 }),
      getOrders({ status: 3, page: 1, limit: 1 })
    ])
    orderCount.value = {
      unpaid: unpaidRes.data?.total || 0,
      unshipped: unshippedRes.data?.total || 0,
      shipped: shippedRes.data?.total || 0,
      completed: completedRes.data?.total || 0
    }
  } catch (e) {
    console.error('获取订单统计失败', e)
  }
}

function goLogin() {
  uni.navigateTo({ url: '/pages/login/login' })
}

function goProfile() {
  uni.showToast({ title: '个人资料', icon: 'none' })
}

function goSettings() {
  uni.showToast({ title: '设置', icon: 'none' })
}

function goOrderList(status) {
  uni.navigateTo({ url: `/pages/order/list?status=${status}` })
}

function goAddresses() {
  uni.navigateTo({ url: '/pages/address/list' })
}

function goFavorites() {
  uni.showToast({ title: '我的收藏', icon: 'none' })
}

function goCoupons() {
  uni.showToast({ title: '优惠券', icon: 'none' })
}

function goPoints() {
  uni.showToast({ title: '我的积分', icon: 'none' })
}

function goCommunity() {
  uni.navigateTo({ url: '/pages/community/list' })
}

function goMerchantApply() {
  uni.navigateTo({ url: '/pages/merchant/apply' })
}

function goCustomerService() {
  uni.showToast({ title: '客服功能', icon: 'none' })
}

function goHelp() {
  uni.showToast({ title: '帮助中心', icon: 'none' })
}
</script>

<style lang="scss" scoped>
.user-page {
  min-height: 100vh;
  background: #f8f8f8;
}

.user-header {
  position: relative;
  background: linear-gradient(135deg, #FF4D4F 0%, #FF7875 100%);
  padding: 60rpx 30rpx 40rpx;
  display: flex;
  align-items: center;
  
  .user-info {
    display: flex;
    align-items: center;
    flex: 1;
    
    .avatar, .avatar-placeholder {
      width: 120rpx;
      height: 120rpx;
      border-radius: 50%;
      border: 4rpx solid rgba(255, 255, 255, 0.5);
      margin-right: 24rpx;
    }
    
    .avatar-placeholder {
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(255, 255, 255, 0.3);
      font-size: 60rpx;
    }
    
    .info {
      .nickname {
        color: #fff;
        font-size: 36rpx;
        font-weight: bold;
        display: block;
        margin-bottom: 12rpx;
      }
      
      .desc {
        color: rgba(255, 255, 255, 0.8);
        font-size: 24rpx;
      }
      
      .level-badge {
        display: inline-block;
        background: rgba(255, 255, 255, 0.3);
        color: #fff;
        font-size: 22rpx;
        padding: 4rpx 16rpx;
        border-radius: 20rpx;
        margin-top: 8rpx;
      }
    }
  }
  
  .setting-btn {
    font-size: 44rpx;
  }
}

.order-section {
  background: #fff;
  margin: 20rpx;
  border-radius: 20rpx;
  padding: 30rpx;
  
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 30rpx;
    
    .title {
      font-size: 32rpx;
      font-weight: bold;
      color: #333;
    }
    
    .more {
      font-size: 24rpx;
      color: #999;
    }
  }
  
  .order-grid {
    display: flex;
    justify-content: space-around;
    
    .order-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      
      .icon-wrap {
        position: relative;
        font-size: 56rpx;
        margin-bottom: 16rpx;
        
        .badge {
          position: absolute;
          top: -10rpx;
          right: -20rpx;
          background: #FF4D4F;
          color: #fff;
          font-size: 20rpx;
          min-width: 32rpx;
          height: 32rpx;
          border-radius: 16rpx;
          display: flex;
          align-items: center;
          justify-content: center;
          padding: 0 8rpx;
        }
      }
      
      text {
        font-size: 24rpx;
        color: #666;
      }
    }
  }
}

.tools-section {
  background: #fff;
  margin: 20rpx;
  border-radius: 20rpx;
  padding: 30rpx;
  
  .tools-grid {
    display: flex;
    flex-wrap: wrap;
    
    .tool-item {
      width: 25%;
      display: flex;
      flex-direction: column;
      align-items: center;
      margin-bottom: 30rpx;
      
      .icon {
        font-size: 48rpx;
        margin-bottom: 12rpx;
      }
      
      text {
        font-size: 24rpx;
        color: #666;
      }
    }
  }
}
</style>
