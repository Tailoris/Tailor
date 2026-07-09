<template>
  <section class="section" aria-label="商品列表区域">
    <div class="section-header">
      <h2 class="section-title">{{ title }}</h2>
      <router-link :to="moreLink" class="view-more" v-if="moreLink" aria-label="查看更多商品">查看更多 →</router-link>
    </div>
    <el-skeleton :loading="loading" animated :rows="2">
      <div v-if="error" class="empty-state">
        <el-icon class="empty-icon"><Warning /></el-icon>
        <p class="empty-text">数据加载失败，请稍后重试</p>
      </div>
      <div v-else class="product-grid">
        <ProductCard
          v-for="product in products"
          :key="product.id"
          :product="product"
        />
        <div v-if="products.length === 0 && !loading" class="empty-state">
          <el-icon class="empty-icon"><ShoppingCart /></el-icon>
          <p class="empty-text">暂无商品</p>
        </div>
      </div>
    </el-skeleton>
  </section>
</template>

<script setup lang="ts">
import { Warning, ShoppingCart } from '@element-plus/icons-vue'
import ProductCard from '@/components/ProductCard.vue'
import type { Product } from '@/types'

defineProps<{
  title: string
  products: Product[]
  loading: boolean
  error?: boolean
  moreLink?: string
}>()
</script>

<style scoped>
.section {
  margin-bottom: 40px;
}
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.section-title {
  font-size: 24px;
  color: #333;
  margin: 0 0 16px;
}
.section-header .section-title {
  margin-bottom: 0;
}
.view-more {
  color: #1d39c4;
  text-decoration: none;
  font-size: 14px;
}
.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}
.empty-state {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #94a3b8;
}
.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}
.empty-text {
  font-size: 14px;
  margin: 0;
}
</style>
