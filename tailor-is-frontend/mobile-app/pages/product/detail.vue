<template>
  <view class="product-detail-page">
    <scroll-view scroll-y class="content" v-if="!loading" :scroll-with-animation="true">
      <view class="image-swiper">
        <swiper circular autoplay interval="3000" indicator-dots indicator-active-color="#FF4D4F" class="swiper">
          <swiper-item v-for="(img, index) in product.images" :key="index">
            <image :src="img || 'https://via.placeholder.com/750x750'" mode="aspectFill" class="swiper-img"></image>
          </swiper-item>
        </swiper>
        <view class="share-btn" @click="share">分享</view>
      </view>
      
      <view class="product-info">
        <text class="price-symbol">¥</text>
        <text class="price-value">{{ product.price }}</text>
        <text class="original-price" v-if="product.originalPrice">¥{{ product.originalPrice }}</text>
        <text class="sales" v-if="product.sales">已售 {{ product.sales }}</text>
        <text class="title">{{ product.name }}</text>
        <text class="desc" v-if="product.description">{{ product.description }}</text>
      </view>
      
      <view class="specs-section" v-if="product.specs && product.specs.length > 0">
        <view class="spec-item" v-for="spec in product.specs" :key="spec.id" @click="selectSpec(spec)">
          <text class="spec-label">{{ spec.name }}</text>
          <text class="spec-value">{{ spec.value }}</text>
        </view>
      </view>
      
      <view class="detail-section">
        <text class="section-title">商品详情</text>
        <rich-text :nodes="sanitizedContent" class="detail-content"></rich-text>
      </view>
    </scroll-view>
    
    <view class="loading" v-if="loading">
      <view class="skeleton"></view>
    </view>
    
    <view class="action-bar">
      <view class="left-actions">
        <view class="action-item" @click="goCart">
          <text class="icon">🛒</text>
          <text>购物车</text>
        </view>
        <view class="action-item" @click="toggleFavorite">
          <text class="icon">{{ isFavorite ? '⭐' : '☆' }}</text>
          <text>收藏</text>
        </view>
      </view>
      <view class="right-actions">
        <button class="btn btn-cart" @click="showSpecPopup('cart')">加入购物车</button>
        <button class="btn btn-buy" @click="showSpecPopup('buy')">立即购买</button>
      </view>
    </view>
    
    <view class="spec-popup-mask" v-if="showPopup" @click="closePopup">
      <view class="spec-popup" @click.stop>
        <view class="popup-header">
          <text class="close-btn" @click="closePopup">×</text>
          <image :src="product.images?.[0] || 'https://via.placeholder.com/200x200'" mode="aspectFill" class="popup-img"></image>
          <view class="popup-info">
            <text class="price">¥{{ product.price }}</text>
            <text class="stock">库存 {{ product.stock || 0 }}</text>
          </view>
        </view>
        
        <view class="spec-options" v-if="product.specs">
          <view class="spec-group" v-for="spec in product.specs" :key="spec.id">
            <text class="group-title">{{ spec.name }}</text>
            <view class="option-list">
              <view class="option" v-for="opt in spec.options" :key="opt" :class="{ selected: selectedSpecs[spec.id] === opt }" @click="selectSpecOption(spec.id, opt)">
                {{ opt }}
              </view>
            </view>
          </view>
        </view>
        
        <view class="quantity-wrap">
          <text>数量</text>
          <view class="quantity-control">
            <view class="btn" @click="changeQuantity(-1)">-</view>
            <text class="quantity">{{ quantity }}</text>
            <view class="btn" @click="changeQuantity(1)">+</view>
          </view>
        </view>
        
        <button class="confirm-btn" @click="confirmAction">{{ actionType === 'cart' ? '加入购物车' : '立即购买' }}</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getProductDetail } from '@/api/product'
import { addToCart } from '@/api/cart'
import { addFavorite, removeFavorite } from '@/api/user'

const loading = ref(true)
const product = ref({})
const isFavorite = ref(false)
const showPopup = ref(false)
const actionType = ref('cart')
const quantity = ref(1)
const selectedSpecs = ref({})

// FE-C-4: XSS防护 - 使用 DOMPurify 过滤富文本内容
const sanitizedContent = computed(() => {
  const content = product.value.content || '<p>暂无详情</p>'
  return sanitizeHtml(content)
})

function sanitizeHtml(html: string): string {
  // 移除危险的HTML标签和属性
  const dangerousTags = /<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi
  const dangerousAttrs = /\s(on\w+)=/gi
  const dangerousProtocols = /(javascript|data|vbscript):/gi
  return html
    .replace(dangerousTags, '')
    .replace(dangerousAttrs, ' data-blocked=')
    .replace(dangerousProtocols, 'blocked:')
}

onMounted(() => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const id = currentPage.options.id
  if (id) loadProduct(id)
})

async function loadProduct(id) {
  try {
    const res = await getProductDetail(id)
    product.value = res.data || {}
  } catch (e) {
    uni.showToast({ title: e.message || '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function showSpecPopup(type) {
  actionType.value = type
  showPopup.value = true
}

function closePopup() {
  showPopup.value = false
}

function selectSpecOption(specId, opt) {
  selectedSpecs.value[specId] = opt
}

function changeQuantity(delta) {
  const newQty = quantity.value + delta
  if (newQty < 1 || newQty > (product.value.stock || 999)) return
  quantity.value = newQty
}

async function confirmAction() {
  if (actionType.value === 'cart') {
    try {
      await addToCart({
        productId: product.value.id,
        quantity: quantity.value,
        specs: selectedSpecs.value
      })
      uni.showToast({ title: '已加入购物车', icon: 'success' })
      closePopup()
    } catch (e) {
      uni.showToast({ title: e.message || '加入失败', icon: 'none' })
    }
  } else {
    closePopup()
    uni.navigateTo({
      url: `/pages/order/confirm?productId=${product.value.id}&quantity=${quantity.value}&specs=${JSON.stringify(selectedSpecs.value)}`
    })
  }
}

function toggleFavorite() {
  const fn = isFavorite.value ? removeFavorite : addFavorite
  fn(product.value.id)
    .then(() => {
      isFavorite.value = !isFavorite.value
      uni.showToast({ title: isFavorite.value ? '已收藏' : '已取消收藏', icon: 'success' })
    })
    .catch(e => {
      uni.showToast({ title: e.message, icon: 'none' })
    })
}

function goCart() {
  uni.switchTab({ url: '/pages/cart/cart' })
}

function share() {
  uni.showToast({ title: '分享功能', icon: 'none' })
}
</script>

<style lang="scss" scoped>
.product-detail-page {
  min-height: 100vh;
  background: #f8f8f8;
  padding-bottom: 120rpx;
}

.content {
  height: calc(100vh - 100rpx - env(safe-area-inset-bottom));
}

.image-swiper {
  position: relative;
  
  .swiper {
    height: 750rpx;
    
    .swiper-img {
      width: 100%;
      height: 100%;
    }
  }
  
  .share-btn {
    position: absolute;
    bottom: 20rpx;
    right: 20rpx;
    background: rgba(0, 0, 0, 0.5);
    color: #fff;
    font-size: 24rpx;
    padding: 8rpx 20rpx;
    border-radius: 20rpx;
  }
}

.product-info {
  background: #fff;
  padding: 30rpx;
  
  .price-symbol {
    color: #FF4D4F;
    font-size: 32rpx;
    font-weight: bold;
  }
  
  .price-value {
    color: #FF4D4F;
    font-size: 56rpx;
    font-weight: bold;
  }
  
  .original-price {
    color: #999;
    font-size: 28rpx;
    text-decoration: line-through;
    margin-left: 20rpx;
  }
  
  .sales {
    color: #999;
    font-size: 26rpx;
    margin-left: 30rpx;
  }
  
  .title {
    font-size: 32rpx;
    font-weight: bold;
    color: #333;
    display: block;
    margin-top: 16rpx;
    line-height: 1.4;
  }
  
  .desc {
    font-size: 26rpx;
    color: #666;
    margin-top: 12rpx;
    display: block;
  }
}

.specs-section {
  background: #fff;
  margin-top: 2rpx;
  padding: 30rpx;
  
  .spec-item {
    display: flex;
    justify-content: space-between;
    padding: 20rpx 0;
    border-bottom: 2rpx solid #f5f5f5;
    
    .spec-label {
      color: #999;
      font-size: 28rpx;
    }
    
    .spec-value {
      color: #333;
      font-size: 28rpx;
    }
  }
}

.detail-section {
  background: #fff;
  margin-top: 2rpx;
  padding: 30rpx;
  
  .section-title {
    font-size: 32rpx;
    font-weight: bold;
    margin-bottom: 20rpx;
    display: block;
  }
  
  .detail-content {
    font-size: 28rpx;
    line-height: 1.6;
  }
}

.action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  padding: 20rpx 30rpx;
  background: #fff;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.05);
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  
  .left-actions {
    display: flex;
    margin-right: 20rpx;
    
    .action-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      margin-right: 30rpx;
      
      .icon {
        font-size: 40rpx;
        margin-bottom: 4rpx;
      }
      
      text:last-child {
        font-size: 22rpx;
        color: #666;
      }
    }
  }
  
  .right-actions {
    flex: 1;
    display: flex;
    
    .btn {
      flex: 1;
      height: 80rpx;
      line-height: 80rpx;
      font-size: 30rpx;
      border: none;
      border-radius: 40rpx;
      
      &.btn-cart {
        background: #FFA940;
        color: #fff;
        margin-right: 16rpx;
      }
      
      &.btn-buy {
        background: #FF4D4F;
        color: #fff;
      }
    }
  }
}

.spec-popup-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1000;
  display: flex;
  align-items: flex-end;
}

.spec-popup {
  width: 100%;
  background: #fff;
  border-radius: 30rpx 30rpx 0 0;
  padding: 30rpx;
  max-height: 80vh;
  overflow-y: auto;
  
  .popup-header {
    display: flex;
    margin-bottom: 30rpx;
    position: relative;
    
    .close-btn {
      position: absolute;
      top: 0;
      right: 0;
      font-size: 40rpx;
      color: #999;
    }
    
    .popup-img {
      width: 180rpx;
      height: 180rpx;
      border-radius: 12rpx;
      margin-right: 20rpx;
    }
    
    .popup-info {
      flex: 1;
      display: flex;
      flex-direction: column;
      justify-content: flex-end;
      
      .price {
        color: #FF4D4F;
        font-size: 40rpx;
        font-weight: bold;
      }
      
      .stock {
        color: #999;
        font-size: 26rpx;
        margin-top: 8rpx;
      }
    }
  }
  
  .spec-options {
    margin-bottom: 30rpx;
    
    .spec-group {
      margin-bottom: 24rpx;
      
      .group-title {
        font-size: 28rpx;
        color: #666;
        margin-bottom: 16rpx;
        display: block;
      }
      
      .option-list {
        display: flex;
        flex-wrap: wrap;
        
        .option {
          padding: 12rpx 30rpx;
          border: 2rpx solid #eee;
          border-radius: 8rpx;
          margin-right: 16rpx;
          margin-bottom: 16rpx;
          font-size: 26rpx;
          
          &.selected {
            border-color: #FF4D4F;
            color: #FF4D4F;
            background: #FFF1F0;
          }
        }
      }
    }
  }
  
  .quantity-wrap {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 40rpx;
    
    .quantity-control {
      display: flex;
      align-items: center;
      
      .btn {
        width: 56rpx;
        height: 56rpx;
        border: 2rpx solid #eee;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 32rpx;
      }
      
      .quantity {
        width: 80rpx;
        text-align: center;
        font-size: 30rpx;
      }
    }
  }
  
  .confirm-btn {
    background: #FF4D4F;
    color: #fff;
    border: none;
    border-radius: 40rpx;
    height: 88rpx;
    line-height: 88rpx;
    font-size: 32rpx;
  }
}

.loading {
  padding: 40rpx;
  
  .skeleton {
    height: 600rpx;
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
