<template>
  <div
    class="design-view"
    role="main"
    aria-label="服装设计"
  >
    <div class="view-header">
      <h1 class="page-title">
        服装设计
      </h1>
      <p class="page-subtitle">
        灵感库与协作工具，激发创意无限
      </p>
    </div>

    <div class="tabs-container">
      <div class="tabs">
        <button
          :class="{ active: activeTab === 'inspiration' }"
          @click="activeTab = 'inspiration'"
        >
          <el-icon><ReadingLamp /></el-icon>
          <span>灵感库</span>
        </button>
        <button
          :class="{ active: activeTab === 'collaboration' }"
          @click="activeTab = 'collaboration'"
        >
          <el-icon><User /></el-icon>
          <span>协作工具</span>
        </button>
        <button
          :class="{ active: activeTab === 'copyright' }"
          @click="activeTab = 'copyright'"
        >
          <el-icon><Stamp /></el-icon>
          <span>版权登记</span>
        </button>
      </div>
    </div>

    <div
      v-if="activeTab === 'inspiration'"
      class="tab-content"
    >
      <div class="search-section">
        <div class="search-box">
          <el-icon class="search-icon">
            <Search />
          </el-icon>
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="搜索灵感、款式、风格..."
            @keyup.enter="handleSearch"
          >
          <button
            class="search-btn"
            @click="handleSearch"
          >
            搜索
          </button>
        </div>
      </div>

      <div class="filter-section">
        <div class="filter-group">
          <span class="filter-label">风格分类</span>
          <div class="filter-tags">
            <button
              v-for="style in styleFilters"
              :key="style.value"
              :class="{ active: selectedStyles.includes(style.value) }"
              @click="toggleStyle(style.value)"
            >
              {{ style.label }}
            </button>
          </div>
        </div>
        <div class="filter-group">
          <span class="filter-label">灵感来源</span>
          <div class="filter-tags">
            <button
              v-for="source in sourceFilters"
              :key="source.value"
              :class="{ active: selectedSource === source.value }"
              @click="selectedSource = source.value"
            >
              {{ source.label }}
            </button>
          </div>
        </div>
        <div class="filter-group">
          <span class="filter-label">色彩趋势</span>
          <div class="color-picker">
            <button
              v-for="color in colorFilters"
              :key="color.value"
              :class="{ active: selectedColor === color.value }"
              :style="{ '--color': color.value }"
              :title="color.label"
              @click="selectedColor = color.value"
            />
          </div>
        </div>
      </div>

      <div class="inspiration-grid">
        <div
          v-for="item in inspirationItems"
          :key="item.id"
          class="inspiration-card"
          @click="viewDetail(item)"
        >
          <div class="card-image-wrapper">
            <img
              :src="item.image"
              :alt="item.title"
              class="card-image"
            >
            <div class="card-overlay">
              <button
                class="overlay-btn"
                @click.stop="saveInspiration(item.id)"
              >
                <el-icon><Star /></el-icon>
              </button>
            </div>
          </div>
          <div class="card-content">
            <h3 class="card-title">
              {{ item.title }}
            </h3>
            <p class="card-desc">
              {{ item.description }}
            </p>
            <div class="card-tags">
              <span
                v-for="tag in item.tags"
                :key="tag"
                class="tag"
              >{{ tag }}</span>
            </div>
            <div class="card-meta">
              <span class="author">{{ item.author }}</span>
              <span class="date">{{ item.date }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="totalCount"
          :page-sizes="[12, 24, 36]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handlePageSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <div
      v-if="activeTab === 'collaboration'"
      class="tab-content"
    >
      <div class="collab-container">
        <div class="projects-sidebar">
          <div class="sidebar-header">
            <h3>我的项目</h3>
            <el-button
              type="primary"
              size="small"
              @click="createProject"
            >
              <el-icon><Plus /></el-icon>
              新建项目
            </el-button>
          </div>
          <div class="project-list">
            <div
              v-for="project in projects"
              :key="project.id"
              :class="['project-item', { active: selectedProject?.id === project.id }]"
              @click="selectProject(project)"
            >
              <div class="project-icon">
                {{ project.icon }}
              </div>
              <div class="project-info">
                <span class="project-name">{{ project.name }}</span>
                <span class="project-members">{{ project.members.length }} 位成员</span>
              </div>
            </div>
          </div>
        </div>

        <div
          v-if="selectedProject"
          class="workspace"
        >
          <div class="workspace-header">
            <div class="workspace-title">
              <h2>{{ selectedProject.name }}</h2>
              <span class="project-status">{{ selectedProject.status }}</span>
            </div>
            <div class="workspace-actions">
              <el-button @click="inviteMember">
                <el-icon><CirclePlus /></el-icon>
                邀请成员
              </el-button>
              <el-button @click="openHistory">
                <el-icon><Clock /></el-icon>
                历史版本
              </el-button>
            </div>
          </div>

          <div class="members-bar">
            <div class="members-label">
              团队成员
            </div>
            <div class="members-list">
              <div
                v-for="member in selectedProject.members"
                :key="member.id"
                class="member-item"
              >
                <div class="member-avatar">
                  {{ member.avatar }}
                </div>
                <span class="member-name">{{ member.name }}</span>
                <span :class="['member-role', member.role]">{{ member.role }}</span>
              </div>
            </div>
          </div>

          <div class="editor-area">
            <div class="editor-toolbar">
              <button class="toolbar-btn">
                <el-icon><PenLine /></el-icon>
              </button>
              <button class="toolbar-btn">
                <el-icon><Brush /></el-icon>
              </button>
              <button class="toolbar-btn">
                <el-icon><RefreshLeft /></el-icon>
              </button>
              <button class="toolbar-btn">
                <el-icon><RefreshRight /></el-icon>
              </button>
              <div class="toolbar-divider" />
              <button class="toolbar-btn">
                <el-icon><ZoomIn /></el-icon>
              </button>
              <button class="toolbar-btn">
                <el-icon><ZoomOut /></el-icon>
              </button>
              <button class="toolbar-btn">
                <el-icon><FullScreen /></el-icon>
              </button>
            </div>

            <div class="editor-canvas">
              <div class="canvas-placeholder">
                <el-icon class="placeholder-icon">
                  <Picture />
                </el-icon>
                <p>点击上传设计稿或开始创作</p>
              </div>
            </div>
          </div>

          <div class="comments-section">
            <div class="comments-header">
              <h4>实时批注</h4>
            </div>
            <div class="comments-list">
              <div
                v-for="comment in selectedProject.comments"
                :key="comment.id"
                class="comment-item"
              >
                <div class="comment-avatar">
                  {{ comment.authorAvatar }}
                </div>
                <div class="comment-content">
                  <div class="comment-header">
                    <span class="comment-author">{{ comment.author }}</span>
                    <span class="comment-time">{{ comment.time }}</span>
                  </div>
                  <p class="comment-text">
                    {{ comment.text }}
                  </p>
                </div>
              </div>
            </div>
            <div class="comment-input">
              <input
                v-model="newComment"
                type="text"
                placeholder="添加批注..."
                @keyup.enter="addComment"
              >
              <button
                class="send-btn"
                @click="addComment"
              >
                <el-icon><Share /></el-icon>
              </button>
            </div>
          </div>
        </div>

        <div
          v-else
          class="empty-workspace"
        >
          <el-icon class="empty-icon">
            <FileText />
          </el-icon>
          <p>选择或创建一个项目开始协作</p>
        </div>
      </div>
    </div>

    <div
      v-if="activeTab === 'copyright'"
      class="tab-content"
    >
      <div class="copyright-container">
        <div class="copyright-intro">
          <h3>版权快速登记</h3>
          <p>对接区块链版权存证系统，一键完成设计作品版权登记，保护您的创意成果</p>
        </div>

        <div class="form-container">
          <el-form
            ref="copyrightFormRef"
            :model="copyrightForm"
            label-width="120px"
            class="copyright-form"
          >
            <el-form-item
              label="作品名称"
              required
            >
              <el-input
                v-model="copyrightForm.title"
                placeholder="请输入作品名称"
              />
            </el-form-item>

            <el-form-item
              label="作品类型"
              required
            >
              <el-select
                v-model="copyrightForm.type"
                placeholder="请选择作品类型"
              >
                <el-option
                  label="服装设计图"
                  value="fashion-design"
                />
                <el-option
                  label="纸样设计"
                  value="pattern-design"
                />
                <el-option
                  label="插画作品"
                  value="illustration"
                />
                <el-option
                  label="其他"
                  value="other"
                />
              </el-select>
            </el-form-item>

            <el-form-item
              label="创作时间"
              required
            >
              <el-date-picker
                v-model="copyrightForm.createDate"
                type="date"
                placeholder="请选择创作时间"
              />
            </el-form-item>

            <el-form-item label="作品描述">
              <el-textarea
                v-model="copyrightForm.description"
                :rows="3"
                placeholder="请简要描述作品内容..."
              />
            </el-form-item>

            <el-form-item label="作品文件">
              <div class="upload-area">
                <el-upload
                  class="uploader"
                  drag
                  action="/api/upload"
                  :on-success="handleUploadSuccess"
                  :on-error="handleUploadError"
                  :file-list="copyrightFiles"
                  accept=".jpg,.png,.pdf"
                >
                  <el-icon class="upload-icon">
                    <Upload />
                  </el-icon>
                  <div class="upload-text">
                    拖拽文件到此处，或<em>点击上传</em>
                  </div>
                  <div class="upload-hint">
                    支持 JPG、PNG、PDF 格式，单个文件不超过 20MB
                  </div>
                </el-upload>
              </div>
            </el-form-item>

            <el-form-item label="声明">
              <el-checkbox v-model="copyrightForm.declaration">
                我声明该作品为本人原创，不存在侵权行为，同意平台版权登记服务条款
              </el-checkbox>
            </el-form-item>

            <el-form-item>
              <div class="form-actions">
                <el-button
                  type="primary"
                  size="large"
                  @click="submitCopyright"
                >
                  <el-icon><Stamp /></el-icon>
                  提交版权登记
                </el-button>
              </div>
            </el-form-item>
          </el-form>
        </div>

        <div class="history-section">
          <h3>已登记作品</h3>
          <div
            v-if="copyrightHistory.length === 0"
            class="empty-state"
          >
            <div class="empty-icon">
              📄
            </div>
            <p>暂无已登记作品</p>
          </div>
          <div
            v-else
            class="history-list"
          >
            <div
              v-for="item in copyrightHistory"
              :key="item.id"
              class="history-card"
            >
              <div class="history-header">
                <span class="history-title">{{ item.title }}</span>
                <span :class="['history-status', item.status]">{{ item.statusText }}</span>
              </div>
              <div class="history-info">
                <span>登记时间：{{ item.registerTime }}</span>
                <span>存证编号：{{ item.certificateNo }}</span>
              </div>
              <div class="history-actions">
                <el-button
                  size="small"
                  @click="viewCertificate(item.id)"
                >
                  查看证书
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import {
  ReadingLamp,
  User,
  Stamp,
  Search,
  Star,
  Plus,
  CirclePlus,
  Clock,
  Brush,
  RefreshLeft,
  RefreshRight,
  ZoomIn,
  ZoomOut,
  FullScreen,
  Picture,
  Share,
  Upload,
} from '@element-plus/icons-vue';

const activeTab = ref('inspiration');
const searchKeyword = ref('');
const selectedStyles = ref<string[]>([]);
const selectedSource = ref('');
const selectedColor = ref('');
const currentPage = ref(1);
const pageSize = ref(12);
const totalCount = ref(48);

const newComment = ref('');
const copyrightFiles = ref<Record<string, unknown>[]>([]);

const styleFilters = [
  { label: '休闲', value: 'casual' },
  { label: '商务', value: 'business' },
  { label: '运动', value: 'sport' },
  { label: '优雅', value: 'elegant' },
  { label: '复古', value: 'vintage' },
  { label: '时尚', value: 'fashion' },
  { label: '极简', value: 'minimal' },
];

const sourceFilters = [
  { label: '全部', value: '' },
  { label: '时尚秀场', value: 'fashion-show' },
  { label: '街拍灵感', value: 'street-style' },
  { label: '艺术作品', value: 'art' },
  { label: '自然风景', value: 'nature' },
  { label: '历史文化', value: 'history' },
];

const colorFilters = [
  { label: '全部', value: '' },
  { label: '红色系', value: '#ef4444' },
  { label: '橙色系', value: '#f97316' },
  { label: '黄色系', value: '#eab308' },
  { label: '绿色系', value: '#22c55e' },
  { label: '蓝色系', value: '#3b82f6' },
  { label: '紫色系', value: '#a855f7' },
  { label: '粉色系', value: '#ec4899' },
  { label: '黑白灰', value: '#6b7280' },
];

const inspirationItems = ref([
  {
    id: 1,
    title: '春季花卉主题系列',
    description: '灵感来源于春日花园，运用淡雅色彩和花卉图案...',
    image: 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=400&h=300&fit=crop',
    tags: ['春季', '花卉', '优雅'],
    author: '设计师小美',
    date: '2024-01-15',
  },
  {
    id: 2,
    title: '极简主义都市风格',
    description: '简约线条与几何造型的完美结合，适合现代都市生活...',
    image: 'https://images.unsplash.com/photo-1525450824786-227cbef70703?w=400&h=300&fit=crop',
    tags: ['极简', '都市', '时尚'],
    author: '设计师阿杰',
    date: '2024-01-14',
  },
  {
    id: 3,
    title: '复古波西米亚风',
    description: '融合民族元素与现代设计，展现独特的艺术气息...',
    image: 'https://images.unsplash.com/photo-1542272604-787c3835535d?w=400&h=300&fit=crop',
    tags: ['复古', '波西米亚', '民族'],
    author: '设计师莉莉',
    date: '2024-01-13',
  },
  {
    id: 4,
    title: '运动休闲新风尚',
    description: '功能性与时尚感的平衡，适合运动与日常穿搭...',
    image: 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=400&h=300&fit=crop',
    tags: ['运动', '休闲', '舒适'],
    author: '设计师大卫',
    date: '2024-01-12',
  },
  {
    id: 5,
    title: '海洋蓝色系列',
    description: '从深海汲取灵感，运用层次丰富的蓝色调...',
    image: 'https://images.unsplash.com/photo-1525450824786-227cbef70703?w=400&h=300&fit=crop',
    tags: ['海洋', '蓝色', '清新'],
    author: '设计师艾米',
    date: '2024-01-11',
  },
  {
    id: 6,
    title: '未来科技感设计',
    description: '金属质感与流线型设计，展现未来时尚趋势...',
    image: 'https://images.unsplash.com/photo-1542272604-787c3835535d?w=400&h=300&fit=crop',
    tags: ['科技', '未来', '前卫'],
    author: '设计师卡尔',
    date: '2024-01-10',
  },
]);

const projects = ref([
  {
    id: 1,
    name: '2024春季新品系列',
    icon: '🌸',
    status: '进行中',
    members: [
      { id: 1, name: '小美', avatar: '美', role: '负责人' },
      { id: 2, name: '阿杰', avatar: '杰', role: '设计师' },
      { id: 3, name: '莉莉', avatar: '莉', role: '打版师' },
    ],
    comments: [
      { id: 1, author: '小美', authorAvatar: '美', time: '10分钟前', text: '整体配色方案很好，领口设计可以再优化一下' },
      { id: 2, author: '阿杰', authorAvatar: '杰', time: '25分钟前', text: '好的，我调整一下领口的弧度' },
      { id: 3, author: '莉莉', authorAvatar: '莉', time: '1小时前', text: '版型图已经更新，请查看' },
    ],
  },
  {
    id: 2,
    name: '联名限定款',
    icon: '🎨',
    status: '待审核',
    members: [
      { id: 1, name: '大卫', avatar: '卫', role: '负责人' },
      { id: 2, name: '艾米', avatar: '米', role: '设计师' },
    ],
    comments: [],
  },
  {
    id: 3,
    name: '经典款改良',
    icon: '✨',
    status: '已完成',
    members: [{ id: 1, name: '卡尔', avatar: '卡', role: '负责人' }],
    comments: [],
  },
]);

const selectedProject = ref<(typeof projects.value)[0] | null>(null);

const copyrightForm = reactive({
  title: '',
  type: '',
  createDate: null,
  description: '',
  declaration: false,
});

const copyrightHistory = ref([
  {
    id: 1,
    title: '春季花卉主题系列',
    status: 'completed',
    statusText: '已完成',
    registerTime: '2024-01-10 14:30',
    certificateNo: 'BC202401100001',
  },
  {
    id: 2,
    title: '极简主义都市风格',
    status: 'pending',
    statusText: '审核中',
    registerTime: '2024-01-15 09:00',
    certificateNo: 'BC202401150002',
  },
]);

function toggleStyle(style: string) {
  const index = selectedStyles.value.indexOf(style);
  if (index > -1) {
    selectedStyles.value.splice(index, 1);
  } else {
    selectedStyles.value.push(style);
  }
}

function handleSearch() {
  currentPage.value = 1;
}

function handlePageSizeChange(val: number) {
  pageSize.value = val;
  currentPage.value = 1;
}

function handlePageChange(val: number) {
  currentPage.value = val;
}

function viewDetail(item: (typeof inspirationItems.value)[0]) {
  console.log('查看详情:', item);
}

function saveInspiration(id: number) {
  console.log('收藏灵感:', id);
}

function createProject() {
  console.log('创建项目');
}

function selectProject(project: (typeof projects.value)[0]) {
  selectedProject.value = project;
}

function inviteMember() {
  console.log('邀请成员');
}

function openHistory() {
  console.log('打开历史版本');
}

function addComment() {
  if (!newComment.value.trim()) return;
  console.log('添加批注:', newComment.value);
  newComment.value = '';
}

function handleUploadSuccess(response: Record<string, unknown>, file: Record<string, unknown>) {
  copyrightFiles.value.push(file);
}

function handleUploadError(err: unknown) {
  console.error('上传失败:', err);
}

function submitCopyright() {
  console.log('提交版权登记');
}

function viewCertificate(id: number) {
  console.log('查看证书:', id);
}
</script>

<style scoped>
.design-view {
  max-width: 1400px;
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

.search-section {
  margin-bottom: 24px;
}

.search-box {
  display: flex;
  max-width: 600px;
  margin: 0 auto;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
}

.search-icon {
  padding: 12px 16px;
  color: #94a3b8;
}

.search-box input {
  flex: 1;
  border: none;
  padding: 12px;
  font-size: 14px;
  outline: none;
}

.search-btn {
  background: #1d39c4;
  color: white;
  border: none;
  padding: 0 24px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
}

.filter-section {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f1f5f9;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-label {
  font-size: 14px;
  color: #64748b;
  font-weight: 500;
}

.filter-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-tags button {
  padding: 6px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  background: white;
  color: #475569;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.filter-tags button:hover {
  border-color: #1d39c4;
  color: #1d39c4;
}

.filter-tags button.active {
  background: #1d39c4;
  color: white;
  border-color: #1d39c4;
}

.color-picker {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.color-picker button {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 2px solid #e2e8f0;
  cursor: pointer;
  transition: all 0.2s;
}

.color-picker button:first-child {
  background: transparent;
}

.color-picker button:not(:first-child) {
  background: var(--color);
}

.color-picker button:hover {
  transform: scale(1.1);
}

.color-picker button.active {
  border-color: #1d39c4;
  transform: scale(1.15);
}

.inspiration-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.inspiration-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.inspiration-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.card-image-wrapper {
  position: relative;
  padding-top: 75%;
  overflow: hidden;
}

.card-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.card-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.3);
  opacity: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: opacity 0.2s;
}

.inspiration-card:hover .card-overlay {
  opacity: 1;
}

.overlay-btn {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: white;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #1d39c4;
  cursor: pointer;
  transition: transform 0.2s;
}

.overlay-btn:hover {
  transform: scale(1.1);
}

.card-content {
  padding: 16px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 8px 0;
}

.card-desc {
  font-size: 13px;
  color: #64748b;
  margin: 0 0 12px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

.tag {
  padding: 3px 8px;
  background: #f1f5f9;
  color: #64748b;
  font-size: 11px;
  border-radius: 4px;
}

.card-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.author,
.date {
  font-size: 12px;
  color: #94a3b8;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 40px;
}

.collab-container {
  display: flex;
  gap: 20px;
  height: calc(100vh - 300px);
}

.projects-sidebar {
  width: 280px;
  flex-shrink: 0;
  background: #f8fafc;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e2e8f0;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.project-list {
  flex: 1;
  overflow-y: auto;
}

.project-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 8px;
}

.project-item:hover {
  background: white;
}

.project-item.active {
  background: #1d39c4;
}

.project-icon {
  font-size: 24px;
}

.project-info {
  flex: 1;
  min-width: 0;
}

.project-name {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.project-item.active .project-name {
  color: white;
}

.project-members {
  display: block;
  font-size: 12px;
  color: #94a3b8;
}

.project-item.active .project-members {
  color: rgba(255, 255, 255, 0.8);
}

.workspace {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 12px;
  overflow: hidden;
}

.workspace-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f1f5f9;
}

.workspace-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.workspace-title h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
}

.project-status {
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.project-status.进行中 {
  background: #dbeafe;
  color: #2563eb;
}

.project-status.待审核 {
  background: #fef3c7;
  color: #d97706;
}

.project-status.已完成 {
  background: #dcfce7;
  color: #16a34a;
}

.workspace-actions {
  display: flex;
  gap: 8px;
}

.members-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.members-label {
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
}

.members-list {
  display: flex;
  gap: 16px;
}

.member-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.member-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #1d39c4;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 500;
}

.member-name {
  font-size: 13px;
  color: #1e293b;
}

.member-role {
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
}

.member-role.负责人 {
  background: #dbeafe;
  color: #2563eb;
}

.member-role.设计师 {
  background: #fef3c7;
  color: #d97706;
}

.member-role.打版师 {
  background: #dcfce7;
  color: #16a34a;
}

.editor-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 16px;
}

.editor-toolbar {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 12px;
  background: #f8fafc;
  border-radius: 8px;
  margin-bottom: 12px;
}

.toolbar-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
}

.toolbar-btn:hover {
  background: white;
  color: #1e293b;
}

.toolbar-divider {
  width: 1px;
  height: 24px;
  background: #e2e8f0;
  margin: 0 8px;
}

.editor-canvas {
  flex: 1;
  background: #fafafa;
  border-radius: 8px;
  border: 2px dashed #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.canvas-placeholder {
  text-align: center;
}

.placeholder-icon {
  font-size: 48px;
  color: #cbd5e1;
  margin-bottom: 12px;
}

.canvas-placeholder p {
  font-size: 14px;
  color: #94a3b8;
  margin: 0;
}

.comments-section {
  padding: 16px 20px;
  border-top: 1px solid #f1f5f9;
  background: #f8fafc;
}

.comments-header h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.comments-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 12px;
}

.comment-item {
  display: flex;
  gap: 10px;
}

.comment-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #1d39c4;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  flex-shrink: 0;
}

.comment-content {
  flex: 1;
  background: white;
  padding: 10px 12px;
  border-radius: 8px;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.comment-author {
  font-size: 13px;
  font-weight: 500;
  color: #1e293b;
}

.comment-time {
  font-size: 11px;
  color: #94a3b8;
}

.comment-text {
  font-size: 13px;
  color: #475569;
  margin: 0;
}

.comment-input {
  display: flex;
  gap: 8px;
}

.comment-input input {
  flex: 1;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 13px;
  outline: none;
}

.send-btn {
  width: 40px;
  height: 40px;
  border: none;
  background: #1d39c4;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  cursor: pointer;
}

.empty-workspace {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: white;
  border-radius: 12px;
}

.empty-icon {
  font-size: 64px;
  color: #cbd5e1;
  margin-bottom: 16px;
}

.empty-workspace p {
  font-size: 16px;
  color: #64748b;
  margin: 0;
}

.copyright-container {
  max-width: 800px;
  margin: 0 auto;
}

.copyright-intro {
  text-align: center;
  margin-bottom: 32px;
  padding: 24px;
  background: #f8fafc;
  border-radius: 12px;
}

.copyright-intro h3 {
  margin: 0 0 8px 0;
  font-size: 20px;
  font-weight: 600;
  color: #1e293b;
}

.copyright-intro p {
  margin: 0;
  font-size: 14px;
  color: #64748b;
}

.form-container {
  margin-bottom: 32px;
}

.copyright-form {
  padding: 16px 0;
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
  justify-content: center;
  margin-top: 16px;
}

.history-section {
  padding-top: 24px;
  border-top: 1px solid #f1f5f9;
}

.history-section h3 {
  margin: 0 0 16px 0;
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
}

.empty-state {
  text-align: center;
  padding: 40px 0;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.empty-state p {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.history-card {
  background: #f8fafc;
  border-radius: 8px;
  padding: 16px;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.history-title {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
}

.history-status {
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.history-status.已完成 {
  background: #dcfce7;
  color: #16a34a;
}

.history-status.审核中 {
  background: #fef3c7;
  color: #d97706;
}

.history-info {
  display: flex;
  gap: 20px;
  margin-bottom: 12px;
}

.history-info span {
  font-size: 13px;
  color: #64748b;
}

.history-actions {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 1024px) {
  .inspiration-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .collab-container {
    flex-direction: column;
    height: auto;
  }

  .projects-sidebar {
    width: 100%;
    height: 200px;
  }
}

@media (max-width: 600px) {
  .inspiration-grid {
    grid-template-columns: 1fr;
  }

  .filter-section {
    flex-direction: column;
  }

  .workspace-header {
    flex-direction: column;
    gap: 12px;
  }

  .members-bar {
    flex-wrap: wrap;
  }

  .members-list {
    flex-wrap: wrap;
  }
}
</style>
