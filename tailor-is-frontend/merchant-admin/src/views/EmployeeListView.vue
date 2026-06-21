<template>
  <div class="employee-list">
    <PageHeader title="员工管理">
      <template #actions>
        <el-button type="primary" @click="addDialogVisible = true">
          <el-icon><Plus /></el-icon>
          添加员工
        </el-button>
      </template>
    </PageHeader>

    <el-card shadow="never">
      <el-table :data="employees" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column label="角色" width="120">
          <template #default="{ row }">
            <el-tag :type="roleType[row.role]" size="small">
              {{ roleLabel[row.role] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="shopName" label="所属店铺" width="140" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '在职' : '离职' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="入职时间" width="160" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openRoleDialog(row)">修改角色</el-button>
            <el-button link type="danger" size="small" @click="handleRemove(row.id)">移除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无数据" />
        </template>
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchEmployees"
        />
      </div>
    </el-card>

    <el-dialog v-model="addDialogVisible" title="添加员工" width="500px">
      <el-form :model="addForm" label-width="80px">
        <el-form-item label="用户ID">
          <el-input v-model="addForm.userId" placeholder="请输入用户ID" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="addForm.role" placeholder="请选择角色" style="width: 100%">
            <el-option label="管理员" value="admin" />
            <el-option label="操作员" value="operator" />
            <el-option label="查看者" value="viewer" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAdd" :loading="submitting">确认添加</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="roleDialogVisible" title="修改角色" width="400px">
      <el-form :model="roleForm" label-width="80px">
        <el-form-item label="角色">
          <el-select v-model="roleForm.role" placeholder="请选择角色" style="width: 100%">
            <el-option label="管理员" value="admin" />
            <el-option label="操作员" value="operator" />
            <el-option label="查看者" value="viewer" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUpdateRole" :loading="submitting">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { listEmployees, addEmployee, removeEmployee, updateEmployeeRole } from '@/api/shop'
import type { Employee } from '@/types'
import PageHeader from '@/components/PageHeader.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const userStore = useUserStore()

const loading = ref(false)
const submitting = ref(false)
const employees = ref<Employee[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const roleType: Record<string, string> = {
  admin: 'danger',
  operator: '',
  viewer: 'info',
}

const roleLabel: Record<string, string> = {
  admin: '管理员',
  operator: '操作员',
  viewer: '查看者',
}

async function fetchEmployees() {
  loading.value = true
  try {
    const shopId = userStore.currentShopId || 1
    const res = await listEmployees(shopId, page.value, pageSize.value)
    employees.value = res.records
    total.value = res.total
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

const addDialogVisible = ref(false)
const addForm = reactive({
  userId: '',
  role: 'operator',
})

async function handleAdd() {
  if (!addForm.userId) {
    ElMessage.warning('请输入用户ID')
    return
  }
  submitting.value = true
  try {
    const shopId = userStore.currentShopId || 1
    await addEmployee(shopId, {
      userId: Number(addForm.userId),
      role: addForm.role as 'admin' | 'operator' | 'viewer',
    })
    ElMessage.success('添加成功')
    addDialogVisible.value = false
    fetchEmployees()
  } catch {
    // error handled by interceptor
  } finally {
    submitting.value = false
  }
}

const roleDialogVisible = ref(false)
const roleForm = reactive({
  employeeId: 0,
  role: '',
})

function openRoleDialog(row: Employee) {
  roleForm.employeeId = row.id
  roleForm.role = row.role
  roleDialogVisible.value = true
}

async function handleUpdateRole() {
  submitting.value = true
  try {
    const shopId = userStore.currentShopId || 1
    await updateEmployeeRole(shopId, roleForm.employeeId, roleForm.role)
    ElMessage.success('角色修改成功')
    roleDialogVisible.value = false
    fetchEmployees()
  } catch {
    // error handled by interceptor
  } finally {
    submitting.value = false
  }
}

async function handleRemove(id: number) {
  try {
    await ElMessageBox.confirm('确定要移除该员工吗？', '移除确认', { type: 'warning' })
    const shopId = userStore.currentShopId || 1
    await removeEmployee(shopId, id)
    ElMessage.success('移除成功')
    fetchEmployees()
  } catch {
    // cancelled or error
  }
}

onMounted(() => {
  fetchEmployees()
})
</script>

<style scoped>
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
