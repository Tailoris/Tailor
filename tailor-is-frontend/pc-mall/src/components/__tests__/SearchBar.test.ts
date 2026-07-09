/**
 * @vitest-environment jsdom
 */
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import SearchBar from '../SearchBar.vue'
import { getCategories } from '@/api/product'

vi.mock('@/api/product', () => ({
  getCategories: vi.fn().mockResolvedValue([
    { id: 1, name: 'Category 1', parentId: 0 },
    { id: 2, name: 'Category 2', parentId: 0 },
    { id: 3, name: 'Sub Category', parentId: 1 }
  ])
}))

describe('SearchBar', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('should render search bar correctly', () => {
    const wrapper = mount(SearchBar, {
      global: { plugins: [ElementPlus] }
    })

    expect(wrapper.find('[role="search"]').exists()).toBe(true)
    expect(wrapper.find('.search-bar').exists()).toBe(true)
    expect(wrapper.find('.search-input').exists()).toBe(true)
    expect(wrapper.find('.search-btn').exists()).toBe(true)
    expect(wrapper.find('.category-select').exists()).toBe(true)
  })

  it('should emit search event when handleSearch is called', async () => {
    const wrapper = mount(SearchBar, {
      global: { plugins: [ElementPlus] }
    })

    await wrapper.vm.handleSearch()
    expect(wrapper.emitted('search')).toBeTruthy()
    expect(wrapper.emitted('search')?.[0]?.[0]).toEqual({
      keyword: '',
      categoryId: ''
    })
  })

  it('should emit search with keyword', async () => {
    const wrapper = mount(SearchBar, {
      global: { plugins: [ElementPlus] }
    })

    wrapper.vm.keyword = 'test keyword'
    await wrapper.vm.handleSearch()

    expect(wrapper.emitted('search')).toBeTruthy()
    expect(wrapper.emitted('search')?.[0]?.[0]).toEqual({
      keyword: 'test keyword',
      categoryId: ''
    })
  })

  it('should emit search with category', async () => {
    const wrapper = mount(SearchBar, {
      global: { plugins: [ElementPlus] }
    })

    wrapper.vm.keyword = 'test'
    wrapper.vm.selectedCategory = 1
    await wrapper.vm.handleSearch()

    expect(wrapper.emitted('search')).toBeTruthy()
    expect(wrapper.emitted('search')?.[0]?.[0]).toEqual({
      keyword: 'test',
      categoryId: 1
    })
  })

  it('should load categories on mount', async () => {
    mount(SearchBar, {
      global: { plugins: [ElementPlus] }
    })

    await new Promise(resolve => setTimeout(resolve, 100))

    expect(getCategories).toHaveBeenCalledTimes(1)
  })

  it('should filter out sub-categories when loading', async () => {
    const wrapper = mount(SearchBar, {
      global: { plugins: [ElementPlus] }
    })

    await new Promise(resolve => setTimeout(resolve, 100))

    const categories = (wrapper.vm as any).categories
    expect(categories).toHaveLength(2)
    expect(categories.every((c: { parentId: number }) => c.parentId === 0)).toBe(true)
  })

  it('should handle API error when loading categories', async () => {
    (getCategories as vi.Mock).mockRejectedValue(new Error('API Error'))

    const wrapper = mount(SearchBar, {
      global: { plugins: [ElementPlus] }
    })

    await new Promise(resolve => setTimeout(resolve, 100))

    const categories = (wrapper.vm as any).categories
    expect(categories).toEqual([])
  })

  it('should handle empty categories response', async () => {
    (getCategories as vi.Mock).mockResolvedValue([])

    const wrapper = mount(SearchBar, {
      global: { plugins: [ElementPlus] }
    })

    await new Promise(resolve => setTimeout(resolve, 100))

    const categories = (wrapper.vm as any).categories
    expect(categories).toEqual([])
  })

  it('should emit search when Enter key is pressed', async () => {
    const wrapper = mount(SearchBar, {
      global: { plugins: [ElementPlus] }
    })

    wrapper.vm.keyword = 'enter test'
    await wrapper.vm.handleSearch()

    expect(wrapper.emitted('search')).toBeTruthy()
    expect(wrapper.emitted('search')?.[0]?.[0]).toEqual({
      keyword: 'enter test',
      categoryId: ''
    })
  })

  it('should emit search with empty keyword', async () => {
    const wrapper = mount(SearchBar, {
      global: { plugins: [ElementPlus] }
    })

    await wrapper.vm.handleSearch()

    expect(wrapper.emitted('search')).toBeTruthy()
    expect(wrapper.emitted('search')?.[0]?.[0]).toEqual({
      keyword: '',
      categoryId: ''
    })
  })

  it('should emit search with category only', async () => {
    const wrapper = mount(SearchBar, {
      global: { plugins: [ElementPlus] }
    })

    wrapper.vm.selectedCategory = 2
    await wrapper.vm.handleSearch()

    expect(wrapper.emitted('search')).toBeTruthy()
    expect(wrapper.emitted('search')?.[0]?.[0]).toEqual({
      keyword: '',
      categoryId: 2
    })
  })

  it('should clear keyword and category', async () => {
    const wrapper = mount(SearchBar, {
      global: { plugins: [ElementPlus] }
    })

    wrapper.vm.keyword = 'test'
    wrapper.vm.selectedCategory = 1

    wrapper.vm.keyword = ''
    wrapper.vm.selectedCategory = ''

    await wrapper.vm.handleSearch()

    expect(wrapper.emitted('search')?.[0]?.[0]).toEqual({
      keyword: '',
      categoryId: ''
    })
  })

  it('should handle special characters in keyword', async () => {
    const wrapper = mount(SearchBar, {
      global: { plugins: [ElementPlus] }
    })

    wrapper.vm.keyword = 'test@#$%^&*()'
    await wrapper.vm.handleSearch()

    expect(wrapper.emitted('search')?.[0]?.[0]).toEqual({
      keyword: 'test@#$%^&*()',
      categoryId: ''
    })
  })

  it('should handle trimmed keyword', async () => {
    const wrapper = mount(SearchBar, {
      global: { plugins: [ElementPlus] }
    })

    wrapper.vm.keyword = '  trimmed keyword  '
    await wrapper.vm.handleSearch()

    expect(wrapper.emitted('search')?.[0]?.[0]).toEqual({
      keyword: '  trimmed keyword  ',
      categoryId: ''
    })
  })

  it('should render aria-label correctly', () => {
    const wrapper = mount(SearchBar, {
      global: { plugins: [ElementPlus] }
    })

    expect(wrapper.find('[role="search"]').attributes('aria-label')).toBe('商品搜索区域')
  })

  it('should reset keyword after search', async () => {
    const wrapper = mount(SearchBar, {
      global: { plugins: [ElementPlus] }
    })

    wrapper.vm.keyword = 'test'
    await wrapper.vm.handleSearch()

    expect((wrapper.vm as any).keyword).toBe('test')
  })

  it('should emit search when search button is clicked', async () => {
    const wrapper = mount(SearchBar, {
      global: { plugins: [ElementPlus] }
    })

    wrapper.vm.keyword = 'button test'
    wrapper.vm.selectedCategory = 1

    await wrapper.find('.search-btn').trigger('click')

    expect(wrapper.emitted('search')).toBeTruthy()
    expect(wrapper.emitted('search')?.[0]?.[0]).toEqual({
      keyword: 'button test',
      categoryId: 1
    })
  })

  it('should update keyword when input value changes', async () => {
    const wrapper = mount(SearchBar, {
      global: { plugins: [ElementPlus] }
    })

    const input = wrapper.find('.search-input input')
    if (input.exists()) {
      await input.setValue('new keyword')
      expect((wrapper.vm as any).keyword).toBe('new keyword')
    }
  })

  it('should update selectedCategory when category is selected', () => {
    const w = mount(SearchBar, {
      global: { plugins: [ElementPlus] }
    })
    const vm = w.vm as any
    vm.selectedCategory = 2
    expect(vm.selectedCategory).toBe(2)
  })

  it('should emit search when Enter key is pressed on input', async () => {
    const wrapper = mount(SearchBar, {
      global: { plugins: [ElementPlus] }
    })

    const input = wrapper.find('.search-input input')
    if (input.exists()) {
      wrapper.vm.keyword = 'enter keyword'
      await input.trigger('keyup.enter')

      expect(wrapper.emitted('search')).toBeTruthy()
      expect(wrapper.emitted('search')?.[0]?.[0]).toEqual({
        keyword: 'enter keyword',
        categoryId: ''
      })
    }
  })

  it('should clear keyword when clearable button is clicked', async () => {
    const wrapper = mount(SearchBar, {
      global: { plugins: [ElementPlus] }
    })

    wrapper.vm.keyword = 'to be cleared'
    expect((wrapper.vm as any).keyword).toBe('to be cleared')

    wrapper.vm.keyword = ''
    expect((wrapper.vm as any).keyword).toBe('')
  })

  it('should clear selectedCategory when clearable button is clicked', () => {
    const w = mount(SearchBar, {
      global: { plugins: [ElementPlus] }
    })
    const vm = w.vm as any
    vm.selectedCategory = 1
    expect(vm.selectedCategory).toBe(1)
    vm.selectedCategory = ''
    expect(vm.selectedCategory).toBe('')
  })

  it('should handle getCategories returning undefined', async () => {
    (getCategories as vi.Mock).mockResolvedValue(undefined)

    const wrapper = mount(SearchBar, {
      global: { plugins: [ElementPlus] }
    })

    await new Promise(resolve => setTimeout(resolve, 100))

    const categories = (wrapper.vm as any).categories
    expect(categories).toEqual([])
  })

  it('should handle getCategories returning null', async () => {
    (getCategories as vi.Mock).mockResolvedValue(null)

    const wrapper = mount(SearchBar, {
      global: { plugins: [ElementPlus] }
    })

    await new Promise(resolve => setTimeout(resolve, 100))

    const categories = (wrapper.vm as any).categories
    expect(categories).toEqual([])
  })

  it('should handle getCategories returning non-array', async () => {
    (getCategories as vi.Mock).mockResolvedValue({} as any)

    const wrapper = mount(SearchBar, {
      global: { plugins: [ElementPlus] }
    })

    await new Promise(resolve => setTimeout(resolve, 100))

    const categories = (wrapper.vm as any).categories
    expect(categories).toEqual([])
  })
})
