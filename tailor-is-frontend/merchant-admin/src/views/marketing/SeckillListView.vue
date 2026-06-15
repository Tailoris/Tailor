<template>
  <div class="seckill-list">
    <PageHeader title="秒杀活动管理">
      <template #actions>
        <el-button type="primary" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          创建秒杀活动
        </el-button>
      </template>
    </PageHeader>

    <el-card shadow="never">
      <el-table :data="activities" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="活动名称" min-width="180" />
        <el-table-column label="开始时间" width="180">
          <template #default="{ row }">
            {{ row.startTime }}
          </template>
        </el-table-column>
        <el-table-column label="结束时间" width="180">
          <template #default="{ row }">
            {{ row.endTime }}
          </template>
        </el-table-column>
        <el-table-column label="活动商品数" width="100" align="center">
          <template #default="{ row }">
            {{ row.products?.length ?? 0 }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="seckillStatusType[row.status]" size="small">
              {{ seckillStatusLabel[row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="success" size="small" @click="openJoinDialog(row)">参与</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
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
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="fetchActivities"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑秒杀活动' : '创建秒杀活动'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="活动名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入活动名称" />
        </el-form-item>
        <el-form-item label="活动时间" prop="timeRange">
          <el-date-picker
            v-model="form.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="joinDialogVisible" title="参与秒杀活动" width="500px">
      <el-form ref="joinFormRef" :model="joinForm" :rules="joinRules" label-width="100px">
        <el-form-item label="活动名称">
          <el-input :model-value="currentActivity?.name" disabled />
        </el-form-item>
        <el-form-item label="商品ID" prop="productId">
          <el-input-number
            v-model="joinForm.productId"
            :min="1"
            :controls="false"
            style="width: 100%"
            placeholder="请输入商品ID"
          />
        </el-form-item>
        <el-form-item label="秒杀价格" prop="seckillPrice">
          <el-input-number
            v-model="joinForm.seckillPrice"
            :min="0"
            :precision="2"
            :controls="false"
            style="width: 100%"
            placeholder="请输入秒杀价格"
          />
        </el-form-item>
        <el-form-item label="秒杀库存" prop="stock">
          <el-input-number
            v-model="joinForm.stock"
            :min="1"
            :controls="false"
            style="width: 100%"
            placeholder="请输入秒杀库存"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="joinDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleJoinSubmit" :loading="joinSubmitting">确认参与</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getSeckillActivities, createSeckill, updateSeckill, deleteSeckill, joinSeckill } from '@/api/marketing'
import type { SeckillActivity } from '@/api/marketing'
import PageHeader from '@/components/PageHeader.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'

const loading = ref(false)
const submitting = ref(false)
const joinSubmitting = ref(false)
const activities = ref<SeckillActivity[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  id: 0,
  name: '',
  timeRange: null as [string, string] | null,
})

const rules = {
  name: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
  timeRange: [{ required: true, message: '请选择活动时间', trigger: 'change' }],
}

const joinDialogVisible = ref(false)
const joinFormRef = ref<FormInstance>()
const currentActivity = ref<SeckillActivity | null>(null)

const joinForm = reactive({
  productId: 0,
  seckillPrice: 0,
  stock: 100,
})

const joinRules = {
  productId: [{ required: true, message: '请输入商品ID', trigger: 'change' }],
  seckillPrice: [{ required: true, message: '请输入秒杀价格', trigger: 'change' }],
  stock: [{ required: true, message: '请输入秒杀库存', trigger: 'change' }],
}

const seckillStatusType: Record<string, string> = {
  upcoming: 'info',
  ongoing: 'success',
  ended: 'danger',
}

const seckillStatusLabel: Record<string, string> = {
  upcoming: '即将开始',
  ongoing: '进行中',
  ended: '已结束',
}

async function fetchActivities() {
  loading.value = true
  try {
    const res = await getSeckillActivities({
      current: page.value,
      size: pageSize.value,
    })
    activities.value = res.records
    total.value = res.total
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

function handleSizeChange() {
  page.value = 1
  fetchActivities()
}

function openCreateDialog() {
  isEdit.value = false
  form.id = 0
  form.name = ''
  form.timeRange = null
  dialogVisible.value = true
}

function openEditDialog(row: SeckillActivity) {
  isEdit.value = true
  form.id = row.id
  form.name = row.name
  form.timeRange = [row.startTime, row.endTime]
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      if (!form.timeRange) {
        ElMessage.warning('请选择活动时间')
        return
      }
      submitting.value = true
      try {
        const data = {
          name: form.name,
          startTime: form.timeRange[0],
          endTime: form.timeRange[1],
        }
        if (isEdit.value) {
          await updateSeckill(form.id, data)
          ElMessage.success('修改成功')
        } else {
          await createSeckill(data)
          ElMessage.success('创建成功')
        }
        dialogVisible.value = false
        fetchActivities()
      } catch {
        // error handled by interceptor
      } finally {
        submitting.value = false
      }
    }
  })
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定要删除该秒杀活动吗？', '删除确认', { type: 'warning' })
    await deleteSeckill(id)
    ElMessage.success('删除成功')
    fetchActivities()
  } catch {
    // cancelled or error
  }
}

function openJoinDialog(row: SeckillActivity) {
  currentActivity.value = row
  joinForm.productId = 0
  joinForm.seckillPrice = 0
  joinForm.stock = 100
  joinDialogVisible.value = true
}

async function handleJoinSubmit() {
  if (!joinFormRef.value || !currentActivity.value) return
  await joinFormRef.value.validate(async (valid) => {
    if (valid) {
      joinSubmitting.value = true
      try {
        await joinSeckill(
          currentActivity.value!.id,
          joinForm.productId,
          joinForm.seckillPrice,
          joinForm.stock
        )
        ElMessage.success('参与成功')
        joinDialogVisible.value = false
        fetchActivities()
      } catch {
        // error handled by interceptor
      } finally {
        joinSubmitting.value = false
      }
    }
  })
}

onMounted(() => {
  fetchActivities()
})
</script>

<style scoped>
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>