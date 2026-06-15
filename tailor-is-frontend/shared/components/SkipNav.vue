<script setup lang="ts">
/**
 * 跳过导航链接组件 - 修复 F-M07
 *
 * <p>无障碍设计 - 为使用键盘和屏幕阅读器的用户提供快速跳转到主内容区的链接。</p>
 */
withDefaults(defineProps<{
  /** 目标内容ID */
  target?: string
  /** 显示文字 */
  label?: string
}>(), {
  target: 'main-content',
  label: '跳到主要内容'
})
</script>

<template>
  <a
    :href="`#${target}`"
    class="skip-nav"
    role="navigation"
    aria-label="跳到主要内容"
  >
    {{ label }}
  </a>
</template>

<style scoped>
.skip-nav {
  position: absolute;
  top: -100px;
  left: 0;
  background: #409eff;
  color: white;
  padding: 12px 24px;
  z-index: 9999;
  text-decoration: none;
  border-radius: 0 0 4px 0;
  font-size: 14px;
  font-weight: 500;
  transition: top 0.2s ease-in-out;
}

.skip-nav:focus,
.skip-nav:focus-visible {
  top: 0;
  outline: 2px solid #fff;
  outline-offset: 2px;
}

/* 高对比度模式 */
@media (prefers-contrast: more) {
  .skip-nav {
    background: #000;
    color: #fff;
  }
}
</style>
