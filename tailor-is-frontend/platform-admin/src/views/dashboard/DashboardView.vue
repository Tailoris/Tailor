<template>
  <div
    v-loading="loading"
    class="dashboard-page"
  >
    <div class="page-header">
      <h2 class="page-title">
        系统概览
      </h2>
      <p class="page-subtitle">
        裁智云平台整体运营数据概览
      </p>
    </div>

    <el-row
      :gutter="20"
      class="stat-row"
    >
      <el-col :span="6">
        <el-card
          shadow="never"
          class="stat-card"
        >
          <div class="stat-content">
            <div class="stat-icon users">
              <el-icon :size="24">
                <UserFilled />
              </el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">
                总用户数
              </div>
              <div class="stat-value">
                {{ formatNumber(stats.userCount) }}
              </div>
              <div class="stat-trend up">
                较昨日 +{{ stats.userGrowth }}%
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card
          shadow="never"
          class="stat-card"
        >
          <div class="stat-content">
            <div class="stat-icon orders">
              <el-icon :size="24">
                <Document />
              </el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">
                总订单数
              </div>
              <div class="stat-value">
                {{ formatNumber(stats.orderCount) }}
              </div>
              <div class="stat-trend up">
                较昨日 +{{ stats.orderGrowth }}%
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card
          shadow="never"
          class="stat-card"
        >
          <div class="stat-content">
            <div class="stat-icon revenue">
              <el-icon :size="24">
                <Money />
              </el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">
                今日收入
              </div>
              <div class="stat-value">
                ¥{{ formatMoney(stats.todayRevenue) }}
              </div>
              <div class="stat-trend up">
                较昨日 +{{ stats.revenueGrowth }}%
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card
          shadow="never"
          class="stat-card"
        >
          <div class="stat-content">
            <div class="stat-icon modules">
              <el-icon :size="24">
                <Monitor />
              </el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">
                待审核
              </div>
              <div class="stat-value">
                {{ stats.pendingAuditCount }}
              </div>
              <div class="stat-trend normal">
                审核中
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row
      :gutter="20"
      style="margin-top: 20px"
    >
      <el-col :span="16">
        <el-card
          shadow="never"
          class="chart-card"
        >
          <template #header>
            <div class="card-header">
              <span>平台收入趋势</span>
              <el-radio-group
                v-model="chartRange"
                size="small"
              >
                <el-radio-button label="week">
                  近7天
                </el-radio-button>
                <el-radio-button label="month">
                  近30天
                </el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div class="chart-area">
            <div class="chart-bars">
              <div
                v-for="(item, index) in chartData"
                :key="index"
                class="bar-item"
              >
                <div class="bar-wrapper">
                  <div
                    class="bar"
                    :style="{ height: getBarHeight(item.amount) + '%' }"
                  />
                </div>
                <span class="bar-label">{{ item.label }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card
          shadow="never"
          class="module-card"
        >
          <template #header>
            <span>模块运行状态</span>
          </template>
          <div class="module-list">
            <div
              v-for="mod in stats.moduleStatus"
              :key="mod.name"
              class="module-item"
            >
              <div class="module-info">
                <div class="module-name">
                  {{ mod.name }}
                </div>
                <div class="module-port">
                  端口: {{ mod.port }}
                </div>
              </div>
              <el-tag
                :type="mod.status === 'running' ? 'success' : 'danger'"
                size="small"
              >
                {{ mod.status === 'running' ? '运行中' : '已停止' }}
              </el-tag>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch } from 'vue';
import { UserFilled, Document, Money, Monitor } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { getDashboardStats } from '@/api/dashboard';

interface ModuleStatus {
  name: string;
  port: number;
  status: 'running' | 'stopped';
}

interface DashboardStats {
  userCount: number;
  merchantCount: number;
  orderCount: number;
  todayRevenue: number;
  todayOrders: number;
  pendingAuditCount: number;
  userGrowth?: number;
  orderGrowth?: number;
  revenueGrowth?: number;
  moduleStatus?: ModuleStatus[];
  revenueTrend?: { date: string; amount: number }[];
}

const loading = ref(false);
const error = ref(false);
const chartRange = ref<'week' | 'month'>('week');

const stats = reactive<DashboardStats>({
  userCount: 0,
  merchantCount: 0,
  orderCount: 0,
  todayRevenue: 0,
  todayOrders: 0,
  pendingAuditCount: 0,
  userGrowth: 0,
  orderGrowth: 0,
  revenueGrowth: 0,
  moduleStatus: [],
  revenueTrend: [],
});

async function loadDashboard() {
  loading.value = true;
  error.value = false;
  try {
    const data = await getDashboardStats({ range: chartRange.value });
    if (data) {
      Object.assign(stats, data);
    }
  } catch (err) {
    error.value = true;
    console.warn('加载仪表盘数据失败，使用本地数据:', err);
  } finally {
    loading.value = false;
  }
}

watch(chartRange, () => {
  loadDashboard();
});

const weekLabels = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];
const monthLabels = Array.from({ length: 30 }, (_, i) => `${i + 1}日`);

const chartData = computed(() => {
  const data = stats.revenueTrend;
  const labels = chartRange.value === 'week' ? weekLabels.slice(0, data.length) : monthLabels.slice(0, data.length);
  return data.map((item, index) => ({
    label: labels[index] || item.date.slice(5),
    amount: item.amount,
  }));
});

function formatNumber(num: number): string {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + '万';
  }
  return num.toLocaleString();
}

function formatMoney(num: number): string {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + '万';
  }
  return num.toLocaleString();
}

function getBarHeight(amount: number): number {
  const max = Math.max(...chartData.value.map((i) => i.amount), 1);
  return (amount / max) * 100;
}

onMounted(() => {
  loadDashboard();
});
</script>

<style scoped>
.dashboard-page {
  max-width: 1400px;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.page-subtitle {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-top: 4px;
}

.stat-card {
  height: 100%;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon.users {
  background-color: rgba(99, 102, 241, 0.1);
  color: #6366f1;
}

.stat-icon.orders {
  background-color: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.stat-icon.revenue {
  background-color: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
}

.stat-icon.modules {
  background-color: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 4px 0;
}

.stat-trend {
  font-size: 12px;
}

.stat-trend.up {
  color: #10b981;
}

.stat-trend.down {
  color: #ef4444;
}

.stat-trend.normal {
  color: #3b82f6;
}

.chart-card,
.module-card {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-area {
  min-height: 260px;
  display: flex;
  align-items: flex-end;
  padding: 20px 0 0;
}

.chart-bars {
  display: flex;
  justify-content: space-around;
  align-items: flex-end;
  width: 100%;
  gap: 16px;
}

.bar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.bar-wrapper {
  width: 40px;
  height: 200px;
  display: flex;
  align-items: flex-end;
}

.bar {
  width: 100%;
  background: linear-gradient(180deg, #6366f1, #818cf8);
  border-radius: 4px 4px 0 0;
  min-height: 4px;
  transition: height 0.5s ease;
}

.bar-label {
  font-size: 12px;
  color: var(--color-text-secondary);
  white-space: nowrap;
}

.module-list {
  max-height: 260px;
  overflow-y: auto;
}

.module-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid var(--color-border);
}

.module-item:last-child {
  border-bottom: none;
}

.module-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.module-name {
  font-size: 14px;
  color: var(--color-text-primary);
  font-weight: 500;
}

.module-port {
  font-size: 12px;
  color: var(--color-text-secondary);
}
</style>
