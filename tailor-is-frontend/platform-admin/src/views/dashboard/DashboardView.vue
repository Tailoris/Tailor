<template>
  <div
    v-loading="loading"
    class="dashboard-page"
  >
    <!-- 页头 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">
          系统概览
        </h2>
        <p class="page-subtitle">
          裁智云平台整体运营数据概览 · {{ todayStr }}
        </p>
      </div>
      <div class="header-right">
        <el-button
          :icon="Refresh"
          :loading="loading"
          size="default"
          @click="loadAll"
        >
          刷新
        </el-button>
      </div>
    </div>

    <!-- 第一行：6 个指标卡 -->
    <el-row
      :gutter="20"
      class="stat-row"
    >
      <el-col
        :xs="24"
        :sm="12"
        :md="8"
        :lg="4"
      >
        <el-card
          shadow="hover"
          class="stat-card"
        >
          <StatCard
            label="总用户数"
            :value="stats.userCount"
            icon-color="#6366f1"
            icon-bg="rgba(99, 102, 241, 0.1)"
            :trend="stats.userGrowth"
            :trend-type="growthType(stats.userGrowth)"
            :sparkline="userSparkline"
            :formatter="formatNumber"
          >
            <template #icon>
              <el-icon :size="22">
                <UserFilled />
              </el-icon>
            </template>
          </StatCard>
        </el-card>
      </el-col>
      <el-col
        :xs="24"
        :sm="12"
        :md="8"
        :lg="4"
      >
        <el-card
          shadow="hover"
          class="stat-card"
        >
          <StatCard
            label="商家总数"
            :value="stats.merchantCount"
            icon-color="#10b981"
            icon-bg="rgba(16, 185, 129, 0.1)"
            :trend-label="`今日新增 ${stats.todayNewUsers || 0}`"
            :formatter="formatNumber"
          >
            <template #icon>
              <el-icon :size="22">
                <Briefcase />
              </el-icon>
            </template>
          </StatCard>
        </el-card>
      </el-col>
      <el-col
        :xs="24"
        :sm="12"
        :md="8"
        :lg="4"
      >
        <el-card
          shadow="hover"
          class="stat-card"
        >
          <StatCard
            label="总订单数"
            :value="stats.orderCount"
            icon-color="#8b5cf6"
            icon-bg="rgba(139, 92, 246, 0.1)"
            :trend="stats.ordersGrowth"
            :trend-type="growthType(stats.ordersGrowth)"
            :sparkline="orderSparkline"
            :formatter="formatNumber"
          >
            <template #icon>
              <el-icon :size="22">
                <Document />
              </el-icon>
            </template>
          </StatCard>
        </el-card>
      </el-col>
      <el-col
        :xs="24"
        :sm="12"
        :md="8"
        :lg="4"
      >
        <el-card
          shadow="hover"
          class="stat-card"
        >
          <StatCard
            label="今日营收"
            :value="stats.todayRevenue"
            icon-color="#f59e0b"
            icon-bg="rgba(245, 158, 11, 0.1)"
            :trend="stats.revenueGrowth"
            :trend-type="growthType(stats.revenueGrowth)"
            :sparkline="revenueSparkline"
            :formatter="formatMoney"
          >
            <template #icon>
              <el-icon :size="22">
                <Wallet />
              </el-icon>
            </template>
          </StatCard>
        </el-card>
      </el-col>
      <el-col
        :xs="24"
        :sm="12"
        :md="8"
        :lg="4"
      >
        <el-card
          shadow="hover"
          class="stat-card stat-card-clickable"
          @click="router.push('/merchant-audit')"
        >
          <StatCard
            label="待审核"
            :value="stats.pendingAuditCount"
            icon-color="#ef4444"
            icon-bg="rgba(239, 68, 68, 0.1)"
            :trend-label="`商家${stats.pendingMerchantCount || 0} · 商品${stats.pendingProductCount || 0}`"
            :formatter="formatNumber"
          >
            <template #icon>
              <el-icon :size="22">
                <Clock />
              </el-icon>
            </template>
          </StatCard>
        </el-card>
      </el-col>
      <el-col
        :xs="24"
        :sm="12"
        :md="8"
        :lg="4"
      >
        <el-card
          shadow="hover"
          class="stat-card"
        >
          <StatCard
            label="商品总数"
            :value="stats.totalProducts || 0"
            icon-color="#3b82f6"
            icon-bg="rgba(59, 130, 246, 0.1)"
            :trend-label="`纸样 ${stats.totalPatterns || 0}`"
            :formatter="formatNumber"
          >
            <template #icon>
              <el-icon :size="22">
                <Box />
              </el-icon>
            </template>
          </StatCard>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第二行：趋势图 + 订单分布 -->
    <el-row
      :gutter="20"
      class="chart-row"
    >
      <el-col
        :xs="24"
        :lg="16"
      >
        <el-card
          shadow="never"
          class="chart-card"
        >
          <template #header>
            <div class="card-header">
              <span class="card-title">营收与订单趋势</span>
              <div class="card-controls">
                <el-radio-group
                  v-model="trendMetric"
                  size="small"
                >
                  <el-radio-button label="amount">
                    营收
                  </el-radio-button>
                  <el-radio-button label="orderCount">
                    订单数
                  </el-radio-button>
                </el-radio-group>
                <el-radio-group
                  v-model="trendRange"
                  size="small"
                >
                  <el-radio-button label="7">
                    近7天
                  </el-radio-button>
                  <el-radio-button label="30">
                    近30天
                  </el-radio-button>
                </el-radio-group>
              </div>
            </div>
          </template>
          <TrendChart
            :data="trendData.items"
            :metric="trendMetric"
          />
        </el-card>
      </el-col>
      <el-col
        :xs="24"
        :lg="8"
      >
        <el-card
          shadow="never"
          class="chart-card"
        >
          <template #header>
            <span class="card-title">订单状态分布</span>
          </template>
          <DonutChart
            :data="orderDistData"
            center-label="总订单"
          />
        </el-card>
      </el-col>
    </el-row>

    <!-- 第三行：商家排行 + 商品排行 -->
    <el-row
      :gutter="20"
      class="chart-row"
    >
      <el-col
        :xs="24"
        :lg="12"
      >
        <el-card
          shadow="never"
          class="chart-card"
        >
          <template #header>
            <div class="card-header">
              <span class="card-title">商家营收排行 TOP10</span>
              <el-link
                type="primary"
                :underline="false"
                @click="router.push('/merchant-list')"
              >
                查看全部
              </el-link>
            </div>
          </template>
          <RankingList
            :data="overview.topMerchants"
            :show-count="true"
            count-label="订单数"
          />
        </el-card>
      </el-col>
      <el-col
        :xs="24"
        :lg="12"
      >
        <el-card
          shadow="never"
          class="chart-card"
        >
          <template #header>
            <div class="card-header">
              <span class="card-title">商品销量排行 TOP10</span>
              <el-link
                type="primary"
                :underline="false"
                @click="router.push('/product-list')"
              >
                查看全部
              </el-link>
            </div>
          </template>
          <RankingList
            :data="overview.topProducts"
            :show-count="true"
            count-label="销量"
          />
        </el-card>
      </el-col>
    </el-row>

    <!-- 第四行：待办任务 + 快捷入口 -->
    <el-row
      :gutter="20"
      class="chart-row"
    >
      <el-col
        :xs="24"
        :lg="10"
      >
        <el-card
          shadow="never"
          class="chart-card"
        >
          <template #header>
            <span class="card-title">待办任务</span>
          </template>
          <div class="todo-grid">
            <div
              v-for="task in todoTasks"
              :key="task.path"
              class="todo-item"
              @click="router.push(task.path)"
            >
              <div
                class="todo-icon"
                :style="{ backgroundColor: task.bg, color: task.color }"
              >
                <el-icon :size="20">
                  <component :is="task.icon" />
                </el-icon>
              </div>
              <div class="todo-info">
                <div class="todo-count">
                  {{ task.count }}
                </div>
                <div class="todo-label">
                  {{ task.label }}
                </div>
              </div>
              <el-icon class="todo-arrow">
                <ArrowRight />
              </el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col
        :xs="24"
        :lg="14"
      >
        <el-card
          shadow="never"
          class="chart-card"
        >
          <template #header>
            <span class="card-title">快捷入口</span>
          </template>
          <div class="shortcut-grid">
            <div
              v-for="sc in shortcuts"
              :key="sc.path"
              class="shortcut-item"
              @click="router.push(sc.path)"
            >
              <el-icon
                :size="24"
                :color="sc.color"
              >
                <component :is="sc.icon" />
              </el-icon>
              <span class="shortcut-label">{{ sc.label }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import {
    UserFilled, Briefcase, Document, Wallet, Clock, Box,
    Refresh, ArrowRight, ShoppingCart, Goods, Bell, Setting,
    TrendCharts, Lock,
} from '@element-plus/icons-vue';
import StatCard from '@/components/dashboard/StatCard.vue';
import TrendChart from '@/components/dashboard/TrendChart.vue';
import DonutChart from '@/components/dashboard/DonutChart.vue';
import RankingList from '@/components/dashboard/RankingList.vue';
import {
    getDashboardStats, getDashboardTrend, getDashboardOverview,
} from '@/api/dashboard';
import type {
    DashboardStats, DashboardTrendResponse, DashboardOverviewResponse,
} from '@/api/dashboard';

const router = useRouter();
const loading = ref(false);

// 核心统计数据
const stats = reactive<DashboardStats>({
    userCount: 0,
    merchantCount: 0,
    orderCount: 0,
    todayRevenue: 0,
    todayOrders: 0,
    pendingAuditCount: 0,
    todayNewUsers: 0,
    yesterdayOrders: 0,
    yesterdayRevenue: 0,
    yesterdayNewUsers: 0,
    ordersGrowth: 0,
    revenueGrowth: 0,
    userGrowth: 0,
    pendingMerchantCount: 0,
    pendingProductCount: 0,
    pendingAfterSaleCount: 0,
    totalProducts: 0,
    totalPatterns: 0,
});

// 趋势数据
const trendData = reactive<DashboardTrendResponse>({ items: [], days: 7 });
const trendRange = ref<'7' | '30'>('7');
const trendMetric = ref<'amount' | 'orderCount'>('amount');

// 概览数据
const overview = reactive<DashboardOverviewResponse>({
    orderStatusDist: [],
    topMerchants: [],
    topProducts: [],
});

const todayStr = computed(() => {
    const d = new Date();
    const week = ['日', '一', '二', '三', '四', '五', '六'];
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} 周${week[d.getDay()]}`;
});

// sparkline 数据（从趋势数据派生）
const revenueSparkline = computed(() => trendData.items.map((i) => i.amount));
const orderSparkline = computed(() => trendData.items.map((i) => i.orderCount));
const userSparkline = computed(() => [
    stats.yesterdayNewUsers || 0,
    stats.todayNewUsers || 0,
]);

// 订单分布数据（映射颜色）
const ORDER_STATUS_COLORS: Record<number, string> = {
    0: '#f59e0b', 1: '#3b82f6', 2: '#8b5cf6', 3: '#10b981',
    4: '#9ca3af', 5: '#ef4444', 6: '#6366f1',
};
const orderDistData = computed(() =>
    overview.orderStatusDist
        .filter((s) => s.count > 0)
        .map((s) => ({
            label: s.label,
            value: s.count,
            color: ORDER_STATUS_COLORS[s.status] || '#9ca3af',
        }))
);

// 待办任务
const todoTasks = computed(() => [
    {
        label: '待审核商家', count: stats.pendingMerchantCount || 0,
        path: '/merchant-audit', icon: Briefcase,
        color: '#10b981', bg: 'rgba(16, 185, 129, 0.1)',
    },
    {
        label: '待审核商品', count: stats.pendingProductCount || 0,
        path: '/product-audit', icon: Box,
        color: '#3b82f6', bg: 'rgba(59, 130, 246, 0.1)',
    },
    {
        label: '待处理售后', count: stats.pendingAfterSaleCount || 0,
        path: '/order-arbitration', icon: Bell,
        color: '#ef4444', bg: 'rgba(239, 68, 68, 0.1)',
    },
]);

// 快捷入口
const shortcuts = [
    { label: '商家审核', path: '/merchant-audit', icon: Briefcase, color: '#10b981' },
    { label: '商品审核', path: '/product-audit', icon: Box, color: '#3b82f6' },
    { label: '纸样审核', path: '/pattern-audit', icon: Goods, color: '#8b5cf6' },
    { label: '订单仲裁', path: '/order-arbitration', icon: ShoppingCart, color: '#f59e0b' },
    { label: '版权登记', path: '/copyrights', icon: Lock, color: '#6366f1' },
    { label: '退款管理', path: '/refunds', icon: Wallet, color: '#ef4444' },
    { label: '数据报告', path: '/reports', icon: TrendCharts, color: '#14b8a6' },
    { label: '系统设置', path: '/settings', icon: Setting, color: '#64748b' },
];

function growthType(growth?: number): 'up' | 'down' | 'normal' {
    if (growth === undefined || growth === null || growth === 0) {
        return 'normal';
    }
    return growth > 0 ? 'up' : 'down';
}

function formatNumber(v: number | string): string {
    const n = Number(v);
    if (n >= 10000) {
        return (n / 10000).toFixed(1) + '万';
    }
    return n.toLocaleString();
}

function formatMoney(v: number | string): string {
    const n = Number(v);
    if (n >= 10000) {
        return '¥' + (n / 10000).toFixed(1) + '万';
    }
    return '¥' + n.toLocaleString();
}

async function loadStats() {
    try {
        const data = await getDashboardStats();
        Object.assign(stats, data);
    } catch (err) {
        console.warn('加载统计数据失败', err);
    }
}

async function loadTrend() {
    try {
        const data = await getDashboardTrend(Number(trendRange.value));
        Object.assign(trendData, data);
    } catch (err) {
        console.warn('加载趋势数据失败', err);
    }
}

async function loadOverview() {
    try {
        const data = await getDashboardOverview();
        Object.assign(overview, data);
    } catch (err) {
        console.warn('加载概览数据失败', err);
    }
}

async function loadAll() {
    loading.value = true;
    await Promise.allSettled([loadStats(), loadTrend(), loadOverview()]);
    loading.value = false;
}

watch(trendRange, () => {
    loadTrend();
});

onMounted(() => {
    loadAll();
});
</script>

<style scoped>
.dashboard-page {
    max-width: 1600px;
}

.page-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 20px;
}

.page-title {
    font-size: 22px;
    font-weight: 600;
    color: var(--color-text-primary, #303133);
    margin: 0;
}

.page-subtitle {
    font-size: 13px;
    color: var(--color-text-secondary, #909399);
    margin-top: 4px;
}

.stat-row {
    margin-bottom: 0;
}

.stat-card {
    height: 100%;
    margin-bottom: 20px;
    cursor: default;
    transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.stat-card:hover {
    transform: translateY(-2px);
}

.stat-card-clickable {
    cursor: pointer;
}

.chart-row {
    margin-bottom: 20px;
}

.chart-card {
    height: 100%;
    margin-bottom: 20px;
}

.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.card-title {
    font-size: 15px;
    font-weight: 600;
    color: var(--color-text-primary, #303133);
}

.card-controls {
    display: flex;
    gap: 8px;
    align-items: center;
}

/* 待办任务 */
.todo-grid {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.todo-item {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 12px;
    border-radius: 8px;
    cursor: pointer;
    transition: background-color 0.2s ease;
    border: 1px solid #f0f0f0;
}

.todo-item:hover {
    background-color: #f9fafb;
}

.todo-icon {
    width: 42px;
    height: 42px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
}

.todo-info {
    flex: 1;
}

.todo-count {
    font-size: 22px;
    font-weight: 700;
    color: var(--color-text-primary, #303133);
    line-height: 1.2;
}

.todo-label {
    font-size: 13px;
    color: var(--color-text-secondary, #909399);
    margin-top: 2px;
}

.todo-arrow {
    color: var(--color-text-secondary, #c0c4cc);
}

/* 快捷入口 */
.shortcut-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 12px;
}

.shortcut-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    padding: 18px 8px;
    border-radius: 10px;
    cursor: pointer;
    transition: background-color 0.2s ease;
    border: 1px solid #f0f0f0;
}

.shortcut-item:hover {
    background-color: #f9fafb;
}

.shortcut-label {
    font-size: 13px;
    color: var(--color-text-primary, #606266);
}

@media (max-width: 768px) {
    .shortcut-grid {
        grid-template-columns: repeat(2, 1fr);
    }

    .card-controls {
        flex-direction: column;
        gap: 4px;
    }
}
</style>
