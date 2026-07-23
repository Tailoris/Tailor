<template>
  <div class="locale-switcher-wrapper" role="combobox" aria-haspopup="listbox">
    <button
      class="locale-trigger"
      @click="isOpen = !isOpen"
      @mouseenter="isOpen = true"
      aria-label="Language Switcher"
      :aria-expanded="isOpen"
    >
      <svg class="globe-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="12" cy="12" r="10" />
        <path d="M2 12h20" />
        <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z" />
        <path d="M12 2a15.3 15.3 0 0 0-4 10 15.3 15.3 0 0 0 4 10 15.3 15.3 0 0 0 4-10 15.3 15.3 0 0 0-4-10z" />
      </svg>
      <span class="current-lang">{{ currentLangName }}</span>
      <svg v-if="isOpen" class="arrow-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M19 9l-7 7-7-7" />
      </svg>
      <svg v-else class="arrow-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M5 15l7-7 7 7" />
      </svg>
    </button>

    <Transition name="dropdown">
      <div
        v-if="isOpen"
        class="locale-dropdown"
        role="listbox"
        @mouseleave="isOpen = false"
      >
        <div class="dropdown-header">
          <span>{{ $t('common.language') }}</span>
        </div>
        <div class="dropdown-items">
          <button
            v-for="lang in languages"
            :key="lang.code"
            class="dropdown-item"
            :class="{ active: currentLocale === lang.code }"
            :aria-selected="currentLocale === lang.code"
            role="option"
            @click="selectLanguage(lang.code)"
          >
            <span class="lang-flag">{{ lang.flag }}</span>
            <span class="lang-name">{{ lang.name }}</span>
            <span v-if="currentLocale === lang.code" class="lang-check">✓</span>
          </button>
        </div>
        <div class="dropdown-footer">
          <span>{{ $t('common.languageTips') }}</span>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
import { useI18n } from 'vue-i18n';

const { locale, t } = useI18n();

const isOpen = ref(false);
const currentLocale = ref(locale.value);

const languages = [
  { code: 'zh-CN', name: t('locale.zhCN'), flag: '🇨🇳' },
  { code: 'en-US', name: t('locale.enUS'), flag: '🇺🇸' },
  { code: 'es-ES', name: t('locale.esES'), flag: '🇪🇸' },
  { code: 'fr-FR', name: t('locale.frFR'), flag: '🇫🇷' },
  { code: 'ja-JP', name: t('locale.jaJP'), flag: '🇯🇵' },
  { code: 'ko-KR', name: t('locale.koKR'), flag: '🇰🇷' },
];

const currentLangName = computed(() => {
  const lang = languages.find((l) => l.code === currentLocale.value);
  return lang ? lang.name : '';
});

watch(
  () => locale.value,
  (newLocale) => {
    currentLocale.value = newLocale;
    updateLanguages();
  }
);

function updateLanguages() {
  languages[0].name = t('locale.zhCN');
  languages[1].name = t('locale.enUS');
  languages[2].name = t('locale.esES');
  languages[3].name = t('locale.frFR');
  languages[4].name = t('locale.jaJP');
  languages[5].name = t('locale.koKR');
}

function selectLanguage(code: string) {
  currentLocale.value = code;
  locale.value = code;
  localStorage.setItem('tailor-is-locale', code);
  isOpen.value = false;
  document.documentElement.lang = code;
  updateLanguages();
}

function handleClickOutside(event: MouseEvent) {
  const target = event.target as HTMLElement;
  if (!target.closest('.locale-switcher-wrapper')) {
    isOpen.value = false;
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside);
  document.documentElement.lang = currentLocale.value;
});

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside);
});
</script>

<style scoped>
.locale-switcher-wrapper {
  position: relative;
  display: inline-block;
}

.locale-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  background: transparent;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  color: var(--el-text-color-primary);
  font-size: 13px;
  transition: all 0.2s ease;
  min-width: 44px;
  min-height: 44px;
}

.locale-trigger:hover {
  background: var(--el-fill-color-light);
}

.locale-trigger:focus {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 2px;
}

.globe-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.current-lang {
  font-weight: 500;
}

.arrow-icon {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
  transition: transform 0.2s ease;
}

.locale-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  min-width: 200px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  z-index: 2000;
  border: 1px solid var(--el-border-color-light);
}

.dropdown-header {
  padding: 12px 16px;
  background: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color-light);
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.dropdown-items {
  max-height: 280px;
  overflow-y: auto;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 16px;
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 14px;
  color: var(--el-text-color-primary);
  transition: all 0.15s ease;
  text-align: left;
}

.dropdown-item:hover {
  background: var(--el-fill-color-light);
}

.dropdown-item.active {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.lang-flag {
  font-size: 18px;
  flex-shrink: 0;
}

.lang-name {
  flex: 1;
}

.lang-check {
  font-size: 14px;
  color: var(--el-color-primary);
  font-weight: 600;
}

.dropdown-footer {
  padding: 8px 16px;
  background: var(--el-fill-color-light);
  border-top: 1px solid var(--el-border-color-light);
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.25s ease;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(0.95);
}

@media (max-width: 768px) {
  .locale-trigger {
    padding: 6px 8px;
  }

  .current-lang {
    display: none;
  }

  .locale-dropdown {
    right: -20px;
    min-width: 180px;
  }
}
</style>