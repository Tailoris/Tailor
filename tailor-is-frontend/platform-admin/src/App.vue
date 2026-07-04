<template>
  <router-view v-slot="{ Component, route }">
    <transition name="fade" mode="out-in">
      <component :is="Component" :key="route.fullPath" />
    </transition>
  </router-view>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { useWindowSize } from '@vueuse/core'

// 响应式窗口尺寸
const { width } = useWindowSize()

// 移动端断点
const MOBILE_BREAKPOINT = 768
const isMobile = () => width.value < MOBILE_BREAKPOINT

// 移动端菜单状态
import { ref } from 'vue'
const sidebarOpen = ref(false)

const onResize = () => {
  if (width.value >= MOBILE_BREAKPOINT) {
    sidebarOpen.value = false
  }
}

onMounted(() => {
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
})

// 提供给子组件
import { provide } from 'vue'
provide('isMobile', isMobile)
provide('sidebarOpen', sidebarOpen)
</script>

<style>
/* 全局过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 响应式断点 */
:root {
  --breakpoint-mobile: 768px;
  --breakpoint-tablet: 1024px;
  --breakpoint-desktop: 1280px;
}

@media (max-width: 768px) {
  /* 移动端全局调整 */
  html {
    font-size: 14px;
  }

  body {
    overflow-x: hidden;
  }
}

@media (max-width: 1024px) and (min-width: 769px) {
  /* 平板调整 */
  html {
    font-size: 15px;
  }
}

/* 隐藏移动端元素 */
@media (max-width: 768px) {
  .desktop-only {
    display: none !important;
  }
}

/* 隐藏桌面端元素 */
@media (min-width: 769px) {
  .mobile-only {
    display: none !important;
  }
}
</style>
