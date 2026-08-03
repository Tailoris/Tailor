<template>
  <div
    v-loading="loading"
    class="page-container"
  >
    <div class="page-header">
      <h2>账单记录</h2>
    </div>
    <el-card class="page-card">
      <div class="search-bar">
        <el-input
          v-model="searchForm.merchantId"
          placeholder="商家ID"
          style="width: 140px"
          clearable
        />
        <el-select
          v-model="billTypeTab"
          placeholder="账单类型"
          style="width: 160px"
          @change="handleBillTypeChange"
        >
          <el-option
            label="全部"
            value=""
          />
          <el-option
            label="结算"
            value="settlement"
          />
          <el-option
            label="提现"
            value="withdraw"
          />
          <el-option
            label="保障金流水"
            value="deposit_flow"
          />
          <el-option
            label="账户流水"
            value="transaction"
          />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 260px"
          value-format="YYYY-MM-DD"
        />
        <el-button
          type="primary"
          @click="handleSearch"
        >
          搜索
        </el-button>
        <el-button @click="handleReset">
          重置
        </el-button>
      </div>
      <el-table
        :data="tableData"
        border
        stripe
      >
        <el-table-column
          prop="billNo"
          label="账单号"
          width="170"
        />
        <el-table-column
          label="类型"
          width="110"
          align="center"
        >
          <template #default="{ row }">
            <el-tag
              :type="billTypeTag(row.billType)"
              size="small"
            >
              {{ billTypeText(row.billType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="merchantName"
          label="商家名称"
          min-width="160"
        />
        <el-table-column
          label="金额"
          width="120"
          align="right"
        >
          <template #default="{ row }">
            <span :class="row.direction === 1 ? 'text-success' : 'text-danger'">
              {{ row.direction === 1 ? '+' : '-' }}¥{{ Math.abs(row.amount).toFixed(2) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column
          label="方向"
          width="80"
          align="center"
        >
          <template #default="{ row }">
            <span
              :class="row.direction === 1 ? 'text-success' : 'text-danger'"
              style="font-size: 18px; font-weight: bold"
            >
              {{ row.direction === 1 ? '↑' : '↓' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column
          prop="balanceBefore"
          label="变动前余额"
          width="130"
          align="right"
        />
        <el-table-column
          prop="balanceAfter"
          label="变动后余额"
          width="130"
          align="right"
        />
        <el-table-column
          prop="relatedNo"
          label="关联业务号"
          width="150"
        />
        <el-table-column
          prop="remark"
          label="备注"
          min-width="150"
          show-overflow-tooltip
        />
        <el-table-column
          prop="createTime"
          label="时间"
          width="170"
        />
      </el-table>
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        class="pagination"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { listMerchantBillPage, type BillRecord } from '@/api/merchant';

const loading = ref(false);
const tableData = ref<BillRecord[]>([]);
const pagination = reactive({ current: 1, size: 10, total: 0 });
const dateRange = ref<string[]>([]);
const billTypeTab = ref('');
const searchForm = reactive({
  merchantId: '',
});

function billTypeText(type: string) {
  const map: Record<string, string> = { settlement: '结算', withdraw: '提现', deposit_flow: '保障金', transaction: '账户流水' };
  return map[type] || type;
}
function billTypeTag(type: string) {
  const map: Record<string, string> = { settlement: 'success', withdraw: 'warning', deposit_flow: 'primary', transaction: 'info' };
  return map[type] || 'info';
}

async function loadData() {
  loading.value = true;
  try {
    const params: Record<string, unknown> = {
      current: pagination.current,
      size: pagination.size,
      merchantId: searchForm.merchantId || undefined,
      billType: billTypeTab.value || undefined,
    };
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0];
      params.endDate = dateRange.value[1];
    }
    const res = await listMerchantBillPage(params);
    tableData.value = res.records;
    pagination.total = res.total;
  } catch (e) {
    ElMessage.error('加载账单记录失败');
  } finally {
    loading.value = false;
  }
}

function handleBillTypeChange() {
  pagination.current = 1;
  loadData();
}
function handleSearch() {
  pagination.current = 1;
  loadData();
}
function handleReset() {
  searchForm.merchantId = '';
  billTypeTab.value = '';
  dateRange.value = [];
  pagination.current = 1;
  loadData();
}

onMounted(loadData);
</script>

<style scoped>
.page-container { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: 20px; font-weight: 600; color: #1f2937; }
.page-card { border-radius: 8px; }
.search-bar { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
.text-success { color: #67c23a; font-weight: 500; }
.text-danger { color: #f56c6c; font-weight: 500; }
</style>
