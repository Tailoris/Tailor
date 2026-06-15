<template>
  <div class="price-summary">
    <h3>费用明细</h3>
    <div class="summary-item">
      <span>商品合计</span>
      <span>{{ formatPrice(subtotal) }}</span>
    </div>
    <div class="summary-item">
      <span>优惠金额</span>
      <span class="discount">-{{ formatPrice(discount) }}</span>
    </div>
    <div class="summary-item">
      <span>运费</span>
      <span>{{ formatPrice(shipping) }}</span>
    </div>
    <div class="summary-divider"></div>
    <div class="summary-total">
      <span>应付总额</span>
      <span class="total-price">{{ formatPrice(total) }}</span>
    </div>
    <el-button type="primary" size="large" class="submit-btn" @click="$emit('submit')" :loading="submitting">
      提交订单
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { formatPrice } from '@/utils/format'

defineProps<{
  subtotal: number
  discount: number
  shipping: number
  total: number
  submitting: boolean
}>()

defineEmits<{
  (e: 'submit'): void
}>()
</script>

<style scoped>
.price-summary {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  position: sticky;
  top: 80px;
}
.price-summary h3 {
  font-size: 16px;
  margin: 0 0 16px;
}
.summary-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 14px;
  color: #666;
}
.summary-item .discount {
  color: #f5222d;
}
.summary-divider {
  border-top: 1px solid #eee;
  margin: 16px 0;
}
.summary-total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.summary-total .total-price {
  font-size: 24px;
  color: #f5222d;
  font-weight: 700;
}
.submit-btn {
  width: 100%;
}
</style>
