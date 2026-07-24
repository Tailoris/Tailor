<template>
  <div v-loading="loading" class="page-container">
    <div class="page-header">
      <h2>财务报表</h2>
      <el-button type="primary" @click="handleGenerate">
        <el-icon><Plus /></el-icon>生成报表
      </el-button>
    </div>
    <el-card class="page-card">
      <div class="search-bar">
        <el-select v-model="searchForm.type" placeholder="报表类型" clearable style="width: 130px" @change="handleSearch">
          <el-option label="日报" value="daily" /><el-option label="周报" value="weekly" />
          <el-option label="月报" value="monthly" /><el-option label="季报" value="quarterly" />
          <el-option label="年报" value="yearly" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
      <el-table :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="65" />
        <el-table-column prop="name" label="报表名称" min-width="200" />
        <el-table-column label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="getTypeTag(row.type)" size="small">{{ getTypeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="period" label="统计周期" width="120" />
        <el-table-column label="总收入" width="110" align="right">
          <template #default="{ row }">¥{{ row.totalRevenue.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="平台费" width="90" align="right">
          <template #default="{ row }">¥{{ row.totalFee.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="结算额" width="100" align="right">
          <template #default="{ row }">¥{{ row.totalSettlement.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="退款额" width="90" align="right">
          <template #default="{ row }">¥{{ row.totalRefund.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="净利润" width="110" align="right">
          <template #default="{ row }"><strong :style="{ color: row.netProfit >= 0 ? '#22c55e' : '#ef4444' }">¥{{ row.netProfit.toLocaleString() }}</strong></template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'generated' ? 'success' : 'warning'" size="small">{{ row.status === 'generated' ? '已生成' : '生成中' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="生成时间" width="155" prop="createTime" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="handleExport(row)">导出</el-button>
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

    <el-dialog v-model="generateVisible" title="生成财务报表" width="480px">
      <el-form ref="genFormRef" :model="genForm" :rules="genRules" label-width="90px">
        <el-form-item label="报表类型" prop="type">
          <el-select v-model="genForm.type" placeholder="请选择报表类型" style="width: 100%">
            <el-option label="日报" value="daily" /><el-option label="周报" value="weekly" />
            <el-option label="月报" value="monthly" /><el-option label="季报" value="quarterly" />
            <el-option label="年报" value="yearly" />
          </el-select>
        </el-form-item>
        <el-form-item label="统计周期" prop="period">
          <el-date-picker v-if="genForm.type === 'yearly'" v-model="genForm.period" type="year" placeholder="选择年份" style="width: 100%" />
          <el-date-picker v-else-if="genForm.type === 'monthly'" v-model="genForm.period" type="month" placeholder="选择月份" style="width: 100%" />
          <el-date-picker v-else v-model="genForm.period" type="date" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="generateVisible = false">取消</el-button>
        <el-button type="primary" :loading="generating" @click="doGenerate">开始生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { Plus } from '@element-plus/icons-vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { listFinanceReports, generateFinanceReport, exportFinanceReport, type FinanceReport, type FinanceQueryParams } from '@/api/finance';
import { recordAuditLog, auditActions, createFinanceAuditLog } from '@/utils/audit';

const loading = ref(false);
const generating = ref(false);
const tableData = ref<FinanceReport[]>([]);
const pagination = reactive({ current: 1, size: 10, total: 0 });
const searchForm = reactive<FinanceQueryParams>({ type: '' });

const generateVisible = ref(false);
const genFormRef = ref<FormInstance>();
const genForm = reactive({ type: 'daily' as string, period: '' });
const genRules: FormRules = {
  type: [{ required: true, message: '请选择报表类型', trigger: 'change' }],
  period: [{ required: true, message: '请选择统计周期', trigger: 'change' }],
};

function getTypeTag(t: string) { const m: Record<string, string> = { daily: 'info', weekly: 'warning', monthly: 'primary', quarterly: 'success', yearly: 'danger' }; return m[t] || 'info'; }
function getTypeText(t: string) { const m: Record<string, string> = { daily: '日报', weekly: '周报', monthly: '月报', quarterly: '季报', yearly: '年报' }; return m[t] || t; }

async function loadData() {
  loading.value = true;
  try {
    const res = await listFinanceReports({ ...searchForm, current: pagination.current, size: pagination.size });
    tableData.value = res.records; pagination.total = res.total;
  } catch { ElMessage.error('加载报表列表失败'); } finally { loading.value = false; }
}

function handleSearch() { pagination.current = 1; loadData(); }
function handleReset() { searchForm.type = ''; pagination.current = 1; loadData(); }

function handleGenerate() { genForm.type = 'daily'; genForm.period = ''; generateVisible.value = true; }

async function doGenerate() {
  if (!genFormRef.value) return;
  await genFormRef.value.validate(async (valid) => {
    if (!valid) return;
    generating.value = true;
    try {
      const periodStr = typeof genForm.period === 'string' ? genForm.period : new Date(genForm.period).toISOString().slice(0, genForm.type === 'yearly' ? 4 : 7);
      await generateFinanceReport(genForm.type, periodStr);
      ElMessage.success('报表生成成功');
      recordAuditLog(createFinanceAuditLog(auditActions.REPORT_GENERATE, 'report', undefined, `${getTypeText(genForm.type)} ${periodStr}`));
      generateVisible.value = false; loadData();
    } catch { ElMessage.error('报表生成失败'); } finally { generating.value = false; }
  });
}

async function handleExport(row: FinanceReport) {
  try {
    const blob = await exportFinanceReport(row.id);
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a'); a.href = url; a.download = `${row.name}.pdf`;
    a.click(); URL.revokeObjectURL(url);
    ElMessage.success(`已导出报表: ${row.name}`);
    recordAuditLog(createFinanceAuditLog(auditActions.REPORT_EXPORT, 'report', row.id, row.name));
  } catch { ElMessage.error('报表导出失败'); }
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