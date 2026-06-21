<script setup lang="ts">
/**
 * 跳过导航链接组件 - UX-P3-01
 *
 * 为键盘和屏幕阅读器用户提供快速跳转到主内容区的链接。
 * 该组件是对 shared/SkipNav 的增强包装，额外支持焦点管理。
 */
import { skipToContent } from '@/utils/a11y'

withDefaults(defineProps<{
  target?: string
  label?: string
}>(), {
  target: 'main-content',
  label: '跳到主要内容'
})

function handleClick(e: MouseEvent) {
  e.preventDefault()
  skipToContent()
}
</script>

<template>
  <a
    :href="`#${target}`"
    class="skip-to-content"
    @click="handleClick"
  >
    {{ label }}
  </a>
</template>

<style scoped>
.skip-to-content {
  position: absolute;
  top: -100px;
  left: 50%;
  transform: translateX(-50%);
  background: #1d39c4;
  color: #fff;
  padding: 12px 24px;
  z-index: 10001;
  text-decoration: none;
  border-radius: 0 0 8px 8px;
  font-size: 14px;
  font-weight: 600;
  transition: top 0.2s ease;
  white-space: nowrap;
}

.skip-to-content:focus,
.skip-to-content:focus-visible {
  top: 0;
  outline: 3px solid #fff;
  outline-offset: -1px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}
</style>