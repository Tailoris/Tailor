<template>
  <view class="index-page">
    <view class="header">
      <view class="search-bar" @click="goSearch">
        <text class="icon-search">🔍</text>
        <text class="placeholder">搜索商品</text>
      </view>
    </view>
    
    <scroll-view scroll-y class="content" v-if="!loading">
      <view class="banner-wrap">
        <swiper class="banner-swiper" circular autoplay interval="3000" indicator-dots indicator-active-color="#FF4D4F">
          <swiper-item v-for="(item, index) in banners" :key="index">
            <image :src="item.image" mode="aspectFill" class="banner-img" @click="goBanner(item)"></image>
          </swiper-item>
        </swiper>
      </view>
      
      <view class="category-nav">
        <view class="category-item" v-for="(cat, index) in categories.slice(0, 10)" :key="cat.id" @click="goCategory(cat)">
          <view class="icon-wrap" :style="{ background: cat.color || '#FFE5E5' }">
            <text>{{ cat.icon || '📦' }}</text>
          </view>
          <text class="name">{{ cat.name }}</text>
        </view>
      </view>
      
      <view class="seckill-section" v-if="seckillList.length > 0">
        <view class="section-header flex-between">
          <view class="flex">
            <text class="title">⚡ 限时秒杀</text>
            <view class="countdown">
              <text class="time" v-for="(t, i) in countdownParts" :key="i">{{ t }}</text>
            </view>
          </view>
          <text class="more" @click="goSeckillList">查看更多 ></text>
        </view>
        <scroll-view scroll-x class="seckill-scroll">
          <view class="seckill-item" v-for="item in seckillList" :key="item.id" @click="goDetail(item.id)">
            <image :src="item.image || 'https://via.placeholder.com/300x300'" mode="aspectFill" class="seckill-img"></image>
            <view class="price-wrap">
              <text class="price">¥{{ item.seckillPrice }}</text>
              <text class="original">¥{{ item.price }}</text>
            </view>
          </view>
        </scroll-view>
      </view>
      
      <view class="recommend-section">
        <view class="section-header flex-between">
          <text class="title">✨ 为您推荐</text>
        </view>
        <view class="goods-grid">
          <view class="goods-item" v-for="item in recommendList" :key="item.id" @click="goDetail(item.id)">
            <image :src="item.image || 'https://via.placeholder.com/300x300'" mode="aspectFill" class="goods-img"></image>
            <view class="goods-info">
              <text class="goods-name text-ellipsis-2">{{ item.name }}</text>
              <view class="price-row">
                <text class="price">¥{{ item.price }}</text>
                <text class="sales" v-if="item.sales">已售{{ item.sales }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>
      
      <view class="footer-info">
        <text>-- Tailor IS 智能定制平台 --</text>
      </view>
    </scroll-view>
    
    <view class="loading-wrap" v-if="loading">
      <view class="skeleton" v-for="i in 6" :key="i"></view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { getProducts, getCategories, getSeckillProducts } from '@/api/product'

const banners = ref([
  { id: 1, image: 'https://via.placeholder.com/750x300/FF4D4F/FFFFFF?text=Tailor+IS', link: '' },
  { id: 2, image: 'https://via.placeholder.com/750x300/1890FF/FFFFFF?text=Custom+Fashion', link: '' }
])

const categories = ref([])
const seckillList = ref([])
const recommendList = ref([])
const loading = ref(true)
const countdown = ref(7200)
const countdownParts = ref(['02', '00', '00'])

let timer = null

onMounted(() => {
  loadData()
  startCountdown()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

async function loadData() {
  try {
    const [catRes, seckillRes, productRes] = await Promise.all([
      getCategories(),
      getSeckillProducts(),
      getProducts({ page: 1, limit: 10 })
    ])
    categories.value = catRes.data || []
    seckillList.value = seckillRes.data || []
    recommendList.value = productRes.data?.list || []
  } catch (e) {
    console.error('加载首页数据失败', e)
  } finally {
    loading.value = false
  }
}

function startCountdown() {
  timer = setInterval(() => {
    if (countdown.value <= 0) {
      clearInterval(timer)
      return
    }
    countdown.value--
    const h = Math.floor(countdown.value / 3600)
    const m = Math.floor((countdown.value % 3600) / 60)
    const s = countdown.value % 60
    countdownParts.value = [
      String(h).padStart(2, '0'),
      String(m).padStart(2, '0'),
      String(s).padStart(2, '0')
    ]
  }, 1000)
}

function goSearch() {
  uni.showToast({ title: '搜索功能', icon: 'none' })
}

function goCategory(cat) {
  uni.switchTab({ url: '/pages/category/category' })
}

function goBanner(item) {
  if (item.link) {
    uni.navigateTo({ url: item.link })
  }
}

function goDetail(id) {
  uni.navigateTo({ url: `/pages/product/detail?id=${id}` })
}

function goSeckillList() {
  uni.showToast({ title: '秒杀列表', icon: 'none' })
}
</script>

<style lang="scss" scoped>
.index-page {
  min-height: 100vh;
  background: #f8f8f8;
}

.header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  padding: 20rpx 30rpx;
  background: linear-gradient(135deg, #FF4D4F 0%, #FF7875 100%);
  padding-top: calc(20rpx + env(safe-area-inset-top));
}

.search-bar {
  display: flex;
  align-items: center;
  height: 72rpx;
  padding: 0 24rpx;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 36rpx;
  
  .icon-search {
    font-size: 32rpx;
    margin-right: 16rpx;
  }
  
  .placeholder {
    color: #999;
    font-size: 28rpx;
  }
}

.content {
  margin-top: 140rpx;
}

.banner-wrap {
  padding: 20rpx 30rpx;
  
  .banner-swiper {
    height: 300rpx;
    border-radius: 20rpx;
    overflow: hidden;
    
    .banner-img {
      width: 100%;
      height: 100%;
    }
  }
}

.category-nav {
  display: flex;
  flex-wrap: wrap;
  padding: 30rpx;
  background: #fff;
  margin: 0 20rpx 20rpx;
  border-radius: 20rpx;
  
  .category-item {
    width: 20%;
    display: flex;
    flex-direction: column;
    align-items: center;
    margin-bottom: 30rpx;
    
    .icon-wrap {
      width: 90rpx;
      height: 90rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 50%;
      font-size: 44rpx;
      margin-bottom: 12rpx;
    }
    
    .name {
      font-size: 24rpx;
      color: #333;
    }
  }
}

.seckill-section {
  background: #fff;
  margin: 0 20rpx 20rpx;
  border-radius: 20rpx;
  padding: 24rpx;
  
  .section-header {
    margin-bottom: 24rpx;
    
    .title {
      font-size: 32rpx;
      font-weight: bold;
      margin-right: 20rpx;
    }
    
    .countdown {
      display: flex;
      align-items: center;
      
      .time {
        background: #333;
        color: #fff;
        font-size: 24rpx;
        padding: 4rpx 10rpx;
        border-radius: 8rpx;
        margin: 0 4rpx;
        font-weight: bold;
      }
    }
    
    .more {
      font-size: 24rpx;
      color: #999;
    }
  }
  
  .seckill-scroll {
    white-space: nowrap;
    
    .seckill-item {
      display: inline-block;
      width: 220rpx;
      margin-right: 20rpx;
      
      .seckill-img {
        width: 220rpx;
        height: 220rpx;
        border-radius: 12rpx;
      }
      
      .price-wrap {
        padding: 12rpx 0;
        
        .price {
          color: #FF4D4F;
          font-size: 30rpx;
          font-weight: bold;
        }
        
        .original {
          color: #999;
          font-size: 22rpx;
          text-decoration: line-through;
          margin-left: 12rpx;
        }
      }
    }
  }
}

.recommend-section {
  padding: 0 20rpx;
  
  .section-header {
    padding: 24rpx;
    background: #fff;
    border-radius: 20rpx 20rpx 0 0;
    margin-bottom: 2rpx;
    
    .title {
      font-size: 32rpx;
      font-weight: bold;
    }
  }
  
  .goods-grid {
    display: flex;
    flex-wrap: wrap;
    background: #fff;
    padding: 0 16rpx 20rpx;
    border-radius: 0 0 20rpx 20rpx;
    
    .goods-item {
      width: 50%;
      padding: 16rpx;
      
      .goods-img {
        width: 100%;
        height: 320rpx;
        border-radius: 12rpx;
      }
      
      .goods-info {
        padding: 16rpx 0;
        
        .goods-name {
          font-size: 26rpx;
          color: #333;
          line-height: 1.4;
          margin-bottom: 12rpx;
        }
        
        .price-row {
          display: flex;
          align-items: center;
          justify-content: space-between;
          
          .price {
            color: #FF4D4F;
            font-size: 32rpx;
            font-weight: bold;
          }
          
          .sales {
            color: #999;
            font-size: 22rpx;
          }
        }
      }
    }
  }
}

.footer-info {
  text-align: center;
  padding: 40rpx 0;
  color: #999;
  font-size: 24rpx;
}

.loading-wrap {
  padding: 40rpx 30rpx;
  
  .skeleton {
    height: 400rpx;
    background: linear-gradient(90deg, #f2f2f2 25%, #e6e6e6 50%, #f2f2f2 75%);
    background-size: 200% 100%;
    animation: skeleton-loading 1.5s infinite;
    border-radius: 12rpx;
    margin-bottom: 20rpx;
  }
}

@keyframes skeleton-loading {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
