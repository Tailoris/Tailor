<template>
  <div class="home-view" role="main" aria-label="首页">
    <BannerCarousel />

    <CategoryNav :categories="categories" />

    <SeckillSection
      :products="seckillProducts"
      :loading="seckillLoading"
      :hours="countdownHours"
      :minutes="countdownMinutes"
      :seconds="countdownSeconds"
    />

    <ProductGrid
      title="精选推荐"
      :products="featuredProducts"
      :loading="featuredLoading"
      more-link="/products"
    />

    <ProductGrid
      title="新品上市"
      :products="newProducts"
      :loading="newLoading"
      more-link="/products"
    />

    <CommunitySection
      :posts="communityPosts"
      :loading="communityLoading"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import BannerCarousel from '@/components/BannerCarousel.vue'
import CategoryNav from '@/components/CategoryNav.vue'
import SeckillSection from '@/components/SeckillSection.vue'
import ProductGrid from '@/components/ProductGrid.vue'
import CommunitySection from '@/components/CommunitySection.vue'
import { getHotProducts, getNewProducts, getSeckillProducts, getCategories } from '@/api/product'
import { getPosts } from '@/api/community'
import type { Product, ProductCategory, Post } from '@/types'

const categories = ref<ProductCategory[]>([])
const seckillProducts = ref<Product[]>([])
const featuredProducts = ref<Product[]>([])
const newProducts = ref<Product[]>([])
const communityPosts = ref<Post[]>([])

const seckillLoading = ref(true)
const featuredLoading = ref(true)
const newLoading = ref(true)
const communityLoading = ref(true)

const countdownHours = ref('02')
const countdownMinutes = ref('00')
const countdownSeconds = ref('00')

let countdownTimer: ReturnType<typeof setInterval> | null = null
let targetTime: Date = new Date()

function startCountdown() {
  targetTime = new Date()
  targetTime.setHours(targetTime.getHours() + 2)
  countdownTimer = setInterval(() => {
    const now = new Date()
    const diff = targetTime.getTime() - now.getTime()
    if (diff <= 0) {
      countdownHours.value = '00'
      countdownMinutes.value = '00'
      countdownSeconds.value = '00'
      return
    }
    const hours = Math.floor(diff / (1000 * 60 * 60))
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
    const seconds = Math.floor((diff % (1000 * 60)) / 1000)
    countdownHours.value = String(hours).padStart(2, '0')
    countdownMinutes.value = String(minutes).padStart(2, '0')
    countdownSeconds.value = String(seconds).padStart(2, '0')
  }, 1000)
}

async function loadCategories() {
  try {
    categories.value = await getCategories()
  } catch {
    categories.value = []
  }
}

async function loadSeckillProducts() {
  try {
    seckillProducts.value = await getSeckillProducts()
  } catch {
    seckillProducts.value = []
  } finally {
    seckillLoading.value = false
  }
}

async function loadFeaturedProducts() {
  try {
    featuredProducts.value = await getHotProducts()
  } catch {
    featuredProducts.value = []
  } finally {
    featuredLoading.value = false
  }
}

async function loadNewProducts() {
  try {
    newProducts.value = await getNewProducts()
  } catch {
    newProducts.value = []
  } finally {
    newLoading.value = false
  }
}

async function loadCommunityPosts() {
  try {
    const res = await getPosts({ current: 1, size: 3 })
    communityPosts.value = res.records
  } catch {
    communityPosts.value = []
  } finally {
    communityLoading.value = false
  }
}

onMounted(async () => {
  startCountdown()
  // Critical: load first
  await Promise.all([loadCategories(), loadSeckillProducts()])
  // Non-critical: load after a tick
  setTimeout(async () => {
    await Promise.all([loadFeaturedProducts(), loadNewProducts(), loadCommunityPosts()])
  }, 100)
})

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})
</script>

<style scoped>
.home-view {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

@media (min-width: 1536px) {
  .home-view {
    max-width: 1536px;
    padding: 0 40px;
  }
}

@media (min-width: 1920px) {
  .home-view {
    max-width: 1920px;
    padding: 0 60px;
  }
}
</style>
