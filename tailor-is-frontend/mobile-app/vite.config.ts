// ==============================================================================
// Tailor IS - Mobile App 前端性能优化配置
// Phase 3 P3-4: CDN + 前端性能优化 (首屏 < 2s)
// ==============================================================================

import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'
import { resolve } from 'path'

const CDN_BASE = process.env.CDN_BASE_URL || '/'

export default defineConfig({
  plugins: [uni()],
  resolve: {
    alias: {
      '@shared': resolve(__dirname, '../shared'),
    },
  },
  base: CDN_BASE,
  server: {
    host: '0.0.0.0',
    port: 5173,
    fs: {
      strict: false,
    },
  },
  publicDir: 'static',
  build: {
    target: 'es2020',
    assetsInlineLimit: 4096,
    sourcemap: false,
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true,
        drop_debugger: true,
      },
      output: {
        comments: false,
      },
    },
    rollupOptions: {
      output: {
        chunkFileNames: 'js/[name]-[hash:8].js',
        entryFileNames: 'js/[name]-[hash:8].js',
        assetFileNames: (assetInfo) => {
          const ext = assetInfo.name?.split('.').pop()
          if (/\.(png|jpe?g|gif|svg|webp|ico)$/i.test(ext || '')) return 'images/[name]-[hash:8].[ext]'
          if (/\.(woff2?|eot|ttf|otf)$/i.test(ext || '')) return 'fonts/[name]-[hash:8].[ext]'
          if (/\.css$/i.test(ext || '')) return 'css/[name]-[hash:8].[ext]'
          return 'assets/[name]-[hash:8].[ext]'
        },
      },
    },
    chunkSizeWarningLimit: 500,
    reportCompressedSize: true,
  },
})