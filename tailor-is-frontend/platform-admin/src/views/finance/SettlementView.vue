<template>
  <div v-loading="loading" class="page-container">
    <div class="page-header">
      <h2>结算管理</h2>
      <el-button type="primary" @click="loadData"><el-icon><RefreshRight /></el-icon>刷新</el-button>
    </div>
    <el-card class="page-card">
      <div class="search-bar">
        <el-input v-model="searchForm.keyword" placeholder="搜索结算单号/商家" style="width: 220px" clearable @keyup.enter="handleSearch">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="searchForm.status" placeholder="状态" clearable style="width: 120px">
          <el-option label="待审核" value="pending" /><el-option label="已批准" value="approved" />
          <el-option label="已打款" value="paid" /><el-option label="已驳回" value="rejected" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
      <el-table :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="65" />
        <el-table-column prop="settlementNo" label="结算单号" min-width="150" />
        <el-table-column prop="merchantName" label="商家名称" min-width="130" />
        <el-table-column label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="getMerchantTypeTag(row.merchantType)" size="small">{{ getMerchantTypeText(row.merchantType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="period" label="结算周期" width="120" />
        <el-table-column label="订单数" width="70" align="center">
          <template #default="{ row }">{{ row.orderCount }}单</template>
        </el-table-column>
        <el-table-column label="交易总额" width="110" align="right">
          <template #default="{ row }">¥{{ row.totalAmount.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="平台费" width="90" align="right">
          <template #default="{ row }">¥{{ row.platformFee.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="结算金额" width="110" align="right">
          <template #default="{ row }"><strong>¥{{ row.settlementAmount.toLocaleString() }}</strong></template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="155" prop="applyTime" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'pending'" type="success" size="small" @click="handleApprove(row)">批准</el-button>
            <el-button v-if="row.status === 'pending'" type="danger" size="small" @click="handleReject(row)">驳回</el-button>
            <el-button v-if="row.status === 'approved'" type="primary" size="small" @click="handlePay(row)">打款</el-button>
            <el-tag v-if="row.status === 'paid'" type="success" size="small">已打款</el-tag>
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
import { listSettlements, approveSettlement, rejectSettlement, paySettlement, type SettlementInfo, type FinanceQueryParams } from '@/api/finance';
import { recordAuditLog, auditActions, createFinanceAuditLog } from '@/utils/audit';

const loading = ref(false);
const tableData = ref<SettlementInfo[]>([]);
const pagination = reactive({ current: 1, size: 10, total: 0 });
const searchForm = reactive<FinanceQueryParams>({ keyword: '', status: '' });

function getMerchantTypeTag(t: string) { const m: Record<string, string> = { designer: 'warning', pattern_maker: 'primary', shop: 'success' }; return m[t] || 'info'; }
function getMerchantTypeText(t: string) { const m: Record<string, string> = { designer: '设计师', pattern_maker: '版师', shop: '店铺' }; return m[t] || t; }
function getStatusTag(s: string) { const m: Record<string, string> = { pending: 'warning', approved: 'primary', paid: 'success', rejected: 'danger' }; return m[s] || 'info'; }
function getStatusText(s: string) { const m: Record<string, string> = { pending: '待审核', approved: '已批准', paid: '已打款', rejected: '已驳回' }; return m[s] || s; }

async function loadData() {
  loading.value = true;
  try {
    const res = await listSettlements({ ...searchForm, current: pagination.current, size: pagination.size });
    tableData.value = res.records; pagination.total = res.total;
  } catch { ElMessage.error('加载结算列表失败'); } finally { loading.value = false; }
}

function handleSearch() { pagination.current = 1; loadData(); }
function handleReset() { searchForm.keyword = ''; searchForm.status = ''; pagination.current = 1; loadData(); }

async function handleApprove(row: SettlementInfo) {
  try {
    await ElMessageBox.confirm(`确定批准结算 "${row.settlementNo}" 吗？结算金额 ¥${row.settlementAmount.toLocaleString()}`, '批准结算', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' });
    await approveSettlement(row.id);
    row.status = 'approved';
    ElMessage.success(`已批准结算: ${row.settlementNo}`);
    recordAuditLog(createFinanceAuditLog(auditActions.SETTLEMENT_APPROVE, 'settlement', row.id, row.settlementNo, `批准结算，金额: ¥${row.settlementAmount.toLocaleString()}`));
  } catch { /* 取消 */ }
}

async function handleReject(row: SettlementInfo) {
  try {
    const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回结算', { confirmButtonText: '确定', cancelButtonText: '取消', inputType: 'textarea' });
    await rejectSettlement(row.id, value || '');
    row.status = 'rejected';
    ElMessage.success(`已驳回结算: ${row.settlementNo}`);
    recordAuditLog(createFinanceAuditLog(auditActions.SETTLEMENT_REJECT, 'settlement', row.id, row.settlementNo, `驳回结算，原因: ${value || '无'}`));
  } catch { /* 取消 */ }
}

async function handlePay(row: SettlementInfo) {
  try {
    await ElMessageBox.confirm(`确定向 "${row.merchantName}" 打款 ¥${row.settlementAmount.toLocaleString()} 吗？`, '确认打款', { confirmButtonText: '确定打款', cancelButtonText: '取消', type: 'warning' });
    await paySettlement(row.id);
    row.status = 'paid';
    ElMessage.success(`已打款: ${row.settlementNo}`);
    recordAuditLog(createFinanceAuditLog(auditActions.SETTLEMENT_APPROVE, 'settlement', row.id, row.settlementNo, `打款成功，金额: ¥${row.settlementAmount.toLocaleString()}`));
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