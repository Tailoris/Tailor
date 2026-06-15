import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw, NavigationGuardNext, RouteLocationNormalized } from 'vue-router'
import { decryptSync } from '@/utils/crypto'

// 扩展路由meta类型
declare module 'vue-router' {
  interface RouteMeta {
    /** 是否需要登录 */
    requiresAuth?: boolean
    /** 需要的角色列表（任一即可） */
    roles?: string[]
    /** 需要的权限列表（全部满足） */
    permissions?: string[]
    /** 页面标题 */
    title?: string
    /** 是否缓存 */
    keepAlive?: boolean
  }
}

const routes: RouteRecordRaw[] = [
  { path: '/', name: 'Home', component: () => import('@/views/HomeView.vue'),
    meta: { title: '首页' } },
  { path: '/products', name: 'Products', component: () => import('@/views/ProductListView.vue'),
    meta: { title: '商品列表' } },
  { path: '/product/:id', name: 'ProductDetail', component: () => import('@/views/ProductDetailView.vue'),
    props: true, meta: { title: '商品详情' } },
  { path: '/cart', name: 'Cart', component: () => import('@/views/CartView.vue'),
    meta: { requiresAuth: true, title: '购物车' } },
  { path: '/checkout', name: 'Checkout', component: () => import('@/views/CheckoutView.vue'),
    meta: { requiresAuth: true, title: '结算' } },
  { path: '/orders', name: 'Orders', component: () => import('@/views/OrderListView.vue'),
    meta: { requiresAuth: true, title: '我的订单' } },
  { path: '/order/:id', name: 'OrderDetail', component: () => import('@/views/OrderDetailView.vue'),
    props: true, meta: { requiresAuth: true, title: '订单详情' } },
  { path: '/profile', name: 'Profile', component: () => import('@/views/ProfileView.vue'),
    meta: { requiresAuth: true, title: '个人中心' } },
  { path: '/community', name: 'Community', component: () => import('@/views/CommunityView.vue'),
    meta: { title: '社区' } },
  { path: '/community/:postId', name: 'PostDetail', component: () => import('@/views/CommunityView.vue'),
    props: true, meta: { title: '帖子详情' } },
  { path: '/merchant-apply', name: 'MerchantApply', component: () => import('@/views/MerchantApplyView.vue'),
    meta: { requiresAuth: true, title: '商家入驻' } },
  { path: '/login', name: 'Login', component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录' } },
  { path: '/register', name: 'Register', component: () => import('@/views/RegisterView.vue'),
    meta: { title: '注册' } },
  { path: '/forgot-password', name: 'ForgotPassword', component: () => import('@/views/ForgotPasswordView.vue'),
    meta: { title: '找回密码' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

/**
 * 🔒 F-H14: 增强版导航守卫
 *
 * <p>支持：</p>
 * <ul>
 *   <li>登录拦截（meta.requiresAuth）</li>
 *   <li>角色检查（meta.roles）- 不再信任本地数据，仅做 token 存在性检查，由后端处理授权</li>
 *   <li>权限检查（meta.permissions）- 同上，由后端处理授权</li>
 *   <li>页面标题设置</li>
 *   <li>已登录访问登录页自动跳转</li>
 * </ul>
 *
 * 🔒 C-012修复: 前端不再依赖 localStorage 中的 roles/permissions 做授权决策，
 * 仅检查 token 是否存在（即已认证）。角色和权限检查交由后端 API 处理，
 * 前端仅作为体验优化（如隐藏无权限的 UI 元素）。
 */
router.beforeEach((
  to: RouteLocationNormalized,
  _from: RouteLocationNormalized,
  next: NavigationGuardNext
) => {
  // 1. 设置页面标题
  const pageTitle = to.meta.title as string | undefined
  if (pageTitle) {
    document.title = `${pageTitle} - Tailor IS`
  } else {
    document.title = 'Tailor IS - 服装全产业平台'
  }

  const token = decryptSync(localStorage.getItem('token') || '')
  const isAuthenticated = !!token

  // 2. 已登录用户访问登录/注册页 - 跳转首页
  if (isAuthenticated && (to.name === 'Login' || to.name === 'Register')) {
    next({ path: '/' })
    return
  }

  // 3. 检查是否需要登录（仅检查 token 存在性，不依赖本地角色数据）
  if (to.meta.requiresAuth && !isAuthenticated) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  // 🔒 C-012: 不再在前端检查 roles/permissions（localStorage 数据不可信）
  // 后端 API 层会根据 token 验证用户角色和权限，返回 403 时前端显示错误提示即可
  // 如果需要在路由层做强校验，应调用服务端 API 验证：
  //   if (to.meta.roles) { await verifyUserRoles(to.meta.roles) }
  // 但为保持性能和简单性，此处仅做认证检查。

  // 6. 通过所有检查
  next()
})

export default router
