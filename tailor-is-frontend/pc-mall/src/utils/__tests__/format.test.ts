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

  it('应处理null值', () => {
    expect(formatPrice(null)).toBe('¥0.00')
  })

  it('应处理undefined值', () => {
    expect(formatPrice(undefined)).toBe('¥0.00')
  })

  it('应处理空字符串', () => {
    expect(formatPrice('')).toBe('¥0.00')
  })

  it('应处理无法转换为数字的字符串', () => {
    expect(formatPrice('abc')).toBe('¥0.00')
  })

  it('应处理字符串形式的数字', () => {
    expect(formatPrice('123.45')).toBe('¥123.45')
  })

  it('应处理负数价格', () => {
    expect(formatPrice(-50)).toBe('¥-50.00')
  })

  it('应处理超大数值', () => {
    expect(formatPrice(999999999.99)).toBe('¥999999999.99')
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

  it('应处理null值', () => {
    expect(formatDate(null)).toBe('-')
  })

  it('应处理undefined值', () => {
    expect(formatDate(undefined)).toBe('-')
  })

  it('应处理空字符串', () => {
    expect(formatDate('')).toBe('-')
  })

  it('应处理无效日期字符串', () => {
    expect(formatDate('invalid-date')).toBe('-')
  })

  it('应处理Date对象', () => {
    const date = new Date(2024, 5, 15, 14, 30)
    const result = formatDate(date)
    expect(result).toBe('2024-06-15 14:30')
  })

  it('应处理边界时间（0点0分）', () => {
    const result = formatDate('2024-01-01T00:00:00')
    expect(result).toBe('2024-01-01 00:00')
  })

  it('应处理23点59分', () => {
    const result = formatDate('2024-12-31T23:59:00')
    expect(result).toBe('2024-12-31 23:59')
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