<template>
  <div v-loading="loading" class="page-container">
    <div class="page-header">
      <h2>优惠券管理</h2>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>新增优惠券
      </el-button>
    </div>
    <el-card class="page-card">
      <div class="search-bar">
        <el-input v-model="searchForm.keyword" placeholder="搜索优惠券名称" style="width: 200px" clearable @keyup.enter="handleSearch">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="searchForm.type" placeholder="优惠类型" clearable style="width: 120px">
          <el-option label="固定金额" value="fixed" />
          <el-option label="百分比折扣" value="percent" />
        </el-select>
        <el-select v-model="searchForm.status" placeholder="状态" clearable style="width: 120px">
          <el-option label="未开始" value="upcoming" />
          <el-option label="进行中" value="active" />
          <el-option label="已结束" value="ended" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
      <el-table :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="优惠券名称" min-width="140" />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.type === 'fixed' ? 'primary' : 'warning'" size="small">
              {{ row.type === 'fixed' ? '固定金额' : '百分比折扣' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优惠" width="100" align="right">
          <template #default="{ row }">
            {{ row.type === 'fixed' ? `¥${row.discount}` : `${row.discount}%` }}
          </template>
        </el-table-column>
        <el-table-column label="最低消费" width="100" align="right">
          <template #default="{ row }">¥{{ row.minAmount }}</template>
        </el-table-column>
        <el-table-column label="发放/已用" width="110" align="center">
          <template #default="{ row }">{{ row.usedCount }} / {{ row.totalCount }}</template>
        </el-table-column>
        <el-table-column label="有效期" min-width="200">
          <template #default="{ row }">{{ row.startTime }} ~ {{ row.endTime }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="handleEdit(row)">编辑</el-button>
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
        <el-form-item label="优惠券名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入优惠券名称" maxlength="64" />
        </el-form-item>
        <el-form-item label="优惠类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio value="fixed">固定金额</el-radio>
            <el-radio value="percent">百分比折扣</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="form.type === 'fixed' ? '优惠金额(元)' : '折扣百分比(%)'" prop="discount">
          <el-input-number v-model="form.discount" :min="1" :max="form.type === 'fixed' ? 9999 : 99" style="width: 100%" />
        </el-form-item>
        <el-form-item label="最低消费" prop="minAmount">
          <el-input-number v-model="form.minAmount" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="发放数量" prop="totalCount">
          <el-input-number v-model="form.totalCount" :min="1" :max="99999" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker v-model="form.startTime" type="datetime" placeholder="选择开始时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker v-model="form.endTime" type="datetime" placeholder="选择结束时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="优惠券描述（可选）" maxlength="128" />
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
import { listCoupons, createCoupon, updateCoupon, deleteCoupon, type CouponInfo, type MarketingQueryParams } from '@/api/marketing';

const loading = ref(false);
const saving = ref(false);
const tableData = ref<CouponInfo[]>([]);
const pagination = reactive({ current: 1, size: 10, total: 0 });
const searchForm = reactive<MarketingQueryParams>({ keyword: '', type: '', status: '' });

const dialogVisible = ref(false);
const dialogTitle = ref('新增优惠券');
const editingId = ref<number | null>(null);
const formRef = ref<FormInstance>();
const form = reactive<Partial<CouponInfo>>({
  name: '', type: 'fixed', discount: 10, minAmount: 0, totalCount: 1000,
  startTime: '', endTime: '', description: '',
});
const formRules: FormRules = {
  name: [{ required: true, message: '请输入优惠券名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择优惠类型', trigger: 'change' }],
  discount: [{ required: true, message: '请输入优惠值', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
};

function getStatusType(status: string) {
  const map: Record<string, string> = { upcoming: 'info', active: 'success', ended: 'danger' };
  return map[status] || 'info';
}
function getStatusText(status: string) {
  const map: Record<string, string> = { upcoming: '未开始', active: '进行中', ended: '已结束' };
  return map[status] || status;
}

async function loadData() {
  loading.value = true;
  try {
    const res = await listCoupons({ ...searchForm, current: pagination.current, size: pagination.size });
    tableData.value = res.records;
    pagination.total = res.total;
  } catch { ElMessage.error('加载优惠券列表失败'); } finally { loading.value = false; }
}

function handleSearch() { pagination.current = 1; loadData(); }
function handleReset() { searchForm.keyword = ''; searchForm.type = ''; searchForm.status = ''; pagination.current = 1; loadData(); }

function resetForm() {
  editingId.value = null;
  Object.assign(form, { name: '', type: 'fixed', discount: 10, minAmount: 0, totalCount: 1000, startTime: '', endTime: '', description: '' });
  formRef.value?.clearValidate();
}

function handleAdd() { resetForm(); dialogTitle.value = '新增优惠券'; dialogVisible.value = true; }

function handleEdit(row: CouponInfo) {
  editingId.value = row.id; dialogTitle.value = '编辑优惠券';
  form.name = row.name; form.type = row.type; form.discount = row.discount;
  form.minAmount = row.minAmount; form.totalCount = row.totalCount;
  form.startTime = row.startTime; form.endTime = row.endTime; form.description = row.description;
  dialogVisible.value = true;
}

async function handleSubmit() {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    saving.value = true;
    try {
      editingId.value ? await updateCoupon(editingId.value, { ...form }) : await createCoupon({ ...form });
      ElMessage.success(editingId.value ? '优惠券更新成功' : '优惠券创建成功');
      dialogVisible.value = false; loadData();
    } catch { ElMessage.error('操作失败'); } finally { saving.value = false; }
  });
}

async function handleDelete(row: CouponInfo) {
  try {
    await ElMessageBox.confirm(`确定要删除优惠券 "${row.name}" 吗？`, '删除确认', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
    });
    await deleteCoupon(row.id);
    ElMessage.success(`已删除优惠券: ${row.name}`);
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