<template>
  <div class="coupon-list">
    <PageHeader :title="activeTab === 'coupon' ? '优惠券管理' : '秒杀活动'">
      <template #actions>
        <el-button v-if="activeTab === 'coupon'" type="primary" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          创建优惠券
        </el-button>
      </template>
    </PageHeader>

    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="优惠券" name="coupon" />
        <el-tab-pane label="秒杀活动" name="seckill" />
      </el-tabs>

      <div v-if="activeTab === 'coupon'">
        <el-table :data="coupons" v-loading="loading" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="name" label="优惠券名称" min-width="180" />
          <el-table-column label="类型" width="100">
            <template #default="{ row }">
              {{ couponTypeLabel[row.type] }}
            </template>
          </el-table-column>
          <el-table-column label="优惠值" width="100">
            <template #default="{ row }">
              {{ row.type === 'percentage' ? `${row.discountValue}%` : `¥${row.discountValue}` }}
            </template>
          </el-table-column>
          <el-table-column label="最低消费" width="100">
            <template #default="{ row }">¥{{ row.minAmount?.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="有效期" width="200">
            <template #default="{ row }">
              {{ row.validFrom }} ~ {{ row.validTo }}
            </template>
          </el-table-column>
          <el-table-column label="领取/总量" width="120">
            <template #default="{ row }">
              {{ row.usedQuantity }}/{{ row.totalQuantity }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="couponStatusType[row.status]" size="small">
                {{ couponStatusLabel[row.status] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
              <el-button link type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无数据" />
          </template>
        </el-table>
      </div>

      <div v-else>
        <el-empty description="暂无秒杀活动" :image-size="80">
          <el-button type="primary" @click="ElMessage.info('功能开发中')">创建活动</el-button>
        </el-empty>
      </div>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="fetchCoupons"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑优惠券' : '创建优惠券'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="优惠券名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入优惠券名称" />
        </el-form-item>
        <el-form-item label="优惠类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择优惠类型" style="width: 100%">
            <el-option label="满减" value="fixed" />
            <el-option label="折扣" value="percentage" />
            <el-option label="立减" value="discount" />
          </el-select>
        </el-form-item>
        <el-form-item label="优惠值" prop="discountValue">
          <el-input-number
            v-model="form.discountValue"
            :min="0"
            :precision="2"
            :controls="false"
            style="width: 100%"
          />
          <div class="form-hint">{{ form.type === 'percentage' ? '请输入折扣百分比(如 80 表示 8折)' : '请输入优惠金额' }}</div>
        </el-form-item>
        <el-form-item label="最低消费" prop="minAmount">
          <el-input-number
            v-model="form.minAmount"
            :min="0"
            :precision="2"
            :controls="false"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="有效期" prop="validFrom">
          <el-date-picker
            v-model="validDateRange"
            type="daterange"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="发放数量" prop="totalQuantity">
          <el-input-number
            v-model="form.totalQuantity"
            :min="1"
            :controls="false"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { getCoupons, createCoupon, updateCoupon, deleteCoupon } from '@/api/marketing'
import type { Coupon } from '@/types'
import PageHeader from '@/components/PageHeader.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'

const loading = ref(false)
const submitting = ref(false)
const activeTab = ref('coupon')
const coupons = ref<Coupon[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  id: 0,
  name: '',
  type: 'fixed' as 'discount' | 'fixed' | 'percentage',
  discountValue: 0,
  minAmount: 0,
  totalQuantity: 100,
})

const validDateRange = ref<[string, string] | null>(null)

const rules = {
  name: [{ required: true, message: '请输入优惠券名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择优惠类型', trigger: 'change' }],
  discountValue: [{ required: true, message: '请输入优惠值', trigger: 'change' }],
  totalQuantity: [{ required: true, message: '请输入发放数量', trigger: 'change' }],
}

const couponTypeLabel: Record<string, string> = {
  discount: '立减',
  fixed: '满减',
  percentage: '折扣',
}

const couponStatusType: Record<number, string> = {
  1: 'success',
  0: 'info',
  2: 'danger',
}

const couponStatusLabel: Record<number, string> = {
  1: '进行中',
  0: '未开始',
  2: '已过期',
}

async function fetchCoupons() {
  loading.value = true
  try {
    const res = await getCoupons({
      current: page.value,
      size: pageSize.value,
    })
    coupons.value = res.records
    total.value = res.total
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

function handleSizeChange() {
  page.value = 1
  fetchCoupons()
}

function openCreateDialog() {
  isEdit.value = false
  form.id = 0
  form.name = ''
  form.type = 'fixed'
  form.discountValue = 0
  form.minAmount = 0
  form.totalQuantity = 100
  validDateRange.value = null
  dialogVisible.value = true
}

function openEditDialog(row: Coupon) {
  isEdit.value = true
  form.id = row.id
  form.name = row.name
  form.type = row.type
  form.discountValue = row.discountValue
  form.minAmount = row.minAmount
  form.totalQuantity = row.totalQuantity
  validDateRange.value = [row.validFrom, row.validTo]
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      if (!validDateRange.value) {
        ElMessage.warning('请选择有效期')
        return
      }
      submitting.value = true
      try {
        const data = {
          name: form.name,
          type: form.type,
          discountValue: form.discountValue,
          minAmount: form.minAmount,
          validFrom: validDateRange.value[0],
          validTo: validDateRange.value[1],
          totalQuantity: form.totalQuantity,
        }
        if (isEdit.value) {
          await updateCoupon(form.id, data)
          ElMessage.success('修改成功')
        } else {
          await createCoupon(data)
          ElMessage.success('创建成功')
        }
        dialogVisible.value = false
        fetchCoupons()
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
    await ElMessageBox.confirm('确定要删除该优惠券吗？', '删除确认', { type: 'warning' })
    await deleteCoupon(id)
    ElMessage.success('删除成功')
    fetchCoupons()
  } catch {
    // cancelled or error
  }
}

watch(activeTab, () => {
  if (activeTab.value === 'coupon') {
    fetchCoupons()
  }
})

onMounted(() => {
  fetchCoupons()
})
</script>

<style scoped>
.form-hint {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-top: 4px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
