<template>
  <div class="stat-card-inner">
    <div class="stat-card-top">
      <div
        class="stat-icon"
        :style="{ backgroundColor: iconBg, color: iconColor }"
      >
        <slot name="icon">
          <el-icon :size="22">
            <DataAnalysis />
          </el-icon>
        </slot>
      </div>
      <div class="stat-info">
        <div class="stat-label">
          {{ label }}
        </div>
        <div class="stat-value">
          {{ displayValue }}
        </div>
      </div>
    </div>
    <div class="stat-card-bottom">
      <span
        v-if="trend !== null && trend !== undefined"
        class="stat-trend"
        :class="trendClass"
      >
        <el-icon
          v-if="trendType === 'up'"
          :size="12"
        >
          <CaretTop />
        </el-icon>
        <el-icon
          v-else-if="trendType === 'down'"
          :size="12"
        >
          <CaretBottom />
        </el-icon>
        <span class="trend-text">{{ trendText }}</span>
      </span>
      <span
        v-else
        class="stat-trend normal"
      >{{ trendLabel }}</span>
      <!-- 迷你折线图 -->
      <svg
        v-if="sparkline && sparkline.length >= 2"
        class="sparkline"
        :viewBox="`0 0 ${sparkW} ${sparkH}`"
        preserveAspectRatio="none"
      >
        <path
          :d="sparkAreaPath"
          :fill="iconColor"
          opacity="0.12"
        />
        <path
          :d="sparkLinePath"
          fill="none"
          :stroke="iconColor"
          stroke-width="1.5"
          stroke-linejoin="round"
          stroke-linecap="round"
        />
      </svg>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { DataAnalysis, CaretTop, CaretBottom } from '@element-plus/icons-vue';

/**
 * 增强指标卡组件
 * @prop label 指标名称
 * @prop value 数值
 * @prop iconColor 图标颜色（同时用于 sparkline）
 * @prop iconBg 图标背景色
 * @prop trend 增长率数值（正数上升，负数下降）
 * @prop trendType 趋势类型 up/down/normal
 * @prop trendLabel 自定义趋势文案（trend 为空时显示）
 * @prop sparkline 迷你折线图数据
 */
const props = withDefaults(defineProps<{
    label: string;
    value: number | string;
    iconColor?: string;
    iconBg?: string;
    trend?: number | null;
    trendType?: 'up' | 'down' | 'normal';
    trendLabel?: string;
    sparkline?: number[];
    formatter?: (v: number | string) => string;
}>(), {
    iconColor: '#6366f1',
    iconBg: 'rgba(99, 102, 241, 0.1)',
    trend: null,
    trendType: 'normal',
    trendLabel: '',
    sparkline: () => [],
    formatter: (v: number | string) => String(v),
});

const sparkW = 100;
const sparkH = 32;

const displayValue = computed(() => props.formatter(props.value));

const trendClass = computed(() => props.trendType);

const trendText = computed(() => {
    if (props.trend === null || props.trend === undefined) {
        return props.trendLabel;
    }
    const sign = props.trend > 0 ? '+' : '';
    return `${sign}${props.trend.toFixed(1)}% 较昨日`;
});

// 计算 sparkline SVG 路径
const sparkLinePath = computed(() => {
    const data = props.sparkline;
    if (!data || data.length < 2) {
        return '';
    }
    const max = Math.max(...data);
    const min = Math.min(...data);
    const range = max - min || 1;
    const stepX = sparkW / (data.length - 1);
    return data
        .map((v, i) => {
            const x = i * stepX;
            const y = sparkH - ((v - min) / range) * (sparkH - 4) - 2;
            return `${i === 0 ? 'M' : 'L'}${x.toFixed(1)},${y.toFixed(1)}`;
        })
        .join(' ');
});

const sparkAreaPath = computed(() => {
    const line = sparkLinePath.value;
    if (!line) {
        return '';
    }
    return `${line} L${sparkW},${sparkH} L0,${sparkH} Z`;
});
</script>

<style scoped>
.stat-card-inner {
    display: flex;
    flex-direction: column;
    gap: 14px;
    height: 100%;
}

.stat-card-top {
    display: flex;
    align-items: center;
    gap: 14px;
}

.stat-icon {
    width: 46px;
    height: 46px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
}

.stat-info {
    flex: 1;
    min-width: 0;
}

.stat-label {
    font-size: 13px;
    color: var(--color-text-secondary, #909399);
    line-height: 1.4;
}

.stat-value {
    font-size: 26px;
    font-weight: 700;
    color: var(--color-text-primary, #303133);
    margin-top: 2px;
    line-height: 1.2;
}

.stat-card-bottom {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    gap: 8px;
}

.stat-trend {
    display: inline-flex;
    align-items: center;
    gap: 2px;
    font-size: 12px;
    line-height: 1;
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

.sparkline {
    width: 100px;
    height: 32px;
    flex-shrink: 0;
}
</style>
