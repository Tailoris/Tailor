<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <div class="login-logo">
          <span class="logo-icon">T</span>
        </div>
        <h1 class="login-title">Tailor IS 商家后台</h1>
        <p class="login-subtitle">请登录您的商家账号</p>
      </div>

      <el-tabs v-model="activeTab" class="login-tabs">
        <!-- 密码登录 Tab -->
        <el-tab-pane label="密码登录" name="password">
          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            class="login-form"
            @keyup.enter="handlePasswordLogin"
          >
            <el-form-item prop="shopName">
              <el-input
                v-model="passwordForm.shopName"
                placeholder="请输入店铺名称"
                size="large"
                :prefix-icon="Shop"
              />
            </el-form-item>
            <el-form-item prop="account">
              <el-input
                v-model="passwordForm.account"
                placeholder="用户名 / 手机号 / 邮箱"
                size="large"
                :prefix-icon="User"
                @input="onAccountInput"
              />
              <span v-if="accountType" class="account-type-hint">
                {{ accountTypeHint }}
              </span>
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="passwordForm.password"
                type="password"
                placeholder="请输入密码"
                size="large"
                :prefix-icon="Lock"
                show-password
              />
            </el-form-item>
            <el-form-item>
              <div class="remember-row">
                <el-checkbox v-model="rememberMe">记住我</el-checkbox>
                <router-link to="/forgot-password" class="forgot-link">忘记密码？</router-link>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                size="large"
                class="login-btn"
                :loading="loading"
                :disabled="!canPasswordLogin"
                @click="handlePasswordLogin"
              >
                登 录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 短信登录 Tab -->
        <el-tab-pane label="验证码登录" name="code">
          <el-form
            ref="codeFormRef"
            :model="codeForm"
            :rules="codeRules"
            class="login-form"
            @keyup.enter="handleCodeLogin"
          >
            <el-form-item prop="shopName">
              <el-input
                v-model="codeForm.shopName"
                placeholder="请输入店铺名称"
                size="large"
                :prefix-icon="Shop"
              />
            </el-form-item>
            <el-form-item prop="account">
              <el-input
                v-model="codeForm.account"
                placeholder="请输入已注册手机号/邮箱"
                size="large"
                :prefix-icon="Phone"
                @input="onCodeAccountInput"
              />
              <span v-if="codeAccountType" class="account-type-hint">
                {{ codeAccountTypeHint }}
              </span>
            </el-form-item>
            <el-form-item prop="code">
              <div class="sms-row">
                <el-input
                  v-model="codeForm.code"
                  placeholder="请输入验证码"
                  size="large"
                  :prefix-icon="Key"
                />
                <el-button
                  size="large"
                  :disabled="sending || countdown.counting.value || !canSendCode"
                  @click="handleSendCode"
                >
                  {{ countdown.getButtonText() }}
                </el-button>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                size="large"
                class="login-btn"
                :loading="loading"
                :disabled="!canCodeLogin"
                @click="handleCodeLogin"
              >
                登 录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <div class="login-footer">
        <span>还没有账号？</span>
        <router-link to="/register" class="register-link">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { User, Lock, Phone, Key, Shop } from '@element-plus/icons-vue'
import { adminLogin, loginByCode, sendSmsCode, sendEmailCode } from '@/api/auth'
import { validate } from '@/utils/validate'
import { storage } from '@/utils/storage'
import { useCountdown } from '@/composables/useCountdown'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('password')
const loading = ref(false)
const rememberMe = ref(false)

// 密码登录表单
const passwordForm = ref({
  shopName: '',
  account: '',
  password: '',
})
const passwordFormRef = ref<FormInstance>()
const accountType = ref<'phone' | 'email' | 'username' | 'unknown'>('unknown')

function onAccountInput(val: string) {
  accountType.value = validate.identifyAccount(val)
}

const accountTypeHint = computed(() => {
  const map: Record<string, string> = {
    phone: '📱 手机号',
    email: '📧 邮箱',
    username: '👤 用户名',
    unknown: ''
  }
  return map[accountType.value] || ''
})

const canPasswordLogin = computed(() => {
  return passwordForm.value.shopName.trim() !== '' &&
         passwordForm.value.account.trim() !== '' &&
         passwordForm.value.password.trim() !== ''
})

const passwordRules: FormRules = {
  shopName: [{ required: true, message: '请输入店铺名称', trigger: 'blur' }],
  account: [
    { required: true, message: '请输入用户名/手机号/邮箱', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (!value) {
          callback(new Error('请输入用户名/手机号/邮箱'))
          return
        }
        const type = validate.identifyAccount(value)
        if (type === 'phone' && !validate.isPhone(value)) {
          callback(new Error('手机号格式不正确'))
          return
        }
        if (type === 'email' && !validate.isEmail(value)) {
          callback(new Error('邮箱格式不正确'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' },
  ],
}

// 短信登录表单
const codeForm = ref({
  shopName: '',
  account: '',
  code: '',
})
const codeFormRef = ref<FormInstance>()
const codeAccountType = ref<'phone' | 'email' | 'unknown'>('unknown')
const sending = ref(false)

function onCodeAccountInput(val: string) {
  const type = validate.identifyAccount(val)
  codeAccountType.value = type === 'phone' ? 'phone' : type === 'email' ? 'email' : 'unknown'
}

const codeAccountTypeHint = computed(() => {
  if (codeAccountType.value === 'phone') return '📱 将发送短信验证码'
  if (codeAccountType.value === 'email') return '📧 将发送邮箱验证码'
  return ''
})

const canSendCode = computed(() => {
  return codeAccountType.value === 'phone' || codeAccountType.value === 'email'
})

const canCodeLogin = computed(() => {
  const accountOk = codeAccountType.value === 'phone' || codeAccountType.value === 'email'
  return codeForm.value.shopName.trim() !== '' && accountOk && validate.isSixCode(codeForm.value.code)
})

const codeRules: FormRules = {
  shopName: [{ required: true, message: '请输入店铺名称', trigger: 'blur' }],
  account: [
    { required: true, message: '请输入手机号/邮箱', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (!value) {
          callback(new Error('请输入手机号/邮箱'))
          return
        }
        const type = validate.identifyAccount(value)
        if (type === 'phone' && !validate.isPhone(value)) {
          callback(new Error('手机号格式不正确'))
          return
        }
        if (type === 'email' && !validate.isEmail(value)) {
          callback(new Error('邮箱格式不正确'))
          return
        }
        if (type === 'username') {
          callback(new Error('请输入手机号或邮箱'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '请输入6位验证码', trigger: 'blur' }
  ]
}

// 倒计时
const countdown = useCountdown(60)

// 页面加载时回填记住的账号
onMounted(() => {
  const savedAccount = storage.getAccount()
  if (savedAccount) {
    passwordForm.value.account = savedAccount
    rememberMe.value = true
  }
})

// 切换Tab时清空
watch(activeTab, () => {
  passwordFormRef.value?.clearValidate()
  codeFormRef.value?.clearValidate()
})

// 密码登录
async function handlePasswordLogin() {
  if (!passwordFormRef.value) return
  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const res = await adminLogin({
          shopName: passwordForm.value.shopName,
          username: passwordForm.value.account,
          password: passwordForm.value.password,
        })
        userStore.setToken(res.token)
        userStore.setUserInfo(res.userInfo)
        if (rememberMe.value) {
          storage.saveAccount(passwordForm.value.account)
        } else {
          storage.clearAccount()
        }
        ElMessage.success('登录成功')
        router.push('/dashboard')
      } catch {
        ElMessage.error('账号或密码错误，请重新输入')
      } finally {
        loading.value = false
      }
    }
  })
}

// 发送验证码
async function handleSendCode() {
  if (!codeForm.value.account) {
    ElMessage.warning('请先输入手机号/邮箱')
    return
  }
  const type = validate.identifyAccount(codeForm.value.account)
  if (type !== 'phone' && type !== 'email') {
    ElMessage.warning('请输入正确的手机号或邮箱')
    return
  }
  if (sending.value || countdown.counting.value) return
  sending.value = true
  try {
    if (type === 'phone') {
      await sendSmsCode(codeForm.value.account)
    } else {
      await sendEmailCode(codeForm.value.account)
    }
    ElMessage.success('验证码已发送')
    countdown.start()
  } catch {
    ElMessage.error('发送失败')
  } finally {
    sending.value = false
  }
}

// 验证码登录
async function handleCodeLogin() {
  if (!codeFormRef.value) return
  await codeFormRef.value.validate(async (valid) => {
    if (valid) {
      const type = codeAccountType.value
      if (type !== 'phone' && type !== 'email') {
        ElMessage.warning('请输入正确的手机号或邮箱')
        return
      }
      loading.value = true
      try {
        const res = await loginByCode({
          target: codeForm.value.account,
          code: codeForm.value.code,
          type,
        })
        userStore.setToken(res.token)
        userStore.setUserInfo(res.userInfo)
        ElMessage.success('登录成功')
        router.push('/dashboard')
      } catch {
        ElMessage.error('验证码错误或已过期')
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #6366F1 0%, #4F46E5 50%, #3730A3 100%);
  padding: 20px;
}

.login-card {
  width: 100%;
  max-width: 420px;
  background: var(--color-white);
  border-radius: 16px;
  padding: 48px 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-logo {
  margin-bottom: 16px;
}

.logo-icon {
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #6366F1, #818CF8);
  color: white;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: bold;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
}

.login-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 8px;
}

.login-subtitle {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0;
}

.login-tabs {
  margin-bottom: 16px;
}

.login-form {
  margin-top: 24px;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 8px;
  padding: 12px 16px;
}

.remember-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.login-btn {
  width: 100%;
  border-radius: 8px;
  font-size: 16px;
  padding: 12px;
}

.sms-row {
  display: flex;
  gap: 12px;
}

.sms-row .el-input {
  flex: 1;
}

.login-footer {
  text-align: center;
  margin-top: 16px;
  font-size: 14px;
  color: #666;
}

.register-link {
  color: #6366F1;
  text-decoration: none;
  margin-left: 8px;
}

.forgot-link {
  color: #6366F1;
  text-decoration: none;
  font-size: 14px;
}

.forgot-link:hover {
  text-decoration: underline;
}

.account-type-hint {
  display: block;
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
</style>
