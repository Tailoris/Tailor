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
  const currentShopId = ref<number | null>(null)

  const isLoggedIn = computed(() => !!token.value)
  const currentShop = computed(() => shopList.value.find(s => s.id === currentShopId.value))

  async function login(shopName: string, username: string, password: string) {
    const res = await adminLogin({ shopName, username, password })
    token.value = res.token
    userInfo.value = res.userInfo
    localStorage.setItem('token', encryptSync(res.token))
    await fetchShopList()
    if (shopList.value.length > 0 && !currentShopId.value) {
      currentShopId.value = shopList.value[0].id
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
    currentShopId.value = null
    localStorage.removeItem('token')
  }

  function switchShop(shopId: number) {
    currentShopId.value = shopId
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
