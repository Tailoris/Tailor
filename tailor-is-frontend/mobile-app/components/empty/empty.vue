<template>
  <view class="empty-component">
    <image :src="image" mode="aspectFit" class="empty-img" v-if="image"></image>
    <text class="icon" v-else>📭</text>
    <text class="text">{{ text }}</text>
    <button class="action-btn" v-if="actionText" @click="$emit('action')">{{ actionText }}</button>
  </view>
</template>

<script setup>
const props = defineProps({
  image: { type: String, default: '' },
  text: { type: String, default: '暂无数据' },
  actionText: { type: String, default: '' }
})

defineEmits(['action'])
</script>

<style lang="scss" scoped>
.empty-component {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100rpx 0;
  
  .empty-img, .icon {
    width: 240rpx;
    height: 240rpx;
    margin-bottom: 30rpx;
    font-size: 160rpx;
  }
  
  .text {
    font-size: 28rpx;
    color: #999;
    margin-bottom: 30rpx;
  }
  
  .action-btn {
    background: #FF4D4F;
    color: #fff;
    border: none;
    border-radius: 40rpx;
    padding: 0 60rpx;
    height: 80rpx;
    line-height: 80rpx;
    font-size: 28rpx;
  }
}
</style>
