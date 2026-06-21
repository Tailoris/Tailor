<template>
  <div class="shop-settings" v-loading="loading">
    <PageHeader title="店铺设置" />

    <el-row :gutter="20">
      <el-col :span="16">
        <el-card shadow="never" style="margin-bottom: 20px">
          <template #header>
            <span>基本信息</span>
          </template>
          <el-form :model="form" label-width="100px">
            <el-form-item label="店铺名称">
              <el-input v-model="form.name" placeholder="请输入店铺名称" />
            </el-form-item>
            <el-form-item label="店铺Logo">
              <el-upload
                class="logo-uploader"
                :action="''"
                :auto-upload="false"
                :show-file-list="false"
                :on-change="handleLogoChange"
              >
                <img v-if="form.logo" :src="form.logo" class="logo-preview" />
                <el-icon v-else :size="40" color="#C0C4CC"><Plus /></el-icon>
              </el-upload>
            </el-form-item>
            <el-form-item label="店铺描述">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="4"
                placeholder="请输入店铺描述"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSaveBasic" :loading="submitting">保存修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never">
          <template #header>
            <span>营业设置</span>
          </template>
          <el-form :model="businessForm" label-width="100px">
            <el-form-item label="营业时间">
              <el-time-picker
                v-model="businessTimeRange"
                is-range
                format="HH:mm"
                value-format="HH:mm"
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="店铺公告">
              <el-input
                v-model="businessForm.announcement"
                type="textarea"
                :rows="3"
                placeholder="请输入店铺公告"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSaveBusiness" :loading="submitting">保存设置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="never">
          <template #header>
            <span>店铺状态</span>
          </template>
          <div class="status-info">
            <div class="status-item">
              <span class="label">店铺ID</span>
              <span class="value">{{ shopInfo.id || '-' }}</span>
            </div>
            <div class="status-item">
              <span class="label">店铺状态</span>
              <el-tag :type="shopInfo.status === 1 ? 'success' : 'info'" size="small">
                {{ shopInfo.status === 1 ? '营业中' : '已关闭' }}
              </el-tag>
            </div>
            <div class="status-item">
              <span class="label">创建时间</span>
              <span class="value">{{ shopInfo.createdAt || '-' }}</span>
            </div>
            <div class="status-item">
              <span class="label">商家ID</span>
              <span class="value">{{ shopInfo.merchantId || '-' }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { getShopInfo, updateShopInfo } from '@/api/shop'
import { uploadImage } from '@/api/product'
import type { Shop } from '@/types'
import PageHeader from '@/components/PageHeader.vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { UploadFile } from 'element-plus'

const userStore = useUserStore()

const loading = ref(false)
const submitting = ref(false)

const shopInfo = reactive<Shop>({
  id: 0,
  name: '',
  logo: '',
  description: '',
  businessHours: '',
  announcement: '',
  status: 1,
  merchantId: 0,
  createdAt: '',
})

const form = reactive({
  name: '',
  logo: '',
  description: '',
})

const businessForm = reactive({
  businessHours: '',
  announcement: '',
})

const businessTimeRange = ref<[string, string] | null>(null)

async function fetchShopInfo() {
  loading.value = true
  try {
    const shopId = userStore.currentShopId || 1
    const res = await getShopInfo(shopId)
    Object.assign(shopInfo, res)
    form.name = res.name
    form.logo = res.logo
    form.description = res.description
    businessForm.businessHours = res.businessHours
    businessForm.announcement = res.announcement
    if (res.businessHours) {
      const [start, end] = res.businessHours.split('-')
      businessTimeRange.value = [start, end]
    }
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

async function handleLogoChange(file: UploadFile) {
  if (file.raw) {
    try {
      const res = await uploadImage(file.raw)
      form.logo = res.url
    } catch {
      ElMessage.error('图片上传失败')
    }
  }
}

async function handleSaveBasic() {
  submitting.value = true
  try {
    const shopId = userStore.currentShopId || 1
    await updateShopInfo(shopId, {
      name: form.name,
      logo: form.logo,
      description: form.description,
    })
    ElMessage.success('基本信息保存成功')
    fetchShopInfo()
  } catch {
    // error handled by interceptor
  } finally {
    submitting.value = false
  }
}

async function handleSaveBusiness() {
  submitting.value = true
  try {
    const shopId = userStore.currentShopId || 1
    const businessHours = businessTimeRange.value?.join('-') || ''
    await updateShopInfo(shopId, {
      businessHours,
      announcement: businessForm.announcement,
    })
    ElMessage.success('营业设置保存成功')
    fetchShopInfo()
  } catch {
    // error handled by interceptor
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchShopInfo()
})
</script>

<style scoped>
.logo-uploader {
  border: 2px dashed var(--color-border);
  border-radius: 8px;
  width: 120px;
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
  transition: border-color var(--transition);
}

.logo-uploader:hover {
  border-color: var(--color-primary);
}

.logo-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.status-info {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.status-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid var(--color-border);
}

.status-item:last-child {
  border-bottom: none;
}

.status-item .label {
  color: var(--color-text-secondary);
  font-size: 14px;
}

.status-item .value {
  color: var(--color-text-primary);
  font-weight: 500;
}
</style>
