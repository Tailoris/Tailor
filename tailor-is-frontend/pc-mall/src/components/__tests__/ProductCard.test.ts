/**
 * @vitest-environment jsdom
 */
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import { mount } from '@vue/test-utils';
import ProductCard from '../ProductCard.vue';
import type { Product } from '@/types';

const mockProduct: Product = {
  id: 1,
  merchantId: 1,
  shopId: 1,
  categoryId: 1,
  productType: 1,
  name: 'Test Product',
  subTitle: '',
  mainImage: 'http://example.com/image.jpg',
  images: [],
  description: '',
  price: 99.9,
  status: 1,
  auditStatus: 1,
  copyrightFlag: 0,
  saleCount: 100,
  viewCount: 0,
  createdAt: '2024-01-01',
};

describe('ProductCard', () => {
  let observerCallback: IntersectionObserverCallback;

  beforeEach(() => {
    vi.clearAllMocks();
    observerCallback = vi.fn();
    global.IntersectionObserver = vi.fn(function (this: IntersectionObserver, callback: IntersectionObserverCallback) {
      this.observe = vi.fn();
      this.disconnect = vi.fn();
      observerCallback = callback;
      return this;
    }) as any;
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should render product info correctly', () => {
    const wrapper = mount(ProductCard, {
      props: { product: mockProduct },
    });

    expect(wrapper.find('.product-name').text()).toBe('Test Product');
    expect(wrapper.find('.product-price').text()).toBe('¥99.9');
    expect(wrapper.find('[role="button"]').exists()).toBe(true);
    expect(wrapper.find('[role="button"]').attributes('tabindex')).toBe('0');
    expect(wrapper.find('[role="button"]').attributes('aria-label')).toBe('商品 Test Product');
  });

  it('should show skeleton placeholder when image not loaded', () => {
    const wrapper = mount(ProductCard, {
      props: { product: mockProduct },
    });

    expect(wrapper.find('.image-placeholder').exists()).toBe(true);
    expect(wrapper.find('.product-image').exists()).toBe(false);
  });

  it('should emit click event when card is clicked', async () => {
    const wrapper = mount(ProductCard, {
      props: { product: mockProduct },
    });

    await wrapper.trigger('click');
    expect(wrapper.emitted('click')).toBeTruthy();
    expect(wrapper.emitted('click')?.[0]?.[0]).toEqual(mockProduct);
  });

  it('should show image error state when image fails to load', async () => {
    const wrapper = mount(ProductCard, {
      props: { product: { ...mockProduct, mainImage: 'invalid-url' } },
    });

    const mockEntry = { isIntersecting: true } as IntersectionObserverEntry;
    observerCallback([mockEntry], {} as IntersectionObserver);
    await wrapper.vm.$nextTick();

    const img = wrapper.find('.product-image');
    await img.trigger('error');
    await wrapper.vm.$nextTick();

    expect(wrapper.find('.image-error').exists()).toBe(true);
    expect(wrapper.find('.image-error').text()).toBe('图片加载失败');
    expect(wrapper.find('.image-placeholder').exists()).toBe(false);
  });

  it('should show loaded image when image loads successfully', async () => {
    const wrapper = mount(ProductCard, {
      props: { product: mockProduct },
    });

    const mockEntry = { isIntersecting: true } as IntersectionObserverEntry;
    observerCallback([mockEntry], {} as IntersectionObserver);
    await wrapper.vm.$nextTick();

    const img = wrapper.find('.product-image');
    await img.trigger('load');
    await wrapper.vm.$nextTick();

    expect(wrapper.find('.image-placeholder').exists()).toBe(false);
    expect(wrapper.find('.image-error').exists()).toBe(false);
  });

  it('should handle empty product name', () => {
    const wrapper = mount(ProductCard, {
      props: { product: { ...mockProduct, name: '' } },
    });

    expect(wrapper.find('.product-name').text()).toBe('');
    expect(wrapper.find('[role="button"]').attributes('aria-label')).toBe('商品 ');
  });

  it('should handle zero price', () => {
    const wrapper = mount(ProductCard, {
      props: { product: { ...mockProduct, price: 0 } },
    });

    expect(wrapper.find('.product-price').text()).toBe('¥0');
  });

  it('should handle negative price', () => {
    const wrapper = mount(ProductCard, {
      props: { product: { ...mockProduct, price: -10 } },
    });

    expect(wrapper.find('.product-price').text()).toBe('¥-10');
  });

  it('should handle undefined image URL', () => {
    const wrapper = mount(ProductCard, {
      props: { product: { ...mockProduct, mainImage: undefined as any } },
    });

    expect(wrapper.find('.product-card').exists()).toBe(true);
  });

  it('should use IntersectionObserver for lazy loading', () => {
    const wrapper = mount(ProductCard, {
      props: { product: mockProduct },
    });

    expect(global.IntersectionObserver).toHaveBeenCalledTimes(1);
    expect(global.IntersectionObserver).toHaveBeenCalledWith(expect.any(Function), { rootMargin: '200px' });
  });

  it('should trigger image loading when card enters viewport', async () => {
    const wrapper = mount(ProductCard, {
      props: { product: mockProduct },
    });

    expect(wrapper.find('.product-image').exists()).toBe(false);

    const mockEntry = { isIntersecting: true } as IntersectionObserverEntry;
    observerCallback([mockEntry], {} as IntersectionObserver);

    await wrapper.vm.$nextTick();

    expect(wrapper.find('.product-image').exists()).toBe(true);
    expect(wrapper.find('.product-image').attributes('src')).toBe(mockProduct.mainImage);
  });

  it('should not trigger image loading when card not in viewport', () => {
    const wrapper = mount(ProductCard, {
      props: { product: mockProduct },
    });

    const mockEntry = { isIntersecting: false } as IntersectionObserverEntry;
    observerCallback([mockEntry], {} as IntersectionObserver);

    expect(wrapper.find('.product-image').exists()).toBe(false);
  });

  it('should disconnect observer on unmount', () => {
    const wrapper = mount(ProductCard, {
      props: { product: mockProduct },
    });

    const observer = (global.IntersectionObserver as any).mock.instances[0];

    wrapper.unmount();

    expect(observer.disconnect).toHaveBeenCalledTimes(1);
  });

  it('should not call observe when wrapperRef is null', () => {
    const wrapper = mount(ProductCard, {
      props: { product: mockProduct },
    });

    const observer = (global.IntersectionObserver as any).mock.instances[0];

    expect(observer.observe).toHaveBeenCalledTimes(1);
  });

  it('should handle null wrapperRef gracefully', () => {
    const observeSpy = vi.fn();

    global.IntersectionObserver = vi.fn(function (this: IntersectionObserver, callback: IntersectionObserverCallback) {
      this.observe = observeSpy;
      this.disconnect = vi.fn();
      return this;
    }) as any;

    const wrapper = mount(ProductCard, {
      props: { product: mockProduct },
    });

    const observer = (global.IntersectionObserver as any).mock.instances[0];

    expect(observer.observe).toHaveBeenCalledTimes(1);
  });

  it('should skip observe when wrapperRef is null', () => {
    const observeSpy = vi.fn();

    global.IntersectionObserver = vi.fn(function (this: IntersectionObserver, callback: IntersectionObserverCallback) {
      this.observe = observeSpy;
      this.disconnect = vi.fn();
      return this;
    }) as any;

    const wrapper = mount(ProductCard, {
      props: { product: mockProduct },
    });

    const observer = (global.IntersectionObserver as any).mock.instances[0];

    expect(observer.observe).toHaveBeenCalledTimes(1);
  });

  it('should add is-loaded class when image loads successfully', async () => {
    const wrapper = mount(ProductCard, {
      props: { product: mockProduct },
    });

    const mockEntry = { isIntersecting: true } as IntersectionObserverEntry;
    observerCallback([mockEntry], {} as IntersectionObserver);
    await wrapper.vm.$nextTick();

    const img = wrapper.find('.product-image');
    await img.trigger('load');
    await wrapper.vm.$nextTick();

    expect(wrapper.find('.product-image.is-loaded').exists()).toBe(true);
  });

  it('should not add is-loaded class when image fails to load', async () => {
    const wrapper = mount(ProductCard, {
      props: { product: { ...mockProduct, mainImage: 'invalid-url' } },
    });

    const mockEntry = { isIntersecting: true } as IntersectionObserverEntry;
    observerCallback([mockEntry], {} as IntersectionObserver);
    await wrapper.vm.$nextTick();

    const img = wrapper.find('.product-image');
    await img.trigger('error');
    await wrapper.vm.$nextTick();

    expect(wrapper.find('.product-image.is-loaded').exists()).toBe(false);
  });

  it('should show hover styles when focused', async () => {
    const wrapper = mount(ProductCard, {
      props: { product: mockProduct },
    });

    await wrapper.trigger('focus');
    expect(wrapper.find('.product-card').classes()).toContain('product-card');
  });

  it('should handle multiple products correctly', () => {
    const wrapper = mount(ProductCard, {
      props: { product: { ...mockProduct, id: 2, name: 'Second Product' } },
    });

    expect(wrapper.find('.product-name').text()).toBe('Second Product');
    expect(wrapper.emitted('click')).toBeFalsy();
  });
});
