<template>
  <div class="user-list-page" v-loading="loading">
    <div class="page-header">
      <h2 class="page-title">用户管理</h2>
      <p class="page-subtitle">管理平台用户、角色分配及状态控制</p>
    </div>

    <div class="toolbar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增用户
      </el-button>
      <el-input
        v-model="searchKeyword"
        placeholder="搜索用户名/手机号/邮箱"
        class="search-input"
        clearable
        @keyup.enter="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable class="status-select">
        <el-option label="全部" value="" />
        <el-option label="启用" value="1" />
        <el-option label="禁用" value="0" />
        <el-option label="锁定" value="2" />
      </el-select>
    </div>

    <el-table :data="userList" border stripe style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="realName" label="真实姓名" min-width="120" />
      <el-table-column prop="phone" label="手机号" width="140" />
      <el-table-column prop="email" label="邮箱" min-width="200" />
      <el-table-column label="角色" min-width="150">
        <template #default="scope">
          <el-tag
            v-for="role in scope.row.roles"
            :key="role"
            size="small"
            style="margin-right: 4px"
          >
            {{ role }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.status)">
            {{ statusLabel(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="scope">
          <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button size="small" type="warning" @click="handleAssignRole(scope.row)">分配角色</el-button>
          <el-button
            size="small"
            :type="scope.row.status === 1 ? 'danger' : 'success'"
            @click="handleToggleStatus(scope.row)"
          >
            {{ scope.row.status === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-button size="small" type="danger" @click="handleResetPassword(scope.row)">重置密码</el-button>
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
        @size-change="loadUsers"
        @current-change="loadUsers"
      />
    </div>

    <el-dialog :title="formMode === 'create' ? '新增用户' : '编辑用户'" v-model="formDialogVisible" width="500px">
      <el-form ref="formRef" :model="userForm" label-width="100px">
        <el-form-item label="用户名" prop="username" :rules="[{ required: true, message: '请输入用户名', trigger: 'blur' }]">
          <el-input v-model="userForm.username" :disabled="formMode === 'edit'" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName" :rules="[{ required: true, message: '请输入真实姓名', trigger: 'blur' }]">
          <el-input v-model="userForm.realName" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" />
        </el-form-item>
        <el-form-item label="密码" v-if="formMode === 'create'" prop="password">
          <el-input v-model="userForm.password" type="password" placeholder="不填则默认为123456" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="userForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitForm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog title="分配角色" v-model="roleDialogVisible" width="500px">
      <el-form label-width="100px">
        <el-form-item label="可用角色">
          <el-checkbox-group v-model="selectedRoleIds">
            <el-checkbox v-for="role in availableRoles" :key="role.id" :label="role.id">
              {{ role.name }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitRoles">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { Plus, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listUsers,
  toggleUserStatus,
  resetUserPassword,
  createUser,
  updateUser,
  assignUserRoles,
  type UserInfo,
  type UserForm,
} from '@/api/user'
import type { RoleInfo } from '@/api/role'

// 🔒 H-16修复: 接入真实用户管理 API

const loading = ref(false)
const searchKeyword = ref('')
const statusFilter = ref('')
const userList = ref<UserInfo[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 编辑/新增弹窗
const formDialogVisible = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const formRef = ref()
const userForm = reactive<UserForm>({
  username: '',
  realName: '',
  phone: '',
  email: '',
  password: '',
  roleIds: [],
  status: 1,
})

// 角色分配弹窗
const roleDialogVisible = ref(false)
const currentUserId = ref<number | null>(null)
const availableRoles = ref<RoleInfo[]>([])
const selectedRoleIds = ref<number[]>([])

function statusTagType(status: number): '' | 'success' | 'danger' | 'warning' {
  const map: Record<number, '' | 'success' | 'danger' | 'warning'> = {
    1: 'success',
    0: 'danger',
    2: 'warning',
  }
  return map[status] || ''
}

function statusLabel(status: number): string {
  const map: Record<number, string> = { 1: '启用', 0: '禁用', 2: '锁定' }
  return map[status] || '未知'
}

async function loadUsers() {
  loading.value = true
  try {
    const res = await listUsers({
      keyword: searchKeyword.value || undefined,
      status: statusFilter.value || undefined,
      current: currentPage.value,
      size: pageSize.value,
    })
    userList.value = res.records
    total.value = res.total
  } catch (error) {
    console.error('加载用户列表失败:', error)
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  loadUsers()
}

function resetForm() {
  Object.assign(userForm, {
    id: undefined,
    username: '',
    realName: '',
    phone: '',
    email: '',
    password: '',
    roleIds: [],
    status: 1,
  })
  formRef.value?.clearValidate()
}

function handleAdd() {
  resetForm()
  formMode.value = 'create'
  formDialogVisible.value = true
}

function handleEdit(row: UserInfo) {
  resetForm()
  formMode.value = 'edit'
  Object.assign(userForm, {
    id: row.id,
    username: row.username,
    realName: row.realName,
    phone: row.phone,
    email: row.email,
    roleIds: row.roles.map(() => 0), // 实际应查询用户角色 ID
    status: row.status,
  })
  formDialogVisible.value = true
}

async function handleSubmitForm() {
  try {
    await formRef.value?.validate()
    if (formMode.value === 'create') {
      await createUser(userForm)
      ElMessage.success('用户创建成功')
    } else if (userForm.id) {
      await updateUser(userForm.id, userForm)
      ElMessage.success('用户更新成功')
    }
    formDialogVisible.value = false
    loadUsers()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('保存用户失败:', error)
      ElMessage.error('保存用户失败')
    }
  }
}

async function handleAssignRole(row: UserInfo) {
  currentUserId.value = row.id
  selectedRoleIds.value = []
  // 加载可用角色
  try {
    const { listAvailableRoles } = await import('@/api/user')
    availableRoles.value = await listAvailableRoles()
    roleDialogVisible.value = true
  } catch (error) {
    console.error('加载角色列表失败:', error)
    ElMessage.error('加载角色列表失败')
  }
}

async function handleSubmitRoles() {
  if (!currentUserId.value) return
  try {
    await assignUserRoles(currentUserId.value, selectedRoleIds.value)
    ElMessage.success('角色分配成功')
    roleDialogVisible.value = false
    loadUsers()
  } catch (error) {
    console.error('角色分配失败:', error)
    ElMessage.error('角色分配失败')
  }
}

async function handleToggleStatus(row: UserInfo) {
  const newStatus = row.status === 1 ? 0 : 1
  const action = row.status === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确定要${action}用户 "${row.username}" 吗？`,
      `${action}确认`,
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await toggleUserStatus(row.id, newStatus)
    ElMessage.success(`用户 "${row.username}" 已${action}`)
    loadUsers()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('切换用户状态失败:', error)
      ElMessage.error('切换用户状态失败')
    }
  }
}

async function handleResetPassword(row: UserInfo) {
  try {
    await ElMessageBox.confirm(
      `确定要重置用户 "${row.username}" 的密码吗？新密码将通过短信/邮件发送。`,
      '重置密码确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    const newPassword = await resetUserPassword(row.id)
    if (newPassword) {
      ElMessageBox.alert(`新密码：${newPassword}`, '密码已重置', {
        confirmButtonText: '复制并关闭',
        callback: () => {
          navigator.clipboard?.writeText(newPassword)
          ElMessage.success('密码已复制到剪贴板')
        },
      })
    } else {
      ElMessage.success('用户密码已重置，新密码已通过短信/邮件发送')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('重置密码失败:', error)
      ElMessage.error('重置密码失败')
    }
  }
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.user-list-page {
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
}

.search-input {
  width: 280px;
}

.status-select {
  width: 140px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>