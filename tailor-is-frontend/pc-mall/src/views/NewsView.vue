<template>
  <div class="news-page">
    <div class="page-header">
      <h1>新闻动态</h1>
      <p>了解裁智云最新资讯与行业动态</p>
    </div>

    <div class="container">
      <div class="news-list">
        <article
          v-for="item in newsList"
          :key="item.id"
          class="news-item"
        >
          <div class="news-date">
            <span class="day">{{ item.day }}</span>
            <span class="month">{{ item.month }}</span>
          </div>
          <div class="news-content">
            <h3>{{ item.title }}</h3>
            <p class="summary">
              {{ item.summary }}
            </p>
            <div class="meta">
              <span class="category">{{ item.category }}</span>
              <span class="views">阅读 {{ item.views }}</span>
            </div>
          </div>
        </article>
      </div>

      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

const currentPage = ref(1);
const pageSize = 10;
const total = 50;

const newsList = ref([
  {
    id: 1,
    day: '15',
    month: '2024-07',
    title: '裁智云 AI 纸样设计系统 2.0 正式发布',
    summary: '全新升级的 AI 纸样设计系统 2.0 正式发布，引入深度学习算法，纸样生成效率提升 300%，精度达到 99.5%。',
    category: '产品更新',
    views: 1256
  },
  {
    id: 2,
    day: '10',
    month: '2024-07',
    title: '裁智云与全球三大面料供应商达成战略合作',
    summary: '裁智云正式与全球领先的面料供应商达成战略合作，为用户提供更优质的面料供应链服务。',
    category: '合作动态',
    views: 892
  },
  {
    id: 3,
    day: '05',
    month: '2024-07',
    title: '区块链版权保护功能上线，守护设计师知识产权',
    summary: '平台正式上线区块链版权存证功能，设计师上传的作品将自动上链存证，确保知识产权安全。',
    category: '功能上线',
    views: 2103
  },
  {
    id: 4,
    day: '28',
    month: '2024-06',
    title: '裁智云荣获"2024 年度最佳服装产业数字化平台"称号',
    summary: '在第十届中国服装产业大会上，裁智云凭借创新的技术实力和优质的服务质量，荣获年度最佳平台称号。',
    category: '荣誉资质',
    views: 1567
  },
  {
    id: 5,
    day: '20',
    month: '2024-06',
    title: '线上学堂夏季课程开放报名，涵盖 20+ 专业课程',
    summary: '裁智云线上学堂夏季课程正式开放报名，涵盖纸样设计、服装工艺、AI 应用等 20+ 专业课程。',
    category: '活动通知',
    views: 945
  }
]);
</script>

<style scoped>
.news-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.page-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  text-align: center;
  padding: 80px 20px;
}

.page-header h1 {
  font-size: 48px;
  margin-bottom: 16px;
}

.page-header p {
  font-size: 20px;
  opacity: 0.9;
}

.container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 60px 20px;
}

.news-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.news-item {
  display: flex;
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: transform 0.3s;
}

.news-item:hover {
  transform: translateY(-4px);
}

.news-date {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 20px;
}

.news-date .day {
  font-size: 32px;
  font-weight: bold;
}

.news-date .month {
  font-size: 14px;
  margin-top: 4px;
}

.news-content {
  flex: 1;
  padding: 24px;
}

.news-content h3 {
  font-size: 20px;
  margin-bottom: 12px;
  color: #1a1a2e;
}

.news-content .summary {
  color: #666;
  line-height: 1.6;
  margin-bottom: 16px;
}

.news-content .meta {
  display: flex;
  gap: 16px;
  font-size: 14px;
  color: #999;
}

.news-content .category {
  background: #f0f0f0;
  padding: 4px 12px;
  border-radius: 12px;
}

.pagination {
  margin-top: 40px;
  display: flex;
  justify-content: center;
}
</style>
