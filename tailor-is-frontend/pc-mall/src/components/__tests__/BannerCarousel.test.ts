/** @vitest-environment jsdom */
import { describe, it, expect, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import BannerCarousel from '../BannerCarousel.vue';
import ElementPlus from 'element-plus';

const mockRouter = { push: vi.fn() };
vi.mock('vue-router', () => ({ useRouter: () => mockRouter }));

describe('BannerCarousel', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render carousel with slides', () => {
    const wrapper = mount(BannerCarousel, { global: { plugins: [ElementPlus] } });
    expect(wrapper.find('.banner-carousel').exists()).toBe(true);
  });

  it('should display slide content correctly', () => {
    const wrapper = mount(BannerCarousel, { global: { plugins: [ElementPlus] } });
    const firstSlide = wrapper.find('.banner-slide');
    expect(firstSlide.find('.slide-title').text()).toBe('智能纸样 精准定制');
    expect(firstSlide.find('.slide-desc').text()).toBe('AI驱动的智能纸样生成系统，让定制更简单');
  });

  it('should have correct aria attributes', () => {
    const wrapper = mount(BannerCarousel, { global: { plugins: [ElementPlus] } });
    expect(wrapper.find('.banner-carousel').attributes('role')).toBe('region');
    expect(wrapper.find('.banner-carousel').attributes('aria-label')).toBe('促销横幅轮播');
  });

  it('should render all 4 slides with different content', () => {
    const wrapper = mount(BannerCarousel, { global: { plugins: [ElementPlus] } });
    const slides = wrapper.findAll('.banner-slide');
    const titles = slides.map((slide) => slide.find('.slide-title').text());
    expect(titles).toContain('智能纸样 精准定制');
    expect(titles).toContain('设计师平台 创意无限');
    expect(titles).toContain('商家入驻 共创未来');
    expect(titles).toContain('限时特惠 不容错过');
  });

  it('should have unique background colors for each slide', () => {
    const wrapper = mount(BannerCarousel, { global: { plugins: [ElementPlus] } });
    const slides = wrapper.findAll('.banner-slide');
    const backgrounds = slides.map((slide) => slide.element.style.background);
    const uniqueBackgrounds = new Set(backgrounds);
    expect(uniqueBackgrounds.size).toBe(4);
  });

  it('should trigger router push when slide button is clicked', async () => {
    const wrapper = mount(BannerCarousel, { global: { plugins: [ElementPlus] } });
    const firstSlide = wrapper.find('.banner-slide');
    const button = firstSlide.find('button');

    await button.trigger('click');

    expect(mockRouter.push).toHaveBeenCalledTimes(1);
    expect(mockRouter.push).toHaveBeenCalledWith('/products?type=0');
  });

  it('should navigate to correct path for each slide', async () => {
    const wrapper = mount(BannerCarousel, { global: { plugins: [ElementPlus] } });
    const slides = wrapper.findAll('.banner-slide');

    const expectedPaths = ['/products?type=0', '/community', '/merchant-apply', '/products'];

    for (let i = 0; i < slides.length; i++) {
      mockRouter.push.mockClear();
      const button = slides[i].find('button');
      await button.trigger('click');
      expect(mockRouter.push).toHaveBeenCalledWith(expectedPaths[i]);
    }
  });

  it('should have correct cta text on buttons', () => {
    const wrapper = mount(BannerCarousel, { global: { plugins: [ElementPlus] } });
    const slides = wrapper.findAll('.banner-slide');
    const ctaTexts = slides.map((slide) => slide.find('button').text());

    expect(ctaTexts).toEqual(['立即体验', '浏览设计', '申请入驻', '立即抢购']);
  });

  it('should have correct aria-label on buttons', () => {
    const wrapper = mount(BannerCarousel, { global: { plugins: [ElementPlus] } });
    const slides = wrapper.findAll('.banner-slide');
    const firstButton = slides[0].find('button');

    expect(firstButton.attributes('aria-label')).toBe('立即体验: 智能纸样 精准定制');
  });
});
