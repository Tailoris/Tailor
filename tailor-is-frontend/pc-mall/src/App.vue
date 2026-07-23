<template>
  <div
    id="app"
    :lang="currentLocale"
  >
    <SkipNav />
    <template v-if="isAuthPage">
      <ErrorBoundary>
        <router-view />
      </ErrorBoundary>
    </template>
    <template v-else>
      <AppHeader />
      <BreadcrumbNav />
      <main
        id="main-content"
        class="main-content"
        role="main"
        aria-label="主内容区域"
        tabindex="-1"
      >
        <ErrorBoundary>
          <router-view v-slot="{ Component }">
            <transition name="fade" mode="out-in">
              <div :key="$route.fullPath">
                <component :is="Component" />
              </div>
            </transition>
          </router-view>
        </ErrorBoundary>
      </main>
      <AppFooter />
      <BackToTop />
    </template>
    <div
      v-if="appStore.isGlobalLoading"
      class="global-loading-overlay"
      role="status"
      aria-label="加载中"
    >
      <div class="loading-spinner">
        <div class="custom-spinner"></div>
        <span>{{ $t('common.loading') }}</span>
      </div>
    </div>
    <TransitionGroup name="toast">
      <div
        v-for="toast in appStore.toasts"
        :key="toast.id"
        class="global-toast"
        :class="toast.type"
        role="status"
        aria-label="提示信息"
        @click="appStore.removeToast(toast.id)"
      >
        <span class="toast-icon">
          <span v-if="toast.type === 'success'">✓</span>
          <span v-else-if="toast.type === 'error'">✕</span>
          <span v-else-if="toast.type === 'warning'">⚠</span>
          <span v-else>ℹ</span>
        </span>
        <span>{{ toast.message }}</span>
        <button class="toast-close" @click.stop="appStore.removeToast(toast.id)">
          ✕
        </button>
      </div>
    </TransitionGroup>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { useAppStore } from '@/store/app';

import AppHeader from '@/components/AppHeader.vue';
import AppFooter from '@/components/AppFooter.vue';
import BreadcrumbNav from '@/components/BreadcrumbNav.vue';
import BackToTop from '@/components/BackToTop.vue';
import SkipNav from '@shared/components/SkipNav.vue';
import ErrorBoundary from '@shared/components/ErrorBoundary.vue';

const { locale } = useI18n();
const route = useRoute();
const router = useRouter();
const appStore = useAppStore();

const currentLocale = computed(() => locale.value);

const authPages = ['/login', '/register', '/forgot-password'];
const isAuthPage = computed(() => authPages.includes(route.path));

router.beforeEach(() => {
  appStore.setGlobalLoading(true);
});

router.afterEach(() => {
  appStore.setGlobalLoading(false);
});

onMounted(() => {
  window.addEventListener('toast', (e: Event) => {
    const event = e as CustomEvent<{ message: string; type?: 'success' | 'error' | 'warning' | 'info' }>;
    appStore.showToast(event.detail.message, event.detail.type);
  });
  
  window.addEventListener('loading', (e: Event) => {
    const event = e as CustomEvent<{ loading: boolean }>;
    appStore.setGlobalLoading(event.detail.loading);
  });
});

onUnmounted(() => {});
</script>

<style scoped>
.main-content {
  min-height: calc(100vh - 64px - 280px);
}

@media (min-width: 1536px) {
  .main-content {
    min-height: calc(100vh - 72px - 320px);
  }
}

@media (min-width: 1920px) {
  .main-content {
    min-height: calc(100vh - 80px - 360px);
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.global-loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.loading-spinner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  color: #1d39c4;
}

.custom-spinner {
  width: 48px;
  height: 48px;
  border: 4px solid rgba(29, 57, 196, 0.2);
  border-top-color: #1d39c4;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.global-toast {
  position: fixed;
  top: 80px;
  right: 24px;
  padding: 12px 16px;
  border-radius: 8px;
  color: #fff;
  font-size: 14px;
  z-index: 9998;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  display: flex;
  align-items: center;
  gap: 8px;
  max-width: 400px;
}

.global-toast.success {
  background: #67c23a;
}

.global-toast.error {
  background: #f56c6c;
}

.global-toast.warning {
  background: #e6a23c;
}

.global-toast.info {
  background: #409eff;
}

.toast-close {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.8);
  cursor: pointer;
  padding: 0;
  margin-left: auto;
}

.toast-close:hover {
  color: #fff;
}

.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}

.toast-enter-from {
  opacity: 0;
  transform: translateX(100%);
}

.toast-leave-to {
  opacity: 0;
  transform: translateX(100%);
}

.toast-move {
  transition: transform 0.3s ease;
}
</style>