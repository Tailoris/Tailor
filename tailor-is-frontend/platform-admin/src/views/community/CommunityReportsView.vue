<template>
  <div v-loading="loading" class="page-container">
    <div class="page-header">
      <h2>举报处理</h2>
      <el-tag type="warning" size="large">{{ pendingCount }} 条待处理</el-tag>
    </div>
    <el-card class="page-card">
      <el-table :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="postTitle" label="被举报帖子" min-width="180" />
        <el-table-column prop="reporter" label="举报人" width="100" />
        <el-table-column prop="reason" label="举报原因" width="120" />
        <el-table-column prop="description" label="详细描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reportTime" label="举报时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="handleView(row)">查看详情</el-button>
            <template v-if="row.status === 'pending'">
              <el-button type="success" text size="small" @click="handleConfirm(row)">确认违规</el-button>
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

    <el-dialog v-model="detailVisible" title="举报详情" width="560px">
      <el-descriptions v-if="currentItem" :column="1" border>
        <el-descriptions-item label="被举报帖子">{{ currentItem.postTitle }}</el-descriptions-item>
        <el-descriptions-item label="举报人">{{ currentItem.reporter }}</el-descriptions-item>
        <el-descriptions-item label="举报原因">{{ currentItem.reason }}</el-descriptions-item>
        <el-descriptions-item label="详细描述">{{ currentItem.description }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentItem.status)" size="small">{{ getStatusText(currentItem.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-if="currentItem.result" label="处理结果">{{ currentItem.result }}</el-descriptions-item>
        <el-descriptions-item label="举报时间">{{ currentItem.reportTime }}</el-descriptions-item>
        <el-descriptions-item v-if="currentItem.handleTime" label="处理时间">{{ currentItem.handleTime }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <template v-if="currentItem?.status === 'pending'">
          <el-button type="success" @click="detailVisible = false; handleConfirm(currentItem!)">确认违规</el-button>
          <el-button type="warning" @click="detailVisible = false; handleIgnore(currentItem!)">忽略</el-button>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { listReports, handleReport, type ReportInfo } from '@/api/community';

const loading = ref(false);
const tableData = ref<ReportInfo[]>([]);
const pagination = reactive({ current: 1, size: 10, total: 0 });
const detailVisible = ref(false);
const currentItem = ref<ReportInfo | null>(null);

const pendingCount = computed(() => tableData.value.filter(r => r.status === 'pending').length);

function getStatusType(status: string) {
  const map: Record<string, string> = { pending: 'warning', confirmed: 'danger', ignored: 'info' };
  return map[status] || 'info';
}
function getStatusText(status: string) {
  const map: Record<string, string> = { pending: '待处理', confirmed: '已确认违规', ignored: '已忽略' };
  return map[status] || status;
}

async function loadData() {
  loading.value = true;
  try {
    const res = await listReports({ current: pagination.current, size: pagination.size });
    tableData.value = res.records;
    pagination.total = res.total;
  } catch { ElMessage.error('加载举报列表失败'); } finally { loading.value = false; }
}

function handleView(row: ReportInfo) { currentItem.value = row; detailVisible.value = true; }

async function handleConfirm(row: ReportInfo) {
  try {
    const { value: remark } = await ElMessageBox.prompt('请输入处理意见', '确认违规', {
      confirmButtonText: '确定', cancelButtonText: '取消', inputPlaceholder: '请输入处理意见...',
    });
    await handleReport(row.id, 'confirmed', remark || '');
    row.status = 'confirmed';
    row.result = remark || '';
    row.handleTime = new Date().toISOString().replace('T', ' ').slice(0, 19);
    ElMessage.success('已确认违规');
  } catch { /* 取消 */ }
}

async function handleIgnore(row: ReportInfo) {
  try {
    await ElMessageBox.confirm('确定忽略该举报吗？', '忽略确认', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
    });
    await handleReport(row.id, 'ignored', '经审核不构成违规');
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