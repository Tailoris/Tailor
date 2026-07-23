<template>
  <div v-loading="loading" class="page-container">
    <div class="page-header">
      <h2>版权登记管理</h2>
      <el-button type="primary" @click="handleRegister">
        <el-icon><Plus /></el-icon>版权登记
      </el-button>
    </div>
    <el-card class="page-card">
      <div class="search-bar">
        <el-input v-model="searchForm.keyword" placeholder="搜索作品名称/作者/登记号" style="width: 240px" clearable @keyup.enter="handleSearch">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="searchForm.status" placeholder="状态筛选" clearable style="width: 130px">
          <el-option label="待审核" value="pending" />
          <el-option label="已登记" value="registered" />
          <el-option label="已拒绝" value="rejected" />
        </el-select>
        <el-select v-model="searchForm.category" placeholder="分类筛选" clearable style="width: 130px">
          <el-option label="服装设计" value="服装设计" />
          <el-option label="纸样设计" value="纸样设计" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
      <el-table :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="workName" label="作品名称" min-width="180" />
        <el-table-column prop="author" label="作者" width="100" />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column prop="registrationNo" label="登记号" width="150" show-overflow-tooltip />
        <el-table-column prop="blockchainHash" label="区块链存证" width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tooltip v-if="row.blockchainHash" :content="row.blockchainHash" placement="top">
              <el-tag type="success" size="small">已存证</el-tag>
            </el-tooltip>
            <el-tag v-else type="info" size="small">未存证</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="registerTime" label="登记时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="handleView(row)">查看证书</el-button>
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

    <el-dialog v-model="dialogVisible" title="版权登记" width="500px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="作品名称" prop="workName">
          <el-input v-model="form.workName" placeholder="请输入作品名称" maxlength="64" />
        </el-form-item>
        <el-form-item label="作者" prop="author">
          <el-input v-model="form.author" placeholder="请输入作者姓名" maxlength="32" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%">
            <el-option label="服装设计" value="服装设计" />
            <el-option label="纸样设计" value="纸样设计" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="作品描述（可选）" maxlength="256" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">提交登记</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { Plus, Search } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { listCopyrights, registerCopyright, approveCopyright, type CopyrightInfo, type CopyrightQueryParams } from '@/api/copyright';

const loading = ref(false);
const saving = ref(false);
const tableData = ref<CopyrightInfo[]>([]);
const pagination = reactive({ current: 1, size: 10, total: 0 });
const searchForm = reactive<CopyrightQueryParams>({ keyword: '', status: '', category: '' });

const dialogVisible = ref(false);
const formRef = ref<FormInstance>();
const form = reactive({ workName: '', author: '', category: '', description: '' });
const formRules: FormRules = {
  workName: [{ required: true, message: '请输入作品名称', trigger: 'blur' }],
  author: [{ required: true, message: '请输入作者', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
};

function getStatusType(status: string) {
  const map: Record<string, string> = { pending: 'warning', registered: 'success', rejected: 'danger' };
  return map[status] || 'info';
}
function getStatusText(status: string) {
  const map: Record<string, string> = { pending: '待审核', registered: '已登记', rejected: '已拒绝' };
  return map[status] || status;
}

async function loadData() {
  loading.value = true;
  try {
    const res = await listCopyrights({ ...searchForm, current: pagination.current, size: pagination.size });
    tableData.value = res.records;
    pagination.total = res.total;
  } catch { ElMessage.error('加载版权列表失败'); } finally { loading.value = false; }
}

function handleSearch() { pagination.current = 1; loadData(); }
function handleReset() { searchForm.keyword = ''; searchForm.status = ''; searchForm.category = ''; pagination.current = 1; loadData(); }

function resetForm() {
  form.workName = ''; form.author = ''; form.category = ''; form.description = '';
  formRef.value?.clearValidate();
}

function handleRegister() { resetForm(); dialogVisible.value = true; }

async function handleSubmit() {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    saving.value = true;
    try {
      await registerCopyright({ ...form });
      ElMessage.success('版权登记提交成功');
      dialogVisible.value = false;
      loadData();
    } catch { ElMessage.error('登记失败'); } finally { saving.value = false; }
  });
}

function handleView(row: CopyrightInfo) {
  ElMessage.info(`查看版权证书: ${row.workName} - 登记号: ${row.registrationNo || '待审核'}`);
}

async function handleApprove(row: CopyrightInfo) {
  try {
    await ElMessageBox.confirm(`确定通过作品 "${row.workName}" 的版权登记吗？`, '审核通过确认', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
    });
    await approveCopyright(row.id, true);
    row.status = 'registered';
    row.registrationNo = `CR${Date.now()}`;
    row.blockchainHash = `0x${Math.random().toString(16).slice(2, 10)}...`;
    ElMessage.success(`已登记版权: ${row.workName}`);
  } catch { /* 取消 */ }
}

async function handleReject(row: CopyrightInfo) {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝审核', {
      confirmButtonText: '确定', cancelButtonText: '取消', inputPlaceholder: '请输入拒绝原因...',
    });
    await approveCopyright(row.id, false, reason || '');
    row.status = 'rejected';
    ElMessage.success(`已拒绝版权登记: ${row.workName}`);
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
</style>