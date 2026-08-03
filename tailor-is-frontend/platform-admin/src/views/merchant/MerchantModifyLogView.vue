<template>
  <div
    v-loading="loading"
    class="page-container"
  >
    <div class="page-header">
      <h2>修改记录</h2>
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
          v-model="searchForm.operatorType"
          placeholder="操作人类型"
          clearable
          style="width: 150px"
        >
          <el-option
            label="商家操作"
            :value="2"
          />
          <el-option
            label="平台操作"
            :value="1"
          />
        </el-select>
        <el-select
          v-model="searchForm.operateType"
          placeholder="操作类型"
          clearable
          style="width: 150px"
        >
          <el-option
            label="新增"
            :value="1"
          />
          <el-option
            label="修改"
            :value="2"
          />
          <el-option
            label="删除"
            :value="3"
          />
          <el-option
            label="审核"
            :value="4"
          />
        </el-select>
        <el-select
          v-model="searchForm.operateModule"
          placeholder="操作模块"
          clearable
          style="width: 150px"
        >
          <el-option
            label="商家信息"
            value="merchant"
          />
          <el-option
            label="店铺信息"
            value="shop"
          />
          <el-option
            label="资质文件"
            value="qualification"
          />
          <el-option
            label="类目关联"
            value="category"
          />
          <el-option
            label="保障金"
            value="deposit"
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
          placeholder="字段/备注模糊搜索"
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
          prop="id"
          label="ID"
          width="70"
        />
        <el-table-column
          prop="merchantId"
          label="商家ID"
          width="90"
        />
        <el-table-column
          label="操作人类型"
          width="110"
        >
          <template #default="{ row }">
            <el-tag
              size="small"
              :type="row.operatorType === 1 ? 'warning' : 'info'"
            >
              {{ row.operatorType === 1 ? '平台' : '商家' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="操作类型"
          width="100"
        >
          <template #default="{ row }">
            <el-tag
              size="small"
              :type="['','success','primary','danger','warning'][row.operateType] || 'info'"
            >
              {{ ['','新增','修改','删除','审核'][row.operateType] || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="operateModule"
          label="操作模块"
          width="110"
        />
        <el-table-column
          prop="fieldName"
          label="修改字段"
          width="130"
          show-overflow-tooltip
        />
        <el-table-column
          label="变更摘要"
          min-width="160"
        >
          <template #default="{ row }">
            <span class="old-val">{{ formatBrief(row.oldValue) }}</span>
            <el-icon class="arrow">
              <Right />
            </el-icon>
            <span class="new-val">{{ formatBrief(row.newValue) }}</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="remark"
          label="备注"
          width="140"
          show-overflow-tooltip
        />
        <el-table-column
          prop="operatorIp"
          label="IP"
          width="130"
        />
        <el-table-column
          prop="createTime"
          label="时间"
          width="170"
        />
        <el-table-column
          label="操作"
          width="100"
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
          </template>
        </el-table-column>
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

    <el-dialog
      v-model="detailVisible"
      title="修改记录详情"
      width="640px"
    >
      <el-descriptions
        v-if="currentItem"
        :column="1"
        border
      >
        <el-descriptions-item label="ID">
          {{ currentItem.id }}
        </el-descriptions-item>
        <el-descriptions-item label="商家ID">
          {{ currentItem.merchantId }}
        </el-descriptions-item>
        <el-descriptions-item label="操作人ID">
          {{ currentItem.operatorId }}
        </el-descriptions-item>
        <el-descriptions-item label="操作人类型">
          {{ currentItem.operatorType === 1 ? '平台' : '商家' }}
        </el-descriptions-item>
        <el-descriptions-item label="操作类型">
          {{ ['','新增','修改','删除','审核'][currentItem.operateType] }}
        </el-descriptions-item>
        <el-descriptions-item label="操作模块">
          {{ currentItem.operateModule }}
        </el-descriptions-item>
        <el-descriptions-item label="修改字段">
          {{ currentItem.fieldName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="变更前">
          <pre class="value-pre">{{ currentItem.oldValue || '-' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="变更后">
          <pre class="value-pre">{{ currentItem.newValue || '-' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="备注">
          {{ currentItem.remark || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="操作IP">
          {{ currentItem.operatorIp || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="操作时间">
          {{ currentItem.createTime }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { Right } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';

interface LogRecord {
  id: number;
  merchantId: number;
  operatorId: number;
  operatorType: number;
  operateType: number;
  operateModule: string;
  fieldName: string;
  oldValue: string;
  newValue: string;
  remark: string;
  operatorIp: string;
  createTime: string;
}

const mockLogs: LogRecord[] = Array.from({ length: 40 }, (_, i) => ({
  id: i + 1,
  merchantId: (i % 10) + 1,
  operatorId: i % 2 === 0 ? 1 : 1000 + (i % 10),
  operatorType: i % 2 === 0 ? 1 : 2,
  operateType: (i % 4) + 1,
  operateModule: ['merchant', 'shop', 'qualification', 'category', 'deposit'][i % 5],
  fieldName: ['contactName', 'contactPhone', 'shopName', 'address', 'status'][i % 5],
  oldValue: `旧值${i}`,
  newValue: `新值${i}`,
  remark: i % 3 === 0 ? '平台操作修正' : i % 3 === 1 ? '商家自行修改' : '审核变更',
  operatorIp: `192.168.${(i % 255)}.${(i % 255)}`,
  createTime: `2026-07-${String(1 + (i % 28)).padStart(2, '0')} ${String((i % 24)).padStart(2, '0')}:30:00`,
}));

const loading = ref(false);
const tableData = ref<LogRecord[]>([]);
const pagination = reactive({ current: 1, size: 10, total: 0 });
const dateRange = ref<string[]>([]);
const searchForm = reactive({
  merchantId: '', operatorType: '' as number | string, operateType: '' as number | string,
  operateModule: '', keyword: '',
});

const detailVisible = ref(false);
const currentItem = ref<LogRecord | null>(null);

function formatBrief(v: string) {
  if (!v) return '-';
  return v.length > 10 ? v.slice(0, 10) + '...' : v;
}

async function loadData() {
  loading.value = true;
  try {
    let filtered = [...mockLogs];
    if (searchForm.merchantId) {
      filtered = filtered.filter(l => l.merchantId === Number(searchForm.merchantId));
    }
    if (searchForm.operatorType !== '') {
      filtered = filtered.filter(l => l.operatorType === Number(searchForm.operatorType));
    }
    if (searchForm.operateType !== '') {
      filtered = filtered.filter(l => l.operateType === Number(searchForm.operateType));
    }
    if (searchForm.operateModule) {
      filtered = filtered.filter(l => l.operateModule === searchForm.operateModule);
    }
    if (searchForm.keyword) {
      const kw = String(searchForm.keyword).toLowerCase();
      filtered = filtered.filter(l =>
        String(l.fieldName || '').toLowerCase().includes(kw) ||
        String(l.remark || '').toLowerCase().includes(kw)
      );
    }
    const total = filtered.length;
    const start = ((pagination.current || 1) - 1) * (pagination.size || 10);
    tableData.value = filtered.slice(start, start + (pagination.size || 10));
    pagination.total = total;
  } catch {
    ElMessage.error('加载修改记录失败');
  } finally {
    loading.value = false;
  }
}

function handleSearch() { pagination.current = 1; loadData(); }
function handleReset() {
  Object.assign(searchForm, { merchantId: '', operatorType: '', operateType: '', operateModule: '', keyword: '' });
  dateRange.value = []; pagination.current = 1; loadData();
}

function handleView(row: LogRecord) {
  currentItem.value = row;
  detailVisible.value = true;
}

onMounted(() => { loadData(); });
</script>

<style scoped>
.page-container { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 18px; font-weight: 600; }
.search-bar { display: flex; gap: 12px; margin-bottom: 20px; flex-wrap: wrap; }
.pagination { margin-top: 20px; text-align: right; }
.old-val { color: #909399; }
.new-val { color: #67c23a; }
.arrow { color: #c0c4cc; vertical-align: middle; margin: 0 4px; }
.value-pre { margin: 0; font-family: monospace; white-space: pre-wrap; word-break: break-all; }
</style>
