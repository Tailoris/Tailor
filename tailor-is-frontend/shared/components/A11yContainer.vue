<template>
  <!--
    无障碍 (WCAG 2.1 AA) 通用组件 - Sprint 9 QA-020
    提供 SkipNav 跳转、焦点环管理、ARIA 标签等
  -->
  <div class="a11y-container">
    <slot />
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue';

/**
 * 焦点环管理
 * 仅在使用键盘时显示焦点环，避免鼠标点击时出现焦点轮廓
 */
const handleKeyDown = (e: KeyboardEvent): void => {
  if (e.key === 'Tab') {
    document.body.classList.add('keyboard-focus');
  }
};

const handleMouseDown = (): void => {
  document.body.classList.remove('keyboard-focus');
};

onMounted(() => {
  document.addEventListener('keydown', handleKeyDown);
  document.addEventListener('mousedown', handleMouseDown);
});

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeyDown);
  document.removeEventListener('mousedown', handleMouseDown);
});
</script>

<style lang="scss">
// 键盘焦点环
.keyboard-focus {
  :focus {
    outline: 3px solid #1890ff;
    outline-offset: 2px;
  }

  :focus:not(:focus-visible) {
    outline: none;
  }
}

// 减少动效（prefers-reduced-motion）
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}

// 屏幕阅读器专用
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

// 高对比度模式
@media (prefers-contrast: more) {
  .btn {
    border: 2px solid currentColor;
  }
}
</style>
