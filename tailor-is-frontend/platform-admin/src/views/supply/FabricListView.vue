<template>
  <div v-loading="loading" class="page-container">
    <div class="page-header">
      <h2>面料管理</h2>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>新增面料
      </el-button>
    </div>
    <el-card class="page-card">
      <div class="search-bar">
        <el-input v-model="searchForm.keyword" placeholder="搜索面料名称/编码/供应商" style="width: 240px" clearable @keyup.enter="handleSearch">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="searchForm.status" placeholder="状态筛选" clearable style="width: 130px">
          <el-option label="启用" value="active" />
          <el-option label="停用" value="inactive" />
        </el-select>
        <el-select v-model="searchForm.category" placeholder="分类筛选" clearable style="width: 130px">
          <el-option label="棉" value="棉" />
          <el-option label="化纤" value="化纤" />
          <el-option label="丝绸" value="丝绸" />
          <el-option label="牛仔" value="牛仔" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
      <el-table :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="面料名称" min-width="150" />
        <el-table-column prop="code" label="编码" width="100" />
        <el-table-column prop="category" label="分类" width="80" />
        <el-table-column prop="composition" label="成分" width="120" />
        <el-table-column prop="width" label="幅宽" width="80" />
        <el-table-column prop="weight" label="克重" width="80" />
        <el-table-column label="单价" width="80" align="right">
          <template #default="{ row }">¥{{ row.price }}/{{ row.unit }}</template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="80" align="center" />
        <el-table-column prop="supplier" label="供应商" min-width="120" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'info'" size="small">
              {{ row.status === 'active' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="handleEdit(row)">编辑</el-button>
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
        <el-form-item label="面料名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入面料名称" maxlength="64" />
        </el-form-item>
        <el-form-item label="编码" prop="code">
          <el-input v-model="form.code" placeholder="请输入面料编码" maxlength="32" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%">
            <el-option label="棉" value="棉" /><el-option label="化纤" value="化纤" />
            <el-option label="丝绸" value="丝绸" /><el-option label="牛仔" value="牛仔" />
          </el-select>
        </el-form-item>
        <el-form-item label="成分" prop="composition">
          <el-input v-model="form.composition" placeholder="如：100%棉" maxlength="32" />
        </el-form-item>
        <el-form-item label="幅宽" prop="width">
          <el-input v-model="form.width" placeholder="如：150cm" maxlength="16" />
        </el-form-item>
        <el-form-item label="克重" prop="weight">
          <el-input v-model="form.weight" placeholder="如：180g/m²" maxlength="16" />
        </el-form-item>
        <el-form-item label="单价" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="form.stock" :min="0" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="面料描述（可选）" maxlength="128" />
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
import { listFabrics, createFabric, updateFabric, deleteFabric, type FabricInfo, type SupplyQueryParams } from '@/api/supply';

const loading = ref(false);
const saving = ref(false);
const tableData = ref<FabricInfo[]>([]);
const pagination = reactive({ current: 1, size: 10, total: 0 });
const searchForm = reactive<SupplyQueryParams>({ keyword: '', status: '', category: '' });

const dialogVisible = ref(false);
const dialogTitle = ref('新增面料');
const editingId = ref<number | null>(null);
const formRef = ref<FormInstance>();
const form = reactive<Partial<FabricInfo>>({ name: '', code: '', category: '', composition: '', width: '', weight: '', price: 0, stock: 0, description: '' });
const formRules: FormRules = {
  name: [{ required: true, message: '请输入面料名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入面料编码', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入单价', trigger: 'blur' }],
};

async function loadData() {
  loading.value = true;
  try {
    const res = await listFabrics({ ...searchForm, current: pagination.current, size: pagination.size });
    tableData.value = res.records;
    pagination.total = res.total;
  } catch { ElMessage.error('加载面料列表失败'); } finally { loading.value = false; }
}

function handleSearch() { pagination.current = 1; loadData(); }
function handleReset() { searchForm.keyword = ''; searchForm.status = ''; searchForm.category = ''; pagination.current = 1; loadData(); }

function resetForm() {
  editingId.value = null;
  Object.assign(form, { name: '', code: '', category: '', composition: '', width: '', weight: '', price: 0, stock: 0, description: '' });
  formRef.value?.clearValidate();
}

function handleAdd() { resetForm(); dialogTitle.value = '新增面料'; dialogVisible.value = true; }

function handleEdit(row: FabricInfo) {
  editingId.value = row.id; dialogTitle.value = '编辑面料';
  form.name = row.name; form.code = row.code; form.category = row.category;
  form.composition = row.composition; form.width = row.width; form.weight = row.weight;
  form.price = row.price; form.stock = row.stock; form.description = row.description;
  dialogVisible.value = true;
}

async function handleSubmit() {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    saving.value = true;
    try {
      editingId.value ? await updateFabric(editingId.value, { ...form }) : await createFabric({ ...form });
      ElMessage.success(editingId.value ? '面料更新成功' : '面料创建成功');
      dialogVisible.value = false; loadData();
    } catch { ElMessage.error('操作失败'); } finally { saving.value = false; }
  });
}

async function handleDelete(row: FabricInfo) {
  try {
    await ElMessageBox.confirm(`确定要删除面料 "${row.name}" 吗？`, '删除确认', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
    });
    await deleteFabric(row.id);
    ElMessage.success(`已删除面料: ${row.name}`);
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