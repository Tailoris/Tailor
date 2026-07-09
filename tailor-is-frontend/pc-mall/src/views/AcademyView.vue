<template>
  <div class="academy-view" role="main" aria-label="线上学堂">
    <div class="view-header">
      <h1 class="page-title">线上学堂</h1>
      <p class="page-subtitle">课程学习与证书认证，成就专业梦想</p>
    </div>

    <div class="tabs-container">
      <div class="tabs">
        <button :class="{ active: activeTab === 'courses' }" @click="activeTab = 'courses'">
          <el-icon><Reading /></el-icon>
          <span>课程中心</span>
        </button>
        <button :class="{ active: activeTab === 'training' }" @click="activeTab = 'training'">
          <el-icon><Lightning /></el-icon>
          <span>实战训练营</span>
        </button>
        <button :class="{ active: activeTab === 'certificates' }" @click="activeTab = 'certificates'">
          <el-icon><Medal /></el-icon>
          <span>证书认证</span>
        </button>
      </div>
    </div>

    <div v-if="activeTab === 'courses'" class="tab-content">
      <div v-if="loading" class="loading-state">
        <el-icon class="loading-icon"><Loading /></el-icon>
        <p>加载课程中...</p>
      </div>
      <div v-else-if="error" class="error-state">
        <p>{{ error }}</p>
        <el-button @click="loadCourses">重试</el-button>
      </div>
      <template v-else>
      <div class="search-section">
        <div class="search-box">
          <el-icon class="search-icon"><Search /></el-icon>
          <input v-model="searchKeyword" type="text" placeholder="搜索课程..." @keyup.enter="handleSearch" />
          <button class="search-btn" @click="handleSearch">搜索</button>
        </div>
      </div>

      <div class="level-tabs">
        <button v-for="level in levels" :key="level.value"
          :class="['level-btn', level.value, { active: selectedLevel === level.value }]"
          @click="selectedLevel = level.value">
          <span class="level-icon">{{ level.icon }}</span>
          <span class="level-name">{{ level.name }}</span>
          <span class="level-count">{{ level.count }}门课程</span>
        </button>
      </div>

      <div class="filter-bar">
        <div class="filter-group">
          <span class="filter-label">分类</span>
          <div class="filter-tags">
            <button :class="{ active: selectedCategory === '' }" @click="selectedCategory = ''">全部</button>
            <button v-for="cat in categories" :key="cat.value"
              :class="{ active: selectedCategory === cat.value }"
              @click="selectedCategory = cat.value">{{ cat.label }}</button>
          </div>
        </div>
        <div class="filter-group">
          <span class="filter-label">排序</span>
          <select v-model="sortBy" @change="handleSort">
            <option value="popular">最受欢迎</option>
            <option value="newest">最新上线</option>
            <option value="price_asc">价格从低到高</option>
            <option value="price_desc">价格从高到低</option>
          </select>
        </div>
      </div>

      <div class="courses-grid">
        <div v-for="course in courses" :key="course.id" class="course-card" @click="viewCourse(course.id)">
          <div class="course-image-wrapper">
            <img :src="course.image" :alt="course.title" class="course-image" />
            <div class="course-badge" v-if="course.badge">{{ course.badge }}</div>
            <div class="course-duration">{{ course.duration }}</div>
          </div>
          <div class="course-content">
            <div class="course-tags">
              <span :class="['level-tag', course.level]">{{ getLevelName(course.level) }}</span>
              <span v-for="tag in course.tags" :key="tag" class="tag">{{ tag }}</span>
            </div>
            <h3 class="course-title">{{ course.title }}</h3>
            <p class="course-desc">{{ course.description }}</p>
            <div class="course-meta">
              <span class="instructor">
                <el-icon><User /></el-icon>
                {{ course.instructor }}
              </span>
              <span class="students">
                <el-icon><User /></el-icon>
                {{ course.students }}人学习
              </span>
            </div>
            <div class="course-rating">
              <div class="star-rating">
                <el-icon v-for="n in 5" :key="n" class="star" :class="{ filled: n <= course.rating }">
                  <Star />
                </el-icon>
              </div>
              <span class="rating-text">{{ course.rating.toFixed(1) }}</span>
            </div>
            <div class="course-price">
              <span v-if="course.price > 0" class="price">¥{{ course.price }}</span>
              <span v-else class="price free">免费</span>
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
      </template>
    </div>

    <div v-if="activeTab === 'training'" class="tab-content">
      <div class="training-container">
        <div class="training-header">
          <h3>实战训练营</h3>
          <p>由行业导师带队，完成真实项目案例，快速提升实战能力</p>
        </div>

        <div class="training-list">
          <div v-for="camp in trainings" :key="camp.id" class="training-card">
            <div class="training-image">
              <img :src="camp.image" :alt="camp.title" />
              <div class="training-badge" v-if="camp.status === 'ongoing'">进行中</div>
              <div class="training-badge upcoming" v-else>即将开始</div>
            </div>

            <div class="training-content">
              <div class="training-tags">
                <span class="duration-tag">{{ camp.duration }}</span>
                <span class="level-tag">{{ camp.level }}</span>
              </div>
              <h3 class="training-title">{{ camp.title }}</h3>
              <p class="training-desc">{{ camp.description }}</p>

              <div class="mentor-section">
                <div class="mentor-avatar">{{ camp.mentorAvatar }}</div>
                <div class="mentor-info">
                  <span class="mentor-name">{{ camp.mentorName }}</span>
                  <span class="mentor-title">{{ camp.mentorTitle }}</span>
                </div>
              </div>

              <div class="training-features">
                <div class="feature-item">
                  <el-icon><Clock /></el-icon>
                  <span>{{ camp.sessions }}节课</span>
                </div>
                <div class="feature-item">
                  <el-icon><User /></el-icon>
                  <span>{{ camp.slots }}个名额</span>
                </div>
                <div class="feature-item">
                  <el-icon><ChatLineSquare /></el-icon>
                  <span>一对一指导</span>
                </div>
              </div>

              <div class="training-price">
                <span class="price">¥{{ camp.price }}</span>
                <span class="original-price" v-if="camp.originalPrice">¥{{ camp.originalPrice }}</span>
              </div>

              <button :class="['enroll-btn', { disabled: camp.status === 'upcoming' }]" @click="enrollTraining(camp.id)">
                {{ camp.status === 'ongoing' ? '立即报名' : '预约报名' }}
              </button>
            </div>
          </div>
        </div>

        <div class="training-stats">
          <div class="stat-card">
            <el-icon class="stat-icon"><School /></el-icon>
            <div class="stat-content">
              <span class="stat-value">5000+</span>
              <span class="stat-label">学员已参与</span>
            </div>
          </div>
          <div class="stat-card">
            <el-icon class="stat-icon"><Trophy /></el-icon>
            <div class="stat-content">
              <span class="stat-value">98%</span>
              <span class="stat-label">好评率</span>
            </div>
          </div>
          <div class="stat-card">
            <el-icon class="stat-icon"><Briefcase /></el-icon>
            <div class="stat-content">
              <span class="stat-value">85%</span>
              <span class="stat-label">就业转化率</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="activeTab === 'certificates'" class="tab-content">
      <div class="certificates-container">
        <div class="certificates-header">
          <h3>证书认证体系</h3>
          <p>与行业协会合作，完成特定课程并通过考核后，颁发行业认可的技能证书</p>
        </div>

        <div class="certificates-grid">
          <div v-for="cert in certificates" :key="cert.id" class="cert-card">
            <div class="cert-icon">{{ cert.icon }}</div>
            <h3 class="cert-title">{{ cert.title }}</h3>
            <p class="cert-desc">{{ cert.description }}</p>
            <div class="cert-requirements">
              <h4>获取条件</h4>
              <ul>
                <li v-for="req in cert.requirements" :key="req">{{ req }}</li>
              </ul>
            </div>
            <div class="cert-benefits">
              <h4>证书权益</h4>
              <ul>
                <li v-for="benefit in cert.benefits" :key="benefit">{{ benefit }}</li>
              </ul>
            </div>
            <button class="apply-btn" @click="applyCert(cert.id)">申请认证</button>
          </div>
        </div>

        <div class="my-certificates">
          <h3>我的证书</h3>
          <div v-if="myCerts.length === 0" class="empty-state">
            <div class="empty-icon">📜</div>
            <p>暂无获得的证书</p>
            <el-button type="primary" @click="activeTab = 'courses'">开始学习</el-button>
          </div>
          <div v-else class="my-certs-list">
            <div v-for="cert in myCerts" :key="cert.id" class="my-cert-card">
              <div class="cert-badge-icon">{{ cert.icon }}</div>
              <div class="cert-info">
                <h4>{{ cert.title }}</h4>
                <span class="cert-date">获得时间：{{ cert.date }}</span>
                <span class="cert-number">证书编号：{{ cert.number }}</span>
              </div>
              <button class="view-btn">查看证书</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Reading, Lightning, Medal, Search, User, Star, Clock, ChatLineSquare, School, Trophy, Briefcase, Loading } from '@element-plus/icons-vue'
import { getCourseList, getCourseDetail, getCourseChapters, type Course } from '@/api/academy'

const activeTab = ref('courses')
const searchKeyword = ref('')
const selectedLevel = ref('all')
const selectedCategory = ref('')
const sortBy = ref('popular')
const currentPage = ref(1)
const pageSize = ref(12)
const totalCount = ref(0)
const loading = ref(false)
const error = ref<string | null>(null)

const levels = [
  { value: 'all', name: '全部', icon: '📚', count: 0 },
  { value: 'beginner', name: '入门', icon: '🌱', count: 0 },
  { value: 'intermediate', name: '进阶', icon: '📈', count: 0 },
  { value: 'advanced', name: '高级', icon: '⭐', count: 0 },
  { value: 'master', name: '大师', icon: '👑', count: 0 }
]

const categories = [
  { label: '设计基础', value: 'design' },
  { label: '打版技术', value: 'pattern' },
  { label: '工艺实操', value: 'craft' },
  { label: '面料知识', value: 'fabric' },
  { label: 'CAD软件', value: 'cad' },
  { label: '行业趋势', value: 'trend' }
]

/**
 * 课程展示数据（映射后端Course到前端展示格式）
 */
interface CourseDisplay {
  id: number
  title: string
  description: string
  image: string
  level: string
  tags: string[]
  instructor: string
  students: number
  rating: number
  price: number
  duration: string
  badge: string
}

const courses = ref<CourseDisplay[]>([])

// 训练营和证书暂为静态数据（后端API待扩展）
const trainings = ref([
  {
    id: 1,
    title: '独立设计师实战训练营',
    description: '从设计创意到成品落地，全程指导完成个人品牌系列设计项目...',
    image: 'https://images.unsplash.com/photo-1525450824786-227cbef70703?w=600&h=400&fit=crop',
    mentorName: '陈大师',
    mentorTitle: '知名设计师',
    mentorAvatar: '陈',
    duration: '4周',
    level: '高级',
    sessions: 24,
    slots: 30,
    price: 3999,
    originalPrice: 4999,
    status: 'ongoing'
  },
  {
    id: 2,
    title: '服装打版精英训练营',
    description: '系统学习各类版型打版技术，完成10个真实订单案例实操...',
    image: 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=600&h=400&fit=crop',
    mentorName: '王师傅',
    mentorTitle: '资深版师',
    mentorAvatar: '王',
    duration: '6周',
    level: '进阶',
    sessions: 36,
    slots: 25,
    price: 4999,
    originalPrice: 5999,
    status: 'ongoing'
  },
  {
    id: 3,
    title: '3D服装设计实战营',
    description: '掌握3D服装设计软件，实现虚拟试衣和数字化展示...',
    image: 'https://images.unsplash.com/photo-1542272604-787c3835535d?w=600&h=400&fit=crop',
    mentorName: '李老师',
    mentorTitle: '技术总监',
    mentorAvatar: '李',
    duration: '5周',
    level: '高级',
    sessions: 30,
    slots: 20,
    price: 3499,
    originalPrice: null,
    status: 'upcoming'
  }
])

const certificates = ref([
  {
    id: 1,
    title: '服装设计师初级认证',
    icon: '🎨',
    description: '证明具备服装设计基础能力，可独立完成简单款式设计',
    requirements: ['完成3门入门级设计课程', '通过线上理论考核', '提交1份设计作品'],
    benefits: ['平台认证标识', '优先推荐设计订单', '入驻设计师专区']
  },
  {
    id: 2,
    title: '服装打版师中级认证',
    icon: '📐',
    description: '证明具备独立打版能力，可完成各类版型的打版工作',
    requirements: ['完成5门打版相关课程', '通过实操技能考核', '完成3个真实打版订单'],
    benefits: ['平台认证标识', '打版师星级评定加分', '专属接单通道']
  },
  {
    id: 3,
    title: '高级定制工艺师认证',
    icon: '👑',
    description: '证明具备高级定制工艺能力，可承接高端定制项目',
    requirements: ['完成大师级课程', '通过专家评审', '提交3份定制作品'],
    benefits: ['平台认证标识', '大师认证标签', '优先推荐高端订单', '专属技术支持']
  }
])

const myCerts = ref([
  { id: 1, title: '服装设计师初级认证', icon: '🎨', date: '2024-01-15', number: 'CDA20240115001' }
])

/**
 * 将后端Course数据映射为前端展示格式
 */
function mapCourseToDisplay(course: Course): CourseDisplay {
  const levelMap: Record<number, string> = { 0: 'beginner', 1: 'intermediate', 2: 'advanced', 3: 'master' }
  const categoryMap: Record<number, string> = { 1: '设计基础', 2: '打版技术', 3: '工艺实操', 4: '面料知识', 5: 'CAD软件', 6: '行业趋势' }
  return {
    id: course.id,
    title: course.title,
    description: course.description || '',
    image: course.coverImage || 'https://images.unsplash.com/photo-1525450824786-227cbef70703?w=400&h=250&fit=crop',
    level: levelMap[course.categoryId] || 'beginner',
    tags: [categoryMap[course.categoryId] || '设计基础'],
    instructor: `讲师 #${course.authorId}`,
    students: 0,
    rating: 4.5,
    price: 0,
    duration: course.duration ? `${course.duration}分钟` : '未知',
    badge: course.status === 1 ? '热门' : ''
  }
}

/**
 * 加载课程列表
 */
async function loadCourses() {
  loading.value = true
  error.value = null
  try {
    const res = await getCourseList({ pageNum: currentPage.value, pageSize: pageSize.value })
    if (res && res.code === 200) {
      const list = Array.isArray(res.data) ? res.data : (res.data?.records || [])
      courses.value = list.map(mapCourseToDisplay)
      totalCount.value = res.data?.total || list.length
    }
  } catch (err: any) {
    error.value = err?.message || '加载课程数据失败'
    console.error('加载课程列表失败:', err)
  } finally {
    loading.value = false
  }
}

/**
 * 查看课程详情
 */
async function viewCourse(id: number) {
  try {
    const [courseRes, chaptersRes] = await Promise.all([
      getCourseDetail(id),
      getCourseChapters(id)
    ])
    if (courseRes && courseRes.code === 200) {
      const course = courseRes.data
      const chapters = chaptersRes?.data || []
      ElMessage.success(`课程: ${course?.title}, 共 ${chapters.length} 个章节`)
    }
  } catch (err: any) {
    ElMessage.error(err?.message || '获取课程详情失败')
  }
}

function getLevelName(level: string) {
  const levelMap: Record<string, string> = {
    beginner: '入门',
    intermediate: '进阶',
    advanced: '高级',
    master: '大师'
  }
  return levelMap[level] || level
}

function handleSearch() {
  currentPage.value = 1
  loadCourses()
}

function handleSort() {
  currentPage.value = 1
  loadCourses()
}

function handlePageSizeChange(val: number) {
  pageSize.value = val
  currentPage.value = 1
  loadCourses()
}

function handlePageChange(val: number) {
  currentPage.value = val
  loadCourses()
}

function enrollTraining(id: number) {
  ElMessage.info(`报名训练营 #${id}`)
}

function applyCert(id: number) {
  ElMessage.info(`申请认证 #${id}`)
}

// 监听分页变化重新加载
watch([currentPage, pageSize], () => {
  if (activeTab.value === 'courses') {
    loadCourses()
  }
})

onMounted(() => {
  loadCourses()
})
</script>

<style scoped>
.academy-view {
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

.level-tabs {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  overflow-x: auto;
  padding-bottom: 8px;
}

.level-btn {
  flex: 1;
  min-width: 140px;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  background: white;
  cursor: pointer;
  transition: all 0.2s;
}

.level-btn:hover {
  border-color: #cbd5e1;
}

.level-btn.active {
  border-color: #1d39c4;
  background: #f0f5ff;
}

.level-btn.beginner.active {
  border-color: #22c55e;
  background: #f0fdf4;
}

.level-btn.intermediate.active {
  border-color: #3b82f6;
  background: #eff6ff;
}

.level-btn.advanced.active {
  border-color: #8b5cf6;
  background: #f5f3ff;
}

.level-btn.master.active {
  border-color: #f59e0b;
  background: #fffbeb;
}

.level-icon {
  font-size: 28px;
  margin-bottom: 8px;
}

.level-name {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.level-count {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 4px;
}

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 12px;
}

.filter-group {
  display: flex;
  align-items: center;
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

.filter-group select {
  padding: 6px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
  cursor: pointer;
}

.courses-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.course-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.course-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.course-image-wrapper {
  position: relative;
  padding-top: 62.5%;
  overflow: hidden;
}

.course-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.course-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 4px 10px;
  background: #f5222d;
  color: white;
  font-size: 12px;
  font-weight: 500;
  border-radius: 4px;
}

.course-duration {
  position: absolute;
  bottom: 8px;
  right: 8px;
  padding: 2px 6px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  font-size: 11px;
  border-radius: 4px;
}

.course-content {
  padding: 16px;
}

.course-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}

.level-tag {
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
}

.level-tag.beginner {
  background: #dcfce7;
  color: #16a34a;
}

.level-tag.intermediate {
  background: #dbeafe;
  color: #2563eb;
}

.level-tag.advanced {
  background: #f3e8ff;
  color: #7c3aed;
}

.level-tag.master {
  background: #fef3c7;
  color: #d97706;
}

.tag {
  padding: 3px 8px;
  background: #f1f5f9;
  color: #64748b;
  font-size: 11px;
  border-radius: 4px;
}

.course-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 8px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.course-desc {
  font-size: 13px;
  color: #64748b;
  margin: 0 0 12px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.course-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 10px;
}

.instructor,
.students {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #94a3b8;
}

.course-rating {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
}

.star-rating {
  display: flex;
  gap: 2px;
}

.star {
  color: #e2e8f0;
}

.star.filled {
  color: #fbbf24;
}

.rating-text {
  font-size: 13px;
  color: #64748b;
}

.course-price {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.price {
  font-size: 20px;
  font-weight: 700;
  color: #f5222d;
}

.price.free {
  color: #10b981;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 40px;
}

.training-container,
.certificates-container {
  max-width: 100%;
}

.training-header,
.certificates-header {
  text-align: center;
  margin-bottom: 24px;
}

.training-header h3,
.certificates-header h3 {
  margin: 0 0 8px 0;
  font-size: 20px;
  font-weight: 600;
  color: #1e293b;
}

.training-header p,
.certificates-header p {
  margin: 0;
  font-size: 14px;
  color: #64748b;
}

.training-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 32px;
}

.training-card {
  display: flex;
  gap: 20px;
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.training-image {
  width: 400px;
  flex-shrink: 0;
  position: relative;
}

.training-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.training-badge {
  position: absolute;
  top: 16px;
  left: 16px;
  padding: 6px 12px;
  background: #1d39c4;
  color: white;
  font-size: 12px;
  font-weight: 500;
  border-radius: 4px;
}

.training-badge.upcoming {
  background: #f59e0b;
}

.training-content {
  flex: 1;
  padding: 20px;
  display: flex;
  flex-direction: column;
}

.training-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.duration-tag {
  padding: 4px 10px;
  background: #f1f5f9;
  color: #64748b;
  font-size: 12px;
  border-radius: 4px;
}

.training-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 8px 0;
}

.training-desc {
  font-size: 14px;
  color: #64748b;
  margin: 0 0 16px 0;
  flex: 1;
}

.mentor-section {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.mentor-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #1d39c4;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 500;
}

.mentor-info {
  display: flex;
  flex-direction: column;
}

.mentor-name {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
}

.mentor-title {
  font-size: 12px;
  color: #94a3b8;
}

.training-features {
  display: flex;
  gap: 24px;
  margin-bottom: 16px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #64748b;
}

.training-price {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 16px;
}

.original-price {
  font-size: 14px;
  color: #94a3b8;
  text-decoration: line-through;
}

.enroll-btn {
  align-self: flex-start;
  padding: 10px 24px;
  background: #1d39c4;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.enroll-btn:hover {
  background: #1e40af;
}

.enroll-btn.disabled {
  background: #94a3b8;
  cursor: not-allowed;
}

.training-stats {
  display: flex;
  gap: 20px;
}

.stat-card {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: #f8fafc;
  border-radius: 12px;
}

.stat-icon {
  font-size: 32px;
  color: #1d39c4;
}

.stat-content {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #1e293b;
}

.stat-label {
  font-size: 13px;
  color: #64748b;
}

.certificates-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 32px;
}

.cert-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.cert-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.cert-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 8px 0;
}

.cert-desc {
  font-size: 14px;
  color: #64748b;
  margin: 0 0 16px 0;
}

.cert-requirements,
.cert-benefits {
  text-align: left;
  margin-bottom: 16px;
}

.cert-requirements h4,
.cert-benefits h4 {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 8px 0;
}

.cert-requirements ul,
.cert-benefits ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.cert-requirements li,
.cert-benefits li {
  font-size: 13px;
  color: #64748b;
  padding: 4px 0;
  padding-left: 16px;
  position: relative;
}

.cert-requirements li::before,
.cert-benefits li::before {
  content: '✓';
  position: absolute;
  left: 0;
  color: #10b981;
}

.apply-btn {
  width: 100%;
  padding: 10px;
  background: #1d39c4;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.apply-btn:hover {
  background: #1e40af;
}

.my-certificates {
  padding-top: 24px;
  border-top: 1px solid #f1f5f9;
}

.my-certificates h3 {
  margin: 0 0 16px 0;
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
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
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.error-state p {
  color: #dc2626;
  margin: 0 0 16px 0;
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

.my-certs-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.my-cert-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #f8fafc;
  border-radius: 12px;
  padding: 16px;
}

.cert-badge-icon {
  font-size: 36px;
}

.cert-info {
  flex: 1;
}

.cert-info h4 {
  margin: 0 0 6px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.cert-date,
.cert-number {
  display: block;
  font-size: 13px;
  color: #64748b;
  margin-bottom: 4px;
}

.view-btn {
  padding: 8px 16px;
  background: #1d39c4;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
}

@media (max-width: 1024px) {
  .courses-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .certificates-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .training-card {
    flex-direction: column;
  }

  .training-image {
    width: 100%;
    height: 250px;
  }
}

@media (max-width: 600px) {
  .courses-grid {
    grid-template-columns: 1fr;
  }

  .certificates-grid {
    grid-template-columns: 1fr;
  }

  .training-stats {
    flex-direction: column;
  }

  .filter-bar {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
