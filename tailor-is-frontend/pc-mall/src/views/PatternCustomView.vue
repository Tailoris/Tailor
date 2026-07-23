<template>
  <div
    class="pattern-custom-view"
    role="main"
    aria-label="纸样定制"
  >
    <div class="view-header">
      <h1 class="page-title">
        纸样定制
      </h1>
      <p class="page-subtitle">
        专业打版师一对一服务，定制专属纸样
      </p>
    </div>

    <div class="tabs-container">
      <div class="tabs">
        <button
          :class="{ active: activeTab === 'create' }"
          @click="activeTab = 'create'"
        >
          <el-icon><Plus /></el-icon>
          <span>发布定制需求</span>
        </button>
        <button
          :class="{ active: activeTab === 'progress' }"
          @click="activeTab = 'progress'; loadPatterns()"
        >
          <el-icon><Loading /></el-icon>
          <span>打版进度</span>
        </button>
        <button
          :class="{ active: activeTab === 'history' }"
          @click="activeTab = 'history'; loadPatterns()"
        >
          <el-icon><Clock /></el-icon>
          <span>历史订单</span>
        </button>
        <button
          :class="{ active: activeTab === 'versions' }"
          @click="activeTab = 'versions'"
        >
          <el-icon><Document /></el-icon>
          <span>版本对比</span>
        </button>
      </div>
    </div>

    <div
      v-if="activeTab === 'create'"
      class="tab-content"
    >
      <div class="form-container">
        <el-form
          ref="customFormRef"
          :model="customForm"
          label-width="120px"
          class="custom-form"
          :rules="formRules"
        >
          <el-form-item
            label="款式类型"
            prop="styleType"
          >
            <el-select
              v-model="customForm.styleType"
              placeholder="请选择款式类型"
            >
              <el-option
                label="连衣裙"
                value="dress"
              />
              <el-option
                label="上衣"
                value="top"
              />
              <el-option
                label="裤子"
                value="pants"
              />
              <el-option
                label="外套"
                value="coat"
              />
              <el-option
                label="半身裙"
                value="skirt"
              />
              <el-option
                label="衬衫"
                value="shirt"
              />
              <el-option
                label="其他"
                value="other"
              />
            </el-select>
          </el-form-item>

          <el-form-item
            label="版型风格"
            prop="patternStyle"
          >
            <div class="style-options">
              <button
                v-for="style in styleOptions"
                :key="style.value"
                :class="{ active: customForm.patternStyle === style.value }"
                @click="customForm.patternStyle = style.value"
              >
                {{ style.label }}
              </button>
            </div>
          </el-form-item>

          <el-form-item
            label="目标人群"
            prop="targetAudience"
          >
            <el-select
              v-model="customForm.targetAudience"
              placeholder="请选择目标人群"
            >
              <el-option
                label="成人女性"
                value="women"
              />
              <el-option
                label="成人男性"
                value="men"
              />
              <el-option
                label="儿童"
                value="children"
              />
              <el-option
                label="大码"
                value="plus-size"
              />
              <el-option
                label="Petite"
                value="petite"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="尺寸范围">
            <div class="size-range">
              <el-input
                v-model.number="customForm.minSize"
                type="number"
                placeholder="最小码"
                :min="1"
                :max="customForm.maxSize || 100"
              />
              <span class="separator">-</span>
              <el-input
                v-model.number="customForm.maxSize"
                type="number"
                placeholder="最大码"
                :min="customForm.minSize || 1"
              />
              <span class="size-unit">码数</span>
            </div>
          </el-form-item>

          <el-form-item label="工艺要求">
            <el-checkbox-group v-model="customForm.techRequirements">
              <el-checkbox
                label="明线工艺"
                value="visible-stitch"
              />
              <el-checkbox
                label="暗线工艺"
                value="hidden-stitch"
              />
              <el-checkbox
                label="包缝处理"
                value="overlock"
              />
              <el-checkbox
                label="锁眼工艺"
                value="buttonhole"
              />
              <el-checkbox
                label="拉链工艺"
                value="zipper"
              />
              <el-checkbox
                label="烫衬处理"
                value="interlining"
              />
            </el-checkbox-group>
          </el-form-item>

          <el-form-item label="设计参考图">
            <div class="upload-area">
              <el-upload
                class="uploader"
                drag
                :action="uploadUrl"
                :on-success="handleUploadSuccess"
                :on-error="handleUploadError"
                :file-list="imageFiles"
                multiple
                :show-file-list="true"
              >
                <el-icon class="upload-icon">
                  <Upload />
                </el-icon>
                <div class="upload-text">
                  拖拽文件到此处，或<em>点击上传</em>
                </div>
                <div class="upload-hint">
                  支持 JPG、PNG、PDF 格式，单个文件不超过 10MB
                </div>
              </el-upload>
            </div>
          </el-form-item>

          <el-form-item
            label="详细描述"
            prop="description"
          >
            <el-textarea
              v-model="customForm.description"
              :rows="4"
              placeholder="请详细描述您的定制需求，包括版型特点、特殊要求、参考款式等..."
            />
          </el-form-item>

          <el-form-item label="预算范围">
            <div class="budget-range">
              <el-input
                v-model.number="customForm.minBudget"
                type="number"
                placeholder="最低预算"
                :min="0"
                :max="customForm.maxBudget || 100000"
                prefix-icon="¥"
              />
              <span class="separator">-</span>
              <el-input
                v-model.number="customForm.maxBudget"
                type="number"
                placeholder="最高预算"
                :min="customForm.minBudget || 0"
                prefix-icon="¥"
              />
              <span class="budget-unit">元</span>
            </div>
          </el-form-item>

          <el-form-item label="交付时间">
            <el-date-picker
              v-model="customForm.deliveryDate"
              type="date"
              placeholder="请选择期望交付日期"
              :disabled-date="disabledDate"
            />
          </el-form-item>

          <el-form-item label="加急服务">
            <el-switch
              v-model="customForm.isUrgent"
              active-text="加急（24小时内响应，费用+50%）"
              inactive-text="普通"
            />
          </el-form-item>

          <el-form-item>
            <div class="agreement-section">
              <el-checkbox v-model="customForm.agreed">
                我已阅读并同意《<a href="#" @click.prevent="showAgreement = true">服装纸样定制平台用户服务协议</a>》
              </el-checkbox>
            </div>
          </el-form-item>

          <el-form-item>
            <div class="form-actions">
              <el-button
                type="primary"
                size="large"
                :loading="submitLoading"
                @click="submitForm"
                :disabled="!customForm.agreed"
              >
                {{ submitLoading ? '提交中...' : '提交定制需求' }}
              </el-button>
              <el-button
                size="large"
                @click="resetForm"
              >
                重置
              </el-button>
            </div>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <div
      v-if="activeTab === 'progress'"
      class="tab-content"
    >
      <div
        v-if="loading"
        class="loading-state"
      >
        <el-icon class="loading-icon">
          <Loading />
        </el-icon>
        <p>加载中...</p>
      </div>
      <div
        v-else-if="error"
        class="error-state"
      >
        <p>{{ error }}</p>
        <el-button @click="loadPatterns">
          重试
        </el-button>
      </div>
      <div
        v-else
        class="progress-container"
      >
        <div
          v-if="orders.length === 0"
          class="empty-state"
        >
          <div class="empty-icon">
            📋
          </div>
          <p>暂无进行中的版型</p>
          <el-button
            type="primary"
            @click="activeTab = 'create'"
          >
            发布定制需求
          </el-button>
        </div>

        <div
          v-else
          class="orders-list"
        >
          <div
            v-for="pattern in orders"
            :key="pattern.id"
            class="order-card"
          >
            <div class="order-header">
              <div class="order-info">
                <span class="order-id">版型ID：{{ pattern.id }}</span>
                <span class="order-type">{{ getStyleLabel(pattern.category) }}</span>
              </div>
              <span :class="['order-status', getStatusClass(pattern.status)]">
                {{ getStatusText(pattern.status) }}
              </span>
            </div>

            <div class="progress-timeline">
              <div
                v-for="(step, index) in progressSteps"
                :key="step.key"
                :class="[
                  'timeline-item',
                  {
                    active: index <= getCurrentStepIndex(pattern.status),
                    completed: index < getCurrentStepIndex(pattern.status),
                    cancelled: getCurrentStepIndex(pattern.status) === -1,
                  },
                ]"
              >
                <div class="timeline-dot">
                  <el-icon v-if="index < getCurrentStepIndex(pattern.status)">
                    <Check />
                  </el-icon>
                  <span v-else-if="getCurrentStepIndex(pattern.status) === -1">
                    <el-icon><Close /></el-icon>
                  </span>
                  <span v-else>{{ index + 1 }}</span>
                </div>
                <div class="timeline-content">
                  <span class="step-name">{{ step.name }}</span>
                </div>
                <div
                  v-if="index < progressSteps.length - 1"
                  class="timeline-line"
                />
              </div>
            </div>

            <div class="order-details">
              <div class="detail-row">
                <span class="detail-label">版型名称：</span>
                <span class="detail-value">{{ pattern.name }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">描述：</span>
                <span class="detail-value">{{ pattern.description || '暂无描述' }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">价格：</span>
                <span class="detail-value">¥{{ pattern.price || 0 }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">创建时间：</span>
                <span class="detail-value">{{ pattern.createdAt }}</span>
              </div>
            </div>

            <div class="order-actions">
              <el-button @click="viewDetail(pattern.id)">
                查看详情
              </el-button>
              <el-button
                v-if="pattern.status === 1"
                type="primary"
                @click="requestModify(pattern.id)"
              >
                申请修改
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div
      v-if="activeTab === 'history'"
      class="tab-content"
    >
      <div
        v-if="loading"
        class="loading-state"
      >
        <el-icon class="loading-icon">
          <Loading />
        </el-icon>
        <p>加载中...</p>
      </div>
      <div
        v-else-if="error"
        class="error-state"
      >
        <p>{{ error }}</p>
        <el-button @click="loadPatterns">
          重试
        </el-button>
      </div>
      <div
        v-else
        class="history-container"
      >
        <div
          v-if="historyOrders.length === 0"
          class="empty-state"
        >
          <div class="empty-icon">
            📝
          </div>
          <p>暂无历史版型</p>
        </div>

        <div
          v-else
          class="history-list"
        >
          <div
            v-for="pattern in historyOrders"
            :key="pattern.id"
            class="history-card"
          >
            <div class="history-header">
              <span class="history-id">{{ pattern.name }}</span>
              <span :class="['history-status', getStatusClass(pattern.status)]">
                {{ getStatusText(pattern.status) }}
              </span>
            </div>
            <div class="history-info">
              <span>{{ getStyleLabel(pattern.category) }}</span>
              <span>{{ pattern.description }}</span>
            </div>
            <div class="history-date">
              创建时间：{{ pattern.createdAt }}
            </div>
            <div class="history-actions">
              <el-button
                size="small"
                @click="viewDetail(pattern.id)"
              >
                查看详情
              </el-button>
              <el-button
                size="small"
                @click="reorder(pattern.id)"
              >
                再次定制
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div
      v-if="activeTab === 'versions'"
      class="tab-content"
    >
      <div class="versions-container">
        <div class="versions-header">
          <h3>选择版型进行版本对比</h3>
          <p>支持最多同时对比三个版型版本</p>
        </div>

        <div class="pattern-selector">
          <div class="selector-item">
            <label>版型 A:</label>
            <el-select
              v-model="selectedPatterns[0]"
              placeholder="请选择版型"
              class="selector-width"
            >
              <el-option
                v-for="p in allPatterns"
                :key="p.id"
                :label="p.name"
                :value="p.id"
              />
            </el-select>
          </div>
          <div class="selector-item">
            <label>版型 B:</label>
            <el-select
              v-model="selectedPatterns[1]"
              placeholder="请选择版型"
              class="selector-width"
              :disabled="!selectedPatterns[0]"
            >
              <el-option
                v-for="p in allPatterns.filter(item => item.id !== selectedPatterns[0])"
                :key="p.id"
                :label="p.name"
                :value="p.id"
              />
            </el-select>
          </div>
          <div class="selector-item">
            <label>版型 C:</label>
            <el-select
              v-model="selectedPatterns[2]"
              placeholder="请选择版型（可选）"
              class="selector-width"
              :disabled="!selectedPatterns[1]"
            >
              <el-option
                v-for="p in allPatterns.filter(item => item.id !== selectedPatterns[0] && item.id !== selectedPatterns[1])"
                :key="p.id"
                :label="p.name"
                :value="p.id"
              />
            </el-select>
          </div>
        </div>

        <div class="compare-actions">
          <el-button
            type="primary"
            @click="startCompare"
            :disabled="selectedPatterns.filter(Boolean).length < 2"
          >
            开始对比
          </el-button>
          <el-button @click="clearSelection">清空选择</el-button>
        </div>

        <div
          v-if="compareResult.length > 0"
          class="compare-result"
        >
          <h4>版本对比结果</h4>
          <div class="compare-grid">
            <div
              v-for="(pattern, index) in compareResult"
              :key="pattern.id"
              class="compare-card"
            >
              <div class="compare-header">
                <span class="compare-label">版型 {{ ['A', 'B', 'C'][index] }}</span>
                <span class="compare-name">{{ pattern.name }}</span>
              </div>
              <div class="compare-image">
                <img :src="pattern.imageUrl" :alt="pattern.name" />
              </div>
              <div class="compare-details">
                <div class="compare-row">
                  <span class="compare-label">款式类型:</span>
                  <span class="compare-value">{{ getStyleLabel(pattern.category) }}</span>
                </div>
                <div class="compare-row">
                  <span class="compare-label">价格:</span>
                  <span class="compare-value">¥{{ pattern.price || 0 }}</span>
                </div>
                <div class="compare-row">
                  <span class="compare-label">状态:</span>
                  <span :class="['compare-status', getStatusClass(pattern.status)]">
                    {{ getStatusText(pattern.status) }}
                  </span>
                </div>
                <div class="compare-row">
                  <span class="compare-label">创建时间:</span>
                  <span class="compare-value">{{ pattern.createdAt }}</span>
                </div>
                <div class="compare-row">
                  <span class="compare-label">描述:</span>
                  <span class="compare-value">{{ pattern.description }}</span>
                </div>
              </div>
              <div class="compare-actions-card">
                <el-button size="small" @click="viewDetail(pattern.id)">查看详情</el-button>
              </div>
            </div>
          </div>

          <div class="compare-summary">
            <h4>对比总结</h4>
            <div class="summary-item">
              <span class="summary-label">价格差异:</span>
              <span class="summary-value">
                最低: ¥{{ Math.min(...compareResult.map(p => p.price || 0)) }} |
                最高: ¥{{ Math.max(...compareResult.map(p => p.price || 0)) }} |
                差价: ¥{{ Math.max(...compareResult.map(p => p.price || 0)) - Math.min(...compareResult.map(p => p.price || 0)) }}
              </span>
            </div>
            <div class="summary-item">
              <span class="summary-label">状态分布:</span>
              <span class="summary-value">
                {{ compareResult.filter(p => p.status === 2).length }} 已完成 |
                {{ compareResult.filter(p => p.status === 1).length }} 进行中 |
                {{ compareResult.filter(p => p.status === 0).length }} 草稿 |
                {{ compareResult.filter(p => p.status === 9).length }} 已取消
              </span>
            </div>
          </div>
        </div>

        <div
          v-if="selectedPatterns.filter(Boolean).length < 2 && compareResult.length === 0"
          class="empty-compare"
        >
          <div class="empty-icon">🔄</div>
          <p>请至少选择两个版型进行对比</p>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="showAgreement"
      title="服装纸样定制平台用户服务协议"
      width="800px"
    >
      <div class="agreement-content">
        <h3>第一条 总则与定义</h3>
        <p>1.1 协议构成：本协议包括本正文、附件及所有本平台已经发布或将来可能发布的各项规则、公告、通知、政策、声明。</p>
        <p>1.2 用户：指阅读并同意本协议，完成注册程序，或未经注册而直接使用本平台服务的主体。用户分为需求方（工厂）、服务方（版师）、访客。</p>
        <h3>第二条 账户注册与管理</h3>
        <p>2.1 资格：您确认，在注册成为本平台用户时，您是具有完全民事行为能力的自然人、法人或其他组织。</p>
        <p>2.2 信息真实：您有义务提供真实、准确、完整、合法的注册信息，并及时更新。</p>
        <h3>第三条 定制服务交易规则</h3>
        <p>3.1 需求发布：工厂应清晰、详尽、无歧义地描述定制需求，包括款式、尺寸、工艺、面料、预算范围、交付期限等。</p>
        <p>3.2 交易模式：公开招标或定向邀请。平台方仅为交易提供信息服务与技术支持。</p>
        <p>3.3 合同成立：当工厂在平台系统内选中特定版师的投标方案，并完成电子合同在线签署及首付款支付至平台担保账户后，视为合同正式成立并生效。</p>
        <h3>第四条 用户权利与义务</h3>
        <p>4.1 工厂有权要求版师按约定标准及期限交付工作成果，应按合同约定及时确认工作节点、提供必要反馈并支付款项。</p>
        <p>4.2 版师有权按合同约定获得劳动报酬，应保证其交付成果为原创，不侵犯任何第三方知识产权。</p>
        <h3>第五条 费用与支付</h3>
        <p>5.1 使用本平台基础服务免费。平台方有权就增值服务收取费用。</p>
        <p>5.2 平台服务费：就每一笔通过本平台完成的定制交易，平台方有权收取一定比例的技术服务费。</p>
        <h3>第六条 违约责任与争议处理</h3>
        <p>6.1 违约认定：任何用户违反本协议、电子合同或平台规则的行为，均构成违约。</p>
        <p>6.2 争议调解：交易双方因履约产生争议，应优先友好协商。协商不成的，任一方可向平台方申请争议调解。</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue';
import { ElMessage, ElForm } from 'element-plus';
import { Plus, Loading, Clock, Upload, Check, Close, Document } from '@element-plus/icons-vue';
import { getPatternList, getPatternDetail, createPattern, type Pattern, type CreatePatternRequest } from '@/api/pattern';

const activeTab = ref('create');
const loading = ref(false);
const submitLoading = ref(false);
const error = ref<string | null>(null);
const showAgreement = ref(false);

const customFormRef = ref<InstanceType<typeof ElForm> | null>(null);

const styleOptions = [
  { label: '休闲', value: 'casual' },
  { label: '商务', value: 'business' },
  { label: '运动', value: 'sport' },
  { label: '优雅', value: 'elegant' },
  { label: '复古', value: 'vintage' },
  { label: '时尚', value: 'fashion' },
];

const progressSteps = [
  { key: 'confirmed', name: '需求确认' },
  { key: 'draft', name: '初版设计' },
  { key: 'modify', name: '修改优化' },
  { key: 'delivery', name: '最终交付' },
];

const styleLabels: Record<string, string> = {
  dress: '连衣裙',
  top: '上衣',
  pants: '裤子',
  coat: '外套',
  skirt: '半身裙',
  shirt: '衬衫',
  other: '其他',
};

const uploadUrl = '/api/v1/upload';

const formRules = {
  styleType: [
    { required: true, message: '请选择款式类型', trigger: 'change' },
  ],
  patternStyle: [
    { required: true, message: '请选择版型风格', trigger: 'change' },
  ],
  targetAudience: [
    { required: true, message: '请选择目标人群', trigger: 'change' },
  ],
  description: [
    { min: 10, max: 1000, message: '描述长度应在10-1000个字符之间', trigger: 'blur' },
  ],
};

function getStyleLabel(category: string): string {
  return styleLabels[category] || category;
}

function getCurrentStepIndex(status: number): number {
  switch (status) {
    case 0:
      return 0;
    case 1:
      return 1;
    case 2:
      return 3;
    case 9:
      return -1;
    default:
      return 0;
  }
}

const customForm = reactive({
  styleType: '',
  patternStyle: '',
  targetAudience: '',
  minSize: null as number | null,
  maxSize: null as number | null,
  techRequirements: [] as string[],
  description: '',
  minBudget: null as number | null,
  maxBudget: null as number | null,
  deliveryDate: null as Date | null,
  isUrgent: false,
  agreed: false,
});

const imageFiles = ref<Record<string, unknown>[]>([]);

const orders = ref<Pattern[]>([]);
const historyOrders = ref<Pattern[]>([]);

const selectedPatterns = ref<(number | null)[]>([null, null, null]);
const compareResult = ref<Pattern[]>([]);

const allPatterns = computed<Pattern[]>(() => [
  ...orders.value,
  ...historyOrders.value,
]);

function getStatusClass(status: number): string {
  switch (status) {
    case 0:
      return 'draft';
    case 1:
      return 'confirmed';
    case 2:
      return 'completed';
    case 9:
      return 'cancelled';
    default:
      return '';
  }
}

function getStatusText(status: number): string {
  switch (status) {
    case 0:
      return '草稿';
    case 1:
      return '进行中';
    case 2:
      return '已完成';
    case 9:
      return '已取消';
    default:
      return '未知';
  }
}

function startCompare() {
  const selectedIds = selectedPatterns.value.filter(Boolean) as number[];
  if (selectedIds.length < 2) {
    ElMessage.warning('请至少选择两个版型进行对比');
    return;
  }
  compareResult.value = allPatterns.value.filter(
    (p) => selectedIds.includes(p.id),
  );
}

function clearSelection() {
  selectedPatterns.value = [null, null, null];
  compareResult.value = [];
}

const mockPatterns: Pattern[] = [
  {
    id: 1,
    name: '休闲连衣裙版型',
    category: 'dress',
    description: '适合夏季穿着的休闲连衣裙版型，宽松舒适',
    imageUrl: 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=400&h=300&fit=crop',
    price: 199,
    status: 1,
    createdAt: '2024-01-15 10:30:00',
    updatedAt: '2024-01-16 14:20:00',
  },
  {
    id: 2,
    name: '商务西装版型',
    category: 'coat',
    description: '经典商务西装版型，修身剪裁',
    imageUrl: 'https://images.unsplash.com/photo-1525450824786-227cbef70703?w=400&h=300&fit=crop',
    price: 399,
    status: 0,
    createdAt: '2024-01-14 09:00:00',
    updatedAt: '2024-01-14 09:00:00',
  },
  {
    id: 3,
    name: '牛仔裤版型',
    category: 'pants',
    description: '直筒牛仔裤版型，百搭经典',
    imageUrl: 'https://images.unsplash.com/photo-1542272604-787c3835535d?w=400&h=300&fit=crop',
    price: 159,
    status: 2,
    createdAt: '2024-01-10 11:00:00',
    updatedAt: '2024-01-12 16:00:00',
  },
  {
    id: 4,
    name: '衬衫版型',
    category: 'shirt',
    description: '商务休闲衬衫版型，舒适透气',
    imageUrl: 'https://images.unsplash.com/photo-1525450824786-227cbef70703?w=400&h=300&fit=crop',
    price: 129,
    status: 9,
    createdAt: '2024-01-08 14:00:00',
    updatedAt: '2024-01-09 10:00:00',
  },
];

async function loadPatterns() {
  loading.value = true;
  error.value = null;
  try {
    const data = await getPatternList({ pageNum: 1, pageSize: 50 });
    let list: Pattern[] = [];
    if (Array.isArray(data)) {
      list = data as Pattern[];
    } else if (data && typeof data === 'object' && 'records' in data) {
      list = (data as { records?: Pattern[] }).records || [];
    }
    orders.value = list.filter((p: Pattern) => p.status !== 2 && p.status !== 9);
    historyOrders.value = list.filter((p: Pattern) => p.status === 2 || p.status === 9);
  } catch (err: unknown) {
    console.warn('加载版型列表失败，使用mock数据:', err);
    orders.value = mockPatterns.filter((p) => p.status !== 2 && p.status !== 9);
    historyOrders.value = mockPatterns.filter((p) => p.status === 2 || p.status === 9);
  } finally {
    loading.value = false;
  }
}

async function viewDetail(id: number) {
  try {
    const res = await getPatternDetail(id) as { code?: number; data?: Pattern };
    if (res && res.code === 200) {
      ElMessage.success(`版型详情: ${res.data?.name || '未知'}`);
    } else {
      const pattern = allPatterns.value.find(p => p.id === id);
      if (pattern) {
        ElMessage.success(`版型详情: ${pattern.name}`);
      }
    }
  } catch (err: unknown) {
    const pattern = allPatterns.value.find(p => p.id === id);
    if (pattern) {
      ElMessage.success(`版型详情: ${pattern.name}`);
    } else {
      ElMessage.error((err as Error)?.message || '获取版型详情失败');
    }
  }
}

async function submitForm() {
  if (!customFormRef.value) return;
  
  try {
    const isValid = await customFormRef.value.validate();
    if (!isValid) return;
    
    submitLoading.value = true;
    
    const patternData: CreatePatternRequest = {
      styleType: customForm.styleType,
      patternStyle: customForm.patternStyle,
      targetAudience: customForm.targetAudience,
      minSize: customForm.minSize || undefined,
      maxSize: customForm.maxSize || undefined,
      techRequirements: customForm.techRequirements.length > 0 ? customForm.techRequirements : undefined,
      description: customForm.description || undefined,
      minBudget: customForm.minBudget || undefined,
      maxBudget: customForm.maxBudget || undefined,
      deliveryDate: customForm.deliveryDate ? formatDate(customForm.deliveryDate) : undefined,
      isUrgent: customForm.isUrgent,
    };
    
    await createPattern(patternData);
    
    ElMessage.success('定制需求提交成功！');
    activeTab.value = 'progress';
    loadPatterns();
    resetForm();
  } catch (err: unknown) {
    console.error('提交失败:', err);
    ElMessage.error((err as Error)?.message || '提交失败，请稍后重试');
  } finally {
    submitLoading.value = false;
  }
}

function formatDate(date: Date): string {
  const d = new Date(date);
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function resetForm() {
  Object.assign(customForm, {
    styleType: '',
    patternStyle: '',
    targetAudience: '',
    minSize: null,
    maxSize: null,
    techRequirements: [],
    description: '',
    minBudget: null,
    maxBudget: null,
    deliveryDate: null,
    isUrgent: false,
    agreed: false,
  });
  imageFiles.value = [];
  customFormRef.value?.resetFields();
}

function disabledDate(date: Date) {
  return date < new Date();
}

function handleUploadSuccess(_response: Record<string, unknown>, file: Record<string, unknown>) {
  imageFiles.value.push(file);
}

function handleUploadError(err: unknown) {
  console.error('上传失败:', err);
  ElMessage.error('文件上传失败，请重试');
}

function requestModify(id: number) {
  ElMessage.info(`申请修改版型 #${id}`);
}

function reorder(id: number) {
  const pattern = allPatterns.value.find(p => p.id === id);
  if (pattern) {
    customForm.styleType = pattern.category;
  }
  activeTab.value = 'create';
}

onMounted(() => {
  loadPatterns();
});
</script>

<style scoped>
.pattern-custom-view {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px;
}

.view-header {
  text-align: center;
  margin-bottom: 32px;
}

.page-title {
  font-size: 32px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 8px 0;
}

.page-subtitle {
  font-size: 16px;
  color: #64748b;
  margin: 0;
}

.tabs-container {
  margin-bottom: 24px;
}

.tabs {
  display: flex;
  gap: 8px;
  background: #f8fafc;
  padding: 6px;
  border-radius: 12px;
}

.tabs button {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #64748b;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.tabs button:hover {
  background: white;
  color: #1e293b;
}

.tabs button.active {
  background: #1d39c4;
  color: white;
}

.tab-content {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.form-container {
  max-width: 800px;
  margin: 0 auto;
}

.custom-form {
  padding: 16px 0;
}

.style-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.style-options button {
  padding: 8px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  background: white;
  color: #475569;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.style-options button:hover {
  border-color: #1d39c4;
  color: #1d39c4;
}

.style-options button.active {
  background: #1d39c4;
  color: white;
  border-color: #1d39c4;
}

.size-range,
.budget-range {
  display: flex;
  align-items: center;
  gap: 8px;
}

.size-range .el-input,
.budget-range .el-input {
  flex: 1;
}

.separator {
  color: #94a3b8;
}

.size-unit,
.budget-unit {
  color: #64748b;
  font-size: 14px;
}

.upload-area {
  width: 100%;
}

.uploader {
  width: 100%;
}

.upload-text {
  margin-top: 8px;
}

.upload-hint {
  margin-top: 4px;
  font-size: 12px;
  color: #94a3b8;
}

.agreement-section {
  margin-bottom: 16px;
}

.agreement-section a {
  color: #1d39c4;
  text-decoration: underline;
}

.form-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: 16px;
}

.progress-container,
.history-container {
  max-width: 100%;
}

.loading-state,
.error-state {
  text-align: center;
  padding: 60px 0;
}

.loading-icon {
  font-size: 32px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.error-state p {
  color: #dc2626;
  margin: 0 0 16px 0;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-state p {
  font-size: 16px;
  color: #64748b;
  margin: 0 0 20px 0;
}

.orders-list,
.history-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-card,
.history-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.order-header,
.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f5f9;
}

.order-info {
  display: flex;
  gap: 12px;
}

.order-id,
.history-id {
  font-size: 14px;
  color: #1e293b;
  font-weight: 500;
}

.order-type {
  padding: 4px 10px;
  background: #f1f5f9;
  border-radius: 4px;
  font-size: 12px;
  color: #64748b;
}

.order-status,
.history-status {
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 500;
}

.order-status.confirmed,
.history-status.completed {
  background: #dcfce7;
  color: #16a34a;
}

.order-status.draft {
  background: #dbeafe;
  color: #2563eb;
}

.order-status.cancelled,
.history-status.cancelled {
  background: #fee2e2;
  color: #dc2626;
}

.progress-timeline {
  display: flex;
  align-items: flex-start;
  margin-bottom: 20px;
  padding: 16px 0;
}

.timeline-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
  position: relative;
}

.timeline-dot {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #94a3b8;
  margin-bottom: 8px;
  z-index: 1;
  transition: all 0.3s;
}

.timeline-item.active .timeline-dot {
  background: #1d39c4;
  color: white;
}

.timeline-item.completed .timeline-dot {
  background: #10b981;
  color: white;
}

.timeline-item.cancelled .timeline-dot {
  background: #ef4444;
  color: white;
}

.timeline-content {
  text-align: center;
}

.step-name {
  display: block;
  font-size: 13px;
  color: #64748b;
}

.timeline-line {
  position: absolute;
  top: 16px;
  left: 50%;
  width: calc(100% - 16px);
  height: 2px;
  background: #e2e8f0;
  transform: translateX(50%);
  z-index: 0;
}

.timeline-item.completed ~ .timeline-item .timeline-line {
  background: #10b981;
}

.order-details {
  background: #f8fafc;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
}

.detail-row {
  display: flex;
  margin-bottom: 8px;
}

.detail-row:last-child {
  margin-bottom: 0;
}

.detail-label {
  color: #64748b;
  font-size: 14px;
  min-width: 80px;
}

.detail-value {
  color: #1e293b;
  font-size: 14px;
}

.order-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.history-info {
  display: flex;
  gap: 16px;
  margin-bottom: 8px;
}

.history-info span {
  font-size: 14px;
  color: #64748b;
}

.history-date {
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 12px;
}

.history-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.agreement-content {
  max-height: 500px;
  overflow-y: auto;
}

.agreement-content h3 {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 16px 0 8px 0;
}

.agreement-content p {
  font-size: 14px;
  color: #64748b;
  line-height: 1.8;
  margin: 0 0 8px 0;
  text-align: justify;
}

@media (max-width: 768px) {
  .progress-timeline {
    flex-direction: column;
    align-items: flex-start;
  }

  .timeline-item {
    flex-direction: row;
    align-items: center;
    gap: 12px;
  }

  .timeline-content {
    text-align: left;
  }

  .timeline-line {
    display: none;
  }

  .order-info {
    flex-direction: column;
    gap: 4px;
  }

  .order-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .history-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .history-info {
    flex-wrap: wrap;
  }
}

.versions-container {
  padding: 16px;
}

.versions-header {
  text-align: center;
  margin-bottom: 24px;
}

.versions-header h3 {
  font-size: 20px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 8px 0;
}

.versions-header p {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

.pattern-selector {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.selector-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.selector-item label {
  font-size: 14px;
  font-weight: 500;
  color: #475569;
}

.selector-width {
  width: 200px;
}

.compare-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-bottom: 32px;
}

.compare-result {
  margin-top: 24px;
}

.compare-result h4 {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 16px 0;
}

.compare-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.compare-card {
  background: white;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 2px solid #f1f5f9;
  transition: all 0.3s;
}

.compare-card:hover {
  border-color: #1d39c4;
  box-shadow: 0 4px 16px rgba(29, 57, 196, 0.1);
}

.compare-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f1f5f9;
}

.compare-label {
  padding: 4px 8px;
  background: #1d39c4;
  color: white;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.compare-name {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.compare-image {
  width: 100%;
  height: 180px;
  overflow: hidden;
  border-radius: 8px;
  margin-bottom: 12px;
}

.compare-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.compare-details {
  margin-bottom: 16px;
}

.compare-row {
  display: flex;
  margin-bottom: 8px;
}

.compare-row:last-child {
  margin-bottom: 0;
}

.compare-details .compare-label {
  background: transparent;
  color: #64748b;
  font-size: 13px;
  font-weight: 500;
  min-width: 70px;
}

.compare-details .compare-value {
  font-size: 13px;
  color: #1e293b;
  flex: 1;
  word-break: break-word;
}

.compare-status {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.compare-status.draft {
  background: #dbeafe;
  color: #2563eb;
}

.compare-status.confirmed {
  background: #dcfce7;
  color: #16a34a;
}

.compare-status.completed {
  background: #dcfce7;
  color: #16a34a;
}

.compare-status.cancelled {
  background: #fee2e2;
  color: #dc2626;
}

.compare-actions-card {
  display: flex;
  justify-content: center;
}

.compare-summary {
  background: #f8fafc;
  border-radius: 12px;
  padding: 20px;
}

.compare-summary h4 {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 16px 0;
}

.summary-item {
  display: flex;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;
}

.summary-item:last-child {
  margin-bottom: 0;
}

.summary-label {
  font-size: 14px;
  font-weight: 500;
  color: #64748b;
  min-width: 100px;
}

.summary-value {
  font-size: 14px;
  color: #1e293b;
}

.empty-compare {
  text-align: center;
  padding: 60px 0;
}

.empty-compare .empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-compare p {
  font-size: 16px;
  color: #64748b;
  margin: 0;
}

@media (max-width: 768px) {
  .pattern-selector {
    flex-direction: column;
    align-items: center;
    gap: 16px;
  }

  .selector-width {
    width: 100%;
    max-width: 280px;
  }

  .compare-grid {
    grid-template-columns: 1fr;
  }

  .compare-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .summary-item {
    flex-direction: column;
  }
}
</style>
