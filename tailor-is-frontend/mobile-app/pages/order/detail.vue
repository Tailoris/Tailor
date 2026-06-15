<template>
  <view class="order-detail-page">
    <scroll-view scroll-y class="content" v-if="!loading">
      <view class="status-bar" :class="'status-' + orderDetail.status">
        <text class="icon">{{ statusIcon }}</text>
        <text class="status-text">{{ orderDetail.statusText }}</text>
        <text class="status-tip">{{ statusTip }}</text>
      </view>
      
      <view class="address-section" v-if="orderDetail.address">
        <view class="name-phone">
          <text class="name">{{ orderDetail.address.name }}</text>
          <text class="phone">{{ orderDetail.address.phone }}</text>
        </view>
        <text class="address">{{ orderDetail.address.fullAddress }}</text>
      </view>
      
      <view class="info-section">
        <view class="info-item">
          <text class="label">订单编号</text>
          <text class="value">{{ orderDetail.orderNo }}</text>
        </view>
        <view class="info-item">
          <text class="label">下单时间</text>
          <text class="value">{{ orderDetail.createTime }}</text>
        </view>
        <view class="info-item" v-if="orderDetail.payTime">
          <text class="label">支付时间</text>
          <text class="value">{{ orderDetail.payTime }}</text>
        </view>
      </view>
      
      <view class="goods-section">
        <text class="section-title">商品信息</text>
        <view class="goods-item" v-for="item in orderDetail.items" :key="item.id">
          <image :src="item.image || 'https://via.placeholder.com/150x150'" mode="aspectFill" class="goods-img"></image>
          <view class="goods-info">
            <text class="name text-ellipsis-2">{{ item.name }}</text>
            <text class="sku" v-if="item.skuName">{{ item.skuName }}</text>
            <view class="price-qty">
              <text class="price">¥{{ item.price }}</text>
              <text class="qty">x{{ item.quantity }}</text>
            </view>
          </view>
        </view>
      </view>
      
      <view class="logistics-section" v-if="orderDetail.logistics">
        <text class="section-title">物流信息</text>
        <view class="logistics-item" v-for="(log, index) in orderDetail.logistics" :key="index">
          <view class="logistics-dot" :class="{ active: index === 0 }"></view>
          <view class="logistics-info">
            <text class="content">{{ log.content }}</text>
            <text class="time">{{ log.time }}</text>
          </view>
        </view>
      </view>
      
      <view class="price-section">
        <view class="price-item">
          <text>商品总价</text>
          <text>¥{{ orderDetail.goodsAmount?.toFixed(2) }}</text>
        </view>
        <view class="price-item" v-if="orderDetail.couponAmount > 0">
          <text>优惠券</text>
          <text class="discount">-¥{{ orderDetail.couponAmount.toFixed(2) }}</text>
        </view>
        <view class="price-item">
          <text>运费</text>
          <text>¥{{ orderDetail.freight?.toFixed(2) }}</text>
        </view>
        <view class="price-item total">
          <text>实付款</text>
          <text class="price">¥{{ orderDetail.totalAmount?.toFixed(2) }}</text>
        </view>
      </view>
    </scroll-view>
    
    <view class="loading" v-if="loading">
      <view class="skeleton"></view>
    </view>
    
    <view class="action-bar" v-if="!loading">
      <button class="btn" v-if="orderDetail.status === 0" @click="cancelOrder">取消订单</button>
      <button class="btn btn-primary" v-if="orderDetail.status === 0" @click="payOrder">去支付</button>
      <button class="btn btn-primary" v-if="orderDetail.status === 2" @click="confirmReceive">确认收货</button>
      <button class="btn" v-if="orderDetail.status === 3" @click="deleteOrder">删除订单</button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getOrderDetail, cancelOrder as cancelOrderApi, confirmReceive as confirmReceiveApi, payOrder as payOrderApi, deleteOrder as deleteOrderApi } from '@/api/order'

const loading = ref(true)
const orderDetail = ref({})

const statusIcon = computed(() => {
  const icons = { 0: '⏳', 1: '📦', 2: '🚚', 3: '✅' }
  return icons[orderDetail.value.status] || '📄'
})

const statusTip = computed(() => {
  const tips = {
    0: '请尽快完成支付',
    1: '商家正在处理您的订单',
    2: '商品正在配送中',
    3: '交易已完成'
  }
  return tips[orderDetail.value.status] || ''
})

onMounted(() => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const id = currentPage.options.id
  if (id) loadOrder(id)
})

async function loadOrder(id) {
  try {
    const res = await getOrderDetail(id)
    orderDetail.value = res.data || {}
  } catch (e) {
    uni.showToast({ title: e.message || '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

async function cancelOrder() {
  uni.showModal({
    title: '确认取消',
    content: '确定要取消该订单吗?',
    success: async (res) => {
      if (res.confirm) {
        try {
          await cancelOrderApi(orderDetail.value.id)
          uni.showToast({ title: '订单已取消', icon: 'success' })
          loadOrder(orderDetail.value.id)
        } catch (e) {
          uni.showToast({ title: e.message || '取消失败', icon: 'none' })
        }
      }
    }
  })
}

async function payOrder() {
  try {
    await payOrderApi(orderDetail.value.id, { payType: 'wechat' })
    uni.showToast({ title: '支付成功', icon: 'success' })
    loadOrder(orderDetail.value.id)
  } catch (e) {
    uni.showToast({ title: e.message || '支付失败', icon: 'none' })
  }
}

async function confirmReceive() {
  uni.showModal({
    title: '确认收货',
    content: '确认已收到商品?',
    success: async (res) => {
      if (res.confirm) {
        try {
          await confirmReceiveApi(orderDetail.value.id)
          uni.showToast({ title: '确认收货成功', icon: 'success' })
          loadOrder(orderDetail.value.id)
        } catch (e) {
          uni.showToast({ title: e.message || '操作失败', icon: 'none' })
        }
      }
    }
  })
}

async function deleteOrder() {
  uni.showModal({
    title: '确认删除',
    content: '确定要删除该订单吗?',
    success: async (res) => {
      if (res.confirm) {
        try {
          await deleteOrderApi(orderDetail.value.id)
          uni.showToast({ title: '删除成功', icon: 'success' })
          setTimeout(() => uni.navigateBack(), 500)
        } catch (e) {
          uni.showToast({ title: e.message || '删除失败', icon: 'none' })
        }
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.order-detail-page {
  min-height: 100vh;
  background: #f8f8f8;
  padding-bottom: 120rpx;
}

.content {
  height: calc(100vh - 120rpx);
}

.status-bar {
  padding: 40rpx 30rpx;
  display: flex;
  flex-direction: column;
  
  &.status-0 { background: linear-gradient(135deg, #FFA940, #FFC069); }
  &.status-1 { background: linear-gradient(135deg, #1890FF, #40A9FF); }
  &.status-2 { background: linear-gradient(135deg, #722ED1, #9254DE); }
  &.status-3 { background: linear-gradient(135deg, #52C41A, #73D13D); }
  
  .icon {
    font-size: 60rpx;
    margin-bottom: 12rpx;
  }
  
  .status-text {
    color: #fff;
    font-size: 36rpx;
    font-weight: bold;
    margin-bottom: 8rpx;
  }
  
  .status-tip {
    color: rgba(255, 255, 255, 0.9);
    font-size: 26rpx;
  }
}

.address-section {
  background: #fff;
  padding: 30rpx;
  margin-bottom: 2rpx;
  
  .name-phone {
    margin-bottom: 12rpx;
    
    .name {
      font-size: 32rpx;
      font-weight: bold;
      margin-right: 20rpx;
    }
    
    .phone {
      font-size: 28rpx;
      color: #666;
    }
  }
  
  .address {
    font-size: 26rpx;
    color: #666;
  }
}

.info-section {
  background: #fff;
  padding: 30rpx;
  margin-bottom: 2rpx;
  
  .info-item {
    display: flex;
    justify-content: space-between;
    padding: 12rpx 0;
    
    .label {
      color: #999;
      font-size: 26rpx;
    }
    
    .value {
      color: #333;
      font-size: 26rpx;
    }
  }
}

.goods-section {
  background: #fff;
  padding: 30rpx;
  margin-bottom: 2rpx;
  
  .section-title {
    font-size: 28rpx;
    font-weight: bold;
    margin-bottom: 20rpx;
    display: block;
  }
  
  .goods-item {
    display: flex;
    margin-bottom: 20rpx;
    
    .goods-img {
      width: 140rpx;
      height: 140rpx;
      border-radius: 12rpx;
      margin-right: 20rpx;
      flex-shrink: 0;
    }
    
    .goods-info {
      flex: 1;
      
      .name {
        font-size: 28rpx;
        color: #333;
        margin-bottom: 8rpx;
      }
      
      .sku {
        font-size: 24rpx;
        color: #999;
        margin-bottom: 12rpx;
      }
      
      .price-qty {
        display: flex;
        justify-content: space-between;
        
        .price {
          color: #FF4D4F;
          font-size: 28rpx;
          font-weight: bold;
        }
        
        .qty {
          color: #999;
          font-size: 26rpx;
        }
      }
    }
  }
}

.logistics-section {
  background: #fff;
  padding: 30rpx;
  margin-bottom: 2rpx;
  
  .section-title {
    font-size: 28rpx;
    font-weight: bold;
    margin-bottom: 20rpx;
    display: block;
  }
  
  .logistics-item {
    display: flex;
    padding: 16rpx 0;
    position: relative;
    
    &:not(:last-child)::after {
      content: '';
      position: absolute;
      left: 8rpx;
      top: 40rpx;
      bottom: -16rpx;
      width: 2rpx;
      background: #eee;
    }
    
    .logistics-dot {
      width: 18rpx;
      height: 18rpx;
      border-radius: 50%;
      background: #ddd;
      margin-right: 20rpx;
      margin-top: 6rpx;
      flex-shrink: 0;
      
      &.active {
        background: #52C41A;
      }
    }
    
    .logistics-info {
      flex: 1;
      
      .content {
        font-size: 26rpx;
        color: #333;
        display: block;
        margin-bottom: 6rpx;
      }
      
      .time {
        font-size: 22rpx;
        color: #999;
      }
    }
  }
}

.price-section {
  background: #fff;
  padding: 30rpx;
  
  .price-item {
    display: flex;
    justify-content: space-between;
    padding: 12rpx 0;
    font-size: 26rpx;
    color: #666;
    
    .discount {
      color: #FF4D4F;
    }
    
    &.total {
      border-top: 2rpx solid #eee;
      padding-top: 20rpx;
      margin-top: 10rpx;
      font-size: 28rpx;
      color: #333;
      
      .price {
        color: #FF4D4F;
        font-size: 36rpx;
        font-weight: bold;
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
  justify-content: flex-end;
  padding: 20rpx 30rpx;
  background: #fff;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.05);
  
  .btn {
    margin-left: 16rpx;
    padding: 0 30rpx;
    height: 72rpx;
    line-height: 72rpx;
    font-size: 28rpx;
    border-radius: 36rpx;
    border: 2rpx solid #ddd;
    background: #fff;
    color: #666;
    
    &.btn-primary {
      border-color: #FF4D4F;
      color: #FF4D4F;
    }
  }
}

.loading {
  padding: 40rpx;
  
  .skeleton {
    height: 500rpx;
    background: linear-gradient(90deg, #f2f2f2 25%, #e6e6e6 50%, #f2f2f2 75%);
    background-size: 200% 100%;
    animation: skeleton-loading 1.5s infinite;
    border-radius: 12rpx;
  }
}

@keyframes skeleton-loading {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
