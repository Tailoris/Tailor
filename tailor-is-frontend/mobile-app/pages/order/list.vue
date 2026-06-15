<template>
  <view class="order-list-page">
    <view class="status-tabs">
      <view class="tab" v-for="t in statusTabs" :key="t.value" :class="{ active: currentStatus === t.value }" @click="changeStatus(t.value)">
        {{ t.label }}
      </view>
    </view>
    
    <scroll-view scroll-y class="order-list" v-if="orderList.length > 0" @scrolltolower="loadMore">
      <view class="order-card" v-for="order in orderList" :key="order.id" @click="goDetail(order.id)">
        <view class="order-header">
          <text class="order-no">订单号: {{ order.orderNo }}</text>
          <text class="status" :class="'status-' + order.status">{{ order.statusText }}</text>
        </view>
        
        <view class="order-goods">
          <view class="goods-item" v-for="item in order.items?.slice(0, 3)" :key="item.id">
            <image :src="item.image || 'https://via.placeholder.com/150x150'" mode="aspectFill" class="goods-img"></image>
            <view class="goods-info">
              <text class="name text-ellipsis">{{ item.name }}</text>
              <text class="spec" v-if="item.skuName">{{ item.skuName }}</text>
            </view>
          </view>
          <text class="more-goods" v-if="order.items?.length > 3">+{{ order.items.length - 3 }}件</text>
        </view>
        
        <view class="order-footer">
          <text class="total">共{{ order.totalCount }}件商品 合计: ¥{{ order.totalAmount }}</text>
          <view class="actions" v-if="order.status === 0">
            <button class="btn btn-cancel" @click.stop="cancelOrder(order.id)">取消订单</button>
            <button class="btn btn-pay" @click.stop="payOrder(order.id)">去支付</button>
          </view>
          <view class="actions" v-else-if="order.status === 2">
            <button class="btn btn-confirm" @click.stop="confirmReceive(order.id)">确认收货</button>
          </view>
        </view>
      </view>
    </scroll-view>
    
    <view class="empty" v-else-if="!loading">
      <text class="icon">📦</text>
      <text class="text">暂无订单</text>
    </view>
    
    <view class="loading-skeleton" v-if="loading">
      <view class="skeleton-card" v-for="i in 3" :key="i"></view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getOrders, cancelOrder, confirmReceive, payOrder as payOrderApi } from '@/api/order'

const statusTabs = [
  { label: '全部', value: '' },
  { label: '待付款', value: 0 },
  { label: '待发货', value: 1 },
  { label: '待收货', value: 2 },
  { label: '已完成', value: 3 }
]

const currentStatus = ref('')
const orderList = ref([])
const loading = ref(true)
const page = ref(1)
const hasMore = ref(true)

onMounted(() => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  if (currentPage.options.status !== undefined) {
    currentStatus.value = currentPage.options.status
  }
  loadOrders()
})

function changeStatus(status) {
  currentStatus.value = status
  page.value = 1
  orderList.value = []
  hasMore.value = true
  loadOrders()
}

async function loadOrders() {
  if (!hasMore.value && page.value > 1) return
  
  try {
    const res = await getOrders({
      status: currentStatus.value,
      page: page.value,
      limit: 10
    })
    const newList = res.data?.list || []
    orderList.value = page.value === 1 ? newList : [...orderList.value, ...newList]
    hasMore.value = newList.length >= 10
    page.value++
  } catch (e) {
    console.error('加载订单失败', e)
  } finally {
    loading.value = false
  }
}

function loadMore() {
  loadOrders()
}

function goDetail(id) {
  uni.navigateTo({ url: `/pages/order/detail?id=${id}` })
}

async function cancelOrder(id) {
  uni.showModal({
    title: '确认取消',
    content: '确定要取消该订单吗?',
    success: async (res) => {
      if (res.confirm) {
        try {
          await cancelOrder(id)
          uni.showToast({ title: '订单已取消', icon: 'success' })
          loadOrders()
        } catch (e) {
          uni.showToast({ title: e.message || '取消失败', icon: 'none' })
        }
      }
    }
  })
}

async function payOrder(id) {
  try {
    await payOrderApi(id, { payType: 'wechat' })
    uni.showToast({ title: '支付成功', icon: 'success' })
    loadOrders()
  } catch (e) {
    uni.showToast({ title: e.message || '支付失败', icon: 'none' })
  }
}

async function confirmReceive(id) {
  uni.showModal({
    title: '确认收货',
    content: '确认已收到商品?',
    success: async (res) => {
      if (res.confirm) {
        try {
          await confirmReceive(id)
          uni.showToast({ title: '确认收货成功', icon: 'success' })
          loadOrders()
        } catch (e) {
          uni.showToast({ title: e.message || '操作失败', icon: 'none' })
        }
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.order-list-page {
  min-height: 100vh;
  background: #f8f8f8;
}

.status-tabs {
  display: flex;
  background: #fff;
  padding: 20rpx 0;
  position: sticky;
  top: 0;
  z-index: 10;
  
  .tab {
    flex: 1;
    text-align: center;
    font-size: 28rpx;
    color: #666;
    padding: 10rpx 0;
    position: relative;
    
    &.active {
      color: #FF4D4F;
      font-weight: bold;
      
      &::after {
        content: '';
        position: absolute;
        bottom: 0;
        left: 50%;
        transform: translateX(-50%);
        width: 40rpx;
        height: 4rpx;
        background: #FF4D4F;
        border-radius: 2rpx;
      }
    }
  }
}

.order-list {
  height: calc(100vh - 120rpx);
  padding: 20rpx;
  
  .order-card {
    background: #fff;
    border-radius: 16rpx;
    margin-bottom: 20rpx;
    overflow: hidden;
    
    .order-header {
      display: flex;
      justify-content: space-between;
      padding: 24rpx;
      border-bottom: 2rpx solid #f5f5f5;
      
      .order-no {
        font-size: 26rpx;
        color: #666;
      }
      
      .status {
        font-size: 26rpx;
        font-weight: bold;
        
        &.status-0 { color: #FFA940; }
        &.status-1 { color: #1890FF; }
        &.status-2 { color: #722ED1; }
        &.status-3 { color: #52C41A; }
      }
    }
    
    .order-goods {
      padding: 24rpx;
      
      .goods-item {
        display: flex;
        margin-bottom: 16rpx;
        
        .goods-img {
          width: 140rpx;
          height: 140rpx;
          border-radius: 12rpx;
          margin-right: 20rpx;
          flex-shrink: 0;
        }
        
        .goods-info {
          flex: 1;
          display: flex;
          flex-direction: column;
          justify-content: center;
          
          .name {
            font-size: 28rpx;
            color: #333;
            margin-bottom: 8rpx;
          }
          
          .spec {
            font-size: 24rpx;
            color: #999;
          }
        }
      }
      
      .more-goods {
        color: #999;
        font-size: 26rpx;
      }
    }
    
    .order-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 24rpx;
      border-top: 2rpx solid #f5f5f5;
      
      .total {
        font-size: 26rpx;
        color: #666;
      }
      
      .actions {
        display: flex;
        
        .btn {
          margin-left: 16rpx;
          padding: 0 24rpx;
          height: 56rpx;
          line-height: 56rpx;
          font-size: 24rpx;
          border-radius: 28rpx;
          border: 2rpx solid #ddd;
          background: #fff;
          
          &.btn-cancel {
            color: #666;
          }
          
          &.btn-pay {
            border-color: #FF4D4F;
            color: #FF4D4F;
          }
          
          &.btn-confirm {
            border-color: #FF4D4F;
            color: #FF4D4F;
          }
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

.loading-skeleton {
  padding: 20rpx;
  
  .skeleton-card {
    height: 300rpx;
    background: linear-gradient(90deg, #f2f2f2 25%, #e6e6e6 50%, #f2f2f2 75%);
    background-size: 200% 100%;
    animation: skeleton-loading 1.5s infinite;
    border-radius: 16rpx;
    margin-bottom: 20rpx;
  }
}

@keyframes skeleton-loading {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
