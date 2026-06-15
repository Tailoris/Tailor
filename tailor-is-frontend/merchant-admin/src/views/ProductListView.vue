<template>
  <div class="product-list">
    <PageHeader title="商品管理">
      <template #actions>
        <el-button type="primary" @click="$router.push('/product/create')">
          <el-icon><Plus /></el-icon>
          创建商品
        </el-button>
      </template>
    </PageHeader>

    <el-card shadow="never" class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="关键词">
          <el-input
            v-model="searchForm.keyword"
            placeholder="商品名称"
            clearable
            style="width: 180px"
            @clear="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 140px" @change="handleSearch">
            <el-option label="草稿" value="draft" />
            <el-option label="待审核" value="pending" />
            <el-option label="上架" value="on_sale" />
            <el-option label="下架" value="off_sale" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="searchForm.category" placeholder="全部" clearable style="width: 140px" @change="handleSearch">
            <el-option label="服装" value="clothing" />
            <el-option label="配饰" value="accessories" />
            <el-option label="鞋帽" value="shoes_hats" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <ProductTable
        :products="products"
        :loading="loading"
        :total="total"
        :current-page="page"
        :page-size="pageSize"
        @edit="handleEdit"
        @delete="handleDelete"
        @update-status="handleUpdateStatus"
        @page-change="handlePageChange"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listProducts, deleteProduct, updateStatus } from '@/api/product'
import type { Product } from '@/types'
import PageHeader from '@/components/PageHeader.vue'
import ProductTable from '@/components/ProductTable.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const router = useRouter()

const loading = ref(false)
const products = ref<Product[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const searchForm = reactive({
  keyword: '',
  status: '',
  category: '',
})

async function fetchProducts() {
  loading.value = true
  try {
    const res = await listProducts({
      keyword: searchForm.keyword || undefined,
      status: searchForm.status || undefined,
      category: searchForm.category || undefined,
      current: page.value,
      size: pageSize.value,
    })
    products.value = res.records
    total.value = res.total
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  fetchProducts()
}

function resetSearch() {
  searchForm.keyword = ''
  searchForm.status = ''
  searchForm.category = ''
  handleSearch()
}

function handlePageChange(params: { page: number; pageSize: number }) {
  page.value = params.page
  pageSize.value = params.pageSize
  fetchProducts()
}

function handleEdit(id: number) {
  router.push(`/product/edit/${id}`)
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定要删除该商品吗？此操作不可恢复', '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteProduct(id)
    ElMessage.success('删除成功')
    fetchProducts()
  } catch {
    // cancelled or error
  }
}

async function handleUpdateStatus(id: number, status: string) {
  try {
    await updateStatus(id, status)
    ElMessage.success('状态更新成功')
    fetchProducts()
  } catch {
    // error handled by interceptor
  }
}

onMounted(() => {
  fetchProducts()
})
</script>

<style scoped>
.search-card :deep(.el-form-item) {
  margin-bottom: 0;
}
</style>
