<template>
  <view class="community-page">
    <scroll-view scroll-y class="post-list" v-if="postList.length > 0" @scrolltolower="loadMore">
      <view class="post-card" v-for="post in postList" :key="post.id" @click="goDetail(post.id)">
        <view class="post-header">
          <image :src="post.userAvatar || 'https://via.placeholder.com/80x80'" mode="aspectFill" class="avatar"></image>
          <view class="user-info">
            <text class="nickname">{{ post.nickname || '用户' }}</text>
            <text class="time">{{ post.createTime }}</text>
          </view>
        </view>
        
        <text class="post-content text-ellipsis-2">{{ post.content }}</text>
        
        <view class="post-images" v-if="post.images && post.images.length > 0">
          <image :src="img" mode="aspectFill" class="post-img" v-for="(img, index) in post.images.slice(0, 9)" :key="index"></image>
        </view>
        
        <view class="post-footer">
          <view class="action-item" @click.stop="likePost(post)">
            <text class="icon">{{ post.isLiked ? '❤️' : '🤍' }}</text>
            <text>{{ post.likeCount || 0 }}</text>
          </view>
          <view class="action-item">
            <text class="icon">💬</text>
            <text>{{ post.commentCount || 0 }}</text>
          </view>
          <view class="action-item">
            <text class="icon">🔗</text>
            <text>分享</text>
          </view>
        </view>
      </view>
    </scroll-view>
    
    <view class="empty" v-else-if="!loading">
      <text class="icon">💬</text>
      <text class="text">暂无帖子</text>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getPosts, likePost as likePostApi } from '@/api/community'

const postList = ref([])
const loading = ref(true)
const page = ref(1)
const hasMore = ref(true)

onMounted(() => {
  loadPosts()
})

async function loadPosts() {
  try {
    const res = await getPosts({ page: page.value, limit: 10 })
    const list = res.data?.list || []
    postList.value = page.value === 1 ? list : [...postList.value, ...list]
    hasMore.value = list.length >= 10
    page.value++
  } catch (e) {
    console.error('加载帖子失败', e)
  } finally {
    loading.value = false
  }
}

function loadMore() {
  if (hasMore.value) loadPosts()
}

async function likePost(post) {
  if (!uni.getStorageSync('token')) {
    uni.navigateTo({ url: '/pages/login/login' })
    return
  }
  try {
    await likePostApi(post.id)
    post.isLiked = !post.isLiked
    post.likeCount += post.isLiked ? 1 : -1
  } catch (e) {
    uni.showToast({ title: e.message || '操作失败', icon: 'none' })
  }
}

function goDetail(id) {
  uni.navigateTo({ url: `/pages/community/detail?id=${id}` })
}
</script>

<style lang="scss" scoped>
.community-page {
  min-height: 100vh;
  background: #f8f8f8;
}

.post-list {
  padding: 20rpx;
  
  .post-card {
    background: #fff;
    border-radius: 16rpx;
    padding: 30rpx;
    margin-bottom: 20rpx;
    
    .post-header {
      display: flex;
      align-items: center;
      margin-bottom: 20rpx;
      
      .avatar {
        width: 80rpx;
        height: 80rpx;
        border-radius: 50%;
        margin-right: 20rpx;
      }
      
      .user-info {
        .nickname {
          font-size: 28rpx;
          font-weight: bold;
          color: #333;
          display: block;
          margin-bottom: 6rpx;
        }
        
        .time {
          font-size: 22rpx;
          color: #999;
        }
      }
    }
    
    .post-content {
      font-size: 28rpx;
      color: #333;
      line-height: 1.5;
      margin-bottom: 20rpx;
      display: block;
    }
    
    .post-images {
      display: flex;
      flex-wrap: wrap;
      margin-bottom: 20rpx;
      
      .post-img {
        width: 30%;
        height: 200rpx;
        border-radius: 8rpx;
        margin-right: 2%;
        margin-bottom: 2%;
      }
    }
    
    .post-footer {
      display: flex;
      justify-content: space-around;
      border-top: 2rpx solid #f5f5f5;
      padding-top: 20rpx;
      
      .action-item {
        display: flex;
        align-items: center;
        
        .icon {
          font-size: 32rpx;
          margin-right: 8rpx;
        }
        
        text:last-child {
          font-size: 24rpx;
          color: #666;
        }
      }
    }
  }
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 200rpx;
  
  .icon { font-size: 120rpx; margin-bottom: 20rpx; }
  .text { font-size: 28rpx; color: #999; }
}
</style>
