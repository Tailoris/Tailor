<template>
  <div id="app" lang="zh-CN">
    <SkipNav />
    <template v-if="isAuthPage">
      <router-view />
    </template>
    <template v-else>
      <AppHeader />
      <main id="main-content" class="main-content" role="main" aria-label="主内容区域" tabindex="-1">
        <router-view />
      </main>
      <AppFooter />
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
import SkipNav from '@shared/components/SkipNav.vue'

const route = useRoute()

const authPages = ['/login', '/register']
const isAuthPage = computed(() => authPages.includes(route.path))
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
</style>