/**
 * @vitest-environment jsdom
 */
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import { mount } from '@vue/test-utils';
import ElementPlus from 'element-plus';
import AppFooter from '../AppFooter.vue';

vi.mock('vue-i18n', () => ({
  useI18n: vi.fn().mockReturnValue({
    t: (key: string) => {
      const messages: Record<string, string> = {
        'footer.newsletterTitle': '订阅我们',
        'footer.newsletterDesc': '订阅获取最新时尚资讯和专属优惠',
        'footer.emailPlaceholder': '请输入您的邮箱',
        'footer.subscribe': '订阅',
        'footer.brandDesc': '全球首个服装全产业智能生态平台',
        'footer.brand': '品牌介绍',
        'footer.about': '关于裁智云',
        'footer.brandStory': '品牌故事',
        'footer.joinUs': '加入我们',
        'footer.news': '新闻动态',
        'footer.support': '帮助中心',
        'footer.shoppingGuide': '购物指南',
        'footer.payment': '支付方式',
        'footer.delivery': '配送说明',
        'footer.afterSale': '售后服务',
        'footer.merchant': '商家服务',
        'footer.merchantApply': '商家入驻',
        'footer.merchantRules': '商家规则',
        'footer.merchantBackend': '商家后台',
        'footer.merchantTraining': '商家培训',
        'footer.contact': '联系我们',
        'footer.hotline': '客服热线: 400-888-8888',
        'footer.workTime': '工作时间: 9:00-18:00',
        'footer.email': '邮箱: support@tailoris.com',
        'footer.address': '地址: 北京市朝阳区建国路88号',
        'footer.allRights': '保留所有权利',
        'footer.privacy': '隐私政策',
        'footer.terms': '服务条款',
        'footer.cookies': 'Cookie政策',
        'footer.icp': 'ICP备案号: 京ICP备12345678号',
        'footer.security': '网络安全认证',
      };
      return messages[key] || key;
    },
  }),
}));

describe('AppFooter', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should render footer structure', () => {
    const wrapper = mount(AppFooter, {
      global: {
        plugins: [ElementPlus],
      },
    });

    expect(wrapper.find('.app-footer').exists()).toBe(true);
    expect(wrapper.find('.footer-content').exists()).toBe(true);
    expect(wrapper.find('.footer-bottom').exists()).toBe(true);
  });

  it('should render brand section', () => {
    const wrapper = mount(AppFooter, {
      global: {
        plugins: [ElementPlus],
      },
    });

    expect(wrapper.find('.footer-brand').exists()).toBe(true);
    expect(wrapper.find('.brand-desc').exists()).toBe(true);
  });

  it('should render quick links sections', () => {
    const wrapper = mount(AppFooter, {
      global: {
        plugins: [ElementPlus],
      },
    });

    expect(wrapper.find('.footer-links').exists()).toBe(true);
    expect(wrapper.findAll('.links-section').length).toBeGreaterThan(0);
  });

  it('should render newsletter subscription form', () => {
    const wrapper = mount(AppFooter, {
      global: {
        plugins: [ElementPlus],
      },
    });

    expect(wrapper.find('.footer-newsletter').exists()).toBe(true);
    expect(wrapper.find('.newsletter-form').exists()).toBe(true);
    expect(wrapper.find('.newsletter-form input').exists()).toBe(true);
    expect(wrapper.find('.subscribe-btn').exists()).toBe(true);
  });

  it('should validate email format', async () => {
    const wrapper = mount(AppFooter, {
      global: {
        plugins: [ElementPlus],
      },
    });

    const input = wrapper.find('.newsletter-form input');
    await input.setValue('invalid-email');

    await wrapper.find('.subscribe-btn').trigger('click');

    expect((wrapper.vm as any).email).toBe('invalid-email');
  });

  it('should handle valid email subscription', async () => {
    const wrapper = mount(AppFooter, {
      global: {
        plugins: [ElementPlus],
      },
    });

    const input = wrapper.find('.newsletter-form input');
    await input.setValue('test@example.com');

    await wrapper.find('.subscribe-btn').trigger('click');

    expect((wrapper.vm as any).email).toBe('test@example.com');
  });

  it('should render social media links', () => {
    const wrapper = mount(AppFooter, {
      global: {
        plugins: [ElementPlus],
      },
    });

    expect(wrapper.find('.footer-social').exists()).toBe(true);
    expect(wrapper.findAll('.social-link').length).toBeGreaterThan(0);
  });

  it('should render contact information', () => {
    const wrapper = mount(AppFooter, {
      global: {
        plugins: [ElementPlus],
      },
    });

    expect(wrapper.find('.footer-contact').exists()).toBe(true);
    expect(wrapper.find('.hotline').exists()).toBe(true);
    expect(wrapper.find('.work-time').exists()).toBe(true);
  });

  it('should render copyright information', () => {
    const wrapper = mount(AppFooter, {
      global: {
        plugins: [ElementPlus],
      },
    });

    expect(wrapper.find('.footer-bottom').exists()).toBe(true);
    expect(wrapper.find('.copyright').exists()).toBe(true);
    expect(wrapper.find('.footer-links-bottom').exists()).toBe(true);
  });

  it('should render quick links with correct hrefs', () => {
    const wrapper = mount(AppFooter, {
      global: {
        plugins: [ElementPlus],
      },
    });

    const links = wrapper.findAll('.quick-link');
    expect(links.length).toBeGreaterThan(0);
  });
});