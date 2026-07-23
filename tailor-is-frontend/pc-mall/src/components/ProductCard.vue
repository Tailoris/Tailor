<script setup lang="ts">
/**
 * 商品卡片组件 - 修复 F-M10
 *
 * <p>支持图片懒加载、骨架屏占位、错误回退的通用商品卡片。</p>
 */
import { ref, onMounted, onUnmounted } from 'vue';
import type { Product } from '@/types';

const props = defineProps<{
  product: Product;
}>();

const emit = defineEmits<{
  (_: 'click', _product: Product): void;
}>();

const imageRef = ref<HTMLImageElement | null>(null);
const wrapperRef = ref<HTMLElement | null>(null);
const loaded = ref(false);
const error = ref(false);
const inViewport = ref(false);

const PLACEHOLDER_IMAGE = '/images/products/placeholder.svg';

let observer: IntersectionObserver | null = null;

function onImageLoad() {
  loaded.value = true;
  error.value = false;
}

function onImageError() {
  loaded.value = true;
  error.value = true;
}

function onCardClick() {
  emit('click', props.product);
}

onMounted(() => {
  observer = new IntersectionObserver(
    (entries) => {
      for (const entry of entries) {
        if (entry.isIntersecting) {
          inViewport.value = true;
          observer?.disconnect();
          observer = null;
          break;
        }
      }
    },
    { rootMargin: '200px' }
  );
  if (wrapperRef.value) {
    observer.observe(wrapperRef.value);
  }
});

onUnmounted(() => {
  observer?.disconnect();
  observer = null;
});
</script>

<template>
  <div
    ref="wrapperRef"
    class="product-card"
    role="button"
    :aria-label="`商品 ${product.name}`"
    tabindex="0"
    @click="onCardClick"
  >
    <div class="product-image-wrapper">
      <!-- 骨架屏占位 -->
      <div
        v-if="!loaded"
        class="image-placeholder"
        aria-hidden="true"
      />
      <!-- 错误回退 - 显示默认占位图 -->
      <img
        v-else-if="error"
        :src="PLACEHOLDER_IMAGE"
        :alt="product.name"
        class="product-image fallback-image"
        aria-hidden="true"
      />
      <!-- 实际图片（懒加载） -->
      <img
        v-if="inViewport"
        ref="imageRef"
        :src="product.mainImage"
        :alt="product.name"
        loading="lazy"
        decoding="async"
        class="product-image"
        :class="{ 'is-loaded': loaded && !error }"
        @load="onImageLoad"
        @error="onImageError"
      >
    </div>
    <div class="product-info">
      <h3 class="product-name">
        {{ product.name }}
      </h3>
      <p class="product-price">
        ¥{{ product.price }}
      </p>
    </div>
  </div>
</template>

<style scoped>
.product-card {
  cursor: pointer;
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
  transition:
    transform 0.2s,
    box-shadow 0.2s;
}

.product-card:hover,
.product-card:focus-visible {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  outline: 2px solid #409eff;
  outline-offset: 2px;
}

.product-image-wrapper {
  position: relative;
  width: 100%;
  aspect-ratio: 1;
  background: #f5f5f5;
  overflow: hidden;
}

.image-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, #e8e8e8 0%, #f5f5f5 50%, #e8e8e8 100%);
  background-size: 200% 100%;
  animation: skeleton 1.5s ease-in-out infinite;
}

.fallback-image {
  opacity: 1;
}

.product-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.product-image.is-loaded {
  opacity: 1;
}

.product-info {
  padding: 12px;
}

.product-name {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
  margin: 0 0 8px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-price {
  font-size: 16px;
  color: #b91c1c;
  font-weight: 600;
  margin: 0;
}

@keyframes skeleton {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}
</style>
