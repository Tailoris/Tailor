<template>
  <div class="register-view">
    <div class="register-card" role="main" aria-label="注册页面">
      <div class="register-header">
        <h2>注册账号</h2>
        <p>加入裁智云 Tailor IS</p>
      </div>

      <!-- Tab 切换 -->
      <div class="register-tabs" role="tablist" aria-label="注册方式选择">
        <div
          class="tab-item"
          :class="{ active: activeRegisterTab === 'phone' }"
          role="tab"
          :aria-selected="activeRegisterTab === 'phone'"
          tabindex="0"
          @click="switchRegisterTab('phone')"
          @keydown.enter="switchRegisterTab('phone')"
          @keydown.space.prevent="switchRegisterTab('phone')"
        >
          手机号注册
        </div>
        <div
          class="tab-item"
          :class="{ active: activeRegisterTab === 'email' }"
          role="tab"
          :aria-selected="activeRegisterTab === 'email'"
          tabindex="0"
          @click="switchRegisterTab('email')"
          @keydown.enter="switchRegisterTab('email')"
          @keydown.space.prevent="switchRegisterTab('email')"
        >
          邮箱注册
        </div>
      </div>

      <!-- 手机号注册表单 -->
      <el-form
        v-show="activeRegisterTab === 'phone'"
        :model="phoneForm"
        :rules="phoneRules"
        ref="phoneFormRef"
        label-position="top"
      >
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
              :disabled="phoneSending || phoneCountdown.counting.value || !canSendPhoneCode"
              @click="handleSendPhoneCode"
            >
              {{ phoneCountdown.getButtonText() }}
            </el-button>
          </div>
        </el-form-item>

        <el-form-item label="验证码" prop="code">
          <el-input
            v-model="phoneForm.code"
            placeholder="请输入短信验证码"
            size="large"
            :prefix-icon="Key"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="phoneForm.password"
            type="password"
            placeholder="请设置密码（不少于6位）"
            size="large"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="phoneForm.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            size="large"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item prop="agreement">
          <el-checkbox v-model="isAgree">
            我已阅读并同意 <el-link type="primary" :underline="false" @click.stop="showAgreement('user')">《用户协议》</el-link> 和 <el-link type="primary" :underline="false" @click.stop="showAgreement('privacy')">《隐私政策》</el-link>
          </el-checkbox>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="register-btn"
            @click="handlePhoneRegister"
            :loading="phoneLoading"
            :disabled="!isAgree || isPhoneSubmitting"
          >
            注册
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 邮箱注册表单 -->
      <el-form
        v-show="activeRegisterTab === 'email'"
        :model="emailForm"
        :rules="emailRules"
        ref="emailFormRef"
        label-position="top"
      >
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
              :disabled="emailSending || emailCountdown.counting.value || !canSendEmailCode"
              @click="handleSendEmailCode"
            >
              {{ emailCountdown.getButtonText() }}
            </el-button>
          </div>
        </el-form-item>

        <el-form-item label="验证码" prop="code">
          <el-input
            v-model="emailForm.code"
            placeholder="请输入邮箱收到的6位验证码"
            size="large"
            :prefix-icon="Key"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="emailForm.password"
            type="password"
            placeholder="请设置密码（不少于6位）"
            size="large"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="emailForm.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            size="large"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item prop="agreement">
          <el-checkbox v-model="isAgree">
            我已阅读并同意 <el-link type="primary" :underline="false" @click.stop="showAgreement('user')">《用户协议》</el-link> 和 <el-link type="primary" :underline="false" @click.stop="showAgreement('privacy')">《隐私政策》</el-link>
          </el-checkbox>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="register-btn"
            @click="handleEmailRegister"
            :loading="emailLoading"
            :disabled="!isAgree || isEmailSubmitting"
          >
            注册
          </el-button>
        </el-form-item>
      </el-form>

      <div class="register-footer">
        <span>已有账号？</span>
        <router-link to="/login" class="login-link">立即登录</router-link>
      </div>
    </div>

    <!-- 协议弹窗 -->
    <el-dialog
      v-model="agreementVisible"
      :title="agreementTitle"
      width="600px"
      role="dialog"
      aria-modal="true"
      :aria-labelledby="'agreement-dialog-title'"
    >
      <template #header>
        <span :id="'agreement-dialog-title'">{{ agreementTitle }}</span>
      </template>
      <div class="agreement-content">
        <p v-if="agreementType === 'user'">
          《用户协议》内容展示区域...
        </p>
        <p v-else>
          《隐私政策》内容展示区域...
        </p>
      </div>
    </el-dialog>

    <!-- 切换确认弹窗 -->
    <el-dialog
      v-model="switchConfirmVisible"
      title="提示"
      width="400px"
      :close-on-click-modal="false"
      role="dialog"
      aria-modal="true"
      aria-labelledby="switch-dialog-title"
    >
      <template #header>
        <span id="switch-dialog-title">提示</span>
      </template>
      <p>切换注册方式将清空当前已填内容，是否继续？</p>
      <template #footer>
        <el-button @click="cancelSwitch">取消</el-button>
        <el-button type="primary" @click="confirmSwitch">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Phone, Key, Lock, Message } from '@element-plus/icons-vue'
import { register as apiRegister, registerByEmail, sendSmsCode, sendEmailCode } from '@/api/auth'
import { validate } from '@/utils/validate'
import { useCountdown } from '@/composables/useCountdown'

const router = useRouter()

// Tab 状态
const activeRegisterTab = ref<'phone' | 'email'>('phone')

// 协议勾选（跨 Tab 共享）
const isAgree = ref(false)

// 切换确认弹窗
const switchConfirmVisible = ref(false)
const pendingTab = ref<'phone' | 'email'>('phone')

// 协议弹窗
const agreementVisible = ref(false)
const agreementType = ref<'user' | 'privacy'>('user')
const agreementTitle = computed(() => agreementType.value === 'user' ? '用户协议' : '隐私政策')

function showAgreement(type: 'user' | 'privacy') {
  agreementType.value = type
  agreementVisible.value = true
}

// 手机号表单
const phoneForm = ref({
  phone: '',
  code: '',
  password: '',
  confirmPassword: '',
  agreement: false
})
const phoneFormRef = ref<FormInstance>()
const phoneLoading = ref(false)
const phoneSending = ref(false)
const isPhoneSubmitting = ref(false)

// 邮箱表单
const emailForm = ref({
  email: '',
  code: '',
  password: '',
  confirmPassword: '',
  agreement: false
})
const emailFormRef = ref<FormInstance>()
const emailLoading = ref(false)
const emailSending = ref(false)
const isEmailSubmitting = ref(false)

// 倒计时
const phoneCountdown = useCountdown(60)
const emailCountdown = useCountdown(60)

// 手机号校验规则
const phoneRules: FormRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入11位中国大陆手机号', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '请输入6位数字验证码', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请设置密码', trigger: 'blur' },
    { min: 6, message: '密码不少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== phoneForm.value.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  agreement: [
    {
      validator: (_rule, value, callback) => {
        if (!value) {
          callback(new Error('请先同意用户协议和隐私政策'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ]
}

// 邮箱校验规则
const emailRules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/, message: '邮箱格式不正确', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '请输入6位数字验证码', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请设置密码', trigger: 'blur' },
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
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== emailForm.value.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  agreement: [
    {
      validator: (_rule, value, callback) => {
        if (!value) {
          callback(new Error('请先同意用户协议和隐私政策'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ]
}

// 发送验证码按钮可用性
const canSendPhoneCode = computed(() => {
  return validate.isPhone(phoneForm.value.phone)
})

const canSendEmailCode = computed(() => {
  return validate.isEmail(emailForm.value.email)
})

// 切换注册 Tab
function switchRegisterTab(tab: 'phone' | 'email') {
  if (tab === activeRegisterTab.value) return

  // 检查当前 Tab 是否有输入内容
  const hasInput = activeRegisterTab.value === 'phone'
    ? phoneForm.value.phone || phoneForm.value.code || phoneForm.value.password
    : emailForm.value.email || emailForm.value.code || emailForm.value.password

  if (hasInput) {
    pendingTab.value = tab
    switchConfirmVisible.value = true
  } else {
    activeRegisterTab.value = tab
  }
}

function cancelSwitch() {
  switchConfirmVisible.value = false
}

function confirmSwitch() {
  // 清空表单和校验
  if (activeRegisterTab.value === 'phone') {
    phoneForm.value = { phone: '', code: '', password: '', confirmPassword: '', agreement: isAgree.value }
    phoneFormRef.value?.clearValidate()
  } else {
    emailForm.value = { email: '', code: '', password: '', confirmPassword: '', agreement: isAgree.value }
    emailFormRef.value?.clearValidate()
  }
  activeRegisterTab.value = pendingTab.value
  switchConfirmVisible.value = false
}

// 发送手机验证码
async function handleSendPhoneCode() {
  if (!validate.isPhone(phoneForm.value.phone)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  if (phoneSending.value || phoneCountdown.counting.value) return

  phoneSending.value = true
  try {
    await sendSmsCode(phoneForm.value.phone)
    ElMessage.success('验证码已发送')
    phoneCountdown.start()
  } catch {
    ElMessage.error('发送失败')
  } finally {
    phoneSending.value = false
  }
}

// 发送邮箱验证码
async function handleSendEmailCode() {
  if (!validate.isEmail(emailForm.value.email)) {
    ElMessage.warning('请输入正确的邮箱')
    return
  }
  if (emailSending.value || emailCountdown.counting.value) return

  emailSending.value = true
  try {
    await sendEmailCode(emailForm.value.email)
    ElMessage.success('验证码已发送')
    emailCountdown.start()
  } catch {
    ElMessage.error('发送失败')
  } finally {
    emailSending.value = false
  }
}

// 手机号注册
async function handlePhoneRegister() {
  if (!phoneFormRef.value) return

  // 防重复提交
  if (isPhoneSubmitting.value) return
  isPhoneSubmitting.value = true

  await phoneFormRef.value.validate(async (valid) => {
    if (valid) {
      if (!isAgree.value) {
        ElMessage.warning('请先同意用户协议和隐私政策')
        isPhoneSubmitting.value = false
        return
      }

      phoneLoading.value = true
      try {
        await apiRegister({
          phone: phoneForm.value.phone,
          code: phoneForm.value.code,
          password: phoneForm.value.password
        })
        ElMessage.success('注册成功，请登录')
        router.push('/login')
      } catch {
        ElMessage.error('注册失败，请检查信息后重试')
      } finally {
        phoneLoading.value = false
        isPhoneSubmitting.value = false
      }
    } else {
      isPhoneSubmitting.value = false
    }
  })
}

// 邮箱注册
async function handleEmailRegister() {
  if (!emailFormRef.value) return

  // 防重复提交
  if (isEmailSubmitting.value) return
  isEmailSubmitting.value = true

  await emailFormRef.value.validate(async (valid) => {
    if (valid) {
      if (!isAgree.value) {
        ElMessage.warning('请先同意用户协议和隐私政策')
        isEmailSubmitting.value = false
        return
      }

      emailLoading.value = true
      try {
        await registerByEmail({
          email: emailForm.value.email,
          code: emailForm.value.code,
          password: emailForm.value.password
        })
        ElMessage.success('注册成功，请登录')
        router.push('/login')
      } catch {
        ElMessage.error('注册失败，请检查信息后重试')
      } finally {
        emailLoading.value = false
        isEmailSubmitting.value = false
      }
    } else {
      isEmailSubmitting.value = false
    }
  })
}
</script>

<style scoped>
.register-view {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.register-card {
  width: 420px;
  background: #fff;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.2);
}
.register-header {
  text-align: center;
  margin-bottom: 32px;
}
.register-header h2 {
  font-size: 28px;
  color: #333;
  margin: 0 0 8px;
}
.register-header p {
  color: #999;
  margin: 0;
}

/* Tab 切换样式 */
.register-tabs {
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
  color: #409eff;
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
  background: #409eff;
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
.register-btn {
  width: 100%;
}
.register-footer {
  text-align: center;
  margin-top: 16px;
  font-size: 14px;
  color: #666;
}
.login-link {
  color: #1d39c4;
  text-decoration: none;
  margin-left: 8px;
}
.agreement-content {
  max-height: 400px;
  overflow-y: auto;
  line-height: 1.8;
  color: #666;
}
</style>
