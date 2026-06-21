<template>
  <section class="section" aria-label="订单商品列表">
    <h3>订单商品</h3>
    <div class="items-header">
      <span class="col-product">商品信息</span>
      <span class="col-price">单价</span>
      <span class="col-quantity">数量</span>
      <span class="col-subtotal">小计</span>
    </div>
    <div v-for="item in items" :key="item.id" class="order-item">
      <div class="product-info">
        <img :src="item.productImage || 'https://via.placeholder.com/60x60'" :alt="item.productName" loading="lazy" />
        <div>
          <span class="product-name">{{ item.productName }}</span>
          <span class="sku-attrs">{{ Object.entries(item.skuAttributes).map(([k, v]) => `${k}: ${v}`).join('; ') }}</span>
        </div>
      </div>
      <span class="price">{{ formatPrice(item.price) }}</span>
      <span class="quantity">{{ item.quantity }}</span>
      <span class="subtotal">{{ formatPrice(item.price * item.quantity) }}</span>
    </div>
  </section>
</template>

<script setup lang="ts">
import { formatPrice } from '@/utils/format'
import type { CartItem } from '@/types'

defineProps<{
  items: CartItem[]
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
.items-header {
  display: flex;
  padding: 8px 0;
  border-bottom: 1px solid #eee;
  font-size: 14px;
  color: #666;
}
.order-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #eee;
}
.col-product { flex: 1; }
.col-price { width: 100px; text-align: center; }
.col-quantity { width: 80px; text-align: center; }
.col-subtotal { width: 100px; text-align: center; }
.product-info {
  flex: 1;
  display: flex;
  gap: 12px;
  align-items: center;
}
.product-info img {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
}
.product-info .product-name {
  display: block;
  font-size: 14px;
  color: #333;
}
.product-info .sku-attrs {
  display: block;
  font-size: 12px;
  color: #999;
}
.price, .quantity {
  width: 100px;
  text-align: center;
  font-size: 14px;
}
.subtotal {
  width: 100px;
  text-align: center;
  color: #f5222d;
  font-weight: 600;
}
</style>
