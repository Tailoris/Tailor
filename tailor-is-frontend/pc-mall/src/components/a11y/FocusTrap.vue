<script setup lang="ts">
/**
 * 焦点陷阱包装组件 - UX-P3-01
 *
 * 用于模态框、弹窗等场景，确保焦点在弹出层内循环，
 * 不会 Tab 到背景页面元素。
 */
import { ref, onMounted, onUnmounted } from 'vue'
import { focusTrap } from '@/utils/a11y'

const props = withDefaults(defineProps<{
  /** 是否激活焦点陷阱 */
  active?: boolean
  /** 是否自动聚焦第一个可聚焦元素 */
  autoFocus?: boolean
}>(), {
  active: true,
  autoFocus: true
})

const emit = defineEmits<{
  (e: 'escape'): void
}>()

const containerRef = ref<HTMLElement | null>(null)
let cleanup: (() => void) | null = null

function activate() {
  if (!containerRef.value) return
  cleanup = focusTrap(containerRef.value)
}

function deactivate() {
  cleanup?.()
  cleanup = null
}

onMounted(() => {
  if (props.active) {
    activate()
  }
  if (containerRef.value) {
    containerRef.value.addEventListener('keydown', (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        emit('escape')
      }
    })
  }
})

onUnmounted(() => {
  deactivate()
})
</script>

<template>
  <div ref="containerRef" class="focus-trap-container">
    <slot />
  </div>
</template>

<style scoped>
.focus-trap-container {
  /* 确保焦点陷阱正常工作，不添加干扰性样式 */
}
</style>