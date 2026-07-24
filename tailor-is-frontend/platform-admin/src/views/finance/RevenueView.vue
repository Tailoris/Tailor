<template>
  <div v-loading="loading" class="page-container">
    <div class="page-header">
      <h2>收入统计</h2>
      <el-select v-model="range" style="width: 120px" @change="loadData">
        <el-option label="今日" value="today" /><el-option label="本周" value="week" />
        <el-option label="本月" value="month" /><el-option label="本季度" value="quarter" />
      </el-select>
    </div>
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon revenue"><el-icon :size="24"><Money /></el-icon></div>
          <div class="stat-content">
            <div class="stat-value">¥{{ formatNumber(stats.thisMonthRevenue) }}</div>
            <div class="stat-label">本月收入</div>
            <div class="stat-trend" :class="stats.revenueGrowth >= 0 ? 'up' : 'down'">{{ stats.revenueGrowth >= 0 ? '+' : '' }}{{ stats.revenueGrowth }}%</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon fee"><el-icon :size="24"><Coin /></el-icon></div>
          <div class="stat-content">
            <div class="stat-value">¥{{ formatNumber(stats.platformFee) }}</div>
            <div class="stat-label">平台手续费</div>
            <div class="stat-trend" :class="stats.feeGrowth >= 0 ? 'up' : 'down'">{{ stats.feeGrowth >= 0 ? '+' : '' }}{{ stats.feeGrowth }}%</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon settlement"><el-icon :size="24"><Sell /></el-icon></div>
          <div class="stat-content">
            <div class="stat-value">¥{{ formatNumber(stats.settlementAmount) }}</div>
            <div class="stat-label">已结算金额</div>
            <div class="stat-trend neutral">累计</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon refund"><el-icon :size="24"><Wallet /></el-icon></div>
          <div class="stat-content">
            <div class="stat-value">¥{{ formatNumber(stats.refundAmount) }}</div>
            <div class="stat-label">退款金额</div>
            <div class="stat-trend down">累计</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="14">
        <el-card class="chart-card">
          <div class="chart-header"><span class="chart-title">收入趋势</span></div>
          <div class="chart-body">
            <div class="dual-chart">
              <div v-for="item in stats.dailyTrend" :key="item.date" class="dual-item">
                <div class="dual-value">¥{{ formatK(item.revenue) }}</div>
                <div class="dual-bars">
                  <div class="dual-bar revenue-bar" :style="{ height: (item.revenue / maxRevenue * 100) + '%' }" />
                  <div class="dual-bar fee-bar" :style="{ height: (item.fee / maxRevenue * 100) + '%' }" />
                </div>
                <div class="dual-label">{{ item.date }}</div>
              </div>
            </div>
            <div class="chart-legend">
              <span class="legend-item"><span class="legend-dot revenue-dot" />收入</span>
              <span class="legend-item"><span class="legend-dot fee-dot" />手续费</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="5">
        <el-card class="chart-card">
          <div class="chart-header"><span class="chart-title">支付渠道</span></div>
          <div class="chart-body">
            <div class="pie-list">
              <div v-for="(item, idx) in stats.channelDistribution" :key="item.name" class="pie-row">
                <div class="pie-dot" :style="{ background: colors[idx] }" />
                <span class="pie-name">{{ item.name }}</span>
                <span class="pie-pct">{{ item.value }}%</span>
                <div class="pie-track"><div class="pie-fill" :style="{ width: item.value + '%', background: colors[idx] }" /></div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="5">
        <el-card class="chart-card">
          <div class="chart-header"><span class="chart-title">收入来源</span></div>
          <div class="chart-body">
            <div class="pie-list">
              <div v-for="(item, idx) in stats.categoryDistribution" :key="item.name" class="pie-row">
                <div class="pie-dot" :style="{ background: colors2[idx] }" />
                <span class="pie-name">{{ item.name }}</span>
                <span class="pie-pct">{{ item.value }}%</span>
                <div class="pie-track"><div class="pie-fill" :style="{ width: item.value + '%', background: colors2[idx] }" /></div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card class="summary-card">
          <div class="summary-header"><span class="chart-title">累计数据概览</span></div>
          <el-row :gutter="20">
            <el-col :span="8">
              <div class="summary-item">
                <div class="summary-label">累计总收入</div>
                <div class="summary-value">¥{{ formatNumber(stats.totalRevenue) }}</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="summary-item">
                <div class="summary-label">累计平台费</div>
                <div class="summary-value">¥{{ formatNumber(stats.totalRevenue * 0.1) }}</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="summary-item">
                <div class="summary-label">净收入</div>
                <div class="summary-value">¥{{ formatNumber(stats.totalRevenue * 0.9) }}</div>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { Money, Coin, Sell, Wallet } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { getRevenueStats, type RevenueStats } from '@/api/finance';

const loading = ref(false);
const range = ref<'today' | 'week' | 'month' | 'quarter'>('month');
const stats = reactive<RevenueStats>({
  todayRevenue: 0, yesterdayRevenue: 0, thisMonthRevenue: 0, lastMonthRevenue: 0,
  totalRevenue: 0, platformFee: 0, settlementAmount: 0, refundAmount: 0,
  revenueGrowth: 0, feeGrowth: 0, dailyTrend: [], channelDistribution: [], categoryDistribution: [],
});
const colors = ['#6366f1', '#22c55e', '#f59e0b', '#ef4444'];
const colors2 = ['#3b82f6', '#8b5cf6', '#ec4899', '#06b6d4'];

const maxRevenue = computed(() => Math.max(...stats.dailyTrend.map(t => t.revenue), 1));

function formatNumber(n: number) { return n.toLocaleString(); }
function formatK(n: number) { return n >= 1000 ? (n / 1000).toFixed(1) + 'k' : n.toString(); }

async function loadData() {
  loading.value = true;
  try {
    const data = await getRevenueStats({ range: range.value });
    Object.assign(stats, data);
  } catch { ElMessage.error('加载收入统计失败'); } finally { loading.value = false; }
}

onMounted(() => { loadData(); });
</script>

<style scoped>
.page-container { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 18px; font-weight: 600; }

.stat-card { display: flex; align-items: center; gap: 16px; padding: 20px; }
.stat-icon { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; color: white; }
.stat-icon.revenue { background: linear-gradient(135deg, #6366f1, #8b5cf6); }
.stat-icon.fee { background: linear-gradient(135deg, #22c55e, #16a34a); }
.stat-icon.settlement { background: linear-gradient(135deg, #3b82f6, #2563eb); }
.stat-icon.refund { background: linear-gradient(135deg, #f59e0b, #d97706); }
.stat-content { flex: 1; }
.stat-value { font-size: 24px; font-weight: 700; color: #1f2937; }
.stat-label { font-size: 13px; color: #6b7280; margin-top: 4px; }
.stat-trend { font-size: 12px; font-weight: 500; margin-top: 4px; }
.stat-trend.up { color: #22c55e; }
.stat-trend.down { color: #ef4444; }
.stat-trend.neutral { color: #6b7280; }

.chart-card { margin-bottom: 0; }
.chart-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.chart-title { font-size: 16px; font-weight: 600; }
.chart-legend { display: flex; gap: 20px; justify-content: center; margin-top: 12px; }
.legend-item { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #6b7280; }
.legend-dot { width: 10px; height: 10px; border-radius: 3px; }
.revenue-dot { background: #6366f1; }
.fee-dot { background: #22c55e; }

.dual-chart { display: flex; align-items: flex-end; justify-content: space-around; height: 200px; }
.dual-item { display: flex; flex-direction: column; align-items: center; flex: 1; height: 100%; }
.dual-value { font-size: 10px; color: #6b7280; margin-bottom: 4px; }
.dual-bars { flex: 1; width: 100%; display: flex; align-items: flex-end; justify-content: center; gap: 4px; padding: 0 6px; }
.dual-bar { width: 14px; border-radius: 3px 3px 0 0; min-height: 2px; transition: height 0.5s ease; }
.revenue-bar { background: linear-gradient(180deg, #6366f1, #8b5cf6); }
.fee-bar { background: linear-gradient(180deg, #22c55e, #16a34a); }
.dual-label { font-size: 11px; color: #9ca3af; margin-top: 6px; }

.pie-list { padding: 10px 0; }
.pie-row { display: flex; align-items: center; gap: 8px; margin-bottom: 14px; }
.pie-dot { width: 10px; height: 10px; border-radius: 3px; flex-shrink: 0; }
.pie-name { font-size: 12px; color: #374151; width: 60px; }
.pie-pct { font-size: 12px; color: #6b7280; width: 32px; text-align: right; }
.pie-track { flex: 1; height: 6px; border-radius: 3px; background: #f3f4f6; overflow: hidden; }
.pie-fill { height: 100%; border-radius: 3px; transition: width 0.5s ease; }

.summary-card { margin-bottom: 0; }
.summary-header { margin-bottom: 20px; }
.summary-item { text-align: center; padding: 16px; }
.summary-label { font-size: 13px; color: #6b7280; margin-bottom: 8px; }
.summary-value { font-size: 28px; font-weight: 700; color: #1f2937; }
</style>