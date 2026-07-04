<template>
  <div v-if="error" class="error-boundary" role="alert">
    <div class="error-boundary__content">
      <h2 class="error-boundary__title">Something went wrong</h2>
      <p class="error-boundary__message">
        An unexpected error occurred. Please try refreshing the page.
      </p>
      <button class="error-boundary__button" @click="resetError">
        Refresh Page
      </button>
    </div>
  </div>
  <slot v-else />
</template>

<script setup lang="ts">
/**
 * P3-L-09: 全局错误边界组件
 *
 * 捕获子组件树中的未处理异常，防止整个应用崩溃。
 * 使用 Vue 3 的 onErrorCaptured 钩子实现。
 *
 * 集成方式:
 *   在 App.vue 中包裹路由视图:
 *   <ErrorBoundary>
 *     <router-view />
 *   </ErrorBoundary>
 */
import { ref } from 'vue'

const error = ref<Error | null>(null)

function resetError() {
  error.value = null
  window.location.reload()
}
</script>

<script lang="ts">
import type { ComponentInternalInstance } from 'vue'

export default {
  // 🔒 M-23修复: 使用 ComponentInternalInstance 替代 any
  errorCaptured(err: Error, instance: ComponentInternalInstance | null, info: string) {
    console.error('[ErrorBoundary] Captured error:', err, info)
    return false // 阻止错误继续冒泡
  },
}
</script>

<style scoped>
.error-boundary {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  padding: 2rem;
  background: var(--color-bg, #f5f5f5);
}

.error-boundary__content {
  text-align: center;
  max-width: 480px;
}

.error-boundary__title {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--color-text-primary, #333);
  margin-bottom: 0.75rem;
}

.error-boundary__message {
  font-size: 0.95rem;
  color: var(--color-text-secondary, #666);
  margin-bottom: 1.5rem;
  line-height: 1.6;
}

.error-boundary__button {
  padding: 0.625rem 1.5rem;
  background: var(--color-primary, #409EFF);
  color: var(--color-white, #fff);
  border: none;
  border-radius: 6px;
  font-size: 0.95rem;
  cursor: pointer;
  transition: opacity 0.2s;
}

.error-boundary__button:hover {
  opacity: 0.9;
}
</style>