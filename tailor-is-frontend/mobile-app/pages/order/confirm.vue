<template>
  <view class="order-confirm-page">
    <view class="address-section" @click="goAddress">
      <view class="address-info" v-if="selectedAddress">
        <view class="name-phone">
          <text class="name">{{ selectedAddress.name }}</text>
          <text class="phone">{{ selectedAddress.phone }}</text>
        </view>
        <text class="address">{{ selectedAddress.fullAddress }}</text>
      </view>
      <view class="address-empty" v-else>
        <text>请选择收货地址</text>
      </view>
      <text class="arrow">></text>
    </view>
    
    <view class="goods-section">
      <view class="goods-item" v-for="item in goodsList" :key="item.id">
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
    
    <view class="coupon-section" @click="selectCoupon">
      <text>优惠券</text>
      <view class="right">
        <text class="value" v-if="coupon">{{ coupon.name }}</text>
        <text class="arrow">></text>
      </view>
    </view>
    
    <view class="remark-section">
      <text>订单备注</text>
      <input type="text" placeholder="选填: 对本订单的说明" v-model="remark" class="input"></input>
    </view>
    
    <view class="price-section">
      <view class="price-item">
        <text>商品总价</text>
        <text>¥{{ goodsTotal.toFixed(2) }}</text>
      </view>
      <view class="price-item" v-if="coupon">
        <text>优惠券</text>
        <text class="discount">-¥{{ coupon.amount.toFixed(2) }}</text>
      </view>
      <view class="price-item">
        <text>运费</text>
        <text>{{ freight > 0 ? `¥${freight.toFixed(2)}` : '免运费' }}</text>
      </view>
    </view>
    
    <view class="footer-bar">
      <view class="total">
        <text>合计:</text>
        <text class="price">¥{{ finalTotal.toFixed(2) }}</text>
      </view>
      <button class="submit-btn" @click="submitOrder">提交订单</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { createOrder } from '@/api/order'
import { getAddresses } from '@/api/user'
import { getCartItems } from '@/api/cart'
import { getProductDetail } from '@/api/product'

const selectedAddress = ref(null)
const goodsList = ref([])
const coupon = ref(null)
const remark = ref('')
const freight = ref(0)

onMounted(() => {
  loadAddress()
  loadGoods()
})

async function loadAddress() {
  try {
    const res = await getAddresses()
    const list = res.data || []
    selectedAddress.value = list.find(a => a.isDefault) || list[0] || null
  } catch (e) {
    console.error('加载地址失败', e)
  }
}

async function loadGoods() {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const cartIds = currentPage.options.cartIds
  const productId = currentPage.options.productId
  
  if (cartIds) {
    // FE-C-3: 移除硬编码，改为调用真实商品API
    try {
      const cartIdList = JSON.parse(cartIds)
      const res = await getCartItems(cartIdList)
      goodsList.value = (res.data || []).map(item => ({
        id: item.productId,
        cartId: item.cartId,
        name: item.productName || '商品',
        price: item.price || 0,
        quantity: item.quantity || 1,
        image: item.image || ''
      }))
    } catch (e) {
      console.error('加载购物车商品失败', e)
      uni.showToast({ title: '加载商品失败', icon: 'none' })
    }
  } else if (productId) {
    // FE-C-3: 移除硬编码，改为调用真实商品API
    try {
      const res = await getProductDetail(productId)
      const product = res.data || {}
      goodsList.value = [{
        id: productId,
        name: product.name || '商品',
        price: product.price || 0,
        quantity: parseInt(currentPage.options.quantity) || 1,
        image: product.mainImage || ''
      }]
    } catch (e) {
      console.error('加载商品详情失败', e)
      uni.showToast({ title: '加载商品失败', icon: 'none' })
    }
  }
}

const goodsTotal = computed(() => {
  return goodsList.value.reduce((sum, item) => sum + item.price * item.quantity, 0)
})

const finalTotal = computed(() => {
  let total = goodsTotal.value + freight.value
  if (coupon.value) {
    total -= coupon.value.amount
  }
  return Math.max(0, total)
})

function goAddress() {
  uni.navigateTo({ url: '/pages/address/list?select=1' })
}

function selectCoupon() {
  uni.showToast({ title: '选择优惠券', icon: 'none' })
}

async function submitOrder() {
  if (!selectedAddress.value) {
    uni.showToast({ title: '请选择收货地址', icon: 'none' })
    return
  }
  
  try {
    const res = await createOrder({
      addressId: selectedAddress.value.id,
      items: goodsList.value.map(item => ({
        productId: item.id,
        quantity: item.quantity
      })),
      couponId: coupon.value?.id,
      remark: remark.value
    })
    uni.showToast({ title: '订单创建成功', icon: 'success' })
    setTimeout(() => {
      uni.redirectTo({ url: `/pages/order/detail?id=${res.data.orderId}` })
    }, 500)
  } catch (e) {
    uni.showToast({ title: e.message || '创建订单失败', icon: 'none' })
  }
}
</script>

<style lang="scss" scoped>
.order-confirm-page {
  min-height: 100vh;
  background: #f8f8f8;
  padding-bottom: 140rpx;
}

.address-section {
  display: flex;
  align-items: center;
  padding: 30rpx;
  background: #fff;
  margin-bottom: 20rpx;
  
  .address-info {
    flex: 1;
    
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
  
  .address-empty {
    flex: 1;
    color: #999;
    font-size: 28rpx;
  }
  
  .arrow {
    color: #999;
    font-size: 32rpx;
    margin-left: 20rpx;
  }
}

.goods-section {
  background: #fff;
  padding: 30rpx;
  margin-bottom: 20rpx;
  
  .goods-item {
    display: flex;
    margin-bottom: 24rpx;
    
    .goods-img {
      width: 160rpx;
      height: 160rpx;
      border-radius: 12rpx;
      margin-right: 20rpx;
      flex-shrink: 0;
    }
    
    .goods-info {
      flex: 1;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      
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
        align-items: center;
        
        .price {
          color: #FF4D4F;
          font-size: 30rpx;
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

.coupon-section, .remark-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx 30rpx;
  background: #fff;
  margin-bottom: 2rpx;
  font-size: 28rpx;
  
  .right {
    display: flex;
    align-items: center;
    
    .value {
      color: #FF4D4F;
      margin-right: 12rpx;
    }
  }
  
  .arrow {
    color: #999;
  }
  
  .input {
    text-align: right;
    font-size: 26rpx;
    color: #999;
  }
}

.price-section {
  background: #fff;
  padding: 30rpx;
  margin-top: 20rpx;
  
  .price-item {
    display: flex;
    justify-content: space-between;
    padding: 12rpx 0;
    font-size: 28rpx;
    color: #666;
    
    .discount {
      color: #FF4D4F;
    }
  }
}

.footer-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 20rpx 30rpx;
  background: #fff;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.05);
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  
  .total {
    margin-right: 24rpx;
    
    text:first-child {
      font-size: 26rpx;
      color: #666;
    }
    
    .price {
      color: #FF4D4F;
      font-size: 36rpx;
      font-weight: bold;
    }
  }
  
  .submit-btn {
    background: #FF4D4F;
    color: #fff;
    border: none;
    border-radius: 40rpx;
    padding: 0 50rpx;
    height: 80rpx;
    line-height: 80rpx;
    font-size: 30rpx;
  }
}
</style>
