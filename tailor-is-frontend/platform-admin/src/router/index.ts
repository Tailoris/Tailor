import { createRouter, createWebHistory } from 'vue-router';
import type { RouteRecordRaw } from 'vue-router';

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean;
    title?: string;
    permission?: string;
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录', requiresAuth: false },
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
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: { title: '仪表盘' },
      },
      {
        path: 'users',
        name: 'UserManagement',
        component: () => import('@/views/system/UserListView.vue'),
        meta: { title: '用户管理', requiresAuth: true },
      },
      {
        path: 'roles',
        name: 'RoleManagement',
        component: () => import('@/views/system/RoleListView.vue'),
        meta: { title: '角色管理', requiresAuth: true },
      },
      {
        path: 'audit-logs',
        name: 'AuditLog',
        component: () => import('@/views/system/AuditLogView.vue'),
        meta: { title: '操作日志', requiresAuth: true },
      },
      {
        path: 'settings',
        name: 'SystemSettings',
        component: () => import('@/views/system/SettingsView.vue'),
        meta: { title: '系统设置', requiresAuth: true },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/system/ProfileView.vue'),
        meta: { title: '个人中心', requiresAuth: true },
      },
      {
        path: 'merchant-audit',
        name: 'MerchantAudit',
        component: () => import('@/views/merchant/MerchantAuditView.vue'),
        meta: { title: '商家审核', requiresAuth: true },
      },
      {
        path: 'merchant-list',
        name: 'MerchantList',
        component: () => import('@/views/merchant/MerchantListView.vue'),
        meta: { title: '商家列表', requiresAuth: true },
      },
      {
        path: 'product-list',
        name: 'ProductList',
        component: () => import('@/views/product/ProductListView.vue'),
        meta: { title: '商品列表', requiresAuth: true },
      },
      {
        path: 'product-audit',
        name: 'ProductAudit',
        component: () => import('@/views/product/ProductAuditView.vue'),
        meta: { title: '商品审核', requiresAuth: true },
      },
      {
        path: 'orders',
        name: 'OrderList',
        component: () => import('@/views/order/OrderListView.vue'),
        meta: { title: '订单列表', requiresAuth: true },
      },
      {
        path: 'order-arbitration',
        name: 'OrderArbitration',
        component: () => import('@/views/order/OrderArbitrationView.vue'),
        meta: { title: '售后仲裁', requiresAuth: true },
      },
      {
        path: 'patterns',
        name: 'PatternList',
        component: () => import('@/views/pattern/PatternListView.vue'),
        meta: { title: '纸样管理', requiresAuth: true },
      },
      {
        path: 'pattern-audit',
        name: 'PatternAudit',
        component: () => import('@/views/pattern/PatternAuditView.vue'),
        meta: { title: '纸样审核', requiresAuth: true },
      },
      {
        path: 'copyrights',
        name: 'CopyrightList',
        component: () => import('@/views/copyright/CopyrightListView.vue'),
        meta: { title: '版权登记', requiresAuth: true },
      },
      {
        path: 'infringement',
        name: 'Infringement',
        component: () => import('@/views/copyright/InfringementView.vue'),
        meta: { title: '侵权检测', requiresAuth: true },
      },
      {
        path: 'community-posts',
        name: 'CommunityPosts',
        component: () => import('@/views/community/CommunityPostsView.vue'),
        meta: { title: '帖子审核', requiresAuth: true },
      },
      {
        path: 'community-reports',
        name: 'CommunityReports',
        component: () => import('@/views/community/CommunityReportsView.vue'),
        meta: { title: '举报处理', requiresAuth: true },
      },
      {
        path: 'fabrics',
        name: 'FabricList',
        component: () => import('@/views/supply/FabricListView.vue'),
        meta: { title: '面料管理', requiresAuth: true },
      },
      {
        path: 'suppliers',
        name: 'SupplierList',
        component: () => import('@/views/supply/SupplierListView.vue'),
        meta: { title: '供应商管理', requiresAuth: true },
      },
      {
        path: 'purchase-orders',
        name: 'PurchaseOrders',
        component: () => import('@/views/supply/PurchaseOrderView.vue'),
        meta: { title: '采购订单', requiresAuth: true },
      },
      {
        path: 'coupons',
        name: 'CouponList',
        component: () => import('@/views/marketing/CouponListView.vue'),
        meta: { title: '优惠券管理', requiresAuth: true },
      },
      {
        path: 'campaigns',
        name: 'CampaignList',
        component: () => import('@/views/marketing/CampaignListView.vue'),
        meta: { title: '活动管理', requiresAuth: true },
      },
      {
        path: 'reports',
        name: 'Reports',
        component: () => import('@/views/analytics/ReportsView.vue'),
        meta: { title: '数据报告', requiresAuth: true },
      },
      {
        path: 'statistics',
        name: 'Statistics',
        component: () => import('@/views/analytics/StatisticsView.vue'),
        meta: { title: '数据统计', requiresAuth: true },
      },
      {
        path: 'courses',
        name: 'CourseList',
        component: () => import('@/views/academy/CourseListView.vue'),
        meta: { title: '课程管理', requiresAuth: true },
      },
      {
        path: 'instructors',
        name: 'InstructorList',
        component: () => import('@/views/academy/InstructorListView.vue'),
        meta: { title: '讲师管理', requiresAuth: true },
      },
      {
        path: 'transactions',
        name: 'TransactionList',
        component: () => import('@/views/finance/TransactionListView.vue'),
        meta: { title: '交易流水', requiresAuth: true, permission: 'finance:transaction:view' },
      },
      {
        path: 'revenue',
        name: 'RevenueStats',
        component: () => import('@/views/finance/RevenueView.vue'),
        meta: { title: '收入统计', requiresAuth: true, permission: 'finance:revenue:view' },
      },
      {
        path: 'settlements',
        name: 'SettlementList',
        component: () => import('@/views/finance/SettlementView.vue'),
        meta: { title: '结算管理', requiresAuth: true, permission: 'finance:settlement:view' },
      },
      {
        path: 'refunds',
        name: 'RefundList',
        component: () => import('@/views/finance/RefundView.vue'),
        meta: { title: '退款管理', requiresAuth: true, permission: 'finance:refund:view' },
      },
      {
        path: 'finance-reports',
        name: 'FinanceReports',
        component: () => import('@/views/finance/FinanceReportView.vue'),
        meta: { title: '财务报表', requiresAuth: true, permission: 'finance:report:view' },
      },
      {
        path: 'fee-config',
        name: 'FeeConfig',
        component: () => import('@/views/finance/FeeConfigView.vue'),
        meta: { title: '费用配置', requiresAuth: true, permission: 'finance:config:view' },
      },
    ],
  },
];

const router = createRouter({
  history: createWebHistory('/admin'),
  routes,
});

router.beforeEach((to, _from, next) => {
  document.title = `${to.meta.title || '裁智云平台管理'} - Tailor IS`;

  const token = localStorage.getItem('token');
  const isAuthenticated = !!token;

  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth !== false);

  if (requiresAuth && !isAuthenticated) {
    next({ path: '/login', query: { redirect: to.fullPath } });
    return;
  }

  if (isAuthenticated && to.path === '/login') {
    next({ path: '/dashboard' });
    return;
  }

  const requiredPermission = to.meta.permission;
  if (isAuthenticated && requiredPermission) {
    const userPermissions = JSON.parse(localStorage.getItem('permissions') || '[]');
    if (!userPermissions.includes(requiredPermission)) {
      next('/dashboard');
      return;
    }
  }

  next();
});

export default router;
