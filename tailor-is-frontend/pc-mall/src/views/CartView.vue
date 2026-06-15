<template>
  <div class="cart-view">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>购物车</el-breadcrumb-item>
    </el-breadcrumb>

    <h2 class="page-title">购物车 ({{ cartStore.totalCount }})</h2>

    <el-skeleton :loading="loading" animated :rows="4">
      <template v-if="cartStore.items.length > 0">
        <div class="cart-header">
          <el-checkbox :model-value="cartStore.isAllChecked" @change="cartStore.toggleAll">全选</el-checkbox>
          <span class="col-product">商品信息</span>
          <span class="col-price">单价</span>
          <span class="col-quantity">数量</span>
          <span class="col-subtotal">小计</span>
          <span class="col-action">操作</span>
        </div>

        <div
          v-for="item in cartStore.items"
          :key="item.id"
          class="cart-item"
        >
          <el-checkbox :model-value="item.checked" @change="cartStore.toggleCheck(item.id)" />
          <div class="product-info">
            <img :src="item.productImage || 'https://via.placeholder.com/80x80'" :alt="item.productName" />
            <div class="product-text">
              <span class="product-name">{{ item.productName }}</span>
              <span class="sku-attrs">{{ Object.entries(item.skuAttributes).map(([k, v]) => `${k}: ${v}`).join('; ') }}</span>
            </div>
          </div>
          <span class="price">{{ formatPrice(item.price) }}</span>
          <el-input-number
            v-model="item.quantity"
            :min="1"
            :max="99"
            size="small"
            @change="(val: number) => cartStore.updateQuantity(item.id, val)"
          />
          <span class="subtotal">{{ formatPrice(item.price * item.quantity) }}</span>
          <el-button type="danger" link @click="cartStore.removeFromCart(item.id)">删除</el-button>
        </div>

        <div class="cart-footer">
          <div class="footer-left">
            <el-checkbox :model-value="cartStore.isAllChecked" @change="cartStore.toggleAll">全选</el-checkbox>
            <el-button type="danger" link @click="handleClear">清空购物车</el-button>
          </div>
          <div class="footer-right">
            <span class="total-info">
              已选 <em>{{ cartStore.checkedCount }}</em> 件商品，合计：
              <em class="total-price">{{ formatPrice(cartStore.totalPrice) }}</em>
            </span>
            <el-button
              type="primary"
              size="large"
              :disabled="cartStore.checkedCount === 0"
              @click="handleCheckout"
            >
              去结算
            </el-button>
          </div>
        </div>
      </template>
      <el-empty v-else-if="!loading" description="购物车是空的">
        <el-button type="primary" @click="$router.push('/products')">去逛逛</el-button>
      </el-empty>
    </el-skeleton>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useCartStore } from '@/store/cart'
import { formatPrice } from '@/utils/format'

const router = useRouter()
const cartStore = useCartStore()
const loading = ref(true)

async function loadCart() {
  loading.value = true
  try {
    await cartStore.fetchCart()
  } catch {
    ElMessage.error('加载购物车失败')
  } finally {
    loading.value = false
  }
}

async function handleClear() {
  try {
    await ElMessageBox.confirm('确定要清空购物车吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    cartStore.clearCart()
    ElMessage.success('已清空购物车')
  } catch {
    // cancelled
  }
}

function handleCheckout() {
  const checkedItems = cartStore.checkout()
  if (checkedItems.length === 0) {
    ElMessage.warning('请选择要结算的商品')
    return
  }
  const ids = checkedItems.map((item) => item.id)
  router.push({ path: '/checkout', query: { cartIds: ids.join(',') } })
}

onMounted(() => {
  loadCart()
})
</script>

<style scoped>
.cart-view {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}
.el-breadcrumb {
  margin-bottom: 20px;
}
.page-title {
  font-size: 24px;
  color: #333;
  margin: 0 0 20px;
}
.cart-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: #f5f5f5;
  border-radius: 8px 8px 0 0;
  font-size: 14px;
  color: #666;
}
.cart-item {
  display: flex;
  align-items: center;
  padding: 16px;
  background: #fff;
  border-bottom: 1px solid #eee;
  gap: 12px;
}
.cart-item:last-of-type {
  border-radius: 0 0 8px 8px;
}
.col-product { flex: 1; }
.col-price { width: 100px; text-align: center; }
.col-quantity { width: 120px; text-align: center; }
.col-subtotal { width: 100px; text-align: center; }
.col-action { width: 60px; text-align: center; }
.product-info {
  flex: 1;
  display: flex;
  gap: 12px;
  align-items: center;
}
.product-info img {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
}
.product-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.product-name {
  font-size: 14px;
  color: #333;
}
.sku-attrs {
  font-size: 12px;
  color: #999;
}
.price {
  width: 100px;
  text-align: center;
  font-size: 14px;
  color: #333;
}
.subtotal {
  width: 100px;
  text-align: center;
  font-size: 16px;
  color: #f5222d;
  font-weight: 600;
}
.cart-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  margin-top: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}
.footer-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.footer-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.total-info {
  font-size: 14px;
  color: #333;
}
.total-info em {
  color: #f5222d;
  font-style: normal;
  font-weight: 600;
}
.total-price {
  font-size: 20px;
}
</style>
