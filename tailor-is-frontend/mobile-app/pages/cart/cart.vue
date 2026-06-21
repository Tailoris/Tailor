<template>
  <view class="cart-page">
    <view class="cart-content" v-if="cartList.length > 0">
      <view class="cart-header">
        <view class="select-all" @click="toggleSelectAll">
          <text class="checkbox" :class="{ checked: isAllSelected }">{{ isAllSelected ? '✓' : '' }}</text>
          <text>全选</text>
        </view>
        <text class="delete-btn" @click="deleteSelected">删除选中</text>
      </view>
      
      <view class="cart-list">
        <view class="cart-item" v-for="item in cartList" :key="item.id">
          <view class="item-check" @click="toggleItemSelect(item)">
            <text class="checkbox" :class="{ checked: item.selected }">{{ item.selected ? '✓' : '' }}</text>
          </view>
          <image :src="item.image || 'https://via.placeholder.com/200x200'" mode="aspectFill" class="item-image" @click="goDetail(item.productId)"></image>
          <view class="item-info">
            <text class="item-name text-ellipsis-2">{{ item.productName }}</text>
            <text class="item-sku" v-if="item.skuName">{{ item.skuName }}</text>
            <view class="item-bottom">
              <text class="item-price">¥{{ item.price }}</text>
              <view class="quantity-control">
                <view class="btn" @click="changeQuantity(item, -1)" :disabled="item.quantity <= 1">-</view>
                <input type="number" :value="item.quantity" @change="quantityChange(item, $event)" class="quantity-input"></input>
                <view class="btn" @click="changeQuantity(item, 1)">+</view>
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>
    
    <view class="empty-cart" v-else>
      <text class="icon">🛒</text>
      <text class="text">购物车还是空的</text>
      <button class="go-shopping" @click="goShopping">去逛逛</button>
    </view>
    
    <view class="cart-footer" v-if="cartList.length > 0">
      <view class="footer-left">
        <view class="select-all" @click="toggleSelectAll">
          <text class="checkbox" :class="{ checked: isAllSelected }">{{ isAllSelected ? '✓' : '' }}</text>
          <text>全选</text>
        </view>
      </view>
      <view class="footer-right">
        <view class="total-price">
          <text class="label">合计:</text>
          <text class="price">¥{{ totalPrice.toFixed(2) }}</text>
        </view>
        <button class="checkout-btn" :disabled="selectedCount === 0" @click="checkout">
          结算({{ selectedCount }})
        </button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getCart, updateCartQuantity, deleteCart } from '@/api/cart'

const cartList = ref([])

const isAllSelected = computed(() => {
  return cartList.value.length > 0 && cartList.value.every(item => item.selected)
})

const selectedCount = computed(() => {
  return cartList.value.filter(item => item.selected).length
})

const totalPrice = computed(() => {
  return cartList.value
    .filter(item => item.selected)
    .reduce((sum, item) => sum + item.price * item.quantity, 0)
})

onMounted(() => {
  loadCart()
})

async function loadCart() {
  try {
    const res = await getCart()
    cartList.value = (res.data || []).map(item => ({ ...item, selected: false }))
  } catch (e) {
    console.error('加载购物车失败', e)
  }
}

function toggleItemSelect(item) {
  item.selected = !item.selected
}

function toggleSelectAll() {
  const selectAll = !isAllSelected.value
  cartList.value.forEach(item => {
    item.selected = selectAll
  })
}

function changeQuantity(item, delta) {
  const newQty = item.quantity + delta
  if (newQty < 1) return
  updateQuantity(item, newQty)
}

function quantityChange(item, event) {
  const newQty = parseInt(event.detail.value) || 1
  if (newQty < 1) return
  updateQuantity(item, newQty)
}

async function updateQuantity(item, quantity) {
  try {
    await updateCartQuantity({ cartId: item.id, quantity })
    item.quantity = quantity
  } catch (e) {
    uni.showToast({ title: e.message || '更新失败', icon: 'none' })
  }
}

async function deleteSelected() {
  const selected = cartList.value.filter(item => item.selected)
  if (selected.length === 0) {
    uni.showToast({ title: '请选择要删除的商品', icon: 'none' })
    return
  }
  
  uni.showModal({
    title: '确认删除',
    content: `确定要删除${selected.length}件商品吗?`,
    success: async (res) => {
      if (res.confirm) {
        try {
          await Promise.all(selected.map(item => deleteCart(item.id)))
          cartList.value = cartList.value.filter(item => !item.selected)
          uni.showToast({ title: '删除成功', icon: 'success' })
        } catch (e) {
          uni.showToast({ title: '删除失败', icon: 'none' })
        }
      }
    }
  })
}

function checkout() {
  if (selectedCount.value === 0) {
    uni.showToast({ title: '请选择商品', icon: 'none' })
    return
  }
  
  const selectedIds = cartList.value.filter(item => item.selected).map(item => ({
    cartId: item.id,
    quantity: item.quantity
  }))
  
  uni.navigateTo({
    url: `/pages/order/confirm?cartIds=${JSON.stringify(selectedIds)}`
  })
}

function goShopping() {
  uni.switchTab({ url: '/pages/index/index' })
}

function goDetail(productId) {
  uni.navigateTo({ url: `/pages/product/detail?id=${productId}` })
}
</script>

<style lang="scss" scoped>
.cart-page {
  min-height: 100vh;
  background: #f8f8f8;
  padding-bottom: 120rpx;
}

.cart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 30rpx;
  background: #fff;
  
  .select-all {
    display: flex;
    align-items: center;
    
    .checkbox {
      width: 40rpx;
      height: 40rpx;
      border: 2rpx solid #ddd;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 12rpx;
      font-size: 24rpx;
      
      &.checked {
        background: #FF4D4F;
        border-color: #FF4D4F;
        color: #fff;
      }
    }
  }
  
  .delete-btn {
    color: #FF4D4F;
    font-size: 28rpx;
  }
}

.cart-list {
  .cart-item {
    display: flex;
    padding: 24rpx 30rpx;
    background: #fff;
    margin-bottom: 2rpx;
    
    .item-check {
      display: flex;
      align-items: center;
      margin-right: 20rpx;
      
      .checkbox {
        width: 40rpx;
        height: 40rpx;
        border: 2rpx solid #ddd;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 24rpx;
        
        &.checked {
          background: #FF4D4F;
          border-color: #FF4D4F;
          color: #fff;
        }
      }
    }
    
    .item-image {
      width: 160rpx;
      height: 160rpx;
      border-radius: 12rpx;
      margin-right: 20rpx;
      flex-shrink: 0;
    }
    
    .item-info {
      flex: 1;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      
      .item-name {
        font-size: 28rpx;
        color: #333;
        margin-bottom: 8rpx;
      }
      
      .item-sku {
        font-size: 24rpx;
        color: #999;
        margin-bottom: 12rpx;
      }
      
      .item-bottom {
        display: flex;
        align-items: center;
        justify-content: space-between;
        
        .item-price {
          color: #FF4D4F;
          font-size: 32rpx;
          font-weight: bold;
        }
        
        .quantity-control {
          display: flex;
          align-items: center;
          
          .btn {
            width: 48rpx;
            height: 48rpx;
            border: 1rpx solid #ddd;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 32rpx;
            color: #666;
          }
          
          .quantity-input {
            width: 80rpx;
            height: 48rpx;
            text-align: center;
            border-top: 1rpx solid #ddd;
            border-bottom: 1rpx solid #ddd;
            font-size: 28rpx;
          }
        }
      }
    }
  }
}

.empty-cart {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 200rpx;
  
  .icon {
    font-size: 160rpx;
    margin-bottom: 30rpx;
  }
  
  .text {
    font-size: 32rpx;
    color: #999;
    margin-bottom: 40rpx;
  }
  
  .go-shopping {
    background: #FF4D4F;
    color: #fff;
    border: none;
    border-radius: 40rpx;
    padding: 0 60rpx;
    height: 80rpx;
    line-height: 80rpx;
    font-size: 30rpx;
  }
}

.cart-footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 30rpx;
  background: #fff;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.05);
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  
  .footer-left {
    .select-all {
      display: flex;
      align-items: center;
      
      .checkbox {
        width: 40rpx;
        height: 40rpx;
        border: 2rpx solid #ddd;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 12rpx;
        font-size: 24rpx;
        
        &.checked {
          background: #FF4D4F;
          border-color: #FF4D4F;
          color: #fff;
        }
      }
    }
  }
  
  .footer-right {
    display: flex;
    align-items: center;
    
    .total-price {
      margin-right: 24rpx;
      
      .label {
        font-size: 26rpx;
        color: #666;
      }
      
      .price {
        color: #FF4D4F;
        font-size: 36rpx;
        font-weight: bold;
      }
    }
    
    .checkout-btn {
      background: linear-gradient(135deg, #FF4D4F 0%, #FF7875 100%);
      color: #fff;
      border: none;
      border-radius: 40rpx;
      padding: 0 50rpx;
      height: 80rpx;
      line-height: 80rpx;
      font-size: 30rpx;
      
      &[disabled] {
        background: #ccc;
      }
    }
  }
}
</style>
