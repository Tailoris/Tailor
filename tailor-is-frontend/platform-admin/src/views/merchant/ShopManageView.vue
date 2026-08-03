<template>
  <div
    v-loading="loading"
    class="page-container"
  >
    <div class="page-header">
      <h2>店铺管理</h2>
      <el-button
        type="primary"
        @click="handleAdd"
      >
        <el-icon><Plus /></el-icon>新增店铺
      </el-button>
    </div>
    <el-card class="page-card">
      <div class="search-bar">
        <el-input
          v-model="searchForm.shopName"
          placeholder="搜索店铺名称"
          style="width: 220px"
          clearable
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select
          v-model="searchForm.shopStatus"
          placeholder="店铺状态"
          clearable
          style="width: 150px"
        >
          <el-option
            label="装修中"
            :value="0"
          />
          <el-option
            label="营业中"
            :value="1"
          />
          <el-option
            label="暂停营业"
            :value="2"
          />
          <el-option
            label="已关闭"
            :value="3"
          />
        </el-select>
        <el-input
          v-model="searchForm.merchantId"
          placeholder="商家ID"
          style="width: 140px"
          clearable
        />
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
          prop="shopName"
          label="店铺名称"
          min-width="160"
          show-overflow-tooltip
        />
        <el-table-column
          prop="merchantId"
          label="商家ID"
          width="90"
        />
        <el-table-column
          label="Logo"
          width="80"
          align="center"
        >
          <template #default="{ row }">
            <el-avatar
              :size="32"
              :src="row.shopLogo"
            >
              <el-icon><ShopIcon /></el-icon>
            </el-avatar>
          </template>
        </el-table-column>
        <el-table-column
          label="店铺评分"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-rate
              v-model="row.shopRating"
              disabled
              :max="5"
              :colors="['#f56c6c', '#e6a23c', '#67c23a']"
              size="small"
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="fansCount"
          label="粉丝数"
          width="100"
          align="right"
        />
        <el-table-column
          prop="goodsCount"
          label="商品数"
          width="90"
          align="right"
        />
        <el-table-column
          prop="salesCount"
          label="销量"
          width="100"
          align="right"
        />
        <el-table-column
          label="状态"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-tag
              :type="shopStatusTag(row.shopStatus)"
              size="small"
            >
              {{ shopStatusText(row.shopStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="createTime"
          label="创建时间"
          width="170"
        />
        <el-table-column
          label="操作"
          width="240"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              type="primary"
              text
              size="small"
              @click="handleView(row)"
            >
              详情
            </el-button>
            <el-button
              type="warning"
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
              @click="handleToggle(row)"
            >
              {{ row.shopStatus === 1 ? '暂停' : '开启' }}
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
      width="640px"
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item
          label="店铺名称"
          prop="shopName"
        >
          <el-input
            v-model="form.shopName"
            placeholder="请输入店铺名称"
            maxlength="64"
          />
        </el-form-item>
        <el-form-item
          label="商家ID"
          prop="merchantId"
        >
          <el-input-number
            v-model="form.merchantId"
            :min="1"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="Logo">
          <el-input
            v-model="form.shopLogo"
            placeholder="Logo图片URL"
            maxlength="255"
          />
        </el-form-item>
        <el-form-item label="Banner">
          <el-input
            v-model="form.shopBanner"
            placeholder="Banner图片URL"
            maxlength="255"
          />
        </el-form-item>
        <el-form-item label="店铺简介">
          <el-input
            v-model="form.shopDesc"
            type="textarea"
            :rows="2"
            placeholder="请输入店铺简介"
            maxlength="256"
          />
        </el-form-item>
        <el-form-item label="主题">
          <el-input
            v-model="form.shopTheme"
            placeholder="主题标识，默认default"
            maxlength="32"
          />
        </el-form-item>
        <el-form-item label="公告">
          <el-input
            v-model="form.announcement"
            type="textarea"
            :rows="2"
            placeholder="店铺公告"
            maxlength="500"
          />
        </el-form-item>
        <el-form-item label="客服联系方式">
          <el-input
            v-model="form.contactService"
            placeholder="客服电话或在线联系方式"
            maxlength="64"
          />
        </el-form-item>
        <el-form-item label="所在地区">
          <div style="display: flex; gap: 8px; width: 100%">
            <el-input
              v-model="form.province"
              placeholder="省"
              maxlength="16"
              style="flex: 1"
            />
            <el-input
              v-model="form.city"
              placeholder="市"
              maxlength="16"
              style="flex: 1"
            />
            <el-input
              v-model="form.district"
              placeholder="区"
              maxlength="16"
              style="flex: 1"
            />
          </div>
        </el-form-item>
        <el-form-item label="详细地址">
          <el-input
            v-model="form.address"
            placeholder="详细地址"
            maxlength="255"
          />
        </el-form-item>
        <el-form-item label="经纬度">
          <div style="display: flex; gap: 8px; width: 100%">
            <el-input-number
              v-model="form.longitude"
              :precision="6"
              :step="0.0001"
              placeholder="经度"
              style="flex: 1"
            />
            <el-input-number
              v-model="form.latitude"
              :precision="6"
              :step="0.0001"
              placeholder="纬度"
              style="flex: 1"
            />
          </div>
        </el-form-item>
        <el-form-item
          label="店铺状态"
          prop="shopStatus"
        >
          <el-select
            v-model="form.shopStatus"
            style="width: 100%"
          >
            <el-option
              label="装修中"
              :value="0"
            />
            <el-option
              label="营业中"
              :value="1"
            />
            <el-option
              label="暂停营业"
              :value="2"
            />
            <el-option
              label="已关闭"
              :value="3"
            />
          </el-select>
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

    <el-dialog
      v-model="detailVisible"
      title="店铺详情"
      width="640px"
    >
      <el-descriptions
        v-if="currentItem"
        :column="2"
        border
        size="small"
      >
        <el-descriptions-item label="店铺ID">
          {{ currentItem.id }}
        </el-descriptions-item>
        <el-descriptions-item label="商家ID">
          {{ currentItem.merchantId }}
        </el-descriptions-item>
        <el-descriptions-item
          label="店铺名称"
          :span="2"
        >
          {{ currentItem.shopName }}
        </el-descriptions-item>
        <el-descriptions-item
          label="评分"
          :span="2"
        >
          <el-rate
            v-model="currentItem.shopRating"
            disabled
            size="small"
          />
        </el-descriptions-item>
        <el-descriptions-item label="粉丝数">
          {{ currentItem.fansCount }}
        </el-descriptions-item>
        <el-descriptions-item label="商品数">
          {{ currentItem.goodsCount }}
        </el-descriptions-item>
        <el-descriptions-item label="累计销量">
          {{ currentItem.salesCount }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag
            :type="shopStatusTag(currentItem.shopStatus)"
            size="small"
          >
            {{ shopStatusText(currentItem.shopStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item
          label="客服"
          :span="2"
        >
          {{ currentItem.contactService }}
        </el-descriptions-item>
        <el-descriptions-item
          label="地址"
          :span="2"
        >
          {{ currentItem.province }} {{ currentItem.city }} {{ currentItem.district }} {{ currentItem.address }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ currentItem.createTime }}
        </el-descriptions-item>
        <el-descriptions-item label="更新时间">
          {{ currentItem.updateTime }}
        </el-descriptions-item>
        <el-descriptions-item
          label="公告"
          :span="2"
        >
          {{ currentItem.announcement || '-' }}
        </el-descriptions-item>
        <el-descriptions-item
          label="店铺简介"
          :span="2"
        >
          {{ currentItem.shopDesc || '-' }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">
          关闭
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import {
  listShops, getShopById, createShop, updateShop, toggleShopStatus, type Shop,
} from '@/api/merchant';
import { Plus, Search, Shop as ShopIcon } from '@element-plus/icons-vue';

const loading = ref(false);
const saving = ref(false);
const tableData = ref<Shop[]>([]);
const pagination = reactive({ current: 1, size: 10, total: 0 });
const searchForm = reactive({ shopName: '', shopStatus: null as number | null, merchantId: '' });

const dialogVisible = ref(false);
const detailVisible = ref(false);
const isEdit = ref(false);
const editId = ref<number | null>(null);
const currentItem = ref<Shop | null>(null);
const formRef = ref<FormInstance>();
const form = reactive<Partial<Shop>>({
  shopName: '', merchantId: 1, shopLogo: '', shopBanner: '', shopDesc: '', shopTheme: 'default',
  announcement: '', contactService: '', province: '', city: '', district: '', address: '',
  longitude: 0, latitude: 0, shopStatus: 1,
});
const formRules: FormRules = {
  shopName: [{ required: true, message: '请输入店铺名称', trigger: 'blur' }],
  merchantId: [{ required: true, message: '请输入商家ID', trigger: 'change' }],
  shopStatus: [{ required: true, message: '请选择店铺状态', trigger: 'change' }],
};
const dialogTitle = computed(() => (isEdit.value ? '编辑店铺' : '新增店铺'));

function shopStatusText(s: number) {
  return ['装修中', '营业中', '暂停营业', '已关闭'][s] || '未知';
}
function shopStatusTag(s: number) {
  return ['info', 'success', 'warning', 'danger'][s] || 'info';
}

async function loadData() {
  loading.value = true;
  try {
    const params: Record<string, unknown> = {
      current: pagination.current,
      size: pagination.size,
      shopName: searchForm.shopName || undefined,
      shopStatus: searchForm.shopStatus ?? undefined,
      merchantId: searchForm.merchantId || undefined,
    };
    const res = await listShops(params);
    tableData.value = res.records;
    pagination.total = res.total;
  } catch (e) {
    ElMessage.error('加载店铺列表失败');
  } finally {
    loading.value = false;
  }
}
function handleSearch() { pagination.current = 1; loadData(); }
function handleReset() {
  searchForm.shopName = ''; searchForm.shopStatus = null; searchForm.merchantId = '';
  pagination.current = 1; loadData();
}

function resetForm() {
  Object.assign(form, {
    shopName: '', merchantId: 1, shopLogo: '', shopBanner: '', shopDesc: '', shopTheme: 'default',
    announcement: '', contactService: '', province: '', city: '', district: '', address: '',
    longitude: 0, latitude: 0, shopStatus: 1,
  });
  isEdit.value = false; editId.value = null;
  formRef.value?.clearValidate();
}
async function handleAdd() { resetForm(); dialogVisible.value = true; }
async function handleEdit(row: Shop) {
  try {
    const detail = await getShopById(row.id);
    Object.assign(form, detail);
    isEdit.value = true; editId.value = row.id; dialogVisible.value = true;
  } catch {
    ElMessage.error('加载店铺详情失败');
  }
}
async function handleView(row: Shop) {
  try {
    currentItem.value = await getShopById(row.id);
    detailVisible.value = true;
  } catch {
    ElMessage.error('加载店铺详情失败');
  }
}
async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  saving.value = true;
  try {
    if (isEdit.value && editId.value) {
      await updateShop(editId.value, form as Omit<Shop, 'id' | 'createTime' | 'updateTime'>);
      ElMessage.success('店铺更新成功');
    } else {
      await createShop(form as Omit<Shop, 'id' | 'createTime' | 'updateTime'>);
      ElMessage.success('店铺创建成功');
    }
    dialogVisible.value = false;
    loadData();
  } catch {
    ElMessage.error(isEdit.value ? '更新店铺失败' : '创建店铺失败');
  } finally {
    saving.value = false;
  }
}
async function handleToggle(row: Shop) {
  const nextStatus = row.shopStatus === 1 ? 2 : 1;
  const action = nextStatus === 1 ? '开启' : '暂停';
  try {
    await ElMessageBox.confirm(`确定要${action}店铺「${row.shopName}」吗？`, '操作确认', {
      type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消',
    });
    await toggleShopStatus(row.id, nextStatus);
    ElMessage.success(`${action}成功`);
    loadData();
  } catch (e) {
    if ((e as { action?: string }).action !== 'cancel') ElMessage.error(`${action}失败`);
  }
}

onMounted(loadData);
</script>

<style scoped>
.page-container { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: 20px; font-weight: 600; color: #1f2937; }
.page-card { border-radius: 8px; }
.search-bar { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
