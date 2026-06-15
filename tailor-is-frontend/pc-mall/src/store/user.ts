import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as apiLogin, getUserInfo, logout as apiLogout } from '@/api/auth'
import { decryptSync, encryptSync } from '@/utils/crypto'
import type { User } from '@/types'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(decryptSync(localStorage.getItem('token') || '') || '')
  const userInfo = ref<User | null>(null)

  async function login(data: { username: string; password: string }) {
    const res = await apiLogin(data)
    token.value = res.token
    localStorage.setItem('token', encryptSync(res.token))
    userInfo.value = res.user
    return res
  }

  async function logout() {
    try {
      await apiLogout()
    } finally {
      token.value = ''
      userInfo.value = null
      localStorage.removeItem('token')
    }
  }

  function setToken(t: string) {
    token.value = t
    localStorage.setItem('token', encryptSync(t))
  }

  async function fetchUserInfo() {
    const res = await getUserInfo()
    userInfo.value = res
    return res
  }

  function updateUserInfo(info: Partial<User>) {
    if (userInfo.value) {
      Object.assign(userInfo.value, info)
    }
  }

  return { token, userInfo, login, logout, setToken, fetchUserInfo, updateUserInfo }
})
