<template>
  <div v-loading="loading" class="page-container">
    <div class="page-header">
      <h2>商家审核</h2>
      <el-tag type="warning" size="large">{{ pendingCount }} 条待审核</el-tag>
    </div>
    <el-card class="page-card">
      <el-table :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="商家名称" min-width="150" />
        <el-table-column label="店铺类型" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ getShopTypeText(row.shopType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="contact" label="联系人" width="100" />
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="address" label="地址" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="handleView(row)">查看</el-button>
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

    <el-dialog v-model="detailVisible" title="商家详情" width="560px">
      <el-descriptions v-if="currentItem" :column="1" border>
        <el-descriptions-item label="商家名称">{{ currentItem.name }}</el-descriptions-item>
        <el-descriptions-item label="店铺类型">{{ getShopTypeText(currentItem.shopType) }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ currentItem.contact }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ currentItem.phone }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ currentItem.email }}</el-descriptions-item>
        <el-descriptions-item label="地址">{{ currentItem.address }}</el-descriptions-item>
        <el-descriptions-item label="简介">{{ currentItem.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentItem.status)" size="small">{{ getStatusText(currentItem.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ currentItem.createTime }}</el-descriptions-item>
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
import { listMerchants, approveMerchant, type MerchantInfo } from '@/api/merchant';

const loading = ref(false);
const tableData = ref<MerchantInfo[]>([]);
const pagination = reactive({ current: 1, size: 10, total: 0 });
const detailVisible = ref(false);
const currentItem = ref<MerchantInfo | null>(null);

const pendingCount = computed(() => tableData.value.filter(m => m.status === 'pending').length);

function getShopTypeText(type: string) {
  const map: Record<string, string> = { personal: '个人店', enterprise: '企业店', brand: '品牌店', factory: '工厂店' };
  return map[type] || type;
}
function getStatusType(status: string) {
  const map: Record<string, string> = { pending: 'warning', approved: 'success', rejected: 'danger', disabled: 'info' };
  return map[status] || 'info';
}
function getStatusText(status: string) {
  const map: Record<string, string> = { pending: '待审核', approved: '已通过', rejected: '已拒绝', disabled: '已禁用' };
  return map[status] || status;
}

async function loadData() {
  loading.value = true;
  try {
    const res = await listMerchants({ current: pagination.current, size: pagination.size });
    tableData.value = res.records;
    pagination.total = res.total;
  } catch {
    ElMessage.error('加载审核列表失败');
  } finally {
    loading.value = false;
  }
}

function handleView(row: MerchantInfo) {
  currentItem.value = row;
  detailVisible.value = true;
}

async function handleApprove(row: MerchantInfo) {
  try {
    await ElMessageBox.confirm(`确定通过商家 "${row.name}" 的审核吗？`, '审核通过确认', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
    });
    await approveMerchant(row.id, true);
    row.status = 'approved';
    ElMessage.success(`已通过商家: ${row.name}`);
  } catch { /* 取消 */ }
}

async function handleReject(row: MerchantInfo) {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝审核', {
      confirmButtonText: '确定', cancelButtonText: '取消',
      inputPlaceholder: '请输入拒绝原因...',
    });
    await approveMerchant(row.id, false, reason || '');
    row.status = 'rejected';
    ElMessage.success(`已拒绝商家: ${row.name}`);
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