import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/RegisterView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('@/views/ForgotPasswordView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/',
    component: () => import('@/components/AdminLayout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: { title: '仪表盘', breadcrumb: ['仪表盘'] },
      },
      {
        path: 'product',
        name: 'ProductList',
        component: () => import('@/views/ProductListView.vue'),
        meta: { title: '商品管理', breadcrumb: ['商品管理', '商品列表'] },
      },
      {
        path: 'product/create',
        name: 'ProductCreate',
        component: () => import('@/views/ProductFormView.vue'),
        meta: { title: '创建商品', breadcrumb: ['商品管理', '创建商品'] },
      },
      {
        path: 'product/edit/:id',
        name: 'ProductEdit',
        component: () => import('@/views/ProductFormView.vue'),
        meta: { title: '编辑商品', breadcrumb: ['商品管理', '编辑商品'] },
      },
      {
        path: 'order',
        name: 'OrderList',
        component: () => import('@/views/OrderListView.vue'),
        meta: { title: '订单管理', breadcrumb: ['订单管理', '订单列表'] },
      },
      {
        path: 'order/:orderNo',
        name: 'OrderDetail',
        component: () => import('@/views/OrderDetailView.vue'),
        meta: { title: '订单详情', breadcrumb: ['订单管理', '订单详情'] },
      },
      {
        path: 'aftersale',
        name: 'AfterSaleList',
        component: () => import('@/views/AfterSaleListView.vue'),
        meta: { title: '售后工单', breadcrumb: ['售后工单', '工单列表'] },
      },
      {
        path: 'aftersale/:ticketNo',
        name: 'AfterSaleDetail',
        component: () => import('@/views/AfterSaleDetailView.vue'),
        meta: { title: '工单详情', breadcrumb: ['售后工单', '工单详情'] },
      },
      {
        path: 'shop/employees',
        name: 'EmployeeList',
        component: () => import('@/views/EmployeeListView.vue'),
        meta: { title: '员工管理', breadcrumb: ['店铺管理', '员工管理'] },
      },
      {
        path: 'shop/settings',
        name: 'ShopSettings',
        component: () => import('@/views/ShopSettingsView.vue'),
        meta: { title: '店铺设置', breadcrumb: ['店铺管理', '店铺设置'] },
      },
      {
        path: 'finance/settlement',
        name: 'FinanceSettlement',
        component: () => import('@/views/FinanceSettlementView.vue'),
        meta: { title: '财务结算', breadcrumb: ['财务结算'] },
      },
      {
        path: 'finance/withdraw',
        name: 'FinanceWithdraw',
        component: () => import('@/views/finance/FinanceWithdrawView.vue'),
        meta: { title: '提现管理', breadcrumb: ['财务结算', '提现管理'] },
      },
      {
        path: 'marketing/coupon',
        name: 'CouponList',
        component: () => import('@/views/CouponListView.vue'),
        meta: { title: '优惠券', breadcrumb: ['营销中心', '优惠券'] },
      },
      {
        path: 'marketing/seckill',
        name: 'SeckillList',
        component: () => import('@/views/marketing/SeckillListView.vue'),
        meta: { title: '秒杀活动', breadcrumb: ['营销中心', '秒杀活动'] },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth !== false)

  if (requiresAuth && !userStore.isLoggedIn) {
    next('/login')
  } else if (to.path === '/login' && userStore.isLoggedIn) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
