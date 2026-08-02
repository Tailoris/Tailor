<template>
  <div class="ranking-list">
    <div
      v-if="data.length === 0"
      class="ranking-empty"
    >
      暂无排行数据
    </div>
    <div
      v-for="(item, i) in data"
      :key="i"
      class="rank-item"
    >
      <span
        class="rank-no"
        :class="{ top: i < 3 }"
        :style="i < 3 ? { backgroundColor: topColors[i] } : {}"
      >{{ i + 1 }}</span>
      <div class="rank-main">
        <div class="rank-header">
          <span
            class="rank-name"
            :title="item.name"
          >{{ item.name }}</span>
          <span class="rank-value">{{ formatValue(item.value) }}<span
            v-if="unit"
            class="rank-unit"
          >{{ unit }}</span></span>
        </div>
        <div class="rank-bar-wrap">
          <div
            class="rank-bar"
            :style="{
              width: getPercent(item.value) + '%',
              backgroundColor: i < 3 ? topColors[i] : '#6366f1'
            }"
          />
        </div>
        <div
          v-if="showCount"
          class="rank-count"
        >
          {{ countLabel }}：{{ item.count }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { RankItem } from '@/api/dashboard';

/**
 * 排行榜组件（纯 CSS 进度条样式）
 * @prop data 排行数据
 * @prop unit 主指标单位
 * @param showCount 是否显示次指标
 * @param countLabel 次指标标签
 */
const props = withDefaults(defineProps<{
    data: RankItem[];
    unit?: string;
    showCount?: boolean;
    countLabel?: string;
    money?: boolean;
}>(), {
    unit: '',
    showCount: true,
    countLabel: '订单数',
    money: true,
});

const topColors = ['#f59e0b', '#9ca3af', '#cd7f32'];

const maxVal = computed(() => {
    const m = Math.max(...props.data.map((d) => Number(d.value) || 0), 1);
    return m;
});

function getPercent(v: number): number {
    return Math.max(2, ((Number(v) || 0) / maxVal.value) * 100);
}

function formatValue(v: number): string {
    if (props.money) {
        if (v >= 10000) {
            return '¥' + (v / 10000).toFixed(1) + '万';
        }
        return '¥' + v.toLocaleString();
    }
    if (v >= 10000) {
        return (v / 10000).toFixed(1) + '万';
    }
    return v.toLocaleString();
}
</script>

<style scoped>
.ranking-list {
    display: flex;
    flex-direction: column;
    gap: 14px;
}

.ranking-empty {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 120px;
    color: var(--color-text-secondary, #909399);
    font-size: 14px;
}

.rank-item {
    display: flex;
    gap: 12px;
    align-items: flex-start;
}

.rank-no {
    width: 22px;
    height: 22px;
    border-radius: 6px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: 600;
    color: var(--color-text-secondary, #909399);
    background-color: #f0f0f0;
    flex-shrink: 0;
    margin-top: 2px;
}

.rank-no.top {
    color: #fff;
}

.rank-main {
    flex: 1;
    min-width: 0;
}

.rank-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 8px;
    margin-bottom: 6px;
}

.rank-name {
    font-size: 13px;
    color: var(--color-text-primary, #303133);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    flex: 1;
}

.rank-value {
    font-size: 14px;
    font-weight: 600;
    color: var(--color-text-primary, #303133);
    flex-shrink: 0;
}

.rank-unit {
    font-size: 12px;
    font-weight: 400;
    color: var(--color-text-secondary, #909399);
    margin-left: 2px;
}

.rank-bar-wrap {
    height: 6px;
    background-color: #f5f5f5;
    border-radius: 3px;
    overflow: hidden;
}

.rank-bar {
    height: 100%;
    border-radius: 3px;
    transition: width 0.6s ease;
}

.rank-count {
    font-size: 12px;
    color: var(--color-text-secondary, #909399);
    margin-top: 4px;
}
</style>
