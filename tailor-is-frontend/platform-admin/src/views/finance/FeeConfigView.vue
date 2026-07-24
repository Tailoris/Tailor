<template>
  <div v-loading="loading" class="page-container">
    <div class="page-header">
      <h2>费用配置</h2>
      <span class="page-desc">管理平台各项费用参数，修改后立即生效</span>
    </div>
    <el-card class="page-card">
      <el-table :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="65" />
        <el-table-column prop="name" label="配置名称" min-width="160" />
        <el-table-column prop="configKey" label="配置键" width="140" />
        <el-table-column label="费率" width="80" align="center">
          <template #default="{ row }">{{ row.rate > 0 ? row.rate + '%' : '--' }}</template>
        </el-table-column>
        <el-table-column label="固定金额" width="90" align="right">
          <template #default="{ row }">{{ row.fixedAmount > 0 ? '¥' + row.fixedAmount : '--' }}</template>
        </el-table-column>
        <el-table-column label="下限" width="80" align="right">
          <template #default="{ row }">{{ row.minAmount > 0 ? '¥' + row.minAmount : '--' }}</template>
        </el-table-column>
        <el-table-column label="上限" width="80" align="right">
          <template #default="{ row }">{{ row.maxAmount > 0 ? '¥' + row.maxAmount : '无上限' }}</template>
        </el-table-column>
        <el-table-column label="描述" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">{{ row.description }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'info'" size="small">{{ row.status === 'active' ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="155" prop="updateTime" />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="handleEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="编辑费用配置" width="520px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="配置名称">
          <el-input :model-value="form.name" disabled />
        </el-form-item>
        <el-form-item label="费率(%)" prop="rate">
          <el-input-number v-model="form.rate" :min="0" :max="100" :precision="2" :step="0.1" style="width: 100%" />
          <div class="form-hint">百分比费率，0表示不按比例收费</div>
        </el-form-item>
        <el-form-item label="固定金额" prop="fixedAmount">
          <el-input-number v-model="form.fixedAmount" :min="0" :precision="2" :step="1" style="width: 100%" />
          <div class="form-hint">每笔固定收取的金额，0表示不收取</div>
        </el-form-item>
        <el-form-item label="最低收费" prop="minAmount">
          <el-input-number v-model="form.minAmount" :min="0" :precision="2" :step="1" style="width: 100%" />
          <div class="form-hint">收费下限，0表示无下限</div>
        </el-form-item>
        <el-form-item label="最高收费" prop="maxAmount">
          <el-input-number v-model="form.maxAmount" :min="0" :precision="2" :step="1" style="width: 100%" />
          <div class="form-hint">收费上限，0表示无上限</div>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="128" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="form.status" active-value="active" inactive-value="inactive" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { listFeeConfigs, updateFeeConfig, type FeeConfig } from '@/api/finance';
import { recordAuditLog, auditActions, createFinanceAuditLog } from '@/utils/audit';

const loading = ref(false);
const saving = ref(false);
const tableData = ref<FeeConfig[]>([]);

const dialogVisible = ref(false);
const editingId = ref<number | null>(null);
const formRef = ref<FormInstance>();
const form = reactive<Partial<FeeConfig>>({
  name: '', rate: 0, fixedAmount: 0, minAmount: 0, maxAmount: 0, description: '', status: 'active',
});
const formRules: FormRules = {
  rate: [{ required: true, message: '请输入费率', trigger: 'blur' }],
};

async function loadData() {
  loading.value = true;
  try {
    tableData.value = await listFeeConfigs();
  } catch { ElMessage.error('加载费用配置失败'); } finally { loading.value = false; }
}

function resetForm() {
  editingId.value = null;
  Object.assign(form, { name: '', rate: 0, fixedAmount: 0, minAmount: 0, maxAmount: 0, description: '', status: 'active' });
  formRef.value?.clearValidate();
}

function handleEdit(row: FeeConfig) {
  editingId.value = row.id;
  form.name = row.name; form.rate = row.rate; form.fixedAmount = row.fixedAmount;
  form.minAmount = row.minAmount; form.maxAmount = row.maxAmount;
  form.description = row.description; form.status = row.status;
  dialogVisible.value = true;
}

async function handleSubmit() {
  if (!formRef.value || !editingId.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    saving.value = true;
    try {
      await updateFeeConfig(editingId.value!, { rate: form.rate, fixedAmount: form.fixedAmount, minAmount: form.minAmount, maxAmount: form.maxAmount, description: form.description, status: form.status });
      ElMessage.success('费用配置更新成功');
      recordAuditLog(createFinanceAuditLog(auditActions.FEE_CONFIG_UPDATE, 'fee_config', String(editingId.value!), form.name, `更新费用配置: ${form.name}, 费率: ${form.rate}%`));
      dialogVisible.value = false; loadData();
    } catch { ElMessage.error('更新失败'); } finally { saving.value = false; }
  });
}

onMounted(() => { loadData(); });
</script>

<style scoped>
.page-container { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 18px; font-weight: 600; }
.page-desc { font-size: 13px; color: #9ca3af; }
.page-card { margin-bottom: 0; }
.form-hint { font-size: 11px; color: #9ca3af; margin-top: 4px; line-height: 1.4; }
</style>