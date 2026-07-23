<template>
  <div v-loading="loading" class="page-container">
    <div class="page-header">
      <h2>订单管理</h2>
    </div>
    <el-card class="page-card">
      <div class="search-bar">
        <el-input v-model="searchForm.orderNo" placeholder="搜索订单号" style="width: 200px" clearable @keyup.enter="handleSearch">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-input v-model="searchForm.keyword" placeholder="搜索用户/商品/商家" style="width: 200px" clearable @keyup.enter="handleSearch">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="searchForm.status" placeholder="状态筛选" clearable style="width: 130px">
          <el-option label="待支付" value="pending" />
          <el-option label="已支付" value="paid" />
          <el-option label="处理中" value="processing" />
          <el-option label="配送中" value="delivering" />
          <el-option label="已完成" value="completed" />
          <el-option label="已取消" value="cancelled" />
          <el-option label="退款中" value="refunding" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
      <el-table :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="orderNo" label="订单号" width="170" />
        <el-table-column prop="userName" label="用户" width="100" />
        <el-table-column prop="productName" label="商品" min-width="180" show-overflow-tooltip />
        <el-table-column prop="merchantName" label="商家" min-width="150" />
        <el-table-column label="金额" width="110" align="right">
          <template #default="{ row }">
            <span class="price">¥{{ row.totalAmount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="支付方式" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.paymentMethod ? '' : 'info'">{{ row.paymentMethod || '未支付' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="handleView(row)">查看</el-button>
            <el-dropdown v-if="canUpdateStatus(row.status)" @command="(cmd: string) => handleUpdateStatus(row, cmd)">
              <el-button type="warning" text size="small">更新状态<el-icon><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="row.status === 'pending'" command="paid">标记已支付</el-dropdown-item>
                  <el-dropdown-item v-if="row.status === 'paid'" command="processing">开始处理</el-dropdown-item>
                  <el-dropdown-item v-if="row.status === 'processing'" command="delivering">开始配送</el-dropdown-item>
                  <el-dropdown-item v-if="row.status === 'delivering'" command="completed">完成订单</el-dropdown-item>
                  <el-dropdown-item v-if="['pending', 'paid'].includes(row.status)" command="cancelled">取消订单</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pagination.current" v-model:page-size="pagination.size"
        :total="pagination.total" :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next" class="pagination"
        @size-change="loadData" @current-change="loadData"
      />
    </el-card>

    <el-dialog v-model="detailVisible" title="订单详情" width="600px">
      <el-descriptions v-if="currentItem" :column="1" border>
        <el-descriptions-item label="订单号">{{ currentItem.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="用户">{{ currentItem.userName }}</el-descriptions-item>
        <el-descriptions-item label="商品">{{ currentItem.productName }}</el-descriptions-item>
        <el-descriptions-item label="商家">{{ currentItem.merchantName }}</el-descriptions-item>
        <el-descriptions-item label="订单金额">¥{{ currentItem.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="已付金额">¥{{ currentItem.paidAmount }}</el-descriptions-item>
        <el-descriptions-item label="支付方式">{{ currentItem.paymentMethod || '未支付' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentItem.status)" size="small">{{ getStatusText(currentItem.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="备注">{{ currentItem.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentItem.createTime }}</el-descriptions-item>
        <el-descriptions-item v-if="currentItem.payTime" label="支付时间">{{ currentItem.payTime }}</el-descriptions-item>
        <el-descriptions-item v-if="currentItem.completeTime" label="完成时间">{{ currentItem.completeTime }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { Search, ArrowDown } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { listOrders, updateOrderStatus, type OrderInfo, type OrderQueryParams } from '@/api/order';

const loading = ref(false);
const tableData = ref<OrderInfo[]>([]);
const pagination = reactive({ current: 1, size: 10, total: 0 });
const searchForm = reactive<OrderQueryParams>({ orderNo: '', status: '', keyword: '' });
const detailVisible = ref(false);
const currentItem = ref<OrderInfo | null>(null);

function getStatusType(status: string) {
  const map: Record<string, string> = {
    pending: 'warning', paid: 'primary', processing: '', delivering: 'success',
    completed: 'success', cancelled: 'info', refunding: 'danger',
  };
  return map[status] || 'info';
}
function getStatusText(status: string) {
  const map: Record<string, string> = {
    pending: '待支付', paid: '已支付', processing: '处理中', delivering: '配送中',
    completed: '已完成', cancelled: '已取消', refunding: '退款中',
  };
  return map[status] || status;
}
function canUpdateStatus(status: string) {
  return !['completed', 'cancelled', 'refunding'].includes(status);
}

async function loadData() {
  loading.value = true;
  try {
    const res = await listOrders({ ...searchForm, current: pagination.current, size: pagination.size });
    tableData.value = res.records;
    pagination.total = res.total;
  } catch { ElMessage.error('加载订单列表失败'); } finally { loading.value = false; }
}

function handleSearch() { pagination.current = 1; loadData(); }
function handleReset() { searchForm.orderNo = ''; searchForm.status = ''; searchForm.keyword = ''; pagination.current = 1; loadData(); }

function handleView(row: OrderInfo) { currentItem.value = row; detailVisible.value = true; }

async function handleUpdateStatus(row: OrderInfo, newStatus: string) {
  const statusText = getStatusText(newStatus);
  try {
    await updateOrderStatus(row.id, newStatus);
    row.status = newStatus as OrderInfo['status'];
    ElMessage.success(`订单状态已更新为: ${statusText}`);
  } catch { ElMessage.error('状态更新失败'); }
}

onMounted(() => { loadData(); });
</script>

<style scoped>
.page-container { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 18px; font-weight: 600; }
.search-bar { display: flex; gap: 12px; margin-bottom: 20px; flex-wrap: wrap; }
.page-card { margin-bottom: 0; }
.pagination { margin-top: 20px; text-align: right; }
.price { color: #ef4444; font-weight: 600; }
</style>