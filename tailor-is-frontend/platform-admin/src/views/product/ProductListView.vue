<template>
  <div v-loading="loading" class="page-container">
    <div class="page-header">
      <h2>商品管理</h2>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>新增商品
      </el-button>
    </div>
    <el-card class="page-card">
      <div class="search-bar">
        <el-input v-model="searchForm.keyword" placeholder="搜索商品名称/编码/商家" style="width: 240px" clearable @keyup.enter="handleSearch">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="searchForm.status" placeholder="状态筛选" clearable style="width: 130px">
          <el-option label="上架" value="online" />
          <el-option label="下架" value="offline" />
          <el-option label="待审核" value="pending" />
          <el-option label="已拒绝" value="rejected" />
        </el-select>
        <el-select v-model="searchForm.category" placeholder="分类筛选" clearable style="width: 130px">
          <el-option label="女装" value="女装" />
          <el-option label="男装" value="男装" />
          <el-option label="童装" value="童装" />
          <el-option label="裤装" value="裤装" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
      <el-table :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="商品名称" min-width="180" />
        <el-table-column prop="code" label="商品编码" width="120" />
        <el-table-column prop="category" label="分类" width="80" />
        <el-table-column prop="merchantName" label="商家" min-width="150" />
        <el-table-column label="价格" width="120" align="right">
          <template #default="{ row }">
            <span class="price">¥{{ row.price }}</span>
            <span class="original-price">¥{{ row.originalPrice }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="saleCount" label="销量" width="80" align="center" />
        <el-table-column prop="stock" label="库存" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="warning" text size="small" @click="handleToggle(row)">
              {{ row.status === 'online' ? '下架' : '上架' }}
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
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入商品名称" maxlength="64" />
        </el-form-item>
        <el-form-item label="商品编码" prop="code">
          <el-input v-model="form.code" placeholder="请输入商品编码" maxlength="32" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%">
            <el-option label="女装" value="女装" />
            <el-option label="男装" value="男装" />
            <el-option label="童装" value="童装" />
            <el-option label="裤装" value="裤装" />
          </el-select>
        </el-form-item>
        <el-form-item label="售价" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="原价" prop="originalPrice">
          <el-input-number v-model="form.originalPrice" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="form.stock" :min="0" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="商品描述（可选）" maxlength="256" />
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
  listProducts, createProduct, updateProduct, toggleProductStatus, deleteProduct,
  type ProductInfo, type ProductForm, type ProductQueryParams,
} from '@/api/product';

const loading = ref(false);
const saving = ref(false);
const tableData = ref<ProductInfo[]>([]);
const pagination = reactive({ current: 1, size: 10, total: 0 });
const searchForm = reactive<ProductQueryParams>({ keyword: '', status: '', category: '' });

const dialogVisible = ref(false);
const dialogTitle = ref('新增商品');
const editingId = ref<number | null>(null);
const formRef = ref<FormInstance>();
const form = reactive<ProductForm>({
  name: '', code: '', category: '', merchantId: 0, price: 0, originalPrice: 0, stock: 0, description: '', mainImage: '',
});
const formRules: FormRules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入商品编码', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入售价', trigger: 'blur' }],
};

function getStatusType(status: string) {
  const map: Record<string, string> = { online: 'success', offline: 'info', pending: 'warning', rejected: 'danger' };
  return map[status] || 'info';
}
function getStatusText(status: string) {
  const map: Record<string, string> = { online: '上架', offline: '下架', pending: '待审核', rejected: '已拒绝' };
  return map[status] || status;
}

async function loadData() {
  loading.value = true;
  try {
    const res = await listProducts({ ...searchForm, current: pagination.current, size: pagination.size });
    tableData.value = res.records;
    pagination.total = res.total;
  } catch { ElMessage.error('加载商品列表失败'); } finally { loading.value = false; }
}

function handleSearch() { pagination.current = 1; loadData(); }
function handleReset() { searchForm.keyword = ''; searchForm.status = ''; searchForm.category = ''; pagination.current = 1; loadData(); }

function resetForm() {
  editingId.value = null;
  Object.assign(form, { name: '', code: '', category: '', merchantId: 0, price: 0, originalPrice: 0, stock: 0, description: '', mainImage: '' });
  formRef.value?.clearValidate();
}

function handleAdd() { resetForm(); dialogTitle.value = '新增商品'; dialogVisible.value = true; }

function handleEdit(row: ProductInfo) {
  editingId.value = row.id;
  dialogTitle.value = '编辑商品';
  form.name = row.name; form.code = row.code; form.category = row.category;
  form.price = row.price; form.originalPrice = row.originalPrice; form.stock = row.stock;
  form.description = row.description;
  dialogVisible.value = true;
}

async function handleSubmit() {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    saving.value = true;
    try {
      editingId.value ? await updateProduct(editingId.value, { ...form }) : await createProduct({ ...form });
      ElMessage.success(editingId.value ? '商品更新成功' : '商品创建成功');
      dialogVisible.value = false;
      loadData();
    } catch { ElMessage.error('操作失败'); } finally { saving.value = false; }
  });
}

async function handleToggle(row: ProductInfo) {
  const newStatus = row.status === 'online' ? 'offline' : 'online';
  const action = newStatus === 'offline' ? '下架' : '上架';
  try {
    await ElMessageBox.confirm(`确定要${action}商品 "${row.name}" 吗？`, `${action}确认`, {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
    });
    await toggleProductStatus(row.id, newStatus);
    row.status = newStatus;
    ElMessage.success(`已${action}商品: ${row.name}`);
  } catch { /* 取消 */ }
}

async function handleDelete(row: ProductInfo) {
  try {
    await ElMessageBox.confirm(`确定要删除商品 "${row.name}" 吗？`, '删除确认', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
    });
    await deleteProduct(row.id);
    ElMessage.success(`已删除商品: ${row.name}`);
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
.price { color: #ef4444; font-weight: 600; margin-right: 8px; }
.original-price { color: #9ca3af; text-decoration: line-through; font-size: 12px; }
</style>