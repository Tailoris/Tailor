import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { adminLogin, getAdminInfo, logout as logoutApi } from '@/api/auth'
import { getMerchantShops } from '@/api/shop'
import { decryptSync, encryptSync } from '@/utils/crypto'
import type { Merchant } from '@/types'

interface ShopItem {
  id: number
  name: string
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(decryptSync(localStorage.getItem('token') || '') || '')
  const userInfo = ref<Merchant | null>(null)
  const shopList = ref<ShopItem[]>([])
  const currentShopId = ref<number | null>(
    localStorage.getItem('currentShopId') ? Number(localStorage.getItem('currentShopId')) : null
  )

  const isLoggedIn = computed(() => !!token.value)
  const currentShop = computed(() => shopList.value.find(s => s.id === currentShopId.value))

  function persistCurrentShopId(id: number | null) {
    currentShopId.value = id
    if (id !== null) {
      localStorage.setItem('currentShopId', String(id))
    } else {
      localStorage.removeItem('currentShopId')
    }
  }

  async function login(shopName: string, username: string, password: string) {
    const res = await adminLogin({ shopName, username, password })
    token.value = res.token
    userInfo.value = res.userInfo
    localStorage.setItem('token', encryptSync(res.token))
    await fetchShopList()
    if (shopList.value.length > 0 && !currentShopId.value) {
      persistCurrentShopId(shopList.value[0].id)
    }
  }

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('token', encryptSync(newToken))
  }

  function setUserInfo(info: Merchant) {
    userInfo.value = info
  }

  async function fetchUserInfo() {
    if (!token.value) return
    try {
      userInfo.value = await getAdminInfo()
    } catch {
      token.value = ''
      localStorage.removeItem('token')
      // 会话已失效，跳转登录页（硬跳转以清空运行时状态）
      if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
  }

  async function fetchShopList() {
    try {
      shopList.value = await getMerchantShops()
    } catch {
      shopList.value = []
    }
  }

  async function logout() {
    try {
      await logoutApi()
    } catch {
      // ignore
    }
    token.value = ''
    userInfo.value = null
    shopList.value = []
    persistCurrentShopId(null)
    localStorage.removeItem('token')
  }

  function switchShop(shopId: number) {
    persistCurrentShopId(shopId)
  }

  return {
    token,
    userInfo,
    shopList,
    currentShopId,
    isLoggedIn,
    currentShop,
    login,
    setToken,
    setUserInfo,
    fetchUserInfo,
    fetchShopList,
    logout,
    switchShop,
  }
})
