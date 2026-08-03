<template>
  <div
    v-loading="loading"
    class="page-container"
  >
    <div class="page-header">
      <h2>店铺保障金</h2>
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
          v-model="searchForm.status"
          placeholder="状态"
          clearable
          style="width: 150px"
        >
          <el-option
            label="未缴纳"
            :value="0"
          />
          <el-option
            label="已缴纳"
            :value="1"
          />
          <el-option
            label="不足"
            :value="2"
          />
          <el-option
            label="已退还"
            :value="3"
          />
        </el-select>
        <el-input-number
          v-model="searchForm.minDeposit"
          :min="0"
          placeholder="最低金额"
          style="width: 160px"
        />
        <el-input-number
          v-model="searchForm.maxDeposit"
          :min="0"
          placeholder="最高金额"
          style="width: 160px"
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
          prop="merchantId"
          label="商家ID"
          width="90"
        />
        <el-table-column
          prop="merchantName"
          label="商家名称"
          min-width="160"
        />
        <el-table-column
          prop="contactName"
          label="联系人"
          width="100"
        />
        <el-table-column
          label="联系电话"
          width="140"
        >
          <template #default="{ row }">
            {{ formatPhone(row.contactPhone) }}
          </template>
        </el-table-column>
        <el-table-column
          prop="depositAmount"
          label="总额"
          width="110"
        />
        <el-table-column
          prop="frozenAmount"
          label="冻结"
          width="100"
        />
        <el-table-column
          prop="availableAmount"
          label="可用"
          width="100"
        />
        <el-table-column
          prop="minDeposit"
          label="最低要求"
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
          label="操作"
          width="180"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              type="primary"
              text
              size="small"
              @click="handleViewDetail(row)"
            >
              详情
            </el-button>
            <el-button
              type="warning"
              text
              size="small"
              @click="handleAdjust(row)"
            >
              调整
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
      title="保障金详情"
      width="800px"
    >
      <el-tabs v-if="currentDeposit">
        <el-tab-pane
          label="基本信息"
          name="basic"
        >
          <el-descriptions
            :column="2"
            border
          >
            <el-descriptions-item label="商家ID">
              {{ currentDeposit.merchantId }}
            </el-descriptions-item>
            <el-descriptions-item label="商家名称">
              {{ currentDeposit.merchantName }}
            </el-descriptions-item>
            <el-descriptions-item label="联系人">
              {{ currentDeposit.contactName }}
            </el-descriptions-item>
            <el-descriptions-item label="联系电话">
              {{ formatPhone(currentDeposit.contactPhone) }}
            </el-descriptions-item>
            <el-descriptions-item label="保障金总额">
              {{ currentDeposit.depositAmount }}
            </el-descriptions-item>
            <el-descriptions-item label="冻结金额">
              {{ currentDeposit.frozenAmount }}
            </el-descriptions-item>
            <el-descriptions-item label="可用金额">
              {{ currentDeposit.availableAmount }}
            </el-descriptions-item>
            <el-descriptions-item label="最低要求">
              {{ currentDeposit.minDeposit }}
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag
                :type="statusTagType(currentDeposit.status)"
                size="small"
              >
                {{ statusText(currentDeposit.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">
              {{ currentDeposit.createTime }}
            </el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane
          label="流水记录"
          name="flows"
        >
          <el-table
            :data="flowRecords"
            border
            stripe
            size="small"
          >
            <el-table-column
              prop="flowNo"
              label="流水号"
              width="170"
            />
            <el-table-column
              label="类型"
              width="110"
            >
              <template #default="{ row }">
                {{ flowTypeText(row.flowType) }}
              </template>
            </el-table-column>
            <el-table-column
              label="变动金额"
              width="110"
            >
              <template #default="{ row }">
                <span :class="row.amount > 0 ? 'text-success' : 'text-danger'">
                  {{ row.amount > 0 ? '+' : '' }}{{ row.amount }}
                </span>
              </template>
            </el-table-column>
            <el-table-column
              prop="balanceBefore"
              label="变动前"
              width="110"
            />
            <el-table-column
              prop="balanceAfter"
              label="变动后"
              width="110"
            />
            <el-table-column
              label="关联单号"
              width="140"
            >
              <template #default="{ row }">
                {{ row.relatedNo || '-' }}
              </template>
            </el-table-column>
            <el-table-column
              prop="remark"
              label="备注"
              min-width="120"
              show-overflow-tooltip
            />
            <el-table-column
              prop="createTime"
              label="时间"
              width="170"
            />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <el-dialog
      v-model="adjustVisible"
      title="保障金调整"
      width="480px"
      @close="resetAdjustForm"
    >
      <el-form
        ref="adjustFormRef"
        :model="adjustForm"
        :rules="adjustRules"
        label-width="100px"
      >
        <el-form-item label="商家ID">
          <el-input
            v-model="adjustForm.merchantId"
            disabled
          />
        </el-form-item>
        <el-form-item
          label="调整类型"
          prop="adjustType"
        >
          <el-select
            v-model="adjustForm.adjustType"
            style="width: 100%"
          >
            <el-option
              label="补缴"
              :value="1"
            />
            <el-option
              label="调增"
              :value="2"
            />
            <el-option
              label="调减"
              :value="3"
            />
            <el-option
              label="退还"
              :value="4"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="调整金额"
          prop="amount"
        >
          <el-input-number
            v-model="adjustForm.amount"
            :min="0.01"
            :precision="2"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item
          label="关联单号"
          prop="relatedOrderNo"
        >
          <el-input
            v-model="adjustForm.relatedOrderNo"
            placeholder="可选"
            maxlength="64"
          />
        </el-form-item>
        <el-form-item
          label="备注"
          prop="remark"
        >
          <el-input
            v-model="adjustForm.remark"
            type="textarea"
            :rows="3"
            maxlength="200"
            placeholder="请输入备注（必填）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="adjustSaving"
          @click="handleAdjustSubmit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import {
  listDepositPage, getDepositDetail, adjustDeposit, listDepositFlow,
  formatPhone, type DepositRecord, type DepositFlow,
} from '@/api/merchant';

const loading = ref(false);
const tableData = ref<DepositRecord[]>([]);
const pagination = reactive({ current: 1, size: 10, total: 0 });
const searchForm = reactive({
  merchantId: '', status: '' as number | string,
  minDeposit: undefined as number | undefined, maxDeposit: undefined as number | undefined,
});

const detailVisible = ref(false);
const currentDeposit = ref<DepositRecord | null>(null);
const flowRecords = ref<DepositFlow[]>([]);

const adjustVisible = ref(false);
const adjustSaving = ref(false);
const adjustFormRef = ref<FormInstance>();
const adjustForm = reactive({ merchantId: 0, adjustType: 1, amount: 0, relatedOrderNo: '', remark: '' });
const adjustRules: FormRules = {
  adjustType: [{ required: true, message: '请选择调整类型', trigger: 'change' }],
  amount: [{ required: true, message: '请输入调整金额', trigger: 'blur' }],
  remark: [{ required: true, message: '请输入备注', trigger: 'blur' }],
};

function statusTagType(s: number) {
  return ['info', 'success', 'warning', 'danger'][s] || 'info';
}
function statusText(s: number) {
  return ['未缴纳', '已缴纳', '不足', '已退还'][s] || '未知';
}
function flowTypeText(t: number) {
  return ['', '缴纳', '补缴', '扣除', '退还', '释放冻结', '增加', '减少'][t] || '未知';
}

async function loadData() {
  loading.value = true;
  try {
    const params: Record<string, unknown> = { ...searchForm, current: pagination.current, size: pagination.size };
    const res = await listDepositPage(params);
    tableData.value = res.records;
    pagination.total = res.total;
  } catch {
    ElMessage.error('加载数据失败');
  } finally {
    loading.value = false;
  }
}

function handleSearch() { pagination.current = 1; loadData(); }
function handleReset() {
  Object.assign(searchForm, { merchantId: '', status: '', minDeposit: undefined, maxDeposit: undefined });
  pagination.current = 1; loadData();
}

async function handleViewDetail(row: DepositRecord) {
  try {
    const detail = await getDepositDetail(row.merchantId);
    currentDeposit.value = detail;
    flowRecords.value = detail.flows || [];
    if (flowRecords.value.length === 0) {
      try {
        const flowRes = await listDepositFlow({ merchantId: row.merchantId, current: 1, size: 50 });
        flowRecords.value = flowRes.records;
      } catch { flowRecords.value = []; }
    }
  } catch {
    currentDeposit.value = row;
    flowRecords.value = [];
  }
  detailVisible.value = true;
}

function handleAdjust(row: DepositRecord) {
  adjustForm.merchantId = row.merchantId;
  adjustForm.adjustType = 1;
  adjustForm.amount = 0;
  adjustForm.relatedOrderNo = '';
  adjustForm.remark = '';
  adjustVisible.value = true;
}

function resetAdjustForm() {
  adjustFormRef.value?.clearValidate();
}

async function handleAdjustSubmit() {
  if (!adjustFormRef.value) return;
  await adjustFormRef.value.validate(async (valid) => {
    if (!valid) return;
    adjustSaving.value = true;
    try {
      await adjustDeposit({ ...adjustForm });
      ElMessage.success('调整成功');
      adjustVisible.value = false;
      loadData();
    } catch {
      ElMessage.error('调整失败');
    } finally {
      adjustSaving.value = false;
    }
  });
}

onMounted(() => { loadData(); });
</script>

<style scoped>
.page-container { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 18px; font-weight: 600; }
.search-bar { display: flex; gap: 12px; margin-bottom: 20px; flex-wrap: wrap; align-items: center; }
.pagination { margin-top: 20px; text-align: right; }
.text-success { color: #67c23a !important; }
.text-danger { color: #f56c6c !important; }
</style>
