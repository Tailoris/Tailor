// ==============================================================================
// Tailor IS - Merchant Admin 前端性能优化配置
// Phase 3 P3-4: CDN + 前端性能优化 (首屏 < 2s)
// ==============================================================================

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import viteCompression from 'vite-plugin-compression'

const CDN_BASE = process.env.CDN_BASE_URL || '/merchant/'

export default defineConfig({
  plugins: [
    vue(),
    viteCompression({
      algorithm: 'gzip',
      ext: '.gz',
      threshold: 10240,
      deleteOriginFile: false,
    }),
    viteCompression({
      algorithm: 'brotliCompress',
      ext: '.br',
      threshold: 10240,
      deleteOriginFile: false,
    }),
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
      '@shared': resolve(__dirname, '../shared'),
    },
  },
  base: CDN_BASE,
  server: {
    host: '0.0.0.0',
    port: 3002,
    proxy: {
      '/api': {
        target: 'http://localhost:9000',
        changeOrigin: true,
      },
    },
  },
  build: {
    target: 'es2020',
    outDir: 'dist',
    assetsDir: 'assets',
    cssCodeSplit: true,
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
        // 🔒 修复: 合并 element-plus 到 vue-vendor 以避免循环依赖
        manualChunks(id) {
          if (!id.includes('node_modules')) return
          if (id.includes('vue') || id.includes('pinia') || id.includes('vue-router') || id.includes('@vue/') || id.includes('element-plus') || id.includes('@element-plus/')) {
            return 'vue-vendor'
          }
          if (id.includes('axios') || id.includes('dayjs') || id.includes('lodash-es') || id.includes('lodash')) {
            return 'utils-vendor'
          }
        },
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
  // esbuild 配置：生产构建时移除 console 和 debugger 调用
  esbuild: {
    drop: process.env.NODE_ENV === 'production' ? ['console', 'debugger'] : [],
  },
  optimizeDeps: {
    include: ['vue', 'vue-router', 'pinia', 'element-plus', 'axios'],
  },
})