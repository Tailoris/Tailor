/** @vitest-environment jsdom */
import { describe, it, expect, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import CategoryNav from '../CategoryNav.vue';
import { createRouter, createWebHistory } from 'vue-router';

const mockRouter = {
  push: vi.fn(),
};

vi.mock('vue-router', () => ({
  useRouter: () => mockRouter,
  createRouter: vi.fn(),
  createWebHistory: vi.fn(),
}));

vi.mock('@shared/utils/placeholder', () => ({
  PLACEHOLDER_60x60: 'placeholder-image.jpg',
}));

describe('CategoryNav', () => {
  const categories = [
    { id: 1, name: '男装', icon: 'icon-1.jpg' },
    { id: 2, name: '女装', icon: 'icon-2.jpg' },
    { id: 3, name: '童装', icon: 'icon-3.jpg' },
    { id: 4, name: '鞋靴', icon: 'icon-4.jpg' },
    { id: 5, name: '配饰', icon: 'icon-5.jpg' },
    { id: 6, name: '家居', icon: 'icon-6.jpg' },
    { id: 7, name: '运动', icon: 'icon-7.jpg' },
    { id: 8, name: '内衣', icon: 'icon-8.jpg' },
    { id: 9, name: '箱包', icon: 'icon-9.jpg' },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render category list correctly', () => {
    const wrapper = mount(CategoryNav, {
      global: {
        mocks: {
          $router: mockRouter,
        },
      },
      props: { categories },
    });

    expect(wrapper.find('.section-title').text()).toBe('商品分类');
    expect(wrapper.findAll('.category-item').length).toBe(8);
  });

  it('should display category name and icon', () => {
    const wrapper = mount(CategoryNav, {
      global: {
        mocks: {
          $router: mockRouter,
        },
      },
      props: { categories },
    });

    const firstCategory = wrapper.find('.category-item:nth-child(1)');
    expect(firstCategory.find('span').text()).toBe('男装');
    expect(firstCategory.find('img').attributes('src')).toBe('icon-1.jpg');
    expect(firstCategory.find('img').attributes('alt')).toBe('男装');
  });

  it('should use placeholder image when icon is not provided', () => {
    const categoriesWithoutIcon = [{ id: 1, name: '分类1' }];
    const wrapper = mount(CategoryNav, {
      global: {
        mocks: {
          $router: mockRouter,
        },
      },
      props: { categories: categoriesWithoutIcon },
    });

    expect(wrapper.find('img').attributes('src')).toBe('placeholder-image.jpg');
  });

  it('should navigate when category clicked', async () => {
    const wrapper = mount(CategoryNav, {
      global: {
        mocks: {
          $router: mockRouter,
        },
      },
      props: { categories },
    });

    const firstCategory = wrapper.find('.category-item:nth-child(1)');
    await firstCategory.trigger('click');
    expect(mockRouter.push).toHaveBeenCalledWith('/products?categoryId=1');
  });

  it('should navigate when enter key pressed', async () => {
    const wrapper = mount(CategoryNav, {
      global: {
        mocks: {
          $router: mockRouter,
        },
      },
      props: { categories },
    });

    const category = wrapper.find('.category-item:nth-child(2)');
    await category.trigger('keydown.enter');
    expect(mockRouter.push).toHaveBeenCalledWith('/products?categoryId=2');
  });

  it('should navigate when space key pressed', async () => {
    const wrapper = mount(CategoryNav, {
      global: {
        mocks: {
          $router: mockRouter,
        },
      },
      props: { categories },
    });

    const category = wrapper.find('.category-item:nth-child(3)');
    await category.trigger('keydown.space');
    expect(mockRouter.push).toHaveBeenCalledWith('/products?categoryId=3');
  });

  it('should limit to 8 categories even if more provided', () => {
    const wrapper = mount(CategoryNav, {
      global: {
        mocks: {
          $router: mockRouter,
        },
      },
      props: { categories: [...categories, { id: 10, name: '更多', icon: 'icon-10.jpg' }] },
    });

    expect(wrapper.findAll('.category-item').length).toBe(8);
  });

  it('should handle empty categories array', () => {
    const wrapper = mount(CategoryNav, {
      global: {
        mocks: {
          $router: mockRouter,
        },
      },
      props: { categories: [] },
    });

    expect(wrapper.findAll('.category-item').length).toBe(0);
    expect(wrapper.find('.section-title').exists()).toBe(true);
  });

  it('should have correct aria attributes', () => {
    const wrapper = mount(CategoryNav, {
      global: {
        mocks: {
          $router: mockRouter,
        },
      },
      props: { categories },
    });

    expect(wrapper.find('.category-grid').attributes('role')).toBe('list');
    expect(wrapper.find('.category-grid').attributes('aria-label')).toBe('分类列表');
    expect(wrapper.find('.category-item:nth-child(1)').attributes('role')).toBe('listitem');
    expect(wrapper.find('.category-item:nth-child(1)').attributes('aria-label')).toBe('分类: 男装');
  });

  it('should have tabindex for accessibility', () => {
    const wrapper = mount(CategoryNav, {
      global: {
        mocks: {
          $router: mockRouter,
        },
      },
      props: { categories },
    });

    expect(wrapper.find('.category-item:nth-child(1)').attributes('tabindex')).toBe('0');
  });
});
