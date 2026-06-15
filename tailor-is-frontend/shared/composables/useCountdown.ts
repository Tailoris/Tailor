import { ref } from 'vue'

/**
 * 验证码倒计时 Composable
 * @param duration 倒计时秒数，默认 60
 * @returns
 */
export function useCountdown(duration = 60) {
  const counting = ref(false)
  const remaining = ref(0)
  let timer: ReturnType<typeof setInterval> | null = null

  function start() {
    if (counting.value) return
    counting.value = true
    remaining.value = duration

    timer = setInterval(() => {
      remaining.value--
      if (remaining.value <= 0) {
        stop()
      }
    }, 1000)
  }

  function stop() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
    counting.value = false
    remaining.value = 0
  }

  function getButtonText(): string {
    return counting.value ? `${remaining.value}s` : '发送验证码'
  }

  return { counting, remaining, start, stop, getButtonText }
}
