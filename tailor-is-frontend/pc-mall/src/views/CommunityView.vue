<template>
  <div
    class="community-view"
    role="main"
    aria-label="社区页面"
  >
    <el-breadcrumb
      separator="/"
      aria-label="面包屑导航"
    >
      <el-breadcrumb-item :to="{ path: '/' }">
        首页
      </el-breadcrumb-item>
      <el-breadcrumb-item>社区</el-breadcrumb-item>
    </el-breadcrumb>

    <template v-if="isDetail && postDetail">
      <div class="detail-header">
        <el-button
          class="back-btn"
          @click="router.push('/community')"
        >
          <el-icon><ArrowLeft /></el-icon>
          返回列表
        </el-button>
      </div>
      <div class="post-detail-card">
        <div class="post-user-info">
          <el-avatar
            :size="50"
            :src="postDetail.userAvatar || PLACEHOLDER_40x40"
          />
          <div class="user-detail">
            <span class="user-name">{{ postDetail.userName }}</span>
            <span class="post-time">{{ formatDate(postDetail.createdAt) }}</span>
          </div>
        </div>
        <h2 class="post-title">
          {{ postDetail.title }}
        </h2>
        <p class="post-content">
          {{ postDetail.content }}
        </p>
        <div
          v-if="postDetail.images && postDetail.images.length > 0"
          class="post-images"
        >
          <el-image
            v-for="(img, i) in postDetail.images"
            :key="i"
            :src="img"
            :preview-src-list="postDetail.images"
            :initial-index="i"
            fit="cover"
            class="post-image-item"
          />
        </div>
        <div class="post-actions">
          <button
            :class="['action-btn', { liked: postLikeMap[postDetail.id] }]"
            @click="toggleLike(postDetail)"
          >
            <el-icon><Star /></el-icon>
            <span>{{ postDetail.likeCount + (postLikeMap[postDetail.id] ? 1 : 0) }}</span>
          </button>
          <button
            class="action-btn"
            @click="toggleComment(postDetail.id)"
          >
            <el-icon><ChatLineRound /></el-icon>
            <span>{{ postDetail.commentCount }}</span>
          </button>
          <button class="action-btn">
            <el-icon><View /></el-icon>
            <span>{{ postDetail.viewCount }}</span>
          </button>
        </div>
        <div
          v-if="expandedComments[postDetail.id]"
          class="comment-section"
        >
          <el-input
            v-model="commentContent[postDetail.id]"
            type="textarea"
            :rows="2"
            placeholder="写下你的评论..."
            @keyup.enter.ctrl="handleComment(postDetail.id)"
          />
          <el-button
            type="primary"
            size="small"
            :loading="commenting"
            @click="handleComment(postDetail.id)"
          >
            发表评论
          </el-button>
        </div>
      </div>
    </template>

    <template v-else>
      <div class="community-header">
        <h2 class="page-title">
          社区动态
        </h2>
        <el-button
          type="primary"
          @click="showPostDialog = true"
        >
          <el-icon><Plus /></el-icon>
          发布动态
        </el-button>
      </div>

      <el-skeleton
        :loading="loading"
        animated
        :rows="3"
      >
        <template v-if="posts.length > 0">
          <div
            v-for="post in posts"
            :key="post.id"
            class="post-card"
          >
            <div class="post-user-info">
              <el-avatar
                :size="40"
                :src="post.userAvatar || PLACEHOLDER_40x40"
              />
              <div class="user-detail">
                <span class="user-name">{{ post.userName }}</span>
                <span class="post-time">{{ formatDate(post.createdAt) }}</span>
              </div>
            </div>
            <h3
              class="post-title"
              @click="viewPost(post.id)"
            >
              {{ post.title }}
            </h3>
            <p class="post-content">
              {{ post.content }}
            </p>
            <div
              v-if="post.images.length > 0"
              class="post-images"
            >
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
              <button
                :class="['action-btn', { liked: postLikeMap[post.id] }]"
                @click="toggleLike(post)"
              >
                <el-icon><Star /></el-icon>
                <span>{{ post.likeCount + (postLikeMap[post.id] ? 1 : 0) }}</span>
              </button>
              <button
                class="action-btn"
                @click="toggleComment(post.id)"
              >
                <el-icon><ChatLineRound /></el-icon>
                <span>{{ post.commentCount }}</span>
              </button>
              <button class="action-btn">
                <el-icon><View /></el-icon>
                <span>{{ post.viewCount }}</span>
              </button>
            </div>
            <div
              v-if="expandedComments[post.id]"
              class="comment-section"
            >
              <el-input
                v-model="commentContent[post.id]"
                type="textarea"
                :rows="2"
                placeholder="写下你的评论..."
                @keyup.enter.ctrl="handleComment(post.id)"
              />
              <el-button
                type="primary"
                size="small"
                :loading="commenting"
                @click="handleComment(post.id)"
              >
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
        <el-empty
          v-else-if="!loading"
          description="暂无动态"
        />
      </el-skeleton>
    </template>

    <el-dialog
      v-model="showPostDialog"
      title="发布动态"
      width="600px"
    >
      <el-form
        ref="postFormRef"
        :model="newPost"
        :rules="postRules"
        label-width="80px"
      >
        <el-form-item
          label="标题"
          prop="title"
        >
          <el-input
            v-model="newPost.title"
            placeholder="请输入标题"
          />
        </el-form-item>
        <el-form-item
          label="内容"
          prop="content"
        >
          <el-input
            v-model="newPost.content"
            type="textarea"
            :rows="5"
            placeholder="分享你的想法..."
          />
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="newPost.type">
            <el-radio :label="0">
              穿搭分享
            </el-radio>
            <el-radio :label="1">
              设计灵感
            </el-radio>
            <el-radio :label="2">
              评测体验
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="图片">
          <el-input
            v-model="imageUrl"
            placeholder="输入图片URL，多个URL用逗号分隔"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPostDialog = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="submitting"
          @click="handleCreatePost"
        >
          发布
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Plus, Star, ChatLineRound, View, ArrowLeft } from '@element-plus/icons-vue';
import type { FormInstance, FormRules } from 'element-plus';
import { getPosts, getPostDetail, createPost, likePost, commentPost } from '@/api/community';
import { formatDate } from '@/utils/format';
import type { Post } from '@/types';
import { PLACEHOLDER_40x40 } from '@shared/utils/placeholder';

const router = useRouter();
const route = useRoute();

const posts = ref<Post[]>([]);
const postDetail = ref<Post | null>(null);
const loading = ref(true);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

const isDetail = computed(() => {
  const postId = route.params.postId;
  return postId !== undefined && postId !== null;
});

const showPostDialog = ref(false);
const submitting = ref(false);
const postFormRef = ref<FormInstance>();
const imageUrl = ref('');

const newPost = ref({
  title: '',
  content: '',
  type: 0,
});

const postRules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }],
};

const postLikeMap = ref<Record<number, boolean>>({});
const expandedComments = ref<Record<number, boolean>>({});
const commentContent = ref<Record<number, string>>({});
const commenting = ref(false);

async function loadPosts() {
  loading.value = true;
  try {
    const res = await getPosts({ current: currentPage.value, size: pageSize.value });
    posts.value = res.records;
    total.value = res.total;
  } catch {
    posts.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

async function loadPostDetail() {
  loading.value = true;
  try {
    const postId = route.params.postId;
    if (postId) {
      const res = await getPostDetail(Number(postId));
      postDetail.value = res;
    }
  } catch {
    postDetail.value = null;
    ElMessage.error('加载帖子详情失败');
  } finally {
    loading.value = false;
  }
}

async function handleCreatePost() {
  if (!postFormRef.value) return;
  await postFormRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true;
      try {
        const images = imageUrl.value
          ? imageUrl.value
              .split(',')
              .map((u) => u.trim())
              .filter(Boolean)
          : [];
        await createPost({
          title: newPost.value.title,
          content: newPost.value.content,
          type: newPost.value.type,
          images,
        });
        ElMessage.success('发布成功');
        showPostDialog.value = false;
        newPost.value = { title: '', content: '', type: 0 };
        imageUrl.value = '';
        loadPosts();
      } catch {
        ElMessage.error('发布失败');
      } finally {
        submitting.value = false;
      }
    }
  });
}

async function toggleLike(post: Post) {
  try {
    await likePost(post.id);
    postLikeMap.value[post.id] = !postLikeMap.value[post.id];
  } catch {
    // ignore
  }
}

function toggleComment(postId: number) {
  expandedComments.value[postId] = !expandedComments.value[postId];
  if (!commentContent.value[postId]) {
    commentContent.value[postId] = '';
  }
}

async function handleComment(postId: number) {
  const content = commentContent.value[postId];
  if (!content || !content.trim()) {
    ElMessage.warning('请输入评论内容');
    return;
  }
  commenting.value = true;
  try {
    await commentPost(postId, { content: content.trim() });
    ElMessage.success('评论成功');
    commentContent.value[postId] = '';
    expandedComments.value[postId] = false;
    loadPosts();
  } catch {
    ElMessage.error('评论失败');
  } finally {
    commenting.value = false;
  }
}

function viewPost(id: number) {
  router.push(`/community/${id}`);
}

onMounted(() => {
  if (isDetail.value) {
    loadPostDetail();
  } else {
    loadPosts();
  }
});
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
.detail-header {
  margin-bottom: 20px;
}
.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
}
.post-detail-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}
.post-detail-card .post-title {
  font-size: 24px;
  font-weight: 600;
  margin: 16px 0;
  color: #333;
}
.post-detail-card .post-content {
  font-size: 16px;
  line-height: 1.8;
  color: #444;
  margin: 16px 0;
}
.post-detail-card .post-images {
  margin: 20px 0;
}
.post-detail-card .post-image-item {
  height: auto;
  max-height: 400px;
}
.post-detail-card .comment-section {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #eee;
}
</style>
