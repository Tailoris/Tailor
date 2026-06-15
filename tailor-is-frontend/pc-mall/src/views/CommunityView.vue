<template>
  <div class="community-view">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>社区</el-breadcrumb-item>
    </el-breadcrumb>

    <div class="community-header">
      <h2 class="page-title">社区动态</h2>
      <el-button type="primary" @click="showPostDialog = true">
        <el-icon><Plus /></el-icon>
        发布动态
      </el-button>
    </div>

    <el-skeleton :loading="loading" animated :rows="3">
      <template v-if="posts.length > 0">
        <div v-for="post in posts" :key="post.id" class="post-card">
          <div class="post-user-info">
            <el-avatar :size="40" :src="post.userAvatar || 'https://via.placeholder.com/40x40'" />
            <div class="user-detail">
              <span class="user-name">{{ post.userName }}</span>
              <span class="post-time">{{ formatDate(post.createdAt) }}</span>
            </div>
          </div>
          <h3 class="post-title" @click="viewPost(post.id)">{{ post.title }}</h3>
          <p class="post-content">{{ post.content }}</p>
          <div v-if="post.images.length > 0" class="post-images">
            <el-image
              v-for="(img, i) in post.images"
              :key="i"
              :src="img"
              :preview-src-list="post.images"
              :initial-index="i"
              fit="cover"
              class="post-image-item"
            />
          </div>
          <div class="post-actions">
            <button :class="['action-btn', { liked: postLikeMap[post.id] }]" @click="toggleLike(post)">
              <el-icon><Star /></el-icon>
              <span>{{ post.likeCount + (postLikeMap[post.id] ? 1 : 0) }}</span>
            </button>
            <button class="action-btn" @click="toggleComment(post.id)">
              <el-icon><ChatLineRound /></el-icon>
              <span>{{ post.commentCount }}</span>
            </button>
            <button class="action-btn">
              <el-icon><View /></el-icon>
              <span>{{ post.viewCount }}</span>
            </button>
          </div>
          <div v-if="expandedComments[post.id]" class="comment-section">
            <el-input
              v-model="commentContent[post.id]"
              type="textarea"
              :rows="2"
              placeholder="写下你的评论..."
              @keyup.enter.ctrl="handleComment(post.id)"
            />
            <el-button type="primary" size="small" @click="handleComment(post.id)" :loading="commenting">
              发表评论
            </el-button>
          </div>
        </div>
        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            layout="total, prev, pager, next"
            @current-change="loadPosts"
          />
        </div>
      </template>
      <el-empty v-else-if="!loading" description="暂无动态" />
    </el-skeleton>

    <el-dialog v-model="showPostDialog" title="发布动态" width="600px">
      <el-form :model="newPost" :rules="postRules" ref="postFormRef" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="newPost.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="newPost.content" type="textarea" :rows="5" placeholder="分享你的想法..." />
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="newPost.type">
            <el-radio :label="0">穿搭分享</el-radio>
            <el-radio :label="1">设计灵感</el-radio>
            <el-radio :label="2">评测体验</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="图片">
          <el-input v-model="imageUrl" placeholder="输入图片URL，多个URL用逗号分隔" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPostDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreatePost" :loading="submitting">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Star, ChatLineRound, View } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { getPosts, createPost, likePost, commentPost } from '@/api/community'
import { formatDate } from '@/utils/format'
import type { Post } from '@/types'

const router = useRouter()

const posts = ref<Post[]>([])
const loading = ref(true)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const showPostDialog = ref(false)
const submitting = ref(false)
const postFormRef = ref<FormInstance>()
const imageUrl = ref('')

const newPost = ref({
  title: '',
  content: '',
  type: 0
})

const postRules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

const postLikeMap = ref<Record<number, boolean>>({})
const expandedComments = ref<Record<number, boolean>>({})
const commentContent = ref<Record<number, string>>({})
const commenting = ref(false)

async function loadPosts() {
  loading.value = true
  try {
    const res = await getPosts({ current: currentPage.value, size: pageSize.value })
    posts.value = res.records
    total.value = res.total
  } catch {
    posts.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function handleCreatePost() {
  if (!postFormRef.value) return
  await postFormRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        const images = imageUrl.value ? imageUrl.value.split(',').map((u) => u.trim()).filter(Boolean) : []
        await createPost({
          title: newPost.value.title,
          content: newPost.value.content,
          type: newPost.value.type,
          images
        })
        ElMessage.success('发布成功')
        showPostDialog.value = false
        newPost.value = { title: '', content: '', type: 0 }
        imageUrl.value = ''
        loadPosts()
      } catch {
        ElMessage.error('发布失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

async function toggleLike(post: Post) {
  try {
    await likePost(post.id)
    postLikeMap.value[post.id] = !postLikeMap.value[post.id]
  } catch {
    // ignore
  }
}

function toggleComment(postId: number) {
  expandedComments.value[postId] = !expandedComments.value[postId]
  if (!commentContent.value[postId]) {
    commentContent.value[postId] = ''
  }
}

async function handleComment(postId: number) {
  const content = commentContent.value[postId]
  if (!content || !content.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  commenting.value = true
  try {
    await commentPost(postId, { content: content.trim() })
    ElMessage.success('评论成功')
    commentContent.value[postId] = ''
    expandedComments.value[postId] = false
    loadPosts()
  } catch {
    ElMessage.error('评论失败')
  } finally {
    commenting.value = false
  }
}

function viewPost(id: number) {
  router.push(`/community/${id}`)
}

onMounted(() => {
  loadPosts()
})
</script>

<style scoped>
.community-view {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}
.el-breadcrumb {
  margin-bottom: 20px;
}
.community-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.page-title {
  font-size: 24px;
  color: #333;
  margin: 0;
}
.post-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}
.post-user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.user-detail {
  display: flex;
  flex-direction: column;
  gap: 2px;
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
  font-size: 18px;
  color: #333;
  margin: 0 0 8px;
  cursor: pointer;
}
.post-title:hover {
  color: #1d39c4;
}
.post-content {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  margin: 0 0 12px;
}
.post-images {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-bottom: 12px;
}
.post-image-item {
  width: 100%;
  height: 150px;
  border-radius: 4px;
}
.post-actions {
  display: flex;
  gap: 24px;
}
.action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  cursor: pointer;
  color: #666;
  font-size: 14px;
  padding: 4px 0;
}
.action-btn:hover,
.action-btn.liked {
  color: #1d39c4;
}
.comment-section {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #eee;
}
.comment-section .el-button {
  margin-top: 8px;
}
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
