<template>
  <div v-loading="loading" class="page-container">
    <div class="page-header">
      <h2>数据报告</h2>
      <el-button type="primary" @click="handleGenerate">
        <el-icon><Plus /></el-icon>生成报告
      </el-button>
    </div>
    <el-card class="page-card">
      <div class="search-bar">
        <el-select v-model="searchForm.type" placeholder="报告类型" clearable style="width: 140px" @change="handleSearch">
          <el-option label="月度报告" value="monthly" />
          <el-option label="季度报告" value="quarterly" />
          <el-option label="年度报告" value="yearly" />
        </el-select>
        <el-date-picker v-model="searchForm.date" type="month" placeholder="选择月份" style="width: 150px" @change="handleSearch" />
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
      <el-table :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="报告名称" min-width="200" />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getTypeTag(row.type)" size="small">{{ getTypeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="date" label="报告周期" width="120" />
        <el-table-column label="摘要" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">{{ row.summary }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'generated' ? 'success' : 'warning'" size="small">
              {{ row.status === 'generated' ? '已生成' : '生成中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="生成时间" width="160" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="handleView(row)">查看</el-button>
            <el-button type="success" text size="small" @click="handleExport(row)">导出</el-button>
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

    <el-dialog v-model="generateVisible" title="生成报告" width="480px">
      <el-form ref="genFormRef" :model="genForm" :rules="genRules" label-width="90px">
        <el-form-item label="报告类型" prop="type">
          <el-select v-model="genForm.type" placeholder="请选择报告类型" style="width: 100%">
            <el-option label="月度报告" value="monthly" />
            <el-option label="季度报告" value="quarterly" />
            <el-option label="年度报告" value="yearly" />
          </el-select>
        </el-form-item>
        <el-form-item label="报告周期" prop="date">
          <el-date-picker
            v-if="genForm.type === 'yearly'"
            v-model="genForm.date" type="year" placeholder="选择年份" style="width: 100%"
          />
          <el-date-picker
            v-else-if="genForm.type === 'quarterly'"
            v-model="genForm.date" type="month" placeholder="选择季度起始月" style="width: 100%"
          />
          <el-date-picker
            v-else
            v-model="genForm.date" type="month" placeholder="选择月份" style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="generateVisible = false">取消</el-button>
        <el-button type="primary" :loading="generating" @click="doGenerate">开始生成</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="viewVisible" title="报告详情" width="600px">
      <el-descriptions :column="2" border v-if="viewReport">
        <el-descriptions-item label="报告名称" :span="2">{{ viewReport.name }}</el-descriptions-item>
        <el-descriptions-item label="报告类型">{{ getTypeText(viewReport.type) }}</el-descriptions-item>
        <el-descriptions-item label="报告周期">{{ viewReport.date }}</el-descriptions-item>
        <el-descriptions-item label="生成时间">{{ viewReport.createTime }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="viewReport.status === 'generated' ? 'success' : 'warning'" size="small">
            {{ viewReport.status === 'generated' ? '已生成' : '生成中' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="摘要" :span="2">{{ viewReport.summary }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { Plus } from '@element-plus/icons-vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { listReports, generateReport, getReportById, exportReport, type ReportInfo, type AnalyticsQueryParams } from '@/api/analytics';

const loading = ref(false);
const generating = ref(false);
const tableData = ref<ReportInfo[]>([]);
const pagination = reactive({ current: 1, size: 10, total: 0 });
const searchForm = reactive<AnalyticsQueryParams>({ type: '', date: '' });

const generateVisible = ref(false);
const genFormRef = ref<FormInstance>();
const genForm = reactive({ type: 'monthly' as string, date: '' });
const genRules: FormRules = {
  type: [{ required: true, message: '请选择报告类型', trigger: 'change' }],
  date: [{ required: true, message: '请选择报告周期', trigger: 'change' }],
};

const viewVisible = ref(false);
const viewReport = ref<ReportInfo | null>(null);

function getTypeTag(type: string) {
  const map: Record<string, string> = { monthly: 'primary', quarterly: 'warning', yearly: 'danger' };
  return map[type] || 'info';
}
function getTypeText(type: string) {
  const map: Record<string, string> = { monthly: '月度报告', quarterly: '季度报告', yearly: '年度报告' };
  return map[type] || type;
}

async function loadData() {
  loading.value = true;
  try {
    const res = await listReports({ ...searchForm, current: pagination.current, size: pagination.size });
    tableData.value = res.records;
    pagination.total = res.total;
  } catch { ElMessage.error('加载报告列表失败'); } finally { loading.value = false; }
}

function handleSearch() { pagination.current = 1; loadData(); }
function handleReset() { searchForm.type = ''; searchForm.date = ''; pagination.current = 1; loadData(); }

function handleGenerate() {
  genForm.type = 'monthly'; genForm.date = '';
  generateVisible.value = true;
}

async function doGenerate() {
  if (!genFormRef.value) return;
  await genFormRef.value.validate(async (valid) => {
    if (!valid) return;
    generating.value = true;
    try {
      const dateStr = typeof genForm.date === 'string' ? genForm.date : new Date(genForm.date).toISOString().slice(0, 7);
      await generateReport(genForm.type, dateStr);
      ElMessage.success('报告生成成功');
      generateVisible.value = false;
      loadData();
    } catch { ElMessage.error('报告生成失败'); } finally { generating.value = false; }
  });
}

async function handleView(row: ReportInfo) {
  try {
    viewReport.value = await getReportById(row.id);
    viewVisible.value = true;
  } catch { ElMessage.error('加载报告详情失败'); }
}

async function handleExport(row: ReportInfo) {
  try {
    const blob = await exportReport(row.id);
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = `${row.name}.pdf`;
    a.click(); URL.revokeObjectURL(url);
    ElMessage.success(`已导出报告: ${row.name}`);
  } catch { ElMessage.error('报告导出失败'); }
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