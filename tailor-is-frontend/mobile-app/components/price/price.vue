<template>
  <view class="price-component">
    <text class="current-price" :class="{ large: size === 'large' }">
      <text class="symbol" v-if="showSymbol">¥</text>{{ price }}
    </text>
    <text class="original-price" v-if="originalPrice">¥{{ originalPrice }}</text>
  </view>
</template>

<script setup lang="ts">
const props = defineProps({
  price: { type: [Number, String], default: '0.00' },
  originalPrice: { type: [Number, String], default: '' },
  showSymbol: { type: Boolean, default: true },
  size: { type: String, default: 'normal' }
})
</script>

<style lang="scss" scoped>
.price-component {
  display: inline-flex;
  align-items: baseline;
  
  .current-price {
    color: #FF4D4F;
    font-size: 28rpx;
    font-weight: bold;
    
    &.large {
      font-size: 40rpx;
    }
    
    .symbol {
      font-size: 0.7em;
    }
  }
  
  .original-price {
    color: #999;
    font-size: 22rpx;
    text-decoration: line-through;
    margin-left: 12rpx;
  }
}
</style>
