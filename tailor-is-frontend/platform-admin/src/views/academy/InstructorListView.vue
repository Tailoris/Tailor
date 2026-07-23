<template>
  <div v-loading="loading" class="page-container">
    <div class="page-header">
      <h2>讲师管理</h2>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>新增讲师
      </el-button>
    </div>
    <el-card class="page-card">
      <div class="search-bar">
        <el-input v-model="searchForm.keyword" placeholder="搜索讲师名称/职称" style="width: 220px" clearable @keyup.enter="handleSearch">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="searchForm.status" placeholder="状态" clearable style="width: 120px">
          <el-option label="待审核" value="pending" />
          <el-option label="已认证" value="verified" />
          <el-option label="已禁用" value="disabled" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
      <el-table :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="讲师名称" min-width="120" />
        <el-table-column prop="title" label="职称" width="100" />
        <el-table-column label="擅长领域" min-width="180">
          <template #default="{ row }">
            <el-tag v-for="s in row.specialty" :key="s" size="small" style="margin-right: 4px; margin-bottom: 2px">{{ s }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="experience" label="从业经验" width="90" align="center" />
        <el-table-column label="课程数" width="80" align="center">
          <template #default="{ row }">{{ row.courses }}</template>
        </el-table-column>
        <el-table-column label="学员数" width="90" align="center">
          <template #default="{ row }">{{ row.students.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="评分" width="90" align="center">
          <template #default="{ row }">
            <el-rate v-model="row.rating" disabled :max="5" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button
              v-if="row.status === 'pending'"
              type="success" text size="small" @click="handleVerify(row)">认证
            </el-button>
            <el-button
              v-if="row.status === 'verified'"
              type="warning" text size="small" @click="handleDisable(row)">禁用
            </el-button>
            <el-button
              v-if="row.status === 'disabled'"
              type="success" text size="small" @click="handleVerify(row)">启用
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
        <el-form-item label="讲师名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入讲师名称" maxlength="32" />
        </el-form-item>
        <el-form-item label="职称" prop="title">
          <el-input v-model="form.title" placeholder="如：高级版师" maxlength="32" />
        </el-form-item>
        <el-form-item label="从业经验" prop="experience">
          <el-input v-model="form.experience" placeholder="如：15年" maxlength="16" />
        </el-form-item>
        <el-form-item label="擅长领域" prop="specialty">
          <el-select v-model="form.specialty" multiple placeholder="请选择擅长领域" style="width: 100%">
            <el-option label="打版" value="打版" />
            <el-option label="设计" value="设计" />
            <el-option label="立裁" value="立裁" />
            <el-option label="工艺" value="工艺" />
            <el-option label="AI" value="AI" />
            <el-option label="教学" value="教学" />
          </el-select>
        </el-form-item>
        <el-form-item label="讲师描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="讲师描述（可选）" maxlength="256" />
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
import { listInstructors, createInstructor, updateInstructor, deleteInstructor, type InstructorInfo, type AcademyQueryParams } from '@/api/academy';

const loading = ref(false);
const saving = ref(false);
const tableData = ref<InstructorInfo[]>([]);
const pagination = reactive({ current: 1, size: 10, total: 0 });
const searchForm = reactive<AcademyQueryParams>({ keyword: '', status: '' });

const dialogVisible = ref(false);
const dialogTitle = ref('新增讲师');
const editingId = ref<number | null>(null);
const formRef = ref<FormInstance>();
const form = reactive<Partial<InstructorInfo>>({
  name: '', title: '', experience: '', specialty: [], description: '',
});
const formRules: FormRules = {
  name: [{ required: true, message: '请输入讲师名称', trigger: 'blur' }],
  title: [{ required: true, message: '请输入职称', trigger: 'blur' }],
  experience: [{ required: true, message: '请输入从业经验', trigger: 'blur' }],
};

function getStatusType(status: string) {
  const map: Record<string, string> = { pending: 'warning', verified: 'success', disabled: 'info' };
  return map[status] || 'info';
}
function getStatusText(status: string) {
  const map: Record<string, string> = { pending: '待审核', verified: '已认证', disabled: '已禁用' };
  return map[status] || status;
}

async function loadData() {
  loading.value = true;
  try {
    const res = await listInstructors({ ...searchForm, current: pagination.current, size: pagination.size });
    tableData.value = res.records;
    pagination.total = res.total;
  } catch { ElMessage.error('加载讲师列表失败'); } finally { loading.value = false; }
}

function handleSearch() { pagination.current = 1; loadData(); }
function handleReset() { searchForm.keyword = ''; searchForm.status = ''; pagination.current = 1; loadData(); }

function resetForm() {
  editingId.value = null;
  Object.assign(form, { name: '', title: '', experience: '', specialty: [], description: '' });
  formRef.value?.clearValidate();
}

function handleAdd() { resetForm(); dialogTitle.value = '新增讲师'; dialogVisible.value = true; }

function handleEdit(row: InstructorInfo) {
  editingId.value = row.id; dialogTitle.value = '编辑讲师';
  form.name = row.name; form.title = row.title; form.experience = row.experience;
  form.specialty = [...row.specialty]; form.description = row.description;
  dialogVisible.value = true;
}

async function handleSubmit() {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    saving.value = true;
    try {
      editingId.value ? await updateInstructor(editingId.value, { ...form }) : await createInstructor({ ...form });
      ElMessage.success(editingId.value ? '讲师信息更新成功' : '讲师创建成功');
      dialogVisible.value = false; loadData();
    } catch { ElMessage.error('操作失败'); } finally { saving.value = false; }
  });
}

async function handleVerify(row: InstructorInfo) {
  try {
    await updateInstructor(row.id, { status: 'verified' });
    row.status = 'verified';
    ElMessage.success(`已认证讲师: ${row.name}`);
  } catch { ElMessage.error('操作失败'); }
}

async function handleDisable(row: InstructorInfo) {
  try {
    await updateInstructor(row.id, { status: 'disabled' });
    row.status = 'disabled';
    ElMessage.success(`已禁用讲师: ${row.name}`);
  } catch { ElMessage.error('操作失败'); }
}

async function handleDelete(row: InstructorInfo) {
  try {
    await ElMessageBox.confirm(`确定要删除讲师 "${row.name}" 吗？`, '删除确认', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
    });
    await deleteInstructor(row.id);
    ElMessage.success(`已删除讲师: ${row.name}`);
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