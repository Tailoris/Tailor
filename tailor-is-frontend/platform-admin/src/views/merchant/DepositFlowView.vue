<template>
  <div
    v-loading="loading"
    class="page-container"
  >
    <div class="page-header">
      <h2>保障金流水</h2>
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
          v-model="searchForm.flowType"
          placeholder="流水类型"
          clearable
          style="width: 150px"
        >
          <el-option
            label="缴纳"
            :value="1"
          />
          <el-option
            label="补缴"
            :value="2"
          />
          <el-option
            label="扣除"
            :value="3"
          />
          <el-option
            label="退还"
            :value="4"
          />
          <el-option
            label="释放冻结"
            :value="5"
          />
          <el-option
            label="增加"
            :value="6"
          />
          <el-option
            label="减少"
            :value="7"
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
          prop="flowNo"
          label="流水号"
          width="170"
        />
        <el-table-column
          prop="merchantId"
          label="商家ID"
          width="90"
        />
        <el-table-column
          label="商家名称"
          min-width="160"
        >
          <template #default="{ row }">
            {{ merchantMap[row.merchantId] || '-' }}
          </template>
        </el-table-column>
        <el-table-column
          label="流水类型"
          width="110"
        >
          <template #default="{ row }">
            <el-tag
              size="small"
              :type="flowTagType(row.flowType)"
            >
              {{ flowTypeText(row.flowType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="变动金额"
          width="120"
          align="right"
        >
          <template #default="{ row }">
            <span :class="row.amount > 0 ? 'text-success' : 'text-danger'">
              {{ row.amount > 0 ? '+' : '' }}{{ row.amount.toFixed(2) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column
          label="变动前"
          width="110"
          align="right"
        >
          <template #default="{ row }">
            {{ row.balanceBefore.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column
          label="变动后"
          width="110"
          align="right"
        >
          <template #default="{ row }">
            {{ row.balanceAfter.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column
          label="关联类型"
          width="110"
        >
          <template #default="{ row }">
            {{ relatedTypeText(row.relatedType) }}
          </template>
        </el-table-column>
        <el-table-column
          prop="relatedNo"
          label="关联单号"
          width="150"
          show-overflow-tooltip
        />
        <el-table-column
          label="操作人"
          width="100"
        >
          <template #default="{ row }">
            {{ row.operatorType === 1 ? '平台' : '商家' }}
          </template>
        </el-table-column>
        <el-table-column
          prop="remark"
          label="备注"
          min-width="140"
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
        :page-sizes="[10, 20, 50, 100]"
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
import { listDepositFlow, type DepositFlow } from '@/api/merchant';

const loading = ref(false);
const tableData = ref<DepositFlow[]>([]);
const pagination = reactive({ current: 1, size: 10, total: 0 });
const dateRange = ref<string[]>([]);
const searchForm = reactive({ merchantId: '', flowType: '' as number | string });
const merchantMap = ref<Record<number, string>>({});

const merchantNames = ['时尚工坊', '精品制衣', '创意设计工作室', '恒达服装厂', '米兰设计中心'];
for (let i = 1; i <= 25; i++) {
  merchantMap.value[i] = merchantNames[(i - 1) % 5] + (i > 5 ? ` #${i}` : '');
}

function flowTypeText(t: number) {
  return ['', '缴纳', '补缴', '扣除', '退还', '释放冻结', '增加', '减少'][t] || '未知';
}
function flowTagType(t: number) {
  return ['', 'success', 'warning', 'danger', 'primary', 'info', 'success', 'danger'][t] || 'info';
}
function relatedTypeText(t: number) {
  return ['', '入驻缴纳', '订单赔付', '违规处罚', '退款冻结', '提现'][t] || '-';
}

async function loadData() {
  loading.value = true;
  try {
    const params: Record<string, unknown> = { ...searchForm, current: pagination.current, size: pagination.size };
    if (dateRange.value?.length === 2) {
      params.startDate = dateRange.value[0];
      params.endDate = dateRange.value[1];
    }
    const res = await listDepositFlow(params);
    tableData.value = res.records;
    pagination.total = res.total;
  } catch {
    ElMessage.error('加载流水失败');
  } finally {
    loading.value = false;
  }
}

function handleSearch() { pagination.current = 1; loadData(); }
function handleReset() {
  Object.assign(searchForm, { merchantId: '', flowType: '' });
  dateRange.value = []; pagination.current = 1; loadData();
}

onMounted(() => { loadData(); });
</script>

<style scoped>
.page-container { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 18px; font-weight: 600; }
.search-bar { display: flex; gap: 12px; margin-bottom: 20px; flex-wrap: wrap; }
.pagination { margin-top: 20px; text-align: right; }
.text-success { color: #67c23a; }
.text-danger { color: #f56c6c; }
</style>
