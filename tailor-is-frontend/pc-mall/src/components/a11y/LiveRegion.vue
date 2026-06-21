<script setup lang="ts">
/**
 * ARIA Live Region 组件 - UX-P3-01
 *
 * 用于动态内容播报，如操作反馈、状态变化等。
 * 支持 polite（礼貌）和 assertive（强制）两种播报优先级。
 */
import { ref, watch } from 'vue'

const props = withDefaults(defineProps<{
  /** 播报文本 */
  message: string
  /** 播报优先级：polite 不打断当前播报，assertive 立即打断 */
  priority?: 'polite' | 'assertive'
  /** 是否在播报后清空文本 */
  clearAfter?: boolean
}>(), {
  priority: 'polite',
  clearAfter: true
})

const liveText = ref('')

watch(() => props.message, (newMsg) => {
  if (!newMsg) return
  // 先清空再设置，确保每次变化都能触发播报
  liveText.value = ''
  requestAnimationFrame(() => {
    liveText.value = newMsg
  })
  if (props.clearAfter) {
    setTimeout(() => {
      liveText.value = ''
    }, 100)
  }
}, { immediate: true })
</script>

<template>
  <div
    class="sr-only"
    :aria-live="priority"
    :role="priority === 'assertive' ? 'alert' : 'status'"
    aria-atomic="true"
  >
    {{ liveText }}
  </div>
</template>

<style scoped>
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
</style>