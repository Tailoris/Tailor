<template>
  <div class="settings-page" v-loading="loading">
    <div class="page-header">
      <h2 class="page-title">系统设置</h2>
      <p class="page-subtitle">管理平台全局配置参数</p>
    </div>

    <el-tabs v-model="activeTab" type="border-card" class="settings-tabs">
      <el-tab-pane label="基本设置" name="basic">
        <el-form
          ref="basicFormRef"
          :model="basicForm"
          :rules="basicRules"
          label-width="140px"
          class="settings-form"
        >
          <el-form-item label="平台名称" prop="platformName">
            <el-input v-model="basicForm.platformName" placeholder="请输入平台名称" />
          </el-form-item>
          <el-form-item label="平台Logo">
            <el-upload
              class="logo-uploader"
              action="#"
              :show-file-list="false"
              :auto-upload="false"
            >
              <el-icon v-if="!basicForm.logoUrl" class="upload-icon" :size="28"><Plus /></el-icon>
              <img v-else :src="basicForm.logoUrl" class="uploaded-logo" />
            </el-upload>
          </el-form-item>
          <el-form-item label="客服电话" prop="servicePhone">
            <el-input v-model="basicForm.servicePhone" placeholder="请输入客服电话" />
          </el-form-item>
          <el-form-item label="客服邮箱" prop="serviceEmail">
            <el-input v-model="basicForm.serviceEmail" placeholder="请输入客服邮箱" />
          </el-form-item>
          <el-form-item label="平台公告">
            <el-input
              v-model="basicForm.announcement"
              type="textarea"
              :rows="3"
              placeholder="请输入平台公告内容"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="handleSaveBasic">保存设置</el-button>
            <el-button @click="handleResetBasic">重置</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="安全设置" name="security">
        <el-form label-width="140px" class="settings-form">
          <el-form-item label="登录验证码">
            <el-switch v-model="securityForm.captchaEnabled" />
          </el-form-item>
          <el-form-item label="注册审核">
            <el-switch v-model="securityForm.registerAudit" />
          </el-form-item>
          <el-form-item label="短信验证">
            <el-switch v-model="securityForm.smsVerification" />
          </el-form-item>
          <el-form-item label="登录失败锁定">
            <el-input-number v-model="securityForm.loginFailLock" :min="3" :max="20" />
            <span class="form-tip">次失败后锁定账户</span>
          </el-form-item>
          <el-form-item label="会话超时">
            <el-input-number v-model="securityForm.sessionTimeout" :min="15" :max="480" :step="15" />
            <span class="form-tip">分钟无操作后自动退出</span>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="handleSaveSecurity">保存设置</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="第三方服务" name="thirdParty">
        <el-form label-width="140px" class="settings-form">
          <el-form-item label="阿里云OSS">
            <el-switch v-model="thirdPartyForm.ossEnabled" />
          </el-form-item>
          <el-form-item label="OSS Endpoint">
            <el-input v-model="thirdPartyForm.ossEndpoint" placeholder="oss-cn-hangzhou.aliyuncs.com" />
          </el-form-item>
          <el-form-item label="OSS Bucket">
            <el-input v-model="thirdPartyForm.ossBucket" placeholder="tailor-is" />
          </el-form-item>
          <el-form-item label="微信支付">
            <el-switch v-model="thirdPartyForm.wechatPayEnabled" />
          </el-form-item>
          <el-form-item label="支付宝支付">
            <el-switch v-model="thirdPartyForm.alipayEnabled" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="handleSaveThirdParty">保存设置</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { getSettings, saveBasicSettings, saveSecuritySettings, saveThirdPartySettings } from '@/api/settings'

const loading = ref(false)
const saving = ref(false)
const activeTab = ref('basic')
const basicFormRef = ref<FormInstance>()

const basicForm = reactive({
  platformName: '裁智云 - Tailor IS',
  logoUrl: '',
  servicePhone: '400-888-9999',
  serviceEmail: 'support@tailoris.com',
  announcement: '',
})

const basicRules: FormRules = {
  platformName: [
    { required: true, message: '请输入平台名称', trigger: 'blur' },
  ],
  servicePhone: [
    { required: true, message: '请输入客服电话', trigger: 'blur' },
  ],
  serviceEmail: [
    { required: true, message: '请输入客服邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
}

const securityForm = reactive({
  captchaEnabled: true,
  registerAudit: false,
  smsVerification: true,
  loginFailLock: 5,
  sessionTimeout: 120,
})

const thirdPartyForm = reactive({
  ossEnabled: true,
  ossEndpoint: 'oss-cn-hangzhou.aliyuncs.com',
  ossBucket: 'tailor-is',
  wechatPayEnabled: true,
  alipayEnabled: true,
})

async function handleSaveBasic() {
  if (!basicFormRef.value) return
  await basicFormRef.value.validate(async (valid) => {
    if (!valid) return
    // FE-M-6: 接入真实API，替代 setTimeout 假保存
    saving.value = true
    try {
      await saveBasicSettings({ ...basicForm })
      ElMessage.success('基本设置保存成功')
    } catch {
      // error handled by interceptor
    } finally {
      saving.value = false
    }
  })
}

function handleResetBasic() {
  basicFormRef.value?.resetFields()
}

async function handleSaveSecurity() {
  // FE-M-6: 接入真实API，替代 setTimeout 假保存
  saving.value = true
  try {
    await saveSecuritySettings({ ...securityForm })
    ElMessage.success('安全设置保存成功')
  } catch {
    // error handled by interceptor
  } finally {
    saving.value = false
  }
}

async function handleSaveThirdParty() {
  // FE-M-6: 接入真实API，替代 setTimeout 假保存
  saving.value = true
  try {
    await saveThirdPartySettings({ ...thirdPartyForm })
    ElMessage.success('第三方服务设置保存成功')
  } catch {
    // error handled by interceptor
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    const data = await getSettings()
    if (data) {
      Object.assign(basicForm, data.basic)
      Object.assign(securityForm, data.security)
      Object.assign(thirdPartyForm, data.thirdParty)
    }
  } catch {
    // 首次加载若后端未配置则使用默认值
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.settings-page {
  max-width: 900px;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.page-subtitle {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-top: 4px;
}

.settings-tabs {
  background: var(--color-white);
}

.settings-form {
  padding: 24px 0;
  max-width: 640px;
}

.logo-uploader {
  width: 100px;
  height: 100px;
  border: 1px dashed var(--color-border);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: border-color 0.3s;
}

.logo-uploader:hover {
  border-color: var(--color-primary);
}

.upload-icon {
  color: var(--color-text-placeholder);
}

.uploaded-logo {
  width: 100px;
  height: 100px;
  object-fit: cover;
  border-radius: 8px;
}

.form-tip {
  margin-left: 12px;
  font-size: 13px;
  color: var(--color-text-secondary);
}
</style>