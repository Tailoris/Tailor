// ==============================================================================
// Tailor IS - Service Worker 独立构建脚本
// 离线浏览支持 (UX-P2-01)
//
// 用法: node scripts/build-sw.mjs
//
// 使用 esbuild 将 TypeScript 编写的 Service Worker 编译为独立的 JS 文件，
// 输出到 dist/build/h5/sw.js，确保 SW 可以控制整个 scope。
// ==============================================================================

import { build } from 'esbuild'
import { resolve, dirname } from 'path'
import { existsSync, mkdirSync, writeFileSync } from 'fs'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = dirname(__filename)

const projectRoot = resolve(__dirname, '..')
const swSource = resolve(projectRoot, 'src/service-worker/sw.ts')
const outDir = resolve(projectRoot, 'dist/build/h5')
const outFile = resolve(outDir, 'sw.js')

async function buildSW() {
  // 确保输出目录存在
  if (!existsSync(outDir)) {
    mkdirSync(outDir, { recursive: true })
    console.log(`[build-sw] Created output directory: ${outDir}`)
  }

  console.log(`[build-sw] Building Service Worker...`)
  console.log(`  Source: ${swSource}`)
  console.log(`  Output: ${outFile}`)

  try {
    const result = await build({
      entryPoints: [swSource],
      bundle: true,
      format: 'iife',
      target: 'es2020',
      write: false,
      minify: process.env.NODE_ENV === 'production',
      platform: 'browser',
      define: {
        'process.env.NODE_ENV': JSON.stringify(process.env.NODE_ENV || 'production'),
      },
    })

    const sizeKB = (result.outputFiles[0].text.length / 1024).toFixed(1)
    writeFileSync(outFile, result.outputFiles[0].text)
    console.log(`[build-sw] ✓ Service Worker built successfully (${sizeKB} KB)`)
    console.log(`[build-sw]   Output: ${outFile}`)
  } catch (err) {
    console.error('[build-sw] ✗ Build failed:', err.message)
    process.exit(1)
  }
}

buildSW()