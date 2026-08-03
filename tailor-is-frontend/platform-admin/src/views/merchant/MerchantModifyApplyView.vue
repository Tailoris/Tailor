<template>
  <div
    v-loading="loading"
    class="page-container"
  >
    <div class="page-header">
      <h2>修改申请</h2>
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
          v-model="searchForm.applyType"
          placeholder="申请类型"
          clearable
          style="width: 140px"
        >
          <el-option
            label="基本信息"
            :value="1"
          />
          <el-option
            label="资质变更"
            :value="2"
          />
          <el-option
            label="类目变更"
            :value="3"
          />
          <el-option
            label="入驻类型"
            :value="4"
          />
          <el-option
            label="其他"
            :value="5"
          />
        </el-select>
        <el-select
          v-model="searchForm.status"
          placeholder="状态"
          clearable
          style="width: 130px"
        >
          <el-option
            label="待审核"
            :value="0"
          />
          <el-option
            label="已通过"
            :value="1"
          />
          <el-option
            label="已驳回"
            :value="2"
          />
          <el-option
            label="已撤销"
            :value="3"
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
        <el-input
          v-model="searchForm.keyword"
          placeholder="单号模糊搜索"
          style="width: 180px"
          clearable
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
          prop="applyNo"
          label="单号"
          width="170"
        />
        <el-table-column
          prop="merchantId"
          label="商家ID"
          width="90"
        />
        <el-table-column
          label="申请类型"
          width="110"
        >
          <template #default="{ row }">
            <el-tag size="small">
              {{ applyTypeText(row.applyType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="状态"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-tag
              :type="statusType(row.status)"
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
      title="修改申请详情"
      width="900px"
    >
      <div
        v-if="currentItem"
        class="detail-wrapper"
      >
        <div class="detail-header">
          <div><span class="label">单号：</span>{{ currentItem.applyNo }}</div>
          <div><span class="label">申请类型：</span>{{ applyTypeText(currentItem.applyType) }}</div>
          <div>
            <span class="label">状态：</span>
            <el-tag
              :type="statusType(currentItem.status)"
              size="small"
            >
              {{ statusText(currentItem.status) }}
            </el-tag>
          </div>
          <div><span class="label">提交时间：</span>{{ currentItem.createTime }}</div>
        </div>
        <div class="reason-block">
          <div class="label">
            修改原因：
          </div>
          <div class="reason-content">
            {{ currentItem.reason }}
          </div>
        </div>
        <el-descriptions
          :column="2"
          border
          class="compare-desc"
        >
          <template
            v-for="key in fieldKeys"
            :key="key"
          >
            <el-descriptions-item :label="`原值 · ${fieldLabel(key)}`">
              {{ formatValue(currentItem.originalSnapshot[key]) }}
            </el-descriptions-item>
            <el-descriptions-item
              :label="`新值 · ${fieldLabel(key)}`"
              class="modified-col"
            >
              <span
                v-if="currentItem.originalSnapshot[key] !== currentItem.modifyContent[key]"
                class="modified-tag"
              >已变更</span>
              {{ formatValue(currentItem.modifyContent[key]) }}
            </el-descriptions-item>
          </template>
        </el-descriptions>
        <div
          v-if="currentItem.auditRemark"
          class="audit-block"
        >
          <div class="label">
            审核备注：
          </div>
          <div class="audit-content">
            {{ currentItem.auditRemark }}
          </div>
        </div>
      </div>
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
import { ref, reactive, onMounted, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  listModifyApplyPage, auditModifyApply, getModifyApplyById,
  type ModifyApply,
} from '@/api/merchant';

const loading = ref(false);
const tableData = ref<ModifyApply[]>([]);
const pagination = reactive({ current: 1, size: 10, total: 0 });
const dateRange = ref<string[]>([]);
const searchForm = reactive({
  merchantId: '', applyType: '' as number | string, status: '' as number | string, keyword: '',
});

const detailVisible = ref(false);
const currentItem = ref<ModifyApply | null>(null);

const fieldKeys = computed(() => {
  if (!currentItem.value) return [];
  const keys = new Set<string>();
  Object.keys(currentItem.value.originalSnapshot || {}).forEach(k => keys.add(k));
  Object.keys(currentItem.value.modifyContent || {}).forEach(k => keys.add(k));
  return Array.from(keys);
});

const fieldLabelMap: Record<string, string> = {
  contactName: '联系人', contactPhone: '联系电话', contactEmail: '联系邮箱',
  companyName: '公司名称', address: '地址', businessScope: '经营范围',
  shopCategoryId: '主营分类', applyTypeCode: '入驻类型',
};

function applyTypeText(t: number) {
  return ['', '基本信息', '资质变更', '类目变更', '入驻类型', '其他'][t] || '未知';
}
function statusType(s: number) {
  return ['warning', 'success', 'danger', 'info'][s] || 'info';
}
function statusText(s: number) {
  return ['待审核', '已通过', '已驳回', '已撤销'][s] || '未知';
}
function fieldLabel(k: string) { return fieldLabelMap[k] || k; }
function formatValue(v: unknown) {
  if (v === null || v === undefined || v === '') return '-';
  return String(v);
}

async function loadData() {
  loading.value = true;
  try {
    const params: Record<string, unknown> = { ...searchForm, current: pagination.current, size: pagination.size };
    if (dateRange.value?.length === 2) {
      params.startDate = dateRange.value[0];
      params.endDate = dateRange.value[1];
    }
    const res = await listModifyApplyPage(params);
    tableData.value = res.records;
    pagination.total = res.total;
  } catch {
    ElMessage.error('加载修改申请列表失败');
  } finally {
    loading.value = false;
  }
}

function handleSearch() { pagination.current = 1; loadData(); }
function handleReset() {
  Object.assign(searchForm, { merchantId: '', applyType: '', status: '', keyword: '' });
  dateRange.value = [];
  pagination.current = 1; loadData();
}

async function handleView(row: ModifyApply) {
  try {
    currentItem.value = await getModifyApplyById(row.id);
  } catch {
    currentItem.value = row;
  }
  detailVisible.value = true;
}

async function handleApprove(row: ModifyApply) {
  try {
    await ElMessageBox.confirm(`确定通过该修改申请吗？`, '审核通过确认', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
    });
    await auditModifyApply({ applyId: row.id, approved: true });
    row.status = 1;
    ElMessage.success('审核通过');
  } catch { /* 取消 */ }
}

async function handleReject(row: ModifyApply) {
  try {
    const { value: remark } = await ElMessageBox.prompt('请输入驳回原因', '驳回审核', {
      confirmButtonText: '确定', cancelButtonText: '取消',
      inputPlaceholder: '请输入驳回原因...',
    });
    await auditModifyApply({ applyId: row.id, approved: false, remark });
    row.status = 2;
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
.detail-wrapper { display: flex; flex-direction: column; gap: 16px; }
.detail-header { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px 20px; font-size: 14px; }
.label { color: #909399; }
.reason-block, .audit-block { padding: 12px; background: #f5f7fa; border-radius: 6px; }
.reason-content, .audit-content { margin-top: 4px; color: #303133; }
.modified-col .modified-tag { display: inline-block; margin-right: 6px; padding: 0 6px; height: 20px; line-height: 20px; background: #ecf5ff; color: #409eff; font-size: 12px; border-radius: 4px; }
</style>
