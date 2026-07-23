<template>
  <header
    class="app-header"
    :class="{ scrolled: isScrolled, mobileMenuOpen: isMobileMenuOpen }"
    role="banner"
  >
    <div class="header-container">
      <div class="header-left">
        <router-link
          to="/"
          class="logo"
          aria-label="裁智云 Tailor IS 首页"
        >
          <span
            class="logo-icon"
            aria-hidden="true"
          >✂️</span>
          <span class="logo-text">裁智云 Tailor IS</span>
        </router-link>
        <nav
          class="nav-links"
          role="navigation"
          aria-label="主导航"
        >
          <router-link
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="nav-item"
            data-testid="main-nav-item"
            :aria-label="item.label"
          >
            {{ item.label }}
            <el-badge v-if="item.badge" :value="item.badge" class="nav-badge" />
          </router-link>
        </nav>
      </div>
      <div class="header-center">
        <div
          class="search-box"
          role="search"
          aria-label="商品搜索"
        >
          <input
            v-model="searchKeyword"
            type="text"
            :placeholder="$t('common.searchPlaceholder')"
            aria-label="搜索商品、店铺"
            @keyup.enter="handleSearch"
            @focus="showSearchSuggestions = true"
            @blur="hideSearchSuggestions"
          >
          <button
            class="search-btn"
            aria-label="执行搜索"
            data-testid="search-button"
            @click="handleSearch"
          >
            <el-icon><Search /></el-icon>
          </button>
        </div>
        <div
          v-if="showSearchSuggestions && searchSuggestions.length > 0"
          class="search-suggestions"
          role="listbox"
          aria-label="搜索建议"
        >
          <div
            v-for="(suggestion, index) in searchSuggestions"
            :key="suggestion.id"
            class="suggestion-item"
            role="option"
            :aria-selected="index === selectedSuggestionIndex"
            :tabindex="0"
            @click="handleSuggestionClick(suggestion)"
            @mouseenter="selectedSuggestionIndex = index"
            @keydown.enter="handleSuggestionClick(suggestion)"
          >
            <el-icon><Search /></el-icon>
            <span>{{ suggestion.name }}</span>
            <span class="suggestion-category">{{ suggestion.categoryName }}</span>
          </div>
        </div>
      </div>
      <div class="header-right">
        <div class="header-actions">
          <router-link
            to="/cart"
            class="action-icon cart-icon"
            aria-label="购物车，共{{ cartStore.totalCount }}件商品"
          >
            <el-icon :size="24">
              <ShoppingCart />
            </el-icon>
            <span
              v-if="cartStore.totalCount > 0"
              class="cart-badge"
              aria-hidden="true"
            >{{
              cartStore.totalCount > 99 ? '99+' : cartStore.totalCount
            }}</span>
          </router-link>
          <el-badge
            v-if="unreadNotifications > 0"
            :value="unreadNotifications > 99 ? '99+' : unreadNotifications"
            class="notification-badge"
          >
            <button
              class="action-icon notification-icon"
              aria-label="通知中心"
              @click="showNotifications = !showNotifications"
            >
              <el-icon :size="24">
                <Bell />
              </el-icon>
            </button>
          </el-badge>
          <button
            v-else
            class="action-icon notification-icon"
            aria-label="通知中心"
            @click="showNotifications = !showNotifications"
          >
            <el-icon :size="24">
              <Bell />
            </el-icon>
          </button>
          <LocaleSwitcher />
          <router-link
            to="/merchant-apply"
            class="merchant-entrance"
            aria-label="商家入驻"
          >
            <el-icon><Shop /></el-icon>
            <span>{{ $t('nav.merchant') }}</span>
          </router-link>
        </div>
        <template v-if="userStore.token">
          <el-dropdown
            trigger="click"
            @command="handleUserCommand"
          >
            <span class="user-info">
              <el-avatar
                :size="32"
                :src="userStore.userInfo?.avatar || PLACEHOLDER_32x32"
              />
              <span class="username">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  {{ $t('nav.profile') }}
                </el-dropdown-item>
                <el-dropdown-item command="orders">
                  <el-icon><List /></el-icon>
                  {{ $t('nav.orders') }}
                </el-dropdown-item>
                <el-dropdown-item command="cart">
                  <el-icon><ShoppingCart /></el-icon>
                  {{ $t('nav.cart') }}
                </el-dropdown-item>
                <el-dropdown-item command="coupons">
                  <el-icon><Ticket /></el-icon>
                  我的优惠券
                </el-dropdown-item>
                <el-dropdown-item command="address">
                  <el-icon><MapLocation /></el-icon>
                  收货地址
                </el-dropdown-item>
                <el-dropdown-item
                  command="logout"
                  divided
                >
                  <el-icon><SwitchButton /></el-icon>
                  {{ $t('nav.logout') }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <div class="auth-buttons">
            <router-link
              to="/login"
              class="auth-btn login-btn"
            >
              {{ $t('nav.login') }}
            </router-link>
            <router-link
              to="/register"
              class="auth-btn register-btn"
            >
              {{ $t('nav.register') }}
            </router-link>
          </div>
        </template>
        <button
          class="mobile-menu-toggle"
          aria-label="切换移动端菜单"
          @click="isMobileMenuOpen = !isMobileMenuOpen"
        >
          <el-icon :size="24">
            <Menu />
          </el-icon>
        </button>
      </div>
    </div>
    <div
      v-if="showNotifications"
      class="notification-panel"
      role="dialog"
      aria-label="通知中心"
    >
      <div class="notification-header">
        <h3>{{ $t('header.notifications') }}</h3>
        <button
          class="clear-all-btn"
          @click="markAllAsRead"
        >
          {{ $t('header.markAllRead') }}
        </button>
      </div>
      <div class="notification-list">
        <div
          v-for="notification in notifications"
          :key="notification.id"
          class="notification-item"
          :class="{ unread: !notification.read }"
          @click="markAsRead(notification.id)"
        >
          <div class="notification-icon">
            <el-icon><Message /></el-icon>
          </div>
          <div class="notification-content">
            <p>{{ notification.content }}</p>
            <span class="notification-time">{{ notification.time }}</span>
          </div>
        </div>
        <div v-if="notifications.length === 0" class="no-notifications">
          {{ $t('header.noNotifications') }}
        </div>
      </div>
    </div>
    <div
      v-if="isMobileMenuOpen"
      class="mobile-overlay"
      @click="isMobileMenuOpen = false"
    ></div>
    <div
      v-if="isMobileMenuOpen"
      class="mobile-menu"
      role="navigation"
      aria-label="移动端导航"
    >
      <div class="mobile-search">
        <input
          v-model="searchKeyword"
          type="text"
          :placeholder="$t('common.searchPlaceholder')"
          @keyup.enter="handleSearch"
        >
        <button @click="handleSearch">
          <el-icon><Search /></el-icon>
        </button>
      </div>
      <nav class="mobile-nav">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="mobile-nav-item"
          @click="isMobileMenuOpen = false"
        >
          {{ item.label }}
        </router-link>
      </nav>
      <div class="mobile-bottom">
        <LocaleSwitcher />
        <router-link
          to="/merchant-apply"
          class="mobile-merchant-btn"
        >
          {{ $t('nav.merchant') }}
        </router-link>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { useUserStore } from '@/store/user';
import { useCartStore } from '@/store/cart';
import { Search, ShoppingCart, ArrowDown, Bell, Menu, User, List, Ticket, MapLocation, SwitchButton, Message, Shop } from '@element-plus/icons-vue';
import { PLACEHOLDER_32x32 } from '@shared/utils/placeholder';
import LocaleSwitcher from './LocaleSwitcher.vue';

const { t } = useI18n();
const router = useRouter();
const userStore = useUserStore();
const cartStore = useCartStore();

const isScrolled = ref(false);
const searchKeyword = ref('');
const showSearchSuggestions = ref(false);
const searchSuggestions = ref<Array<{ id: number; name: string; categoryName: string }>>([]);
const selectedSuggestionIndex = ref(0);
const showNotifications = ref(false);
const isMobileMenuOpen = ref(false);

interface Notification {
  id: number;
  content: string;
  time: string;
  read: boolean;
}

const notifications = ref<Notification[]>([]);

const unreadNotifications = computed(() =>
  notifications.value.filter((n) => !n.read).length
);

const navItems = computed(() => [
  { path: '/', label: t('nav.home'), badge: null },
  { path: '/patterns', label: t('nav.patterns'), badge: null },
  { path: '/pattern-custom', label: t('nav.patternCustom'), badge: null },
  { path: '/design', label: t('nav.design'), badge: null },
  { path: '/community', label: t('nav.community'), badge: null },
  { path: '/knowledge', label: t('nav.knowledge'), badge: null },
  { path: '/academy', label: t('nav.academy'), badge: null },
]);

function handleSearch() {
  if (searchKeyword.value.trim()) {
    router.push({ path: '/products', query: { keyword: searchKeyword.value.trim() } });
    showSearchSuggestions.value = false;
  }
}

function handleSuggestionClick(suggestion: { id: number; name: string }) {
  router.push({ path: '/product/' + suggestion.id });
  searchKeyword.value = suggestion.name;
  showSearchSuggestions.value = false;
}

function hideSearchSuggestions() {
  setTimeout(() => {
    showSearchSuggestions.value = false;
  }, 200);
}

function handleUserCommand(command: string) {
  switch (command) {
    case 'profile':
      router.push('/profile');
      break;
    case 'orders':
      router.push('/orders');
      break;
    case 'cart':
      router.push('/cart');
      break;
    case 'coupons':
      router.push('/profile?tab=coupons');
      break;
    case 'address':
      router.push('/profile?tab=address');
      break;
    case 'logout':
      userStore.logout();
      router.push('/');
      break;
  }
}

function handleScroll() {
  isScrolled.value = window.scrollY > 0;
}

function loadCart() {
  if (userStore.userInfo?.id) {
    cartStore.fetchCart();
  }
}

function loadNotifications() {
  notifications.value = [
    { id: 1, content: '您的订单已发货', time: '5分钟前', read: false },
    { id: 2, content: '关注的店铺上新了', time: '30分钟前', read: false },
    { id: 3, content: '纸样定制需求已确认', time: '2小时前', read: true },
    { id: 4, content: '社区帖子收到新评论', time: '1天前', read: true },
  ];
}

function markAsRead(id: number) {
  const notification = notifications.value.find((n) => n.id === id);
  if (notification) {
    notification.read = true;
  }
}

function markAllAsRead() {
  notifications.value.forEach((n) => {
    n.read = true;
  });
}

watch(searchKeyword, async (keyword) => {
  if (keyword.trim() && showSearchSuggestions.value) {
    searchSuggestions.value = [
      { id: 1, name: `${keyword} 女士连衣裙纸样`, categoryName: '连衣裙' },
      { id: 2, name: `${keyword} 男士衬衫纸样`, categoryName: '衬衫' },
      { id: 3, name: `${keyword} 童装套装纸样`, categoryName: '童装' },
    ];
  } else {
    searchSuggestions.value = [];
  }
});

watch(
  () => userStore.userInfo?.id,
  () => {
    loadCart();
    loadNotifications();
  }
);

onMounted(() => {
  window.addEventListener('scroll', handleScroll);
  loadCart();
  loadNotifications();
});

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll);
});
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
.nav-item {
  text-decoration: none;
  color: #333;
  font-size: 14px;
  padding: 8px 0;
  position: relative;
  transition: color 0.2s;
  display: flex;
  align-items: center;
  gap: 4px;
}
.nav-item:hover,
.nav-item.router-link-active {
  color: #1d39c4;
}
.nav-badge {
  font-size: 10px;
}
.header-center {
  flex: 1;
  max-width: 480px;
  position: relative;
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
.search-suggestions {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  margin-top: 4px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  max-height: 240px;
  overflow-y: auto;
  z-index: 1001;
}
.suggestion-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s;
}
.suggestion-item:hover {
  background: #f1f5f9;
}
.suggestion-item .el-icon {
  color: #94a3b8;
}
.suggestion-item span:first-of-type {
  flex: 1;
  font-size: 14px;
  color: #334155;
}
.suggestion-category {
  font-size: 12px;
  color: #94a3b8;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 4px;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.action-icon {
  position: relative;
  color: #333;
  text-decoration: none;
  display: flex;
  align-items: center;
  padding: 8px;
  cursor: pointer;
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
.notification-badge {
  --el-badge-font-size: 10px;
}
.merchant-entrance {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  background: linear-gradient(135deg, #1d39c4 0%, #3b82f6 100%);
  color: #fff;
  border-radius: 20px;
  font-size: 13px;
  text-decoration: none;
  transition: opacity 0.2s;
}
.merchant-entrance:hover {
  opacity: 0.9;
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
.mobile-menu-toggle {
  display: none;
  background: none;
  border: none;
  cursor: pointer;
  padding: 8px;
}
.notification-panel {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 8px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  width: 360px;
  max-height: 400px;
  z-index: 1002;
}
.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #e2e8f0;
}
.notification-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}
.clear-all-btn {
  background: none;
  border: none;
  color: #1d39c4;
  font-size: 13px;
  cursor: pointer;
}
.notification-list {
  padding: 8px;
  max-height: 320px;
  overflow-y: auto;
}
.notification-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}
.notification-item:hover {
  background: #f8fafc;
}
.notification-item.unread {
  background: #eff6ff;
}
.notification-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e0f2fe;
  border-radius: 8px;
  flex-shrink: 0;
}
.notification-content p {
  margin: 0 0 4px 0;
  font-size: 14px;
  color: #334155;
}
.notification-time {
  font-size: 12px;
  color: #94a3b8;
}
.no-notifications {
  text-align: center;
  padding: 32px;
  color: #94a3b8;
}
.mobile-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 998;
}
.mobile-menu {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  width: 280px;
  background: #fff;
  z-index: 999;
  display: flex;
  flex-direction: column;
  padding: 20px;
}
.mobile-search {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
}
.mobile-search input {
  flex: 1;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
}
.mobile-search button {
  padding: 0 16px;
  background: #1d39c4;
  color: #fff;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}
.mobile-nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.mobile-nav-item {
  padding: 12px 16px;
  text-decoration: none;
  color: #333;
  font-size: 16px;
  border-radius: 8px;
  transition: background 0.2s;
}
.mobile-nav-item:hover,
.mobile-nav-item.router-link-active {
  background: #f1f5f9;
  color: #1d39c4;
}
.mobile-bottom {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #e2e8f0;
}
.mobile-merchant-btn {
  display: block;
  text-align: center;
  padding: 12px;
  background: #1d39c4;
  color: #fff;
  border-radius: 8px;
  text-decoration: none;
  font-size: 14px;
}
@media (max-width: 1024px) {
  .nav-links {
    display: none;
  }
  .header-center {
    max-width: 320px;
  }
}
@media (max-width: 768px) {
  .header-actions {
    display: none;
  }
  .merchant-entrance {
    display: none;
  }
  .mobile-menu-toggle {
    display: block;
  }
  .auth-buttons {
    display: none;
  }
}
</style>