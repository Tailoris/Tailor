<template>
  <section class="section">
    <div class="section-header">
      <h2 class="section-title">{{ title }}</h2>
      <router-link :to="moreLink" class="view-more">查看更多 →</router-link>
    </div>
    <el-skeleton :loading="loading" animated :rows="2">
      <div class="product-grid">
        <ProductCard
          v-for="product in products"
          :key="product.id"
          :product="product"
        />
      </div>
    </el-skeleton>
  </section>
</template>

<script setup lang="ts">
import ProductCard from '@/components/ProductCard.vue'
import type { Product } from '@/types'

defineProps<{
  title: string
  products: Product[]
  loading: boolean
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
</style>
