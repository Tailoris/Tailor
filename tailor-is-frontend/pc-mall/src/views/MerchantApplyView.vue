<template>
  <div
    class="merchant-apply-view"
    role="main"
    aria-label="商家入驻申请页面"
  >
    <el-breadcrumb
      separator="/"
      aria-label="面包屑导航"
    >
      <el-breadcrumb-item :to="{ path: '/' }">
        首页
      </el-breadcrumb-item>
      <el-breadcrumb-item>商家入驻</el-breadcrumb-item>
    </el-breadcrumb>

    <h2 class="page-title">
      商家入驻申请
    </h2>

    <div class="apply-container">
      <el-steps
        :active="currentStep"
        finish-status="success"
        align-center
        class="apply-steps"
      >
        <el-step title="基本信息" />
        <el-step title="资质上传" />
        <el-step title="联系信息" />
        <el-step title="确认提交" />
      </el-steps>

      <div class="step-content">
        <div
          v-show="currentStep === 0"
          class="step-panel"
        >
          <el-form
            ref="step1FormRef"
            :model="formData"
            :rules="step1Rules"
            label-width="120px"
          >
            <el-form-item
              label="商家类型"
              prop="merchantType"
            >
              <el-select
                v-model="formData.merchantType"
                placeholder="请选择商家类型"
              >
                <el-option
                  label="个人商家"
                  :value="1"
                />
                <el-option
                  label="企业商家"
                  :value="2"
                />
                <el-option
                  label="品牌商家"
                  :value="3"
                />
                <el-option
                  label="工厂商家"
                  :value="4"
                />
              </el-select>
            </el-form-item>
            <el-form-item
              label="店铺名称"
              prop="shopName"
            >
              <el-input
                v-model="formData.shopName"
                placeholder="请输入店铺名称（2-64字符）"
                maxlength="64"
                show-word-limit
              />
            </el-form-item>
            <el-form-item
              label="经营类目"
              prop="businessCategories"
            >
              <el-checkbox-group v-model="formData.businessCategories">
                <el-checkbox
                  v-for="cat in availableCategories"
                  :key="cat.code"
                  :value="cat.code"
                >
                  {{ cat.name }}
                </el-checkbox>
              </el-checkbox-group>
              <div class="form-tip">
                最多选择2个经营类目
              </div>
            </el-form-item>
            <el-form-item
              v-if="isPersonal"
              label="申请人姓名"
              prop="contactName"
            >
              <el-input
                v-model="formData.contactName"
                placeholder="请输入申请人真实姓名"
                maxlength="64"
              />
            </el-form-item>
            <el-form-item
              v-else
              label="公司名称"
              prop="companyName"
            >
              <el-input
                v-model="formData.companyName"
                placeholder="请输入营业执照上的公司全称"
                maxlength="128"
              />
            </el-form-item>
            <el-form-item
              v-if="isPersonal"
              label="身份证号"
              prop="idCardNo"
            >
              <el-input
                v-model="formData.idCardNo"
                placeholder="请输入18位身份证号码"
                maxlength="18"
              />
            </el-form-item>
            <el-form-item
              v-else
              label="营业执照号"
              prop="licenseNo"
            >
              <el-input
                v-model="formData.licenseNo"
                placeholder="请输入统一社会信用代码"
                maxlength="64"
              />
            </el-form-item>
          </el-form>
          <div class="step-actions">
            <el-button
              type="primary"
              @click="nextStep"
            >
              下一步
            </el-button>
          </div>
        </div>

        <div
          v-show="currentStep === 1"
          class="step-panel"
        >
          <el-form
            ref="step2FormRef"
            :model="formData"
            :rules="step2Rules"
            label-width="120px"
          >
            <template v-if="isPersonal">
              <div class="id-card-upload-section">
                <el-form-item
                  label="身份证正面"
                  prop="idCardFront"
                >
                  <el-upload
                    class="upload-card"
                    :show-file-list="false"
                    :before-upload="beforeIdCardFrontUpload"
                    accept="image/jpeg,image/png,.jpg,.png"
                  >
                    <div
                      v-if="formData.idCardFront"
                      class="upload-preview-img"
                    >
                      <img
                        :src="formData.idCardFront"
                        alt="身份证正面"
                      >
                      <div class="upload-mask">
                        <span>点击重新上传</span>
                      </div>
                    </div>
                    <div
                      v-else
                      class="upload-placeholder"
                    >
                      <el-icon :size="32">
                        <Plus />
                      </el-icon>
                      <span class="upload-tip">上传身份证正面</span>
                      <span class="upload-hint">支持JPG、PNG格式，不超过3MB</span>
                    </div>
                  </el-upload>
                  <div class="upload-desc">
                    请上传身份证人像面照片，确保信息清晰可辨
                  </div>
                </el-form-item>

                <el-form-item
                  label="身份证反面"
                  prop="idCardBack"
                >
                  <el-upload
                    class="upload-card"
                    :show-file-list="false"
                    :before-upload="beforeIdCardBackUpload"
                    accept="image/jpeg,image/png,.jpg,.png"
                  >
                    <div
                      v-if="formData.idCardBack"
                      class="upload-preview-img"
                    >
                      <img
                        :src="formData.idCardBack"
                        alt="身份证反面"
                      >
                      <div class="upload-mask">
                        <span>点击重新上传</span>
                      </div>
                    </div>
                    <div
                      v-else
                      class="upload-placeholder"
                    >
                      <el-icon :size="32">
                        <Plus />
                      </el-icon>
                      <span class="upload-tip">上传身份证反面</span>
                      <span class="upload-hint">支持JPG、PNG格式，不超过3MB</span>
                    </div>
                  </el-upload>
                  <div class="upload-desc">
                    请上传身份证国徽面照片，确保信息清晰可辨
                  </div>
                </el-form-item>
              </div>
            </template>
            <template v-else>
              <el-form-item
                label="营业执照"
                prop="licenseImage"
              >
                <el-upload
                  class="upload-card license-upload"
                  :show-file-list="false"
                  :before-upload="beforeLicenseUpload"
                  :http-request="handleLicenseUpload"
                  accept="image/jpeg,image/png,.jpg,.png,.pdf"
                >
                  <div
                    v-if="formData.licenseImage"
                    class="upload-preview-img"
                  >
                    <img
                      :src="formData.licenseImage"
                      alt="营业执照"
                    >
                    <div class="upload-mask">
                      <span>点击重新上传</span>
                    </div>
                  </div>
                  <div
                    v-else
                    class="upload-placeholder"
                  >
                    <el-icon :size="32">
                      <Plus />
                    </el-icon>
                    <span class="upload-tip">上传营业执照</span>
                    <span class="upload-hint">支持JPG、PNG、PDF格式，不超过5MB</span>
                  </div>
                </el-upload>
                <div class="upload-desc">
                  请上传营业执照正本或副本照片，确保信息清晰可辨
                </div>
              </el-form-item>
            </template>
            <el-form-item
              v-if="needsPatternWorks"
              label="纸样作品"
              prop="patternWorks"
            >
              <el-upload
                v-model:file-list="patternFileList"
                :before-upload="beforePatternWorkUpload"
                :http-request="handlePatternWorkUpload"
                list-type="picture-card"
                :limit="9"
                accept="image/jpeg,image/png,.jpg,.png,.dxf,.pdf"
              >
                <el-icon><Plus /></el-icon>
              </el-upload>
              <div class="form-tip">
                请上传至少3套原创纸样作品，支持JPG、PNG、DXF、PDF格式
              </div>
            </el-form-item>
            <el-form-item
              v-if="needsDesignWorks"
              label="设计作品"
              prop="designWorks"
            >
              <el-upload
                v-model:file-list="designFileList"
                :before-upload="beforeDesignWorkUpload"
                :http-request="handleDesignWorkUpload"
                list-type="picture-card"
                :limit="9"
                accept="image/jpeg,image/png,.jpg,.png"
              >
                <el-icon><Plus /></el-icon>
              </el-upload>
              <div class="form-tip">
                请上传至少3套原创设计作品，支持JPG、PNG格式
              </div>
            </el-form-item>
            <el-form-item label="资质证书">
              <el-input
                v-model="certImagesInput"
                placeholder="请输入资质证书图片URL，多个用逗号分隔"
                type="textarea"
                :rows="3"
              />
            </el-form-item>
          </el-form>
          <div class="step-actions">
            <el-button @click="currentStep = 0">
              上一步
            </el-button>
            <el-button
              type="primary"
              @click="nextStep"
            >
              下一步
            </el-button>
          </div>
        </div>

        <div
          v-show="currentStep === 2"
          class="step-panel"
        >
          <el-form
            ref="step3FormRef"
            :model="formData"
            :rules="step3Rules"
            label-width="120px"
          >
            <el-form-item
              label="联系人"
              prop="contactName"
            >
              <el-input
                v-model="formData.contactName"
                placeholder="请输入联系人姓名"
              />
            </el-form-item>
            <el-form-item
              label="联系电话"
              prop="contactPhone"
            >
              <el-input
                v-model="formData.contactPhone"
                placeholder="请输入联系电话"
              />
            </el-form-item>
            <el-form-item
              label="联系邮箱"
              prop="contactEmail"
            >
              <el-input
                v-model="formData.contactEmail"
                placeholder="请输入联系邮箱"
              />
            </el-form-item>
          </el-form>
          <div class="step-actions">
            <el-button @click="currentStep = 1">
              上一步
            </el-button>
            <el-button
              type="primary"
              @click="nextStep"
            >
              下一步
            </el-button>
          </div>
        </div>

        <div
          v-show="currentStep === 3"
          class="step-panel"
        >
          <div class="review-section">
            <h3>申请信息确认</h3>
            <el-descriptions
              :column="1"
              border
            >
              <el-descriptions-item label="商家类型">
                {{ merchantTypeLabel }}
              </el-descriptions-item>
              <el-descriptions-item label="店铺名称">
                {{ formData.shopName }}
              </el-descriptions-item>
              <el-descriptions-item label="经营类目">
                {{ categoryLabels.join('、') }}
              </el-descriptions-item>
              <el-descriptions-item
                v-if="!isPersonal"
                label="公司名称"
              >
                {{ formData.companyName }}
              </el-descriptions-item>
              <el-descriptions-item
                v-if="!isPersonal"
                label="营业执照号"
              >
                {{ formData.licenseNo }}
              </el-descriptions-item>
              <el-descriptions-item
                v-if="isPersonal"
                label="申请人姓名"
              >
                {{ formData.contactName }}
              </el-descriptions-item>
              <el-descriptions-item
                v-if="isPersonal"
                label="身份证号"
              >
                {{ formData.idCardNo }}
              </el-descriptions-item>
              <el-descriptions-item
                v-if="isPersonal"
                label="身份证正面"
              >
                <el-image
                  v-if="formData.idCardFront"
                  :src="formData.idCardFront"
                  :preview-src-list="[formData.idCardFront]"
                  fit="cover"
                  style="width: 120px; height: 80px; border-radius: 4px;"
                />
                <span
                  v-else
                  style="color: #999;"
                >未上传</span>
              </el-descriptions-item>
              <el-descriptions-item
                v-if="isPersonal"
                label="身份证反面"
              >
                <el-image
                  v-if="formData.idCardBack"
                  :src="formData.idCardBack"
                  :preview-src-list="[formData.idCardBack]"
                  fit="cover"
                  style="width: 120px; height: 80px; border-radius: 4px;"
                />
                <span
                  v-else
                  style="color: #999;"
                >未上传</span>
              </el-descriptions-item>
              <el-descriptions-item label="联系人">
                {{ formData.contactName }}
              </el-descriptions-item>
              <el-descriptions-item label="联系电话">
                {{ formData.contactPhone }}
              </el-descriptions-item>
              <el-descriptions-item label="联系邮箱">
                {{ formData.contactEmail }}
              </el-descriptions-item>
              <el-descriptions-item
                v-if="needsPatternWorks"
                label="纸样作品"
              >
                <div class="works-preview">
                  <el-image
                    v-for="(url, idx) in formData.patternWorks"
                    :key="idx"
                    :src="url"
                    :preview-src-list="formData.patternWorks"
                    :initial-index="idx"
                    fit="cover"
                    style="width: 60px; height: 60px; margin-right: 8px; border-radius: 4px;"
                  />
                  <span
                    v-if="formData.patternWorks.length === 0"
                    style="color: #999;"
                  >未上传</span>
                </div>
              </el-descriptions-item>
              <el-descriptions-item
                v-if="needsDesignWorks"
                label="设计作品"
              >
                <div class="works-preview">
                  <el-image
                    v-for="(url, idx) in formData.designWorks"
                    :key="idx"
                    :src="url"
                    :preview-src-list="formData.designWorks"
                    :initial-index="idx"
                    fit="cover"
                    style="width: 60px; height: 60px; margin-right: 8px; border-radius: 4px;"
                  />
                  <span
                    v-if="formData.designWorks.length === 0"
                    style="color: #999;"
                  >未上传</span>
                </div>
              </el-descriptions-item>
            </el-descriptions>
          </div>
          <div class="agreement-section">
            <div class="agreement-title">
              请阅读并确认以下协议（全部勾选后方可提交申请）
            </div>
            <div class="agreement-list">
              <div
                class="agreement-item"
                :class="{ 'is-error': agreementErrors.merchant }"
              >
                <el-checkbox v-model="agreements.merchant">
                  《商家入驻协议》
                </el-checkbox>
                <a
                  class="view-link"
                  @click="openAgreement('merchant')"
                >查看完整内容</a>
              </div>
              <div
                class="agreement-item"
                :class="{ 'is-error': agreementErrors.copyright }"
              >
                <el-checkbox v-model="agreements.copyright">
                  《版权保护协议》
                </el-checkbox>
                <a
                  class="view-link"
                  @click="openAgreement('copyright')"
                >查看完整内容</a>
              </div>
              <div
                class="agreement-item"
                :class="{ 'is-error': agreementErrors.transaction }"
              >
                <el-checkbox v-model="agreements.transaction">
                  《交易履约规则》
                </el-checkbox>
                <a
                  class="view-link"
                  @click="openAgreement('transaction')"
                >查看完整内容</a>
              </div>
            </div>
            <div
              v-if="agreementErrorMsg"
              class="agreement-error-tip"
            >
              <el-icon><Warning /></el-icon>
              {{ agreementErrorMsg }}
            </div>
          </div>
          <div class="step-actions">
            <el-button @click="currentStep = 2">
              上一步
            </el-button>
            <el-button
              type="primary"
              :loading="submitting"
              :disabled="!allAgreed"
              @click="handleSubmit"
            >
              提交申请
            </el-button>
          </div>
        </div>
      </div>
    </div>
    <AgreementDialog
      v-model="agreementDialogVisible"
      :default-type="agreementDialogType"
      :show-confirm="true"
      @confirm="handleAgreementConfirm"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Plus, Warning } from '@element-plus/icons-vue';
import type { FormInstance, FormRules, UploadProps } from 'element-plus';
import { applyMerchant, uploadFile, getApplyStatus } from '@/api/merchant';
import type { MerchantApplyData } from '@/api/merchant';
import AgreementDialog from '@/components/agreements/AgreementDialog.vue';

const router = useRouter();
const currentStep = ref(0);
const submitting = ref(false);
const uploading = ref(false);

const agreements = reactive({
  merchant: false,
  copyright: false,
  transaction: false,
});

const agreementErrors = reactive({
  merchant: false,
  copyright: false,
  transaction: false,
});

const agreementDialogVisible = ref(false);
const agreementDialogType = ref<'merchant' | 'copyright' | 'transaction'>('merchant');

const allAgreed = computed(() => {
  return agreements.merchant && agreements.copyright && agreements.transaction;
});

const agreementErrorMsg = computed(() => {
  const unagreed: string[] = [];
  if (!agreements.merchant) unagreed.push('《商家入驻协议》');
  if (!agreements.copyright) unagreed.push('《版权保护协议》');
  if (!agreements.transaction) unagreed.push('《交易履约规则》');
  if (unagreed.length === 0) return '';
  return `请先阅读并勾选${unagreed.join('、')}`;
});

function openAgreement(type: 'merchant' | 'copyright' | 'transaction') {
  agreementDialogType.value = type;
  agreementDialogVisible.value = true;
}

function handleAgreementConfirm(type: string) {
  if (type === 'merchant') {
    agreements.merchant = true;
    agreementErrors.merchant = false;
  } else if (type === 'copyright') {
    agreements.copyright = true;
    agreementErrors.copyright = false;
  } else if (type === 'transaction') {
    agreements.transaction = true;
    agreementErrors.transaction = false;
  }
}

const certImagesInput = ref('');
const patternFileList = ref<any[]>([]);
const designFileList = ref<any[]>([]);

const MAX_IMAGE_SIZE = 3 * 1024 * 1024;
const MAX_LICENSE_SIZE = 5 * 1024 * 1024;
const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/jpg'];
const ALLOWED_LICENSE_TYPES = ['image/jpeg', 'image/png', 'image/jpg', 'application/pdf'];

const formData = ref({
  merchantType: 1,
  shopName: '',
  businessCategories: [] as string[],
  companyName: '',
  licenseNo: '',
  licenseImage: '',
  idCardFront: '',
  idCardBack: '',
  idCardFrontFile: null as File | null,
  idCardBackFile: null as File | null,
  idCardNo: '',
  certImages: [] as string[],
  contactName: '',
  contactPhone: '',
  contactEmail: '',
  patternWorks: [] as string[],
  designWorks: [] as string[],
});

const merchantTypeMap: Record<number, string> = {
  1: '个人商家',
  2: '企业商家',
  3: '品牌商家',
  4: '工厂商家',
};

const categoryMap: Record<string, string> = {
  pattern: '服装纸样',
  design: '服装设计',
  fabric: '面辅料供应',
  factory: '服装工厂',
};

const merchantTypeLabel = computed(() => merchantTypeMap[formData.value.merchantType] || '未知');

const categoryLabels = computed(() =>
  formData.value.businessCategories.map((c) => categoryMap[c] || c)
);

const isPersonal = computed(() => formData.value.merchantType === 1);

const merchantCategories: Record<number, Array<{ code: string; name: string }>> = {
  1: [
    { code: 'pattern', name: '服装纸样' },
    { code: 'design', name: '服装设计' },
  ],
  2: [
    { code: 'pattern', name: '服装纸样' },
    { code: 'design', name: '服装设计' },
    { code: 'fabric', name: '面辅料供应' },
    { code: 'factory', name: '服装工厂' },
  ],
  3: [
    { code: 'pattern', name: '服装纸样' },
    { code: 'design', name: '服装设计' },
    { code: 'fabric', name: '面辅料供应' },
    { code: 'factory', name: '服装工厂' },
  ],
  4: [
    { code: 'fabric', name: '面辅料供应' },
    { code: 'factory', name: '服装工厂' },
  ],
};

const availableCategories = computed(() => {
  return merchantCategories[formData.value.merchantType] || [];
});

const needsPatternWorks = computed(() => formData.value.businessCategories.includes('pattern'));
const needsDesignWorks = computed(() => formData.value.businessCategories.includes('design'));

function validateImageFile(file: File): boolean {
  if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
    ElMessage.error('仅支持 JPG、PNG 格式的图片');
    return false;
  }
  if (file.size > MAX_IMAGE_SIZE) {
    ElMessage.error('图片大小不能超过 3MB');
    return false;
  }
  return true;
}

function validateLicenseFile(file: File): boolean {
  if (!ALLOWED_LICENSE_TYPES.includes(file.type)) {
    ElMessage.error('仅支持 JPG、PNG、PDF 格式');
    return false;
  }
  if (file.size > MAX_LICENSE_SIZE) {
    ElMessage.error('文件大小不能超过 5MB');
    return false;
  }
  return true;
}

async function uploadImageAndSave(file: File, field: 'idCardFront' | 'idCardBack' | 'licenseImage') {
  try {
    uploading.value = true;
    const result = await uploadFile(file, 'identity');
    formData.value[field] = result.url;
    return result.url;
  } catch (err) {
    const msg = err instanceof Error ? err.message : '上传失败';
    ElMessage.error(`${field === 'idCardFront' ? '身份证正面' : field === 'idCardBack' ? '身份证反面' : '营业执照'}上传失败：${msg}`);
    throw err;
  } finally {
    uploading.value = false;
  }
}

const beforeIdCardFrontUpload: UploadProps['beforeUpload'] = (file) => {
  if (!validateImageFile(file)) return false;
  const reader = new FileReader();
  reader.onload = async (e) => {
    formData.value.idCardFront = e.target?.result as string;
    formData.value.idCardFrontFile = file;
    try {
      await uploadImageAndSave(file, 'idCardFront');
    } catch {
    }
    if (step2FormRef.value) {
      step2FormRef.value.validateField('idCardFront').catch(() => {});
    }
  };
  reader.readAsDataURL(file);
  return false;
};

const beforeIdCardBackUpload: UploadProps['beforeUpload'] = (file) => {
  if (!validateImageFile(file)) return false;
  const reader = new FileReader();
  reader.onload = async (e) => {
    formData.value.idCardBack = e.target?.result as string;
    formData.value.idCardBackFile = file;
    try {
      await uploadImageAndSave(file, 'idCardBack');
    } catch {
    }
    if (step2FormRef.value) {
      step2FormRef.value.validateField('idCardBack').catch(() => {});
    }
  };
  reader.readAsDataURL(file);
  return false;
};

const beforeLicenseUpload: UploadProps['beforeUpload'] = (file) => {
  if (!validateLicenseFile(file)) return false;
  return true;
};

async function handleLicenseUpload(options: { file: File }) {
  try {
    uploading.value = true;
    const result = await uploadFile(options.file, 'license');
    formData.value.licenseImage = result.url;
    if (step2FormRef.value) {
      step2FormRef.value.validateField('licenseImage').catch(() => {});
    }
    ElMessage.success('营业执照上传成功');
  } catch (err) {
    const msg = err instanceof Error ? err.message : '上传失败';
    ElMessage.error(`营业执照上传失败：${msg}`);
  } finally {
    uploading.value = false;
  }
}

const beforePatternWorkUpload: UploadProps['beforeUpload'] = (file) => {
  const allowedTypes = ['image/jpeg', 'image/png', 'image/jpg', 'application/pdf',
    'application/dxf', 'application/x-dxf', 'image/vnd.dxf'];
  const isAllowed = allowedTypes.includes(file.type) ||
    file.name.toLowerCase().endsWith('.dxf') ||
    file.name.toLowerCase().endsWith('.pdf');
  if (!isAllowed) {
    ElMessage.error('仅支持 JPG、PNG、DXF、PDF 格式');
    return false;
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 10MB');
    return false;
  }
  return true;
};

async function handlePatternWorkUpload(options: { file: File; onSuccess: (res: any) => void; onError: (err: any) => void }) {
  try {
    const result = await uploadFile(options.file, 'pattern');
    formData.value.patternWorks.push(result.url);
    options.onSuccess(result);
  } catch (err) {
    const msg = err instanceof Error ? err.message : '上传失败';
    ElMessage.error(`纸样作品上传失败：${msg}`);
    options.onError(err);
  }
}

const beforeDesignWorkUpload: UploadProps['beforeUpload'] = (file) => {
  if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
    ElMessage.error('仅支持 JPG、PNG 格式的图片');
    return false;
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 5MB');
    return false;
  }
  return true;
};

async function handleDesignWorkUpload(options: { file: File; onSuccess: (res: any) => void; onError: (err: any) => void }) {
  try {
    const result = await uploadFile(options.file, 'design');
    formData.value.designWorks.push(result.url);
    options.onSuccess(result);
  } catch (err) {
    const msg = err instanceof Error ? err.message : '上传失败';
    ElMessage.error(`设计作品上传失败：${msg}`);
    options.onError(err);
  }
}

watch(
  () => formData.value.merchantType,
  () => {
    formData.value.companyName = '';
    formData.value.licenseNo = '';
    formData.value.licenseImage = '';
    formData.value.idCardFront = '';
    formData.value.idCardBack = '';
    formData.value.idCardFrontFile = null;
    formData.value.idCardBackFile = null;
    formData.value.businessCategories = [];
    formData.value.patternWorks = [];
    formData.value.designWorks = [];
    if (step1FormRef.value) {
      step1FormRef.value.clearValidate();
    }
    if (step2FormRef.value) {
      step2FormRef.value.clearValidate();
    }
  },
);

const step1FormRef = ref<FormInstance>();
const step2FormRef = ref<FormInstance>();
const step3FormRef = ref<FormInstance>();

const step1Rules = computed<FormRules>(() => ({
  merchantType: [{ required: true, message: '请选择商家类型', trigger: 'change' }],
  shopName: [
    { required: true, message: '请输入店铺名称', trigger: 'blur' },
    { min: 2, max: 64, message: '店铺名称长度应在2-64个字符之间', trigger: 'blur' },
  ],
  businessCategories: [
    {
      type: 'array',
      required: true,
      message: '请至少选择一个经营类目',
      trigger: 'change',
    },
    {
      type: 'array',
      max: 2,
      message: '最多选择2个经营类目',
      trigger: 'change',
    },
  ],
  companyName: [
    { required: !isPersonal.value, message: '请输入公司名称', trigger: 'blur' },
    { max: 128, message: '公司名称不能超过128个字符', trigger: 'blur' },
  ],
  licenseNo: [
    { required: !isPersonal.value, message: '请输入营业执照号', trigger: 'blur' },
    { max: 64, message: '营业执照号不能超过64个字符', trigger: 'blur' },
  ],
  contactName: [
    { required: isPersonal.value, message: '请输入申请人姓名', trigger: 'blur' },
    { max: 64, message: '姓名不能超过64个字符', trigger: 'blur' },
  ],
  idCardNo: [
    { required: isPersonal.value, message: '请输入身份证号', trigger: 'blur' },
    {
      pattern: /(^\d{15}$)|(^\d{17}(\d|X|x)$)/,
      message: '身份证号格式不正确',
      trigger: 'blur',
    },
  ],
}));

const step2Rules = computed<FormRules>(() => {
  const rules: Record<string, any> = {};

  if (isPersonal.value) {
    rules.idCardFront = [
      { required: true, message: '请上传身份证正面照片', trigger: 'change' },
      {
        validator: (_rule: unknown, _value: unknown, callback: any) => {
          if (formData.value.idCardFront) {
            callback();
          } else {
            callback(new Error('请上传身份证正面照片'));
          }
        },
        trigger: 'change',
      },
    ];
    rules.idCardBack = [
      { required: true, message: '请上传身份证反面照片', trigger: 'change' },
      {
        validator: (_rule: unknown, _value: unknown, callback: any) => {
          if (formData.value.idCardBack) {
            callback();
          } else {
            callback(new Error('请上传身份证反面照片'));
          }
        },
        trigger: 'change',
      },
    ];
  } else {
    rules.licenseImage = [
      { required: true, message: '请上传营业执照', trigger: 'blur' },
    ];
  }

  if (needsPatternWorks.value) {
    rules.patternWorks = [
      {
        type: 'array',
        required: true,
        min: 3,
        message: '请上传至少3套纸样作品',
        trigger: 'change',
      },
    ];
  }

  if (needsDesignWorks.value) {
    rules.designWorks = [
      {
        type: 'array',
        required: true,
        min: 3,
        message: '请上传至少3套设计作品',
        trigger: 'change',
      },
    ];
  }

  return rules;
});

const step3Rules: FormRules = {
  contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '电话格式不正确', trigger: 'blur' },
  ],
  contactEmail: [
    { required: true, message: '请输入联系邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
};

async function validateCurrentStep(): Promise<boolean> {
  let formRef: FormInstance | undefined;
  switch (currentStep.value) {
    case 0:
      formRef = step1FormRef.value;
      break;
    case 1:
      formRef = step2FormRef.value;
      break;
    case 2:
      formRef = step3FormRef.value;
      break;
  }
  if (formRef) {
    const valid = await formRef.validate().catch(() => false);
    return !!valid;
  }
  return true;
}

async function nextStep() {
  const valid = await validateCurrentStep();
  if (valid) {
    if (currentStep.value === 1) {
      formData.value.certImages = certImagesInput.value
        ? certImagesInput.value
            .split(',')
            .map((u) => u.trim())
            .filter(Boolean)
        : [];
    }
    if (currentStep.value < 3) {
      currentStep.value++;
    }
  }
}

function buildSubmitData(): MerchantApplyData {
  const data: MerchantApplyData = {
    shopName: formData.value.shopName.trim(),
    merchantType: formData.value.merchantType,
    businessCategories: formData.value.businessCategories,
    contactName: formData.value.contactName.trim(),
    contactPhone: formData.value.contactPhone.trim(),
    contactEmail: formData.value.contactEmail.trim(),
    agreeMerchantAgreement: agreements.merchant,
    agreeCopyrightAgreement: agreements.copyright,
    agreeTransactionRules: agreements.transaction,
  };

  if (isPersonal.value) {
    data.idCardNo = formData.value.idCardNo.trim();
    const idCardFront = formData.value.idCardFront;
    const idCardBack = formData.value.idCardBack;
    if (idCardFront && !idCardFront.startsWith('data:')) {
      data.idCardFront = idCardFront;
    }
    if (idCardBack && !idCardBack.startsWith('data:')) {
      data.idCardBack = idCardBack;
    }
  } else {
    data.companyName = formData.value.companyName.trim();
    data.licenseNo = formData.value.licenseNo.trim();
    data.licenseImage = formData.value.licenseImage;
  }

  if (formData.value.patternWorks.length > 0) {
    data.patternWorks = formData.value.patternWorks;
  }
  if (formData.value.designWorks.length > 0) {
    data.designWorks = formData.value.designWorks;
  }

  return data;
}

function getErrorMessage(err: unknown): string {
  if (err instanceof Error) {
    return err.message;
  }
  if (typeof err === 'string') {
    return err;
  }
  return '提交失败，请重试';
}

async function handleSubmit() {
  const unagreed: string[] = [];
  agreementErrors.merchant = false;
  agreementErrors.copyright = false;
  agreementErrors.transaction = false;

  if (!agreements.merchant) {
    unagreed.push('《商家入驻协议》');
    agreementErrors.merchant = true;
  }
  if (!agreements.copyright) {
    unagreed.push('《版权保护协议》');
    agreementErrors.copyright = true;
  }
  if (!agreements.transaction) {
    unagreed.push('《交易履约规则》');
    agreementErrors.transaction = true;
  }

  if (unagreed.length > 0) {
    ElMessage.warning(`请先阅读并勾选${unagreed.join('、')}`);
    return;
  }

  if (uploading.value) {
    ElMessage.warning('文件上传中，请稍候再提交');
    return;
  }

  if (isPersonal.value) {
    if (formData.value.idCardFront?.startsWith('data:')) {
      ElMessage.warning('身份证正面图片未上传成功，请重新上传');
      return;
    }
    if (formData.value.idCardBack?.startsWith('data:')) {
      ElMessage.warning('身份证反面图片未上传成功，请重新上传');
      return;
    }
  }

  submitting.value = true;
  try {
    const submitData = buildSubmitData();
    await applyMerchant(submitData);
    ElMessage.success('申请提交成功，请等待审核，预计1-3个工作日完成');
    setTimeout(() => {
      router.push('/');
    }, 1500);
  } catch (err) {
    const msg = getErrorMessage(err);
    ElMessage.error(msg);
  } finally {
    submitting.value = false;
  }
}

onMounted(() => {
  getApplyStatus()
    .then((info) => {
      if (info && (info as any).applyStatus) {
        const status = (info as any).applyStatus;
        if (status === 1 || status === 2) {
          ElMessage.info('您已提交入驻申请，请耐心等待审核');
          router.push('/');
        }
      }
    })
    .catch(() => {
    });
});
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
  padding: 20px;
  background: #f9fafb;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}
.agreement-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 16px;
}
.agreement-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.agreement-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  transition: all 0.2s;
}
.agreement-item:hover {
  border-color: #6366f1;
}
.agreement-item.is-error {
  border-color: #f56c6c;
  background: #fef2f2;
}
.view-link {
  color: #6366f1;
  font-size: 13px;
  cursor: pointer;
  text-decoration: none;
}
.view-link:hover {
  text-decoration: underline;
}
.agreement-error-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  padding: 10px 14px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 6px;
  color: #dc2626;
  font-size: 13px;
}
.id-card-upload-section {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}
.id-card-upload-section .el-form-item {
  flex: 1;
  min-width: 280px;
}
.upload-card {
  width: 100%;
}
.upload-card :deep(.el-upload) {
  width: 100%;
}
.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 160px;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  background: #fafafa;
  transition: border-color 0.3s;
  gap: 8px;
}
.upload-placeholder:hover {
  border-color: #409eff;
}
.upload-placeholder .upload-tip {
  font-size: 14px;
  color: #666;
}
.upload-placeholder .upload-hint {
  font-size: 12px;
  color: #999;
}
.upload-preview-img {
  position: relative;
  width: 100%;
  height: 160px;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
}
.upload-preview-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.upload-mask {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 14px;
  opacity: 0;
  transition: opacity 0.3s;
}
.upload-preview-img:hover .upload-mask {
  opacity: 1;
}
.upload-desc {
  font-size: 12px;
  color: #999;
  margin-top: 6px;
  line-height: 1.5;
}
.form-tip {
  font-size: 12px;
  color: #999;
  margin-top: 6px;
  line-height: 1.5;
}
.works-preview {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}
</style>
