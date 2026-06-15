<template>
  <div id="app" lang="zh-CN">
    <a href="#main-content" class="skip-link">跳转到主内容</a>
    <template v-if="isAuthPage">
      <router-view />
    </template>
    <template v-else>
      <AppHeader />
      <main id="main-content" class="main-content">
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

const route = useRoute()

const authPages = ['/login', '/register']
const isAuthPage = computed(() => authPages.includes(route.path))
</script>

<style scoped>
.main-content {
  min-height: calc(100vh - 64px - 280px);
}
</style>

<style>
.skip-link {
  position: absolute;
  top: -100px;
  left: 0;
  z-index: 9999;
  padding: 8px 16px;
  background: #1d39c4;
  color: #fff;
  text-decoration: none;
  border-radius: 0 0 8px 8px;
  transition: top 0.2s;
}

.skip-link:focus {
  top: 0;
}
</style>