<template>
  <section class="section">
    <div class="section-header">
      <h2 class="section-title">社区精选</h2>
      <router-link to="/community" class="view-more">查看更多 →</router-link>
    </div>
    <el-skeleton :loading="loading" animated :rows="1">
      <div class="community-grid">
        <div
          v-for="post in posts"
          :key="post.id"
          class="community-card"
          @click="$router.push(`/community/${post.id}`)"
        >
          <div class="post-header">
            <el-avatar :size="32" :src="post.userAvatar || 'https://via.placeholder.com/32x32'" />
            <div class="post-user">
              <span class="user-name">{{ post.userName }}</span>
              <span class="post-time">{{ formatDate(post.createdAt) }}</span>
            </div>
          </div>
          <h4 class="post-title">{{ post.title }}</h4>
          <p class="post-content">{{ truncateText(post.content, 80) }}</p>
          <div v-if="post.images.length > 0" class="post-images">
            <img v-for="(img, i) in post.images.slice(0, 3)" :key="i" :src="img" loading="lazy" />
          </div>
          <div class="post-stats">
            <span><el-icon><View /></el-icon> {{ post.viewCount }}</span>
            <span><el-icon><Star /></el-icon> {{ post.likeCount }}</span>
            <span><el-icon><ChatLineRound /></el-icon> {{ post.commentCount }}</span>
          </div>
        </div>
      </div>
    </el-skeleton>
  </section>
</template>

<script setup lang="ts">
import { View, Star, ChatLineRound } from '@element-plus/icons-vue'
import { formatDate, truncateText } from '@/utils/format'
import type { Post } from '@/types'

defineProps<{
  posts: Post[]
  loading: boolean
}>()
</script>

<style scoped>
.section {
  margin-bottom: 40px;
}
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.section-title {
  font-size: 24px;
  color: #333;
  margin: 0 0 16px;
}
.section-header .section-title {
  margin-bottom: 0;
}
.view-more {
  color: #1d39c4;
  text-decoration: none;
  font-size: 14px;
}
.community-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}
.community-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: box-shadow 0.2s;
}
.community-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}
.post-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.post-user {
  display: flex;
  flex-direction: column;
}
.user-name {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}
.post-time {
  font-size: 12px;
  color: #999;
}
.post-title {
  font-size: 16px;
  color: #333;
  margin: 0 0 8px;
}
.post-content {
  font-size: 14px;
  color: #666;
  margin: 0 0 12px;
  line-height: 1.5;
}
.post-images {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.post-images img {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
}
.post-stats {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #999;
}
.post-stats span {
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
