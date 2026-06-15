import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import {
  getCart as apiGetCart,
  addToCart as apiAddToCart,
  updateCart as apiUpdateCart,
  deleteCart as apiDeleteCart
} from '@/api/cart'
import type { CartItem } from '@/types'

// 🔒 F-H07: 购物车本地存储key
const CART_STORAGE_KEY = 'tailor_is_cart'

/**
 * 🔒 F-H07: 从localStorage恢复购物车数据
 */
function loadCartFromStorage(): CartItem[] {
  try {
    const stored = localStorage.getItem(CART_STORAGE_KEY)
    if (stored) {
      return JSON.parse(stored) as CartItem[]
    }
  } catch (e) {
    console.warn('购物车数据恢复失败:', e)
  }
  return []
}

/**
 * 🔒 F-H07: 保存购物车到localStorage
 */
function saveCartToStorage(items: CartItem[]): void {
  try {
    localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(items))
  } catch (e) {
    console.warn('购物车数据保存失败:', e)
  }
}

export const useCartStore = defineStore('cart', () => {
  // 🔒 F-H07: 初始值从localStorage恢复
  const items = ref<CartItem[]>(loadCartFromStorage())

  // 🔒 F-H07: 监听变化自动持久化
  watch(
    items,
    (newItems) => {
      saveCartToStorage(newItems)
    },
    { deep: true }
  )

  const totalCount = computed(() => items.value.reduce((sum, item) => sum + item.quantity, 0))

  const totalPrice = computed(() =>
    items.value
      .filter((item) => item.checked)
      .reduce((sum, item) => sum + item.price * item.quantity, 0)
  )

  const checkedCount = computed(() => items.value.filter((item) => item.checked).length)

  const isAllChecked = computed(() => items.value.length > 0 && checkedCount.value === items.value.length)

  async function fetchCart() {
    const res = await apiGetCart()
    items.value = res
    // 🔒 F-H07: 同步到本地存储
    saveCartToStorage(items.value)
  }

  async function addItem(data: { productId: number; skuId: number; quantity: number }) {
    await apiAddToCart(data)
    await fetchCart()
  }

  async function updateQuantity(id: number, quantity: number) {
    await apiUpdateCart(id, { quantity })
    await fetchCart()
  }

  async function removeFromCart(id: number) {
    await apiDeleteCart(id)
    await fetchCart()
  }

  async function toggleCheck(id: number) {
    const item = items.value.find((i) => i.id === id)
    if (item) {
      await apiUpdateCart(id, { checked: !item.checked })
      await fetchCart()
    }
  }

  async function toggleAll() {
    const target = !isAllChecked.value
    const promises = items.value.map((item) => apiUpdateCart(item.id, { checked: target }))
    await Promise.all(promises)
    await fetchCart()
  }

  async function clearCart() {
    items.value = []
    // 🔒 F-H07: 清空本地存储
    localStorage.removeItem(CART_STORAGE_KEY)
  }

  function checkout() {
    const checkedItems = items.value.filter((item) => item.checked)
    return checkedItems
  }

  return {
    items,
    totalCount,
    totalPrice,
    checkedCount,
    isAllChecked,
    fetchCart,
    addItem,
    updateQuantity,
    removeFromCart,
    toggleCheck,
    toggleAll,
    clearCart,
    checkout
  }
})
