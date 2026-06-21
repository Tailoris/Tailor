<template>
  <div class="aftersale-list">
    <PageHeader title="售后工单" />

    <el-card shadow="never" class="search-card">
      <el-tabs v-model="searchForm.status" @tab-change="handleSearch" style="margin-bottom: 16px">
        <el-tab-pane label="全部" name="" />
        <el-tab-pane label="待处理" name="pending" />
        <el-tab-pane label="处理中" name="processing" />
        <el-tab-pane label="已完成" name="completed" />
        <el-tab-pane label="已拒绝" name="rejected" />
      </el-tabs>
      <el-form :model="searchForm" inline>
        <el-form-item label="工单号">
          <el-input
            v-model="searchForm.ticketNo"
            placeholder="请输入工单号"
            clearable
            style="width: 200px"
            @clear="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <el-table :data="tickets" v-loading="loading" stripe>
        <el-table-column prop="ticketNo" label="工单号" width="180" show-overflow-tooltip />
        <el-table-column prop="orderNo" label="关联订单" width="180" show-overflow-tooltip />
        <el-table-column prop="buyerName" label="买家" width="120" />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            {{ typeLabel[row.type] }}
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="200" show-overflow-tooltip />
        <el-table-column label="退款金额" width="120">
          <template #default="{ row }">
            <span class="amount">¥{{ row.refundAmount?.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType[row.status]" size="small">
              {{ statusLabel[row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="申请时间" width="160" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="$router.push(`/aftersale/${row.ticketNo}`)">
              {{ row.status === 0 ? '处理' : '查看' }}
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无数据" />
        </template>
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="fetchTickets"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { listTickets } from '@/api/aftersale'
import type { AfterSaleTicket } from '@/types'
import PageHeader from '@/components/PageHeader.vue'

const loading = ref(false)
const tickets = ref<AfterSaleTicket[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const searchForm = reactive({
  status: '',
  ticketNo: '',
})

const typeLabel: Record<string, string> = {
  refund_only: '仅退款',
  return_and_refund: '退货退款',
  exchange: '换货',
}

const statusType: Record<number, string> = {
  0: 'warning',
  1: '',
  2: 'success',
  3: 'danger',
  4: 'info',
}

const statusLabel: Record<number, string> = {
  0: '待处理',
  1: '处理中',
  2: '已同意',
  3: '已拒绝',
  4: '已完成',
}

async function fetchTickets() {
  loading.value = true
  try {
    const res = await listTickets({
      status: searchForm.status || undefined,
      ticketNo: searchForm.ticketNo || undefined,
      current: page.value,
      size: pageSize.value,
    })
    tickets.value = res.records
    total.value = res.total
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  fetchTickets()
}

function resetSearch() {
  searchForm.status = ''
  searchForm.ticketNo = ''
  handleSearch()
}

function handleSizeChange() {
  page.value = 1
  fetchTickets()
}

onMounted(() => {
  fetchTickets()
})
</script>

<style scoped>
.search-card :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.search-card :deep(.el-form-item) {
  margin-bottom: 0;
}

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
