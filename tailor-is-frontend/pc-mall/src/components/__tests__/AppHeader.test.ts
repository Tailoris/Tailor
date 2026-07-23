/**
 * @vitest-environment jsdom
 */
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import { mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import ElementPlus from 'element-plus';
import { createRouter, createWebHistory } from 'vue-router';
import * as vueI18n from 'vue-i18n';
import AppHeader from '../AppHeader.vue';
import { useUserStore } from '@/store/user';
import { useCartStore } from '@/store/cart';

vi.mock('vue-i18n', () => ({
  useI18n: vi.fn().mockReturnValue({
    t: (key: string) => {
      const messages: Record<string, string> = {
        'nav.home': '首页',
        'nav.patterns': '纸样商城',
        'nav.patternCustom': '纸样定制',
        'nav.design': '服装设计',
        'nav.community': '社区交流',
        'nav.knowledge': '知识库',
        'nav.academy': '线上学堂',
        'nav.merchant': '商家入驻',
        'nav.login': '登录',
        'nav.register': '注册',
        'nav.logout': '退出登录',
        'nav.profile': '个人中心',
        'nav.orders': '我的订单',
        'nav.cart': '购物车',
        'header.notifications': '通知中心',
        'header.markAllRead': '全部已读',
        'header.noNotifications': '暂无通知',
        'common.searchPlaceholder': '搜索商品、店铺',
      };
      return messages[key] || key;
    },
    locale: { value: 'zh-CN' },
  }),
}));

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'Home' },
    { path: '/patterns', name: 'Patterns' },
    { path: '/pattern-custom', name: 'PatternCustom' },
    { path: '/design', name: 'Design' },
    { path: '/community', name: 'Community' },
    { path: '/knowledge', name: 'Knowledge' },
    { path: '/academy', name: 'Academy' },
    { path: '/cart', name: 'Cart' },
    { path: '/login', name: 'Login' },
    { path: '/register', name: 'Register' },
    { path: '/profile', name: 'Profile' },
    { path: '/orders', name: 'Orders' },
    { path: '/merchant-apply', name: 'MerchantApply' },
    { path: '/products', name: 'Products' },
  ],
});

describe('AppHeader', () => {
  beforeEach(() => {
    const pinia = createPinia();
    setActivePinia(pinia);
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should render header with logo and navigation', async () => {
    router.push('/');
    await router.isReady();

    const wrapper = mount(AppHeader, {
      global: {
        plugins: [router, ElementPlus, createPinia()],
      },
    });

    expect(wrapper.find('.logo').exists()).toBe(true);
    expect(wrapper.find('.logo-text').text()).toBe('裁智云 Tailor IS');
    expect(wrapper.find('.nav-links').exists()).toBe(true);
    expect(wrapper.findAll('.nav-item').length).toBe(7);
  });

  it('should render search box', async () => {
    router.push('/');
    await router.isReady();

    const wrapper = mount(AppHeader, {
      global: {
        plugins: [router, ElementPlus, createPinia()],
      },
    });

    expect(wrapper.find('.search-box').exists()).toBe(true);
    expect(wrapper.find('.search-box input').exists()).toBe(true);
    expect(wrapper.find('.search-btn').exists()).toBe(true);
  });

  it('should handle search functionality', async () => {
    router.push('/');
    await router.isReady();

    const wrapper = mount(AppHeader, {
      global: {
        plugins: [router, ElementPlus, createPinia()],
      },
    });

    const input = wrapper.find('.search-box input');
    await input.setValue('test keyword');
    await wrapper.find('.search-btn').trigger('click');

    expect(router.currentRoute.value.path).toBe('/products');
    expect(router.currentRoute.value.query.keyword).toBe('test keyword');
  });

  it('should render login/register buttons when not authenticated', async () => {
    router.push('/');
    await router.isReady();

    const wrapper = mount(AppHeader, {
      global: {
        plugins: [router, ElementPlus, createPinia()],
      },
    });

    expect(wrapper.find('.login-btn').exists()).toBe(true);
    expect(wrapper.find('.register-btn').exists()).toBe(true);
    expect(wrapper.find('.user-info').exists()).toBe(false);
  });

  it('should render user dropdown when authenticated', async () => {
    router.push('/');
    await router.isReady();

    const pinia = createPinia();
    setActivePinia(pinia);
    const userStore = useUserStore();
    userStore.setToken('test-token');
    userStore.userInfo = { id: 1, username: 'testuser', nickname: 'Test User' } as any;

    const wrapper = mount(AppHeader, {
      global: {
        plugins: [router, ElementPlus, pinia],
      },
    });

    expect(wrapper.find('.user-info').exists()).toBe(true);
    expect(wrapper.find('.username').text()).toBe('Test User');
    expect(wrapper.find('.login-btn').exists()).toBe(false);
  });

  it('should handle logout command', async () => {
    router.push('/');
    await router.isReady();

    const pinia = createPinia();
    setActivePinia(pinia);
    const userStore = useUserStore();
    userStore.setToken('test-token');
    userStore.userInfo = { id: 1, username: 'testuser' } as any;

    const wrapper = mount(AppHeader, {
      global: {
        plugins: [router, ElementPlus, pinia],
      },
    });

    await wrapper.vm.handleUserCommand('logout');

    expect(userStore.token).toBe('');
    expect(userStore.userInfo).toBe(null);
  });

  it('should show notification panel when clicked', async () => {
    router.push('/');
    await router.isReady();

    const wrapper = mount(AppHeader, {
      global: {
        plugins: [router, ElementPlus, createPinia()],
      },
    });

    expect(wrapper.find('.notification-panel').exists()).toBe(false);

    await wrapper.find('.notification-icon').trigger('click');

    expect(wrapper.find('.notification-panel').exists()).toBe(true);
  });

  it('should mark notifications as read', async () => {
    router.push('/');
    await router.isReady();

    const wrapper = mount(AppHeader, {
      global: {
        plugins: [router, ElementPlus, createPinia()],
      },
    });

    await wrapper.find('.notification-icon').trigger('click');
    await wrapper.find('.clear-all-btn').trigger('click');

    const unreadCount = (wrapper.vm as any).unreadNotifications;
    expect(unreadCount).toBe(0);
  });

  it('should show cart badge when cart has items', async () => {
    router.push('/');
    await router.isReady();

    const pinia = createPinia();
    setActivePinia(pinia);
    const cartStore = useCartStore();
    cartStore.items = [{ id: 1, productId: 1, skuId: 1, quantity: 3, price: 100, checked: true }] as any;

    const wrapper = mount(AppHeader, {
      global: {
        plugins: [router, ElementPlus, pinia],
      },
    });

    expect(wrapper.find('.cart-badge').exists()).toBe(true);
    expect(wrapper.find('.cart-badge').text()).toBe('3');
  });

  it('should show 99+ for large cart counts', async () => {
    router.push('/');
    await router.isReady();

    const pinia = createPinia();
    setActivePinia(pinia);
    const cartStore = useCartStore();
    const items = Array.from({ length: 150 }, (_, i) => ({
      id: i,
      productId: 1,
      skuId: 1,
      quantity: 1,
      price: 100,
      checked: true,
    }));
    cartStore.items = items as any;

    const wrapper = mount(AppHeader, {
      global: {
        plugins: [router, ElementPlus, pinia],
      },
    });

    expect(wrapper.find('.cart-badge').text()).toBe('99+');
  });

  it('should toggle mobile menu', async () => {
    router.push('/');
    await router.isReady();

    const wrapper = mount(AppHeader, {
      global: {
        plugins: [router, ElementPlus, createPinia()],
      },
    });

    expect(wrapper.find('.mobile-menu').exists()).toBe(false);

    await wrapper.find('.mobile-menu-toggle').trigger('click');

    expect(wrapper.find('.mobile-menu').exists()).toBe(true);
  });

  it('should navigate to correct routes via nav items', async () => {
    router.push('/');
    await router.isReady();

    const wrapper = mount(AppHeader, {
      global: {
        plugins: [router, ElementPlus, createPinia()],
      },
    });

    const navItems = wrapper.findAll('.nav-item');
    await navItems[1].trigger('click');

    expect(router.currentRoute.value.path).toBe('/patterns');
  });

  it('should show search suggestions when typing', async () => {
    router.push('/');
    await router.isReady();

    const wrapper = mount(AppHeader, {
      global: {
        plugins: [router, ElementPlus, createPinia()],
      },
    });

    const input = wrapper.find('.search-box input');
    await input.setValue('dress');
    await input.trigger('focus');

    expect(wrapper.find('.search-suggestions').exists()).toBe(true);
    expect(wrapper.findAll('.suggestion-item').length).toBe(3);
  });

  it('should handle merchant entrance link', async () => {
    router.push('/');
    await router.isReady();

    const wrapper = mount(AppHeader, {
      global: {
        plugins: [router, ElementPlus, createPinia()],
      },
    });

    expect(wrapper.find('.merchant-entrance').exists()).toBe(true);
  });

  it('should update scrolled state on scroll', async () => {
    router.push('/');
    await router.isReady();

    const wrapper = mount(AppHeader, {
      global: {
        plugins: [router, ElementPlus, createPinia()],
      },
    });

    expect(wrapper.classes()).not.toContain('scrolled');

    window.scrollY = 100;
    window.dispatchEvent(new Event('scroll'));

    expect(wrapper.classes()).toContain('scrolled');
  });
});