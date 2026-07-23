<template>
  <div
    v-loading="loading"
    class="role-list-page"
  >
    <div class="page-header">
      <h2 class="page-title">
        角色管理
      </h2>
      <p class="page-subtitle">
        管理系统角色及权限分配
      </p>
    </div>

    <div class="toolbar">
      <el-button
        type="primary"
        @click="handleAdd"
      >
        <el-icon><Plus /></el-icon>
        新增角色
      </el-button>
      <el-input
        v-model="searchKeyword"
        placeholder="搜索角色名称"
        class="search-input"
        clearable
        @keyup.enter="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <el-table
      :data="roleList"
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
        prop="name"
        label="角色名称"
        min-width="150"
      />
      <el-table-column
        prop="code"
        label="角色编码"
        min-width="150"
      />
      <el-table-column
        prop="description"
        label="描述"
        min-width="200"
      />
      <el-table-column
        prop="userCount"
        label="关联用户数"
        width="120"
        align="center"
      />
      <el-table-column
        prop="createTime"
        label="创建时间"
        width="180"
      />
      <el-table-column
        label="操作"
        width="220"
        fixed="right"
      >
        <template #default="scope">
          <el-button
            size="small"
            @click="handleEdit(scope.row)"
          >
            编辑
          </el-button>
          <el-button
            size="small"
            type="warning"
            @click="handlePermissions(scope.row)"
          >
            权限
          </el-button>
          <el-button
            size="small"
            type="danger"
            @click="handleDelete(scope.row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadRoles"
        @current-change="loadRoles"
      />
    </div>

    <!-- 新增/编辑角色弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="90px"
      >
        <el-form-item
          label="角色名称"
          prop="name"
        >
          <el-input
            v-model="form.name"
            placeholder="请输入角色名称"
          />
        </el-form-item>
        <el-form-item
          label="角色编码"
          prop="code"
        >
          <el-input
            v-model="form.code"
            placeholder="请输入角色编码（如 ROLE_ADMIN）"
          />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="角色描述（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          @click="handleSubmit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { Plus, Search } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { listRoles, createRole, updateRole, deleteRole, type Role, type RoleForm } from '@/api/role';

const loading = ref(false);
const searchKeyword = ref('');
const roleList = ref<Role[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

// 新增/编辑弹窗
const dialogVisible = ref(false);
const dialogTitle = ref('新增角色');
const editingId = ref<number | null>(null);
const formRef = ref();
const form = reactive<RoleForm>({
  name: '',
  code: '',
  description: '',
});
const formRules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
};

async function loadRoles() {
  loading.value = true;
  try {
    const res = await listRoles({
      keyword: searchKeyword.value,
      current: currentPage.value,
      size: pageSize.value,
    });
    roleList.value = res.records;
    total.value = res.total;
  } catch {
    console.warn('加载角色列表失败，使用本地数据');
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  currentPage.value = 1;
  loadRoles();
}

function handleAdd() {
  dialogTitle.value = '新增角色';
  editingId.value = null;
  form.name = '';
  form.code = '';
  form.description = '';
  dialogVisible.value = true;
}

function handleEdit(row: Role) {
  dialogTitle.value = '编辑角色';
  editingId.value = row.id;
  form.name = row.name;
  form.code = row.code;
  form.description = row.description;
  dialogVisible.value = true;
}

async function handleSubmit() {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return;
    try {
      if (editingId.value) {
        await updateRole(editingId.value, { ...form });
        ElMessage.success('角色更新成功');
      } else {
        await createRole({ ...form });
        ElMessage.success('角色创建成功');
      }
      dialogVisible.value = false;
      loadRoles();
    } catch {
      ElMessage.error(editingId.value ? '更新角色失败' : '创建角色失败');
    }
  });
}

function handlePermissions(row: Role) {
  ElMessage.info(`角色 "${row.name}" 权限分配功能请通过权限管理页面操作`);
}

async function handleDelete(row: Role) {
  try {
    await ElMessageBox.confirm(`确定要删除角色 "${row.name}" 吗？删除后关联用户将失去对应权限。`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });
    await deleteRole(row.id);
    ElMessage.success(`角色 "${row.name}" 已删除`);
    loadRoles();
  } catch {
    // 用户取消或删除失败
  }
}

onMounted(() => {
  loadRoles();
});
</script>

<style scoped>
.role-list-page {
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
}

.search-input {
  width: 280px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
