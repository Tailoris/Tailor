/** @vitest-environment jsdom */
import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import PriceSummary from '../PriceSummary.vue';
import ElementPlus from 'element-plus';

describe('PriceSummary', () => {
  it('should render price details correctly', () => {
    const wrapper = mount(PriceSummary, {
      global: { plugins: [ElementPlus] },
      props: { subtotal: 1000, discount: 100, shipping: 10, total: 910, submitting: false },
    });
    expect(wrapper.find('h3').text()).toBe('费用明细');
    expect(wrapper.find('.total-price').text()).toBe('¥910.00');
  });

  it('should display discount', () => {
    const wrapper = mount(PriceSummary, {
      global: { plugins: [ElementPlus] },
      props: { subtotal: 500, discount: 50, shipping: 0, total: 450, submitting: false },
    });
    expect(wrapper.find('.discount').exists()).toBe(true);
  });

  it('should emit submit event when button clicked', async () => {
    const wrapper = mount(PriceSummary, {
      global: { plugins: [ElementPlus] },
      props: { subtotal: 100, discount: 0, shipping: 0, total: 100, submitting: false },
    });
    await wrapper.find('.submit-btn').trigger('click');
    expect(wrapper.emitted('submit')).toHaveLength(1);
  });

  it('should handle zero discount correctly', () => {
    const wrapper = mount(PriceSummary, {
      global: { plugins: [ElementPlus] },
      props: { subtotal: 200, discount: 0, shipping: 15, total: 215, submitting: false },
    });
    expect(wrapper.find('.discount').text()).toBe('-¥0.00');
  });

  it('should handle free shipping correctly', () => {
    const wrapper = mount(PriceSummary, {
      global: { plugins: [ElementPlus] },
      props: { subtotal: 1000, discount: 0, shipping: 0, total: 1000, submitting: false },
    });
    expect(wrapper.find('.summary-item:nth-child(3) span:last-child').text()).toBe('-¥0.00');
  });

  it('should handle large numbers correctly', () => {
    const wrapper = mount(PriceSummary, {
      global: { plugins: [ElementPlus] },
      props: { subtotal: 999999, discount: 50000, shipping: 1000, total: 950999, submitting: false },
    });
    expect(wrapper.find('.total-price').text()).toBe('¥950999.00');
  });

  it('should have correct aria attributes', () => {
    const wrapper = mount(PriceSummary, {
      global: { plugins: [ElementPlus] },
      props: { subtotal: 100, discount: 0, shipping: 0, total: 100, submitting: false },
    });
    expect(wrapper.find('.price-summary').attributes('aria-label')).toBe('费用明细');
  });
});
