/**
 * @vitest-environment jsdom
 */
import { vi, describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import * as Vue from 'vue'

vi.mock('vue', async () => {
  const actual = await vi.importActual('vue') as any
  return {
    ...actual,
    ref: vi.fn(() => ({ value: null })),
    onMounted: actual.onMounted,
    onUnmounted: actual.onUnmounted
  }
})

import ProductCard from '../ProductCard.vue'

const mockProduct = {
  id: 1,
  name: 'Test Product',
  price: 99.9,
  mainImage: 'http://example.com/image.jpg',
  sales: 100,
  createTime: '2024-01-01'
}

describe('ProductCard with null wrapperRef', () => {
  it('should not call observe when wrapperRef is null', () => {
    const observeSpy = vi.fn()
    
    global.IntersectionObserver = vi.fn(function(this: IntersectionObserver, callback: (entries: IntersectionObserverEntry[]) => void) {
      this.observe = observeSpy
      this.disconnect = vi.fn()
      return this
    })

    const wrapper = mount(ProductCard, {
      props: { product: mockProduct }
    })

    expect(observeSpy).toHaveBeenCalledTimes(0)
  })
})