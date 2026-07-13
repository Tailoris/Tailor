<template>
  <div
    class="home-view"
    role="main"
    aria-label="首页"
  >
    <BannerCarousel />

    <CategoryNav :categories="categories" />

    <div class="modules-grid">
      <router-link
        to="/patterns"
        class="module-card"
        aria-label="纸样商城"
      >
        <div class="module-icon">
          📐
        </div>
        <div class="module-info">
          <h3>纸样商城</h3>
          <p>海量纸样任您挑选</p>
        </div>
        <el-icon class="arrow-icon">
          <ArrowRight />
        </el-icon>
      </router-link>

      <router-link
        to="/pattern-custom"
        class="module-card"
        aria-label="纸样定制"
      >
        <div class="module-icon">
          ✂️
        </div>
        <div class="module-info">
          <h3>纸样定制</h3>
          <p>专业打版师一对一服务</p>
        </div>
        <el-icon class="arrow-icon">
          <ArrowRight />
        </el-icon>
      </router-link>

      <router-link
        to="/design"
        class="module-card"
        aria-label="服装设计"
      >
        <div class="module-icon">
          🎨
        </div>
        <div class="module-info">
          <h3>服装设计</h3>
          <p>灵感库与协作工具</p>
        </div>
        <el-icon class="arrow-icon">
          <ArrowRight />
        </el-icon>
      </router-link>

      <router-link
        to="/community"
        class="module-card"
        aria-label="社区交流"
      >
        <div class="module-icon">
          💬
        </div>
        <div class="module-info">
          <h3>社区交流</h3>
          <p>技术交流与资源对接</p>
        </div>
        <el-icon class="arrow-icon">
          <ArrowRight />
        </el-icon>
      </router-link>

      <router-link
        to="/knowledge"
        class="module-card"
        aria-label="知识库"
      >
        <div class="module-icon">
          📚
        </div>
        <div class="module-info">
          <h3>知识库</h3>
          <p>专业知识与问答悬赏</p>
        </div>
        <el-icon class="arrow-icon">
          <ArrowRight />
        </el-icon>
      </router-link>

      <router-link
        to="/academy"
        class="module-card"
        aria-label="线上学堂"
      >
        <div class="module-icon">
          🎓
        </div>
        <div class="module-info">
          <h3>线上学堂</h3>
          <p>课程学习与证书认证</p>
        </div>
        <el-icon class="arrow-icon">
          <ArrowRight />
        </el-icon>
      </router-link>
    </div>

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
      :error="featuredError"
      more-link="/patterns"
    />

    <ProductGrid
      title="新品上市"
      :products="newProducts"
      :loading="newLoading"
      :error="newError"
      more-link="/patterns"
    />

    <CommunitySection
      :posts="communityPosts"
      :loading="communityLoading"
      :error="communityError"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import BannerCarousel from '@/components/BannerCarousel.vue';
import CategoryNav from '@/components/CategoryNav.vue';
import SeckillSection from '@/components/SeckillSection.vue';
import ProductGrid from '@/components/ProductGrid.vue';
import CommunitySection from '@/components/CommunitySection.vue';
import { getHotProducts, getNewProducts, getSeckillProducts, getCategories } from '@/api/product';
import { getPosts } from '@/api/community';
import { ArrowRight } from '@element-plus/icons-vue';
import type { Product, ProductCategory, Post } from '@/types';

const categories = ref<ProductCategory[]>([]);
const seckillProducts = ref<Product[]>([]);
const featuredProducts = ref<Product[]>([]);
const newProducts = ref<Product[]>([]);
const communityPosts = ref<Post[]>([]);

const seckillLoading = ref(true);
const featuredLoading = ref(true);
const newLoading = ref(true);
const communityLoading = ref(true);

const categoriesError = ref(false);
const seckillError = ref(false);
const featuredError = ref(false);
const newError = ref(false);
const communityError = ref(false);

const countdownHours = ref('02');
const countdownMinutes = ref('00');
const countdownSeconds = ref('00');

let countdownTimer: ReturnType<typeof setInterval> | null = null;
let targetTime: Date = new Date();

function startCountdown() {
  targetTime = new Date();
  targetTime.setHours(targetTime.getHours() + 2);
  countdownTimer = setInterval(() => {
    const now = new Date();
    const diff = targetTime.getTime() - now.getTime();
    if (diff <= 0) {
      countdownHours.value = '00';
      countdownMinutes.value = '00';
      countdownSeconds.value = '00';
      return;
    }
    const hours = Math.floor(diff / (1000 * 60 * 60));
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
    const seconds = Math.floor((diff % (1000 * 60)) / 1000);
    countdownHours.value = String(hours).padStart(2, '0');
    countdownMinutes.value = String(minutes).padStart(2, '0');
    countdownSeconds.value = String(seconds).padStart(2, '0');
  }, 1000);
}

async function loadCategories() {
  try {
    categories.value = await getCategories();
    categoriesError.value = false;
  } catch {
    categories.value = [];
    categoriesError.value = true;
  }
}

async function loadSeckillProducts() {
  try {
    seckillProducts.value = await getSeckillProducts({ showError: false });
    seckillError.value = false;
  } catch {
    seckillProducts.value = [];
    seckillError.value = true;
  } finally {
    seckillLoading.value = false;
  }
}

async function loadFeaturedProducts() {
  try {
    featuredProducts.value = await getHotProducts({ showError: false });
    featuredError.value = false;
  } catch {
    featuredProducts.value = [];
    featuredError.value = true;
  } finally {
    featuredLoading.value = false;
  }
}

async function loadNewProducts() {
  try {
    newProducts.value = await getNewProducts({ showError: false });
    newError.value = false;
  } catch {
    newProducts.value = [];
    newError.value = true;
  } finally {
    newLoading.value = false;
  }
}

async function loadCommunityPosts() {
  try {
    const res = await getPosts({ current: 1, size: 3 }, { showError: false });
    communityPosts.value = res.records || [];
    communityError.value = false;
  } catch {
    communityPosts.value = [];
    communityError.value = true;
  } finally {
    communityLoading.value = false;
  }
}

onMounted(async () => {
  startCountdown();
  await Promise.all([loadCategories(), loadSeckillProducts()]);
  setTimeout(async () => {
    await Promise.all([loadFeaturedProducts(), loadNewProducts(), loadCommunityPosts()]);
  }, 100);
});

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer);
});
</script>

<style scoped>
.home-view {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.modules-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin: 32px 0;
}

.module-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 12px;
  text-decoration: none;
  transition: all 0.3s ease;
  border: 1px solid transparent;
}

.module-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border-color: #e2e8f0;
}

.module-icon {
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  background: white;
  border-radius: 12px;
  flex-shrink: 0;
}

.module-info {
  flex: 1;
  min-width: 0;
}

.module-info h3 {
  margin: 0 0 4px 0;
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
}

.module-info p {
  margin: 0;
  font-size: 14px;
  color: #64748b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.arrow-icon {
  color: #94a3b8;
  font-size: 18px;
  flex-shrink: 0;
}

@media (min-width: 768px) {
  .modules-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (min-width: 1024px) {
  .modules-grid {
    grid-template-columns: repeat(6, 1fr);
  }

  .module-card {
    flex-direction: column;
    text-align: center;
    padding: 24px 16px;
  }

  .module-info h3 {
    font-size: 16px;
  }

  .module-info p {
    font-size: 12px;
    white-space: normal;
  }

  .arrow-icon {
    display: none;
  }
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
