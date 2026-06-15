import { createSSRApp } from 'vue'
import { createPinia } from 'pinia'
import { renderToString } from 'vue/server-renderer'
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { createRouter, createMemoryHistory } from 'vue-router'
import i18n from '../i18n'
import App from '../App.vue'
import type { RouteRecordRaw } from 'vue-router'

// Lazy-load all route components (same routes as client router)
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/HomeView.vue'),
    meta: { title: '首页' }
  },
  {
    path: '/products',
    name: 'Products',
    component: () => import('../views/ProductListView.vue'),
    meta: { title: '商品列表' }
  },
  {
    path: '/product/:id',
    name: 'ProductDetail',
    component: () => import('../views/ProductDetailView.vue'),
    props: true,
    meta: { title: '商品详情' }
  },
  {
    path: '/cart',
    name: 'Cart',
    component: () => import('../views/CartView.vue'),
    meta: { requiresAuth: true, title: '购物车' }
  },
  {
    path: '/checkout',
    name: 'Checkout',
    component: () => import('../views/CheckoutView.vue'),
    meta: { requiresAuth: true, title: '结算' }
  },
  {
    path: '/orders',
    name: 'Orders',
    component: () => import('../views/OrderListView.vue'),
    meta: { requiresAuth: true, title: '我的订单' }
  },
  {
    path: '/order/:id',
    name: 'OrderDetail',
    component: () => import('../views/OrderDetailView.vue'),
    props: true,
    meta: { requiresAuth: true, title: '订单详情' }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/ProfileView.vue'),
    meta: { requiresAuth: true, title: '个人中心' }
  },
  {
    path: '/community',
    name: 'Community',
    component: () => import('../views/CommunityView.vue'),
    meta: { title: '社区' }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/LoginView.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/RegisterView.vue'),
    meta: { title: '注册' }
  }
]

export async function createApp(url: string) {
  const app = createSSRApp(App)
  const pinia = createPinia()

  const router = createRouter({
    history: createMemoryHistory(),
    routes
  })

  // Register ElementPlus icons
  for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
  }

  app.use(pinia)
  app.use(router)
  app.use(ElementPlus)
  app.use(i18n)

  await router.push(url)
  await router.isReady()

  const ctx: Record<string, unknown> = {}
  const html = await renderToString(app, ctx)

  return { html, pinia }
}
