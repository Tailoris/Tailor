<template>
  <view class="category-page">
    <view class="search-bar" @click="goSearch">
      <text class="icon">🔍</text>
      <text class="placeholder">搜索商品名称</text>
    </view>
    
    <view class="category-content" v-if="!loading">
      <scroll-view scroll-y class="left-nav">
        <view 
          class="nav-item" 
          :class="{ active: currentCategory === cat.id }" 
          v-for="cat in categories" 
          :key="cat.id"
          @click="selectCategory(cat)"
        >
          <text>{{ cat.name }}</text>
        </view>
      </scroll-view>
      
      <scroll-view scroll-y class="right-content">
        <view class="sub-categories" v-if="currentSubCategories.length > 0">
          <text class="sub-title">{{ currentCategoryName }}</text>
          <view class="sub-grid">
            <view class="sub-item" v-for="sub in currentSubCategories" :key="sub.id" @click="selectSubCategory(sub)">
              <image :src="sub.icon || 'https://via.placeholder.com/150x150'" mode="aspectFill" class="sub-icon"></image>
              <text class="sub-name">{{ sub.name }}</text>
            </view>
          </view>
        </view>
        
        <view class="product-grid">
          <view class="product-item" v-for="item in productList" :key="item.id" @click="goDetail(item.id)">
            <image :src="item.image || 'https://via.placeholder.com/300x300'" mode="aspectFill" class="product-img"></image>
            <view class="product-info">
              <text class="name text-ellipsis-2">{{ item.name }}</text>
              <text class="price">¥{{ item.price }}</text>
            </view>
          </view>
        </view>
      </scroll-view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getCategories, getCategoryProducts } from '@/api/product'

const categories = ref([])
const currentCategory = ref(0)
const currentSubCategories = ref([])
const productList = ref([])
const loading = ref(true)

const currentCategoryName = computed(() => {
  const cat = categories.value.find(c => c.id === currentCategory.value)
  return cat ? cat.name : ''
})

onMounted(() => {
  loadCategories()
})

async function loadCategories() {
  try {
    const res = await getCategories()
    categories.value = res.data || []
    if (categories.value.length > 0) {
      selectCategory(categories.value[0])
    }
  } catch (e) {
    console.error('加载分类失败', e)
  } finally {
    loading.value = false
  }
}

function selectCategory(cat) {
  currentCategory.value = cat.id
  currentSubCategories.value = cat.children || []
  loadProducts(cat.id)
}

async function loadProducts(categoryId) {
  try {
    const res = await getCategoryProducts(categoryId, { page: 1, limit: 20 })
    productList.value = res.data?.list || []
  } catch (e) {
    console.error('加载商品失败', e)
  }
}

function selectSubCategory(sub) {
  uni.showToast({ title: sub.name, icon: 'none' })
}

function goDetail(id) {
  uni.navigateTo({ url: `/pages/product/detail?id=${id}` })
}

function goSearch() {
  uni.showToast({ title: '搜索功能', icon: 'none' })
}
</script>

<style lang="scss" scoped>
.category-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f8f8f8;
}

.search-bar {
  display: flex;
  align-items: center;
  height: 80rpx;
  margin: 20rpx 30rpx;
  padding: 0 24rpx;
  background: #fff;
  border-radius: 40rpx;
  
  .icon {
    font-size: 32rpx;
    margin-right: 16rpx;
  }
  
  .placeholder {
    color: #999;
    font-size: 28rpx;
  }
}

.category-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.left-nav {
  width: 200rpx;
  background: #fff;
  height: calc(100vh - 160rpx);
  
  .nav-item {
    height: 100rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28rpx;
    color: #666;
    position: relative;
    
    &.active {
      background: #f8f8f8;
      color: #FF4D4F;
      font-weight: bold;
      
      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 50%;
        transform: translateY(-50%);
        width: 6rpx;
        height: 40rpx;
        background: #FF4D4F;
        border-radius: 0 6rpx 6rpx 0;
      }
    }
  }
}

.right-content {
  flex: 1;
  background: #fff;
  height: calc(100vh - 160rpx);
  padding: 20rpx;
}

.sub-categories {
  margin-bottom: 30rpx;
  
  .sub-title {
    font-size: 28rpx;
    font-weight: bold;
    margin-bottom: 20rpx;
    display: block;
  }
  
  .sub-grid {
    display: flex;
    flex-wrap: wrap;
    
    .sub-item {
      width: 33.33%;
      display: flex;
      flex-direction: column;
      align-items: center;
      margin-bottom: 30rpx;
      
      .sub-icon {
        width: 100rpx;
        height: 100rpx;
        border-radius: 50%;
        margin-bottom: 12rpx;
      }
      
      .sub-name {
        font-size: 24rpx;
        color: #333;
      }
    }
  }
}

.product-grid {
  display: flex;
  flex-wrap: wrap;
  
  .product-item {
    width: 50%;
    padding: 10rpx;
    
    .product-img {
      width: 100%;
      height: 260rpx;
      border-radius: 12rpx;
    }
    
    .product-info {
      padding: 12rpx 0;
      
      .name {
        font-size: 24rpx;
        color: #333;
        margin-bottom: 8rpx;
      }
      
      .price {
        color: #FF4D4F;
        font-size: 28rpx;
        font-weight: bold;
      }
    }
  }
}
</style>
