<template>
  <header class="app-header" :class="{ scrolled: isScrolled }" role="banner">
    <div class="header-container">
      <div class="header-left">
        <router-link to="/" class="logo" aria-label="裁智云 Tailor IS 首页">
          <span class="logo-icon" aria-hidden="true">✂️</span>
          <span class="logo-text">裁智云 Tailor IS</span>
        </router-link>
        <nav class="nav-links" role="navigation" aria-label="主导航">
          <router-link to="/" data-testid="main-nav-item">首页</router-link>
          <router-link to="/products" data-testid="main-nav-item">商品</router-link>
          <router-link to="/community" data-testid="main-nav-item">社区</router-link>
          <router-link to="/merchant-apply" data-testid="main-nav-item">商家入驻</router-link>
        </nav>
      </div>
      <div class="header-center">
        <div class="search-box" role="search" aria-label="商品搜索">
          <input v-model="searchKeyword" type="text" placeholder="搜索商品、店铺" aria-label="搜索商品、店铺" @keyup.enter="handleSearch" />
          <button class="search-btn" @click="handleSearch" aria-label="执行搜索" data-testid="search-button">
            <el-icon><Search /></el-icon>
          </button>
        </div>
      </div>
      <div class="header-right">
        <router-link to="/cart" class="cart-icon" aria-label="购物车，共{{ cartStore.totalCount }}件商品">
          <el-icon :size="24"><ShoppingCart /></el-icon>
          <span v-if="cartStore.totalCount > 0" class="cart-badge" aria-hidden="true">{{ cartStore.totalCount > 99 ? '99+' : cartStore.totalCount }}</span>
        </router-link>
        <template v-if="userStore.token">
          <el-dropdown trigger="click" @command="handleUserCommand">
            <span class="user-info">
              <el-avatar :size="32" :src="userStore.userInfo?.avatar || 'https://via.placeholder.com/32x32'" />
              <span class="username">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="orders">我的订单</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <div class="auth-buttons">
            <router-link to="/login" class="auth-btn login-btn">登录</router-link>
            <router-link to="/register" class="auth-btn register-btn">注册</router-link>
          </div>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useCartStore } from '@/store/cart'
import { Search, ShoppingCart, ArrowDown } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()

const isScrolled = ref(false)
const searchKeyword = ref('')

function handleSearch() {
  if (searchKeyword.value.trim()) {
    router.push({ path: '/products', query: { keyword: searchKeyword.value.trim() } })
  }
}

function handleUserCommand(command: string) {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'orders':
      router.push('/orders')
      break
    case 'logout':
      userStore.logout()
      router.push('/')
      break
  }
}

function handleScroll() {
  isScrolled.value = window.scrollY > 0
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
  if (userStore.token) {
    cartStore.fetchCart()
  }
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: 1000;
  background: #fff;
  transition: box-shadow 0.3s;
}
.app-header.scrolled {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
.header-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  height: 64px;
  display: flex;
  align-items: center;
  gap: 24px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 32px;
  flex-shrink: 0;
}
.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  font-weight: 700;
  font-size: 20px;
  color: #1d39c4;
}
.logo-icon {
  font-size: 24px;
}
.nav-links {
  display: flex;
  gap: 24px;
}
.nav-links a {
  text-decoration: none;
  color: #333;
  font-size: 14px;
  padding: 8px 0;
  position: relative;
  transition: color 0.2s;
}
.nav-links a:hover,
.nav-links a.router-link-active {
  color: #1d39c4;
}
.header-center {
  flex: 1;
  max-width: 480px;
}
.search-box {
  display: flex;
  border: 2px solid #1d39c4;
  border-radius: 20px;
  overflow: hidden;
}
.search-box input {
  flex: 1;
  border: none;
  padding: 8px 16px;
  font-size: 14px;
  outline: none;
}
.search-btn {
  background: #1d39c4;
  border: none;
  padding: 0 16px;
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}
.cart-icon {
  position: relative;
  color: #333;
  text-decoration: none;
  display: flex;
  align-items: center;
  padding: 8px;
}
.cart-badge {
  position: absolute;
  top: 0;
  right: 0;
  background: #f5222d;
  color: #fff;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 10px;
  min-width: 16px;
  text-align: center;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}
.username {
  font-size: 14px;
  color: #333;
}
.auth-buttons {
  display: flex;
  gap: 8px;
}
.auth-btn {
  text-decoration: none;
  padding: 6px 16px;
  border-radius: 4px;
  font-size: 14px;
}
.login-btn {
  color: #1d39c4;
  border: 1px solid #1d39c4;
}
.register-btn {
  background: #1d39c4;
  color: #fff;
  border: 1px solid #1d39c4;
}
</style>
