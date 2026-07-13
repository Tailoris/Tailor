<template>
  <div
    class="knowledge-view"
    role="main"
    aria-label="知识库"
  >
    <div class="view-header">
      <h1 class="page-title">
        知识库
      </h1>
      <p class="page-subtitle">
        专业知识与问答悬赏，助力成长提升
      </p>
    </div>

    <div class="tabs-container">
      <div class="tabs">
        <button
          :class="{ active: activeTab === 'library' }"
          @click="activeTab = 'library'"
        >
          <el-icon><Reading /></el-icon>
          <span>知识文库</span>
        </button>
        <button
          :class="{ active: activeTab === 'qa' }"
          @click="activeTab = 'qa'"
        >
          <el-icon><ChatLineSquare /></el-icon>
          <span>问答社区</span>
        </button>
        <button
          :class="{ active: activeTab === 'rewards' }"
          @click="activeTab = 'rewards'"
        >
          <el-icon><Coin /></el-icon>
          <span>悬赏问答</span>
        </button>
      </div>
    </div>

    <div
      v-if="activeTab === 'library'"
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
            placeholder="搜索知识、技巧、教程..."
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

      <div class="content-layout">
        <aside class="sidebar">
          <div class="sidebar-section">
            <h3 class="sidebar-title">
              知识分类
            </h3>
            <ul class="category-tree">
              <li
                v-for="category in categories"
                :key="category.id"
                class="category-item"
              >
                <div
                  :class="['category-header', { active: selectedCategory === category.id }]"
                  @click="selectCategory(category.id)"
                >
                  <el-icon
                    v-if="category.children?.length"
                    class="expand-icon"
                  >
                    <ArrowRight />
                  </el-icon>
                  <span>{{ category.name }}</span>
                  <span class="count">{{ category.count }}</span>
                </div>
                <ul
                  v-if="category.children?.length"
                  class="sub-categories"
                >
                  <li
                    v-for="sub in category.children"
                    :key="sub.id"
                    :class="['sub-category', { active: selectedSubCategory === sub.id }]"
                    @click.stop="selectSubCategory(sub.id)"
                  >
                    <span>{{ sub.name }}</span>
                    <span class="count">{{ sub.count }}</span>
                  </li>
                </ul>
              </li>
            </ul>
          </div>

          <div class="sidebar-section">
            <h3 class="sidebar-title">
              热门标签
            </h3>
            <div class="tag-cloud">
              <button
                v-for="tag in hotTags"
                :key="tag.name"
                :style="{ fontSize: tag.size + 'px' }"
                @click="handleTagClick(tag.name)"
              >
                {{ tag.name }}
              </button>
            </div>
          </div>
        </aside>

        <main class="main-content">
          <div class="content-tabs">
            <button
              :class="{ active: contentTab === 'all' }"
              @click="contentTab = 'all'"
            >
              全部
            </button>
            <button
              :class="{ active: contentTab === 'article' }"
              @click="contentTab = 'article'"
            >
              文章
            </button>
            <button
              :class="{ active: contentTab === 'video' }"
              @click="contentTab = 'video'"
            >
              视频
            </button>
          </div>

          <div class="articles-list">
            <div
              v-for="article in articles"
              :key="article.id"
              class="article-card"
              @click="viewArticle(article.id)"
            >
              <div
                v-if="article.type === 'video'"
                class="article-video"
              >
                <img
                  :src="article.cover"
                  :alt="article.title"
                  class="video-cover"
                >
                <div class="video-overlay">
                  <div class="play-btn">
                    <el-icon><VideoPlay /></el-icon>
                  </div>
                  <span class="video-duration">{{ article.duration }}</span>
                </div>
              </div>
              <div
                v-else
                class="article-image"
                :style="{ backgroundImage: `url(${article.cover})` }"
              />

              <div class="article-content">
                <div class="article-tags">
                  <span
                    v-for="tag in article.tags"
                    :key="tag"
                    class="tag"
                  >{{ tag }}</span>
                  <span
                    v-if="article.type === 'video'"
                    class="video-tag"
                  >视频</span>
                </div>
                <h3 class="article-title">
                  {{ article.title }}
                </h3>
                <p class="article-desc">
                  {{ article.description }}
                </p>
                <div class="article-meta">
                  <span class="author">
                    <el-icon><User /></el-icon>
                    {{ article.author }}
                  </span>
                  <span class="views">
                    <el-icon><View /></el-icon>
                    {{ article.views }}
                  </span>
                  <span class="likes">
                    <el-icon><Star /></el-icon>
                    {{ article.likes }}
                  </span>
                  <span class="date">{{ article.date }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :total="totalCount"
              :page-sizes="[10, 20, 30]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handlePageSizeChange"
              @current-change="handlePageChange"
            />
          </div>
        </main>
      </div>
    </div>

    <div
      v-if="activeTab === 'qa'"
      class="tab-content"
    >
      <div class="qa-container">
        <div class="qa-header">
          <div class="search-box">
            <el-icon class="search-icon">
              <Search />
            </el-icon>
            <input
              v-model="qaSearch"
              type="text"
              placeholder="搜索问题..."
              @keyup.enter="searchQA"
            >
            <button
              class="search-btn"
              @click="searchQA"
            >
              搜索
            </button>
          </div>
          <el-button
            type="primary"
            @click="askQuestion"
          >
            <el-icon><Plus /></el-icon>
            提问
          </el-button>
        </div>

        <div class="qa-stats">
          <div class="stat-item">
            <span class="stat-value">{{ stats.total }}</span>
            <span class="stat-label">问题总数</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ stats.solved }}</span>
            <span class="stat-label">已解决</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ stats.experts }}</span>
            <span class="stat-label">专家数</span>
          </div>
        </div>

        <div class="qa-filter">
          <button
            :class="{ active: qaFilter === 'latest' }"
            @click="qaFilter = 'latest'"
          >
            最新
          </button>
          <button
            :class="{ active: qaFilter === 'hot' }"
            @click="qaFilter = 'hot'"
          >
            热门
          </button>
          <button
            :class="{ active: qaFilter === 'unsolved' }"
            @click="qaFilter = 'unsolved'"
          >
            未解决
          </button>
          <button
            :class="{ active: qaFilter === 'solved' }"
            @click="qaFilter = 'solved'"
          >
            已解决
          </button>
        </div>

        <div class="questions-list">
          <div
            v-for="question in questions"
            :key="question.id"
            class="question-card"
            @click="viewQuestion(question.id)"
          >
            <div class="question-votes">
              <div class="vote-item">
                <span class="vote-count">{{ question.votes }}</span>
                <span class="vote-label">赞同</span>
              </div>
              <div class="vote-item">
                <span class="vote-count">{{ question.answers }}</span>
                <span class="vote-label">回答</span>
              </div>
            </div>

            <div class="question-content">
              <h3 class="question-title">
                {{ question.title }}
              </h3>
              <p class="question-desc">
                {{ question.description }}
              </p>
              <div class="question-tags">
                <span
                  v-for="tag in question.tags"
                  :key="tag"
                  class="tag"
                >{{ tag }}</span>
              </div>
              <div class="question-meta">
                <span class="author">
                  <el-icon><User /></el-icon>
                  {{ question.author }}
                </span>
                <span class="time">{{ question.time }}</span>
                <span
                  v-if="question.accepted"
                  class="accepted"
                >
                  <el-icon><CircleCheckFilled /></el-icon>
                  已采纳
                </span>
              </div>
            </div>
          </div>
        </div>

        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="qaPage"
            v-model:page-size="qaPageSize"
            :total="qaTotal"
            :page-sizes="[10, 20, 30]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleQaPageSizeChange"
            @current-change="handleQaPageChange"
          />
        </div>
      </div>
    </div>

    <div
      v-if="activeTab === 'rewards'"
      class="tab-content"
    >
      <div class="rewards-container">
        <div class="rewards-header">
          <h3>悬赏问答</h3>
          <p>设置悬赏金，吸引专业人士解答您的疑难问题</p>
        </div>

        <div class="rewards-filter">
          <button
            :class="{ active: rewardFilter === 'all' }"
            @click="rewardFilter = 'all'"
          >
            全部
          </button>
          <button
            :class="{ active: rewardFilter === 'highest' }"
            @click="rewardFilter = 'highest'"
          >
            赏金最高
          </button>
          <button
            :class="{ active: rewardFilter === 'ending' }"
            @click="rewardFilter = 'ending'"
          >
            即将结束
          </button>
        </div>

        <div class="rewards-list">
          <div
            v-for="reward in rewards"
            :key="reward.id"
            class="reward-card"
            @click="viewReward(reward.id)"
          >
            <div class="reward-bounty">
              <span class="bounty-label">悬赏</span>
              <span class="bounty-amount">¥{{ reward.bounty }}</span>
            </div>

            <div class="reward-content">
              <h3 class="reward-title">
                {{ reward.title }}
              </h3>
              <p class="reward-desc">
                {{ reward.description }}
              </p>
              <div class="reward-tags">
                <span
                  v-for="tag in reward.tags"
                  :key="tag"
                  class="tag"
                >{{ tag }}</span>
              </div>
              <div class="reward-meta">
                <span class="author">
                  <el-icon><User /></el-icon>
                  {{ reward.author }}
                </span>
                <span class="time">剩余 {{ reward.daysLeft }} 天</span>
                <span class="answers">{{ reward.answers }} 人已回答</span>
              </div>
            </div>

            <div class="reward-status">
              <span :class="['status', reward.status]">{{ reward.statusText }}</span>
            </div>
          </div>
        </div>

        <div class="create-reward">
          <h3>发布悬赏问题</h3>
          <el-form
            ref="rewardFormRef"
            :model="rewardForm"
            label-width="100px"
            class="reward-form"
          >
            <el-form-item
              label="问题标题"
              required
            >
              <el-input
                v-model="rewardForm.title"
                placeholder="请输入问题标题"
              />
            </el-form-item>
            <el-form-item
              label="问题描述"
              required
            >
              <el-textarea
                v-model="rewardForm.description"
                :rows="4"
                placeholder="请详细描述您的问题..."
              />
            </el-form-item>
            <el-form-item label="悬赏金额">
              <div class="bounty-options">
                <button
                  v-for="amount in bountyOptions"
                  :key="amount"
                  :class="{ active: rewardForm.bounty === amount }"
                  @click="rewardForm.bounty = amount"
                >
                  ¥{{ amount }}
                </button>
              </div>
              <el-input
                v-model.number="rewardForm.bounty"
                type="number"
                placeholder="自定义金额"
                class="custom-bounty"
              />
            </el-form-item>
            <el-form-item label="截止时间">
              <el-select
                v-model="rewardForm.days"
                placeholder="请选择截止时间"
              >
                <el-option
                  label="3天"
                  :value="3"
                />
                <el-option
                  label="7天"
                  :value="7"
                />
                <el-option
                  label="14天"
                  :value="14"
                />
                <el-option
                  label="30天"
                  :value="30"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="标签">
              <el-select
                v-model="rewardForm.tags"
                multiple
                placeholder="请选择标签"
              >
                <el-option
                  label="设计基础"
                  value="设计基础"
                />
                <el-option
                  label="打版技巧"
                  value="打版技巧"
                />
                <el-option
                  label="工艺详解"
                  value="工艺详解"
                />
                <el-option
                  label="设备操作"
                  value="设备操作"
                />
                <el-option
                  label="面料知识"
                  value="面料知识"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                size="large"
                @click="submitReward"
              >
                发布悬赏
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import {
  Reading,
  ChatLineSquare,
  Coin,
  Search,
  ArrowRight,
  VideoPlay,
  User,
  View,
  Star,
  Plus,
  CircleCheckFilled,
} from '@element-plus/icons-vue';

const activeTab = ref('library');
const searchKeyword = ref('');
const contentTab = ref('all');
const selectedCategory = ref<number | null>(null);
const selectedSubCategory = ref<number | null>(null);
const currentPage = ref(1);
const pageSize = ref(10);
const totalCount = ref(86);

const qaSearch = ref('');
const qaFilter = ref('latest');
const qaPage = ref(1);
const qaPageSize = ref(10);
const qaTotal = ref(156);

const rewardFilter = ref('all');

const categories = ref([
  {
    id: 1,
    name: '设计基础',
    count: 234,
    children: [
      { id: 11, name: '色彩理论', count: 45 },
      { id: 12, name: '款式设计', count: 67 },
      { id: 13, name: '设计原理', count: 89 },
      { id: 14, name: '设计软件', count: 33 },
    ],
  },
  {
    id: 2,
    name: '打版技巧',
    count: 189,
    children: [
      { id: 21, name: '原型裁剪', count: 56 },
      { id: 22, name: '立体裁剪', count: 43 },
      { id: 23, name: '版型调整', count: 67 },
      { id: 24, name: '放码技术', count: 23 },
    ],
  },
  {
    id: 3,
    name: '工艺详解',
    count: 156,
    children: [
      { id: 31, name: '缝纫工艺', count: 78 },
      { id: 32, name: '熨烫工艺', count: 34 },
      { id: 33, name: '整烫工艺', count: 23 },
      { id: 34, name: '特殊工艺', count: 21 },
    ],
  },
  {
    id: 4,
    name: '设备操作',
    count: 89,
    children: [
      { id: 41, name: '缝纫机操作', count: 34 },
      { id: 42, name: '裁剪机操作', count: 23 },
      { id: 43, name: '熨烫设备', count: 18 },
      { id: 44, name: 'CAD系统', count: 14 },
    ],
  },
]);

const hotTags = ref([
  { name: '原型裁剪', size: 16 },
  { name: '色彩搭配', size: 14 },
  { name: '缝纫技巧', size: 18 },
  { name: '版型调整', size: 15 },
  { name: '设计灵感', size: 17 },
  { name: '面料选择', size: 13 },
  { name: '工艺详解', size: 16 },
  { name: 'CAD打版', size: 14 },
]);

const articles = ref([
  {
    id: 1,
    title: '服装设计色彩搭配的基本原则',
    description: '色彩是服装设计中最具表现力的元素之一，掌握色彩搭配的基本原则对于创造出色的设计作品至关重要...',
    cover: 'https://images.unsplash.com/photo-1525450824786-227cbef70703?w=400&h=250&fit=crop',
    type: 'article',
    tags: ['设计基础', '色彩理论'],
    author: '设计师小美',
    views: 2340,
    likes: 156,
    date: '2024-01-15',
  },
  {
    id: 2,
    title: '原型裁剪入门教程：从零开始学打版',
    description: '原型裁剪是服装打版的基础方法，本视频详细讲解了原型的制作过程和应用技巧...',
    cover: 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=400&h=250&fit=crop',
    type: 'video',
    duration: '25:30',
    tags: ['打版技巧', '原型裁剪'],
    author: '资深版师老王',
    views: 5678,
    likes: 423,
    date: '2024-01-14',
  },
  {
    id: 3,
    title: '高级定制工艺详解：手工缝制技巧',
    description: '高级定制服装注重细节和品质，手工缝制是其核心工艺之一...',
    cover: 'https://images.unsplash.com/photo-1542272604-787c3835535d?w=400&h=250&fit=crop',
    type: 'article',
    tags: ['工艺详解', '缝纫工艺'],
    author: '工艺大师李师傅',
    views: 1890,
    likes: 123,
    date: '2024-01-13',
  },
  {
    id: 4,
    title: '服装CAD软件操作指南',
    description: '现代服装打版离不开CAD软件，本教程带你快速掌握常用CAD软件的基本操作...',
    cover: 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=400&h=250&fit=crop',
    type: 'video',
    duration: '38:15',
    tags: ['设备操作', 'CAD系统'],
    author: '技术达人小张',
    views: 3456,
    likes: 289,
    date: '2024-01-12',
  },
]);

const stats = ref({
  total: 1256,
  solved: 987,
  experts: 156,
});

const questions = ref([
  {
    id: 1,
    title: '请问如何解决连衣裙领口变形的问题？',
    description: '我在制作连衣裙时，领口总是容易变形，试了很多方法都没解决，想请教各位大师有什么好的解决方案...',
    tags: ['打版技巧', '版型调整'],
    author: '新手裁缝',
    votes: 45,
    answers: 8,
    time: '2小时前',
    accepted: true,
  },
  {
    id: 2,
    title: '什么面料适合做夏季连衣裙？',
    description: '想制作夏季连衣裙，不知道选什么面料比较好，要求透气舒适，不易皱...',
    tags: ['面料知识', '设计基础'],
    author: '服装设计爱好者',
    votes: 23,
    answers: 12,
    time: '5小时前',
    accepted: false,
  },
  {
    id: 3,
    title: '立体裁剪和平面裁剪哪个更适合新手？',
    description: '作为服装制作新手，纠结于学习立体裁剪还是平面裁剪，想听听大家的建议...',
    tags: ['打版技巧', '裁剪方法'],
    author: '初学者小李',
    votes: 67,
    answers: 15,
    time: '1天前',
    accepted: true,
  },
  {
    id: 4,
    title: '如何调整大码服装的版型？',
    description: '接了一个大码服装的订单，不知道如何调整版型才能既合身又美观...',
    tags: ['打版技巧', '版型调整'],
    author: '独立设计师',
    votes: 34,
    answers: 5,
    time: '2天前',
    accepted: false,
  },
]);

const rewards = ref([
  {
    id: 1,
    title: '急求：特殊体型纸样调整方案',
    description: '客户体型特殊，普通纸样无法适配，需要专业的纸样调整方案，能提供详细方案者可追加赏金...',
    tags: ['打版技巧', '版型调整'],
    author: '服装工作室',
    bounty: 500,
    daysLeft: 3,
    answers: 3,
    status: 'active',
    statusText: '进行中',
  },
  {
    id: 2,
    title: '高端定制工艺难题求解',
    description: '在制作高端定制服装时遇到了工艺难题，需要专业人士指导解决...',
    tags: ['工艺详解', '高级定制'],
    author: '定制店老板',
    bounty: 300,
    daysLeft: 1,
    answers: 5,
    status: 'ending',
    statusText: '即将结束',
  },
  {
    id: 3,
    title: 'AI服装设计软件使用教程',
    description: '想学习AI服装设计软件，但找不到系统的教程，希望有人能提供详细的使用指南...',
    tags: ['设计软件', 'AI设计'],
    author: '设计专业学生',
    bounty: 100,
    daysLeft: 7,
    answers: 2,
    status: 'active',
    statusText: '进行中',
  },
]);

const bountyOptions = [50, 100, 200, 500, 1000];

const rewardForm = reactive({
  title: '',
  description: '',
  bounty: 100,
  days: 7,
  tags: [] as string[],
});

function selectCategory(id: number) {
  selectedCategory.value = selectedCategory.value === id ? null : id;
}

function selectSubCategory(id: number) {
  selectedSubCategory.value = selectedSubCategory.value === id ? null : id;
}

function handleTagClick(tag: string) {
  searchKeyword.value = tag;
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

function viewArticle(id: number) {
  console.log('查看文章:', id);
}

function searchQA() {
  qaPage.value = 1;
}

function askQuestion() {
  console.log('提问');
}

function viewQuestion(id: number) {
  console.log('查看问题:', id);
}

function handleQaPageSizeChange(val: number) {
  qaPageSize.value = val;
  qaPage.value = 1;
}

function handleQaPageChange(val: number) {
  qaPage.value = val;
}

function viewReward(id: number) {
  console.log('查看悬赏:', id);
}

function submitReward() {
  console.log('发布悬赏:', rewardForm);
}
</script>

<style scoped>
.knowledge-view {
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

.content-layout {
  display: flex;
  gap: 24px;
}

.sidebar {
  width: 260px;
  flex-shrink: 0;
}

.sidebar-section {
  background: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.sidebar-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 16px 0;
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f5f9;
}

.category-tree {
  list-style: none;
  padding: 0;
  margin: 0;
}

.category-item {
  margin-bottom: 4px;
}

.category-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  cursor: pointer;
  color: #475569;
  font-size: 14px;
  transition: color 0.2s;
}

.category-header:hover,
.category-header.active {
  color: #1d39c4;
}

.expand-icon {
  font-size: 12px;
  margin-right: 8px;
}

.count {
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  color: #64748b;
}

.sub-categories {
  list-style: none;
  padding: 0;
  margin: 0 0 8px 20px;
}

.sub-category {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  cursor: pointer;
  color: #64748b;
  font-size: 13px;
  transition: color 0.2s;
}

.sub-category:hover,
.sub-category.active {
  color: #1d39c4;
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-cloud button {
  padding: 6px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  background: white;
  color: #475569;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.tag-cloud button:hover {
  border-color: #1d39c4;
  color: #1d39c4;
}

.main-content {
  flex: 1;
  min-width: 0;
}

.content-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
}

.content-tabs button {
  padding: 8px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: white;
  color: #64748b;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.content-tabs button:hover {
  border-color: #1d39c4;
  color: #1d39c4;
}

.content-tabs button.active {
  background: #1d39c4;
  color: white;
  border-color: #1d39c4;
}

.articles-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.article-card {
  display: flex;
  gap: 16px;
  background: white;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.article-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.article-video,
.article-image {
  width: 200px;
  height: 125px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
}

.video-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.video-overlay {
  position: relative;
  top: -100%;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.4);
}

.play-btn {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #1d39c4;
  cursor: pointer;
}

.video-duration {
  position: absolute;
  bottom: 8px;
  right: 8px;
  padding: 2px 6px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  font-size: 12px;
  border-radius: 4px;
}

.article-image {
  background-size: cover;
  background-position: center;
}

.article-content {
  flex: 1;
  min-width: 0;
}

.article-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}

.tag {
  padding: 3px 8px;
  background: #f1f5f9;
  color: #64748b;
  font-size: 11px;
  border-radius: 4px;
}

.video-tag {
  background: #fef3c7;
  color: #d97706;
}

.article-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 8px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.article-desc {
  font-size: 13px;
  color: #64748b;
  margin: 0 0 12px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-meta {
  display: flex;
  align-items: center;
  gap: 16px;
}

.author,
.views,
.likes,
.date {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #94a3b8;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 40px;
}

.qa-container {
  max-width: 100%;
}

.qa-header {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
}

.qa-header .search-box {
  flex: 1;
  margin: 0;
  max-width: none;
}

.qa-stats {
  display: flex;
  gap: 32px;
  padding: 20px;
  background: #f8fafc;
  border-radius: 12px;
  margin-bottom: 20px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 28px;
  font-weight: 700;
  color: #1d39c4;
}

.stat-label {
  font-size: 14px;
  color: #64748b;
}

.qa-filter {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
}

.qa-filter button {
  padding: 8px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: white;
  color: #64748b;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.qa-filter button:hover {
  border-color: #1d39c4;
  color: #1d39c4;
}

.qa-filter button.active {
  background: #1d39c4;
  color: white;
  border-color: #1d39c4;
}

.questions-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.question-card {
  display: flex;
  gap: 16px;
  background: white;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.question-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.question-votes {
  display: flex;
  flex-direction: column;
  gap: 8px;
  text-align: center;
}

.vote-item {
  display: flex;
  flex-direction: column;
}

.vote-count {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
}

.vote-label {
  font-size: 12px;
  color: #94a3b8;
}

.question-content {
  flex: 1;
  min-width: 0;
}

.question-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 8px 0;
}

.question-desc {
  font-size: 13px;
  color: #64748b;
  margin: 0 0 12px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.question-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

.question-meta {
  display: flex;
  align-items: center;
  gap: 16px;
}

.accepted {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #10b981;
}

.rewards-container {
  max-width: 100%;
}

.rewards-header {
  text-align: center;
  margin-bottom: 24px;
}

.rewards-header h3 {
  margin: 0 0 8px 0;
  font-size: 20px;
  font-weight: 600;
  color: #1e293b;
}

.rewards-header p {
  margin: 0;
  font-size: 14px;
  color: #64748b;
}

.rewards-filter {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
}

.rewards-filter button {
  padding: 8px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: white;
  color: #64748b;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.rewards-filter button:hover {
  border-color: #1d39c4;
  color: #1d39c4;
}

.rewards-filter button.active {
  background: #1d39c4;
  color: white;
  border-color: #1d39c4;
}

.rewards-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 32px;
}

.reward-card {
  display: flex;
  gap: 16px;
  background: white;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.reward-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.reward-bounty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 20px;
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  border-radius: 8px;
  flex-shrink: 0;
}

.bounty-label {
  font-size: 12px;
  color: #92400e;
}

.bounty-amount {
  font-size: 24px;
  font-weight: 700;
  color: #d97706;
}

.reward-content {
  flex: 1;
  min-width: 0;
}

.reward-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 8px 0;
}

.reward-desc {
  font-size: 13px;
  color: #64748b;
  margin: 0 0 10px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.reward-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

.reward-meta {
  display: flex;
  align-items: center;
  gap: 16px;
}

.reward-status {
  flex-shrink: 0;
}

.status {
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.status.active {
  background: #dbeafe;
  color: #2563eb;
}

.status.ending {
  background: #fee2e2;
  color: #dc2626;
}

.create-reward {
  padding-top: 24px;
  border-top: 1px solid #f1f5f9;
}

.create-reward h3 {
  margin: 0 0 16px 0;
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
}

.reward-form {
  padding: 16px 0;
}

.bounty-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.bounty-options button {
  padding: 8px 16px;
  border: 2px solid #e2e8f0;
  border-radius: 8px;
  background: white;
  color: #475569;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.bounty-options button:hover {
  border-color: #1d39c4;
  color: #1d39c4;
}

.bounty-options button.active {
  border-color: #f59e0b;
  background: #fef3c7;
  color: #d97706;
}

.custom-bounty {
  width: 200px;
}

@media (max-width: 1024px) {
  .content-layout {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
  }

  .article-card {
    flex-direction: column;
  }

  .article-video,
  .article-image {
    width: 100%;
    height: 200px;
  }
}

@media (max-width: 768px) {
  .qa-header {
    flex-direction: column;
  }

  .qa-stats {
    gap: 16px;
  }

  .question-card {
    flex-direction: column;
  }

  .question-votes {
    flex-direction: row;
    justify-content: space-around;
    padding-bottom: 12px;
    border-bottom: 1px solid #f1f5f9;
  }

  .reward-card {
    flex-direction: column;
  }

  .reward-bounty {
    flex-direction: row;
    justify-content: center;
    gap: 12px;
  }
}
</style>
