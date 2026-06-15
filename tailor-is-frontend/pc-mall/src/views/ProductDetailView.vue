<template>
  <div class="product-detail-view" v-loading="loading" role="main" aria-label="商品详情">
    <template v-if="product">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/products' }">商品列表</el-breadcrumb-item>
        <el-breadcrumb-item>{{ product.name }}</el-breadcrumb-item>
      </el-breadcrumb>

      <div class="product-main">
        <div class="gallery-section" aria-label="商品图片画廊">
          <el-carousel :autoplay="false" height="480px">
            <el-carousel-item v-for="(img, index) in (product.images.length > 0 ? product.images : [product.mainImage])" :key="index">
              <img :src="img || 'https://via.placeholder.com/480x480'" :alt="`${product.name} 图片${index + 1}`" />
            </el-carousel-item>
          </el-carousel>
          <div class="thumbnail-list" role="tablist" aria-label="商品图片缩略图">
            <img
              v-for="(img, index) in (product.images.length > 0 ? product.images : [product.mainImage])"
              :key="index"
              :src="img"
              :class="{ active: currentImage === index }"
              @click="currentImage = index"
              :alt="`${product.name} 缩略图${index + 1}`"
              role="tab"
              :aria-selected="currentImage === index"
              :aria-label="`查看第${index + 1}张图片`"
              tabindex="0"
            />
          </div>
        </div>

        <div class="info-section">
          <h1 class="product-name">{{ product.name }}</h1>
          <p class="product-subtitle">{{ product.subTitle }}</p>
          <div class="price-section">
            <span class="current-price">{{ formatPrice(product.price) }}</span>
            <span v-if="product.price > 0" class="original-price">{{ formatPrice(product.price * ORIGINAL_PRICE_MULTIPLIER) }}</span>
          </div>
          <div class="meta-info">
            <div class="meta-item">
              <span class="label">销量</span>
              <span class="value">{{ product.saleCount }}</span>
            </div>
            <div class="meta-item">
              <span class="label">浏览</span>
              <span class="value">{{ product.viewCount }}</span>
            </div>
            <div class="meta-item">
              <span class="label">类型</span>
              <span class="value">{{ formatProductType(product.productType) }}</span>
            </div>
          </div>

          <div v-if="product.skus && product.skus.length > 0" class="sku-section" aria-label="选择商品规格">
            <template v-for="(attrValues, attrName) in skuAttributes" :key="attrName">
              <div class="sku-group">
                <span class="group-label">{{ attrName }}</span>
                <div class="sku-options" role="radiogroup" :aria-label="`选择${attrName}`">
                  <button
                    v-for="value in attrValues"
                    :key="value"
                    :class="{ active: selectedAttrs[attrName] === value }"
                    @click="selectAttr(attrName, value)"
                    role="radio"
                    :aria-checked="selectedAttrs[attrName] === value"
                    :aria-label="`${attrName}: ${value}`"
                  >
                    {{ value }}
                  </button>
                </div>
              </div>
            </template>
          </div>

          <div class="quantity-section" aria-label="选择购买数量">
            <span class="label">数量</span>
            <el-input-number v-model="quantity" :min="1" :max="MAX_PURCHASE_QUANTITY" aria-label="购买数量" />
          </div>

          <div class="action-section" role="toolbar" aria-label="商品操作">
            <el-button type="primary" size="large" class="add-cart-btn" @click="handleAddToCart" aria-label="加入购物车">
              加入购物车
            </el-button>
            <el-button type="danger" size="large" class="buy-now-btn" @click="handleBuyNow" aria-label="立即购买">
              立即购买
            </el-button>
          </div>
        </div>
      </div>

      <div class="tabs-section" aria-label="商品详情标签页">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="商品详情" name="detail">
            <div class="detail-content" v-html="DOMPurify.sanitize(product.description)" role="article" aria-label="商品详细描述"></div>
          </el-tab-pane>
          <el-tab-pane label="规格参数" name="specs">
            <el-table :data="product.skus || []" border>
              <el-table-column prop="skuCode" label="SKU编码" />
              <el-table-column label="属性">
                <template #default="{ row }">
                  {{ Object.entries(row.attributes).map(([k, v]) => `${k}: ${v}`).join(', ') }}
                </template>
              </el-table-column>
              <el-table-column prop="price" label="价格">
                <template #default="{ row }">{{ formatPrice(row.price) }}</template>
              </el-table-column>
              <el-table-column prop="stock" label="库存" />
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="用户评价" name="reviews">
            <div v-loading="reviewsLoading" class="reviews-section">
              <template v-if="reviews.length > 0">
                <div class="reviews-summary">
                  <div class="rating-overview">
                    <span class="rating-number">{{ averageRating.toFixed(1) }}</span>
                    <div class="rating-stars">
                      <span v-for="i in 5" :key="i" :class="['star', { active: i <= averageRating }]">★</span>
                    </div>
                    <span class="review-count">共 {{ totalReviews }} 条评价</span>
                  </div>
                </div>
                <div class="review-list">
                  <div v-for="review in reviews" :key="review.id" class="review-item">
                    <div class="review-header">
                      <span class="reviewer-name">{{ review.isAnonymous === 1 ? '匿名用户' : ('user' + review.userId) }}</span>
                      <span class="review-sku" v-if="review.skuSpec">{{ review.skuSpec }}</span>
                      <div class="review-rating">
                        <span v-for="i in 5" :key="i" :class="['star', { active: i <= review.rating }]">★</span>
                      </div>
                    </div>
                    <p class="review-content">{{ review.content }}</p>
                    <div v-if="review.images && review.images.length > 0" class="review-images">
                      <img v-for="(img, idx) in review.images" :key="idx" :src="img" class="review-img" :alt="`评价图片${idx + 1}`" />
                    </div>
                    <div class="review-footer">
                      <span class="review-date">{{ review.createdAt }}</span>
                    </div>
                    <div v-if="review.merchantReply" class="merchant-reply">
                      <strong>商家回复：</strong>{{ review.merchantReply }}
                    </div>
                  </div>
                </div>
                <div class="review-pagination">
                  <el-pagination
                    v-model:current-page="reviewPage"
                    :page-size="reviewPageSize"
                    :total="totalReviews"
                    layout="prev, pager, next"
                    @current-change="loadReviews"
                  />
                </div>
              </template>
              <el-empty v-else-if="!reviewsLoading" description="暂无评价" />
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import DOMPurify from 'dompurify'
import { getProductDetail } from '@/api/product'
import { getProductReviews } from '@/api/review'
import { useProductGraphQL, PRODUCT_DETAIL_QUERY, graphqlQuery } from '@/api/graphql'
import { useCartStore } from '@/store/cart'
import { formatPrice, formatProductType } from '@/utils/format'
import type { Product, Sku, ProductReview } from '@/types'

// Constants
const ORIGINAL_PRICE_MULTIPLIER = 1.2
const MAX_PURCHASE_QUANTITY = 99

const props = defineProps<{
  id: string
}>()

const router = useRouter()
const cartStore = useCartStore()

// GraphQL composable
const { product: graphqlProduct, loading: graphqlLoading, fetch: fetchGraphQL } = useProductGraphQL(Number(props.id))

// Fallback to REST if GraphQL fails
const product = ref<Product | null>(null)
const loading = ref(true)
const currentImage = ref(0)
const quantity = ref(1)
const selectedAttrs = ref<Record<string, string>>({})
const activeTab = ref('detail')

// Review state
const reviews = ref<ProductReview[]>([])
const reviewsLoading = ref(false)
const reviewPage = ref(1)
const reviewPageSize = 10
const totalReviews = ref(0)
const reviewsLoaded = ref(false)

/**
 * Normalize SKU attributes from both REST and GraphQL formats.
 * Extracted to avoid duplicate parsing logic (M-015).
 */
function normalizeSkuAttributes(sku: Sku): Record<string, string> {
  const attrs = sku.attributes || {}
  return 'key' in attrs
    ? (attrs as Array<{ key: string; value: string }>).reduce(
        (acc, a) => ({ ...acc, [a.key]: a.value }),
        {} as Record<string, string>
      )
    : (attrs as Record<string, string>)
}

const skuAttributes = computed(() => {
  const prod = graphqlProduct.value || product.value
  if (!prod?.skus) return {}
  const attrs: Record<string, Set<string>> = {}
  prod.skus.forEach((sku) => {
    const rawAttrs = normalizeSkuAttributes(sku)
    Object.entries(rawAttrs).forEach(([key, value]) => {
      if (!attrs[key]) attrs[key] = new Set()
      attrs[key].add(value)
    })
  })
  const result: Record<string, string[]> = {}
  Object.entries(attrs).forEach(([key, values]) => {
    result[key] = Array.from(values)
  })
  return result
})

function selectAttr(attrName: string, value: string) {
  selectedAttrs.value[attrName] = value
}

function getSelectedSku() {
  const prod = graphqlProduct.value || product.value
  return prod?.skus?.find((sku) => {
    const rawAttrs = normalizeSkuAttributes(sku)
    return Object.entries(selectedAttrs.value).every(([key, value]) => rawAttrs[key] === value)
  })
}

const averageRating = computed(() => {
  if (reviews.value.length === 0) return 0
  const sum = reviews.value.reduce((acc, r) => acc + r.rating, 0)
  return sum / reviews.value.length
})

async function loadReviews() {
  if (!props.id) return
  reviewsLoading.value = true
  try {
    const res = await getProductReviews(Number(props.id), {
      pageNum: reviewPage.value,
      pageSize: reviewPageSize
    })
    reviews.value = res.records || []
    totalReviews.value = res.total || 0
  } catch {
    reviews.value = []
  } finally {
    reviewsLoading.value = false
    reviewsLoaded.value = true
  }
}

watch(activeTab, (tab) => {
  if (tab === 'reviews' && !reviewsLoaded.value) {
    loadReviews()
  }
})

async function loadProduct() {
  try {
    product.value = await getProductDetail(Number(props.id))
    if (product.value?.skus && product.value.skus.length > 0) {
      const firstSku = product.value.skus[0]
      const rawAttrs = 'key' in (firstSku.attributes || {})
        ? (firstSku.attributes as Array<{ key: string; value: string }>).reduce((acc, a) => ({ ...acc, [a.key]: a.value }), {} as Record<string, string>)
        : firstSku.attributes as Record<string, string>
      Object.entries(rawAttrs).forEach(([key, value]) => {
        selectedAttrs.value[key] = value
      })
    }
  } catch {
    product.value = null
  } finally {
    loading.value = false
  }
}

async function handleAddToCart() {
  const sku = getSelectedSku()
  const prod = graphqlProduct.value || product.value
  if (!sku || !prod) {
    ElMessage.warning('请选择规格')
    return
  }
  try {
    await cartStore.addItem({
      productId: prod.id,
      skuId: sku.id,
      quantity: quantity.value
    })
    ElMessage.success('已加入购物车')
  } catch {
    ElMessage.error('加入购物车失败')
  }
}

function handleBuyNow() {
  const sku = getSelectedSku()
  const prod = graphqlProduct.value || product.value
  if (!sku || !prod) {
    ElMessage.warning('请选择规格')
    return
  }
  router.push({
    path: '/checkout',
    query: { productId: String(prod.id), skuId: sku.id, quantity: quantity.value }
  })
}

onMounted(async () => {
  // Try GraphQL first for better performance (single request for all data)
  await fetchGraphQL()
  if (!graphqlProduct.value) {
    // Fallback to REST API
    await loadProduct()
  }
})
</script>

<style scoped>
.product-detail-view {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  min-height: 400px;
}
.el-breadcrumb {
  margin-bottom: 20px;
}
.product-main {
  display: flex;
  gap: 32px;
  background: #fff;
  padding: 24px;
  border-radius: 8px;
}
.gallery-section {
  width: 480px;
  flex-shrink: 0;
}
.gallery-section img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.thumbnail-list {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
.thumbnail-list img {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
  cursor: pointer;
  border: 2px solid transparent;
}
.thumbnail-list img.active {
  border-color: #1d39c4;
}
.info-section {
  flex: 1;
}
.product-name {
  font-size: 24px;
  color: #333;
  margin: 0 0 8px;
}
.product-subtitle {
  color: #666;
  font-size: 14px;
  margin: 0 0 16px;
}
.price-section {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 20px;
  padding: 16px;
  background: #fff1f0;
  border-radius: 8px;
}
.current-price {
  font-size: 32px;
  color: #f5222d;
  font-weight: 700;
}
.original-price {
  color: #999;
  text-decoration: line-through;
  font-size: 16px;
}
.meta-info {
  display: flex;
  gap: 24px;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}
.meta-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.meta-item .label {
  font-size: 12px;
  color: #999;
}
.meta-item .value {
  font-size: 16px;
  color: #333;
  font-weight: 500;
}
.sku-section {
  margin-bottom: 20px;
}
.sku-group {
  margin-bottom: 12px;
}
.group-label {
  display: block;
  font-size: 14px;
  color: #333;
  margin-bottom: 8px;
}
.sku-options {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.sku-options button {
  padding: 6px 16px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}
.sku-options button:hover,
.sku-options button.active {
  border-color: #1d39c4;
  color: #1d39c4;
  background: #e6f7ff;
}
.quantity-section {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}
.quantity-section .label {
  font-size: 14px;
  color: #333;
}
.action-section {
  display: flex;
  gap: 16px;
}
.add-cart-btn,
.buy-now-btn {
  flex: 1;
}
.tabs-section {
  margin-top: 24px;
  background: #fff;
  padding: 24px;
  border-radius: 8px;
}
.detail-content {
  line-height: 1.8;
  color: #333;
}

/* Review section styles */
.reviews-section {
  min-height: 100px;
}
.reviews-summary {
  padding-bottom: 20px;
  margin-bottom: 20px;
  border-bottom: 1px solid #eee;
}
.rating-overview {
  display: flex;
  align-items: center;
  gap: 16px;
}
.rating-number {
  font-size: 40px;
  font-weight: 700;
  color: #ff9900;
}
.rating-stars {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.star {
  font-size: 16px;
  color: #ddd;
}
.star.active {
  color: #ff9900;
}
.review-count {
  font-size: 14px;
  color: #666;
}
.review-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.review-item {
  padding: 16px 0;
  border-bottom: 1px solid #f5f5f5;
}
.review-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.reviewer-name {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}
.review-sku {
  font-size: 12px;
  color: #999;
  background: #f5f5f5;
  padding: 2px 8px;
  border-radius: 4px;
}
.review-rating {
  display: flex;
  gap: 2px;
  margin-left: auto;
}
.review-content {
  font-size: 14px;
  color: #333;
  line-height: 1.6;
  margin: 0 0 8px;
}
.review-images {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}
.review-img {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
}
.review-footer {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #999;
}
.merchant-reply {
  margin-top: 12px;
  padding: 12px;
  background: #f9f9f9;
  border-radius: 8px;
  font-size: 14px;
  color: #666;
}
.review-pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

/* Responsive: Tablet (≤1024px) */
@media (max-width: 1024px) {
  .gallery-section {
    width: 360px;
  }
}

/* Responsive: Mobile (≤768px) */
@media (max-width: 768px) {
  .product-main {
    flex-direction: column;
    padding: 16px;
    gap: 20px;
  }
  .gallery-section {
    width: 100%;
  }
  .price-section {
    flex-direction: column;
    gap: 8px;
  }
  .meta-info {
    flex-wrap: wrap;
    gap: 16px;
  }
  .action-section {
    flex-direction: column;
  }
  .add-cart-btn,
  .buy-now-btn {
    width: 100%;
  }
}
</style>
