<template>
  <div v-loading="loading" class="page-container">
    <div class="page-header">
      <h2>交易流水</h2>
      <el-button @click="loadData"><el-icon><RefreshRight /></el-icon>刷新</el-button>
    </div>
    <el-card class="page-card">
      <div class="search-bar">
        <el-input v-model="searchForm.keyword" placeholder="搜索交易号/订单号/用户" style="width: 220px" clearable @keyup.enter="handleSearch">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="searchForm.type" placeholder="交易类型" clearable style="width: 120px">
          <el-option label="支付" value="payment" /><el-option label="退款" value="refund" />
          <el-option label="结算" value="settlement" /><el-option label="平台费" value="platform_fee" />
          <el-option label="质保金" value="deposit" />
        </el-select>
        <el-select v-model="searchForm.channel" placeholder="支付渠道" clearable style="width: 120px">
          <el-option label="微信" value="wechat" /><el-option label="支付宝" value="alipay" />
          <el-option label="PayPal" value="paypal" /><el-option label="Stripe" value="stripe" />
          <el-option label="余额" value="balance" />
        </el-select>
        <el-select v-model="searchForm.status" placeholder="状态" clearable style="width: 100px">
          <el-option label="成功" value="success" /><el-option label="待处理" value="pending" />
          <el-option label="失败" value="failed" />
        </el-select>
        <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" style="width: 250px" @change="handleSearch" />
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
      <el-table :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="transactionNo" label="交易流水号" min-width="170" />
        <el-table-column prop="orderNo" label="关联订单" min-width="150" />
        <el-table-column label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="getTypeTag(row.type)" size="small">{{ getTypeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="100" align="right">
          <template #default="{ row }">¥{{ row.amount.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="手续费" width="80" align="right">
          <template #default="{ row }">¥{{ row.fee }}</template>
        </el-table-column>
        <el-table-column label="净额" width="100" align="right">
          <template #default="{ row }">¥{{ row.netAmount.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="付款方" width="100">
          <template #default="{ row }">{{ row.payerName }}</template>
        </el-table-column>
        <el-table-column label="收款方" width="100">
          <template #default="{ row }">{{ row.payeeName }}</template>
        </el-table-column>
        <el-table-column label="渠道" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="getChannelTag(row.channel)" size="small">{{ getChannelText(row.channel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="160" prop="createTime" />
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
import { ElMessage } from 'element-plus';
import { listTransactions, type TransactionInfo, type FinanceQueryParams } from '@/api/finance';

const loading = ref(false);
const tableData = ref<TransactionInfo[]>([]);
const pagination = reactive({ current: 1, size: 10, total: 0 });
const searchForm = reactive<FinanceQueryParams>({ keyword: '', type: '', channel: '', status: '' });
const dateRange = ref<[Date, Date] | null>(null);

function getTypeTag(t: string) {
  const m: Record<string, string> = { payment: 'success', refund: 'danger', settlement: 'primary', platform_fee: 'warning', deposit: 'info' };
  return m[t] || 'info';
}
function getTypeText(t: string) {
  const m: Record<string, string> = { payment: '支付', refund: '退款', settlement: '结算', platform_fee: '平台费', deposit: '质保金' };
  return m[t] || t;
}
function getChannelTag(c: string) { return c === 'wechat' ? 'success' : c === 'alipay' ? 'primary' : c === 'paypal' ? 'warning' : c === 'stripe' ? 'danger' : 'info'; }
function getChannelText(c: string) {
  const m: Record<string, string> = { wechat: '微信', alipay: '支付宝', paypal: 'PayPal', stripe: 'Stripe', balance: '余额' };
  return m[c] || c;
}
function getStatusTag(s: string) {
  const m: Record<string, string> = { success: 'success', pending: 'warning', failed: 'danger', cancelled: 'info' };
  return m[s] || 'info';
}
function getStatusText(s: string) {
  const m: Record<string, string> = { success: '成功', pending: '待处理', failed: '失败', cancelled: '已取消' };
  return m[s] || s;
}

async function loadData() {
  loading.value = true;
  try {
    const params: FinanceQueryParams = { ...searchForm, current: pagination.current, size: pagination.size };
    if (dateRange.value) {
      params.startDate = dateRange.value[0].toISOString().slice(0, 10);
      params.endDate = dateRange.value[1].toISOString().slice(0, 10);
    }
    const res = await listTransactions(params);
    tableData.value = res.records;
    pagination.total = res.total;
  } catch { ElMessage.error('加载交易流水失败'); } finally { loading.value = false; }
}

function handleSearch() { pagination.current = 1; loadData(); }
function handleReset() { searchForm.keyword = ''; searchForm.type = ''; searchForm.channel = ''; searchForm.status = ''; dateRange.value = null; pagination.current = 1; loadData(); }

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