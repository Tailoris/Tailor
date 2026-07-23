<template>
  <div v-loading="loading" class="page-container">
    <div class="page-header">
      <h2>纸样审核</h2>
      <el-tag type="warning" size="large">{{ pendingCount }} 条待审核</el-tag>
    </div>
    <el-card class="page-card">
      <el-table :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="纸样名称" min-width="180" />
        <el-table-column prop="designer" label="设计师" width="100" />
        <el-table-column prop="category" label="分类" width="80" />
        <el-table-column prop="format" label="格式" width="70" />
        <el-table-column label="评分" width="120" align="center">
          <template #default="{ row }">
            <el-rate v-model="row.score" disabled show-score :max="5" :score-template="`${row.score}`" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="标签" min-width="180">
          <template #default="{ row }">
            <el-tag v-for="tag in row.tags" :key="tag" size="small" style="margin-right: 4px" type="success">{{ tag }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="uploadTime" label="上传时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="handleView(row)">查看详情</el-button>
            <el-button v-if="row.status === 'pending'" type="success" text size="small" @click="handleApprove(row)">通过</el-button>
            <el-button v-if="row.status === 'pending'" type="danger" text size="small" @click="handleReject(row)">拒绝</el-button>
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

    <el-dialog v-model="detailVisible" title="纸样审核详情" width="560px">
      <el-descriptions v-if="currentItem" :column="1" border>
        <el-descriptions-item label="纸样名称">{{ currentItem.name }}</el-descriptions-item>
        <el-descriptions-item label="设计师">{{ currentItem.designer }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ currentItem.category }}</el-descriptions-item>
        <el-descriptions-item label="格式">{{ currentItem.format }}</el-descriptions-item>
        <el-descriptions-item label="尺寸">{{ currentItem.size }}</el-descriptions-item>
        <el-descriptions-item label="评分">
          <el-rate v-model="currentItem.score" disabled show-score :max="5" size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="标签">
          <el-tag v-for="tag in currentItem.tags" :key="tag" size="small" style="margin-right: 4px" type="success">{{ tag }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="描述">{{ currentItem.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentItem.status)" size="small">{{ getStatusText(currentItem.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="上传时间">{{ currentItem.uploadTime }}</el-descriptions-item>
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
import { ref, reactive, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { listPatterns, approvePattern, type PatternInfo } from '@/api/pattern';

const loading = ref(false);
const tableData = ref<PatternInfo[]>([]);
const pagination = reactive({ current: 1, size: 10, total: 0 });
const detailVisible = ref(false);
const currentItem = ref<PatternInfo | null>(null);

const pendingCount = computed(() => tableData.value.filter(p => p.status === 'pending').length);

function getStatusType(status: string) {
  const map: Record<string, string> = { pending: 'warning', approved: 'success', rejected: 'danger' };
  return map[status] || 'info';
}
function getStatusText(status: string) {
  const map: Record<string, string> = { pending: '待审核', approved: '已通过', rejected: '已拒绝' };
  return map[status] || status;
}

async function loadData() {
  loading.value = true;
  try {
    const res = await listPatterns({ current: pagination.current, size: pagination.size });
    tableData.value = res.records;
    pagination.total = res.total;
  } catch { ElMessage.error('加载审核列表失败'); } finally { loading.value = false; }
}

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

onMounted(() => { loadData(); });
</script>

<style scoped>
.page-container { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 18px; font-weight: 600; }
.page-card { margin-bottom: 0; }
.pagination { margin-top: 20px; text-align: right; }
</style>