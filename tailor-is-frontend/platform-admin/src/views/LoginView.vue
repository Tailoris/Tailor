<template>
  <div class="login-view">
    <div class="login-card">
      <div class="login-header">
        <h2>欢迎登录</h2>
        <p>裁智云平台管理</p>
      </div>

      <el-tabs v-model="activeTab" class="login-tabs">
        <!-- 密码登录 Tab -->
        <el-tab-pane label="密码登录" name="password">
          <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef">
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
                placeholder="密码"
                size="large"
                :prefix-icon="Lock"
                show-password
                @keyup.enter="handlePasswordLogin"
              />
            </el-form-item>
            <el-form-item>
              <div class="remember-row">
                <el-checkbox v-model="rememberMe">记住我</el-checkbox>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                size="large"
                class="login-btn"
                @click="handlePasswordLogin"
                :loading="loading"
                :disabled="!canPasswordLogin"
              >
                登录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 短信登录 Tab -->
        <el-tab-pane label="短信登录" name="code">
          <el-form :model="codeForm" :rules="codeRules" ref="codeFormRef">
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
                  placeholder="验证码"
                  size="large"
                  :prefix-icon="Key"
                  @keyup.enter="handleCodeLogin"
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
                @click="handleCodeLogin"
                :loading="loading"
                :disabled="!canCodeLogin"
              >
                登录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { User, Lock, Phone, Key } from '@element-plus/icons-vue'
import { login as apiLogin, sendVerificationCode, loginByCode } from '@/api/auth'
import { validate } from '@/utils/validate'
import { storage } from '@/utils/storage'
import { useCountdown } from '@/composables/useCountdown'

const router = useRouter()

const activeTab = ref('password')
const loading = ref(false)
const rememberMe = ref(false)

// 密码登录表单
const passwordForm = ref({
  account: '',
  password: ''
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
    username: ' 用户名',
    unknown: ''
  }
  return map[accountType.value] || ''
})

const passwordRules: FormRules = {
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
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const canPasswordLogin = computed(() => {
  return passwordForm.value.account.trim() !== '' && passwordForm.value.password.trim() !== ''
})

// 短信登录表单
const codeForm = ref({
  account: '',
  code: ''
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

const codeRules: FormRules = {
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

const canCodeLogin = computed(() => {
  const accountOk = codeAccountType.value === 'phone' || codeAccountType.value === 'email'
  return accountOk && validate.isSixCode(codeForm.value.code)
})

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

watch(activeTab, () => {
  passwordFormRef.value?.clearValidate()
  codeFormRef.value?.clearValidate()
})

async function handlePasswordLogin() {
  if (!passwordFormRef.value) return
  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const res = await apiLogin({
          username: passwordForm.value.account,
          password: passwordForm.value.password
        })
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
    await sendVerificationCode(codeForm.value.account, type)
    ElMessage.success('验证码已发送')
    countdown.start()
  } catch {
    ElMessage.error('发送失败')
  } finally {
    sending.value = false
  }
}

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
        await loginByCode({
          target: codeForm.value.account,
          code: codeForm.value.code,
          type
        })
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
.login-view {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 420px;
  background: #fff;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.2);
}
.login-header {
  text-align: center;
  margin-bottom: 32px;
}
.login-header h2 {
  font-size: 28px;
  color: #333;
  margin: 0 0 8px;
}
.login-header p {
  color: #999;
  margin: 0;
}
.login-tabs {
  margin-bottom: 16px;
}
.remember-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.login-btn {
  width: 100%;
}
.sms-row {
  display: flex;
  gap: 12px;
}
.sms-row .el-input {
  flex: 1;
}
.account-type-hint {
  display: block;
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
</style>
