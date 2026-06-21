// ==============================================================================
// Tailor IS - Mobile App 前端性能优化配置
// Phase 3 P3-4: CDN + 前端性能优化 (首屏 < 2s)
// UX-P2-01: Service Worker 离线浏览支持
// ==============================================================================

import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'
import { resolve } from 'path'
import { writeFileSync, existsSync, mkdirSync } from 'fs'

const CDN_BASE = process.env.CDN_BASE_URL || '/'

/**
 * 自定义 Vite 插件：编译 Service Worker
 *
 * 将 TypeScript 编写的 Service Worker 编译为独立的 JavaScript 文件，
 * 输出到构建目录的根路径下，确保 SW 可以控制整个 scope。
 *
 * uni-app 的 H5 构建使用 Vite，本插件在 build 阶段介入，
 * 使用 esbuild 将 sw.ts 编译为 sw.js 并输出到 dist/build/h5/sw.js。
 */
function serviceWorkerPlugin() {
  let swSourcePath = resolve(__dirname, 'src/service-worker/sw.ts')

  return {
    name: 'tailor-is-sw-plugin',
    enforce: 'post' as const,

    // 开发模式下，将 sw.js 从 src/service-worker 复制到 dev server 根路径
    configureServer(server: any) {
      server.middlewares.use('/sw.js', async (_req: any, res: any) => {
        try {
          const { build } = await import('esbuild')
          const result = await build({
            entryPoints: [swSourcePath],
            bundle: true,
            format: 'iife',
            target: 'es2020',
            write: false,
            minify: false,
            platform: 'browser',
            define: {
              'process.env.NODE_ENV': JSON.stringify('development'),
            },
          })
          const code = result.outputFiles[0].text
          res.setHeader('Content-Type', 'application/javascript')
          res.setHeader('Service-Worker-Allowed', '/')
          res.end(code)
        } catch (err) {
          console.error('[SW Plugin] Dev compilation error:', err)
          res.statusCode = 500
          res.end('// SW compilation failed')
        }
      })
    },

    // 构建模式下，使用 esbuild 编译 sw.ts 并输出到 dist 根目录
    async writeBundle() {
      const outDir = resolve(__dirname, 'dist/build/h5')
      if (!existsSync(outDir)) {
        mkdirSync(outDir, { recursive: true })
      }

      try {
        const { build } = await import('esbuild')
        const result = await build({
          entryPoints: [swSourcePath],
          bundle: true,
          format: 'iife',
          target: 'es2020',
          write: false,
          minify: true,
          platform: 'browser',
          define: {
            'process.env.NODE_ENV': JSON.stringify('production'),
          },
        })

        const outputPath = resolve(outDir, 'sw.js')
        writeFileSync(outputPath, result.outputFiles[0].text)
        console.log(`[SW Plugin] Service Worker built → ${outputPath}`)
      } catch (err) {
        console.error('[SW Plugin] Build compilation error:', err)
      }
    },
  }
}

export default defineConfig({
  plugins: [uni(), serviceWorkerPlugin()],
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