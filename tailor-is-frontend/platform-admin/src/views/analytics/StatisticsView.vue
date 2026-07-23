<template>
  <div v-loading="loading" class="page-container">
    <div class="page-header">
      <h2>数据统计</h2>
      <el-select v-model="chartRange" style="width: 120px" @change="loadStatistics">
        <el-option label="本周" value="week" />
        <el-option label="本月" value="month" />
        <el-option label="本季度" value="quarter" />
      </el-select>
    </div>
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon revenue">
            <el-icon :size="24"><Money /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">¥{{ formatNumber(stats.monthlyRevenue) }}</div>
            <div class="stat-label">本月收入</div>
            <div class="stat-trend" :class="stats.revenueGrowth >= 0 ? 'up' : 'down'">
              {{ stats.revenueGrowth >= 0 ? '+' : '' }}{{ stats.revenueGrowth }}%
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon order">
            <el-icon :size="24"><Document /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ formatNumber(stats.monthlyOrders) }}</div>
            <div class="stat-label">本月订单</div>
            <div class="stat-trend" :class="stats.orderGrowth >= 0 ? 'up' : 'down'">
              {{ stats.orderGrowth >= 0 ? '+' : '' }}{{ stats.orderGrowth }}%
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon user">
            <el-icon :size="24"><User /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ formatNumber(stats.newUsers) }}</div>
            <div class="stat-label">新增用户</div>
            <div class="stat-trend" :class="stats.userGrowth >= 0 ? 'up' : 'down'">
              {{ stats.userGrowth >= 0 ? '+' : '' }}{{ stats.userGrowth }}%
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon pattern">
            <el-icon :size="24"><Picture /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ formatNumber(stats.patternUploads) }}</div>
            <div class="stat-label">纸样上传</div>
            <div class="stat-trend" :class="stats.patternGrowth >= 0 ? 'up' : 'down'">
              {{ stats.patternGrowth >= 0 ? '+' : '' }}{{ stats.patternGrowth }}%
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="14">
        <el-card class="chart-card">
          <div class="chart-header">
            <span class="chart-title">收入趋势</span>
          </div>
          <div class="chart-body">
            <div class="bar-chart">
              <div v-for="item in stats.revenueTrend" :key="item.label" class="bar-item">
                <div class="bar-value">¥{{ formatNumber(item.amount) }}</div>
                <div class="bar-fill-wrapper">
                  <div class="bar-fill" :style="{ height: getBarHeight(item.amount, stats.revenueTrend) }" />
                </div>
                <div class="bar-label">{{ item.label }}</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card class="chart-card">
          <div class="chart-header">
            <span class="chart-title">品类分布</span>
          </div>
          <div class="chart-body">
            <div class="pie-chart">
              <div v-for="(item, idx) in stats.categoryDistribution" :key="item.name" class="pie-item">
                <div class="pie-color" :style="{ background: getColor(idx) }" />
                <span class="pie-name">{{ item.name }}</span>
                <span class="pie-value">{{ item.value }}%</span>
                <div class="pie-bar-track">
                  <div class="pie-bar-fill" :style="{ width: item.value + '%', background: getColor(idx) }" />
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card class="chart-card">
          <div class="chart-header">
            <span class="chart-title">订单趋势</span>
          </div>
          <div class="chart-body">
            <div class="line-chart">
              <div v-for="item in stats.orderTrend" :key="item.label" class="line-item">
                <div class="line-value">{{ item.count }}</div>
                <div class="line-dot-wrapper">
                  <div class="line-dot" :style="{ bottom: (item.count / getMaxOrderCount(stats.orderTrend) * 100) + '%' }" />
                </div>
                <div class="line-label">{{ item.label }}</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { Money, Document, User, Picture } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { getStatistics, type StatisticsData } from '@/api/analytics';

const loading = ref(false);
const chartRange = ref<'week' | 'month' | 'quarter'>('month');
const stats = reactive<StatisticsData>({
  monthlyRevenue: 0, monthlyOrders: 0, newUsers: 0, patternUploads: 0,
  revenueGrowth: 0, orderGrowth: 0, userGrowth: 0, patternGrowth: 0,
  revenueTrend: [], orderTrend: [], categoryDistribution: [],
});

const colors = ['#6366f1', '#22c55e', '#3b82f6', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#06b6d4'];

function getColor(idx: number) { return colors[idx % colors.length]; }
function formatNumber(n: number) { return n.toLocaleString(); }

function getBarHeight(amount: number, trend: { amount: number }[]) {
  const max = Math.max(...trend.map(t => t.amount), 1);
  return (amount / max * 100) + '%';
}

function getMaxOrderCount(trend: { count: number }[]) {
  return Math.max(...trend.map(t => t.count), 1);
}

async function loadStatistics() {
  loading.value = true;
  try {
    const data = await getStatistics({ range: chartRange.value });
    Object.assign(stats, data);
  } catch {
    ElMessage.error('加载统计数据失败');
  } finally {
    loading.value = false;
  }
}

onMounted(() => { loadStatistics(); });
</script>

<style scoped>
.page-container { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 18px; font-weight: 600; }

.stat-card { display: flex; align-items: center; gap: 16px; padding: 20px; }
.stat-icon { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.stat-icon.revenue { background: linear-gradient(135deg, #6366f1, #8b5cf6); color: white; }
.stat-icon.order { background: linear-gradient(135deg, #22c55e, #16a34a); color: white; }
.stat-icon.user { background: linear-gradient(135deg, #3b82f6, #2563eb); color: white; }
.stat-icon.pattern { background: linear-gradient(135deg, #f59e0b, #d97706); color: white; }
.stat-content { flex: 1; }
.stat-value { font-size: 24px; font-weight: 700; color: #1f2937; }
.stat-label { font-size: 13px; color: #6b7280; margin-top: 4px; }
.stat-trend { font-size: 12px; font-weight: 500; margin-top: 4px; }
.stat-trend.up { color: #22c55e; }
.stat-trend.down { color: #ef4444; }

.chart-card { margin-bottom: 0; }
.chart-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.chart-title { font-size: 16px; font-weight: 600; }

.bar-chart { display: flex; align-items: flex-end; justify-content: space-around; height: 220px; padding: 0 10px; }
.bar-item { display: flex; flex-direction: column; align-items: center; flex: 1; height: 100%; }
.bar-value { font-size: 11px; color: #6b7280; margin-bottom: 4px; white-space: nowrap; }
.bar-fill-wrapper { flex: 1; width: 100%; display: flex; align-items: flex-end; justify-content: center; padding: 0 8px; }
.bar-fill { width: 100%; max-width: 40px; background: linear-gradient(180deg, #6366f1, #8b5cf6); border-radius: 4px 4px 0 0; min-height: 4px; transition: height 0.5s ease; }
.bar-label { font-size: 11px; color: #9ca3af; margin-top: 6px; }

.pie-chart { padding: 10px 0; }
.pie-item { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.pie-color { width: 12px; height: 12px; border-radius: 3px; flex-shrink: 0; }
.pie-name { font-size: 13px; color: #374151; width: 40px; }
.pie-value { font-size: 13px; color: #6b7280; width: 36px; }
.pie-bar-track { flex: 1; height: 8px; border-radius: 4px; background: #f3f4f6; overflow: hidden; }
.pie-bar-fill { height: 100%; border-radius: 4px; transition: width 0.5s ease; }

.line-chart { display: flex; align-items: flex-end; justify-content: space-around; height: 200px; padding: 0 10px; }
.line-item { display: flex; flex-direction: column; align-items: center; flex: 1; height: 100%; }
.line-value { font-size: 11px; color: #6b7280; margin-bottom: 6px; }
.line-dot-wrapper { flex: 1; width: 100%; position: relative; border-bottom: 2px solid #e5e7eb; margin: 0 12px; }
.line-dot { position: absolute; width: 10px; height: 10px; border-radius: 50%; background: #3b82f6; border: 2px solid white; box-shadow: 0 1px 3px rgba(0,0,0,.15); transform: translateY(50%); left: 50%; margin-left: -5px; }
.line-label { font-size: 11px; color: #9ca3af; margin-top: 10px; }
</style>