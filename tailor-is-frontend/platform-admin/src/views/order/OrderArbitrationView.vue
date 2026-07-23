<template>
  <div v-loading="loading" class="page-container">
    <div class="page-header">
      <h2>售后仲裁</h2>
      <el-tag type="warning" size="large">{{ pendingCount }} 条待处理</el-tag>
    </div>
    <el-card class="page-card">
      <el-table :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="orderNo" label="订单号" width="170" />
        <el-table-column prop="userName" label="用户" width="100" />
        <el-table-column prop="merchantName" label="商家" min-width="150" />
        <el-table-column prop="reason" label="仲裁原因" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applyTime" label="申请时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="handleView(row)">查看详情</el-button>
            <template v-if="row.status === 'pending'">
              <el-button type="success" text size="small" @click="handleHandle(row, 'user_wins')">支持用户</el-button>
              <el-button type="warning" text size="small" @click="handleHandle(row, 'merchant_wins')">支持商家</el-button>
            </template>
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

    <el-dialog v-model="detailVisible" title="仲裁详情" width="560px">
      <el-descriptions v-if="currentItem" :column="1" border>
        <el-descriptions-item label="订单号">{{ currentItem.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="用户">{{ currentItem.userName }}</el-descriptions-item>
        <el-descriptions-item label="商家">{{ currentItem.merchantName }}</el-descriptions-item>
        <el-descriptions-item label="仲裁原因">{{ currentItem.reason }}</el-descriptions-item>
        <el-descriptions-item label="详细描述">{{ currentItem.description }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentItem.status)" size="small">{{ getStatusText(currentItem.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-if="currentItem.result" label="处理结果">{{ currentItem.result }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ currentItem.applyTime }}</el-descriptions-item>
        <el-descriptions-item v-if="currentItem.handleTime" label="处理时间">{{ currentItem.handleTime }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <template v-if="currentItem?.status === 'pending'">
          <el-button type="success" @click="detailVisible = false; handleHandle(currentItem!, 'user_wins')">支持用户</el-button>
          <el-button type="warning" @click="detailVisible = false; handleHandle(currentItem!, 'merchant_wins')">支持商家</el-button>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { listArbitrations, handleArbitration, type ArbitrationInfo } from '@/api/order';

const loading = ref(false);
const tableData = ref<ArbitrationInfo[]>([]);
const pagination = reactive({ current: 1, size: 10, total: 0 });
const detailVisible = ref(false);
const currentItem = ref<ArbitrationInfo | null>(null);

const pendingCount = computed(() => tableData.value.filter(a => a.status === 'pending').length);

function getStatusType(status: string) {
  const map: Record<string, string> = { pending: 'warning', user_wins: 'success', merchant_wins: 'info', cancelled: 'info' };
  return map[status] || 'info';
}
function getStatusText(status: string) {
  const map: Record<string, string> = { pending: '待处理', user_wins: '支持用户', merchant_wins: '支持商家', cancelled: '已取消' };
  return map[status] || status;
}

async function loadData() {
  loading.value = true;
  try {
    const res = await listArbitrations({ current: pagination.current, size: pagination.size });
    tableData.value = res.records;
    pagination.total = res.total;
  } catch { ElMessage.error('加载仲裁列表失败'); } finally { loading.value = false; }
}

function handleView(row: ArbitrationInfo) { currentItem.value = row; detailVisible.value = true; }

async function handleHandle(row: ArbitrationInfo, result: 'user_wins' | 'merchant_wins') {
  const actionText = result === 'user_wins' ? '支持用户' : '支持商家';
  try {
    const { value: remark } = await ElMessageBox.prompt(`请输入${actionText}的处理意见`, '仲裁处理', {
      confirmButtonText: '确定', cancelButtonText: '取消', inputPlaceholder: '请输入处理意见...',
    });
    await handleArbitration(row.id, result, remark || '');
    row.status = result;
    row.result = remark || '';
    row.handleTime = new Date().toISOString().replace('T', ' ').slice(0, 19);
    ElMessage.success(`仲裁处理完成: ${actionText}`);
  } catch { /* 取消 */ }
}

onMounted(() => { loadData(); });
</script>

<style scoped>
.page-container { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 18px; font-weight: 600; }
.page-card { margin-bottom: 0; }
.pagination { margin-top: 20px; text-align: right; }
</style>