import { vi } from 'vitest'

vi.mock('@/router', () => ({
  default: {
    currentRoute: { value: { path: '/' } },
    push: vi.fn()
  }
}))



vi.mock('@element-plus/icons-vue', () => ({
  Search: {},
  ShoppingCart: {},
  ArrowDown: {}
}))

vi.mock('@shared/utils/placeholder', () => ({
  PLACEHOLDER_32x32: 'placeholder-32x32.jpg',
  PLACEHOLDER_60x60: 'placeholder-60x60.jpg'
}))

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key
  })
}))