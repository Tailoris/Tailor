<template>
  <div class="checkout-view" v-loading="loading" role="main" aria-label="结算页面">
    <!-- Error state (M-011) -->
    <div v-if="error" class="error-container">
      <el-result icon="error" :title="error" sub-title="请返回上一页重试">
        <template #extra>
          <el-button type="primary" @click="router.back()">返回</el-button>
        </template>
      </el-result>
    </div>

    <!-- Empty state (M-011) -->
    <el-empty v-else-if="!loading && orderItems.length === 0" description="订单商品为空">
      <el-button type="primary" @click="router.push('/products')">去逛逛</el-button>
    </el-empty>

    <template v-else>
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item :to="{ path: '/cart' }">购物车</el-breadcrumb-item>
      <el-breadcrumb-item>确认订单</el-breadcrumb-item>
    </el-breadcrumb>

    <h2 class="page-title">确认订单</h2>

    <div class="checkout-layout">
      <div class="checkout-main">
        <AddressSelector
          :addresses="addresses"
          v-model="selectedAddress"
          @add="showAddressDialog = true"
        />

        <OrderItems :items="orderItems" />

        <section class="section remark-section">
          <h3>订单备注</h3>
          <el-input v-model="remark" type="textarea" :rows="3" placeholder="请输入订单备注（选填）" />
        </section>
      </div>

      <div class="checkout-sidebar">
        <PriceSummary
          :subtotal="subtotal"
          :discount="discount"
          :shipping="shipping"
          :total="total"
          :submitting="submitting"
          @submit="handleSubmit"
        />
      </div>
    </div>

    <el-dialog v-model="showAddressDialog" title="新增收货地址" width="500px">
      <el-form :model="newAddress" :rules="addressRules" ref="addressFormRef" label-width="80px">
        <el-form-item label="收货人" prop="name">
          <el-input v-model="newAddress.name" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="newAddress.phone" />
        </el-form-item>
        <el-form-item label="所在地区">
          <el-row :gutter="8">
            <el-col :span="8">
              <el-input v-model="newAddress.province" placeholder="省" />
            </el-col>
            <el-col :span="8">
              <el-input v-model="newAddress.city" placeholder="市" />
            </el-col>
            <el-col :span="8">
              <el-input v-model="newAddress.district" placeholder="区" />
            </el-col>
          </el-row>
        </el-form-item>
        <el-form-item label="详细地址" prop="detail">
          <el-input v-model="newAddress.detail" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="默认地址">
          <el-switch v-model="newAddress.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddressDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAddAddress">确定</el-button>
      </template>
    </el-dialog>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createOrder } from '@/api/order'
import { getAddresses, createAddress } from '@/api/address'
import { checkoutCart } from '@/api/cart'
import { getProductDetail } from '@/api/product'
import AddressSelector from '@/components/AddressSelector.vue'
import OrderItems from '@/components/OrderItems.vue'
import PriceSummary from '@/components/PriceSummary.vue'
import type { Address, CartItem } from '@/types'

const router = useRouter()
const route = useRoute()

const loading = ref(true)
const error = ref('')
const submitting = ref(false)
const addresses = ref<Address[]>([])
const selectedAddress = ref<number>()
const orderItems = ref<CartItem[]>([])
const remark = ref('')
const showAddressDialog = ref(false)
const addressFormRef = ref<FormInstance>()

const newAddress = ref({
  name: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  isDefault: 0
})

const addressRules: FormRules = {
  name: [{ required: true, message: '请输入收货人', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  detail: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

const subtotal = computed(() =>
  orderItems.value.reduce((sum, item) => sum + item.price * item.quantity, 0)
)

const discount = ref(0)
const shipping = ref(0)
const total = computed(() => subtotal.value - discount.value + shipping.value)

async function loadAddresses() {
  try {
    addresses.value = await getAddresses()
    const defaultAddr = addresses.value.find((a) => a.isDefault === 1)
    selectedAddress.value = defaultAddr?.id || addresses.value[0]?.id
  } catch {
    addresses.value = []
  }
}

async function loadOrderItems() {
  const cartIds = route.query.cartIds as string | undefined
  const productIdStr = route.query.productId as string | undefined

  if (cartIds) {
    try {
      const ids = cartIds.split(',').map(Number)
      const res = await checkoutCart(ids)
      orderItems.value = res.items
    } catch {
      orderItems.value = []
    }
  } else if (productIdStr) {
    const productId = Number(productIdStr)
    if (isNaN(productId)) {
      ElMessage.error('商品ID无效')
      router.push('/')
      return
    }
    const skuIdStr = route.query.skuId as string | undefined
    const skuId = skuIdStr ? Number(skuIdStr) : undefined
    if (skuId !== undefined && isNaN(skuId)) {
      ElMessage.error('SKU ID无效')
      router.push('/')
      return
    }
    const quantityStr = route.query.quantity as string | undefined
    const quantity = quantityStr ? Number(quantityStr) : 1
    if (quantity < 1) {
      ElMessage.error('商品数量无效')
      router.push('/')
      return
    }
    try {
      const product = await getProductDetail(productId)
      const sku = skuId ? product.skus?.find((s) => s.id === skuId) : product.skus?.[0]
      if (sku) {
        orderItems.value = [{
          id: 0,
          userId: 0,
          productId: product.id,
          productName: product.name,
          productImage: product.mainImage,
          skuId: sku.id,
          skuAttributes: sku.attributes,
          quantity,
          price: sku.price,
          checked: true
        }]
      }
    } catch {
      ElMessage.error('获取商品信息失败')
      orderItems.value = []
    }
  }
}

async function handleAddAddress() {
  if (!addressFormRef.value) return
  await addressFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await createAddress(newAddress.value)
        showAddressDialog.value = false
        ElMessage.success('地址添加成功')
        await loadAddresses()
        newAddress.value = { name: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: 0 }
      } catch {
        ElMessage.error('添加地址失败')
      }
    }
  })
}

async function handleSubmit() {
  if (!selectedAddress.value) {
    ElMessage.warning('请选择收货地址')
    return
  }
  if (orderItems.value.length === 0) {
    ElMessage.warning('订单商品不能为空')
    return
  }
  submitting.value = true
  try {
    const res = await createOrder({
      items: orderItems.value.map((item) => ({
        productId: item.productId,
        skuId: item.skuId,
        quantity: item.quantity
      })),
      addressId: selectedAddress.value,
      remark: remark.value
    })
    ElMessage.success('订单创建成功')
    router.push(`/order/${res.orderNo}`)
  } catch {
    ElMessage.error('订单创建失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  loading.value = true
  await Promise.all([loadAddresses(), loadOrderItems()])
  loading.value = false
})
</script>

<style scoped>
.checkout-view {
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
.checkout-layout {
  display: flex;
  gap: 24px;
}
.checkout-main {
  flex: 1;
  min-width: 0;
}
.checkout-sidebar {
  width: 320px;
  flex-shrink: 0;
}
.section {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 16px;
}
.section h3 {
  font-size: 16px;
  color: #333;
  margin: 0 0 16px;
}
</style>
