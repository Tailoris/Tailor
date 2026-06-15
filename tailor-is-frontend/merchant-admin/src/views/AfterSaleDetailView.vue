<template>
  <div class="aftersale-detail" v-loading="loading">
    <PageHeader title="工单详情">
      <template #actions>
        <el-button @click="$router.back()">返回</el-button>
      </template>
    </PageHeader>

    <el-row :gutter="20">
      <el-col :span="16">
        <el-card shadow="never" style="margin-bottom: 16px">
          <template #header>
            <div class="card-header">
              <span>工单信息</span>
              <el-tag :type="statusType[ticket.status]" size="large">
                {{ statusLabel[ticket.status] }}
              </el-tag>
            </div>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="工单号">{{ ticket.ticketNo }}</el-descriptions-item>
            <el-descriptions-item label="关联订单">
              <el-button link type="primary" @click="$router.push(`/order/${ticket.orderNo}`)">
                {{ ticket.orderNo }}
              </el-button>
            </el-descriptions-item>
            <el-descriptions-item label="买家">{{ ticket.buyerName }}</el-descriptions-item>
            <el-descriptions-item label="类型">{{ typeLabel[ticket.type] }}</el-descriptions-item>
            <el-descriptions-item label="退款金额">
              <span class="amount">¥{{ ticket.refundAmount?.toFixed(2) }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="申请时间">{{ ticket.createdAt }}</el-descriptions-item>
          </el-descriptions>
          <el-divider>售后原因</el-divider>
          <p class="reason-text">{{ ticket.reason }}</p>
          <el-divider>详细说明</el-divider>
          <p class="desc-text">{{ ticket.description || '无' }}</p>
        </el-card>

        <el-card shadow="never">
          <template #header>
            <span>凭证图片</span>
          </template>
          <div class="evidence-images">
            <el-image
              v-for="(img, index) in ticket.evidenceImages"
              :key="index"
              :src="img"
              fit="cover"
              style="width: 120px; height: 120px; border-radius: 8px; cursor: pointer"
              :preview-src-list="ticket.evidenceImages"
              :initial-index="index"
            />
            <el-empty v-if="!ticket.evidenceImages?.length" description="无凭证图片" :image-size="60" />
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="never" v-if="ticket.status === 'pending'">
          <template #header>
            <span>处理工单</span>
          </template>
          <el-form :model="processForm" label-width="80px">
            <el-form-item label="退款金额">
              <el-input-number
                v-model="processForm.refundAmount"
                :min="0"
                :precision="2"
                :controls="false"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="处理备注">
              <el-input
                v-model="processForm.remark"
                type="textarea"
                :rows="4"
                placeholder="请输入处理备注"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="success" @click="handleApprove" :loading="processing" style="width: 100%">
                同意退款
              </el-button>
            </el-form-item>
            <el-form-item>
              <el-button type="danger" @click="handleReject" :loading="processing" style="width: 100%">
                拒绝申请
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" v-else>
          <template #header>
            <span>处理结果</span>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="处理状态">
              <el-tag :type="statusType[ticket.status]">{{ statusLabel[ticket.status] }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="处理备注">{{ ticket.handlerRemark || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getTicketDetail, approveRefund, rejectTicket } from '@/api/aftersale'
import type { AfterSaleTicket } from '@/types'
import PageHeader from '@/components/PageHeader.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const loading = ref(false)
const processing = ref(false)

const ticket = reactive<AfterSaleTicket>({
  id: 0,
  ticketNo: '',
  orderNo: '',
  buyerName: '',
  type: 'refund_only',
  reason: '',
  description: '',
  evidenceImages: [],
  refundAmount: 0,
  status: 'pending',
  handlerRemark: '',
  createdAt: '',
  updatedAt: '',
})

const processForm = reactive({
  refundAmount: 0,
  remark: '',
})

const typeLabel: Record<string, string> = {
  refund_only: '仅退款',
  return_and_refund: '退货退款',
  exchange: '换货',
}

const statusType: Record<string, string> = {
  pending: 'warning',
  processing: '',
  approved: 'success',
  rejected: 'danger',
  completed: 'info',
}

const statusLabel: Record<string, string> = {
  pending: '待处理',
  processing: '处理中',
  approved: '已同意',
  rejected: '已拒绝',
  completed: '已完成',
}

async function fetchTicketDetail() {
  loading.value = true
  try {
    const res = await getTicketDetail(route.params.ticketNo as string)
    Object.assign(ticket, res)
    processForm.refundAmount = res.refundAmount
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

async function handleApprove() {
  try {
    await ElMessageBox.confirm('确定同意退款吗？', '确认操作', { type: 'warning' })
    processing.value = true
    await approveRefund(ticket.ticketNo, processForm.refundAmount, processForm.remark)
    ElMessage.success('已同意退款')
    fetchTicketDetail()
  } catch {
    // cancelled or error
  } finally {
    processing.value = false
  }
}

async function handleReject() {
  if (!processForm.remark) {
    ElMessage.warning('请填写拒绝原因')
    return
  }
  try {
    await ElMessageBox.confirm('确定拒绝该申请吗？', '确认操作', { type: 'warning' })
    processing.value = true
    await rejectTicket(ticket.ticketNo, processForm.remark)
    ElMessage.success('已拒绝申请')
    fetchTicketDetail()
  } catch {
    // cancelled or error
  } finally {
    processing.value = false
  }
}

onMounted(() => {
  fetchTicketDetail()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.reason-text,
.desc-text {
  color: var(--color-text-primary);
  line-height: 1.8;
  margin: 0;
}

.amount {
  color: var(--color-danger);
  font-weight: 600;
  font-size: 16px;
}

.evidence-images {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
</style>
