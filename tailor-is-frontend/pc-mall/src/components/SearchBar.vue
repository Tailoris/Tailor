<template>
  <div class="search-bar">
    <el-select v-model="selectedCategory" placeholder="全部分类" class="category-select" clearable>
      <el-option label="全部分类" value="" />
      <el-option
        v-for="cat in categories"
        :key="cat.id"
        :label="cat.name"
        :value="cat.id"
      />
    </el-select>
    <el-input
      v-model="keyword"
      placeholder="搜索商品、店铺"
      class="search-input"
      clearable
      aria-label="搜索"
      @keyup.enter="handleSearch"
    >
      <template #prefix>
        <el-icon><Search /></el-icon>
      </template>
    </el-input>
    <el-button type="primary" class="search-btn" @click="handleSearch">
      搜索
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { getCategories } from '@/api/product'
import type { ProductCategory } from '@/types'

const emit = defineEmits<{
  search: [data: { keyword: string; categoryId: number | '' }]
}>()

const keyword = ref('')
const selectedCategory = ref<number | ''>('')
const categories = ref<ProductCategory[]>([])

async function loadCategories() {
  try {
    const res = await getCategories()
    categories.value = res.filter((c: ProductCategory) => c.parentId === 0)
  } catch {
    categories.value = []
  }
}

function handleSearch() {
  emit('search', {
    keyword: keyword.value,
    categoryId: selectedCategory.value
  })
}

onMounted(() => {
  loadCategories()
})
</script>

<style scoped>
.search-bar {
  display: flex;
  gap: 12px;
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}
.category-select {
  width: 160px;
}
.search-input {
  flex: 1;
}
.search-btn {
  min-width: 100px;
}
</style>
