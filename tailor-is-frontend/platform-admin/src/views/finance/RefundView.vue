<template>
  <div v-loading="loading" class="page-container">
    <div class="page-header">
      <h2>退款管理</h2>
      <el-button type="primary" @click="loadData"><el-icon><RefreshRight /></el-icon>刷新</el-button>
    </div>
    <el-card class="page-card">
      <div class="search-bar">
        <el-input v-model="searchForm.keyword" placeholder="搜索退款单号/订单号/用户" style="width: 220px" clearable @keyup.enter="handleSearch">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="searchForm.status" placeholder="状态" clearable style="width: 120px">
          <el-option label="待处理" value="pending" /><el-option label="已批准" value="approved" />
          <el-option label="已驳回" value="rejected" /><el-option label="已完成" value="completed" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
      <el-table :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="65" />
        <el-table-column prop="refundNo" label="退款单号" min-width="150" />
        <el-table-column prop="orderNo" label="关联订单" min-width="140" />
        <el-table-column prop="userName" label="申请人" width="100" />
        <el-table-column label="退款金额" width="100" align="right">
          <template #default="{ row }"><strong style="color: #ef4444">¥{{ row.amount.toLocaleString() }}</strong></template>
        </el-table-column>
        <el-table-column label="退款原因" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.reason }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="155" prop="applyTime" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'pending'" type="success" size="small" @click="handleApprove(row)">批准退款</el-button>
            <el-button v-if="row.status === 'pending'" type="danger" size="small" @click="handleReject(row)">驳回</el-button>
            <el-button v-if="row.status === 'approved'" type="primary" size="small" @click="handleComplete(row)">确认退款</el-button>
            <el-tag v-if="row.status === 'completed'" type="success" size="small">已完成</el-tag>
            <el-tag v-if="row.status === 'rejected'" type="danger" size="small">已驳回</el-tag>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { Search, RefreshRight } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { listRefunds, approveRefund, rejectRefund, completeRefund, type RefundInfo, type FinanceQueryParams } from '@/api/finance';
import { recordAuditLog, auditActions, createFinanceAuditLog } from '@/utils/audit';

const loading = ref(false);
const tableData = ref<RefundInfo[]>([]);
const pagination = reactive({ current: 1, size: 10, total: 0 });
const searchForm = reactive<FinanceQueryParams>({ keyword: '', status: '' });

function getStatusTag(s: string) { const m: Record<string, string> = { pending: 'warning', approved: 'primary', rejected: 'danger', completed: 'success' }; return m[s] || 'info'; }
function getStatusText(s: string) { const m: Record<string, string> = { pending: '待处理', approved: '已批准', rejected: '已驳回', completed: '已完成' }; return m[s] || s; }

async function loadData() {
  loading.value = true;
  try {
    const res = await listRefunds({ ...searchForm, current: pagination.current, size: pagination.size });
    tableData.value = res.records; pagination.total = res.total;
  } catch { ElMessage.error('加载退款列表失败'); } finally { loading.value = false; }
}

function handleSearch() { pagination.current = 1; loadData(); }
function handleReset() { searchForm.keyword = ''; searchForm.status = ''; pagination.current = 1; loadData(); }

async function handleApprove(row: RefundInfo) {
  try {
    const { value } = await ElMessageBox.prompt('请输入审批备注', '批准退款', { confirmButtonText: '确定批准', cancelButtonText: '取消', inputType: 'textarea', inputPlaceholder: '审批备注（可选）' });
    await approveRefund(row.id, value || '');
    row.status = 'approved';
    ElMessage.success(`已批准退款: ${row.refundNo}`);
    recordAuditLog(createFinanceAuditLog(auditActions.REFUND_APPROVE, 'refund', row.id, row.refundNo, `批准退款，金额: ¥${row.amount.toLocaleString()}`));
  } catch { /* 取消 */ }
}

async function handleReject(row: RefundInfo) {
  try {
    const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回退款', { confirmButtonText: '确定', cancelButtonText: '取消', inputType: 'textarea', inputValidator: (v) => v ? true : '请输入驳回原因', inputErrorMessage: '驳回原因不能为空' });
    await rejectRefund(row.id, value!);
    row.status = 'rejected';
    ElMessage.success(`已驳回退款: ${row.refundNo}`);
    recordAuditLog(createFinanceAuditLog(auditActions.REFUND_REJECT, 'refund', row.id, row.refundNo, `驳回退款，原因: ${value}`));
  } catch { /* 取消 */ }
}

async function handleComplete(row: RefundInfo) {
  try {
    await ElMessageBox.confirm(`确定完成退款 ¥${row.amount.toLocaleString()} 给 "${row.userName}" 吗？`, '确认退款', { confirmButtonText: '确定退款', cancelButtonText: '取消', type: 'warning' });
    await completeRefund(row.id);
    row.status = 'completed';
    ElMessage.success(`退款已完成: ${row.refundNo}`);
    recordAuditLog(createFinanceAuditLog(auditActions.REFUND_APPROVE, 'refund', row.id, row.refundNo, `退款完成，金额: ¥${row.amount.toLocaleString()}`));
  } catch { /* 取消 */ }
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
</style>