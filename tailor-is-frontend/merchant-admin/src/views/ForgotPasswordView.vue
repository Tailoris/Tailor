<template>
  <div class="forgot-container">
    <div class="forgot-card">
      <div class="forgot-header">
        <div class="forgot-logo">
          <span class="logo-icon">T</span>
        </div>
        <h1 class="forgot-title">找回密码</h1>
        <p class="forgot-subtitle">通过手机或邮箱找回商家后台账号密码</p>
      </div>

      <!-- 渠道选择 Tab -->
      <div class="channel-tabs">
        <div
          class="tab-item"
          :class="{ active: activeTab === 'phone' }"
          @click="switchTab('phone')"
        >
          手机号找回
        </div>
        <div
          class="tab-item"
          :class="{ active: activeTab === 'email' }"
          @click="switchTab('email')"
        >
          邮箱找回
        </div>
      </div>

      <!-- 手机号找回 -->
      <el-form
        v-show="activeTab === 'phone'"
        :model="phoneForm"
        :rules="phoneRules"
        ref="phoneFormRef"
        label-position="top"
      >
        <el-form-item label="店铺名称" prop="shopName">
          <el-input
            v-model="phoneForm.shopName"
            placeholder="请输入店铺名称"
            size="large"
            :prefix-icon="Shop"
          />
        </el-form-item>

        <el-form-item label="手机号" prop="phone">
          <div class="phone-row">
            <el-input
              v-model="phoneForm.phone"
              placeholder="请输入11位中国大陆手机号"
              size="large"
              :prefix-icon="Phone"
            />
            <el-button
              size="large"
              :disabled="sending || phoneCountdown.counting.value || !canSendPhoneCode"
              @click="handleSendPhoneCode"
            >
              {{ phoneCountdown.getButtonText() }}
            </el-button>
          </div>
        </el-form-item>

        <el-form-item label="验证码" prop="code">
          <el-input
            v-model="phoneForm.code"
            placeholder="请输入6位验证码"
            size="large"
            :prefix-icon="Key"
          />
        </el-form-item>

        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="phoneForm.newPassword"
            type="password"
            placeholder="请设置新密码（8位以上，含数字+字母+特殊字符）"
            size="large"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="phoneForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            size="large"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handlePhoneReset"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="reset-btn"
            @click="handlePhoneReset"
            :loading="loading"
            :disabled="!canPhoneReset"
          >
            重置密码
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 邮箱找回 -->
      <el-form
        v-show="activeTab === 'email'"
        :model="emailForm"
        :rules="emailRules"
        ref="emailFormRef"
        label-position="top"
      >
        <el-form-item label="店铺名称" prop="shopName">
          <el-input
            v-model="emailForm.shopName"
            placeholder="请输入店铺名称"
            size="large"
            :prefix-icon="Shop"
          />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <div class="phone-row">
            <el-input
              v-model="emailForm.email"
              placeholder="请输入常用邮箱账号"
              size="large"
              :prefix-icon="Message"
            />
            <el-button
              size="large"
              :disabled="sending || emailCountdown.counting.value || !canSendEmailCode"
              @click="handleSendEmailCode"
            >
              {{ emailCountdown.getButtonText() }}
            </el-button>
          </div>
        </el-form-item>

        <el-form-item label="验证码" prop="code">
          <el-input
            v-model="emailForm.code"
            placeholder="请输入6位验证码"
            size="large"
            :prefix-icon="Key"
          />
        </el-form-item>

        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="emailForm.newPassword"
            type="password"
            placeholder="请设置新密码（8位以上，含数字+字母+特殊字符）"
            size="large"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="emailForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            size="large"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleEmailReset"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="reset-btn"
            @click="handleEmailReset"
            :loading="loading"
            :disabled="!canEmailReset"
          >
            重置密码
          </el-button>
        </el-form-item>
      </el-form>

      <div class="forgot-footer">
        <span>想起密码了？</span>
        <router-link to="/login" class="login-link">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Phone, Key, Lock, Message, Shop } from '@element-plus/icons-vue'
import { sendResetCode, resetPassword } from '@/api/auth'
import { validate } from '@/utils/validate'
import { useCountdown } from '@/composables/useCountdown'

const router = useRouter()

// Tab 状态
const activeTab = ref<'phone' | 'email'>('phone')

// 手机号表单
const phoneForm = ref({
  shopName: '',
  phone: '',
  code: '',
  newPassword: '',
  confirmPassword: ''
})
const phoneFormRef = ref<FormInstance>()

// 邮箱表单
const emailForm = ref({
  shopName: '',
  email: '',
  code: '',
  newPassword: '',
  confirmPassword: ''
})
const emailFormRef = ref<FormInstance>()

const loading = ref(false)
const sending = ref(false)

// 倒计时
const phoneCountdown = useCountdown(60)
const emailCountdown = useCountdown(60)

// 手机号校验规则
const phoneRules: FormRules = {
  shopName: [{ required: true, message: '请输入店铺名称', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入11位中国大陆手机号', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '请输入6位数字验证码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请设置新密码', trigger: 'blur' },
    { min: 8, message: '密码不少于8位', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (!/(?=.*\d)(?=.*[a-zA-Z])(?=.*[^a-zA-Z0-9])/.test(value)) {
          callback(new Error('密码需包含数字、字母和特殊字符'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== phoneForm.value.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 邮箱校验规则
const emailRules: FormRules = {
  shopName: [{ required: true, message: '请输入店铺名称', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/, message: '邮箱格式不正确', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '请输入6位数字验证码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请设置新密码', trigger: 'blur' },
    { min: 8, message: '密码不少于8位', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (!/(?=.*\d)(?=.*[a-zA-Z])(?=.*[^a-zA-Z0-9])/.test(value)) {
          callback(new Error('密码需包含数字、字母和特殊字符'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== emailForm.value.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 按钮可用性
const canSendPhoneCode = computed(() => validate.isPhone(phoneForm.value.phone))
const canSendEmailCode = computed(() => validate.isEmail(emailForm.value.email))
const canPhoneReset = computed(() => {
  return phoneForm.value.shopName.trim() !== '' &&
    validate.isPhone(phoneForm.value.phone) &&
    validate.isSixCode(phoneForm.value.code) &&
    phoneForm.value.newPassword.length >= 8 &&
    phoneForm.value.newPassword === phoneForm.value.confirmPassword
})
const canEmailReset = computed(() => {
  return emailForm.value.shopName.trim() !== '' &&
    validate.isEmail(emailForm.value.email) &&
    validate.isSixCode(emailForm.value.code) &&
    emailForm.value.newPassword.length >= 8 &&
    emailForm.value.newPassword === emailForm.value.confirmPassword
})

// 切换 Tab
function switchTab(tab: 'phone' | 'email') {
  activeTab.value = tab
  phoneFormRef.value?.clearValidate()
  emailFormRef.value?.clearValidate()
}

// 发送手机验证码
async function handleSendPhoneCode() {
  if (!validate.isPhone(phoneForm.value.phone)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  if (sending.value || phoneCountdown.counting.value) return
  sending.value = true
  try {
    await sendResetCode({ target: phoneForm.value.phone, type: 'phone' })
    ElMessage.success('验证码已发送')
    phoneCountdown.start()
  } catch {
    ElMessage.error('发送失败')
  } finally {
    sending.value = false
  }
}

// 发送邮箱验证码
async function handleSendEmailCode() {
  if (!validate.isEmail(emailForm.value.email)) {
    ElMessage.warning('请输入正确的邮箱')
    return
  }
  if (sending.value || emailCountdown.counting.value) return
  sending.value = true
  try {
    await sendResetCode({ target: emailForm.value.email, type: 'email' })
    ElMessage.success('验证码已发送')
    emailCountdown.start()
  } catch {
    ElMessage.error('发送失败')
  } finally {
    sending.value = false
  }
}

// 手机号重置密码
async function handlePhoneReset() {
  if (!phoneFormRef.value) return

  await phoneFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await resetPassword({
          shopName: phoneForm.value.shopName,
          target: phoneForm.value.phone,
          code: phoneForm.value.code,
          newPassword: phoneForm.value.newPassword,
          type: 'phone'
        })
        ElMessage.success('密码重置成功，请登录')
        router.push('/login')
      } catch {
        ElMessage.error('重置失败，请检查信息后重试')
      } finally {
        loading.value = false
      }
    }
  })
}

// 邮箱重置密码
async function handleEmailReset() {
  if (!emailFormRef.value) return

  await emailFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await resetPassword({
          shopName: emailForm.value.shopName,
          target: emailForm.value.email,
          code: emailForm.value.code,
          newPassword: emailForm.value.newPassword,
          type: 'email'
        })
        ElMessage.success('密码重置成功，请登录')
        router.push('/login')
      } catch {
        ElMessage.error('重置失败，请检查信息后重试')
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.forgot-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #6366F1 0%, #4F46E5 50%, #3730A3 100%);
  padding: 20px;
}

.forgot-card {
  width: 100%;
  max-width: 420px;
  background: var(--color-white);
  border-radius: 16px;
  padding: 48px 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.forgot-header {
  text-align: center;
  margin-bottom: 32px;
}

.forgot-logo {
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

.forgot-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 8px;
}

.forgot-subtitle {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0;
}

/* Tab 切换样式 */
.channel-tabs {
  display: flex;
  border-bottom: 2px solid #e8e8e8;
  margin-bottom: 24px;
}
.tab-item {
  flex: 1;
  text-align: center;
  padding: 12px 0;
  font-size: 16px;
  color: #999;
  cursor: pointer;
  position: relative;
  transition: color 0.3s;
}
.tab-item.active {
  color: #6366F1;
  font-weight: 600;
}
.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 50%;
  transform: translateX(-50%);
  width: 60px;
  height: 2px;
  background: #6366F1;
}
.tab-item:hover:not(.active) {
  color: #666;
}

.phone-row {
  display: flex;
  gap: 12px;
}
.phone-row .el-input {
  flex: 1;
}
.reset-btn {
  width: 100%;
  border-radius: 8px;
  font-size: 16px;
  padding: 12px;
}
.forgot-footer {
  text-align: center;
  margin-top: 16px;
  font-size: 14px;
  color: #666;
}
.login-link {
  color: #6366F1;
  text-decoration: none;
  margin-left: 8px;
}
</style>
