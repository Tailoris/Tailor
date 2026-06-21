<template>
  <div class="product-list-view" role="main" aria-label="商品列表页面">
    <el-breadcrumb separator="/" aria-label="面包屑导航">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>商品列表</el-breadcrumb-item>
    </el-breadcrumb>

    <div class="layout">
      <aside class="sidebar">
        <div class="filter-section">
          <h3>商品分类</h3>
          <el-tree
            :data="categories"
            :props="{ children: 'children', label: 'name' }"
            node-key="id"
            highlight-current
            @node-click="handleCategoryClick"
          />
        </div>
        <div class="filter-section">
          <h3>价格区间</h3>
          <div class="price-filter">
            <el-input-number v-model="minPrice" :min="0" :precision="2" placeholder="最低价" controls-position="right" />
            <span>-</span>
            <el-input-number v-model="maxPrice" :min="0" :precision="2" placeholder="最高价" controls-position="right" />
            <el-button type="primary" size="small" @click="handlePriceFilter">确定</el-button>
          </div>
        </div>
      </aside>

      <main class="main-content">
        <div class="toolbar">
          <div class="sort-options">
            <span
              v-for="option in sortOptions"
              :key="option.value"
              :class="{ active: sort === option.value }"
              @click="handleSort(option.value)"
            >
              {{ option.label }}
            </span>
          </div>
          <span class="total-text">共 {{ total }} 件商品</span>
        </div>

        <el-skeleton :loading="loading" animated :rows="4">
          <template v-if="products.length > 0">
            <div class="product-grid">
              <ProductCard v-for="product in products" :key="product.id" :product="product" />
            </div>
            <div class="pagination-wrapper">
              <el-pagination
                v-model:current-page="currentPage"
                v-model:page-size="pageSize"
                :total="total"
                :page-sizes="[12, 24, 48]"
                layout="total, sizes, prev, pager, next"
                @size-change="loadProducts"
                @current-change="loadProducts"
              />
            </div>
          </template>
          <el-empty v-else-if="!loading" description="暂无商品" />
        </el-skeleton>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import ProductCard from '@/components/ProductCard.vue'
import { getProducts, getCategories } from '@/api/product'
import type { Product, ProductCategory } from '@/types'

const route = useRoute()

const categories = ref<ProductCategory[]>([])
const products = ref<Product[]>([])
const loading = ref(true)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(12)
const sort = ref('')
const minPrice = ref<number>()
const maxPrice = ref<number>()
const selectedCategoryId = ref<number>()

const sortOptions = [
  { label: '综合', value: '' },
  { label: '销量', value: 'sales' },
  { label: '价格 ↑', value: 'price_asc' },
  { label: '价格 ↓', value: 'price_desc' },
  { label: '新品', value: 'new' }
]

async function loadCategories() {
  try {
    categories.value = await getCategories()
  } catch {
    categories.value = []
  }
}

async function loadProducts() {
  loading.value = true
  try {
    const keyword = route.query.keyword as string | undefined
    const res = await getProducts({
      categoryId: selectedCategoryId.value,
      keyword,
      sort: sort.value,
      minPrice: minPrice.value,
      maxPrice: maxPrice.value,
      current: currentPage.value,
      size: pageSize.value
    })
    products.value = res.records
    total.value = res.total
  } catch {
    products.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 自定义防抖：快速点击筛选时合并为一次请求，避免竞态
function debounce<T extends (...args: any[]) => void>(fn: T, delay: number): T {
  let timer: ReturnType<typeof setTimeout>
  return ((...args: any[]) => {
    clearTimeout(timer)
    timer = setTimeout(() => fn(...args), delay)
  }) as T
}

// 筛选操作（排序/分类/价格）使用防抖加载
const debouncedLoadProducts = debounce(loadProducts, 300)

function handleCategoryClick(node: ProductCategory) {
  selectedCategoryId.value = node.id
  currentPage.value = 1
  debouncedLoadProducts()
}

function handleSort(value: string) {
  sort.value = value
  currentPage.value = 1
  debouncedLoadProducts()
}

function handlePriceFilter() {
  currentPage.value = 1
  debouncedLoadProducts()
}

// 监听路由 keyword 变化（如首页搜索框跳转），keep-alive 下也能刷新列表
watch(
  () => route.query.keyword,
  () => {
    currentPage.value = 1
    loadProducts()
  }
)

onMounted(async () => {
  if (route.query.categoryId) {
    selectedCategoryId.value = Number(route.query.categoryId)
  }
  await Promise.all([loadCategories(), loadProducts()])
})
</script>

<style scoped>
.product-list-view {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}
.el-breadcrumb {
  margin-bottom: 20px;
}
.layout {
  display: flex;
  gap: 24px;
}
.sidebar {
  width: 240px;
  flex-shrink: 0;
}
.filter-section {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
}
.filter-section h3 {
  font-size: 16px;
  margin: 0 0 12px;
  color: #333;
}
.price-filter {
  display: flex;
  align-items: center;
  gap: 8px;
}
.price-filter .el-input-number {
  width: 80px;
}
.main-content {
  flex: 1;
  min-width: 0;
}
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 16px;
}
.sort-options {
  display: flex;
  gap: 16px;
}
.sort-options span {
  cursor: pointer;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 14px;
  color: #666;
  transition: all 0.2s;
}
.sort-options span:hover,
.sort-options span.active {
  color: #1d39c4;
  background: #e6f7ff;
}
.total-text {
  font-size: 14px;
  color: #999;
}
.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}
</style>
