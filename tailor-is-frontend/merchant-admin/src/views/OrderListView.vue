<template>
  <div class="order-list">
    <PageHeader title="订单管理" />

    <el-card shadow="never" class="search-card">
      <el-tabs v-model="searchForm.status" @tab-change="handleSearch" style="margin-bottom: 16px">
        <el-tab-pane label="全部" name="" />
        <el-tab-pane label="待付款" name="pending" />
        <el-tab-pane label="已付款" name="paid" />
        <el-tab-pane label="已发货" name="shipped" />
        <el-tab-pane label="已完成" name="completed" />
        <el-tab-pane label="已取消" name="cancelled" />
      </el-tabs>
      <el-form :model="searchForm" inline>
        <el-form-item label="订单号">
          <el-input
            v-model="searchForm.orderNo"
            placeholder="请输入订单号"
            clearable
            style="width: 200px"
            @clear="handleSearch"
          />
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 240px"
            @change="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <OrderTable
        :orders="orders"
        :loading="loading"
        :total="total"
        :current-page="page"
        :page-size="pageSize"
        @view="handleView"
        @ship="handleShip"
        @remark="handleRemark"
        @page-change="handlePageChange"
      />
    </el-card>

    <el-dialog v-model="shipDialogVisible" title="发货" width="500px">
      <el-form :model="shipForm" label-width="100px">
        <el-form-item label="物流公司" prop="logisticsCompany">
          <el-select v-model="shipForm.logisticsCompany" placeholder="请选择物流公司" style="width: 100%">
            <el-option label="顺丰速运" value="SF" />
            <el-option label="中通快递" value="ZTO" />
            <el-option label="圆通速递" value="YTO" />
            <el-option label="韵达快递" value="YD" />
            <el-option label="申通快递" value="STO" />
            <el-option label="邮政EMS" value="EMS" />
            <el-option label="京东物流" value="JD" />
          </el-select>
        </el-form-item>
        <el-form-item label="快递单号" prop="trackingNumber">
          <el-input v-model="shipForm.trackingNumber" placeholder="请输入快递单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmShip" :loading="shipLoading">确认发货</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="remarkDialogVisible" title="订单备注" width="500px">
      <el-input
        v-model="remarkContent"
        type="textarea"
        :rows="4"
        placeholder="请输入备注内容"
      />
      <template #footer>
        <el-button @click="remarkDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmRemark">保存备注</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listOrders, shipOrder } from '@/api/order'
import type { Order } from '@/types'
import PageHeader from '@/components/PageHeader.vue'
import OrderTable from '@/components/OrderTable.vue'
import { ElMessage } from 'element-plus'

const router = useRouter()

const loading = ref(false)
const orders = ref<Order[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const dateRange = ref<[string, string] | null>(null)

const searchForm = reactive({
  status: '',
  orderNo: '',
})

async function fetchOrders() {
  loading.value = true
  try {
    const res = await listOrders({
      status: searchForm.status || undefined,
      orderNo: searchForm.orderNo || undefined,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1],
      current: page.value,
      size: pageSize.value,
    })
    orders.value = res.records
    total.value = res.total
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  fetchOrders()
}

function resetSearch() {
  searchForm.status = ''
  searchForm.orderNo = ''
  dateRange.value = null
  handleSearch()
}

function handlePageChange(params: { page: number; pageSize: number }) {
  page.value = params.page
  pageSize.value = params.pageSize
  fetchOrders()
}

function handleView(orderNo: string) {
  router.push(`/order/${orderNo}`)
}

const shipDialogVisible = ref(false)
const shipLoading = ref(false)
const currentShipOrderNo = ref('')
const shipForm = reactive({
  logisticsCompany: '',
  trackingNumber: '',
})

function handleShip(orderNo: string) {
  currentShipOrderNo.value = orderNo
  shipForm.logisticsCompany = ''
  shipForm.trackingNumber = ''
  shipDialogVisible.value = true
}

async function confirmShip() {
  if (!shipForm.logisticsCompany || !shipForm.trackingNumber) {
    ElMessage.warning('请填写完整物流信息')
    return
  }
  shipLoading.value = true
  try {
    await shipOrder(currentShipOrderNo.value, shipForm)
    ElMessage.success('发货成功')
    shipDialogVisible.value = false
    fetchOrders()
  } catch {
    // error handled by interceptor
  } finally {
    shipLoading.value = false
  }
}

const remarkDialogVisible = ref(false)
const remarkContent = ref('')

function handleRemark(order: Order) {
  remarkContent.value = order.remark || ''
  remarkDialogVisible.value = true
}

function confirmRemark() {
  ElMessage.success('备注保存成功')
  remarkDialogVisible.value = false
}

onMounted(() => {
  fetchOrders()
})
</script>

<style scoped>
.search-card :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.search-card :deep(.el-form-item) {
  margin-bottom: 0;
}
</style>
