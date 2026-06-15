<template>
  <section class="section">
    <h2 class="section-title">商品分类</h2>
    <div class="category-grid">
      <div
        v-for="cat in categories.slice(0, 8)"
        :key="cat.id"
        class="category-item"
        @click="$router.push(`/products?categoryId=${cat.id}`)"
      >
        <img :src="cat.icon || 'https://via.placeholder.com/60x60'" :alt="cat.name" />
        <span>{{ cat.name }}</span>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { ProductCategory } from '@/types'

defineProps<{
  categories: ProductCategory[]
}>()
</script>

<style scoped>
.section {
  margin-bottom: 40px;
}
.section-title {
  font-size: 24px;
  color: #333;
  margin: 0 0 16px;
}
.category-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 16px;
}
.category-item {
  text-align: center;
  cursor: pointer;
  padding: 16px;
  border-radius: 8px;
  background: #f5f5f5;
  transition: background 0.2s;
}
.category-item:hover {
  background: #e6f7ff;
}
.category-item img {
  width: 48px;
  height: 48px;
  margin-bottom: 8px;
}
.category-item span {
  display: block;
  font-size: 14px;
  color: #333;
}
</style>
