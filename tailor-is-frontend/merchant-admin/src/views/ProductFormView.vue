<template>
  <div class="product-form" v-loading="pageLoading">
    <PageHeader :title="isEdit ? '编辑商品' : '创建商品'" />

    <el-card shadow="never">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        label-position="right"
      >
        <el-tabs v-model="activeTab">
          <el-tab-pane label="基本信息" name="basic">
            <el-form-item label="商品名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入商品名称" maxlength="100" show-word-limit />
            </el-form-item>
            <el-form-item label="商品分类" prop="category">
              <el-select v-model="form.category" placeholder="请选择分类" style="width: 300px">
                <el-option label="服装" value="clothing" />
                <el-option label="配饰" value="accessories" />
                <el-option label="鞋帽" value="shoes_hats" />
                <el-option label="箱包" value="bags" />
                <el-option label="其他" value="other" />
              </el-select>
            </el-form-item>
            <el-form-item label="商品类型" prop="type">
              <el-radio-group v-model="form.type">
                <el-radio label="physical">实物商品</el-radio>
                <el-radio label="virtual">虚拟商品</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="商品描述" prop="description">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="4"
                placeholder="请输入商品描述"
                maxlength="500"
                show-word-limit
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane label="SKU设置" name="sku">
            <el-button type="primary" size="small" @click="addSku" style="margin-bottom: 16px">
              <el-icon><Plus /></el-icon>
              添加SKU
            </el-button>
            <el-table :data="form.skus" border style="width: 100%">
              <el-table-column label="SKU名称" min-width="150">
                <template #default="{ row }">
                  <el-input v-model="row.name" placeholder="如：红色/XL" />
                </template>
              </el-table-column>
              <el-table-column label="SKU编码" width="140">
                <template #default="{ row }">
                  <el-input v-model="row.skuCode" placeholder="SKU编码" />
                </template>
              </el-table-column>
              <el-table-column label="售价(元)" width="140">
                <template #default="{ row }">
                  <el-input-number v-model="row.price" :min="0" :precision="2" :controls="false" style="width: 100%" />
                </template>
              </el-table-column>
              <el-table-column label="原价(元)" width="140">
                <template #default="{ row }">
                  <el-input-number v-model="row.originalPrice" :min="0" :precision="2" :controls="false" style="width: 100%" />
                </template>
              </el-table-column>
              <el-table-column label="库存" width="120">
                <template #default="{ row }">
                  <el-input-number v-model="row.stock" :min="0" :controls="false" style="width: 100%" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="80">
                <template #default="{ $index }">
                  <el-button link type="danger" @click="removeSku($index)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="图片上传" name="images">
            <div class="upload-zone">
              <el-upload
                list-type="picture-card"
                :action="''"
                :auto-upload="false"
                :on-change="handleImageChange"
                :file-list="imageFileList"
                :limit="9"
              >
                <el-icon><Plus /></el-icon>
                <template #tip>
                  <div class="el-upload__tip">支持 jpg/png 格式，最多9张</div>
                </template>
              </el-upload>
            </div>
          </el-tab-pane>

          <el-tab-pane label="商品详情" name="detail">
            <div class="detail-placeholder">
              <el-icon :size="48" color="#C0C4CC"><Document /></el-icon>
              <p>富文本编辑器区域</p>
              <span class="placeholder-hint">可集成 wangEditor、TinyMCE 等富文本编辑器</span>
            </div>
          </el-tab-pane>
        </el-tabs>

        <el-form-item style="margin-top: 24px">
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            {{ isEdit ? '保存修改' : '提交审核' }}
          </el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createProduct, updateProduct, getProductDetail, uploadImage } from '@/api/product'
import type { ProductSku } from '@/types'
import PageHeader from '@/components/PageHeader.vue'
import { ElMessage } from 'element-plus'
import { Plus, Document } from '@element-plus/icons-vue'
import type { FormInstance, UploadFile } from 'element-plus'

const route = useRoute()
const router = useRouter()

const formRef = ref<FormInstance>()
const activeTab = ref('basic')
const submitting = ref(false)
const pageLoading = ref(false)
const imageFileList = ref<UploadFile[]>([])

const isEdit = computed(() => !!route.params.id)

const form = reactive({
  name: '',
  category: '',
  type: 'physical' as 'physical' | 'virtual',
  description: '',
  images: [] as string[],
  skus: [] as Omit<ProductSku, 'id' | 'productId'>[],
})

const rules = {
  name: [
    { required: true, message: '请输入商品名称', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在2-100个字符', trigger: 'blur' },
  ],
  category: [{ required: true, message: '请选择商品分类', trigger: 'change' }],
  type: [{ required: true, message: '请选择商品类型', trigger: 'change' }],
}

function addSku() {
  form.skus.push({
    skuCode: `SKU${Date.now()}`,
    name: '',
    price: 0,
    originalPrice: 0,
    stock: 0,
    image: '',
    status: 1,
  })
}

function removeSku(index: number) {
  form.skus.splice(index, 1)
}

async function handleImageChange(file: UploadFile) {
  if (file.raw) {
    try {
      const res = await uploadImage(file.raw)
      form.images.push(res.url)
    } catch {
      ElMessage.error('图片上传失败')
    }
  }
}

async function loadProduct() {
  if (!isEdit.value) return
  pageLoading.value = true
  try {
    const res = await getProductDetail(Number(route.params.id))
    form.name = res.name
    form.category = res.category
    form.type = res.type
    form.description = res.description
    form.images = res.images || []
    form.skus = res.skus?.map((sku: ProductSku) => ({
      skuCode: sku.skuCode,
      name: sku.name,
      price: sku.price,
      originalPrice: sku.originalPrice,
      stock: sku.stock,
      image: sku.image,
      status: sku.status,
    })) || []
  } catch {
    // error handled by interceptor
  } finally {
    pageLoading.value = false
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        if (isEdit.value) {
          await updateProduct(Number(route.params.id), form)
          ElMessage.success('修改成功')
        } else {
          await createProduct(form)
          ElMessage.success('创建成功')
        }
        router.push('/product')
      } catch {
        // error handled by interceptor
      } finally {
        submitting.value = false
      }
    }
  })
}

onMounted(() => {
  loadProduct()
})
</script>

<style scoped>
.upload-zone {
  padding: 20px;
}

.detail-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  color: var(--color-text-secondary);
  background: var(--color-background);
  border-radius: var(--border-radius);
  border: 2px dashed var(--color-border);
}

.detail-placeholder p {
  margin: 12px 0 4px;
  font-size: 16px;
}

.placeholder-hint {
  font-size: 13px;
  color: var(--color-text-placeholder);
}
</style>
