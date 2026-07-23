<template>
  <nav
    class="breadcrumb-nav"
    role="navigation"
    aria-label="面包屑导航"
  >
    <router-link
      to="/"
      class="breadcrumb-item home-item"
      aria-label="首页"
    >
      {{ t('nav.home') }}
    </router-link>
    <template v-for="(item, index) in breadcrumbItems.slice(1)" :key="index">
      <span class="separator">/</span>
      <router-link
        v-if="index < breadcrumbItems.length - 2"
        :to="item.path"
        class="breadcrumb-item"
        :aria-label="item.label"
      >
        {{ item.label }}
      </router-link>
      <span v-else class="breadcrumb-item current">
        {{ item.label }}
      </span>
    </template>
  </nav>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';


const { t } = useI18n();
const route = useRoute();

interface BreadcrumbItem {
  label: string;
  path: string;
}

const breadcrumbItems = computed<BreadcrumbItem[]>(() => {
  const items: BreadcrumbItem[] = [{ label: t('nav.home'), path: '/' }];
  
  const routeMap: Record<string, string> = {
    '/patterns': '纸样商城',
    '/pattern-custom': '纸样定制',
    '/design': '服装设计',
    '/community': t('nav.community'),
    '/knowledge': '知识库',
    '/academy': '线上学堂',
    '/products': '商品列表',
    '/cart': t('nav.cart'),
    '/checkout': '确认订单',
    '/orders': t('nav.orders'),
    '/profile': t('nav.profile'),
    '/login': t('nav.login'),
    '/register': t('nav.register'),
    '/merchant-apply': '商家入驻',
    '/about': '关于我们',
    '/brand-story': '品牌故事',
    '/join-us': '加入我们',
    '/news': '新闻动态',
    '/shopping-guide': '购物指南',
    '/payment': '支付方式',
    '/delivery': '配送说明',
    '/after-sale': '售后服务',
    '/contact': '联系我们',
    '/merchant-rules': '商家规则',
    '/merchant-backend': '商家后台',
    '/merchant-training': '商家培训',
  };

  const currentPath = route.path;
  const pathSegments = currentPath.split('/').filter(Boolean);
  
  let currentPathAccum = '';
  pathSegments.forEach((segment) => {
    currentPathAccum += `/${segment}`;
    if (routeMap[currentPathAccum]) {
      items.push({ label: routeMap[currentPathAccum], path: currentPathAccum });
    }
  });

  return items;
});
</script>

<style scoped>
.breadcrumb-nav {
  max-width: 1200px;
  margin: 0 auto;
  padding: 12px 20px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #666;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
}

.breadcrumb-item {
  color: #666;
  text-decoration: none;
  display: flex;
  align-items: center;
}

.breadcrumb-item:hover {
  color: #1d39c4;
}

.home-item {
  color: #1d39c4;
}

.breadcrumb-item.current {
  color: #333;
  font-weight: 500;
}

.separator {
  color: #d9d9d9;
  font-size: 12px;
}

@media (max-width: 768px) {
  .breadcrumb-nav {
    display: none;
  }
}
</style>