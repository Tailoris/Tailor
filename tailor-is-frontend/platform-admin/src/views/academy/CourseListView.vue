<template>
  <div v-loading="loading" class="page-container">
    <div class="page-header">
      <h2>课程管理</h2>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>新增课程
      </el-button>
    </div>
    <el-card class="page-card">
      <div class="search-bar">
        <el-input v-model="searchForm.keyword" placeholder="搜索课程名称/讲师" style="width: 200px" clearable @keyup.enter="handleSearch">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="searchForm.category" placeholder="分类" clearable style="width: 110px">
          <el-option label="打版" value="打版" />
          <el-option label="设计" value="设计" />
          <el-option label="工艺" value="工艺" />
        </el-select>
        <el-select v-model="searchForm.level" placeholder="等级" clearable style="width: 100px">
          <el-option label="入门" value="beginner" />
          <el-option label="进阶" value="intermediate" />
          <el-option label="高级" value="advanced" />
          <el-option label="大师" value="master" />
        </el-select>
        <el-select v-model="searchForm.status" placeholder="状态" clearable style="width: 110px">
          <el-option label="待审核" value="pending" />
          <el-option label="已发布" value="published" />
          <el-option label="已下架" value="offline" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
      <el-table :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="课程名称" min-width="160" />
        <el-table-column prop="instructor" label="讲师" width="100" />
        <el-table-column label="分类" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="等级" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="getLevelType(row.level)" size="small">{{ getLevelText(row.level) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="duration" label="时长" width="80" align="center" />
        <el-table-column label="课时" width="60" align="center">
          <template #default="{ row }">{{ row.lessons }}节</template>
        </el-table-column>
        <el-table-column label="学员" width="80" align="center">
          <template #default="{ row }">{{ row.students }}</template>
        </el-table-column>
        <el-table-column label="价格" width="90" align="right">
          <template #default="{ row }">{{ row.price === 0 ? '免费' : `¥${row.price}` }}</template>
        </el-table-column>
        <el-table-column label="评分" width="80" align="center">
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
              type="success" text size="small" @click="handlePublish(row)">发布
            </el-button>
            <el-button
              v-if="row.status === 'published'"
              type="warning" text size="small" @click="handleOffline(row)">下架
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
        <el-form-item label="课程名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入课程名称" maxlength="64" />
        </el-form-item>
        <el-form-item label="讲师" prop="instructor">
          <el-select v-model="form.instructor" placeholder="请选择讲师" style="width: 100%" filterable>
            <el-option label="李明" value="李明" />
            <el-option label="张伟" value="张伟" />
            <el-option label="王芳" value="王芳" />
            <el-option label="陈雪" value="陈雪" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%">
            <el-option label="打版" value="打版" />
            <el-option label="设计" value="设计" />
            <el-option label="工艺" value="工艺" />
            <el-option label="管理" value="管理" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程等级" prop="level">
          <el-select v-model="form.level" placeholder="请选择等级" style="width: 100%">
            <el-option label="入门" value="beginner" />
            <el-option label="进阶" value="intermediate" />
            <el-option label="高级" value="advanced" />
            <el-option label="大师" value="master" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程时长" prop="duration">
          <el-input v-model="form.duration" placeholder="如：3小时" maxlength="16" />
        </el-form-item>
        <el-form-item label="课时数" prop="lessons">
          <el-input-number v-model="form.lessons" :min="1" :max="100" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="课程价格" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="课程描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="课程描述（可选）" maxlength="256" />
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
import { listCourses, createCourse, updateCourse, deleteCourse, type CourseInfo, type AcademyQueryParams } from '@/api/academy';

const loading = ref(false);
const saving = ref(false);
const tableData = ref<CourseInfo[]>([]);
const pagination = reactive({ current: 1, size: 10, total: 0 });
const searchForm = reactive<AcademyQueryParams>({ keyword: '', category: '', level: '', status: '' });

const dialogVisible = ref(false);
const dialogTitle = ref('新增课程');
const editingId = ref<number | null>(null);
const formRef = ref<FormInstance>();
const form = reactive<Partial<CourseInfo>>({
  name: '', instructor: '', category: '', level: 'beginner', duration: '', lessons: 1, price: 0, description: '',
});
const formRules: FormRules = {
  name: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  instructor: [{ required: true, message: '请选择讲师', trigger: 'change' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  level: [{ required: true, message: '请选择课程等级', trigger: 'change' }],
  duration: [{ required: true, message: '请输入课程时长', trigger: 'blur' }],
};

function getLevelType(level: string) {
  const map: Record<string, string> = { beginner: 'info', intermediate: '', advanced: 'warning', master: 'danger' };
  return map[level] || 'info';
}
function getLevelText(level: string) {
  const map: Record<string, string> = { beginner: '入门', intermediate: '进阶', advanced: '高级', master: '大师' };
  return map[level] || level;
}
function getStatusType(status: string) {
  const map: Record<string, string> = { pending: 'warning', published: 'success', offline: 'info' };
  return map[status] || 'info';
}
function getStatusText(status: string) {
  const map: Record<string, string> = { pending: '待审核', published: '已发布', offline: '已下架' };
  return map[status] || status;
}

async function loadData() {
  loading.value = true;
  try {
    const res = await listCourses({ ...searchForm, current: pagination.current, size: pagination.size });
    tableData.value = res.records;
    pagination.total = res.total;
  } catch { ElMessage.error('加载课程列表失败'); } finally { loading.value = false; }
}

function handleSearch() { pagination.current = 1; loadData(); }
function handleReset() { searchForm.keyword = ''; searchForm.category = ''; searchForm.level = ''; searchForm.status = ''; pagination.current = 1; loadData(); }

function resetForm() {
  editingId.value = null;
  Object.assign(form, { name: '', instructor: '', category: '', level: 'beginner', duration: '', lessons: 1, price: 0, description: '' });
  formRef.value?.clearValidate();
}

function handleAdd() { resetForm(); dialogTitle.value = '新增课程'; dialogVisible.value = true; }

function handleEdit(row: CourseInfo) {
  editingId.value = row.id; dialogTitle.value = '编辑课程';
  form.name = row.name; form.instructor = row.instructor; form.category = row.category;
  form.level = row.level; form.duration = row.duration; form.lessons = row.lessons;
  form.price = row.price; form.description = row.description;
  dialogVisible.value = true;
}

async function handleSubmit() {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    saving.value = true;
    try {
      editingId.value ? await updateCourse(editingId.value, { ...form }) : await createCourse({ ...form });
      ElMessage.success(editingId.value ? '课程更新成功' : '课程创建成功');
      dialogVisible.value = false; loadData();
    } catch { ElMessage.error('操作失败'); } finally { saving.value = false; }
  });
}

async function handlePublish(row: CourseInfo) {
  try {
    await updateCourse(row.id, { status: 'published' });
    row.status = 'published';
    ElMessage.success(`已发布课程: ${row.name}`);
  } catch { ElMessage.error('操作失败'); }
}

async function handleOffline(row: CourseInfo) {
  try {
    await updateCourse(row.id, { status: 'offline' });
    row.status = 'offline';
    ElMessage.success(`已下架课程: ${row.name}`);
  } catch { ElMessage.error('操作失败'); }
}

async function handleDelete(row: CourseInfo) {
  try {
    await ElMessageBox.confirm(`确定要删除课程 "${row.name}" 吗？`, '删除确认', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
    });
    await deleteCourse(row.id);
    ElMessage.success(`已删除课程: ${row.name}`);
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