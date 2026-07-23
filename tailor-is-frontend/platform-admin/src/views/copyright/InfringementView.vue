<template>
  <div v-loading="loading" class="page-container">
    <div class="page-header">
      <h2>侵权检测</h2>
      <el-button type="primary" :loading="detecting" @click="handleDetect">
        <el-icon><VideoPlay /></el-icon>开始检测
      </el-button>
    </div>
    <el-card class="page-card">
      <el-table :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="workName" label="作品名称" min-width="180" />
        <el-table-column prop="originalAuthor" label="原作者" width="100" />
        <el-table-column prop="suspectedWork" label="疑似侵权作品" min-width="180" />
        <el-table-column prop="suspectedAuthor" label="疑似侵权人" width="100" />
        <el-table-column label="相似度" width="160">
          <template #default="{ row }">
            <el-progress :percentage="row.similarity" :color="getProgressColor(row.similarity)" :stroke-width="12" />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="detectTime" label="检测时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="handleView(row)">查看详情</el-button>
            <template v-if="row.status === 'detected' || row.status === 'pending'">
              <el-button type="success" text size="small" @click="handleConfirm(row)">确认侵权</el-button>
              <el-button type="warning" text size="small" @click="handleIgnore(row)">忽略</el-button>
            </template>
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

    <el-dialog v-model="detailVisible" title="侵权详情" width="560px">
      <el-descriptions v-if="currentItem" :column="1" border>
        <el-descriptions-item label="作品名称">{{ currentItem.workName }}</el-descriptions-item>
        <el-descriptions-item label="原作者">{{ currentItem.originalAuthor }}</el-descriptions-item>
        <el-descriptions-item label="疑似侵权作品">{{ currentItem.suspectedWork }}</el-descriptions-item>
        <el-descriptions-item label="疑似侵权人">{{ currentItem.suspectedAuthor }}</el-descriptions-item>
        <el-descriptions-item label="相似度">
          <el-progress :percentage="currentItem.similarity" :color="getProgressColor(currentItem.similarity)" :stroke-width="12" />
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentItem.status)" size="small">{{ getStatusText(currentItem.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="详细描述">{{ currentItem.description }}</el-descriptions-item>
        <el-descriptions-item label="检测时间">{{ currentItem.detectTime }}</el-descriptions-item>
        <el-descriptions-item v-if="currentItem.handleTime" label="处理时间">{{ currentItem.handleTime }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <template v-if="currentItem?.status === 'detected' || currentItem?.status === 'pending'">
          <el-button type="success" @click="detailVisible = false; handleConfirm(currentItem!)">确认侵权</el-button>
          <el-button type="warning" @click="detailVisible = false; handleIgnore(currentItem!)">忽略</el-button>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { VideoPlay } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { listInfringements, handleInfringement, detectInfringement, type InfringementInfo } from '@/api/copyright';

const loading = ref(false);
const detecting = ref(false);
const tableData = ref<InfringementInfo[]>([]);
const pagination = reactive({ current: 1, size: 10, total: 0 });
const detailVisible = ref(false);
const currentItem = ref<InfringementInfo | null>(null);

function getProgressColor(similarity: number) {
  if (similarity >= 80) return '#ef4444';
  if (similarity >= 50) return '#f59e0b';
  return '#10b981';
}
function getStatusType(status: string) {
  const map: Record<string, string> = { detected: 'danger', pending: 'warning', confirmed: 'danger', ignored: 'info' };
  return map[status] || 'info';
}
function getStatusText(status: string) {
  const map: Record<string, string> = { detected: '已检测到', pending: '待处理', confirmed: '已确认侵权', ignored: '已忽略' };
  return map[status] || status;
}

async function loadData() {
  loading.value = true;
  try {
    const res = await listInfringements({ current: pagination.current, size: pagination.size });
    tableData.value = res.records;
    pagination.total = res.total;
  } catch { ElMessage.error('加载侵权列表失败'); } finally { loading.value = false; }
}

async function handleDetect() {
  detecting.value = true;
  try {
    const results = await detectInfringement();
    ElMessage.success(`检测完成，发现 ${results.length} 条疑似侵权记录`);
    loadData();
  } catch { ElMessage.error('检测失败'); } finally { detecting.value = false; }
}

function handleView(row: InfringementInfo) { currentItem.value = row; detailVisible.value = true; }

async function handleConfirm(row: InfringementInfo) {
  try {
    await ElMessageBox.confirm('确认该作品存在侵权行为？系统将通知相关方处理。', '确认侵权', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
    });
    await handleInfringement(row.id, 'confirmed', '经审核确认侵权');
    row.status = 'confirmed';
    row.handleTime = new Date().toISOString().replace('T', ' ').slice(0, 19);
    ElMessage.success('已确认侵权');
  } catch { /* 取消 */ }
}

async function handleIgnore(row: InfringementInfo) {
  try {
    await ElMessageBox.confirm('确定忽略该侵权检测结果吗？', '忽略确认', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
    });
    await handleInfringement(row.id, 'ignored', '经审核不构成侵权');
    row.status = 'ignored';
    row.handleTime = new Date().toISOString().replace('T', ' ').slice(0, 19);
    ElMessage.success('已忽略');
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