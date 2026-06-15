<template>
  <div class="order-table">
    <el-table :data="orders" v-loading="loading" stripe style="width: 100%">
      <el-table-column prop="orderNo" label="订单号" width="180" show-overflow-tooltip />
      <el-table-column prop="buyerName" label="买家" width="120" />
      <el-table-column label="商品数量" width="100">
        <template #default="{ row }">
          {{ row.items?.length || 0 }}
        </template>
      </el-table-column>
      <el-table-column prop="payAmount" label="订单金额" width="120" sortable>
        <template #default="{ row }">
          <span class="amount">¥{{ row.payAmount?.toFixed(2) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType[row.status]" size="small">
            {{ statusLabel[row.status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="下单时间" width="160" sortable />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="$emit('view', row.orderNo)">
            查看
          </el-button>
          <el-button
            v-if="row.status === 'paid'"
            link
            type="success"
            size="small"
            @click="$emit('ship', row.orderNo)"
          >
            发货
          </el-button>
          <el-button link type="warning" size="small" @click="$emit('remark', row)">
            备注
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无数据" />
      </template>
    </el-table>
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="$emit('pageChange', { page: currentPage, pageSize })"
        @size-change="$emit('pageChange', { page: 1, pageSize })"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { Order } from '@/types'

defineProps<{
  orders: Order[]
  loading: boolean
  total: number
  currentPage: number
  pageSize: number
}>()

defineEmits<{
  view: [orderNo: string]
  ship: [orderNo: string]
  remark: [order: Order]
  pageChange: [params: { page: number; pageSize: number }]
}>()

const currentPage = ref(1)
const pageSize = ref(10)

const statusType: Record<string, string> = {
  pending: 'info',
  paid: 'warning',
  shipped: '',
  completed: 'success',
  cancelled: 'danger',
}

const statusLabel: Record<string, string> = {
  pending: '待付款',
  paid: '已付款',
  shipped: '已发货',
  completed: '已完成',
  cancelled: '已取消',
}
</script>

<style scoped>
.amount {
  color: var(--color-danger);
  font-weight: 500;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
