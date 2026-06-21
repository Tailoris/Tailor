<template>
  <div class="finance-withdraw" v-loading="loading">
    <PageHeader title="提现管理" />

    <el-card shadow="never" style="margin-bottom: 20px">
      <div class="balance-cards">
        <div class="balance-card primary">
          <div class="balance-label">可提现金额</div>
          <div class="balance-value">¥{{ balance.withdrawableAmount?.toFixed(2) || '0.00' }}</div>
          <el-button type="primary" size="small" @click="withdrawDialogVisible = true" class="withdraw-btn">
            申请提现
          </el-button>
        </div>
        <div class="balance-card">
          <div class="balance-label">累计收益</div>
          <div class="balance-value">¥{{ balance.totalEarnings?.toFixed(2) || '0.00' }}</div>
        </div>
        <div class="balance-card">
          <div class="balance-label">累计提现</div>
          <div class="balance-value">¥{{ balance.totalWithdrawn?.toFixed(2) || '0.00' }}</div>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>提现记录</span>
        </div>
      </template>

      <el-table :data="withdrawRecords" stripe>
        <el-table-column prop="id" label="提现单号" width="180" show-overflow-tooltip />
        <el-table-column label="提现金额" width="120">
          <template #default="{ row }">¥{{ row.amount?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="开户银行" width="120">
          <template #default="{ row }">{{ bankNameLabel[row.bankName] || row.bankName }}</template>
        </el-table-column>
        <el-table-column label="银行账号" width="160">
          <template #default="{ row }">{{ maskBankAccount(row.bankAccount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="withdrawStatusType[row.status]" size="small">
              {{ withdrawStatusLabel[row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="180">
          <template #default="{ row }">{{ row.createdAt || '-' }}</template>
        </el-table-column>
        <el-table-column label="处理时间" width="180">
          <template #default="{ row }">{{ row.processedAt || '-' }}</template>
        </el-table-column>
        <el-table-column label="备注" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无提现记录" />
        </template>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchWithdrawRecords"
        />
      </div>
    </el-card>

    <el-dialog v-model="withdrawDialogVisible" title="提现申请" width="500px">
      <el-form :model="withdrawForm" label-width="100px">
        <el-form-item label="提现金额">
          <el-input-number
            v-model="withdrawForm.amount"
            :min="1"
            :max="balance.withdrawableAmount"
            :precision="2"
            :controls="false"
            style="width: 100%"
          />
          <div class="form-hint">可提现金额：¥{{ balance.withdrawableAmount?.toFixed(2) }}</div>
        </el-form-item>
        <el-form-item label="开户银行">
          <el-select v-model="withdrawForm.bankName" placeholder="请选择银行" style="width: 100%">
            <el-option label="工商银行" value="ICBC" />
            <el-option label="建设银行" value="CCB" />
            <el-option label="农业银行" value="ABC" />
            <el-option label="中国银行" value="BOC" />
            <el-option label="招商银行" value="CMB" />
            <el-option label="交通银行" value="BOCOM" />
          </el-select>
        </el-form-item>
        <el-form-item label="银行账号">
          <el-input v-model="withdrawForm.bankAccount" placeholder="请输入银行账号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="withdrawDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleWithdraw" :loading="withdrawing">确认提现</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getBalance, withdraw, getWithdrawRecords } from '@/api/finance'
import PageHeader from '@/components/PageHeader.vue'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const withdrawing = ref(false)
const withdrawRecords = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const balance = reactive({
  withdrawableAmount: 0,
  totalEarnings: 0,
  totalWithdrawn: 0,
})

const withdrawStatusType: Record<number, string> = {
  0: 'warning',
  1: 'primary',
  2: 'success',
  3: 'danger',
}

const withdrawStatusLabel: Record<number, string> = {
  0: '待处理',
  1: '处理中',
  2: '已到账',
  3: '提现失败',
}

const bankNameLabel: Record<string, string> = {
  ICBC: '工商银行',
  CCB: '建设银行',
  ABC: '农业银行',
  BOC: '中国银行',
  CMB: '招商银行',
  BOCOM: '交通银行',
}

function maskBankAccount(account: string) {
  if (!account) return '-'
  if (account.length <= 8) return account
  return account.slice(0, 4) + '****' + account.slice(-4)
}

const withdrawDialogVisible = ref(false)
const withdrawForm = reactive({
  amount: 0,
  bankName: '',
  bankAccount: '',
})

async function fetchBalance() {
  try {
    const res = await getBalance()
    Object.assign(balance, res)
  } catch {
    // error handled by interceptor
  }
}

async function fetchWithdrawRecords() {
  loading.value = true
  try {
    const res = await getWithdrawRecords({ current: page.value, size: pageSize.value })
    withdrawRecords.value = res.records
    total.value = res.total
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

async function handleWithdraw() {
  if (!withdrawForm.bankName || !withdrawForm.bankAccount) {
    ElMessage.warning('请填写完整的银行信息')
    return
  }
  if (withdrawForm.amount <= 0) {
    ElMessage.warning('请输入正确的提现金额')
    return
  }
  if (withdrawForm.amount > balance.withdrawableAmount) {
    ElMessage.warning('提现金额不能超过可提现金额')
    return
  }
  withdrawing.value = true
  try {
    await withdraw({
      amount: withdrawForm.amount,
      bankName: withdrawForm.bankName,
      bankAccount: withdrawForm.bankAccount,
    })
    ElMessage.success('提现申请已提交')
    withdrawDialogVisible.value = false
    fetchBalance()
    fetchWithdrawRecords()
  } catch {
    // error handled by interceptor
  } finally {
    withdrawing.value = false
  }
}

onMounted(() => {
  fetchBalance()
  fetchWithdrawRecords()
})
</script>

<style scoped>
.balance-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.balance-card {
  background: var(--color-background);
  border-radius: var(--border-radius);
  padding: 24px;
  text-align: center;
}

.balance-card.primary {
  background: linear-gradient(135deg, #6366F1, #818CF8);
  color: white;
}

.balance-label {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-bottom: 8px;
}

.balance-card.primary .balance-label {
  color: rgba(255, 255, 255, 0.8);
}

.balance-value {
  font-size: 28px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 12px;
}

.balance-card.primary .balance-value {
  color: white;
}

.withdraw-btn {
  width: 100%;
}

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

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
