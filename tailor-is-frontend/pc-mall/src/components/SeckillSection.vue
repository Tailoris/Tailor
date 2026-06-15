<template>
  <section class="section">
    <div class="section-header">
      <h2 class="section-title">限时秒杀</h2>
      <div class="countdown">
        <span>{{ hours }}:{{ minutes }}:{{ seconds }}</span>
      </div>
    </div>
    <el-skeleton :loading="loading" animated :rows="1">
      <div class="product-scroll">
        <ProductCard
          v-for="product in products"
          :key="product.id"
          :product="product"
          class="product-card-item"
        />
      </div>
    </el-skeleton>
  </section>
</template>

<script setup lang="ts">
import ProductCard from '@/components/ProductCard.vue'
import type { Product } from '@/types'

defineProps<{
  products: Product[]
  loading: boolean
  hours: string
  minutes: string
  seconds: string
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
.countdown {
  background: #f5222d;
  color: #fff;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 16px;
  font-weight: 700;
}
.product-scroll {
  display: flex;
  gap: 16px;
  overflow-x: auto;
  padding-bottom: 8px;
}
.product-card-item {
  min-width: 220px;
  flex-shrink: 0;
}
</style>
