<script setup lang="ts">
/**
 * 骨架屏组件 - 修复 F-M09
 *
 * <p>带动画效果的骨架屏，提供流畅的加载占位体验。</p>
 */
withDefaults(defineProps<{
  /** 宽度 */
  width?: string
  /** 高度 */
  height?: string
  /** 圆角 */
  radius?: string
  /** 是否为圆形 */
  circle?: boolean
  /** 行数（多行骨架） */
  rows?: number
  /** 动画类型 */
  animation?: 'pulse' | 'wave' | 'none'
}>(), {
  width: '100%',
  height: '1em',
  radius: '4px',
  circle: false,
  rows: 1,
  animation: 'wave'
})
</script>

<template>
  <div
    v-for="row in rows"
    :key="row"
    class="skeleton-item"
    :class="[`skeleton-${animation}`, { 'skeleton-circle': circle }]"
    :style="{
      width,
      height: row === 1 ? height : height,
      borderRadius: circle ? '50%' : radius,
      marginBottom: rows > 1 ? '8px' : '0'
    }"
    role="status"
    aria-label="加载中"
  />
</template>

<style scoped>
.skeleton-item {
  background-color: #e8e8e8;
  display: block;
}

/* 脉冲动画 */
.skeleton-pulse {
  animation: skeleton-pulse 1.5s ease-in-out infinite;
}

@keyframes skeleton-pulse {
  0% { opacity: 1; }
  50% { opacity: 0.5; }
  100% { opacity: 1; }
}

/* 波浪动画 */
.skeleton-wave {
  background: linear-gradient(
    90deg,
    #e8e8e8 0%,
    #f5f5f5 50%,
    #e8e8e8 100%
  );
  background-size: 200% 100%;
  animation: skeleton-wave 1.5s ease-in-out infinite;
}

@keyframes skeleton-wave {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* 圆形 */
.skeleton-circle {
  border-radius: 50% !important;
}

/* 减少动画（无障碍） */
@media (prefers-reduced-motion: reduce) {
  .skeleton-pulse,
  .skeleton-wave {
    animation: none;
  }
}
</style>
