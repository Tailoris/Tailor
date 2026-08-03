<template>
  <div
    v-loading="loading"
    class="page-container"
  >
    <div class="page-header">
      <h2>店铺分类</h2>
      <el-button
        type="primary"
        @click="handleAdd"
      >
        <el-icon><Plus /></el-icon>新增分类
      </el-button>
    </div>
    <el-card class="page-card">
      <div class="content-layout">
        <div class="tree-panel">
          <div class="tree-title">
            分类树
          </div>
          <el-tree
            :data="treeData"
            :props="{ label: 'categoryName', children: 'children' }"
            node-key="id"
            highlight-current
            @node-click="handleNodeClick"
          />
        </div>
        <div class="table-panel">
          <div class="search-bar">
            <el-input
              v-model="searchForm.keyword"
              placeholder="搜索分类名称/编码"
              style="width: 200px"
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
              label="佣金率"
              width="100"
            >
              <template #default="{ row }">
                {{ (row.commissionRate * 100).toFixed(2) }}%
              </template>
            </el-table-column>
            <el-table-column
              prop="depositMin"
              label="最低保障金"
              width="120"
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
        </div>
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="560px"
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item
          label="分类名称"
          prop="categoryName"
        >
          <el-input
            v-model="form.categoryName"
            placeholder="请输入分类名称"
            maxlength="32"
          />
        </el-form-item>
        <el-form-item
          label="上级分类"
          prop="parentId"
        >
          <el-tree-select
            v-model="form.parentId"
            :data="treeData"
            :props="{ label: 'categoryName', children: 'children', value: 'id' }"
            check-strictly
            :render-after-expand="false"
            placeholder="请选择上级分类（不选则为顶级）"
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item
          label="分类编码"
          prop="categoryCode"
        >
          <el-input
            v-model="form.categoryCode"
            placeholder="请输入分类编码"
            maxlength="32"
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
          label="佣金率"
          prop="commissionRate"
        >
          <el-input-number
            v-model="form.commissionRate"
            :min="0"
            :max="1"
            :step="0.0001"
            :precision="4"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item
          label="最低保障金"
          prop="depositMin"
        >
          <el-input-number
            v-model="form.depositMin"
            :min="0"
            :precision="2"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item
          label="图标"
          prop="icon"
        >
          <el-input
            v-model="form.icon"
            placeholder="图标URL"
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
  getShopCategoryTree, listShopCategoryPage, createShopCategory, updateShopCategory, deleteShopCategory,
  type ShopCategory,
} from '@/api/merchant';

const loading = ref(false);
const saving = ref(false);
const treeData = ref<ShopCategory[]>([]);
const tableData = ref<ShopCategory[]>([]);
const pagination = reactive({ current: 1, size: 10, total: 0 });
const searchForm = reactive({ keyword: '', status: '' as number | string, parentId: '' as number | string });

const dialogVisible = ref(false);
const dialogTitle = ref('新增分类');
const editingId = ref<number | null>(null);
const formRef = ref<FormInstance>();
const form = reactive<Omit<ShopCategory, 'id' | 'children'>>({
  categoryName: '', parentId: 0, categoryCode: '', sortOrder: 1, commissionRate: 0.05,
  depositMin: 5000, icon: '', description: '', status: 1,
});
const formRules: FormRules = {
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  categoryCode: [{ required: true, message: '请输入分类编码', trigger: 'blur' }],
};

async function loadTree() {
  try {
    treeData.value = await getShopCategoryTree();
  } catch {
    treeData.value = [];
  }
}

async function loadData() {
  loading.value = true;
  try {
    const res = await listShopCategoryPage({ ...searchForm, current: pagination.current, size: pagination.size });
    tableData.value = res.records;
    pagination.total = res.total;
  } catch {
    ElMessage.error('加载分类列表失败');
  } finally {
    loading.value = false;
  }
}

function handleNodeClick(data: ShopCategory) {
  searchForm.parentId = data.id;
  pagination.current = 1;
  loadData();
}

function handleSearch() { pagination.current = 1; loadData(); }
function handleReset() { searchForm.keyword = ''; searchForm.status = ''; searchForm.parentId = ''; pagination.current = 1; loadData(); }

function resetForm() {
  editingId.value = null;
  Object.assign(form, {
    categoryName: '', parentId: 0, categoryCode: '', sortOrder: 1, commissionRate: 0.05,
    depositMin: 5000, icon: '', description: '', status: 1,
  });
  formRef.value?.clearValidate();
}

function handleAdd() { resetForm(); dialogTitle.value = '新增分类'; dialogVisible.value = true; }

function handleEdit(row: ShopCategory) {
  editingId.value = row.id;
  dialogTitle.value = '编辑分类';
  Object.assign(form, {
    categoryName: row.categoryName, parentId: row.parentId, categoryCode: row.categoryCode,
    sortOrder: row.sortOrder, commissionRate: row.commissionRate, depositMin: row.depositMin,
    icon: row.icon, description: row.description, status: row.status,
  });
  dialogVisible.value = true;
}

async function handleSubmit() {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    saving.value = true;
    try {
      const payload = { ...form, parentId: form.parentId || 0 };
      if (editingId.value) {
        await updateShopCategory(editingId.value, payload);
        ElMessage.success('分类更新成功');
      } else {
        await createShopCategory(payload);
        ElMessage.success('分类创建成功');
      }
      dialogVisible.value = false;
      loadData();
      loadTree();
    } catch {
      ElMessage.error('操作失败');
    } finally {
      saving.value = false;
    }
  });
}

async function handleDelete(row: ShopCategory) {
  try {
    await ElMessageBox.confirm(`确定要删除分类 "${row.categoryName}" 吗？`, '删除确认', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
    });
    await deleteShopCategory(row.id);
    ElMessage.success('删除成功');
    loadData();
    loadTree();
  } catch { /* 取消 */ }
}

onMounted(() => { loadTree(); loadData(); });
</script>

<style scoped>
.page-container { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 18px; font-weight: 600; }
.content-layout { display: flex; gap: 20px; }
.tree-panel { width: 260px; flex-shrink: 0; border: 1px solid var(--color-border); border-radius: 6px; padding: 12px; }
.tree-title { font-weight: 600; font-size: 14px; margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px solid var(--color-border); }
.table-panel { flex: 1; min-width: 0; }
.search-bar { display: flex; gap: 12px; margin-bottom: 20px; flex-wrap: wrap; }
.pagination { margin-top: 20px; text-align: right; }
</style>
