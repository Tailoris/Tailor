<template>
  <view class="aftersale-progress">
    <!-- 售后状态概览 -->
    <view class="status-overview">
      <view class="status-icon" :class="statusClass">
        <text>{{ statusIcon }}</text>
      </view>
      <view class="status-info">
        <text class="status-title">{{ currentStatus.label }}</text>
        <text class="status-desc">{{ currentStatus.description }}</text>
      </view>
    </view>

    <!-- 预计处理时间 -->
    <view class="estimate-time" v-if="estimatedTime">
      <text class="estimate-icon">⏱</text>
      <text class="estimate-text">{{ estimatedTime }}</text>
    </view>

    <!-- 步骤指示器 -->
    <view class="steps-indicator">
      <view
        v-for="(step, index) in steps"
        :key="step.value"
        class="step-item"
        :class="{
          'step-active': index <= currentStepIndex,
          'step-current': index === currentStepIndex
        }"
      >
        <view class="step-dot">
          <text v-if="index < currentStepIndex" class="step-check">✓</text>
          <text v-else class="step-number">{{ index + 1 }}</text>
        </view>
        <view class="step-content">
          <text class="step-label">{{ step.label }}</text>
          <text class="step-time" v-if="step.time">{{ step.time }}</text>
        </view>
        <view
          v-if="index < steps.length - 1"
          class="step-line"
          :class="{ 'line-active': index < currentStepIndex }"
        />
      </view>
    </view>

    <!-- 状态时间线 -->
    <view class="timeline">
      <view class="timeline-title">处理记录</view>
      <view v-if="timeline.length === 0" class="timeline-empty">
        <text>暂无处理记录</text>
      </view>
      <view
        v-for="(item, index) in timeline"
        :key="index"
        class="timeline-item"
        :class="{ 'timeline-first': index === 0 }"
      >
        <view class="timeline-dot" :class="{ 'dot-active': index === 0 }" />
        <view v-if="index < timeline.length - 1" class="timeline-line" />
        <view class="timeline-content">
          <text class="timeline-title">{{ item.title }}</text>
          <text class="timeline-desc" v-if="item.description">{{ item.description }}</text>
          <text class="timeline-time">{{ item.time }}</text>
        </view>
      </view>
    </view>

    <!-- 操作按钮 -->
    <view class="action-area" v-if="actions.length > 0">
      <view
        v-for="action in actions"
        :key="action.type"
        class="action-btn"
        :class="action.class"
        @tap="handleAction(action.type)"
      >
        {{ action.label }}
      </view>
    </view>

    <!-- 加载状态 -->
    <view v-if="loading" class="loading">
      <view class="loading-spinner" />
      <text class="loading-text">加载中...</text>
    </view>
  </view>
</template>

<script lang="ts">
import { defineComponent, PropType, computed } from 'vue'

interface StepItem {
  value: string
  label: string
  time?: string
}

interface TimelineItem {
  title: string
  description?: string
  time: string
}

interface ActionItem {
  type: string
  label: string
  class: string
}

interface AfterSaleProgress {
  id: number
  status: number
  type: string
  steps: StepItem[]
  timeline: TimelineItem[]
  estimatedTime?: string
}

const STATUS_CONFIG: Record<number, { label: string; description: string; icon: string; class: string }> = {
  0: { label: '待审核', description: '您的售后申请已提交，等待商家审核', icon: '📋', class: 'status-pending' },
  1: { label: '审核通过', description: '商家已通过您的售后申请', icon: '✅', class: 'status-approved' },
  2: { label: '退货中', description: '请尽快寄回商品', icon: '📦', class: 'status-returning' },
  3: { label: '退款中', description: '商家已收到退货，正在处理退款', icon: '💰', class: 'status-refunding' },
  4: { label: '已完成', description: '售后已处理完毕', icon: '🎉', class: 'status-completed' },
  5: { label: '已拒绝', description: '商家拒绝了您的售后申请', icon: '❌', class: 'status-rejected' },
  6: { label: '已取消', description: '售后申请已取消', icon: '📭', class: 'status-cancelled' }
}

export default defineComponent({
  name: 'AfterSaleProgress',
  props: {
    /** 售后进度数据 */
    data: {
      type: Object as PropType<AfterSaleProgress>,
      required: true
    },
    /** 是否加载中 */
    loading: {
      type: Boolean,
      default: false
    }
  },
  emits: ['action'],
  setup(props, { emit }) {
    const currentStatus = computed(() => {
      return STATUS_CONFIG[props.data.status] || STATUS_CONFIG[0]
    })

    const statusIcon = computed(() => currentStatus.value.icon)
    const statusClass = computed(() => currentStatus.value.class)

    const steps = computed<StepItem[]>(() => {
      return props.data.steps || getDefaultSteps(props.data.status, props.data.type)
    })

    const currentStepIndex = computed(() => {
      return Math.min(props.data.status, steps.value.length - 1)
    })

    const timeline = computed<TimelineItem[]>(() => {
      return props.data.timeline || []
    })

    const estimatedTime = computed(() => {
      return props.data.estimatedTime || getEstimatedTime(props.data.status)
    })

    const actions = computed<ActionItem[]>(() => {
      const status = props.data.status
      const items: ActionItem[] = []

      switch (status) {
        case 0: // 待审核
          items.push({ type: 'cancel', label: '取消申请', class: 'btn-default' })
          break
        case 1: // 审核通过（退货退款）
          if (props.data.type === 'return') {
            items.push({ type: 'view-address', label: '查看退货地址', class: 'btn-default' })
          }
          break
        case 5: // 已拒绝
          items.push({ type: 'contact', label: '联系客服', class: 'btn-primary' })
          items.push({ type: 'reapply', label: '重新申请', class: 'btn-default' })
          break
        default:
          break
      }

      return items
    })

    function handleAction(type: string) {
      emit('action', { afterSaleId: props.data.id, type })
    }

    return {
      currentStatus,
      statusIcon,
      statusClass,
      steps,
      currentStepIndex,
      timeline,
      estimatedTime,
      actions,
      handleAction
    }
  }
})

/**
 * 根据售后状态获取默认步骤.
 */
function getDefaultSteps(status: number, type: string): StepItem[] {
  if (type === 'return' || type === 'exchange') {
    return [
      { value: 'submit', label: '提交申请' },
      { value: 'approve', label: '商家审核' },
      { value: 'return', label: '寄回商品' },
      { value: 'refund', label: '退款/换货' },
      { value: 'complete', label: '完成' }
    ]
  }

  // 仅退款
  return [
    { value: 'submit', label: '提交申请' },
    { value: 'approve', label: '商家审核' },
    { value: 'refund', label: '退款处理' },
    { value: 'complete', label: '完成' }
  ]
}

/**
 * 获取预计处理时间文案.
 */
function getEstimatedTime(status: number): string {
  switch (status) {
    case 0: return '预计 24 小时内完成审核'
    case 1: return '请在 7 天内寄回商品'
    case 2: return '预计 1-3 天寄回商家'
    case 3: return '预计 1-3 个工作日退款到账'
    case 4: return '售后已处理完毕'
    default: return ''
  }
}
</script>

<style lang="scss" scoped>
.aftersale-progress {
  background: #f8f8f8;
  min-height: 100vh;
  padding-bottom: 40rpx;

  .status-overview {
    background: linear-gradient(135deg, #ff4d4f, #ff7875);
    padding: 40rpx 28rpx;
    display: flex;
    align-items: center;
    margin-bottom: 20rpx;

    .status-icon {
      width: 80rpx;
      height: 80rpx;
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.2);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 40rpx;
      margin-right: 24rpx;
    }

    .status-info {
      flex: 1;

      .status-title {
        font-size: 34rpx;
        font-weight: 600;
        color: #fff;
        display: block;
      }

      .status-desc {
        font-size: 24rpx;
        color: rgba(255, 255, 255, 0.85);
        margin-top: 8rpx;
        display: block;
      }
    }
  }

  .estimate-time {
    background: #fff;
    margin: 0 20rpx 20rpx;
    padding: 20rpx 28rpx;
    border-radius: 12rpx;
    display: flex;
    align-items: center;

    .estimate-icon {
      font-size: 28rpx;
      margin-right: 12rpx;
    }

    .estimate-text {
      font-size: 24rpx;
      color: #666;
    }
  }

  .steps-indicator {
    background: #fff;
    margin: 0 20rpx 20rpx;
    padding: 32rpx 28rpx;
    border-radius: 16rpx;
    display: flex;
    align-items: flex-start;

    .step-item {
      flex: 1;
      position: relative;
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;

      .step-dot {
        width: 44rpx;
        height: 44rpx;
        border-radius: 50%;
        background: #e8e8e8;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 22rpx;
        color: #999;
        position: relative;
        z-index: 2;
        transition: all 0.3s;
      }

      .step-content {
        margin-top: 12rpx;

        .step-label {
          font-size: 22rpx;
          color: #999;
          display: block;
        }

        .step-time {
          font-size: 20rpx;
          color: #ccc;
          display: block;
          margin-top: 4rpx;
        }
      }

      .step-line {
        position: absolute;
        top: 22rpx;
        left: 50%;
        width: 100%;
        height: 2rpx;
        background: #e8e8e8;
        z-index: 1;
        transition: all 0.3s;
      }

      &.step-active {
        .step-dot {
          background: #ff4d4f;
          color: #fff;
        }

        .step-line {
          background: #ff4d4f;
        }

        .step-content {
          .step-label {
            color: #ff4d4f;
          }
        }
      }

      &.step-current {
        .step-dot {
          box-shadow: 0 0 0 6rpx rgba(255, 77, 79, 0.2);
        }
      }
    }
  }

  .timeline {
    background: #fff;
    margin: 0 20rpx 20rpx;
    padding: 28rpx;
    border-radius: 16rpx;

    .timeline-title {
      font-size: 28rpx;
      font-weight: 600;
      color: #333;
      margin-bottom: 24rpx;
    }

    .timeline-empty {
      padding: 40rpx 0;
      text-align: center;
      font-size: 26rpx;
      color: #ccc;
    }

    .timeline-item {
      position: relative;
      padding-left: 40rpx;
      padding-bottom: 28rpx;

      &:last-child {
        padding-bottom: 0;
      }

      .timeline-dot {
        position: absolute;
        left: 0;
        top: 6rpx;
        width: 16rpx;
        height: 16rpx;
        border-radius: 50%;
        background: #ddd;
        z-index: 2;

        &.dot-active {
          background: #ff4d4f;
          box-shadow: 0 0 0 4rpx rgba(255, 77, 79, 0.2);
        }
      }

      .timeline-line {
        position: absolute;
        left: 7rpx;
        top: 22rpx;
        bottom: 0;
        width: 2rpx;
        background: #eee;
      }

      .timeline-content {
        .timeline-title {
          font-size: 26rpx;
          color: #333;
          font-weight: 500;
          margin-bottom: 4rpx;
        }

        .timeline-desc {
          font-size: 24rpx;
          color: #999;
          display: block;
          margin-bottom: 4rpx;
        }

        .timeline-time {
          font-size: 22rpx;
          color: #ccc;
          display: block;
        }
      }
    }
  }

  .action-area {
    display: flex;
    justify-content: center;
    gap: 24rpx;
    padding: 20rpx;

    .action-btn {
      padding: 20rpx 48rpx;
      border-radius: 44rpx;
      font-size: 28rpx;
      border: 1rpx solid #ddd;

      &.btn-primary {
        background: #ff4d4f;
        color: #fff;
        border-color: #ff4d4f;
      }

      &.btn-default {
        background: #fff;
        color: #666;
        border-color: #ddd;
      }
    }
  }

  .loading {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 60rpx 0;

    .loading-spinner {
      width: 48rpx;
      height: 48rpx;
      border: 4rpx solid #f0f0f0;
      border-top-color: #ff4d4f;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }

    .loading-text {
      font-size: 24rpx;
      color: #999;
      margin-top: 16rpx;
    }
  }
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>