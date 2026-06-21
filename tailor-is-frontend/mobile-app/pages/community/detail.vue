<template>
  <view class="post-detail-page">
    <scroll-view scroll-y class="content" v-if="!loading">
      <view class="post-header">
        <image :src="post.userAvatar || 'https://via.placeholder.com/100x100'" mode="aspectFill" class="avatar"></image>
        <view class="user-info">
          <text class="nickname">{{ post.nickname || '用户' }}</text>
          <text class="time">{{ post.createTime }}</text>
        </view>
      </view>
      
      <text class="post-content">{{ post.content }}</text>
      
      <view class="post-images" v-if="post.images && post.images.length > 0">
        <image :src="img" mode="aspectFill" class="post-img" v-for="(img, index) in post.images" :key="index" @click="previewImage(index)"></image>
      </view>
      
      <view class="post-stats">
        <view class="stat-item" @click="likePost">
          <text class="icon">{{ post.isLiked ? '❤️' : '🤍' }}</text>
          <text>{{ post.likeCount || 0 }} 点赞</text>
        </view>
        <view class="stat-item">
          <text class="icon">💬</text>
          <text>{{ commentList.length }} 评论</text>
        </view>
      </view>
      
      <view class="comment-section">
        <text class="section-title">评论 ({{ commentList.length }})</text>
        <view class="comment-item" v-for="comment in commentList" :key="comment.id">
          <image :src="comment.userAvatar || 'https://via.placeholder.com/60x60'" mode="aspectFill" class="comment-avatar"></image>
          <view class="comment-content">
            <text class="comment-user">{{ comment.nickname }}</text>
            <text class="comment-text">{{ comment.content }}</text>
            <text class="comment-time">{{ comment.createTime }}</text>
          </view>
        </view>
      </view>
    </scroll-view>
    
    <view class="loading" v-if="loading">
      <view class="skeleton"></view>
    </view>
    
    <view class="comment-bar">
      <input type="text" placeholder="写评论..." v-model="commentInput" class="input" @confirm="submitComment"></input>
      <button class="send-btn" @click="submitComment">发送</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getPostDetail, getComments, addComment, likePost as likePostApi } from '@/api/community'

const loading = ref(true)
const post = ref({})
const commentList = ref([])
const commentInput = ref('')
const postId = ref(null)

onMounted(() => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  postId.value = currentPage.options.id
  if (postId.value) loadData()
})

async function loadData() {
  try {
    const [postRes, commentRes] = await Promise.all([
      getPostDetail(postId.value),
      getComments(postId.value, { page: 1, limit: 20 })
    ])
    post.value = postRes.data || {}
    commentList.value = commentRes.data?.list || []
  } catch (e) {
    console.error('加载失败', e)
  } finally {
    loading.value = false
  }
}

async function likePost() {
  if (!uni.getStorageSync('token')) {
    uni.navigateTo({ url: '/pages/login/login' })
    return
  }
  try {
    await likePostApi(postId.value)
    post.value.isLiked = !post.value.isLiked
    post.value.likeCount += post.value.isLiked ? 1 : -1
  } catch (e) {
    uni.showToast({ title: e.message || '操作失败', icon: 'none' })
  }
}

async function submitComment() {
  if (!commentInput.value.trim()) {
    uni.showToast({ title: '请输入评论内容', icon: 'none' })
    return
  }
  if (!uni.getStorageSync('token')) {
    uni.navigateTo({ url: '/pages/login/login' })
    return
  }
  try {
    await addComment(postId.value, { content: commentInput.value })
    uni.showToast({ title: '评论成功', icon: 'success' })
    commentInput.value = ''
    loadData()
  } catch (e) {
    uni.showToast({ title: e.message || '评论失败', icon: 'none' })
  }
}

function previewImage(index) {
  uni.previewImage({
    current: index,
    urls: post.value.images || []
  })
}
</script>

<style lang="scss" scoped>
.post-detail-page {
  min-height: 100vh;
  background: #f8f8f8;
  padding-bottom: 120rpx;
}

.content {
  height: calc(100vh - 100rpx);
}

.post-header {
  display: flex;
  align-items: center;
  padding: 30rpx;
  background: #fff;
  margin-bottom: 2rpx;
  
  .avatar {
    width: 80rpx;
    height: 80rpx;
    border-radius: 50%;
    margin-right: 20rpx;
  }
  
  .user-info {
    .nickname {
      font-size: 30rpx;
      font-weight: bold;
      color: #333;
      display: block;
      margin-bottom: 6rpx;
    }
    .time {
      font-size: 24rpx;
      color: #999;
    }
  }
}

.post-content {
  font-size: 30rpx;
  color: #333;
  line-height: 1.6;
  padding: 30rpx;
  background: #fff;
  display: block;
  margin-bottom: 2rpx;
}

.post-images {
  padding: 0 30rpx 30rpx;
  background: #fff;
  display: flex;
  flex-wrap: wrap;
  margin-bottom: 2rpx;
  
  .post-img {
    width: 30%;
    height: 220rpx;
    border-radius: 8rpx;
    margin-right: 3.33%;
    margin-bottom: 10rpx;
  }
}

.post-stats {
  display: flex;
  padding: 24rpx 30rpx;
  background: #fff;
  margin-bottom: 2rpx;
  
  .stat-item {
    display: flex;
    align-items: center;
    margin-right: 40rpx;
    
    .icon {
      font-size: 36rpx;
      margin-right: 10rpx;
    }
    
    text:last-child {
      font-size: 26rpx;
      color: #666;
    }
  }
}

.comment-section {
  background: #fff;
  padding: 30rpx;
  
  .section-title {
    font-size: 28rpx;
    font-weight: bold;
    margin-bottom: 20rpx;
    display: block;
  }
  
  .comment-item {
    display: flex;
    padding: 20rpx 0;
    border-bottom: 2rpx solid #f5f5f5;
    
    .comment-avatar {
      width: 60rpx;
      height: 60rpx;
      border-radius: 50%;
      margin-right: 16rpx;
      flex-shrink: 0;
    }
    
    .comment-content {
      flex: 1;
      
      .comment-user {
        font-size: 26rpx;
        color: #666;
        display: block;
        margin-bottom: 8rpx;
      }
      
      .comment-text {
        font-size: 28rpx;
        color: #333;
        display: block;
        margin-bottom: 8rpx;
      }
      
      .comment-time {
        font-size: 22rpx;
        color: #999;
      }
    }
  }
}

.comment-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  padding: 16rpx 30rpx;
  background: #fff;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.05);
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  
  .input {
    flex: 1;
    height: 72rpx;
    background: #f5f5f5;
    border-radius: 36rpx;
    padding: 0 24rpx;
    font-size: 28rpx;
    margin-right: 16rpx;
  }
  
  .send-btn {
    background: #FF4D4F;
    color: #fff;
    border: none;
    border-radius: 36rpx;
    padding: 0 30rpx;
    height: 72rpx;
    line-height: 72rpx;
    font-size: 28rpx;
  }
}

.loading {
  padding: 40rpx;
  .skeleton {
    height: 500rpx;
    background: linear-gradient(90deg, #f2f2f2 25%, #e6e6e6 50%, #f2f2f2 75%);
    background-size: 200% 100%;
    animation: skeleton-loading 1.5s infinite;
    border-radius: 12rpx;
  }
}

@keyframes skeleton-loading {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
