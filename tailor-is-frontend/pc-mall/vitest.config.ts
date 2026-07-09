import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  test: {
    globals: true,
    setupFiles: ['vitest.setup.ts'],
    exclude: ['e2e/**'],
    coverage: {
      provider: 'v8',
      exclude: [
        'node_modules/**',
        'dist/**',
        'src/server/**',
        'src/router/**',
        'src/store/**',
        'e2e/**'
      ]
    },
    environmentMatchGlobs: [
      ['**/__tests__/home-api.test.ts', 'node'],
      ['**/__tests__/*.test.ts', 'jsdom']
    ]
  },
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
      '@shared': resolve(__dirname, '../shared')
    }
  }
})