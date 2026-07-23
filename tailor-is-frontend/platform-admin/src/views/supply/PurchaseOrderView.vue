<template>
  <div v-loading="loading" class="page-container">
    <div class="page-header">
      <h2>采购订单</h2>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>新建采购单
      </el-button>
    </div>
    <el-card class="page-card">
      <div class="search-bar">
        <el-input v-model="searchForm.keyword" placeholder="搜索采购单号/供应商" style="width: 220px" clearable @keyup.enter="handleSearch">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="searchForm.status" placeholder="状态" clearable style="width: 120px">
          <el-option label="待确认" value="pending" />
          <el-option label="已确认" value="confirmed" />
          <el-option label="已发货" value="shipped" />
          <el-option label="已完成" value="completed" />
          <el-option label="已取消" value="cancelled" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
      <el-table :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="orderNo" label="采购单号" min-width="160" />
        <el-table-column prop="supplier" label="供应商" min-width="120" />
        <el-table-column label="采购金额" width="120" align="right">
          <template #default="{ row }">¥{{ row.totalAmount.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="采购明细" min-width="200">
          <template #default="{ row }">
            <el-tag v-for="(item, idx) in row.items" :key="idx" size="small" style="margin-right: 4px; margin-bottom: 2px">
              {{ item.name }} ×{{ item.quantity }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="handleView(row)">详情</el-button>
            <el-button v-if="row.status === 'pending'" type="success" text size="small" @click="handleConfirm(row)">确认</el-button>
            <el-button v-if="row.status === 'pending'" type="danger" text size="small" @click="handleCancel(row)">取消</el-button>
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
        <el-form-item label="供应商" prop="supplier">
          <el-select v-model="form.supplier" placeholder="请选择供应商" style="width: 100%" filterable>
            <el-option label="华纺集团" value="华纺集团" />
            <el-option label="恒力集团" value="恒力集团" />
            <el-option label="新丝路纺织" value="新丝路纺织" />
          </el-select>
        </el-form-item>
        <el-form-item label="采购金额" prop="totalAmount">
          <el-input-number v-model="form.totalAmount" :min="0" :precision="2" :step="1000" style="width: 100%" />
        </el-form-item>
        <el-form-item label="采购明细" prop="items">
          <div v-for="(item, idx) in (form.items || [])" :key="idx" style="display: flex; gap: 8px; margin-bottom: 8px; align-items: center">
            <el-input v-model="item.name" placeholder="物料名称" style="flex: 2" size="small" />
            <el-input-number v-model="item.quantity" :min="1" placeholder="数量" style="flex: 1" size="small" controls-position="right" />
            <el-input-number v-model="item.price" :min="0" :precision="2" placeholder="单价" style="flex: 1" size="small" controls-position="right" />
            <el-button type="danger" :icon="Delete" circle size="small" @click="removeItem(idx)" />
          </div>
          <el-button type="primary" text size="small" @click="addItem">+ 添加采购项</el-button>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注信息（可选）" maxlength="128" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="采购单详情" width="500px">
      <el-descriptions :column="1" border v-if="detailRow">
        <el-descriptions-item label="采购单号">{{ detailRow.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ detailRow.supplier }}</el-descriptions-item>
        <el-descriptions-item label="采购金额">¥{{ detailRow.totalAmount.toLocaleString() }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(detailRow.status)" size="small">{{ getStatusText(detailRow.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detailRow.createTime }}</el-descriptions-item>
        <el-descriptions-item label="确认时间">{{ detailRow.confirmTime || '--' }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detailRow.remark || '--' }}</el-descriptions-item>
        <el-descriptions-item label="采购明细">
          <div v-for="(item, idx) in detailRow.items" :key="idx">
            {{ item.name }} × {{ item.quantity }} (¥{{ item.price }}/件)
          </div>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { Plus, Search, Delete } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import {
  listPurchaseOrders, createPurchaseOrder, confirmPurchaseOrder, cancelPurchaseOrder,
  type PurchaseOrderInfo, type SupplyQueryParams,
} from '@/api/supply';

const loading = ref(false);
const saving = ref(false);
const tableData = ref<PurchaseOrderInfo[]>([]);
const pagination = reactive({ current: 1, size: 10, total: 0 });
const searchForm = reactive<SupplyQueryParams>({ keyword: '', status: '' });

const dialogVisible = ref(false);
const dialogTitle = ref('新建采购单');
const editingId = ref<number | null>(null);
const formRef = ref<FormInstance>();
const form = reactive<Partial<PurchaseOrderInfo>>({
  supplier: '', totalAmount: 0, items: [{ name: '', quantity: 1, price: 0 }], remark: '',
});
const formRules: FormRules = {
  supplier: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  totalAmount: [{ required: true, message: '请输入采购金额', trigger: 'blur' }],
};

const detailVisible = ref(false);
const detailRow = ref<PurchaseOrderInfo | null>(null);

function getStatusType(status: string) {
  const map: Record<string, string> = { pending: 'warning', confirmed: 'primary', shipped: 'info', completed: 'success', cancelled: 'danger' };
  return map[status] || 'info';
}
function getStatusText(status: string) {
  const map: Record<string, string> = { pending: '待确认', confirmed: '已确认', shipped: '已发货', completed: '已完成', cancelled: '已取消' };
  return map[status] || status;
}

async function loadData() {
  loading.value = true;
  try {
    const res = await listPurchaseOrders({ ...searchForm, current: pagination.current, size: pagination.size });
    tableData.value = res.records;
    pagination.total = res.total;
  } catch { ElMessage.error('加载采购订单失败'); } finally { loading.value = false; }
}

function handleSearch() { pagination.current = 1; loadData(); }
function handleReset() { searchForm.keyword = ''; searchForm.status = ''; pagination.current = 1; loadData(); }

function resetForm() {
  editingId.value = null;
  Object.assign(form, { supplier: '', totalAmount: 0, items: [{ name: '', quantity: 1, price: 0 }], remark: '' });
  formRef.value?.clearValidate();
}

function addItem() { form.items = [...(form.items || []), { name: '', quantity: 1, price: 0 }]; }
function removeItem(idx: number) { form.items = (form.items || []).filter((_, i) => i !== idx); }

function handleAdd() { resetForm(); dialogTitle.value = '新建采购单'; dialogVisible.value = true; }

function handleView(row: PurchaseOrderInfo) { detailRow.value = row; detailVisible.value = true; }

async function handleSubmit() {
  if (!formRef.value) return;
  const validItems = (form.items || []).filter(i => i.name && i.quantity > 0);
  if (validItems.length === 0) { ElMessage.warning('请至少添加一项采购明细'); return; }
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    saving.value = true;
    try {
      await createPurchaseOrder({ ...form, items: validItems });
      ElMessage.success('采购单创建成功');
      dialogVisible.value = false; loadData();
    } catch { ElMessage.error('操作失败'); } finally { saving.value = false; }
  });
}

async function handleConfirm(row: PurchaseOrderInfo) {
  try {
    await ElMessageBox.confirm(`确定要确认采购单 "${row.orderNo}" 吗？`, '确认采购', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
    });
    await confirmPurchaseOrder(row.id);
    row.status = 'confirmed';
    ElMessage.success(`已确认采购单: ${row.orderNo}`);
    loadData();
  } catch { /* 取消 */ }
}

async function handleCancel(row: PurchaseOrderInfo) {
  try {
    await ElMessageBox.confirm(`确定要取消采购单 "${row.orderNo}" 吗？`, '取消采购', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
    });
    await cancelPurchaseOrder(row.id);
    row.status = 'cancelled';
    ElMessage.success(`已取消采购单: ${row.orderNo}`);
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