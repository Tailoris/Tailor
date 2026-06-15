<template>
  <div class="admin-layout">
    <aside class="sidebar" :class="{ collapsed: appStore.sidebarCollapsed }">
      <div class="logo">
        <span class="logo-icon">T</span>
        <span v-show="!appStore.sidebarCollapsed" class="logo-text">Tailor IS</span>
      </div>
      <SidebarMenu />
    </aside>
    <div class="main-wrapper" :class="{ expanded: appStore.sidebarCollapsed }">
      <header class="header">
        <div class="header-left">
          <el-icon class="toggle-btn" @click="appStore.toggleSidebar">
            <Fold v-if="!appStore.sidebarCollapsed" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item
              v-for="(item, index) in appStore.breadcrumbs"
              :key="index"
              :to="index < appStore.breadcrumbs.length - 1 ? { path: '/' } : undefined"
            >
              {{ item.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-select
            v-model="currentShopId"
            class="shop-switcher"
            size="default"
            @change="userStore.switchShop"
          >
            <el-option
              v-for="shop in userStore.shopList"
              :key="shop.id"
              :label="shop.name"
              :value="shop.id"
            />
          </el-select>
          <el-dropdown trigger="click">
            <span class="user-info">
              <el-avatar :size="32" class="user-avatar">
                {{ userStore.userInfo?.name?.charAt(0) || 'U' }}
              </el-avatar>
              <span class="user-name">{{ userStore.userInfo?.name || '用户' }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/shop/settings')">店铺设置</el-dropdown-item>
                <el-dropdown-item @click="router.push('/shop/employees')">员工管理</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <main id="main-content" class="content" :class="`content--${appStore.device}`">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useAppStore } from '@/store/app'
import SidebarMenu from './SidebarMenu.vue'
import { Fold, Expand } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const appStore = useAppStore()

const currentShopId = ref<number | null>(userStore.currentShopId)

watch(
  () => route.path,
  () => {
    const matched = route.matched[route.matched.length - 1]
    if (matched.meta.breadcrumb) {
      appStore.setBreadcrumbs(
        (matched.meta.breadcrumb as string[]).map((title, index, arr) => ({
          title,
          path: index < arr.length - 1 ? '/' : undefined,
        }))
      )
    }
  },
  { immediate: true }
)

onMounted(async () => {
  appStore.initDeviceListener()
  if (userStore.isLoggedIn) {
    await userStore.fetchUserInfo()
    await userStore.fetchShopList()
  }
})

onUnmounted(() => {
  appStore.destroyDeviceListener()
})

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await userStore.logout()
    router.push('/login')
  } catch {
    // cancelled
  }
}
</script>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
}

.sidebar {
  width: var(--sidebar-width);
  background: linear-gradient(180deg, var(--color-primary-dark) 0%, var(--color-primary) 100%);
  transition: width var(--transition);
  display: flex;
  flex-direction: column;
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 100;
}

.sidebar.collapsed {
  width: var(--sidebar-collapsed-width);
}

.logo {
  height: var(--header-height);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 16px;
  color: white;
  font-size: 18px;
  font-weight: 600;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  gap: 10px;
}

.logo-icon {
  width: 32px;
  height: 32px;
  background: white;
  color: var(--color-primary);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 18px;
  flex-shrink: 0;
}

.logo-text {
  white-space: nowrap;
  overflow: hidden;
}

.main-wrapper {
  margin-left: var(--sidebar-width);
  flex: 1;
  transition: margin-left var(--transition);
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.main-wrapper.expanded {
  margin-left: var(--sidebar-collapsed-width);
}

.header {
  height: var(--header-height);
  background: var(--color-white);
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: sticky;
  top: 0;
  z-index: 50;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.toggle-btn {
  font-size: 20px;
  cursor: pointer;
  color: var(--color-text-secondary);
  transition: color var(--transition);
}

.toggle-btn:hover {
  color: var(--color-primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.shop-switcher {
  width: 150px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background var(--transition);
}

.user-info:hover {
  background: var(--color-background);
}

.user-avatar {
  background: var(--color-primary);
  color: white;
  font-size: 14px;
}

.user-name {
  font-size: 14px;
  color: var(--color-text-primary);
}

.content {
  flex: 1;
  padding: 24px;
  background: var(--color-background);
}

@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    transform: translateX(-100%);
  }

  .sidebar:not(.collapsed) {
    transform: translateX(0);
  }

  .main-wrapper {
    margin-left: 0;
  }

  .main-wrapper.expanded {
    margin-left: 0;
  }
}
</style>
