<template>
  <div class="order-detail-view" v-loading="loading">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item :to="{ path: '/orders' }">我的订单</el-breadcrumb-item>
      <el-breadcrumb-item>订单详情</el-breadcrumb-item>
    </el-breadcrumb>

    <template v-if="order">
      <div class="order-header-card">
        <div class="header-left">
          <el-tag :type="getStatusType(order.status)" size="large">{{ formatOrderStatus(order.status) }}</el-tag>
          <div class="header-info">
            <span class="order-no">订单号: {{ order.orderNo }}</span>
            <span class="order-time">下单时间: {{ formatDate(order.createdAt) }}</span>
          </div>
        </div>
        <div class="header-actions">
          <el-button v-if="order.status === 0" type="danger" @click="handlePay">立即付款</el-button>
          <el-button v-if="order.status === 0" @click="handleCancel">取消订单</el-button>
          <el-button v-if="order.status === 2" type="primary" @click="handleConfirm">确认收货</el-button>
        </div>
      </div>

      <div class="detail-grid">
        <section class="section products-section">
          <h3>商品清单</h3>
          <el-table :data="order.items || []" border>
            <el-table-column label="商品信息">
              <template #default="{ row }">
                <div class="product-cell">
                  <img :src="row.productImage || 'https://via.placeholder.com/50x50'" />
                  <span>{{ row.productName }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="SKU属性">
              <template #default="{ row }">
                {{ Object.entries(row.skuAttributes).map(([k, v]) => `${k}: ${v}`).join(', ') }}
              </template>
            </el-table-column>
            <el-table-column prop="price" label="单价" width="100">
              <template #default="{ row }">{{ formatPrice(row.price) }}</template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" width="80" />
            <el-table-column prop="subtotal" label="小计" width="100">
              <template #default="{ row }">{{ formatPrice(row.subtotal) }}</template>
            </el-table-column>
          </el-table>
        </section>

        <section class="section address-section">
          <h3>收货信息</h3>
          <div class="address-info" v-if="order.addressSnapshot">
            <p>{{ order.addressSnapshot }}</p>
          </div>
          <el-empty v-else description="暂无地址信息" />
        </section>

        <section class="section logistics-section">
          <h3>物流信息</h3>
          <el-timeline>
            <el-timeline-item timestamp="2024-01-01 12:00" placement="top" color="#1d39c4">
              <h4>订单已提交</h4>
              <p>等待付款</p>
            </el-timeline-item>
            <el-timeline-item timestamp="2024-01-01 12:05" placement="top" v-if="order.status >= 1">
              <h4>已付款</h4>
              <p>等待发货</p>
            </el-timeline-item>
            <el-timeline-item timestamp="2024-01-02 09:00" placement="top" v-if="order.status >= 2">
              <h4>已发货</h4>
              <p>快递公司: 顺丰快递, 运单号: SF1234567890</p>
            </el-timeline-item>
            <el-timeline-item timestamp="2024-01-03 14:00" placement="top" v-if="order.status >= 3">
              <h4>已签收</h4>
              <p>感谢您的购买</p>
            </el-timeline-item>
          </el-timeline>
        </section>

        <section class="section price-section">
          <h3>费用明细</h3>
          <div class="price-list">
            <div class="price-item">
              <span>商品总额</span>
              <span>{{ formatPrice(order.totalAmount) }}</span>
            </div>
            <div class="price-item">
              <span>优惠金额</span>
              <span class="discount">-{{ formatPrice(order.discountAmount) }}</span>
            </div>
            <div class="price-divider"></div>
            <div class="price-item total">
              <span>实付金额</span>
              <span>{{ formatPrice(order.payAmount) }}</span>
            </div>
            <div class="price-item" v-if="order.paidAt">
              <span>付款时间</span>
              <span>{{ formatDate(order.paidAt) }}</span>
            </div>
            <div class="price-item" v-if="order.payType">
              <span>支付方式</span>
              <span>{{ order.payType === 1 ? '微信支付' : order.payType === 2 ? '支付宝' : '其他' }}</span>
            </div>
          </div>
        </section>
      </div>

      <div v-if="order.remark" class="section remark-section">
        <h3>订单备注</h3>
        <p>{{ order.remark }}</p>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderDetail, payOrder, cancelOrder, confirmOrder } from '@/api/order'
import { formatDate, formatOrderStatus, formatPrice } from '@/utils/format'
import type { Order } from '@/types'

const props = defineProps<{
  id: string
}>()

const order = ref<Order | null>(null)
const loading = ref(true)

async function loadOrder() {
  try {
    order.value = await getOrderDetail(props.id)
  } catch {
    order.value = null
  } finally {
    loading.value = false
  }
}

async function handlePay() {
  try {
    await payOrder(props.id)
    ElMessage.success('支付成功')
    loadOrder()
  } catch {
    ElMessage.error('支付失败')
  }
}

async function handleCancel() {
  try {
    await ElMessageBox.confirm('确定要取消该订单吗？', '提示', { type: 'warning' })
    await cancelOrder(props.id)
    ElMessage.success('订单已取消')
    loadOrder()
  } catch {
    // cancelled or error
  }
}

async function handleConfirm() {
  try {
    await ElMessageBox.confirm('确认已收到货物？', '确认收货', { type: 'info' })
    await confirmOrder(props.id)
    ElMessage.success('已确认收货')
    loadOrder()
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
  loadOrder()
})
</script>

<style scoped>
.order-detail-view {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  min-height: 400px;
}
.el-breadcrumb {
  margin-bottom: 20px;
}
.order-header-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 16px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.header-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.order-no {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}
.order-time {
  font-size: 12px;
  color: #999;
}
.header-actions {
  display: flex;
  gap: 12px;
}
.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}
.section {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
}
.section h3 {
  font-size: 16px;
  color: #333;
  margin: 0 0 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #eee;
}
.product-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}
.product-cell img {
  width: 50px;
  height: 50px;
  object-fit: cover;
  border-radius: 4px;
}
.address-info p {
  font-size: 14px;
  color: #666;
  line-height: 1.8;
}
.price-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.price-item {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #666;
}
.price-item .discount {
  color: #f5222d;
}
.price-item.total {
  font-size: 18px;
  color: #333;
  font-weight: 600;
}
.price-item.total span:last-child {
  color: #f5222d;
}
.price-divider {
  border-top: 1px solid #eee;
}
.remark-section p {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
}
</style>
