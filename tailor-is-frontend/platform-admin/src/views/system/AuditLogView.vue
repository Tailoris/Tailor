<template>
  <div
    v-loading="loading"
    class="audit-log-page"
  >
    <div class="page-header">
      <h2 class="page-title">
        操作日志
      </h2>
      <p class="page-subtitle">
        查看平台管理员及用户操作记录
      </p>
    </div>

    <div class="toolbar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索操作人/操作内容"
        class="search-input"
        clearable
        @keyup.enter="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-select
        v-model="actionType"
        placeholder="操作类型"
        clearable
        class="action-select"
      >
        <el-option
          label="全部"
          value=""
        />
        <el-option
          label="登录"
          value="LOGIN"
        />
        <el-option
          label="创建"
          value="CREATE"
        />
        <el-option
          label="更新"
          value="UPDATE"
        />
        <el-option
          label="删除"
          value="DELETE"
        />
        <el-option
          label="导出"
          value="EXPORT"
        />
        <el-option
          label="权限变更"
          value="PERMISSION"
        />
      </el-select>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        class="date-picker"
      />
      <el-button
        type="primary"
        :disabled="total === 0"
        @click="handleExport"
      >
        <el-icon><Download /></el-icon>
        导出日志
      </el-button>
    </div>

    <el-table
      :data="logList"
      border
      stripe
      style="width: 100%"
    >
      <el-table-column
        prop="id"
        label="ID"
        width="80"
      />
      <el-table-column
        prop="operator"
        label="操作人"
        width="120"
      />
      <el-table-column
        label="操作类型"
        width="110"
        align="center"
      >
        <template #default="scope">
          <el-tag
            :type="actionTagType(scope.row.actionType)"
            size="small"
          >
            {{ actionLabel(scope.row.actionType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        prop="target"
        label="操作对象"
        min-width="150"
      />
      <el-table-column
        prop="description"
        label="操作描述"
        min-width="250"
        show-overflow-tooltip
      />
      <el-table-column
        prop="ip"
        label="IP地址"
        width="140"
      />
      <el-table-column
        prop="createTime"
        label="操作时间"
        width="180"
        sortable
      />
      <el-table-column
        label="操作"
        width="100"
        fixed="right"
      >
        <template #default="scope">
          <el-button
            size="small"
            @click="handleDetail(scope.row)"
          >
            详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadLogs"
        @current-change="loadLogs"
      />
    </div>

    <!-- 日志详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      title="操作日志详情"
      width="600px"
    >
      <el-descriptions
        v-if="currentLog"
        :column="1"
        border
      >
        <el-descriptions-item label="操作ID">
          {{ currentLog.id }}
        </el-descriptions-item>
        <el-descriptions-item label="操作人">
          {{ currentLog.operator }}
        </el-descriptions-item>
        <el-descriptions-item label="操作类型">
          <el-tag
            :type="actionTagType(currentLog.actionType)"
            size="small"
          >
            {{ actionLabel(currentLog.actionType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作对象">
          {{ currentLog.target }}
        </el-descriptions-item>
        <el-descriptions-item label="操作描述">
          {{ currentLog.description }}
        </el-descriptions-item>
        <el-descriptions-item label="IP地址">
          {{ currentLog.ip }}
        </el-descriptions-item>
        <el-descriptions-item label="User-Agent">
          {{ currentLog.userAgent || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="操作时间">
          {{ currentLog.createTime }}
        </el-descriptions-item>
        <el-descriptions-item label="请求参数">
          <pre class="json-preview">{{ formatJson(currentLog.requestParams) }}</pre>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">
          关闭
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { Search, Download } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { listAuditLogs, getAuditLogDetail, exportAuditLogs, type AuditLog } from '@/api/audit-log';

const loading = ref(false);
const searchKeyword = ref('');
const actionType = ref('');
const dateRange = ref<[Date, Date] | null>(null);
const logList = ref<AuditLog[]>([]);
const currentPage = ref(1);
const pageSize = ref(20);
const total = ref(0);

const detailVisible = ref(false);
const currentLog = ref<AuditLog | null>(null);

function actionTagType(type: string): '' | 'success' | 'warning' | 'danger' | 'info' {
  const map: Record<string, '' | 'success' | 'warning' | 'danger' | 'info'> = {
    LOGIN: 'info',
    CREATE: 'success',
    UPDATE: 'warning',
    DELETE: 'danger',
    EXPORT: '',
    PERMISSION: 'warning',
  };
  return map[type] || 'info';
}

function actionLabel(type: string): string {
  const map: Record<string, string> = {
    LOGIN: '登录',
    CREATE: '创建',
    UPDATE: '更新',
    DELETE: '删除',
    EXPORT: '导出',
    PERMISSION: '权限变更',
  };
  return map[type] || type;
}

function formatJson(jsonStr: string): string {
  try {
    return JSON.stringify(JSON.parse(jsonStr), null, 2);
  } catch {
    return jsonStr || '-';
  }
}

async function loadLogs() {
  loading.value = true;
  try {
    const res = await listAuditLogs({
      keyword: searchKeyword.value,
      actionType: actionType.value,
      startDate: dateRange.value?.[0]?.toISOString(),
      endDate: dateRange.value?.[1]?.toISOString(),
      current: currentPage.value,
      size: pageSize.value,
    });
    logList.value = res.records;
    total.value = res.total;
  } catch {
    console.warn('加载操作日志失败，使用本地数据');
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  currentPage.value = 1;
  loadLogs();
}

async function handleDetail(row: AuditLog) {
  try {
    const detail = await getAuditLogDetail(row.id);
    currentLog.value = detail;
  } catch {
    currentLog.value = row;
  }
  detailVisible.value = true;
}

async function handleExport() {
  try {
    const blob = await exportAuditLogs({
      keyword: searchKeyword.value,
      actionType: actionType.value,
      startDate: dateRange.value?.[0]?.toISOString(),
      endDate: dateRange.value?.[1]?.toISOString(),
    });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `audit-logs-${new Date().toISOString().slice(0, 10)}.xlsx`;
    link.click();
    window.URL.revokeObjectURL(url);
    ElMessage.success('日志导出成功');
  } catch {
    ElMessage.error('日志导出失败');
  }
}

onMounted(() => {
  loadLogs();
});
</script>

<style scoped>
.audit-log-page {
  max-width: 1400px;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.page-subtitle {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-top: 4px;
}

.toolbar {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  align-items: center;
  flex-wrap: wrap;
}

.search-input {
  width: 280px;
}

.action-select {
  width: 140px;
}

.date-picker {
  width: 280px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.json-preview {
  font-size: 12px;
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  margin: 0;
  max-height: 300px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
