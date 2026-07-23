<template>
  <div v-loading="loading" class="page-container">
    <div class="page-header">
      <h2>商家管理</h2>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>新增商家
      </el-button>
    </div>
    <el-card class="page-card">
      <div class="search-bar">
        <el-input v-model="searchForm.keyword" placeholder="搜索商家名称/联系人/手机号" style="width: 240px" clearable @keyup.enter="handleSearch">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="searchForm.status" placeholder="状态筛选" clearable style="width: 130px">
          <el-option label="待审核" value="pending" />
          <el-option label="已通过" value="approved" />
          <el-option label="已拒绝" value="rejected" />
          <el-option label="已禁用" value="disabled" />
        </el-select>
        <el-select v-model="searchForm.shopType" placeholder="店铺类型" clearable style="width: 130px">
          <el-option label="个人店" value="personal" />
          <el-option label="企业店" value="enterprise" />
          <el-option label="品牌店" value="brand" />
          <el-option label="工厂店" value="factory" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
      <el-table :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="商家名称" min-width="150" />
        <el-table-column label="店铺类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="getShopTypeTag(row.shopType)">{{ getShopTypeText(row.shopType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="contact" label="联系人" width="100" />
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="warning" text size="small" @click="handleToggleStatus(row)">
              {{ row.status === 'disabled' ? '启用' : '禁用' }}
            </el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="商家名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入商家名称" maxlength="64" />
        </el-form-item>
        <el-form-item label="店铺类型" prop="shopType">
          <el-select v-model="form.shopType" placeholder="请选择店铺类型" style="width: 100%">
            <el-option label="个人店" value="personal" />
            <el-option label="企业店" value="enterprise" />
            <el-option label="品牌店" value="brand" />
            <el-option label="工厂店" value="factory" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系人" prop="contact">
          <el-input v-model="form.contact" placeholder="请输入联系人姓名" maxlength="32" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入联系电话" maxlength="11" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱地址" maxlength="64" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入商家地址" maxlength="128" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="商家简介（可选）" maxlength="256" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { Plus, Search } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import {
  listMerchants, createMerchant, updateMerchant, toggleMerchantStatus, deleteMerchant,
  type MerchantInfo, type MerchantForm, type MerchantQueryParams,
} from '@/api/merchant';

const loading = ref(false);
const saving = ref(false);
const tableData = ref<MerchantInfo[]>([]);
const pagination = reactive({ current: 1, size: 10, total: 0 });
const searchForm = reactive<MerchantQueryParams>({ keyword: '', status: '', shopType: '' });

const dialogVisible = ref(false);
const dialogTitle = ref('新增商家');
const editingId = ref<number | null>(null);
const formRef = ref<FormInstance>();
const form = reactive<MerchantForm>({
  name: '', shopType: 'personal', contact: '', phone: '', email: '', address: '', description: '',
});
const formRules: FormRules = {
  name: [{ required: true, message: '请输入商家名称', trigger: 'blur' }],
  contact: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
  email: [{ type: 'email', message: '请输入正确的邮箱', trigger: 'blur' }],
};

function getShopTypeTag(type: string) {
  const map: Record<string, string> = { personal: '', enterprise: 'success', brand: 'warning', factory: 'info' };
  return map[type] || '';
}
function getShopTypeText(type: string) {
  const map: Record<string, string> = { personal: '个人店', enterprise: '企业店', brand: '品牌店', factory: '工厂店' };
  return map[type] || type;
}
function getStatusType(status: string) {
  const map: Record<string, string> = { pending: 'warning', approved: 'success', rejected: 'danger', disabled: 'info' };
  return map[status] || 'info';
}
function getStatusText(status: string) {
  const map: Record<string, string> = { pending: '待审核', approved: '正常', rejected: '已拒绝', disabled: '已禁用' };
  return map[status] || status;
}

async function loadData() {
  loading.value = true;
  try {
    const res = await listMerchants({ ...searchForm, current: pagination.current, size: pagination.size });
    tableData.value = res.records;
    pagination.total = res.total;
  } catch {
    ElMessage.error('加载商家列表失败');
  } finally {
    loading.value = false;
  }
}

function handleSearch() { pagination.current = 1; loadData(); }
function handleReset() { searchForm.keyword = ''; searchForm.status = ''; searchForm.shopType = ''; pagination.current = 1; loadData(); }

function resetForm() {
  editingId.value = null;
  Object.assign(form, {
    name: '', shopType: 'personal', contact: '', phone: '', email: '', address: '', description: '',
  });
  formRef.value?.clearValidate();
}

function handleAdd() {
  resetForm();
  dialogTitle.value = '新增商家';
  dialogVisible.value = true;
}

function handleEdit(row: MerchantInfo) {
  editingId.value = row.id;
  dialogTitle.value = '编辑商家';
  form.name = row.name;
  form.shopType = row.shopType;
  form.contact = row.contact;
  form.phone = row.phone;
  form.email = row.email;
  form.address = row.address;
  form.description = row.description;
  dialogVisible.value = true;
}

async function handleSubmit() {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    saving.value = true;
    try {
      if (editingId.value) {
        await updateMerchant(editingId.value, { ...form });
        ElMessage.success('商家更新成功');
      } else {
        await createMerchant({ ...form });
        ElMessage.success('商家创建成功');
      }
      dialogVisible.value = false;
      loadData();
    } catch {
      ElMessage.error('操作失败');
    } finally {
      saving.value = false;
    }
  });
}

async function handleToggleStatus(row: MerchantInfo) {
  const newStatus = row.status === 'disabled' ? 'approved' : 'disabled';
  const action = newStatus === 'disabled' ? '禁用' : '启用';
  try {
    await ElMessageBox.confirm(`确定要${action}商家 "${row.name}" 吗？`, `${action}确认`, {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
    });
    await toggleMerchantStatus(row.id, newStatus);
    row.status = newStatus;
    ElMessage.success(`已${action}商家: ${row.name}`);
  } catch { /* 取消 */ }
}

async function handleDelete(row: MerchantInfo) {
  try {
    await ElMessageBox.confirm(`确定要删除商家 "${row.name}" 吗？此操作不可恢复。`, '删除确认', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
    });
    await deleteMerchant(row.id);
    ElMessage.success(`已删除商家: ${row.name}`);
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
</style>