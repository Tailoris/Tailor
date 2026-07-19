<template>
  <div class="merchant-training-page">
    <div class="page-header">
      <h1>商家培训</h1>
      <p>助力商家成长，提升经营能力</p>
    </div>
    <div class="container">
      <section class="training-categories">
        <h2>培训课程</h2>
        <div class="category-tabs">
          <button
            v-for="cat in categories"
            :key="cat.id"
            :class="['tab', { active: activeCategory === cat.id }]"
            @click="activeCategory = cat.id"
          >
            {{ cat.name }}
          </button>
        </div>
        <div class="course-grid">
          <div
            v-for="course in filteredCourses"
            :key="course.id"
            class="course-card"
          >
            <div class="course-cover">
              <span
                class="level-badge"
                :class="course.level"
              >{{ course.levelText }}</span>
            </div>
            <div class="course-info">
              <h3>{{ course.title }}</h3>
              <p class="desc">
                {{ course.description }}
              </p>
              <div class="meta">
                <span>📚 {{ course.lessons }} 课时</span>
                <span>👥 {{ course.students }} 人学习</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="benefits-section">
        <h2>培训权益</h2>
        <div class="benefit-list">
          <div class="benefit-item">
            <div class="icon">
              🎓
            </div>
            <h3>免费课程</h3>
            <p>入驻商家可免费学习所有基础课程</p>
          </div>
          <div class="benefit-item">
            <div class="icon">
              👨‍🏫
            </div>
            <h3>专家辅导</h3>
            <p>行业专家一对一经营辅导</p>
          </div>
          <div class="benefit-item">
            <div class="icon">
              📜
            </div>
            <h3>结业证书</h3>
            <p>完成课程获得官方认证证书</p>
          </div>
          <div class="benefit-item">
            <div class="icon">
              💡
            </div>
            <h3>实操指导</h3>
            <p>真实案例实操，学以致用</p>
          </div>
        </div>
      </section>

      <section class="schedule-section">
        <h2>近期直播</h2>
        <div class="live-list">
          <div
            v-for="live in liveSessions"
            :key="live.id"
            class="live-item"
          >
            <div class="live-date">
              <span class="day">{{ live.day }}</span>
              <span class="month">{{ live.month }}</span>
            </div>
            <div class="live-info">
              <h3>{{ live.title }}</h3>
              <p class="time">
                🕐 {{ live.time }}
              </p>
              <p class="speaker">
                👤 讲师：{{ live.speaker }}
              </p>
            </div>
            <button class="reserve-btn">
              预约直播
            </button>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';

const categories = [
  { id: 'basic', name: '新手入门' },
  { id: 'operation', name: '运营提升' },
  { id: 'marketing', name: '营销推广' },
  { id: 'advanced', name: '高阶课程' }
];

const activeCategory = ref('basic');

const courses = ref([
  { id: 1, category: 'basic', title: '平台入驻全流程指南', description: '从注册到开店，手把手教你完成入驻', lessons: 8, students: 2340, level: 'beginner', levelText: '入门' },
  { id: 2, category: 'basic', title: '商家后台基础操作', description: '熟悉后台各模块功能与操作', lessons: 12, students: 1890, level: 'beginner', levelText: '入门' },
  { id: 3, category: 'operation', title: '商品优化与上架技巧', description: '提升商品曝光与转化率的实战技巧', lessons: 10, students: 1560, level: 'intermediate', levelText: '进阶' },
  { id: 4, category: 'operation', title: '订单管理与客户服务', description: '高效处理订单，提升客户满意度', lessons: 8, students: 1230, level: 'intermediate', levelText: '进阶' },
  { id: 5, category: 'marketing', title: '店铺营销活动策划', description: '打造爆款活动，提升销售额', lessons: 15, students: 980, level: 'advanced', levelText: '高级' },
  { id: 6, category: 'marketing', title: '社交媒体推广实战', description: '利用社交平台引流获客', lessons: 10, students: 870, level: 'advanced', levelText: '高级' },
  { id: 7, category: 'advanced', title: '数据分析与经营决策', description: '用数据驱动经营，提升决策效率', lessons: 12, students: 650, level: 'expert', levelText: '专家' },
  { id: 8, category: 'advanced', title: '品牌建设与长期运营', description: '打造品牌影响力，实现可持续发展', lessons: 20, students: 430, level: 'expert', levelText: '专家' }
]);

const liveSessions = ref([
  { id: 1, day: '20', month: '2024-07', title: '夏季爆款打造策略', time: '14:00-16:00', speaker: '李老师（资深运营专家）' },
  { id: 2, day: '25', month: '2024-07', title: 'AI 设计工具使用技巧', time: '19:00-21:00', speaker: '王老师（AI 产品专家）' },
  { id: 3, day: '30', month: '2024-07', title: '版权保护与维权实务', time: '14:00-16:00', speaker: '张老师（知识产权律师）' }
]);

const filteredCourses = computed(() => courses.value.filter(c => c.category === activeCategory.value));
</script>

<style scoped>
.merchant-training-page { min-height: 100vh; background: #f5f7fa; }
.page-header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; text-align: center; padding: 80px 20px; }
.page-header h1 { font-size: 48px; margin-bottom: 16px; }
.page-header p { font-size: 20px; opacity: 0.9; }
.container { max-width: 1200px; margin: 0 auto; padding: 60px 20px; }
section { margin-bottom: 60px; }
section h2 { font-size: 32px; margin-bottom: 24px; color: #1a1a2e; text-align: center; }
.category-tabs { display: flex; justify-content: center; gap: 16px; margin-bottom: 32px; flex-wrap: wrap; }
.tab { padding: 10px 24px; border: none; background: white; border-radius: 24px; cursor: pointer; font-size: 15px; color: #666; transition: all 0.3s; }
.tab.active { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; }
.course-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 24px; }
.course-card { background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 2px 12px rgba(0,0,0,0.08); transition: transform 0.3s; }
.course-card:hover { transform: translateY(-4px); }
.course-cover { height: 120px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); position: relative; }
.level-badge { position: absolute; top: 12px; right: 12px; padding: 4px 12px; border-radius: 12px; color: white; font-size: 12px; }
.level-badge.beginner { background: #4caf50; }
.level-badge.intermediate { background: #ff9800; }
.level-badge.advanced { background: #f44336; }
.level-badge.expert { background: #9c27b0; }
.course-info { padding: 20px; }
.course-info h3 { font-size: 18px; margin-bottom: 8px; color: #1a1a2e; }
.course-info .desc { color: #666; line-height: 1.6; margin-bottom: 12px; font-size: 14px; }
.course-info .meta { display: flex; gap: 16px; font-size: 13px; color: #999; }
.benefit-list { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 24px; }
.benefit-item { background: white; padding: 32px; border-radius: 12px; text-align: center; box-shadow: 0 2px 12px rgba(0,0,0,0.08); }
.benefit-item .icon { font-size: 48px; margin-bottom: 16px; }
.benefit-item h3 { font-size: 18px; margin-bottom: 12px; color: #1a1a2e; }
.benefit-item p { color: #666; line-height: 1.6; }
.live-list { display: flex; flex-direction: column; gap: 16px; }
.live-item { background: white; padding: 24px; border-radius: 12px; display: flex; align-items: center; gap: 24px; box-shadow: 0 2px 12px rgba(0,0,0,0.08); }
.live-date { display: flex; flex-direction: column; align-items: center; justify-content: center; width: 80px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 16px; border-radius: 8px; }
.live-date .day { font-size: 28px; font-weight: bold; }
.live-date .month { font-size: 12px; }
.live-info { flex: 1; }
.live-info h3 { font-size: 18px; margin-bottom: 8px; color: #1a1a2e; }
.live-info .time, .live-info .speaker { color: #666; font-size: 14px; margin-bottom: 4px; }
.reserve-btn { padding: 10px 24px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; border: none; border-radius: 20px; cursor: pointer; font-weight: 600; }
</style>
