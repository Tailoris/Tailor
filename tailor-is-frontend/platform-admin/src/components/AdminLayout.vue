<template>
  <el-container class="app-layout">
    <el-aside
      :width="isCollapsed ? '64px' : '220px'"
      class="app-sidebar"
    >
      <div class="sidebar-logo">
        <div class="logo-icon">
          <el-icon :size="26">
            <Monitor />
          </el-icon>
        </div>
        <span
          v-show="!isCollapsed"
          class="logo-text"
        >裁智云管理</span>
      </div>
      <el-scrollbar class="sidebar-scroll">
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapsed"
          :collapse-transition="false"
          background-color="#1F2937"
          text-color="#9CA3AF"
          active-text-color="#6366F1"
          router
          unique-opened
          @open="handleMenuOpen"
          @close="handleMenuClose"
          class="sidebar-menu"
        >
          <div
            v-show="!isCollapsed"
            class="menu-group-title"
          >
            <span>主导航</span>
          </div>
          <el-menu-item index="/dashboard">
            <el-icon><DataAnalysis /></el-icon>
            <template #title>
              <span>仪表盘</span>
            </template>
          </el-menu-item>

          <div
            v-show="!isCollapsed"
            class="menu-group-title"
          >
            <span>业务管理</span>
          </div>
          <el-sub-menu 
            index="user" 
            @mouseenter="handleSubMenuMouseEnter('user')"
            @mouseleave="handleSubMenuMouseLeave('user')"
            @touchstart.prevent="handleSubMenuTouchStart('user')"
          >
            <template #title>
              <div 
                tabindex="0" 
                role="menuitem" 
                :aria-expanded="expandedMenus.has('user')" 
                aria-haspopup="true"
                @keydown="handleSubMenuKeydown('user', $event)"
                class="menu-title-wrapper"
              >
                <el-icon><UserFilled /></el-icon>
                <span>用户管理</span>
              </div>
            </template>
            <template #default>
              <div 
                class="sub-menu-content"
                :class="{ 'sub-menu-content-show': expandedMenus.has('user') }"
                @mouseenter="handleSubMenuContentMouseEnter('user')"
                @mouseleave="handleSubMenuContentMouseLeave('user')"
              >
                <el-menu-item index="/users">用户列表</el-menu-item>
                <el-menu-item index="/roles">角色管理</el-menu-item>
              </div>
            </template>
          </el-sub-menu>
          <el-sub-menu 
            index="merchant" 
            @mouseenter="handleSubMenuMouseEnter('merchant')"
            @mouseleave="handleSubMenuMouseLeave('merchant')"
            @touchstart.prevent="handleSubMenuTouchStart('merchant')"
          >
            <template #title>
              <div 
                tabindex="0" 
                role="menuitem" 
                :aria-expanded="expandedMenus.has('merchant')" 
                aria-haspopup="true"
                @keydown="handleSubMenuKeydown('merchant', $event)"
                class="menu-title-wrapper"
              >
                <el-icon><Shop /></el-icon>
                <span>商家管理</span>
              </div>
            </template>
            <template #default>
              <div class="sub-menu-content" :class="{ 'sub-menu-content-show': expandedMenus.has('merchant') }" @mouseenter="handleSubMenuContentMouseEnter('merchant')" @mouseleave="handleSubMenuContentMouseLeave('merchant')">
                <el-menu-item index="/merchant-audit">商家审核</el-menu-item>
                <el-menu-item index="/merchant-list">商家列表</el-menu-item>
              </div>
            </template>
          </el-sub-menu>
          <el-sub-menu 
            index="product" 
            @mouseenter="handleSubMenuMouseEnter('product')"
            @mouseleave="handleSubMenuMouseLeave('product')"
            @touchstart.prevent="handleSubMenuTouchStart('product')"
          >
            <template #title>
              <div 
                tabindex="0" 
                role="menuitem" 
                :aria-expanded="expandedMenus.has('product')" 
                aria-haspopup="true"
                @keydown="handleSubMenuKeydown('product', $event)"
                class="menu-title-wrapper"
              >
                <el-icon><Goods /></el-icon>
                <span>商品管理</span>
              </div>
            </template>
            <template #default>
              <div class="sub-menu-content" :class="{ 'sub-menu-content-show': expandedMenus.has('product') }" @mouseenter="handleSubMenuContentMouseEnter('product')" @mouseleave="handleSubMenuContentMouseLeave('product')">
                <el-menu-item index="/product-list">商品列表</el-menu-item>
                <el-menu-item index="/product-audit">商品审核</el-menu-item>
              </div>
            </template>
          </el-sub-menu>
          <el-sub-menu 
            index="order" 
            @mouseenter="handleSubMenuMouseEnter('order')"
            @mouseleave="handleSubMenuMouseLeave('order')"
            @touchstart.prevent="handleSubMenuTouchStart('order')"
          >
            <template #title>
              <div 
                tabindex="0" 
                role="menuitem" 
                :aria-expanded="expandedMenus.has('order')" 
                aria-haspopup="true"
                @keydown="handleSubMenuKeydown('order', $event)"
                class="menu-title-wrapper"
              >
                <el-icon><Document /></el-icon>
                <span>订单管理</span>
              </div>
            </template>
            <template #default>
              <div class="sub-menu-content" :class="{ 'sub-menu-content-show': expandedMenus.has('order') }" @mouseenter="handleSubMenuContentMouseEnter('order')" @mouseleave="handleSubMenuContentMouseLeave('order')">
                <el-menu-item index="/orders">订单列表</el-menu-item>
                <el-menu-item index="/order-arbitration">售后仲裁</el-menu-item>
              </div>
            </template>
          </el-sub-menu>
          <el-sub-menu 
            index="pattern" 
            @mouseenter="handleSubMenuMouseEnter('pattern')"
            @mouseleave="handleSubMenuMouseLeave('pattern')"
            @touchstart.prevent="handleSubMenuTouchStart('pattern')"
          >
            <template #title>
              <div 
                tabindex="0" 
                role="menuitem" 
                :aria-expanded="expandedMenus.has('pattern')" 
                aria-haspopup="true"
                @keydown="handleSubMenuKeydown('pattern', $event)"
                class="menu-title-wrapper"
              >
                <el-icon><Picture /></el-icon>
                <span>纸样设计</span>
              </div>
            </template>
            <template #default>
              <div class="sub-menu-content" :class="{ 'sub-menu-content-show': expandedMenus.has('pattern') }" @mouseenter="handleSubMenuContentMouseEnter('pattern')" @mouseleave="handleSubMenuContentMouseLeave('pattern')">
                <el-menu-item index="/patterns">纸样管理</el-menu-item>
                <el-menu-item index="/pattern-audit">纸样审核</el-menu-item>
              </div>
            </template>
          </el-sub-menu>
          <el-sub-menu 
            index="copyright" 
            @mouseenter="handleSubMenuMouseEnter('copyright')"
            @mouseleave="handleSubMenuMouseLeave('copyright')"
            @touchstart.prevent="handleSubMenuTouchStart('copyright')"
          >
            <template #title>
              <div 
                tabindex="0" 
                role="menuitem" 
                :aria-expanded="expandedMenus.has('copyright')" 
                aria-haspopup="true"
                @keydown="handleSubMenuKeydown('copyright', $event)"
                class="menu-title-wrapper"
              >
                <el-icon><Lock /></el-icon>
                <span>版权管理</span>
              </div>
            </template>
            <template #default>
              <div class="sub-menu-content" :class="{ 'sub-menu-content-show': expandedMenus.has('copyright') }" @mouseenter="handleSubMenuContentMouseEnter('copyright')" @mouseleave="handleSubMenuContentMouseLeave('copyright')">
                <el-menu-item index="/copyrights">版权登记</el-menu-item>
                <el-menu-item index="/infringement">侵权检测</el-menu-item>
              </div>
            </template>
          </el-sub-menu>

          <div
            v-show="!isCollapsed"
            class="menu-group-title"
          >
            <span>运营支撑</span>
          </div>
          <el-sub-menu 
            index="community" 
            @mouseenter="handleSubMenuMouseEnter('community')"
            @mouseleave="handleSubMenuMouseLeave('community')"
            @touchstart.prevent="handleSubMenuTouchStart('community')"
          >
            <template #title>
              <div 
                tabindex="0" 
                role="menuitem" 
                :aria-expanded="expandedMenus.has('community')" 
                aria-haspopup="true"
                @keydown="handleSubMenuKeydown('community', $event)"
                class="menu-title-wrapper"
              >
                <el-icon><ChatDotRound /></el-icon>
                <span>社区管理</span>
              </div>
            </template>
            <template #default>
              <div class="sub-menu-content" :class="{ 'sub-menu-content-show': expandedMenus.has('community') }" @mouseenter="handleSubMenuContentMouseEnter('community')" @mouseleave="handleSubMenuContentMouseLeave('community')">
                <el-menu-item index="/community-posts">帖子审核</el-menu-item>
                <el-menu-item index="/community-reports">举报处理</el-menu-item>
              </div>
            </template>
          </el-sub-menu>
          <el-sub-menu 
            index="supply" 
            @mouseenter="handleSubMenuMouseEnter('supply')"
            @mouseleave="handleSubMenuMouseLeave('supply')"
            @touchstart.prevent="handleSubMenuTouchStart('supply')"
          >
            <template #title>
              <div 
                tabindex="0" 
                role="menuitem" 
                :aria-expanded="expandedMenus.has('supply')" 
                aria-haspopup="true"
                @keydown="handleSubMenuKeydown('supply', $event)"
                class="menu-title-wrapper"
              >
                <el-icon><Van /></el-icon><span>供应链</span>
              </div>
            </template>
            <template #default>
              <div class="sub-menu-content" :class="{ 'sub-menu-content-show': expandedMenus.has('supply') }" @mouseenter="handleSubMenuContentMouseEnter('supply')" @mouseleave="handleSubMenuContentMouseLeave('supply')">
                <el-menu-item index="/fabrics">面料管理</el-menu-item>
                <el-menu-item index="/suppliers">供应商管理</el-menu-item>
                <el-menu-item index="/purchase-orders">采购订单</el-menu-item>
              </div>
            </template>
          </el-sub-menu>
          <el-sub-menu 
            index="marketing" 
            @mouseenter="handleSubMenuMouseEnter('marketing')"
            @mouseleave="handleSubMenuMouseLeave('marketing')"
            @touchstart.prevent="handleSubMenuTouchStart('marketing')"
          >
            <template #title>
              <div 
                tabindex="0" 
                role="menuitem" 
                :aria-expanded="expandedMenus.has('marketing')" 
                aria-haspopup="true"
                @keydown="handleSubMenuKeydown('marketing', $event)"
                class="menu-title-wrapper"
              >
                <el-icon><Promotion /></el-icon><span>营销管理</span>
              </div>
            </template>
            <template #default>
              <div class="sub-menu-content" :class="{ 'sub-menu-content-show': expandedMenus.has('marketing') }" @mouseenter="handleSubMenuContentMouseEnter('marketing')" @mouseleave="handleSubMenuContentMouseLeave('marketing')">
                <el-menu-item index="/coupons">优惠券管理</el-menu-item>
                <el-menu-item index="/campaigns">活动管理</el-menu-item>
              </div>
            </template>
          </el-sub-menu>
          <el-sub-menu 
            index="analytics" 
            @mouseenter="handleSubMenuMouseEnter('analytics')"
            @mouseleave="handleSubMenuMouseLeave('analytics')"
            @touchstart.prevent="handleSubMenuTouchStart('analytics')"
          >
            <template #title>
              <div 
                tabindex="0" 
                role="menuitem" 
                :aria-expanded="expandedMenus.has('analytics')" 
                aria-haspopup="true"
                @keydown="handleSubMenuKeydown('analytics', $event)"
                class="menu-title-wrapper"
              >
                <el-icon><DataLine /></el-icon><span>数据分析</span>
              </div>
            </template>
            <template #default>
              <div class="sub-menu-content" :class="{ 'sub-menu-content-show': expandedMenus.has('analytics') }" @mouseenter="handleSubMenuContentMouseEnter('analytics')" @mouseleave="handleSubMenuContentMouseLeave('analytics')">
                <el-menu-item index="/reports">数据报告</el-menu-item>
                <el-menu-item index="/statistics">数据统计</el-menu-item>
              </div>
            </template>
          </el-sub-menu>
          <el-sub-menu 
            index="academy" 
            @mouseenter="handleSubMenuMouseEnter('academy')"
            @mouseleave="handleSubMenuMouseLeave('academy')"
            @touchstart.prevent="handleSubMenuTouchStart('academy')"
          >
            <template #title>
              <div 
                tabindex="0" 
                role="menuitem" 
                :aria-expanded="expandedMenus.has('academy')" 
                aria-haspopup="true"
                @keydown="handleSubMenuKeydown('academy', $event)"
                class="menu-title-wrapper"
              >
                <el-icon><Reading /></el-icon><span>线上学堂</span>
              </div>
            </template>
            <template #default>
              <div class="sub-menu-content" :class="{ 'sub-menu-content-show': expandedMenus.has('academy') }" @mouseenter="handleSubMenuContentMouseEnter('academy')" @mouseleave="handleSubMenuContentMouseLeave('academy')">
                <el-menu-item index="/courses">课程管理</el-menu-item>
                <el-menu-item index="/instructors">讲师管理</el-menu-item>
              </div>
            </template>
          </el-sub-menu>
          <el-sub-menu 
            index="finance" 
            @mouseenter="handleSubMenuMouseEnter('finance')"
            @mouseleave="handleSubMenuMouseLeave('finance')"
            @touchstart.prevent="handleSubMenuTouchStart('finance')"
          >
            <template #title>
              <div 
                tabindex="0" 
                role="menuitem" 
                :aria-expanded="expandedMenus.has('finance')" 
                aria-haspopup="true"
                @keydown="handleSubMenuKeydown('finance', $event)"
                class="menu-title-wrapper"
              >
                <el-icon><Money /></el-icon><span>财务管理</span>
              </div>
            </template>
            <template #default>
              <div class="sub-menu-content" :class="{ 'sub-menu-content-show': expandedMenus.has('finance') }" @mouseenter="handleSubMenuContentMouseEnter('finance')" @mouseleave="handleSubMenuContentMouseLeave('finance')">
                <el-menu-item index="/transactions">交易流水</el-menu-item>
                <el-menu-item index="/revenue">收入统计</el-menu-item>
                <el-menu-item index="/settlements">结算管理</el-menu-item>
                <el-menu-item index="/refunds">退款管理</el-menu-item>
                <el-menu-item index="/finance-reports">财务报表</el-menu-item>
                <el-menu-item index="/fee-config">费用配置</el-menu-item>
              </div>
            </template>
          </el-sub-menu>
          <el-sub-menu 
            index="system" 
            @mouseenter="handleSubMenuMouseEnter('system')"
            @mouseleave="handleSubMenuMouseLeave('system')"
            @touchstart.prevent="handleSubMenuTouchStart('system')"
          >
            <template #title>
              <div 
                tabindex="0" 
                role="menuitem" 
                :aria-expanded="expandedMenus.has('system')" 
                aria-haspopup="true"
                @keydown="handleSubMenuKeydown('system', $event)"
                class="menu-title-wrapper"
              >
                <el-icon><Setting /></el-icon><span>系统管理</span>
              </div>
            </template>
            <template #default>
              <div class="sub-menu-content" :class="{ 'sub-menu-content-show': expandedMenus.has('system') }" @mouseenter="handleSubMenuContentMouseEnter('system')" @mouseleave="handleSubMenuContentMouseLeave('system')">
                <el-menu-item index="/audit-logs">操作日志</el-menu-item>
                <el-menu-item index="/settings">系统设置</el-menu-item>
              </div>
            </template>
          </el-sub-menu>
        </el-menu>
      </el-scrollbar>
      <div class="sidebar-footer">
        <el-tooltip
          :content="isCollapsed ? '展开菜单' : '收起菜单'"
          placement="right"
        >
          <el-button
            :icon="isCollapsed ? Expand : Fold"
            text
            class="collapse-btn"
            @click="toggleSidebar"
          />
        </el-tooltip>
      </div>
    </el-aside>
    <el-container>
      <el-header class="app-header">
        <div class="header-left">
          <el-breadcrumb
            separator="/"
            class="header-breadcrumb"
          >
            <el-breadcrumb-item :to="{ path: '/' }">
              首页
            </el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-popover
            v-model:visible="searchVisible"
            placement="bottom"
            :width="480"
            trigger="click"
            @show="handleSearchVisible"
          >
            <template #reference>
              <el-tooltip content="搜索">
                <el-button
                  class="header-icon-btn"
                  :icon="Search"
                  text
                  circle
                />
              </el-tooltip>
            </template>
            <div class="search-panel">
              <el-input
                v-model="searchKeyword"
                placeholder="搜索用户、商品、订单..."
                :prefix-icon="Search"
                clearable
                size="large"
                class="search-input"
                @input="handleSearchInput"
                @clear="handleSearchClear"
              />
              <div
                v-loading="searchLoading"
                class="search-results"
              >
                <template v-if="searchKeyword && !searchLoading">
                  <div
                    v-if="searchResult.total === 0"
                    class="search-empty"
                  >
                    <el-empty
                      description="未找到相关结果"
                      :image-size="60"
                    />
                  </div>
                  <template v-else>
                    <div
                      v-if="searchResult.users.length > 0"
                      class="search-group"
                    >
                      <div class="search-group-title">
                        <el-icon><UserFilled /></el-icon>
                        <span>用户</span>
                      </div>
                      <div
                        v-for="item in searchResult.users.slice(0, 3)"
                        :key="'u' + item.id"
                        class="search-item"
                        @click="goToUser(item.id)"
                      >
                        <el-avatar
                          :size="32"
                          :src="item.avatar"
                          :icon="UserFilled"
                        />
                        <div class="search-item-info">
                          <div class="search-item-title">
                            {{ item.nickname || item.realName || item.username }}
                          </div>
                          <div class="search-item-sub">
                            {{ item.phone || item.email }}
                          </div>
                        </div>
                      </div>
                    </div>
                    <div
                      v-if="searchResult.products.length > 0"
                      class="search-group"
                    >
                      <div class="search-group-title">
                        <el-icon><Goods /></el-icon>
                        <span>商品</span>
                      </div>
                      <div
                        v-for="item in searchResult.products.slice(0, 3)"
                        :key="'p' + item.id"
                        class="search-item"
                        @click="goToProduct(item.id)"
                      >
                        <el-image
                          :src="item.mainImage"
                          :size="32"
                          fit="cover"
                          class="search-item-img"
                        >
                          <template #error>
                            <div class="image-placeholder">
                              <el-icon><Goods /></el-icon>
                            </div>
                          </template>
                        </el-image>
                        <div class="search-item-info">
                          <div class="search-item-title">
                            {{ item.name }}
                          </div>
                          <div class="search-item-sub">
                            ¥{{ item.minPrice }} · 销量 {{ item.saleCount }}
                          </div>
                        </div>
                      </div>
                    </div>
                    <div
                      v-if="searchResult.orders.length > 0"
                      class="search-group"
                    >
                      <div class="search-group-title">
                        <el-icon><Document /></el-icon>
                        <span>订单</span>
                      </div>
                      <div
                        v-for="item in searchResult.orders.slice(0, 3)"
                        :key="'o' + item.id"
                        class="search-item"
                        @click="goToOrder(item.orderNo)"
                      >
                        <div class="search-order-icon">
                          <el-icon><Document /></el-icon>
                        </div>
                        <div class="search-item-info">
                          <div class="search-item-title">
                            {{ item.orderNo }}
                          </div>
                          <div class="search-item-sub">
                            ¥{{ item.totalAmount }}
                          </div>
                        </div>
                      </div>
                    </div>
                  </template>
                </template>
                <div
                  v-else-if="!searchKeyword"
                  class="search-hint"
                >
                  <el-icon :size="32">
                    <Search />
                  </el-icon>
                  <p>输入关键词搜索用户、商品、订单</p>
                </div>
              </div>
            </div>
          </el-popover>
          <el-tooltip content="全屏">
            <el-button
              class="header-icon-btn"
              :icon="FullScreen"
              text
              circle
              @click="toggleFullscreen"
            />
          </el-tooltip>
          <el-dropdown
            trigger="click"
            placement="bottom"
          >
            <el-badge
              :value="unreadCount"
              :max="99"
              class="notification-badge"
              :hidden="unreadCount === 0"
            >
              <el-button
                class="header-icon-btn notification-btn"
                :icon="Bell"
                text
                circle
                @click="loadNotifications"
              />
            </el-badge>
            <template #dropdown>
              <el-dropdown-menu class="notification-dropdown">
                <div class="notification-header">
                  <span>消息通知</span>
                  <el-link
                    type="primary"
                    :underline="false"
                    @click.stop="handleMarkAllRead"
                  >
                    全部已读
                  </el-link>
                </div>
                <div class="notification-list">
                  <el-dropdown-item
                    v-for="item in notifications"
                    :key="item.id"
                    class="notification-item"
                    :class="{ 'is-read': item.isRead === 1 }"
                    @click.stop="handleNotificationClick(item)"
                  >
                    <div
                      class="notification-icon"
                      :class="getNotificationIconClass(item.level)"
                    >
                      <el-icon :size="18">
                        <component :is="getNotificationIcon(item.type)" />
                      </el-icon>
                    </div>
                    <div class="notification-content">
                      <div class="notification-title">
                        {{ item.title }}
                      </div>
                      <div class="notification-desc">
                        {{ item.content }}
                      </div>
                      <div class="notification-time">
                        {{ formatTime(item.createdAt) }}
                      </div>
                    </div>
                    <div
                      v-if="item.isRead === 0"
                      class="notification-dot"
                    />
                  </el-dropdown-item>
                  <div
                    v-if="notifications.length === 0"
                    class="notification-empty"
                  >
                    <el-empty
                      description="暂无通知"
                      :image-size="50"
                    />
                  </div>
                </div>
                <div class="notification-footer">
                  <el-link
                    type="primary"
                    :underline="false"
                  >
                    查看全部
                  </el-link>
                </div>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <div class="header-divider" />
          <el-dropdown trigger="click">
            <span class="avatar-wrapper">
              <el-avatar
                :size="34"
                icon="UserFilled"
                class="user-avatar"
              />
              <span class="admin-name">
                <span class="name-text">系统管理员</span>
                <span class="role-text">超级管理员</span>
              </span>
              <el-icon class="arrow-icon"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu class="user-dropdown">
                <div class="user-dropdown-header">
                  <el-avatar
                    :size="44"
                    icon="UserFilled"
                  />
                  <div class="user-info">
                    <div class="user-name">
                      系统管理员
                    </div>
                    <div class="user-role">
                      超级管理员
                    </div>
                  </div>
                </div>
                <el-dropdown-item
                  class="user-dropdown-item"
                  @click="goToProfile"
                >
                  <el-icon><User /></el-icon>
                  <span>个人中心</span>
                </el-dropdown-item>
                <el-dropdown-item
                  command="settings"
                  class="user-dropdown-item"
                >
                  <el-icon><Setting /></el-icon>
                  <span>账号设置</span>
                </el-dropdown-item>
                <el-dropdown-item
                  divided
                  class="user-dropdown-item logout-item"
                  @click="handleLogout"
                >
                  <el-icon><SwitchButton /></el-icon>
                  <span>退出登录</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  Monitor,
  DataAnalysis,
  UserFilled,
  Shop,
  Goods,
  Document,
  Picture,
  Lock,
  ChatDotRound,
  Van,
  Promotion,
  DataLine,
  Reading,
  Setting,
  Expand,
  Fold,
  ArrowDown,
  Bell,
  Search,
  FullScreen,
  User,
  SwitchButton,
  Money,
} from '@element-plus/icons-vue';
import { globalSearch, type SearchResult } from '@/api/search';
import {
  getUnreadCount,
  getNotifications,
  markAllNotificationsRead,
  markNotificationRead,
  type Notification,
} from '@/api/notification';

const route = useRoute();
const router = useRouter();
const isCollapsed = ref(false);
const expandedMenus = ref<Set<string>>(new Set());

const activeMenu = computed(() => route.path);
const currentTitle = computed(() => (route.meta.title as string) || '');

// 路由到父菜单的映射
const routeParentMap: Record<string, string> = {
  '/users': 'user', '/roles': 'user',
  '/merchant-audit': 'merchant', '/merchant-list': 'merchant',
  '/product-list': 'product', '/product-audit': 'product',
  '/orders': 'order', '/order-arbitration': 'order',
  '/patterns': 'pattern', '/pattern-audit': 'pattern',
  '/copyrights': 'copyright', '/infringement': 'copyright',
  '/purchase-orders': 'supply', '/suppliers': 'supply',
  '/coupons': 'marketing', '/campaigns': 'marketing',
  '/reports': 'analytics', '/statistics': 'analytics',
  '/courses': 'academy', '/instructors': 'academy',
  '/transactions': 'finance', '/revenue': 'finance', '/settlements': 'finance',
  '/refunds': 'finance', '/finance-reports': 'finance', '/fee-config': 'finance',
  '/audit-logs': 'system', '/settings': 'system',
};

function expandMenuByRoute(path: string) {
  const parent = routeParentMap[path];
  if (parent) {
    expandedMenus.value.add(parent);
  }
}

const notifications = ref<Notification[]>([]);
const unreadCount = ref(0);
let notificationTimer: ReturnType<typeof setInterval> | null = null;

// 悬停交互状态管理
const showDelay = 150; // 悬停显示延迟(ms)，不超过300ms
const hideDelay = 200; // 离开隐藏延迟(ms)
const showTimers = ref<Map<string, ReturnType<typeof setTimeout>>>(new Map());
const hideTimers = ref<Map<string, ReturnType<typeof setTimeout>>>(new Map());

// 所有一级菜单key集合
const allMenuKeys = ['user', 'merchant', 'product', 'order', 'pattern', 'copyright', 'community', 'supply', 'marketing', 'analytics', 'academy', 'finance', 'system'];

// 关闭其他菜单（保证同一时间只展开一个）
function closeOtherMenus(currentKey: string) {
  allMenuKeys.forEach(key => {
    if (key !== currentKey && expandedMenus.value.has(key)) {
      expandedMenus.value.delete(key);
    }
  });
}

function handleMenuOpen(index: string) {
  expandedMenus.value.add(index);
}

function handleMenuClose(index: string) {
  expandedMenus.value.delete(index);
}

// 一级菜单鼠标进入
function handleSubMenuMouseEnter(index: string) {
  // 清除该菜单的隐藏定时器
  const hideTimer = hideTimers.value.get(index);
  if (hideTimer) {
    clearTimeout(hideTimer);
    hideTimers.value.delete(index);
  }
  
  // 清除其他菜单的显示定时器
  allMenuKeys.forEach(key => {
    if (key !== index) {
      const timer = showTimers.value.get(key);
      if (timer) {
        clearTimeout(timer);
        showTimers.value.delete(key);
      }
    }
  });
  
  // 设置显示定时器
  const showTimer = setTimeout(() => {
    closeOtherMenus(index);
    expandedMenus.value.add(index);
    showTimers.value.delete(index);
  }, showDelay);
  showTimers.value.set(index, showTimer);
}

// 一级菜单鼠标离开
function handleSubMenuMouseLeave(index: string) {
  // 清除该菜单的显示定时器
  const showTimer = showTimers.value.get(index);
  if (showTimer) {
    clearTimeout(showTimer);
    showTimers.value.delete(index);
  }
  
  // 设置隐藏定时器
  const hideTimer = setTimeout(() => {
    expandedMenus.value.delete(index);
    hideTimers.value.delete(index);
  }, hideDelay);
  hideTimers.value.set(index, hideTimer);
}

// 二级菜单区域鼠标进入（防止从二级菜单移回一级菜单时消失）
function handleSubMenuContentMouseEnter(index: string) {
  const hideTimer = hideTimers.value.get(index);
  if (hideTimer) {
    clearTimeout(hideTimer);
    hideTimers.value.delete(index);
  }
}

// 二级菜单区域鼠标离开
function handleSubMenuContentMouseLeave(index: string) {
  const hideTimer = setTimeout(() => {
    expandedMenus.value.delete(index);
    hideTimers.value.delete(index);
  }, hideDelay);
  hideTimers.value.set(index, hideTimer);
}

// 触摸事件处理（移动端）
function handleSubMenuTouchStart(index: string) {
  if (expandedMenus.value.has(index)) {
    expandedMenus.value.delete(index);
  } else {
    closeOtherMenus(index);
    expandedMenus.value.add(index);
  }
}

// 键盘事件处理（Enter/Space键切换菜单）
function handleSubMenuKeydown(index: string, event: KeyboardEvent) {
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault();
    if (expandedMenus.value.has(index)) {
      expandedMenus.value.delete(index);
    } else {
      closeOtherMenus(index);
      expandedMenus.value.add(index);
    }
  }
}

async function loadNotifications() {
  try {
    const data = await getNotifications({ current: 1, size: 5 });
    if (data && data.records) {
      notifications.value = data.records;
    }
  } catch {
    notifications.value = [];
  }
}

async function loadUnreadCount() {
  try {
    const count = await getUnreadCount();
    unreadCount.value = count || 0;
  } catch {
    unreadCount.value = 0;
  }
}

async function handleMarkAllRead() {
  try {
    await markAllNotificationsRead();
    unreadCount.value = 0;
    notifications.value.forEach((n) => {
      n.isRead = 1;
    });
    ElMessage.success('已全部标记为已读');
  } catch {
    ElMessage.error('操作失败');
  }
}

function handleNotificationClick(item: Notification) {
  if (item.isRead === 0) {
    markNotificationRead(item.id).catch(() => {});
    item.isRead = 1;
    if (unreadCount.value > 0) {
      unreadCount.value--;
    }
  }
}

function getNotificationIcon(type: string) {
  const map: Record<string, any> = {
    system: Setting,
    order: Document,
    product: Goods,
    merchant: Shop,
  };
  return map[type] || Bell;
}

function getNotificationIconClass(level: string) {
  const map: Record<string, string> = {
    info: 'notification-icon-info',
    warning: 'notification-icon-warning',
    error: 'notification-icon-error',
    success: 'notification-icon-success',
  };
  return map[level] || 'notification-icon-info';
}

function formatTime(time: string): string {
  if (!time) return '';
  const date = new Date(time);
  const now = new Date();
  const diff = now.getTime() - date.getTime();
  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);
  if (minutes < 1) return '刚刚';
  if (minutes < 60) return `${minutes}分钟前`;
  if (hours < 24) return `${hours}小时前`;
  if (days < 7) return `${days}天前`;
  return time.slice(5, 16);
}

function startNotificationPolling() {
  loadUnreadCount();
  loadNotifications();
  notificationTimer = setInterval(() => {
    loadUnreadCount();
  }, 60000);
}

function stopNotificationPolling() {
  if (notificationTimer) {
    clearInterval(notificationTimer);
    notificationTimer = null;
  }
}

const searchVisible = ref(false);
const searchKeyword = ref('');
const searchLoading = ref(false);
const searchResult = ref<SearchResult>({
  users: [],
  products: [],
  orders: [],
  total: 0,
});

let searchTimer: ReturnType<typeof setTimeout> | null = null;

function handleSearchVisible() {
  setTimeout(() => {
    const input = document.querySelector('.search-input input') as HTMLInputElement;
    if (input) input.focus();
  }, 100);
}

function handleSearchInput() {
  if (searchTimer) clearTimeout(searchTimer);
  const keyword = searchKeyword.value.trim();
  if (!keyword) {
    searchResult.value = { users: [], products: [], orders: [], total: 0 };
    return;
  }
  searchTimer = setTimeout(() => {
    doSearch(keyword);
  }, 300);
}

function handleSearchClear() {
  searchResult.value = { users: [], products: [], orders: [], total: 0 };
}

async function doSearch(keyword: string) {
  searchLoading.value = true;
  try {
    const data = await globalSearch(keyword);
    if (data) {
      searchResult.value = data;
    }
  } catch {
    searchResult.value = { users: [], products: [], orders: [], total: 0 };
  } finally {
    searchLoading.value = false;
  }
}

function goToUser(_id: number) {
  searchVisible.value = false;
  router.push('/users');
}

function goToProduct(_id: number) {
  searchVisible.value = false;
  router.push('/product-list');
}

function goToOrder(_orderNo: string) {
  searchVisible.value = false;
  router.push('/orders');
}

function goToProfile() {
  router.push('/profile');
}

function handleLogout() {
  localStorage.removeItem('token');
  localStorage.removeItem('userInfo');
  ElMessage.success('已退出登录');
  router.push('/login');
}

function toggleSidebar() {
  isCollapsed.value = !isCollapsed.value;
}

function toggleFullscreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen().catch(() => {
      ElMessage.warning('全屏模式不可用');
    });
  } else {
    document.exitFullscreen();
  }
}

onMounted(() => {
  startNotificationPolling();
  expandMenuByRoute(route.path);
});

watch(
  () => route.path,
  (newPath) => {
    expandMenuByRoute(newPath);
  }
);

onUnmounted(() => {
  stopNotificationPolling();
  
  // 清理所有显示/隐藏定时器
  showTimers.value.forEach((timer) => {
    clearTimeout(timer);
  });
  showTimers.value.clear();
  
  hideTimers.value.forEach((timer) => {
    clearTimeout(timer);
  });
  hideTimers.value.clear();
  
  // 清理搜索定时器
  if (searchTimer) {
    clearTimeout(searchTimer);
    searchTimer = null;
  }
});
</script>

<style scoped>
.app-layout {
  height: 100vh;
}

.app-sidebar {
  background-color: #1f2937;
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  overflow: hidden;
}

.sidebar-logo {
  height: var(--header-height);
  min-height: var(--header-height);
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding: 0 18px;
  gap: 10px;
  color: #ffffff;
  border-bottom: 1px solid #374151;
  flex-shrink: 0;
}

.logo-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.logo-text {
  font-size: 16px;
  font-weight: 700;
  white-space: nowrap;
  background: linear-gradient(135deg, #fff, #d1d5db);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.sidebar-scroll {
  flex: 1;
  overflow: hidden;
}

.sidebar-scroll :deep(.el-scrollbar__wrap) {
  overflow-x: hidden !important;
}

.menu-group-title {
  padding: 16px 20px 8px;
}

.menu-group-title span {
  font-size: 11px;
  font-weight: 600;
  color: #4b5563;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.sidebar-menu {
  border-right: none;
  width: 100% !important;
}

.sidebar-menu:not(.el-menu--collapse) {
  width: 220px;
}

.sidebar-menu :deep(.el-menu-item) {
  height: 44px;
  line-height: 44px;
  margin: 2px 8px;
  border-radius: 6px;
  width: calc(100% - 16px);
  transition: all 0.2s ease;
}

.sidebar-menu :deep(.el-menu-item:hover) {
  background-color: #374151;
  color: #ffffff;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #ffffff !important;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.3);
}

.sidebar-menu :deep(.el-sub-menu__title) {
  height: 44px;
  line-height: 44px;
  margin: 2px 8px;
  border-radius: 6px;
  width: calc(100% - 16px);
  transition: all 0.2s ease;
}

.sidebar-menu :deep(.el-sub-menu__title:hover) {
  background-color: #374151;
  color: #ffffff;
}

/* 菜单标题包装器样式 */
.menu-title-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  height: 100%;
  cursor: pointer;
  outline: none;
  -webkit-tap-highlight-color: transparent;
}

.menu-title-wrapper:focus-visible {
  outline: 2px solid #6366f1;
  outline-offset: 2px;
  border-radius: 6px;
}

@media (hover: none) {
  .menu-title-wrapper {
    min-height: 44px;
  }
}

.sidebar-menu :deep(.el-sub-menu .el-menu-item) {
  min-width: 200px;
  padding-left: 48px !important;
  height: 40px;
  line-height: 40px;
  font-size: 13px;
  margin: 1px 8px;
  width: calc(100% - 16px);
  border-radius: 6px;
}

.sidebar-menu :deep(.el-sub-menu .el-menu-item.is-active) {
  background: rgba(99, 102, 241, 0.15);
  color: #818cf8 !important;
  font-weight: 500;
  box-shadow: none;
}

/* 二级菜单过渡动画 */
.sub-menu-content {
  overflow: hidden;
  max-height: 0;
  opacity: 0;
  transform: translateY(-8px);
  transition: all 0.25s ease-out;
  will-change: max-height, opacity, transform;
}

.sub-menu-content-show {
  max-height: 500px;
  opacity: 1;
  transform: translateY(0);
  transition: all 0.25s ease-in;
}

.sidebar-footer {
  padding: 12px;
  border-top: 1px solid #374151;
  display: flex;
  justify-content: center;
  flex-shrink: 0;
  margin-top: auto;
}

.collapse-btn {
  color: #9ca3af;
  width: 36px;
  height: 36px;
}

.collapse-btn:hover {
  color: #ffffff;
  background-color: #374151;
}

.app-header {
  background-color: var(--color-white);
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: var(--header-height);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-breadcrumb {
  font-size: 14px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-icon-btn {
  width: 38px;
  height: 38px;
  color: #6b7280;
  transition: all 0.2s ease;
}

.header-icon-btn:hover {
  color: #6366f1;
  background-color: #f3f4f6;
}

.notification-badge {
  display: inline-flex;
  align-items: center;
}

.header-divider {
  width: 1px;
  height: 24px;
  background-color: #e5e7eb;
  margin: 0 8px;
}

.avatar-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background-color 0.2s ease;
}

.avatar-wrapper:hover {
  background-color: #f9fafb;
}

.user-avatar {
  border: 2px solid #e5e7eb;
}

.admin-name {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.name-text {
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
}

.role-text {
  font-size: 11px;
  color: #9ca3af;
  margin-top: 2px;
}

.arrow-icon {
  color: #9ca3af;
  font-size: 12px;
  margin-left: 4px;
}

.app-main {
  background-color: var(--color-background);
  padding: 20px;
  overflow-y: auto;
}

.notification-dropdown {
  width: 340px;
  padding: 0;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  border-bottom: 1px solid #e5e7eb;
  font-weight: 600;
  font-size: 14px;
  color: #1f2937;
}

.notification-list {
  max-height: 360px;
  overflow-y: auto;
}

.notification-item {
  padding: 12px 16px;
  display: flex;
  gap: 12px;
  align-items: flex-start;
  border-bottom: 1px solid #f3f4f6;
  position: relative;
}

.notification-item.is-read {
  opacity: 0.7;
}

.notification-item:last-child {
  border-bottom: none;
}

.notification-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: #eff6ff;
  color: #3b82f6;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.notification-icon-info {
  background: #eff6ff;
  color: #3b82f6;
}

.notification-icon-warning {
  background: #fef3c7;
  color: #f59e0b;
}

.notification-icon-error {
  background: #fee2e2;
  color: #ef4444;
}

.notification-icon-success {
  background: #d1fae5;
  color: #10b981;
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-title {
  font-size: 13px;
  color: #1f2937;
  line-height: 1.4;
  margin-bottom: 4px;
  font-weight: 500;
}

.notification-desc {
  font-size: 12px;
  color: #6b7280;
  line-height: 1.5;
  margin-bottom: 6px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notification-time {
  font-size: 11px;
  color: #9ca3af;
}

.notification-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ef4444;
  flex-shrink: 0;
  margin-top: 4px;
}

.notification-empty {
  padding: 20px;
}

.notification-footer {
  padding: 10px 16px;
  text-align: center;
  border-top: 1px solid #e5e7eb;
  font-size: 13px;
}

.user-dropdown {
  width: 240px;
  padding: 0;
}

.user-dropdown-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid #e5e7eb;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
}

.user-role {
  font-size: 12px;
  color: #6b7280;
}

.user-dropdown-item {
  padding: 10px 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: #374151;
}

.user-dropdown-item .el-icon {
  font-size: 16px;
  color: #6b7280;
}

.logout-item {
  color: #ef4444;
}

.logout-item .el-icon {
  color: #ef4444;
}

.search-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.search-input {
  width: 100%;
}

.search-results {
  max-height: 360px;
  overflow-y: auto;
}

.search-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 30px 0;
  color: #9ca3af;
  gap: 8px;
}

.search-hint p {
  margin: 0;
  font-size: 13px;
}

.search-empty {
  padding: 20px 0;
}

.search-group {
  margin-bottom: 12px;
}

.search-group:last-child {
  margin-bottom: 0;
}

.search-group-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #6b7280;
  font-weight: 600;
  padding: 4px 4px 8px;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.search-group-title .el-icon {
  font-size: 14px;
}

.search-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.search-item:hover {
  background-color: #f3f4f6;
}

.search-item-img {
  width: 32px;
  height: 32px;
  border-radius: 4px;
  overflow: hidden;
  flex-shrink: 0;
}

.search-order-icon {
  width: 32px;
  height: 32px;
  border-radius: 4px;
  background: #eff6ff;
  color: #3b82f6;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  color: #9ca3af;
}

.image-placeholder .el-icon {
  font-size: 16px;
}

.search-item-info {
  flex: 1;
  min-width: 0;
}

.search-item-title {
  font-size: 13px;
  color: #1f2937;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.search-item-sub {
  font-size: 11px;
  color: #9ca3af;
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
