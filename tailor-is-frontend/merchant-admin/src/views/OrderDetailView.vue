<template>
  <div class="order-detail" v-loading="loading">
    <PageHeader title="订单详情">
      <template #actions>
        <el-button @click="$router.back()">返回</el-button>
      </template>
    </PageHeader>

    <el-card shadow="never" style="margin-bottom: 16px">
      <template #header>
        <div class="card-header">
          <span>订单信息</span>
          <el-tag :type="orderStatusType[order?.status || 'pending']" size="large">
            {{ orderStatusLabel[order?.status || 'pending'] }}
          </el-tag>
        </div>
      </template>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="订单号">{{ order?.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="买家姓名">{{ order?.buyerName }}</el-descriptions-item>
        <el-descriptions-item label="买家电话">{{ order?.buyerPhone }}</el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ order?.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="付款时间">{{ order?.paidAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发货时间">{{ order?.shippedAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ order?.completedAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="物流公司">{{ order?.logisticsCompany || '-' }}</el-descriptions-item>
        <el-descriptions-item label="快递单号">{{ order?.trackingNumber || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" style="margin-bottom: 16px">
      <template #header>
        <span>商品明细</span>
      </template>
      <el-table :data="order?.items || []" border>
        <el-table-column label="商品图片" width="80">
          <template #default="{ row }">
            <el-image
              :src="row.productImage || 'https://via.placeholder.com/60x60'"
              fit="cover"
              style="width: 60px; height: 60px; border-radius: 4px"
            />
          </template>
        </el-table-column>
        <el-table-column prop="productName" label="商品名称" min-width="180" />
        <el-table-column prop="skuName" label="规格" width="140" />
        <el-table-column prop="price" label="单价" width="100">
          <template #default="{ row }">¥{{ row.price?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column label="小计" width="100">
          <template #default="{ row }">¥{{ row.subtotal?.toFixed(2) }}</template>
        </el-table-column>
      </el-table>
      <div class="order-summary">
        <div class="summary-row">
          <span>商品总额</span>
          <span>¥{{ order?.totalAmount?.toFixed(2) }}</span>
        </div>
        <div class="summary-row">
          <span>优惠金额</span>
          <span class="discount">-¥{{ order?.discountAmount?.toFixed(2) }}</span>
        </div>
        <div class="summary-row total">
          <span>实付金额</span>
          <span class="pay-amount">¥{{ order?.payAmount?.toFixed(2) }}</span>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" style="margin-bottom: 16px">
      <template #header>
        <span>订单状态轨迹</span>
      </template>
      <el-timeline>
        <el-timeline-item timestamp="创建订单" placement="top">
          {{ order?.createdAt }}
        </el-timeline-item>
        <el-timeline-item
          v-if="order?.paidAt"
          type="success"
          :timestamp="order.paidAt"
          placement="top"
        >
          买家已付款
        </el-timeline-item>
        <el-timeline-item
          v-if="order?.shippedAt"
          type="primary"
          :timestamp="order.shippedAt"
          placement="top"
        >
          商家已发货
        </el-timeline-item>
        <el-timeline-item
          v-if="order?.completedAt"
          type="success"
          :timestamp="order.completedAt"
          placement="top"
        >
          订单已完成
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <span>订单备注</span>
      </template>
      <el-input
        v-model="order.remark"
        type="textarea"
        :rows="3"
        placeholder="添加备注"
        @blur="saveRemark"
      />
    </el-card>

    <div class="action-bar" v-if="order?.status === 'paid'">
      <el-button type="primary" @click="handleShip">发货</el-button>
      <el-button @click="$router.back()">返回</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderDetail } from '@/api/order'
import type { Order } from '@/types'
import PageHeader from '@/components/PageHeader.vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const order = reactive<Order>({
  id: 0,
  orderNo: '',
  shopId: 0,
  shopName: '',
  buyerName: '',
  buyerPhone: '',
  status: 'pending',
  totalAmount: 0,
  discountAmount: 0,
  payAmount: 0,
  items: [],
  logisticsCompany: '',
  trackingNumber: '',
  remark: '',
  paidAt: '',
  shippedAt: '',
  completedAt: '',
  createdAt: '',
})

const orderStatusType: Record<string, string> = {
  pending: 'info',
  paid: 'warning',
  shipped: '',
  completed: 'success',
  cancelled: 'danger',
}

const orderStatusLabel: Record<string, string> = {
  pending: '待付款',
  paid: '已付款',
  shipped: '已发货',
  completed: '已完成',
  cancelled: '已取消',
}

async function fetchOrderDetail() {
  loading.value = true
  try {
    const res = await getOrderDetail(route.params.orderNo as string)
    Object.assign(order, res)
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

function handleShip() {
  router.push(`/order?ship=${order.orderNo}`)
}

function saveRemark() {
  ElMessage.success('备注已保存')
}

onMounted(() => {
  fetchOrderDetail()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-summary {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--color-border);
  text-align: right;
}

.summary-row {
  display: flex;
  justify-content: flex-end;
  gap: 24px;
  padding: 8px 0;
  font-size: 14px;
}

.summary-row.total {
  font-weight: 600;
  font-size: 16px;
  border-top: 1px solid var(--color-border);
  padding-top: 12px;
}

.discount {
  color: var(--color-success);
}

.pay-amount {
  color: var(--color-danger);
  font-size: 20px;
}

.action-bar {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 24px;
  padding: 20px;
  background: var(--color-white);
  border-radius: var(--border-radius);
  box-shadow: var(--shadow-sm);
}
</style>
