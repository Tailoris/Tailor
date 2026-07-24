<template>
  <div v-loading="loading" class="page-container">
    <div class="page-header">
      <h2>纸样管理</h2>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>上传纸样
      </el-button>
    </div>
    <el-card class="page-card">
      <div class="search-bar">
        <el-input v-model="searchForm.keyword" placeholder="搜索纸样名称/设计师/分类" style="width: 240px" clearable @keyup.enter="handleSearch">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="searchForm.status" placeholder="状态筛选" clearable style="width: 130px">
          <el-option label="审核中" value="pending" />
          <el-option label="已通过" value="approved" />
          <el-option label="已拒绝" value="rejected" />
        </el-select>
        <el-select v-model="searchForm.category" placeholder="分类筛选" clearable style="width: 130px">
          <el-option label="连衣裙" value="连衣裙" />
          <el-option label="衬衫" value="衬衫" />
          <el-option label="T恤" value="T恤" />
          <el-option label="外套" value="外套" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
      <el-table :data="tableData" border stripe :row-key="row => row.id" :cell-class-name="cellClassName">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="纸样名称" min-width="180" />
        <el-table-column prop="designer" label="设计师" width="100" />
        <el-table-column prop="category" label="分类" width="80" />
        <el-table-column prop="format" label="格式" width="70" />
        <el-table-column prop="size" label="尺寸" width="70" />
        <el-table-column label="评分" width="80" align="center">
          <template #default="{ row }">
            <span class="score-badge" :class="getScoreClass(row.score)">{{ row.score }}</span>
          </template>
        </el-table-column>
        <el-table-column label="标签" min-width="180">
          <template #default="{ row }">
            <span v-for="tag in row.tags" :key="tag" class="simple-tag">{{ tag }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <span class="status-badge" :class="getStatusClass(row.status)">{{ getStatusText(row.status) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="uploadTime" label="上传时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <button class="btn-action btn-primary" @click="handleView(row)">预览</button>
            <button v-show="row.status === 'pending'" class="btn-action btn-success" @click="handleApprove(row)">通过</button>
            <button v-show="row.status === 'pending'" class="btn-action btn-danger" @click="handleReject(row)">拒绝</button>
            <button class="btn-action btn-danger" @click="handleDelete(row)">删除</button>
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

    <el-dialog v-model="detailVisible" title="纸样详情" width="560px">
      <el-descriptions v-if="currentItem" :column="1" border>
        <el-descriptions-item label="纸样名称">{{ currentItem.name }}</el-descriptions-item>
        <el-descriptions-item label="设计师">{{ currentItem.designer }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ currentItem.category }}</el-descriptions-item>
        <el-descriptions-item label="价格">¥{{ currentItem.price }}</el-descriptions-item>
        <el-descriptions-item label="格式">{{ currentItem.format }}</el-descriptions-item>
        <el-descriptions-item label="尺寸">{{ currentItem.size }}</el-descriptions-item>
        <el-descriptions-item label="评分">
          <span class="score-badge" :class="getScoreClass(currentItem.score)">{{ currentItem.score }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="标签">
          <span v-for="tag in currentItem.tags" :key="tag" class="simple-tag">{{ tag }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="描述">{{ currentItem.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <span class="status-badge" :class="getStatusClass(currentItem.status)">{{ getStatusText(currentItem.status) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="上传时间">{{ currentItem.uploadTime }}</el-descriptions-item>
        <el-descriptions-item v-if="currentItem.auditTime" label="审核时间">{{ currentItem.auditTime }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <template v-if="currentItem?.status === 'pending'">
          <el-button type="success" @click="detailVisible = false; handleApprove(currentItem!)">通过</el-button>
          <el-button type="danger" @click="detailVisible = false; handleReject(currentItem!)">拒绝</el-button>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { Plus, Search } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { listPatterns, approvePattern, deletePattern, type PatternInfo, type PatternQueryParams } from '@/api/pattern';

const loading = ref(false);
const tableData = ref<PatternInfo[]>([]);
const pagination = reactive({ current: 1, size: 10, total: 0 });
const searchForm = reactive<PatternQueryParams>({ keyword: '', status: '', category: '' });
const detailVisible = ref(false);
const currentItem = ref<PatternInfo | null>(null);

function cellClassName({ column, row }) { return ''; }

function getScoreClass(score: number) {
  if (score >= 90) return 'score-excellent';
  if (score >= 80) return 'score-good';
  return 'score-normal';
}

function getStatusClass(status: string) {
  const map: Record<string, string> = { pending: 'status-pending', approved: 'status-approved', rejected: 'status-rejected' };
  return map[status] || 'status-default';
}

function getStatusText(status: string) {
  const map: Record<string, string> = { pending: '审核中', approved: '已通过', rejected: '已拒绝' };
  return map[status] || status;
}

async function loadData() {
  loading.value = true;
  try {
    const res = await listPatterns({ ...searchForm, current: pagination.current, size: pagination.size });
    tableData.value = res.records;
    pagination.total = res.total;
  } catch { ElMessage.error('加载纸样列表失败'); } finally { loading.value = false; }
}

function handleSearch() { pagination.current = 1; loadData(); }
function handleReset() { searchForm.keyword = ''; searchForm.status = ''; searchForm.category = ''; pagination.current = 1; loadData(); }

function handleAdd() { ElMessage.info('上传纸样功能请通过纸样管理后台操作'); }
function handleView(row: PatternInfo) { currentItem.value = row; detailVisible.value = true; }

async function handleApprove(row: PatternInfo) {
  try {
    await ElMessageBox.confirm(`确定通过纸样 "${row.name}" 的审核吗？`, '审核通过确认', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
    });
    await approvePattern(row.id, true);
    row.status = 'approved';
    row.auditTime = new Date().toISOString().replace('T', ' ').slice(0, 19);
    ElMessage.success(`已通过纸样: ${row.name}`);
  } catch { /* 取消 */ }
}

async function handleReject(row: PatternInfo) {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝审核', {
      confirmButtonText: '确定', cancelButtonText: '取消', inputPlaceholder: '请输入拒绝原因...',
    });
    await approvePattern(row.id, false, reason || '');
    row.status = 'rejected';
    row.auditTime = new Date().toISOString().replace('T', ' ').slice(0, 19);
    ElMessage.success(`已拒绝纸样: ${row.name}`);
  } catch { /* 取消 */ }
}

async function handleDelete(row: PatternInfo) {
  try {
    await ElMessageBox.confirm(`确定要删除纸样 "${row.name}" 吗？`, '删除确认', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
    });
    await deletePattern(row.id);
    ElMessage.success(`已删除纸样: ${row.name}`);
    loadData();
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

.score-badge { display: inline-block; min-width: 32px; height: 24px; line-height: 24px; text-align: center; border-radius: 4px; font-size: 12px; font-weight: 500; }
.score-excellent { background: #dcfce7; color: #16a34a; }
.score-good { background: #dbeafe; color: #2563eb; }
.score-normal { background: #fef3c7; color: #d97706; }

.simple-tag { display: inline-block; padding: 2px 8px; background: #dcfce7; color: #16a34a; border-radius: 4px; font-size: 11px; margin-right: 4px; margin-bottom: 2px; }

.status-badge { display: inline-block; padding: 2px 10px; border-radius: 4px; font-size: 12px; font-weight: 500; }
.status-pending { background: #fef3c7; color: #d97706; }
.status-approved { background: #dcfce7; color: #16a34a; }
.status-rejected { background: #fee2e2; color: #dc2626; }
.status-default { background: #f3f4f6; color: #6b7280; }

.btn-action { padding: 4px 10px; border: none; border-radius: 4px; font-size: 12px; cursor: pointer; transition: all 0.2s; }
.btn-primary { background: #3b82f6; color: white; }
.btn-primary:hover { background: #2563eb; }
.btn-success { background: #22c55e; color: white; }
.btn-success:hover { background: #16a34a; }
.btn-danger { background: #ef4444; color: white; }
.btn-danger:hover { background: #dc2626; }
</style>