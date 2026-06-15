<template>
  <div class="stat-cards">
    <div class="stat-card" v-for="stat in stats" :key="stat.title">
      <div class="stat-icon" :style="{ background: stat.color }">
        <el-icon :size="28"><component :is="stat.icon" /></el-icon>
      </div>
      <div class="stat-content">
        <div class="stat-title">{{ stat.title }}</div>
        <div class="stat-value">{{ stat.value }}</div>
        <div v-if="stat.subtitle" class="stat-subtitle">{{ stat.subtitle }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Document, Money, Service, Clock } from '@element-plus/icons-vue'

const props = defineProps<{
  todayOrders: number
  todayRevenue: number
  pendingOrders: number
  pendingAftersale: number
}>()

const stats = [
  {
    title: '今日订单',
    value: props.todayOrders.toString(),
    subtitle: '较昨日 +12%',
    icon: Document,
    color: 'linear-gradient(135deg, #6366F1 0%, #818CF8 100%)',
  },
  {
    title: '今日营收',
    value: `¥${props.todayRevenue.toLocaleString()}`,
    subtitle: '较昨日 +8%',
    icon: Money,
    color: 'linear-gradient(135deg, #10B981 0%, #34D399 100%)',
  },
  {
    title: '待处理订单',
    value: props.pendingOrders.toString(),
    icon: Clock,
    color: 'linear-gradient(135deg, #F59E0B 0%, #FBBF24 100%)',
  },
  {
    title: '待处理售后',
    value: props.pendingAftersale.toString(),
    icon: Service,
    color: 'linear-gradient(135deg, #EF4444 0%, #F87171 100%)',
  },
]
</script>

<style scoped>
.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: var(--color-white);
  border-radius: var(--border-radius);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: var(--shadow-sm);
  transition: transform var(--transition), box-shadow var(--transition);
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.stat-content {
  flex: 1;
}

.stat-title {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-bottom: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.stat-subtitle {
  font-size: 12px;
  color: var(--color-success);
  margin-top: 2px;
}

@media (max-width: 992px) {
  .stat-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 576px) {
  .stat-cards {
    grid-template-columns: 1fr;
  }
}
</style>
