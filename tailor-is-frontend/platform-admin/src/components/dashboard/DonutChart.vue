<template>
  <div class="donut-wrap">
    <div
      v-if="total === 0"
      class="donut-empty"
    >
      暂无数据
    </div>
    <template v-else>
      <div class="donut-svg-wrap">
        <svg
          class="donut-svg"
          viewBox="0 0 200 200"
        >
          <!-- 背景环 -->
          <circle
            cx="100"
            cy="100"
            :r="radius"
            fill="none"
            stroke="#f5f5f5"
            :stroke-width="strokeWidth"
          />
          <!-- 分段 -->
          <circle
            v-for="(seg, i) in segments"
            :key="i"
            cx="100"
            cy="100"
            :r="radius"
            fill="none"
            :stroke="seg.color"
            :stroke-width="strokeWidth"
            :stroke-dasharray="`${seg.length} ${circumference - seg.length}`"
            :stroke-dashoffset="seg.offset"
            transform="rotate(-90 100 100)"
            class="donut-seg"
          />
        </svg>
        <div class="donut-center">
          <div class="donut-total">
            {{ formatNum(total) }}
          </div>
          <div class="donut-label">
            {{ centerLabel }}
          </div>
        </div>
      </div>
      <div class="donut-legend">
        <div
          v-for="(seg, i) in segments"
          :key="i"
          class="legend-item"
        >
          <span
            class="legend-dot"
            :style="{ backgroundColor: seg.color }"
          />
          <span class="legend-label">{{ seg.label }}</span>
          <span class="legend-value">{{ formatNum(seg.value) }}</span>
          <span class="legend-pct">{{ seg.percent }}%</span>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

/**
 * 环形分布图组件（纯 SVG 绘制）
 * @prop data 分布数据 {label, value, color}[]
 * @prop centerLabel 中心标签
 */
const props = withDefaults(defineProps<{
    data: { label: string; value: number; color?: string }[];
    centerLabel?: string;
}>(), {
    centerLabel: '总数',
});

const radius = 70;
const strokeWidth = 24;
const circumference = 2 * Math.PI * radius;

const DEFAULT_COLORS = [
    '#10b981', '#3b82f6', '#8b5cf6', '#f59e0b',
    '#ef4444', '#6366f1', '#9ca3af', '#14b8a6',
];

const total = computed(() =>
    props.data.reduce((sum, d) => sum + (Number(d.value) || 0), 0)
);

interface Segment {
    label: string;
    value: number;
    color: string;
    length: number;
    offset: number;
    percent: string;
}

const segments = computed<Segment[]>(() => {
    const t = total.value;
    if (t === 0) {
        return [];
    }
    let accOffset = 0;
    return props.data
        .map((d, i) => {
            const value = Number(d.value) || 0;
            if (value === 0) {
                return null;
            }
            const length = (value / t) * circumference;
            const offset = -accOffset;
            accOffset += length;
            const pct = ((value / t) * 100).toFixed(1);
            return {
                label: d.label,
                value,
                color: d.color || DEFAULT_COLORS[i % DEFAULT_COLORS.length],
                length,
                offset,
                percent: pct,
            } as Segment;
        })
        .filter((s): s is Segment => s !== null);
});

function formatNum(v: number): string {
    if (v >= 10000) {
        return (v / 10000).toFixed(1) + '万';
    }
    return v.toLocaleString();
}
</script>

<style scoped>
.donut-wrap {
    display: flex;
    align-items: center;
    gap: 24px;
    min-height: 200px;
}

.donut-empty {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 200px;
    color: var(--color-text-secondary, #909399);
    font-size: 14px;
}

.donut-svg-wrap {
    position: relative;
    width: 200px;
    height: 200px;
    flex-shrink: 0;
}

.donut-svg {
    width: 100%;
    height: 100%;
}

.donut-seg {
    transition: stroke-width 0.2s ease;
}

.donut-center {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    text-align: center;
}

.donut-total {
    font-size: 28px;
    font-weight: 700;
    color: var(--color-text-primary, #303133);
    line-height: 1.2;
}

.donut-label {
    font-size: 12px;
    color: var(--color-text-secondary, #909399);
    margin-top: 4px;
}

.donut-legend {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.legend-item {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 13px;
}

.legend-dot {
    width: 10px;
    height: 10px;
    border-radius: 3px;
    flex-shrink: 0;
}

.legend-label {
    flex: 1;
    color: var(--color-text-primary, #606266);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.legend-value {
    font-weight: 600;
    color: var(--color-text-primary, #303133);
}

.legend-pct {
    color: var(--color-text-secondary, #909399);
    width: 48px;
    text-align: right;
}
</style>
