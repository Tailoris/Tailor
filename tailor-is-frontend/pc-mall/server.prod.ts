/**
 * 生产环境 SSR 入口.
 *
 * <p>使用预编译的 server bundle（非 Vite 中间件），
 * 通过 Cluster 模式启动多个工作进程以充分利用多核 CPU。
 *
 * <h3>特性</h3>
 * <ul>
 *   <li>Cluster 模式：worker 进程数 = CPU 核心数</li>
 *   <li>优雅关闭：SIGTERM/SIGINT 时逐步断开连接</li>
 *   <li>健康检查：/health 端点</li>
 *   <li>静态文件缓存：immutable 资源 1 年，其他 1 小时</li>
 *   <li>Gzip/Brotli 压缩</li>
 * </ul>
 */

import cluster from 'cluster'
import { availableParallelism } from 'os'
import express from 'express'
import compression from 'compression'
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const PORT = Number(process.env.PORT) || 3000
const NUM_WORKERS = Number(process.env.WORKER_COUNT) || availableParallelism()

// -------- Worker 进程 --------
async function startWorker() {
  const app = express()

  // 压缩中间件
  app.use(compression({
    level: 6,
    threshold: 1024
  }))

  // 静态文件服务（带缓存头）
  const clientDist = path.resolve(__dirname, '../dist/client')
  app.use('/assets', express.static(path.join(clientDist, 'assets'), {
    maxAge: '1y',
    immutable: true
  }))
  app.use('/js', express.static(path.join(clientDist, 'js'), {
    maxAge: '1y',
    immutable: true
  }))
  app.use('/css', express.static(path.join(clientDist, 'css'), {
    maxAge: '1y',
    immutable: true
  }))
  app.use('/images', express.static(path.join(clientDist, 'images'), {
    maxAge: '1y',
    immutable: true
  }))
  app.use('/fonts', express.static(path.join(clientDist, 'fonts'), {
    maxAge: '1y',
    immutable: true
  }))
  app.use(express.static(clientDist, {
    maxAge: '1h',
    setHeaders: (res, filePath) => {
      if (filePath.endsWith('.html')) {
        res.setHeader('Cache-Control', 'no-cache')
      }
    }
  }))

  // 健康检查
  app.get('/health', (_req, res) => {
    res.status(200).json({
      status: 'healthy',
      mode: 'production',
      worker: cluster.worker?.id || 'unknown',
      uptime: process.uptime()
    })
  })

  // SSR 渲染
  app.use('*', async (req, res) => {
    try {
      const template = path.resolve(__dirname, '../dist/client/index.html')

      if (!fs.existsSync(template)) {
        res.status(500).send('Build output not found. Run `npm run build:ssr` first.')
        return
      }

      let html = fs.readFileSync(template, 'utf-8')

      // 生产环境：使用预编译的 SSR 入口
      const { render } = await import(
        path.resolve(__dirname, '../dist/server/entry-server.js')
      )

      const { html: appHtml, pinia } = await render(req.url)
      const piniaState = pinia.state.value

      const rendered = html
        .replace('<!--app-html-->', appHtml)
        .replace(
          '</head>',
          `<script>window.__PINIA_STATE__=${JSON.stringify(piniaState).replace(/</g, '\\u003c')}</script></head>`
        )

      res.status(200).set({ 'Content-Type': 'text/html' }).end(rendered)
    } catch (e) {
      console.error(`[Worker ${cluster.worker?.id}] SSR render error:`, e)
      res.status(500).send('Internal Server Error')
    }
  })

  const server = app.listen(PORT, () => {
    console.log(`[Worker ${cluster.worker?.id}] SSR server running at http://localhost:${PORT}`)
  })

  // 优雅关闭
  const gracefulShutdown = (signal: string) => {
    console.log(`[Worker ${cluster.worker?.id}] Received ${signal}, shutting down gracefully...`)
    server.close(() => {
      console.log(`[Worker ${cluster.worker?.id}] Server closed`)
      process.exit(0)
    })

    // 超时强制退出
    setTimeout(() => {
      console.error(`[Worker ${cluster.worker?.id}] Force shutdown after timeout`)
      process.exit(1)
    }, 30000)
  }

  process.on('SIGTERM', () => gracefulShutdown('SIGTERM'))
  process.on('SIGINT', () => gracefulShutdown('SIGINT'))
}

// -------- 主进程 (Cluster Master) --------
if (cluster.isPrimary) {
  console.log(`[Master] Starting ${NUM_WORKERS} worker processes...`)
  console.log(`[Master] PID: ${process.pid}`)

  // Fork workers
  for (let i = 0; i < NUM_WORKERS; i++) {
    cluster.fork()
  }

  cluster.on('exit', (worker, code, signal) => {
    console.warn(`[Master] Worker ${worker.id} died (${signal || code}). Restarting...`)
    // 延迟重启避免快速循环
    setTimeout(() => cluster.fork(), 1000)
  })

  cluster.on('online', (worker) => {
    console.log(`[Master] Worker ${worker.id} online`)
  })

  // 主进程优雅关闭
  const masterShutdown = (signal: string) => {
    console.log(`[Master] Received ${signal}, shutting down all workers...`)

    const workers = cluster.workers
    if (workers) {
      for (const id of Object.keys(workers)) {
        const worker = workers[id]
        if (worker) {
          worker.kill('SIGTERM')
        }
      }
    }

    // 超时后强制杀死
    setTimeout(() => {
      console.error('[Master] Force killing remaining workers...')
      if (workers) {
        for (const id of Object.keys(workers)) {
          const worker = workers[id]
          if (worker) {
            worker.kill('SIGKILL')
          }
        }
      }
      process.exit(1)
    }, 35000)
  }

  process.on('SIGTERM', () => masterShutdown('SIGTERM'))
  process.on('SIGINT', () => masterShutdown('SIGINT'))
} else {
  // Worker 进程
  startWorker().catch((err) => {
    console.error(`[Worker ${cluster.worker?.id}] Failed to start:`, err)
    process.exit(1)
  })
}