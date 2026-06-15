<template>
  <el-menu
    class="sidebar-menu"
    :default-active="activeMenu"
    :collapse="appStore.sidebarCollapsed"
    background-color="transparent"
    text-color="rgba(255, 255, 255, 0.85)"
    active-text-color="#FFFFFF"
    :default-openeds="['shop', 'finance', 'marketing']"
    router
    aria-label="主导航"
  >
    <el-menu-item index="/dashboard">
      <el-icon><HomeFilled /></el-icon>
      <span>仪表盘</span>
    </el-menu-item>

    <el-menu-item index="/product">
      <el-icon><Goods /></el-icon>
      <span>商品管理</span>
    </el-menu-item>

    <el-menu-item index="/order">
      <el-icon><Document /></el-icon>
      <span>订单管理</span>
    </el-menu-item>

    <el-menu-item index="/aftersale">
      <el-icon><Service /></el-icon>
      <span>售后工单</span>
    </el-menu-item>

    <el-sub-menu index="shop">
      <template #title>
        <el-icon><Shop /></el-icon>
        <span>店铺管理</span>
      </template>
      <el-menu-item index="/shop/employees">员工管理</el-menu-item>
      <el-menu-item index="/shop/settings">店铺设置</el-menu-item>
    </el-sub-menu>

    <el-sub-menu index="finance">
      <template #title>
        <el-icon><Money /></el-icon>
        <span>财务结算</span>
      </template>
      <el-menu-item index="/finance/settlement">结算记录</el-menu-item>
      <el-menu-item index="/finance/withdraw">提现管理</el-menu-item>
    </el-sub-menu>

    <el-sub-menu index="marketing">
      <template #title>
        <el-icon><Present /></el-icon>
        <span>营销中心</span>
      </template>
      <el-menu-item index="/marketing/coupon">优惠券</el-menu-item>
      <el-menu-item index="/marketing/seckill">秒杀活动</el-menu-item>
    </el-sub-menu>
  </el-menu>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/store/app'
import {
  HomeFilled,
  Goods,
  Document,
  Service,
  Shop,
  Money,
  Present,
} from '@element-plus/icons-vue'

const route = useRoute()
const appStore = useAppStore()

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/product')) return '/product'
  if (path.startsWith('/order')) return '/order'
  if (path.startsWith('/aftersale')) return '/aftersale'
  if (path.startsWith('/shop/employees')) return '/shop/employees'
  if (path.startsWith('/shop/settings')) return '/shop/settings'
  if (path.startsWith('/finance/settlement')) return '/finance/settlement'
  if (path.startsWith('/finance/withdraw')) return '/finance/withdraw'
  if (path.startsWith('/marketing/coupon')) return '/marketing/coupon'
  if (path.startsWith('/marketing/seckill')) return '/marketing/seckill'
  return path
})
</script>

<style scoped>
.sidebar-menu {
  flex: 1;
  border-right: none;
  overflow-y: auto;
}

.sidebar-menu.el-menu--collapse {
  width: var(--sidebar-collapsed-width);
}

:deep(.el-menu-item),
:deep(.el-sub-menu__title) {
  border-radius: 8px;
  margin: 4px 8px;
}

:deep(.el-menu-item.is-active) {
  background-color: rgba(255, 255, 255, 0.2) !important;
}

:deep(.el-menu-item:hover),
:deep(.el-sub-menu__title:hover) {
  background-color: rgba(255, 255, 255, 0.1) !important;
}

:deep(.el-sub-menu .el-menu-item) {
  min-width: 120px;
}
</style>
