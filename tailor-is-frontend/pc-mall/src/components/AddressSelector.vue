<template>
  <section class="section" aria-label="选择收货地址">
    <h3>收货地址</h3>
    <div class="address-list" role="radiogroup" aria-label="收货地址列表">
      <div
        v-for="addr in addresses"
        :key="addr.id"
        :class="['address-card', { active: modelValue === addr.id }]"
        role="radio"
        :aria-checked="modelValue === addr.id"
        tabindex="0"
        @click="$emit('update:modelValue', addr.id)"
        @keydown.enter="$emit('update:modelValue', addr.id)"
        @keydown.space.prevent="$emit('update:modelValue', addr.id)"
      >
        <el-radio :model-value="modelValue" :label="addr.id" />
        <div class="address-info">
          <div class="address-header">
            <span class="name">{{ addr.name }}</span>
            <span class="phone">{{ addr.phone }}</span>
            <el-tag v-if="addr.isDefault === 1" size="small" type="primary">默认</el-tag>
          </div>
          <div class="address-detail">
            {{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detail }}
          </div>
        </div>
      </div>
    </div>
    <el-button type="primary" link @click="$emit('add')">+ 新增地址</el-button>
  </section>
</template>

<script setup lang="ts">
import type { Address } from '@/types'

defineProps<{
  addresses: Address[]
  modelValue?: number
}>()

defineEmits<{
  (e: 'update:modelValue', id: number): void
  (e: 'add'): void
}>()
</script>

<style scoped>
.section {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 16px;
}
.section h3 {
  font-size: 16px;
  color: #333;
  margin: 0 0 16px;
}
.address-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 12px;
}

/* Tablet: 2 columns */
@media (max-width: 1024px) {
  .address-list {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* Mobile: 1 column */
@media (max-width: 768px) {
  .address-list {
    grid-template-columns: 1fr;
  }
}
.address-card {
  border: 2px solid #eee;
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: border-color 0.2s;
}
.address-card.active {
  border-color: #1d39c4;
}
.address-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.address-header .name {
  font-weight: 600;
  color: #333;
}
.address-header .phone {
  color: #666;
  font-size: 14px;
}
.address-detail {
  font-size: 14px;
  color: #666;
  line-height: 1.5;
}
</style>
