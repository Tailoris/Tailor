<template>
  <div class="locale-switcher" role="radiogroup" aria-label="Language Selection">
    <button
      :class="['locale-btn', { active: currentLocale === 'zh-CN' }]"
      role="radio"
      :aria-checked="currentLocale === 'zh-CN'"
      aria-label="Switch to Chinese"
      title="简体中文"
      @click="changeLocale('zh-CN')"
    >
      中文
    </button>
    <span class="locale-divider">|</span>
    <button
      :class="['locale-btn', { active: currentLocale === 'en-US' }]"
      role="radio"
      :aria-checked="currentLocale === 'en-US'"
      aria-label="Switch to English"
      title="English"
      @click="changeLocale('en-US')"
    >
      EN
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'

const { locale } = useI18n()
const currentLocale = ref(locale.value)

function changeLocale(lang: string) {
  currentLocale.value = lang
  locale.value = lang
  localStorage.setItem('tailor-is-locale', lang)
}
</script>

<style scoped>
.locale-switcher {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 13px;
}

.locale-btn {
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  color: var(--el-text-color-primary);
  min-width: 44px;
  min-height: 44px;
  transition: all 0.2s;
}

.locale-btn:hover {
  background: var(--el-fill-color-light);
}

.locale-btn.active {
  color: var(--el-color-primary);
  font-weight: 600;
}

.locale-divider {
  color: var(--el-text-color-disabled);
}
</style>