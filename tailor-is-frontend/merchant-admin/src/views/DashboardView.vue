<template>
  <div class="dashboard" v-loading="loading">
    <PageHeader title="仪表盘">
      <template #subtitle>
        <span class="subtitle-text">欢迎回来，{{ userStore.userInfo?.name || '商家' }}</span>
      </template>
    </PageHeader>

    <StatCards
      :today-orders="stats.todayOrders"
      :today-revenue="stats.todayRevenue"
      :pending-orders="stats.pendingOrders"
      :pending-aftersale="stats.pendingAftersale"
    />

    <el-row :gutter="20">
      <el-col :span="16">
        <el-card class="chart-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>营收趋势</span>
              <el-radio-group v-model="chartRange" size="small">
                <el-radio-button label="week">近7天</el-radio-button>
                <el-radio-button label="month">近30天</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div class="chart-placeholder">
            <div class="chart-bars">
              <div
                v-for="(item, index) in stats.revenueTrend"
                :key="index"
                class="chart-bar-item"
              >
                <div
                  class="chart-bar"
                  :style="{ height: `${getBarHeight(item.amount)}%` }"
                />
                <span class="chart-bar-label">{{ formatDate(item.date) }}</span>
                <span class="chart-bar-value">¥{{ item.amount }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="recent-card" shadow="never">
          <template #header>
            <span>最近订单</span>
          </template>
          <div class="recent-list">
            <div v-for="order in stats.recentOrders" :key="order.orderNo" class="recent-item">
              <div class="recent-info">
                <span class="recent-order-no">{{ order.orderNo }}</span>
                <span class="recent-amount">¥{{ order.payAmount?.toFixed(2) }}</span>
              </div>
              <el-tag :type="orderStatusType[order.status]" size="small">
                {{ orderStatusLabel[order.status] }}
              </el-tag>
            </div>
            <el-empty v-if="!stats.recentOrders?.length" description="暂无订单" :image-size="60" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card class="aftersale-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>待处理售后工单</span>
              <el-button text type="primary" @click="$router.push('/aftersale')">查看全部</el-button>
            </div>
          </template>
          <el-table :data="stats.recentAftersales" stripe style="width: 100%">
            <el-table-column prop="ticketNo" label="工单号" width="180" />
            <el-table-column prop="buyerName" label="买家" width="120" />
            <el-table-column label="类型" width="120">
              <template #default="{ row }">
                {{ aftersaleTypeLabel[row.type] }}
              </template>
            </el-table-column>
            <el-table-column prop="reason" label="原因" min-width="200" show-overflow-tooltip />
            <el-table-column label="退款金额" width="120">
              <template #default="{ row }">
                <span class="amount">¥{{ row.refundAmount?.toFixed(2) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="aftersaleStatusType[row.status]" size="small">
                  {{ aftersaleStatusLabel[row.status] }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="$router.push(`/aftersale/${row.ticketNo}`)">
                  处理
                </el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无数据" />
            </template>
          </el-table>
          <el-empty v-if="!stats.recentAftersales?.length" description="暂无售后工单" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getDashboardStats } from '@/api/dashboard'
import type { DashboardStats } from '@/types'
import PageHeader from '@/components/PageHeader.vue'
import StatCards from '@/components/StatCards.vue'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const loading = ref(false)
const chartRange = ref<'week' | 'month'>('week')

const stats = reactive<DashboardStats>({
  todayOrders: 0,
  todayRevenue: 0,
  pendingOrders: 0,
  pendingAftersale: 0,
  totalProducts: 0,
  totalCustomers: 0,
  revenueTrend: [],
  recentOrders: [],
  recentAftersales: [],
})

const orderStatusType: Record<string, string> = {
  pending: 'info',
  paid: 'warning',
  shipped: '',
  completed: 'success',
  cancelled: 'danger',
}

const orderStatusLabel: Record<string, string> = {
  pending: '待付款',
  paid: '已付款',
  shipped: '已发货',
  completed: '已完成',
  cancelled: '已取消',
}

const aftersaleTypeLabel: Record<string, string> = {
  refund_only: '仅退款',
  return_and_refund: '退货退款',
  exchange: '换货',
}

const aftersaleStatusType: Record<string, string> = {
  pending: 'warning',
  processing: '',
  approved: 'success',
  rejected: 'danger',
  completed: 'info',
}

const aftersaleStatusLabel: Record<string, string> = {
  pending: '待处理',
  processing: '处理中',
  approved: '已同意',
  rejected: '已拒绝',
  completed: '已完成',
}

function formatDate(dateStr: string) {
  return dateStr.slice(5)
}

function getBarHeight(amount: number) {
  const max = Math.max(...stats.revenueTrend.map(i => i.amount), 1)
  return (amount / max) * 100
}

async function fetchStats() {
  loading.value = true
  try {
    const res = await getDashboardStats()
    Object.assign(stats, res)
  } catch {
    // 使用默认空数据
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchStats()
})
</script>

<style scoped>
.subtitle-text {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.chart-card,
.recent-card,
.aftersale-card {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-placeholder {
  min-height: 300px;
  display: flex;
  align-items: flex-end;
  padding: 20px 0;
}

.chart-bars {
  display: flex;
  justify-content: space-around;
  width: 100%;
  align-items: flex-end;
  gap: 12px;
}

.chart-bar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.chart-bar {
  width: 32px;
  background: linear-gradient(180deg, #6366F1, #818CF8);
  border-radius: 4px 4px 0 0;
  min-height: 4px;
  transition: height 0.5s ease;
}

.chart-bar-label {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.chart-bar-value {
  font-size: 12px;
  color: var(--color-text-primary);
  font-weight: 500;
}

.recent-list {
  max-height: 320px;
  overflow-y: auto;
}

.recent-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid var(--color-border);
}

.recent-item:last-child {
  border-bottom: none;
}

.recent-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.recent-order-no {
  font-size: 14px;
  color: var(--color-text-primary);
}

.recent-amount {
  font-size: 13px;
  color: var(--color-danger);
  font-weight: 500;
}

.amount {
  color: var(--color-danger);
  font-weight: 500;
}
</style>
