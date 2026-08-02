<template>
  <div class="trend-chart-wrap">
    <div
      v-if="data.length === 0"
      class="chart-empty"
    >
      暂无趋势数据
    </div>
    <template v-else>
      <svg
        class="trend-svg"
        :viewBox="`0 0 ${W} ${H}`"
        preserveAspectRatio="xMidYMid meet"
        @mousemove="onMouseMove"
        @mouseleave="hoverIndex = -1"
      >
        <defs>
          <linearGradient
            :id="areaGradientId"
            x1="0"
            y1="0"
            x2="0"
            y2="1"
          >
            <stop
              offset="0%"
              stop-color="#6366f1"
              stop-opacity="0.28"
            />
            <stop
              offset="100%"
              stop-color="#6366f1"
              stop-opacity="0.02"
            />
          </linearGradient>
        </defs>

        <!-- Y 轴网格线 + 刻度 -->
        <g class="grid">
          <line
            v-for="(g, i) in yGrids"
            :key="'g' + i"
            :x1="padL"
            :y1="g.y"
            :x2="W - padR"
            :y2="g.y"
            stroke="#f0f0f0"
            stroke-width="1"
          />
          <text
            v-for="(g, i) in yGrids"
            :key="'yt' + i"
            :x="padL - 8"
            :y="g.y + 3"
            text-anchor="end"
            class="axis-text"
          >{{ g.label }}</text>
        </g>

        <!-- 面积填充 -->
        <path
          :d="areaPath"
          :fill="`url(#${areaGradientId})`"
        />
        <!-- 折线 -->
        <path
          :d="linePath"
          fill="none"
          stroke="#6366f1"
          stroke-width="2"
          stroke-linejoin="round"
          stroke-linecap="round"
        />

        <!-- hover 竖线 -->
        <line
          v-if="hoverIndex >= 0"
          :x1="points[hoverIndex].x"
          :y1="padT"
          :x2="points[hoverIndex].x"
          :y2="H - padB"
          stroke="#6366f1"
          stroke-width="1"
          stroke-dasharray="4 3"
          opacity="0.5"
        />

        <!-- 数据点 -->
        <g class="points">
          <circle
            v-for="(p, i) in points"
            :key="'p' + i"
            :cx="p.x"
            :cy="p.y"
            :r="hoverIndex === i ? 5 : 3"
            :fill="hoverIndex === i ? '#6366f1' : '#fff'"
            stroke="#6366f1"
            stroke-width="2"
          />
        </g>

        <!-- X 轴日期标签（稀疏显示，避免重叠） -->
        <g class="x-axis">
          <text
            v-for="(p, i) in xLabels"
            :key="'xl' + i"
            :x="p.x"
            :y="H - padB + 18"
            text-anchor="middle"
            class="axis-text"
          >{{ p.label }}</text>
        </g>

        <!-- hover tooltip -->
        <g
          v-if="hoverIndex >= 0"
          class="tooltip"
        >
          <rect
            :x="tooltipX"
            :y="padT - 4"
            width="120"
            height="44"
            rx="6"
            fill="rgba(31, 41, 55, 0.92)"
          />
          <text
            :x="tooltipX + 10"
            :y="padT + 12"
            class="tooltip-text"
          >{{ data[hoverIndex].date }}</text>
          <text
            :x="tooltipX + 10"
            :y="padT + 30"
            class="tooltip-text"
          >{{ metric === 'amount' ? '营收' : '订单' }}：{{ tooltipValue }}</text>
        </g>
      </svg>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import type { TrendItem } from '@/api/dashboard';

/**
 * 趋势面积折线图组件（纯 SVG 绘制，无第三方依赖）
 * @prop data 趋势数据
 * @prop metric 展示指标 amount=营收 orderCount=订单数
 */
const props = withDefaults(defineProps<{
    data: TrendItem[];
    metric?: 'amount' | 'orderCount';
}>(), {
    metric: 'amount',
});

const W = 760;
const H = 280;
const padL = 56;
const padR = 16;
const padT = 20;
const padB = 36;

const areaGradientId = 'trend-grad-' + Math.random().toString(36).slice(2, 8);
const hoverIndex = ref(-1);

const values = computed(() => props.data.map((d) => Number(d[props.metric]) || 0));

const maxVal = computed(() => {
    const m = Math.max(...values.value, 1);
    // 向上取整到合适刻度
    const mag = Math.pow(10, Math.floor(Math.log10(m)));
    return Math.ceil(m / mag) * mag;
});

const points = computed(() => {
    const n = props.data.length;
    if (n === 0) {
        return [];
    }
    const plotW = W - padL - padR;
    const plotH = H - padT - padB;
    const stepX = n > 1 ? plotW / (n - 1) : 0;
    return props.data.map((d, i) => {
        const x = padL + i * stepX;
        const v = Number(d[props.metric]) || 0;
        const y = padT + plotH - (v / maxVal.value) * plotH;
        return { x, y, value: v };
    });
});

const linePath = computed(() => {
    return points.value.map((p, i) => `${i === 0 ? 'M' : 'L'}${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' ');
});

const areaPath = computed(() => {
    const line = linePath.value;
    if (!line) {
        return '';
    }
    const baseY = H - padB;
    const first = points.value[0];
    const last = points.value[points.value.length - 1];
    return `${line} L${last.x.toFixed(1)},${baseY} L${first.x.toFixed(1)},${baseY} Z`;
});

const yGrids = computed(() => {
    const plotH = H - padT - padB;
    const steps = 4;
    const grids = [];
    for (let i = 0; i <= steps; i++) {
        const y = padT + (plotH / steps) * i;
        const val = maxVal.value * (1 - i / steps);
        grids.push({ y, label: formatAxisLabel(val) });
    }
    return grids;
});

const xLabels = computed(() => {
    const n = props.data.length;
    // 稀疏显示：最多显示 7 个标签
    const interval = Math.max(1, Math.ceil(n / 7));
    const labels = [];
    for (let i = 0; i < n; i += interval) {
        const label = props.data[i].date.slice(5);
        labels.push({ x: points.value[i].x, label });
    }
    // 确保最后一个标签显示
    const lastIdx = n - 1;
    if (labels.length > 0 && labels[labels.length - 1].x !== points.value[lastIdx].x) {
        labels.push({ x: points.value[lastIdx].x, label: props.data[lastIdx].date.slice(5) });
    }
    return labels;
});

const tooltipX = computed(() => {
    if (hoverIndex.value < 0) {
        return 0;
    }
    const x = points.value[hoverIndex.value].x;
    return x + 130 > W ? x - 130 : x + 8;
});

const tooltipValue = computed(() => {
    if (hoverIndex.value < 0) {
        return '';
    }
    const v = values.value[hoverIndex.value];
    return props.metric === 'amount' ? formatMoney(v) : v.toLocaleString();
});

function formatAxisLabel(v: number): string {
    if (v >= 10000) {
        return (v / 10000).toFixed(1) + '万';
    }
    return Math.round(v).toString();
}

function formatMoney(v: number): string {
    if (v >= 10000) {
        return '¥' + (v / 10000).toFixed(2) + '万';
    }
    return '¥' + v.toLocaleString();
}

function onMouseMove(e: MouseEvent) {
    const svg = e.currentTarget as SVGElement;
    const rect = svg.getBoundingClientRect();
    const scaleX = W / rect.width;
    const mouseX = (e.clientX - rect.left) * scaleX;
    // 找到最近的点
    let minDist = Infinity;
    let idx = -1;
    points.value.forEach((p, i) => {
        const dist = Math.abs(p.x - mouseX);
        if (dist < minDist) {
            minDist = dist;
            idx = i;
        }
    });
    hoverIndex.value = idx;
}
</script>

<style scoped>
.trend-chart-wrap {
    width: 100%;
    min-height: 280px;
}

.trend-svg {
    width: 100%;
    height: 280px;
    display: block;
}

.chart-empty {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 280px;
    color: var(--color-text-secondary, #909399);
    font-size: 14px;
}

.axis-text {
    font-size: 11px;
    fill: #909399;
}

.tooltip-text {
    font-size: 12px;
    fill: #fff;
}
</style>
