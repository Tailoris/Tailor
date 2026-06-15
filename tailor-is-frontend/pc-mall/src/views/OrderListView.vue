<template>
  <div class="order-list-view">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>我的订单</el-breadcrumb-item>
    </el-breadcrumb>

    <h2 class="page-title">我的订单</h2>

    <el-tabs v-model="activeTab" @tab-click="handleTabClick">
      <el-tab-pane label="全部" name="" />
      <el-tab-pane label="待付款" name="0" />
      <el-tab-pane label="待发货" name="1" />
      <el-tab-pane label="待收货" name="2" />
      <el-tab-pane label="已完成" name="3" />
      <el-tab-pane label="已取消" name="4" />
    </el-tabs>

    <el-skeleton :loading="loading" animated :rows="3">
      <template v-if="orders.length > 0">
        <div v-for="order in orders" :key="order.id" class="order-card">
          <div class="order-header">
            <span class="order-no">订单号: {{ order.orderNo }}</span>
            <span class="order-time">{{ formatDate(order.createdAt) }}</span>
            <el-tag :type="getStatusType(order.status)">{{ formatOrderStatus(order.status) }}</el-tag>
          </div>
          <div class="order-items">
            <div v-for="item in order.items" :key="item.id" class="order-item">
              <img :src="item.productImage || 'https://via.placeholder.com/60x60'" loading="lazy" />
              <div class="item-info">
                <span class="item-name">{{ item.productName }}</span>
                <span class="item-attrs">{{ Object.entries(item.skuAttributes).map(([k, v]) => `${k}: ${v}`).join('; ') }}</span>
              </div>
              <span class="item-price">{{ formatPrice(item.price) }}</span>
              <span class="item-qty">x{{ item.quantity }}</span>
              <span class="item-subtotal">{{ formatPrice(item.subtotal) }}</span>
            </div>
          </div>
          <div class="order-footer">
            <span class="order-total">
              共 {{ order.items?.length || 0 }} 件商品，合计：
              <em>{{ formatPrice(order.payAmount) }}</em>
            </span>
            <div class="order-actions">
              <el-button v-if="order.status === 0" type="danger" size="small" @click="handlePay(order.orderNo)">付款</el-button>
              <el-button v-if="order.status === 0" size="small" @click="handleCancel(order.orderNo)">取消</el-button>
              <el-button v-if="order.status === 2" type="primary" size="small" @click="handleConfirm(order.orderNo)">确认收货</el-button>
              <el-button size="small" @click="$router.push(`/order/${order.orderNo}`)">查看详情</el-button>
            </div>
          </div>
        </div>
        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            layout="total, prev, pager, next"
            @current-change="loadOrders"
          />
        </div>
      </template>
      <el-empty v-else-if="!loading" description="暂无订单" />
    </el-skeleton>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrders, payOrder, cancelOrder, confirmOrder } from '@/api/order'
import { formatDate, formatOrderStatus, formatPrice } from '@/utils/format'
import type { Order } from '@/types'

const orders = ref<Order[]>([])
const loading = ref(true)
const activeTab = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

async function loadOrders() {
  loading.value = true
  try {
    const res = await getOrders({
      status: activeTab.value ? Number(activeTab.value) : undefined,
      current: currentPage.value,
      size: pageSize.value
    })
    orders.value = res.records
    total.value = res.total
  } catch {
    orders.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleTabClick() {
  currentPage.value = 1
  loadOrders()
}

async function handlePay(orderNo: string) {
  try {
    await payOrder(orderNo)
    ElMessage.success('支付成功')
    loadOrders()
  } catch {
    ElMessage.error('支付失败')
  }
}

async function handleCancel(orderNo: string) {
  try {
    await ElMessageBox.confirm('确定要取消该订单吗？', '提示', { type: 'warning' })
    await cancelOrder(orderNo)
    ElMessage.success('订单已取消')
    loadOrders()
  } catch {
    // cancelled or error
  }
}

async function handleConfirm(orderNo: string) {
  try {
    await ElMessageBox.confirm('确认已收到货物？', '确认收货', { type: 'info' })
    await confirmOrder(orderNo)
    ElMessage.success('已确认收货')
    loadOrders()
  } catch {
    // cancelled or error
  }
}

function getStatusType(status: number) {
  const map: Record<number, string> = {
    0: 'warning',
    1: '',
    2: 'primary',
    3: 'success',
    4: 'info'
  }
  return map[status] || 'info'
}

onMounted(() => {
  loadOrders()
})
</script>

<style scoped>
.order-list-view {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}
.el-breadcrumb {
  margin-bottom: 20px;
}
.page-title {
  font-size: 24px;
  color: #333;
  margin: 0 0 20px;
}
.order-card {
  background: #fff;
  border-radius: 8px;
  margin-bottom: 16px;
  overflow: hidden;
}
.order-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #f5f5f5;
  font-size: 14px;
}
.order-no {
  color: #333;
  font-weight: 500;
}
.order-time {
  color: #999;
}
.order-items {
  padding: 16px;
}
.order-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid #eee;
}
.order-item:last-child {
  border-bottom: none;
}
.order-item img {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
}
.item-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.item-name {
  font-size: 14px;
  color: #333;
}
.item-attrs {
  font-size: 12px;
  color: #999;
}
.item-price, .item-qty {
  font-size: 14px;
  color: #666;
  min-width: 60px;
  text-align: center;
}
.item-subtotal {
  font-size: 14px;
  color: #f5222d;
  font-weight: 600;
  min-width: 80px;
  text-align: right;
}
.order-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-top: 1px solid #eee;
}
.order-total {
  font-size: 14px;
  color: #333;
}
.order-total em {
  color: #f5222d;
  font-weight: 600;
  font-style: normal;
}
.order-actions {
  display: flex;
  gap: 8px;
}
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
