<template>
  <div
    v-loading="loading"
    class="page-container"
  >
    <div class="page-header">
      <h2>保障金配置</h2>
    </div>
    <el-card class="page-card">
      <el-tabs
        v-model="activeTab"
        @tab-change="handleTabChange"
      >
        <el-tab-pane
          label="入驻类型保障金"
          name="apply-type"
        >
          <div class="search-bar">
            <el-input
              v-model="applySearch.keyword"
              placeholder="搜索类型名称/编码"
              style="width: 240px"
              clearable
            />
            <el-button
              type="primary"
              @click="loadApplyTypes"
            >
              搜索
            </el-button>
          </div>
          <el-table
            :data="applyTypes"
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
              label="基础保障金"
              width="160"
            >
              <template #default="{ row }">
                <el-input-number
                  v-model="row.depositAmount"
                  :min="0"
                  :precision="2"
                  size="small"
                  @change="handleApplyAmountChange(row)"
                />
              </template>
            </el-table-column>
            <el-table-column
              label="额外佣金率"
              width="140"
            >
              <template #default="{ row }">
                {{ (row.commissionExtraRate * 100).toFixed(2) }}%
              </template>
            </el-table-column>
            <el-table-column
              label="试用天数"
              width="100"
            >
              <template #default="{ row }">
                {{ row.trialDays }}天
              </template>
            </el-table-column>
            <el-table-column
              label="排序"
              width="80"
            >
              <template #default="{ row }">
                {{ row.sortOrder }}
              </template>
            </el-table-column>
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
              width="120"
              fixed="right"
            >
              <template #default="{ row }">
                <el-button
                  type="primary"
                  text
                  size="small"
                  @click="handleEditApplyType(row)"
                >
                  编辑
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane
          label="店铺分类保障金"
          name="category"
        >
          <div class="search-bar">
            <el-input
              v-model="categorySearch.keyword"
              placeholder="搜索分类名称/编码"
              style="width: 240px"
              clearable
            />
            <el-button
              type="primary"
              @click="loadCategories"
            >
              搜索
            </el-button>
          </div>
          <el-table
            :data="categories"
            border
            stripe
          >
            <el-table-column
              prop="id"
              label="ID"
              width="70"
            />
            <el-table-column
              prop="categoryCode"
              label="分类编码"
              width="140"
            />
            <el-table-column
              prop="categoryName"
              label="分类名称"
              min-width="140"
            />
            <el-table-column
              label="分类级别"
              width="100"
            >
              <template #default="{ row }">
                <el-tag
                  size="small"
                  :type="row.parentId === 0 ? 'primary' : 'info'"
                >
                  {{ row.parentId === 0 ? '一级' : '二级' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              label="最低保障金"
              width="180"
            >
              <template #default="{ row }">
                <el-input-number
                  v-model="row.depositMin"
                  :min="0"
                  :precision="2"
                  size="small"
                  @change="handleCategoryDepositChange(row)"
                />
              </template>
            </el-table-column>
            <el-table-column
              label="佣金率"
              width="100"
            >
              <template #default="{ row }">
                {{ (row.commissionRate * 100).toFixed(2) }}%
              </template>
            </el-table-column>
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
              width="120"
              fixed="right"
            >
              <template #default="{ row }">
                <el-button
                  type="primary"
                  text
                  size="small"
                  @click="handleEditCategory(row)"
                >
                  编辑
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog
      v-model="applyDialogVisible"
      title="编辑入驻类型"
      width="560px"
      @close="resetApplyForm"
    >
      <el-form
        ref="applyFormRef"
        :model="applyForm"
        :rules="applyFormRules"
        label-width="110px"
      >
        <el-form-item
          label="类型编码"
          prop="typeCode"
        >
          <el-input
            v-model="applyForm.typeCode"
            disabled
          />
        </el-form-item>
        <el-form-item
          label="类型名称"
          prop="typeName"
        >
          <el-input
            v-model="applyForm.typeName"
            maxlength="32"
          />
        </el-form-item>
        <el-form-item
          label="基础保障金"
          prop="depositAmount"
        >
          <el-input-number
            v-model="applyForm.depositAmount"
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
            v-model="applyForm.commissionExtraRate"
            :min="-0.5"
            :max="0.5"
            :step="0.005"
            :precision="4"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item
          label="试用天数"
          prop="trialDays"
        >
          <el-input-number
            v-model="applyForm.trialDays"
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
            v-model="applyForm.sortOrder"
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
            v-model="applyForm.description"
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
            v-model="applyForm.status"
            :active-value="1"
            :inactive-value="0"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="saving"
          @click="handleSubmitApplyType"
        >
          确定
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="categoryDialogVisible"
      title="编辑店铺分类"
      width="560px"
      @close="resetCategoryForm"
    >
      <el-form
        ref="categoryFormRef"
        :model="categoryForm"
        :rules="categoryFormRules"
        label-width="110px"
      >
        <el-form-item
          label="分类编码"
          prop="categoryCode"
        >
          <el-input
            v-model="categoryForm.categoryCode"
            disabled
          />
        </el-form-item>
        <el-form-item
          label="分类名称"
          prop="categoryName"
        >
          <el-input
            v-model="categoryForm.categoryName"
            maxlength="32"
          />
        </el-form-item>
        <el-form-item
          label="最低保障金"
          prop="depositMin"
        >
          <el-input-number
            v-model="categoryForm.depositMin"
            :min="0"
            :precision="2"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item
          label="佣金率"
          prop="commissionRate"
        >
          <el-input-number
            v-model="categoryForm.commissionRate"
            :min="0"
            :max="1"
            :step="0.0001"
            :precision="4"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item
          label="排序"
          prop="sortOrder"
        >
          <el-input-number
            v-model="categoryForm.sortOrder"
            :min="0"
            :max="9999"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item
          label="状态"
          prop="status"
        >
          <el-switch
            v-model="categoryForm.status"
            :active-value="1"
            :inactive-value="0"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="saving"
          @click="handleSubmitCategory"
        >
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import {
  listApplyTypePage, updateApplyType,
  listShopCategoryPage, updateShopCategory,
  type ApplyType, type ShopCategory,
} from '@/api/merchant';

const loading = ref(false);
const saving = ref(false);
const activeTab = ref('apply-type');

const applySearch = reactive({ keyword: '' });
const applyTypes = ref<ApplyType[]>([]);
const applyDialogVisible = ref(false);
const editingApplyId = ref<number | null>(null);
const applyFormRef = ref<FormInstance>();
const applyForm = reactive<Omit<ApplyType, 'id'>>({
  typeCode: '', typeName: '', requiredQualifications: [], depositAmount: 0,
  commissionExtraRate: 0, auditRequired: 1, trialDays: 0, sortOrder: 1, description: '', status: 1,
});
const applyFormRules: FormRules = {
  typeName: [{ required: true, message: '请输入类型名称', trigger: 'blur' }],
  depositAmount: [{ required: true, message: '请输入基础保障金', trigger: 'blur' }],
};

const categorySearch = reactive({ keyword: '' });
const categories = ref<ShopCategory[]>([]);
const categoryDialogVisible = ref(false);
const editingCategoryId = ref<number | null>(null);
const categoryFormRef = ref<FormInstance>();
const categoryForm = reactive<Omit<ShopCategory, 'id' | 'children'>>({
  categoryName: '', parentId: 0, categoryCode: '', sortOrder: 1, commissionRate: 0.05,
  depositMin: 5000, icon: '', description: '', status: 1,
});
const categoryFormRules: FormRules = {
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  depositMin: [{ required: true, message: '请输入最低保障金', trigger: 'blur' }],
};

async function loadApplyTypes() {
  loading.value = true;
  try {
    const res = await listApplyTypePage({ ...applySearch, current: 1, size: 100 });
    applyTypes.value = res.records;
  } catch {
    ElMessage.error('加载入驻类型失败');
  } finally {
    loading.value = false;
  }
}

async function loadCategories() {
  loading.value = true;
  try {
    const res = await listShopCategoryPage({ ...categorySearch, current: 1, size: 100 });
    categories.value = res.records;
  } catch {
    ElMessage.error('加载分类失败');
  } finally {
    loading.value = false;
  }
}

function handleTabChange(tabName: string | number) {
  if (tabName === 'category' && categories.value.length === 0) {
    loadCategories();
  } else if (tabName === 'apply-type' && applyTypes.value.length === 0) {
    loadApplyTypes();
  }
}

function handleApplyAmountChange(row: ApplyType) {
  updateApplyType(row.id, {
    typeCode: row.typeCode, typeName: row.typeName, requiredQualifications: row.requiredQualifications,
    depositAmount: row.depositAmount, commissionExtraRate: row.commissionExtraRate,
    auditRequired: row.auditRequired, trialDays: row.trialDays, sortOrder: row.sortOrder,
    description: row.description, status: row.status,
  }).then(() => ElMessage.success('保障金已更新')).catch(() => ElMessage.error('更新失败'));
}

function handleCategoryDepositChange(row: ShopCategory) {
  updateShopCategory(row.id, {
    categoryName: row.categoryName, parentId: row.parentId, categoryCode: row.categoryCode,
    sortOrder: row.sortOrder, commissionRate: row.commissionRate, depositMin: row.depositMin,
    icon: row.icon, description: row.description, status: row.status,
  }).then(() => ElMessage.success('保障金已更新')).catch(() => ElMessage.error('更新失败'));
}

function resetApplyForm() {
  editingApplyId.value = null;
  Object.assign(applyForm, {
    typeCode: '', typeName: '', requiredQualifications: [], depositAmount: 0,
    commissionExtraRate: 0, auditRequired: 1, trialDays: 0, sortOrder: 1, description: '', status: 1,
  });
  applyFormRef.value?.clearValidate();
}

function handleEditApplyType(row: ApplyType) {
  editingApplyId.value = row.id;
  Object.assign(applyForm, {
    typeCode: row.typeCode, typeName: row.typeName,
    requiredQualifications: [...row.requiredQualifications], depositAmount: row.depositAmount,
    commissionExtraRate: row.commissionExtraRate, auditRequired: row.auditRequired,
    trialDays: row.trialDays, sortOrder: row.sortOrder, description: row.description, status: row.status,
  });
  applyDialogVisible.value = true;
}

async function handleSubmitApplyType() {
  if (!applyFormRef.value) return;
  await applyFormRef.value.validate(async (valid) => {
    if (!valid || !editingApplyId.value) return;
    saving.value = true;
    try {
      await updateApplyType(editingApplyId.value, { ...applyForm });
      ElMessage.success('已保存');
      applyDialogVisible.value = false;
      loadApplyTypes();
    } catch {
      ElMessage.error('保存失败');
    } finally {
      saving.value = false;
    }
  });
}

function resetCategoryForm() {
  editingCategoryId.value = null;
  Object.assign(categoryForm, {
    categoryName: '', parentId: 0, categoryCode: '', sortOrder: 1, commissionRate: 0.05,
    depositMin: 5000, icon: '', description: '', status: 1,
  });
  categoryFormRef.value?.clearValidate();
}

function handleEditCategory(row: ShopCategory) {
  editingCategoryId.value = row.id;
  Object.assign(categoryForm, {
    categoryName: row.categoryName, parentId: row.parentId, categoryCode: row.categoryCode,
    sortOrder: row.sortOrder, commissionRate: row.commissionRate, depositMin: row.depositMin,
    icon: row.icon, description: row.description, status: row.status,
  });
  categoryDialogVisible.value = true;
}

async function handleSubmitCategory() {
  if (!categoryFormRef.value) return;
  await categoryFormRef.value.validate(async (valid) => {
    if (!valid || !editingCategoryId.value) return;
    saving.value = true;
    try {
      await updateShopCategory(editingCategoryId.value, { ...categoryForm });
      ElMessage.success('已保存');
      categoryDialogVisible.value = false;
      loadCategories();
    } catch {
      ElMessage.error('保存失败');
    } finally {
      saving.value = false;
    }
  });
}

onMounted(() => { loadApplyTypes(); });
</script>

<style scoped>
.page-container { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 18px; font-weight: 600; }
.search-bar { display: flex; gap: 12px; margin-bottom: 20px; }
</style>
