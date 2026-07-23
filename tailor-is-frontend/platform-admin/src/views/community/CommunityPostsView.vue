<template>
  <div v-loading="loading" class="page-container">
    <div class="page-header">
      <h2>帖子审核</h2>
      <el-tag type="warning" size="large">{{ pendingCount }} 条待审核</el-tag>
    </div>
    <el-card class="page-card">
      <div class="search-bar">
        <el-input v-model="searchForm.keyword" placeholder="搜索帖子标题/内容/作者" style="width: 240px" clearable @keyup.enter="handleSearch">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="searchForm.status" placeholder="状态筛选" clearable style="width: 130px">
          <el-option label="待审核" value="pending" />
          <el-option label="已通过" value="approved" />
          <el-option label="已拒绝" value="rejected" />
        </el-select>
        <el-select v-model="searchForm.board" placeholder="板块筛选" clearable style="width: 130px">
          <el-option label="设计交流" value="设计交流" />
          <el-option label="技术分享" value="技术分享" />
          <el-option label="资源对接" value="资源对接" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
      <el-table :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="author" label="作者" width="100" />
        <el-table-column prop="board" label="板块" width="100" />
        <el-table-column prop="content" label="内容" min-width="250" show-overflow-tooltip />
        <el-table-column label="互动" width="160" align="center">
          <template #default="{ row }">
            <span class="stat">👁 {{ row.viewCount }}</span>
            <span class="stat">❤ {{ row.likeCount }}</span>
            <span class="stat">💬 {{ row.commentCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发布时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="handleView(row)">查看</el-button>
            <el-button v-if="row.status === 'pending'" type="success" text size="small" @click="handleApprove(row)">通过</el-button>
            <el-button v-if="row.status === 'pending'" type="danger" text size="small" @click="handleReject(row)">拒绝</el-button>
            <el-button type="danger" text size="small" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="detailVisible" title="帖子详情" width="600px">
      <el-descriptions v-if="currentItem" :column="1" border>
        <el-descriptions-item label="标题">{{ currentItem.title }}</el-descriptions-item>
        <el-descriptions-item label="作者">{{ currentItem.author }}</el-descriptions-item>
        <el-descriptions-item label="板块">{{ currentItem.board }}</el-descriptions-item>
        <el-descriptions-item label="内容">{{ currentItem.content }}</el-descriptions-item>
        <el-descriptions-item label="浏览">{{ currentItem.viewCount }} | 点赞 {{ currentItem.likeCount }} | 评论 {{ currentItem.commentCount }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentItem.status)" size="small">{{ getStatusText(currentItem.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="发布时间">{{ currentItem.createTime }}</el-descriptions-item>
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
import { ref, reactive, computed, onMounted } from 'vue';
import { Search } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { listPosts, approvePost, deletePost, type PostInfo, type CommunityQueryParams } from '@/api/community';

const loading = ref(false);
const tableData = ref<PostInfo[]>([]);
const pagination = reactive({ current: 1, size: 10, total: 0 });
const searchForm = reactive<CommunityQueryParams>({ keyword: '', status: '', board: '' });
const detailVisible = ref(false);
const currentItem = ref<PostInfo | null>(null);

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
    const res = await listPosts({ ...searchForm, current: pagination.current, size: pagination.size });
    tableData.value = res.records;
    pagination.total = res.total;
  } catch { ElMessage.error('加载帖子列表失败'); } finally { loading.value = false; }
}

function handleSearch() { pagination.current = 1; loadData(); }
function handleReset() { searchForm.keyword = ''; searchForm.status = ''; searchForm.board = ''; pagination.current = 1; loadData(); }

function handleView(row: PostInfo) { currentItem.value = row; detailVisible.value = true; }

async function handleApprove(row: PostInfo) {
  try {
    await ElMessageBox.confirm(`确定通过帖子 "${row.title}" 的审核吗？`, '审核通过确认', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
    });
    await approvePost(row.id, true);
    row.status = 'approved';
    row.auditTime = new Date().toISOString().replace('T', ' ').slice(0, 19);
    ElMessage.success(`已通过帖子: ${row.title}`);
  } catch { /* 取消 */ }
}

async function handleReject(row: PostInfo) {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝审核', {
      confirmButtonText: '确定', cancelButtonText: '取消', inputPlaceholder: '请输入拒绝原因...',
    });
    await approvePost(row.id, false, reason || '');
    row.status = 'rejected';
    row.auditTime = new Date().toISOString().replace('T', ' ').slice(0, 19);
    ElMessage.success(`已拒绝帖子: ${row.title}`);
  } catch { /* 取消 */ }
}

async function handleDelete(row: PostInfo) {
  try {
    await ElMessageBox.confirm(`确定要删除帖子 "${row.title}" 吗？`, '删除确认', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
    });
    await deletePost(row.id);
    ElMessage.success(`已删除帖子: ${row.title}`);
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
.stat { margin: 0 8px; font-size: 12px; color: #6b7280; }
</style>