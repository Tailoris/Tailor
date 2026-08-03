<template>
  <div
    v-loading="loading"
    class="page-container"
  >
    <div class="page-header">
      <h2>商户提现审核</h2>
    </div>
    <el-card class="page-card">
      <div class="search-bar">
        <el-input
          v-model="searchForm.merchantName"
          placeholder="商家名称模糊搜索"
          style="width: 220px"
          clearable
        />
        <el-select
          v-model="searchForm.status"
          placeholder="状态"
          clearable
          style="width: 150px"
        >
          <el-option
            label="待审核"
            :value="0"
          />
          <el-option
            label="审核中"
            :value="1"
          />
          <el-option
            label="提现成功"
            :value="2"
          />
          <el-option
            label="提现失败"
            :value="3"
          />
          <el-option
            label="已驳回"
            :value="4"
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
          prop="withdrawNo"
          label="提现单号"
          width="170"
        />
        <el-table-column
          prop="merchantName"
          label="商家名称"
          min-width="160"
        />
        <el-table-column
          prop="amount"
          label="提现金额"
          width="120"
          align="right"
        />
        <el-table-column
          prop="fee"
          label="手续费"
          width="100"
          align="right"
        />
        <el-table-column
          prop="actualAmount"
          label="实际到账"
          width="120"
          align="right"
        />
        <el-table-column
          prop="bankName"
          label="开户银行"
          width="130"
        />
        <el-table-column
          label="账号"
          width="180"
        >
          <template #default="{ row }">
            {{ formatBankCard(row.bankAccount) }}
          </template>
        </el-table-column>
        <el-table-column
          prop="accountName"
          label="开户人"
          width="100"
        />
        <el-table-column
          label="状态"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-tag
              :type="statusTagType(row.status)"
              size="small"
            >
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="createTime"
          label="提交时间"
          width="170"
        />
        <el-table-column
          prop="auditBy"
          label="审核人"
          width="90"
        />
        <el-table-column
          prop="auditTime"
          label="审核时间"
          width="170"
        />
        <el-table-column
          label="操作"
          width="220"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              type="primary"
              text
              size="small"
              @click="handleView(row)"
            >
              详情
            </el-button>
            <el-button
              v-if="row.status === 0"
              type="success"
              text
              size="small"
              @click="handleApprove(row)"
            >
              通过
            </el-button>
            <el-button
              v-if="row.status === 0"
              type="danger"
              text
              size="small"
              @click="handleReject(row)"
            >
              驳回
            </el-button>
          </template>
        </el-table-column>
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

    <el-dialog
      v-model="detailVisible"
      title="提现详情"
      width="640px"
    >
      <el-descriptions
        v-if="currentItem"
        :column="2"
        border
      >
        <el-descriptions-item
          label="提现单号"
          :span="2"
        >
          {{ currentItem.withdrawNo }}
        </el-descriptions-item>
        <el-descriptions-item label="商家ID">
          {{ currentItem.merchantId }}
        </el-descriptions-item>
        <el-descriptions-item label="商家名称">
          {{ currentItem.merchantName }}
        </el-descriptions-item>
        <el-descriptions-item
          label="提现金额"
          align="right"
          class="amount"
        >
          ¥{{ currentItem.amount.toFixed(2) }}
        </el-descriptions-item>
        <el-descriptions-item
          label="手续费"
          align="right"
        >
          ¥{{ currentItem.fee.toFixed(2) }}
        </el-descriptions-item>
        <el-descriptions-item
          label="实际到账"
          align="right"
          :span="2"
          class="amount"
        >
          ¥{{ currentItem.actualAmount.toFixed(2) }}
        </el-descriptions-item>
        <el-descriptions-item label="开户银行">
          {{ currentItem.bankName }}
        </el-descriptions-item>
        <el-descriptions-item label="开户支行">
          {{ currentItem.bankBranch }}
        </el-descriptions-item>
        <el-descriptions-item
          label="银行账号"
          :span="2"
        >
          <span>{{ showFullBank ? currentItem.bankAccount : formatBankCard(currentItem.bankAccount) }}</span>
          <el-button
            v-if="!showFullBank"
            text
            type="primary"
            size="small"
            @click="confirmShowFull"
          >
            查看完整
          </el-button>
        </el-descriptions-item>
        <el-descriptions-item label="开户人姓名">
          {{ currentItem.accountName }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag
            :type="statusTagType(currentItem.status)"
            size="small"
          >
            {{ statusText(currentItem.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="提交时间">
          {{ currentItem.createTime }}
        </el-descriptions-item>
        <el-descriptions-item label="审核人">
          {{ currentItem.auditBy || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="审核时间">
          {{ currentItem.auditTime || '-' }}
        </el-descriptions-item>
        <el-descriptions-item
          v-if="currentItem.auditRemark"
          label="审核备注"
          :span="2"
        >
          {{ currentItem.auditRemark }}
        </el-descriptions-item>
        <el-descriptions-item
          v-if="currentItem.failReason"
          label="失败原因"
          :span="2"
        >
          {{ currentItem.failReason }}
        </el-descriptions-item>
        <el-descriptions-item
          v-if="currentItem.channelTransactionId"
          label="渠道单号"
          :span="2"
        >
          {{ currentItem.channelTransactionId }}
        </el-descriptions-item>
        <el-descriptions-item
          v-if="currentItem.processedAt"
          label="处理时间"
          :span="2"
        >
          {{ currentItem.processedAt }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">
          关闭
        </el-button>
        <template v-if="currentItem?.status === 0">
          <el-button
            type="success"
            @click="detailVisible = false; handleApprove(currentItem!)"
          >
            通过
          </el-button>
          <el-button
            type="danger"
            @click="detailVisible = false; handleReject(currentItem!)"
          >
            驳回
          </el-button>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  listWithdrawPage, getWithdrawById, auditWithdraw,
  formatBankCard, type WithdrawRecord,
} from '@/api/merchant';

const loading = ref(false);
const tableData = ref<WithdrawRecord[]>([]);
const pagination = reactive({ current: 1, size: 10, total: 0 });
const dateRange = ref<string[]>([]);
const searchForm = reactive({ merchantName: '', status: '' as number | string });

const detailVisible = ref(false);
const currentItem = ref<WithdrawRecord | null>(null);
const showFullBank = ref(false);

function statusTagType(s: number) {
  return ['warning', 'primary', 'success', 'danger', 'info'][s] || 'info';
}
function statusText(s: number) {
  return ['待审核', '审核中', '提现成功', '提现失败', '已驳回'][s] || '未知';
}

async function loadData() {
  loading.value = true;
  try {
    const params: Record<string, unknown> = { ...searchForm, current: pagination.current, size: pagination.size };
    if (dateRange.value?.length === 2) {
      params.startDate = dateRange.value[0];
      params.endDate = dateRange.value[1];
    }
    const res = await listWithdrawPage(params);
    tableData.value = res.records;
    pagination.total = res.total;
  } catch {
    ElMessage.error('加载提现列表失败');
  } finally {
    loading.value = false;
  }
}

function handleSearch() { pagination.current = 1; loadData(); }
function handleReset() {
  Object.assign(searchForm, { merchantName: '', status: '' });
  dateRange.value = []; pagination.current = 1; loadData();
}

async function handleView(row: WithdrawRecord) {
  showFullBank.value = false;
  try {
    currentItem.value = await getWithdrawById(row.id);
  } catch {
    currentItem.value = row;
  }
  detailVisible.value = true;
}

async function confirmShowFull() {
  try {
    await ElMessageBox.confirm('查看完整银行账号将记录操作日志，确定继续？', '敏感信息确认', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
    });
    showFullBank.value = true;
  } catch { /* 取消 */ }
}

async function handleApprove(row: WithdrawRecord) {
  try {
    await ElMessageBox.confirm(`确定通过 ${row.merchantName} 金额 ¥${row.amount} 的提现申请吗？`, '审核通过确认', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
    });
    await auditWithdraw({ withdrawId: row.id, approved: true });
    row.status = 1;
    ElMessage.success('审核通过');
  } catch { /* 取消 */ }
}

async function handleReject(row: WithdrawRecord) {
  try {
    const { value: remark } = await ElMessageBox.prompt('请输入驳回原因', '驳回审核', {
      confirmButtonText: '确定', cancelButtonText: '取消',
      inputPlaceholder: '请输入驳回原因...',
    });
    await auditWithdraw({ withdrawId: row.id, approved: false, remark });
    row.status = 4;
    ElMessage.success('已驳回');
  } catch { /* 取消 */ }
}

onMounted(() => { loadData(); });
</script>

<style scoped>
.page-container { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 18px; font-weight: 600; }
.search-bar { display: flex; gap: 12px; margin-bottom: 20px; flex-wrap: wrap; }
.pagination { margin-top: 20px; text-align: right; }
.amount { font-weight: 600; color: #f56c6c; font-size: 15px; }
</style>
