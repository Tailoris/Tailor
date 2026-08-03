<template>
  <div
    v-loading="loading"
    class="page-container"
  >
    <div class="page-header">
      <h2>入驻类型配置</h2>
      <el-button
        type="primary"
        @click="handleAdd"
      >
        <el-icon><Plus /></el-icon>新增类型
      </el-button>
    </div>
    <el-card class="page-card">
      <div class="search-bar">
        <el-input
          v-model="searchForm.keyword"
          placeholder="搜索类型名称/编码"
          style="width: 240px"
          clearable
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select
          v-model="searchForm.status"
          placeholder="状态"
          clearable
          style="width: 130px"
        >
          <el-option
            label="启用"
            :value="1"
          />
          <el-option
            label="禁用"
            :value="0"
          />
        </el-select>
        <el-button
          type="primary"
          @click="handleSearch"
        >
          搜索
        </el-button>
        <el-button @click="handleReset">
          重置
        </el-button>
      </div>
      <el-table
        :data="tableData"
        border
        stripe
      >
        <el-table-column
          prop="id"
          label="ID"
          width="70"
        />
        <el-table-column
          prop="typeCode"
          label="类型编码"
          width="140"
        />
        <el-table-column
          prop="typeName"
          label="类型名称"
          min-width="140"
        />
        <el-table-column
          prop="depositAmount"
          label="基础保障金"
          width="120"
        />
        <el-table-column
          label="额外佣金率"
          width="120"
        >
          <template #default="{ row }">
            <span :class="{ 'text-danger': row.commissionExtraRate < 0, 'text-success': row.commissionExtraRate > 0 }">
              {{ row.commissionExtraRate > 0 ? '+' : '' }}{{ (row.commissionExtraRate * 100).toFixed(2) }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column
          label="需审核"
          width="90"
          align="center"
        >
          <template #default="{ row }">
            <el-tag
              :type="row.auditRequired === 1 ? 'warning' : 'info'"
              size="small"
            >
              {{ row.auditRequired === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="trialDays"
          label="试用天数"
          width="100"
        />
        <el-table-column
          prop="sortOrder"
          label="排序"
          width="80"
        />
        <el-table-column
          label="状态"
          width="90"
          align="center"
        >
          <template #default="{ row }">
            <el-tag
              :type="row.status === 1 ? 'success' : 'danger'"
              size="small"
            >
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="180"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              type="primary"
              text
              size="small"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              type="danger"
              text
              size="small"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        class="pagination"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="110px"
      >
        <el-form-item
          label="类型编码"
          prop="typeCode"
        >
          <el-input
            v-model="form.typeCode"
            placeholder="唯一编码，如 STANDARD"
            maxlength="32"
          />
        </el-form-item>
        <el-form-item
          label="类型名称"
          prop="typeName"
        >
          <el-input
            v-model="form.typeName"
            placeholder="请输入类型名称"
            maxlength="32"
          />
        </el-form-item>
        <el-form-item
          label="资质要求"
          prop="requiredQualifications"
        >
          <el-checkbox-group v-model="form.requiredQualifications">
            <el-checkbox :value="1">
              营业执照
            </el-checkbox>
            <el-checkbox :value="2">
              税务登记证
            </el-checkbox>
            <el-checkbox :value="3">
              组织机构代码证
            </el-checkbox>
            <el-checkbox :value="4">
              食品经营许可证
            </el-checkbox>
            <el-checkbox :value="5">
              其他资质
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item
          label="基础保障金"
          prop="depositAmount"
        >
          <el-input-number
            v-model="form.depositAmount"
            :min="0"
            :precision="2"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item
          label="额外佣金率"
          prop="commissionExtraRate"
        >
          <el-input-number
            v-model="form.commissionExtraRate"
            :min="-0.5"
            :max="0.5"
            :step="0.005"
            :precision="4"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item
          label="需审核"
          prop="auditRequired"
        >
          <el-switch
            v-model="form.auditRequired"
            :active-value="1"
            :inactive-value="0"
          />
        </el-form-item>
        <el-form-item
          label="试用天数"
          prop="trialDays"
        >
          <el-input-number
            v-model="form.trialDays"
            :min="0"
            :max="365"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item
          label="排序"
          prop="sortOrder"
        >
          <el-input-number
            v-model="form.sortOrder"
            :min="0"
            :max="9999"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item
          label="描述"
          prop="description"
        >
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            maxlength="200"
          />
        </el-form-item>
        <el-form-item
          label="状态"
          prop="status"
        >
          <el-switch
            v-model="form.status"
            :active-value="1"
            :inactive-value="0"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="saving"
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
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import {
  listApplyTypePage, createApplyType, updateApplyType, deleteApplyType,
  type ApplyType,
} from '@/api/merchant';

const loading = ref(false);
const saving = ref(false);
const tableData = ref<ApplyType[]>([]);
const pagination = reactive({ current: 1, size: 10, total: 0 });
const searchForm = reactive({ keyword: '', status: '' as number | string });

const dialogVisible = ref(false);
const dialogTitle = ref('新增类型');
const editingId = ref<number | null>(null);
const formRef = ref<FormInstance>();
const form = reactive<Omit<ApplyType, 'id'>>({
  typeCode: '', typeName: '', requiredQualifications: [], depositAmount: 5000,
  commissionExtraRate: 0, auditRequired: 1, trialDays: 30, sortOrder: 1, description: '', status: 1,
});
const formRules: FormRules = {
  typeCode: [{ required: true, message: '请输入类型编码', trigger: 'blur' }],
  typeName: [{ required: true, message: '请输入类型名称', trigger: 'blur' }],
};

async function loadData() {
  loading.value = true;
  try {
    const res = await listApplyTypePage({ ...searchForm, current: pagination.current, size: pagination.size });
    tableData.value = res.records;
    pagination.total = res.total;
  } catch {
    ElMessage.error('加载入驻类型列表失败');
  } finally {
    loading.value = false;
  }
}

function handleSearch() { pagination.current = 1; loadData(); }
function handleReset() { searchForm.keyword = ''; searchForm.status = ''; pagination.current = 1; loadData(); }

function resetForm() {
  editingId.value = null;
  Object.assign(form, {
    typeCode: '', typeName: '', requiredQualifications: [], depositAmount: 5000,
    commissionExtraRate: 0, auditRequired: 1, trialDays: 30, sortOrder: 1, description: '', status: 1,
  });
  formRef.value?.clearValidate();
}

function handleAdd() { resetForm(); dialogTitle.value = '新增类型'; dialogVisible.value = true; }

function handleEdit(row: ApplyType) {
  editingId.value = row.id;
  dialogTitle.value = '编辑类型';
  Object.assign(form, {
    typeCode: row.typeCode, typeName: row.typeName,
    requiredQualifications: [...row.requiredQualifications], depositAmount: row.depositAmount,
    commissionExtraRate: row.commissionExtraRate, auditRequired: row.auditRequired,
    trialDays: row.trialDays, sortOrder: row.sortOrder, description: row.description, status: row.status,
  });
  dialogVisible.value = true;
}

async function handleSubmit() {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    saving.value = true;
    try {
      if (editingId.value) {
        await updateApplyType(editingId.value, { ...form });
        ElMessage.success('类型更新成功');
      } else {
        await createApplyType({ ...form });
        ElMessage.success('类型创建成功');
      }
      dialogVisible.value = false;
      loadData();
    } catch {
      ElMessage.error('操作失败');
    } finally {
      saving.value = false;
    }
  });
}

async function handleDelete(row: ApplyType) {
  try {
    await ElMessageBox.confirm(`确定要删除类型 "${row.typeName}" 吗？此操作不可恢复。`, '删除确认', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
    });
    await deleteApplyType(row.id);
    ElMessage.success('删除成功');
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
.pagination { margin-top: 20px; text-align: right; }
.text-danger { color: #f56c6c; }
.text-success { color: #67c23a; }
</style>
