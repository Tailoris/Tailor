<template>
  <view class="goods-list-component">
    <view class="goods-grid" :class="{ 'two-column': column === 2 }">
      <view class="goods-item" v-for="item in list" :key="item.id" @click="goDetail(item.id)">
        <image :src="item.image || 'https://via.placeholder.com/300x300'" mode="aspectFill" class="goods-img"></image>
        <view class="goods-info">
          <text class="goods-name text-ellipsis-2">{{ item.name }}</text>
          <view class="price-row">
            <text class="price">¥{{ item.price }}</text>
            <text class="original-price" v-if="item.originalPrice">¥{{ item.originalPrice }}</text>
          </view>
          <text class="sales" v-if="item.sales">已售 {{ item.sales }}</text>
        </view>
      </view>
    </view>
    <empty v-if="list.length === 0 && !loading" text="暂无商品"></empty>
  </view>
</template>

<script setup lang="ts">
const props = defineProps({
  list: { type: Array, default: () => [] },
  column: { type: Number, default: 2 },
  loading: { type: Boolean, default: false }
})

function goDetail(id) {
  uni.navigateTo({ url: `/pages/product/detail?id=${id}` })
}
</script>

<style lang="scss" scoped>
.goods-list-component {
  .goods-grid {
    display: flex;
    flex-wrap: wrap;
    
    &.two-column {
      .goods-item {
        width: 50%;
        padding: 12rpx;
      }
    }
    
    .goods-item {
      padding: 12rpx;
      
      .goods-img {
        width: 100%;
        height: 320rpx;
        border-radius: 12rpx;
      }
      
      .goods-info {
        padding: 16rpx 0;
        
        .goods-name {
          font-size: 26rpx;
          color: #333;
          line-height: 1.4;
          margin-bottom: 12rpx;
        }
        
        .price-row {
          display: flex;
          align-items: center;
          margin-bottom: 8rpx;
          
          .price {
            color: #FF4D4F;
            font-size: 32rpx;
            font-weight: bold;
            margin-right: 12rpx;
          }
          
          .original-price {
            color: #999;
            font-size: 22rpx;
            text-decoration: line-through;
          }
        }
        
        .sales {
          font-size: 22rpx;
          color: #999;
        }
      }
    }
  }
}
</style>
