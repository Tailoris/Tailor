import { describe, it, expect } from 'vitest'
import {
  formatPrice,
  formatDate,
  formatOrderStatus,
  formatProductType,
  truncateText
} from '../format'

describe('formatPrice', () => {
  it('应正确格式化整数价格', () => {
    expect(formatPrice(100)).toBe('¥100.00')
  })

  it('应正确格式化小数价格', () => {
    expect(formatPrice(99.9)).toBe('¥99.90')
  })

  it('应正确格式化零元', () => {
    expect(formatPrice(0)).toBe('¥0.00')
  })
})

describe('formatDate', () => {
  it('应正确格式化日期字符串', () => {
    const result = formatDate('2024-01-15T10:30:00')
    expect(result).toBe('2024-01-15 10:30')
  })

  it('应正确处理单数月份和日期', () => {
    const result = formatDate('2024-03-05T08:05:00')
    expect(result).toBe('2024-03-05 08:05')
  })
})

describe('formatOrderStatus', () => {
  it('应返回已知状态码对应的文本', () => {
    expect(formatOrderStatus(0)).toBe('待付款')
    expect(formatOrderStatus(1)).toBe('待发货')
    expect(formatOrderStatus(2)).toBe('待收货')
    expect(formatOrderStatus(3)).toBe('已完成')
    expect(formatOrderStatus(4)).toBe('已取消')
    expect(formatOrderStatus(5)).toBe('已退款')
  })

  it('应对未知状态码返回默认文本', () => {
    expect(formatOrderStatus(99)).toBe('未知状态')
    expect(formatOrderStatus(-1)).toBe('未知状态')
  })
})

describe('formatProductType', () => {
  it('应返回已知类型码对应的文本', () => {
    expect(formatProductType(0)).toBe('数字纸样')
    expect(formatProductType(1)).toBe('定制服务')
    expect(formatProductType(2)).toBe('实物商品')
  })

  it('应对未知类型码返回默认文本', () => {
    expect(formatProductType(99)).toBe('未知类型')
  })
})

describe('truncateText', () => {
  it('应截断超过最大长度的文本', () => {
    expect(truncateText('Hello World', 5)).toBe('Hello...')
  })

  it('应保留不超过最大长度的文本', () => {
    expect(truncateText('Hi', 5)).toBe('Hi')
  })

  it('应正确处理等于最大长度的文本', () => {
    expect(truncateText('Hello', 5)).toBe('Hello')
  })
})