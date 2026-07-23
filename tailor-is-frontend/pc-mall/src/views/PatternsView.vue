<template>
  <div
    class="patterns-view"
    role="main"
    aria-label="纸样商城"
  >
    <div class="view-header">
      <h1 class="page-title">
        纸样商城
      </h1>
      <p class="page-subtitle">
        海量专业纸样，支持3D试穿预览
      </p>
    </div>

    <div class="search-bar">
      <div class="search-box">
        <el-icon class="search-icon">
          <Search />
        </el-icon>
        <input
          v-model="searchKeyword"
          type="text"
          placeholder="搜索纸样、款式、版型"
          @keyup.enter="handleSearch"
        >
        <button
          class="search-btn"
          @click="handleSearch"
        >
          搜索
        </button>
      </div>
    </div>

    <div class="content-container">
      <aside class="sidebar">
        <div class="sidebar-section">
          <h3 class="sidebar-title">
            分类浏览
          </h3>
          <ul class="category-list">
            <li
              :class="{ active: selectedCategory === null }"
              @click="selectedCategory = null"
            >
              <span>全部纸样</span>
              <span class="count">{{ totalCount }}</span>
            </li>
            <li
              v-for="cat in categories"
              :key="cat.id"
              :class="{ active: selectedCategory === cat.id }"
              @click="selectedCategory = cat.id"
            >
              <span>{{ cat.name }}</span>
              <span class="count">{{ cat.productCount || 0 }}</span>
            </li>
          </ul>
        </div>

        <div class="sidebar-section">
          <h3 class="sidebar-title">
            版型风格
          </h3>
          <div class="style-tags">
            <button
              v-for="style in styles"
              :key="style"
              :class="{ active: selectedStyles.includes(style) }"
              @click="toggleStyle(style)"
            >
              {{ style }}
            </button>
          </div>
        </div>

        <div class="sidebar-section">
          <h3 class="sidebar-title">
            价格区间
          </h3>
          <div class="price-range">
            <input
              v-model.number="minPrice"
              type="number"
              placeholder="最低价"
            >
            <span class="price-separator">-</span>
            <input
              v-model.number="maxPrice"
              type="number"
              placeholder="最高价"
            >
            <button
              class="filter-btn"
              @click="applyPriceFilter"
            >
              筛选
            </button>
          </div>
        </div>
      </aside>

      <main class="main-content">
        <div class="toolbar">
          <div class="sort-options">
            <span class="sort-label">排序：</span>
            <select
              v-model="sortBy"
              @change="handleSort"
            >
              <option value="hot">
                热门推荐
              </option>
              <option value="new">
                最新上架
              </option>
              <option value="price_asc">
                价格从低到高
              </option>
              <option value="price_desc">
                价格从高到低
              </option>
            </select>
          </div>
          <div class="view-mode">
            <button
              :class="{ active: viewMode === 'grid' }"
              title="网格视图"
              @click="viewMode = 'grid'"
            >
              <el-icon><Grid /></el-icon>
            </button>
            <button
              :class="{ active: viewMode === 'list' }"
              title="列表视图"
              @click="viewMode = 'list'"
            >
              <el-icon><List /></el-icon>
            </button>
          </div>
        </div>

        <div
          v-if="!loading"
          :class="['patterns-grid', viewMode]"
        >
          <div
            v-for="product in products"
            :key="product.id"
            class="pattern-card"
            @click="goToDetail(product.id)"
          >
            <div class="pattern-image-wrapper">
              <img
                :src="product.mainImage"
                :alt="product.name"
                class="pattern-image"
              >
              <div class="pattern-tags">
                <span
                  v-if="product.specifications"
                  class="tag preview-tag"
                >3D预览</span>
                <span
                  v-if="product.status === 1"
                  class="tag hot-tag"
                >热销</span>
              </div>
            </div>
            <div class="pattern-info">
              <h3 class="pattern-name">
                {{ product.name }}
              </h3>
              <p class="pattern-subtitle">
                {{ product.subTitle }}
              </p>
              <div class="pattern-rating">
                <div class="star-rating">
                  <el-icon
                    v-for="n in 5"
                    :key="n"
                    class="star"
                    :class="{ filled: n <= Math.round(product.favorableRate / 20) }"
                  >
                    <Star />
                  </el-icon>
                </div>
                <span class="rating-text">{{ product.favorableRate }}%</span>
              </div>
              <div class="pattern-price">
                <span class="price">¥{{ product.minPrice }}</span>
                <span class="sales">销量 {{ product.saleCount }}</span>
              </div>
              <div class="pattern-labels">
                <span class="label">版型精准度</span>
                <span class="label">工艺清晰</span>
                <span class="label">适配性强</span>
              </div>
            </div>
          </div>
        </div>

        <div
          v-else
          class="loading-state"
        >
          <el-skeleton
            :rows="6"
            animated
          />
        </div>

        <div
          v-if="products.length === 0 && !loading"
          class="empty-state"
        >
          <div class="empty-icon">
            📦
          </div>
          <p>暂无匹配的纸样商品</p>
        </div>

        <div
          v-if="products.length > 0 && !loading"
          class="pagination-wrapper"
        >
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="totalCount"
            :page-sizes="[12, 24, 36]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handlePageSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { Search, Grid, List, Star } from '@element-plus/icons-vue';
import { getHotProducts, getCategories } from '@/api/product';
import type { Product, ProductCategory } from '@/types';

const router = useRouter();

const searchKeyword = ref('');
const selectedCategory = ref<number | null>(null);
const selectedStyles = ref<string[]>([]);
const minPrice = ref<number | null>(null);
const maxPrice = ref<number | null>(null);
const sortBy = ref('hot');
const viewMode = ref('grid');
const currentPage = ref(1);
const pageSize = ref(12);

const products = ref<Product[]>([]);
const categories = ref<ProductCategory[]>([]);
const loading = ref(true);
const totalCount = ref(0);

const styles = ['休闲', '商务', '运动', '优雅', '复古', '时尚'];

async function loadProducts() {
  loading.value = true;
  try {
    const data = await getHotProducts();
    products.value = data.slice((currentPage.value - 1) * pageSize.value, currentPage.value * pageSize.value);
    totalCount.value = data.length;
  } catch {
    products.value = [];
  } finally {
    loading.value = false;
  }
}

async function loadCategories() {
  try {
    categories.value = await getCategories({ showError: false });
  } catch {
    categories.value = [];
  }
}

function handleSearch() {
  currentPage.value = 1;
  loadProducts();
}

function toggleStyle(style: string) {
  const index = selectedStyles.value.indexOf(style);
  if (index > -1) {
    selectedStyles.value.splice(index, 1);
  } else {
    selectedStyles.value.push(style);
  }
}

function applyPriceFilter() {
  currentPage.value = 1;
  loadProducts();
}

function handleSort() {
  loadProducts();
}

function handlePageSizeChange(val: number) {
  pageSize.value = val;
  currentPage.value = 1;
  loadProducts();
}

function handlePageChange(val: number) {
  currentPage.value = val;
  loadProducts();
}

function goToDetail(id: number) {
  router.push(`/product/${id}`);
}

onMounted(async () => {
  await Promise.all([loadProducts(), loadCategories()]);
});

watch([selectedCategory, selectedStyles], () => {
  currentPage.value = 1;
  loadProducts();
});
</script>

<style scoped>
.patterns-view {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px 20px;
}

.view-header {
  text-align: center;
  margin-bottom: 32px;
}

.page-title {
  font-size: 32px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 8px 0;
}

.page-subtitle {
  font-size: 16px;
  color: #64748b;
  margin: 0;
}

.search-bar {
  margin-bottom: 24px;
}

.search-box {
  display: flex;
  max-width: 600px;
  margin: 0 auto;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
}

.search-icon {
  padding: 12px 16px;
  color: #94a3b8;
}

.search-box input {
  flex: 1;
  border: none;
  padding: 12px;
  font-size: 14px;
  outline: none;
}

.search-btn {
  background: #1d39c4;
  color: white;
  border: none;
  padding: 0 24px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
}

.content-container {
  display: flex;
  gap: 24px;
}

.sidebar {
  width: 240px;
  flex-shrink: 0;
}

.sidebar-section {
  background: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.sidebar-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 16px 0;
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f5f9;
}

.category-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.category-list li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  cursor: pointer;
  color: #475569;
  font-size: 14px;
  transition: color 0.2s;
}

.category-list li:hover,
.category-list li.active {
  color: #1d39c4;
}

.count {
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  color: #64748b;
}

.style-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.style-tags button {
  padding: 6px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  background: white;
  color: #475569;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.style-tags button:hover {
  border-color: #1d39c4;
  color: #1d39c4;
}

.style-tags button.active {
  background: #1d39c4;
  color: white;
  border-color: #1d39c4;
}

.price-range {
  display: flex;
  align-items: center;
  gap: 8px;
}

.price-range input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 13px;
  outline: none;
}

.price-separator {
  color: #94a3b8;
}

.filter-btn {
  background: #1d39c4;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
}

.main-content {
  flex: 1;
  min-width: 0;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.sort-options {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sort-label {
  font-size: 14px;
  color: #64748b;
}

.sort-options select {
  padding: 6px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
  cursor: pointer;
}

.view-mode {
  display: flex;
  gap: 4px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  overflow: hidden;
}

.view-mode button {
  padding: 8px 12px;
  border: none;
  background: white;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
}

.view-mode button:hover {
  background: #f8fafc;
}

.view-mode button.active {
  background: #1d39c4;
  color: white;
}

.patterns-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.patterns-grid.list {
  grid-template-columns: 1fr;
}

.pattern-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.pattern-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.pattern-image-wrapper {
  position: relative;
  padding-top: 100%;
  overflow: hidden;
}

.pattern-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.pattern-tags {
  position: absolute;
  top: 12px;
  left: 12px;
  display: flex;
  gap: 8px;
}

.tag {
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.preview-tag {
  background: rgba(29, 57, 196, 0.9);
  color: white;
}

.hot-tag {
  background: rgba(245, 34, 45, 0.9);
  color: white;
}

.pattern-info {
  padding: 16px;
}

.pattern-name {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 4px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pattern-subtitle {
  font-size: 13px;
  color: #64748b;
  margin: 0 0 12px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pattern-rating {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.star-rating {
  display: flex;
  gap: 2px;
}

.star {
  color: #e2e8f0;
}

.star.filled {
  color: #fbbf24;
}

.rating-text {
  font-size: 13px;
  color: #64748b;
}

.pattern-price {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.price {
  font-size: 20px;
  font-weight: 700;
  color: #f5222d;
}

.sales {
  font-size: 13px;
  color: #94a3b8;
}

.pattern-labels {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.label {
  padding: 3px 8px;
  background: #f1f5f9;
  color: #64748b;
  font-size: 11px;
  border-radius: 4px;
}

.loading-state {
  padding: 40px;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-state p {
  font-size: 16px;
  color: #64748b;
  margin: 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 40px;
}

@media (max-width: 1200px) {
  .patterns-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 900px) {
  .content-container {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
  }

  .patterns-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .patterns-grid {
    grid-template-columns: 1fr;
  }
}
</style>
