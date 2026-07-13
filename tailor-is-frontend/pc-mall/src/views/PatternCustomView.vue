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
          @click="activeTab = 'progress'"
        >
          <el-icon><Loading /></el-icon>
          <span>打版进度</span>
        </button>
        <button
          :class="{ active: activeTab === 'history' }"
          @click="activeTab = 'history'"
        >
          <el-icon><Clock /></el-icon>
          <span>历史订单</span>
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
        >
          <el-form-item
            label="款式类型"
            required
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
            required
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
            required
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

          <el-form-item
            label="尺寸范围"
            required
          >
            <div class="size-range">
              <input
                v-model.number="customForm.minSize"
                type="number"
                placeholder="最小码"
              >
              <span class="separator">-</span>
              <input
                v-model.number="customForm.maxSize"
                type="number"
                placeholder="最大码"
              >
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
                action="/api/upload"
                :on-success="handleUploadSuccess"
                :on-error="handleUploadError"
                :file-list="imageFiles"
                multiple
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

          <el-form-item label="详细描述">
            <el-textarea
              v-model="customForm.description"
              :rows="4"
              placeholder="请详细描述您的定制需求，包括版型特点、特殊要求、参考款式等..."
            />
          </el-form-item>

          <el-form-item label="预算范围">
            <div class="budget-range">
              <input
                v-model.number="customForm.minBudget"
                type="number"
                placeholder="最低预算"
              >
              <span class="separator">-</span>
              <input
                v-model.number="customForm.maxBudget"
                type="number"
                placeholder="最高预算"
              >
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

          <el-form-item>
            <div class="form-actions">
              <el-button
                type="primary"
                size="large"
                @click="submitForm"
              >
                提交定制需求
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
                <span class="order-type">{{ pattern.styleType }}</span>
              </div>
              <span :class="['order-status', pattern.status === 0 ? 'draft' : 'confirmed']">
                {{ pattern.status === 0 ? '草稿' : '进行中' }}
              </span>
            </div>

            <div class="progress-timeline">
              <div
                v-for="(step, index) in progressSteps"
                :key="step.key"
                :class="['timeline-item', { active: index <= 1, completed: index < 1 }]"
              >
                <div class="timeline-dot">
                  <el-icon v-if="index < 1">
                    <Check />
                  </el-icon>
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
                <span class="detail-value">¥{{ pattern.price }}</span>
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
                v-if="pattern.status !== 1"
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
              <span :class="['history-status', 'completed']">已完成</span>
            </div>
            <div class="history-info">
              <span>{{ pattern.styleType }}</span>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { Plus, Loading, Clock, Upload, Check } from '@element-plus/icons-vue';
import { getPatternList, getPatternDetail, type Pattern } from '@/api/pattern';

const activeTab = ref('create');
const loading = ref(false);
const error = ref<string | null>(null);

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
});

const imageFiles = ref<Record<string, unknown>[]>([]);

const orders = ref<Pattern[]>([]);
const historyOrders = ref<Pattern[]>([]);

/**
 * 加载版型列表数据
 * 将后端返回的版型数据映射为打版进度/历史订单展示
 */
async function loadPatterns() {
  loading.value = true;
  error.value = null;
  try {
    const res = await getPatternList({ pageNum: 1, pageSize: 50 });
    if (res && res.code === 200) {
      const list = res.data?.records || [];
      // 根据状态分流：非完成状态为进行中，已完成状态为历史订单
      orders.value = list.filter((p: Pattern) => p.status !== 2 && p.status !== 9);
      historyOrders.value = list.filter((p: Pattern) => p.status === 2 || p.status === 9);
    }
  } catch (err: unknown) {
    error.value = (err as Error)?.message || '加载版型数据失败';
    console.error('加载版型列表失败:', err);
  } finally {
    loading.value = false;
  }
}

/**
 * 查看版型详情
 */
async function viewDetail(id: number) {
  try {
    const res = await getPatternDetail(id);
    if (res && res.code === 200) {
      ElMessage.success(`版型详情: ${res.data?.name || '未知'}`);
    }
  } catch (err: unknown) {
    ElMessage.error((err as Error)?.message || '获取版型详情失败');
  }
}

function disabledDate(date: Date) {
  return date < new Date();
}

function handleUploadSuccess(response: Record<string, unknown>, file: Record<string, unknown>) {
  imageFiles.value.push(file);
}

function handleUploadError(err: unknown) {
  console.error('上传失败:', err);
  ElMessage.error('文件上传失败，请重试');
}

function submitForm() {
  // 表单提交后切换到进度tab，并重新加载数据
  activeTab.value = 'progress';
  loadPatterns();
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
  });
  imageFiles.value = [];
}

function requestModify(_id: number) {
  ElMessage.info(`申请修改版型 #${_id}`);
}

function reorder(_id: number) {
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

.size-range input,
.budget-range input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
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

.order-status.pending-modify {
  background: #fef3c7;
  color: #d97706;
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

.timeline-content {
  text-align: center;
}

.step-name {
  display: block;
  font-size: 13px;
  color: #64748b;
}

.step-time {
  display: block;
  font-size: 11px;
  color: #94a3b8;
  margin-top: 4px;
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

.versions-compare {
  margin-bottom: 16px;
  padding: 16px;
  background: #fef9c3;
  border-radius: 8px;
}

.versions-compare h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #92400e;
}

.versions-list {
  display: flex;
  gap: 16px;
}

.version-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.version-label {
  font-size: 13px;
  color: #1e293b;
  font-weight: 500;
}

.version-date {
  font-size: 12px;
  color: #64748b;
}

.compare-btn {
  padding: 4px 10px;
  background: #fbbf24;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  color: #92400e;
  cursor: pointer;
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
</style>
