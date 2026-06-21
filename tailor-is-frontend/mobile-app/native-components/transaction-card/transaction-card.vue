<template>
  <view class="transaction-card" :style="{ opacity: isRemoving ? 0 : 1 }">
    <!-- 订单头部 -->
    <view class="card-header">
      <view class="merchant-info">
        <image class="merchant-logo" :src="merchant.logo" mode="aspectFill" />
        <text class="merchant-name">{{ merchant.name }}</text>
      </view>
      <view class="order-status" :class="statusClass">
        {{ statusLabel }}
      </view>
    </view>

    <!-- 商品列表 -->
    <view class="product-list">
      <view
        v-for="(item, index) in visibleItems"
        :key="item.id"
        class="product-item"
        @tap="$emit('item-click', item)"
      >
        <image class="product-image" :src="item.image" mode="aspectFill" />
        <view class="product-info">
          <text class="product-name text-ellipsis-2">{{ item.name }}</text>
          <view class="product-meta">
            <text class="product-sku" v-if="item.skuDesc">{{ item.skuDesc }}</text>
            <text class="product-quantity">x{{ item.quantity }}</text>
          </view>
        </view>
        <view class="product-price">
          <price :value="item.price" :font-size="'28rpx'" />
        </view>
      </view>
    </view>

    <!-- 更多商品提示 -->
    <view v-if="hasMoreItems" class="more-items" @tap="showAll = !showAll">
      <text>{{ showAll ? '收起' : `共 ${order.items.length} 件商品` }}</text>
      <text class="arrow" :class="{ 'arrow-up': showAll }">›</text>
    </view>

    <!-- 订单底部 -->
    <view class="card-footer">
      <view class="order-total">
        <text class="total-label">共 {{ totalCount }} 件商品</text>
        <text class="total-amount">
          合计：<price :value="order.payPrice" :font-size="'28rpx'" :bold="true" />
        </text>
      </view>

      <!-- 操作按钮 -->
      <view class="action-buttons" v-if="actionButtons.length > 0">
        <view
          v-for="action in actionButtons"
          :key="action.type"
          class="action-btn"
          :class="action.class"
          @tap.stop="handleAction(action.type)"
        >
          {{ action.label }}
        </view>
      </view>
    </view>

    <!-- 滑动操作区 -->
    <view
      class="swipe-actions"
      :style="{ transform: 'translateX(' + swipeOffset + 'px)' }"
    >
      <view
        v-for="action in swipeActions"
        :key="action.type"
        class="swipe-btn"
        :class="action.class"
        :style="{ width: actionWidth + 'px' }"
        @tap.stop="handleSwipeAction(action.type)"
      >
        {{ action.label }}
      </view>
    </view>

    <!-- 加载骨架屏 -->
    <view v-if="loading" class="skeleton">
      <skeleton :rows="3" :animated="true" />
    </view>
  </view>
</template>

<script lang="ts">
import { defineComponent, PropType, computed, ref, onMounted } from 'vue'

interface MerchantInfo {
  id: number
  name: string
  logo: string
}

interface OrderItemInfo {
  id: number
  productId: number
  productName: string
  productImage: string
  price: number
  quantity: number
  skuDesc?: string
  image?: string
  name?: string
}

interface ActionButton {
  type: string
  label: string
  class: string
}

// 订单状态枚举
const ORDER_STATUS_MAP: Record<number, { label: string; class: string }> = {
  0: { label: '待付款', class: 'status-pending' },
  1: { label: '待发货', class: 'status-paid' },
  2: { label: '待收货', class: 'status-shipped' },
  3: { label: '已完成', class: 'status-completed' },
  4: { label: '已取消', class: 'status-cancelled' },
  5: { label: '售后中', class: 'status-aftersale' }
}

export default defineComponent({
  name: 'TransactionCard',
  props: {
    /** 订单数据 */
    order: {
      type: Object as PropType<{
        id: number
        orderNo: string
        status: number
        totalPrice: number
        payPrice: number
        createTime: string
        merchant?: MerchantInfo
        items: OrderItemInfo[]
      }>,
      required: true
    },
    /** 是否加载中 */
    loading: {
      type: Boolean,
      default: false
    },
    /** 初始显示商品数量（虚拟列表截断） */
    visibleCount: {
      type: Number,
      default: 3
    },
    /** 滑动操作区宽度 */
    actionWidth: {
      type: Number,
      default: 80
    }
  },
  emits: ['item-click', 'action', 'swipe-action'],
  setup(props, { emit }) {
    const showAll = ref(false)
    const swipeOffset = ref(0)
    const isRemoving = ref(false)
    const touchStartX = ref(0)
    const touchStartY = ref(0)
    const isSwiping = ref(false)

    const merchant = computed<MerchantInfo>(() => {
      return props.order.merchant || {
        id: 0,
        name: '默认店铺',
        logo: '/static/merchant-default.png'
      }
    })

    const statusInfo = computed(() => {
      return ORDER_STATUS_MAP[props.order.status] || { label: '未知', class: '' }
    })

    const statusLabel = computed(() => statusInfo.value.label)
    const statusClass = computed(() => statusInfo.value.class)

    const visibleItems = computed(() => {
      const items = props.order.items || []
      if (showAll.value || items.length <= props.visibleCount) {
        return items
      }
      return items.slice(0, props.visibleCount)
    })

    const hasMoreItems = computed(() => {
      return (props.order.items || []).length > props.visibleCount
    })

    const totalCount = computed(() => {
      return (props.order.items || []).reduce((sum, item) => sum + item.quantity, 0)
    })

    /** 操作按钮配置 */
    const actionButtons = computed<ActionButton[]>(() => {
      const status = props.order.status
      const buttons: ActionButton[] = []

      switch (status) {
        case 0: // 待付款
          buttons.push(
            { type: 'cancel', label: '取消订单', class: 'btn-default' },
            { type: 'pay', label: '去付款', class: 'btn-primary' }
          )
          break
        case 1: // 待发货
          buttons.push(
            { type: 'cancel', label: '取消订单', class: 'btn-default' }
          )
          break
        case 2: // 待收货
          buttons.push(
            { type: 'after-sale', label: '申请售后', class: 'btn-default' },
            { type: 'confirm', label: '确认收货', class: 'btn-primary' }
          )
          break
        case 3: // 已完成
          buttons.push(
            { type: 'after-sale', label: '申请售后', class: 'btn-default' },
            { type: 'reorder', label: '再次购买', class: 'btn-primary' }
          )
          break
        case 5: // 售后中
          buttons.push(
            { type: 'view-after-sale', label: '查看售后', class: 'btn-primary' }
          )
          break
        default:
          break
      }

      return buttons
    })

    /** 滑动操作配置 */
    const swipeActions = computed<ActionButton[]>(() => {
      const status = props.order.status
      const actions: ActionButton[] = []

      if (status === 0 || status === 1) {
        actions.push({ type: 'cancel', label: '取消', class: 'swipe-danger' })
      }
      if (status === 2) {
        actions.push({ type: 'confirm', label: '确认收货', class: 'swipe-primary' })
      }

      return actions
    })

    function handleAction(type: string) {
      emit('action', { orderId: props.order.id, type })
    }

    function handleSwipeAction(type: string) {
      isRemoving.value = true
      setTimeout(() => {
        emit('swipe-action', { orderId: props.order.id, type })
      }, 300)
      resetSwipe()
    }

    // 触摸事件处理（滑动）
    function onTouchStart(e: TouchEvent) {
      touchStartX.value = e.touches[0].clientX
      touchStartY.value = e.touches[0].clientY
      isSwiping.value = false
    }

    function onTouchMove(e: TouchEvent) {
      if (swipeActions.value.length === 0) return

      const deltaX = e.touches[0].clientX - touchStartX.value
      const deltaY = e.touches[0].clientY - touchStartY.value

      // 判断水平滑动
      if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > 10) {
        isSwiping.value = true
        const totalWidth = swipeActions.value.length * props.actionWidth
        if (deltaX < 0) {
          swipeOffset.value = Math.max(deltaX, -totalWidth)
        } else {
          swipeOffset.value = Math.min(swipeOffset.value + deltaX, 0)
        }
      }
    }

    function onTouchEnd() {
      if (swipeActions.value.length === 0) return

      const totalWidth = swipeActions.value.length * props.actionWidth
      // 滑动超过一半则展开，否则收起
      if (Math.abs(swipeOffset.value) > totalWidth / 2) {
        swipeOffset.value = -totalWidth
      } else {
        resetSwipe()
      }
    }

    function resetSwipe() {
      swipeOffset.value = 0
    }

    return {
      merchant,
      statusLabel,
      statusClass,
      visibleItems,
      hasMoreItems,
      showAll,
      totalCount,
      actionButtons,
      swipeActions,
      swipeOffset,
      isRemoving,
      handleAction,
      handleSwipeAction,
      onTouchStart,
      onTouchMove,
      onTouchEnd
    }
  }
})
</script>

<style lang="scss" scoped>
.transaction-card {
  position: relative;
  margin: 20rpx;
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
  transition: opacity 0.3s ease;
  box-shadow: 0 2rpx 16rpx rgba(0, 0, 0, 0.06);

  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 24rpx 28rpx;
    border-bottom: 1rpx solid #f5f5f5;

    .merchant-info {
      display: flex;
      align-items: center;

      .merchant-logo {
        width: 40rpx;
        height: 40rpx;
        border-radius: 50%;
        margin-right: 12rpx;
      }

      .merchant-name {
        font-size: 26rpx;
        color: #333;
        font-weight: 500;
      }
    }

    .order-status {
      font-size: 24rpx;
      padding: 4rpx 16rpx;
      border-radius: 20rpx;

      &.status-pending { color: #ff6b00; background: #fff3e6; }
      &.status-paid { color: #1890ff; background: #e6f7ff; }
      &.status-shipped { color: #722ed1; background: #f9f0ff; }
      &.status-completed { color: #52c41a; background: #f6ffed; }
      &.status-cancelled { color: #999; background: #f5f5f5; }
      &.status-aftersale { color: #fa541c; background: #fff2e8; }
    }
  }

  .product-list {
    .product-item {
      display: flex;
      align-items: center;
      padding: 20rpx 28rpx;

      .product-image {
        width: 120rpx;
        height: 120rpx;
        border-radius: 8rpx;
        flex-shrink: 0;
        background: #f5f5f5;
      }

      .product-info {
        flex: 1;
        padding: 0 20rpx;
        min-width: 0;

        .product-name {
          font-size: 26rpx;
          color: #333;
          line-height: 1.4;
        }

        .product-meta {
          margin-top: 8rpx;
          display: flex;
          align-items: center;

          .product-sku {
            font-size: 22rpx;
            color: #999;
            margin-right: 16rpx;
          }

          .product-quantity {
            font-size: 22rpx;
            color: #999;
          }
        }
      }

      .product-price {
        flex-shrink: 0;
      }
    }
  }

  .more-items {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 12rpx 28rpx 20rpx;
    font-size: 24rpx;
    color: #999;

    .arrow {
      margin-left: 8rpx;
      transform: rotate(90deg);
      transition: transform 0.2s;
      font-size: 28rpx;

      &.arrow-up {
        transform: rotate(-90deg);
      }
    }
  }

  .card-footer {
    padding: 20rpx 28rpx;
    border-top: 1rpx solid #f5f5f5;

    .order-total {
      display: flex;
      justify-content: flex-end;
      align-items: baseline;
      margin-bottom: 16rpx;

      .total-label {
        font-size: 24rpx;
        color: #999;
        margin-right: 16rpx;
      }

      .total-amount {
        font-size: 26rpx;
        color: #ff4d4f;
      }
    }

    .action-buttons {
      display: flex;
      justify-content: flex-end;
      gap: 16rpx;

      .action-btn {
        padding: 10rpx 28rpx;
        border-radius: 32rpx;
        font-size: 24rpx;
        border: 1rpx solid #ddd;

        &.btn-default {
          background: #fff;
          color: #666;
          border-color: #ddd;
        }

        &.btn-primary {
          background: #ff4d4f;
          color: #fff;
          border-color: #ff4d4f;
        }
      }
    }
  }

  .swipe-actions {
    position: absolute;
    top: 0;
    right: 0;
    bottom: 0;
    display: flex;
    transform: translateX(0);
    transition: transform 0.2s ease;

    .swipe-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-size: 26rpx;
      font-weight: 500;

      &.swipe-danger {
        background: #ff4d4f;
      }

      &.swipe-primary {
        background: #1890ff;
      }
    }
  }

  .skeleton {
    padding: 20rpx;
  }
}
</style>