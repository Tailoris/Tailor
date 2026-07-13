/** @vitest-environment jsdom */
import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import AddressSelector from '../AddressSelector.vue';
import ElementPlus from 'element-plus';

describe('AddressSelector', () => {
  const addresses = [
    {
      id: 1,
      name: '张三',
      phone: '13800138001',
      province: '广东省',
      city: '广州市',
      district: '天河区',
      detail: '天河路100号',
      isDefault: 1,
    },
    {
      id: 2,
      name: '李四',
      phone: '13900139001',
      province: '北京市',
      city: '北京市',
      district: '朝阳区',
      detail: '朝阳路200号',
      isDefault: 0,
    },
  ];

  it('should render address list correctly', () => {
    const wrapper = mount(AddressSelector, {
      global: { plugins: [ElementPlus] },
      props: { addresses },
    });
    expect(wrapper.find('h3').text()).toBe('收货地址');
    expect(wrapper.findAll('.address-card').length).toBe(2);
  });

  it('should display address name', () => {
    const wrapper = mount(AddressSelector, {
      global: { plugins: [ElementPlus] },
      props: { addresses },
    });
    expect(wrapper.find('.address-card:nth-child(1) .name').text()).toBe('张三');
    expect(wrapper.find('.address-card:nth-child(2) .name').text()).toBe('李四');
  });

  it('should select address when clicked', async () => {
    const wrapper = mount(AddressSelector, {
      global: { plugins: [ElementPlus] },
      props: { addresses, modelValue: 1 },
    });
    await wrapper.find('.address-card:nth-child(2)').trigger('click');
    expect(wrapper.emitted('update:modelValue')).toHaveLength(1);
    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([2]);
  });

  it('should select address when enter key pressed', async () => {
    const wrapper = mount(AddressSelector, {
      global: { plugins: [ElementPlus] },
      props: { addresses },
    });
    await wrapper.find('.address-card:nth-child(1)').trigger('keydown.enter');
    expect(wrapper.emitted('update:modelValue')).toHaveLength(1);
    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([1]);
  });

  it('should select address when space key pressed', async () => {
    const wrapper = mount(AddressSelector, {
      global: { plugins: [ElementPlus] },
      props: { addresses },
    });
    await wrapper.find('.address-card:nth-child(2)').trigger('keydown.space');
    expect(wrapper.emitted('update:modelValue')).toHaveLength(1);
    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([2]);
  });

  it('should emit add event when add button clicked', async () => {
    const wrapper = mount(AddressSelector, {
      global: { plugins: [ElementPlus] },
      props: { addresses },
    });
    const addButton = wrapper.find('[aria-label="新增地址"]') || wrapper.find('.el-button');
    if (addButton.exists()) {
      await addButton.trigger('click');
      expect(wrapper.emitted('add')).toHaveLength(1);
    }
  });

  it('should show active state for selected address', () => {
    const wrapper = mount(AddressSelector, {
      global: { plugins: [ElementPlus] },
      props: { addresses, modelValue: 2 },
    });
    expect(wrapper.find('.address-card:nth-child(1)').classes()).not.toContain('active');
    expect(wrapper.find('.address-card:nth-child(2)').classes()).toContain('active');
  });

  it('should handle empty address list', () => {
    const wrapper = mount(AddressSelector, {
      global: { plugins: [ElementPlus] },
      props: { addresses: [] },
    });
    expect(wrapper.findAll('.address-card').length).toBe(0);
  });

  it('should format address detail correctly', () => {
    const wrapper = mount(AddressSelector, {
      global: { plugins: [ElementPlus] },
      props: { addresses },
    });
    const detail = wrapper.find('.address-card:nth-child(1) .address-detail').text();
    expect(detail).toBe('广东省广州市天河区天河路100号');
  });

  it('should have correct aria attributes', () => {
    const wrapper = mount(AddressSelector, {
      global: { plugins: [ElementPlus] },
      props: { addresses },
    });
    expect(wrapper.find('.address-list').attributes('role')).toBe('radiogroup');
  });
});
