<template>
  <div class="merchant-apply-view" role="main" aria-label="商家入驻申请页面">
    <el-breadcrumb separator="/" aria-label="面包屑导航">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>商家入驻</el-breadcrumb-item>
    </el-breadcrumb>

    <h2 class="page-title">商家入驻申请</h2>

    <div class="apply-container">
      <el-steps :active="currentStep" finish-status="success" align-center class="apply-steps">
        <el-step title="基本信息" />
        <el-step title="资质上传" />
        <el-step title="联系信息" />
        <el-step title="确认提交" />
      </el-steps>

      <div class="step-content">
        <div v-show="currentStep === 0" class="step-panel">
          <el-form :model="formData" :rules="step1Rules" ref="step1FormRef" label-width="120px">
            <el-form-item label="商家类型" prop="merchantType">
              <el-select v-model="formData.merchantType" placeholder="请选择商家类型">
                <el-option label="个人商家" :value="0" />
                <el-option label="企业商家" :value="1" />
                <el-option label="品牌商家" :value="2" />
              </el-select>
            </el-form-item>
            <el-form-item label="公司名称" prop="companyName">
              <el-input v-model="formData.companyName" placeholder="请输入公司/店铺名称" />
            </el-form-item>
            <el-form-item label="营业执照号" prop="licenseNo">
              <el-input v-model="formData.licenseNo" placeholder="请输入营业执照号" />
            </el-form-item>
          </el-form>
          <div class="step-actions">
            <el-button type="primary" @click="nextStep">下一步</el-button>
          </div>
        </div>

        <div v-show="currentStep === 1" class="step-panel">
          <el-form :model="formData" :rules="step2Rules" ref="step2FormRef" label-width="120px">
            <el-form-item label="营业执照" prop="licenseImage">
              <el-input v-model="formData.licenseImage" placeholder="请输入营业执照图片URL" />
            </el-form-item>
            <el-form-item label="资质证书">
              <el-input v-model="certImagesInput" placeholder="请输入资质证书图片URL，多个用逗号分隔" type="textarea" :rows="3" />
            </el-form-item>
          </el-form>
          <div class="step-actions">
            <el-button @click="currentStep = 0">上一步</el-button>
            <el-button type="primary" @click="nextStep">下一步</el-button>
          </div>
        </div>

        <div v-show="currentStep === 2" class="step-panel">
          <el-form :model="formData" :rules="step3Rules" ref="step3FormRef" label-width="120px">
            <el-form-item label="联系人" prop="contactName">
              <el-input v-model="formData.contactName" placeholder="请输入联系人姓名" />
            </el-form-item>
            <el-form-item label="联系电话" prop="contactPhone">
              <el-input v-model="formData.contactPhone" placeholder="请输入联系电话" />
            </el-form-item>
            <el-form-item label="联系邮箱" prop="contactEmail">
              <el-input v-model="formData.contactEmail" placeholder="请输入联系邮箱" />
            </el-form-item>
          </el-form>
          <div class="step-actions">
            <el-button @click="currentStep = 1">上一步</el-button>
            <el-button type="primary" @click="nextStep">下一步</el-button>
          </div>
        </div>

        <div v-show="currentStep === 3" class="step-panel">
          <div class="review-section">
            <h3>申请信息确认</h3>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="商家类型">
                {{ formData.merchantType === 0 ? '个人商家' : formData.merchantType === 1 ? '企业商家' : '品牌商家' }}
              </el-descriptions-item>
              <el-descriptions-item label="公司名称">{{ formData.companyName }}</el-descriptions-item>
              <el-descriptions-item label="营业执照号">{{ formData.licenseNo }}</el-descriptions-item>
              <el-descriptions-item label="联系人">{{ formData.contactName }}</el-descriptions-item>
              <el-descriptions-item label="联系电话">{{ formData.contactPhone }}</el-descriptions-item>
              <el-descriptions-item label="联系邮箱">{{ formData.contactEmail }}</el-descriptions-item>
            </el-descriptions>
          </div>
          <div class="agreement-section">
            <el-checkbox v-model="agreed">我已阅读并同意《商家入驻协议》</el-checkbox>
          </div>
          <div class="step-actions">
            <el-button @click="currentStep = 2">上一步</el-button>
            <el-button type="primary" @click="handleSubmit" :loading="submitting" :disabled="!agreed">
              提交申请
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { applyMerchant } from '@/api/merchant'

const router = useRouter()
const currentStep = ref(0)
const submitting = ref(false)
const agreed = ref(false)

const certImagesInput = ref('')

const formData = ref({
  merchantType: 1,
  companyName: '',
  licenseNo: '',
  licenseImage: '',
  certImages: [] as string[],
  contactName: '',
  contactPhone: '',
  contactEmail: ''
})

const step1FormRef = ref<FormInstance>()
const step2FormRef = ref<FormInstance>()
const step3FormRef = ref<FormInstance>()

const step1Rules: FormRules = {
  merchantType: [{ required: true, message: '请选择商家类型', trigger: 'change' }],
  companyName: [{ required: true, message: '请输入公司名称', trigger: 'blur' }],
  licenseNo: [{ required: true, message: '请输入营业执照号', trigger: 'blur' }]
}

const step2Rules: FormRules = {
  licenseImage: [{ required: true, message: '请上传营业执照', trigger: 'blur' }]
}

const step3Rules: FormRules = {
  contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '电话格式不正确', trigger: 'blur' }
  ],
  contactEmail: [
    { required: true, message: '请输入联系邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ]
}

async function validateCurrentStep(): Promise<boolean> {
  let formRef: FormInstance | undefined
  switch (currentStep.value) {
    case 0:
      formRef = step1FormRef.value
      break
    case 1:
      formRef = step2FormRef.value
      break
    case 2:
      formRef = step3FormRef.value
      break
  }
  if (formRef) {
    const valid = await formRef.validate().catch(() => false)
    return !!valid
  }
  return true
}

async function nextStep() {
  const valid = await validateCurrentStep()
  if (valid) {
    if (currentStep.value === 1) {
      formData.value.certImages = certImagesInput.value
        ? certImagesInput.value.split(',').map((u) => u.trim()).filter(Boolean)
        : []
    }
    if (currentStep.value < 3) {
      currentStep.value++
    }
  }
}

async function handleSubmit() {
  if (!agreed.value) {
    ElMessage.warning('请先同意入驻协议')
    return
  }
  submitting.value = true
  try {
    await applyMerchant(formData.value)
    ElMessage.success('申请提交成功，请等待审核')
    router.push('/')
  } catch {
    ElMessage.error('提交失败，请重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.merchant-apply-view {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}
.el-breadcrumb {
  margin-bottom: 20px;
}
.page-title {
  font-size: 24px;
  color: #333;
  margin: 0 0 24px;
}
.apply-container {
  background: #fff;
  border-radius: 8px;
  padding: 32px;
}
.apply-steps {
  margin-bottom: 40px;
}
.step-panel {
  min-height: 300px;
}
.step-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 32px;
}
.review-section h3 {
  font-size: 18px;
  margin: 0 0 16px;
}
.agreement-section {
  margin-top: 24px;
}
</style>
