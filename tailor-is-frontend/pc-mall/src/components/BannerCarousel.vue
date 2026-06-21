<template>
  <div class="banner-carousel" role="region" aria-label="促销横幅轮播" aria-roledescription="carousel">
    <el-carousel :interval="4000" type="card" height="400px" aria-label="轮播广告">
      <el-carousel-item v-for="(slide, index) in slides" :key="index" :aria-label="`第${index + 1}张: ${slide.title}`" role="group" :aria-roledescription="`slide ${index + 1} of ${slides.length}`">
        <div class="banner-slide" :style="{ background: slide.background }">
          <div class="slide-content">
            <h2 class="slide-title">{{ slide.title }}</h2>
            <p class="slide-desc">{{ slide.description }}</p>
            <el-button type="primary" size="large" @click="handleAction(slide.action)" :aria-label="`${slide.ctaText}: ${slide.title}`">
              {{ slide.ctaText }}
            </el-button>
          </div>
        </div>
      </el-carousel-item>
    </el-carousel>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

interface BannerSlide {
  title: string
  description: string
  ctaText: string
  background: string
  action: string
}

const router = useRouter()

const slides: BannerSlide[] = [
  {
    title: '智能纸样 精准定制',
    description: 'AI驱动的智能纸样生成系统，让定制更简单',
    ctaText: '立即体验',
    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    action: '/products?type=0'
  },
  {
    title: '设计师平台 创意无限',
    description: '汇聚全球优秀设计师，发现独特设计灵感',
    ctaText: '浏览设计',
    background: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
    action: '/community'
  },
  {
    title: '商家入驻 共创未来',
    description: '开放平台，赋能商家，共建服装产业生态',
    ctaText: '申请入驻',
    background: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
    action: '/merchant-apply'
  },
  {
    title: '限时特惠 不容错过',
    description: '精选爆款商品限时折扣，品质生活从这里开始',
    ctaText: '立即抢购',
    background: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
    action: '/products'
  }
]

function handleAction(path: string) {
  router.push(path)
}
</script>

<style scoped>
.banner-carousel {
  margin-bottom: 24px;
}
.banner-slide {
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  overflow: hidden;
  height: 100%;
}
.slide-content {
  text-align: center;
  color: #fff;
  padding: 40px;
}
.slide-title {
  font-size: 36px;
  font-weight: 700;
  margin: 0 0 16px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}
.slide-desc {
  font-size: 18px;
  margin: 0 0 24px;
  opacity: 0.9;
}
</style>
