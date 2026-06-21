// ==============================================================================
// Tailor IS - PC Mall 前端性能优化配置
// Phase 3 P3-4: CDN + 前端性能优化 (首屏 < 2s)
// ==============================================================================

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import viteCompression from 'vite-plugin-compression'
import { visualizer } from 'rollup-plugin-visualizer'

// CDN 配置 (生产环境)
const CDN_BASE = process.env.CDN_BASE_URL || '/'

export default defineConfig({
  plugins: [
    vue(),
    // Gzip 压缩
    viteCompression({
      algorithm: 'gzip',
      ext: '.gz',
      threshold: 10240, // 10KB+
      deleteOriginFile: false,
    }),
    // Brotli 压缩 (更好的压缩率)
    viteCompression({
      algorithm: 'brotliCompress',
      ext: '.br',
      threshold: 10240,
      deleteOriginFile: false,
    }),
    // Bundle 分析 (构建时启用)
    ...(process.env.ANALYZE === 'true' ? [visualizer({
      open: true,
      gzipSize: true,
      brotliSize: true,
      filename: 'dist/stats.html',
    })] : []),
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
      '@shared': resolve(__dirname, '../shared'),
    },
  },
  // CDN 基础路径
  base: CDN_BASE,
  server: {
    port: 3001,
    proxy: {
      '/api': {
        target: process.env.VITE_API_BASE_URL || 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    target: 'es2020', // 现代浏览器，更小的输出
    outDir: 'dist',
    assetsDir: 'assets',
    // 启用 CSS 代码分割
    cssCodeSplit: true,
    // 资源内联阈值 (4KB 以下内联为 base64)
    assetsInlineLimit: 4096,
    // 生成 sourcemap (生产环境关闭)
    sourcemap: process.env.NODE_ENV === 'development',
    // 压缩选项
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true,       // 移除 console
        drop_debugger: true,      // 移除 debugger
        pure_funcs: ['console.log'],
      },
      output: {
        comments: false,          // 移除注释
      },
    },
    // Rollup 配置
    rollupOptions: {
      output: {
        // 代码分割策略
        manualChunks: {
          // 框架层
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
          // UI 组件库
          'element-plus': ['element-plus', '@element-plus/icons-vue'],
          // 工具库
          'utils-vendor': ['axios', 'dayjs', 'lodash-es'],
          // 图片相关
          'media-vendor': ['swiper', 'vue3-lazyload'],
        },
        // 输出命名
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
    // 打包体积警告阈值 (500KB)
    chunkSizeWarningLimit: 500,
    // 构建性能
    reportCompressedSize: true,
  },
  // esbuild 配置：生产构建时移除 console 和 debugger 调用
  // 与 terser drop_console 互补，确保 SSR/开发模式转换阶段也能移除
  esbuild: {
    drop: process.env.NODE_ENV === 'production' ? ['console', 'debugger'] : [],
  },
  // CSS 预处理器
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: '',
      },
    },
    // CSS Modules
    modules: {
      localsConvention: 'camelCaseOnly',
    },
  },
  // SSR 配置
  ssr: {
    noExternal: ['vue', 'vue-router', 'pinia', 'element-plus', '@element-plus/icons-vue'],
  },
  // 优化依赖预构建
  optimizeDeps: {
    include: ['vue', 'vue-router', 'pinia', 'element-plus', 'axios'],
    exclude: [],
  },
})