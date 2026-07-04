<template>
  <div id="app" lang="zh-CN">
    <a href="#main-content" class="skip-link">跳转到主内容</a>
    <ErrorBoundary>
      <router-view />
    </ErrorBoundary>
  </div>
</template>

<script setup lang="ts">
import ErrorBoundary from '@shared/components/ErrorBoundary.vue'
</script>

<style>
.skip-link {
  position: absolute;
  top: -100px;
  left: 0;
  z-index: 9999;
  padding: 8px 16px;
  background: var(--color-primary);
  color: var(--color-white);
  text-decoration: none;
  border-radius: 0 0 var(--border-radius) var(--border-radius);
  transition: top 0.2s;
}

.skip-link:focus {
  top: 0;
}
</style>