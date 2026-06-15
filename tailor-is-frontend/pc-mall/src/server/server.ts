import express from 'express'
import { createServer as createViteServer } from 'vite'
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const isProduction = process.env.NODE_ENV === 'production'

const port = process.env.PORT || 3000

async function createServer() {
  const app = express()

  // 生产环境：提供静态文件
  if (isProduction) {
    const clientDist = path.resolve(__dirname, '../../dist/client')
    app.use(express.static(clientDist))

    app.get('/health', (_req, res) => {
      res.status(200).json({ status: 'healthy', mode: 'production' })
    })

    app.use('*', async (req, res) => {
      try {
        const template = path.resolve(__dirname, '../../dist/client/index.html')

        if (!fs.existsSync(template)) {
          // SPA fallback: 返回构建后的 index.html
          const fallbackHtml = path.resolve(__dirname, '../../dist/client/index.html')
          if (fs.existsSync(fallbackHtml)) {
            res.status(200).set({ 'Content-Type': 'text/html' }).send(fs.readFileSync(fallbackHtml, 'utf-8'))
            return
          }
          res.status(500).send('Build output not found. Run `npm run build:ssr` first.')
          return
        }

        let html = fs.readFileSync(template, 'utf-8')

        // 生产环境：使用预编译的 SSR 入口
        const { render } = await import(
          path.resolve(__dirname, '../../dist/server/entry-server.js')
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
        console.error('SSR render error:', e)
        res.status(500).send('Internal Server Error')
      }
    })

    app.listen(port, () => {
      console.log(`SSR server (production) running at http://localhost:${port}`)
    })
    return
  }

  // 开发环境：使用 Vite 中间件
  const vite = await createViteServer({
    server: { middlewareMode: true },
    appType: 'custom'
  })

  app.use(vite.middlewares)

  app.get('/health', (_req, res) => {
    res.status(200).json({ status: 'healthy', mode: 'development' })
  })

  app.use('*', async (req, res, next) => {
    const url = req.originalUrl

    try {
      const template = path.resolve(__dirname, '../../index.html')
      let html = fs.readFileSync(template, 'utf-8')

      html = await vite.transformIndexHtml(url, html)

      const { createApp } = await vite.ssrLoadModule('/src/server/entry-server.ts')
      const { html: appHtml, pinia } = await createApp(url)
      const piniaState = pinia.state.value

      const rendered = html
        .replace('<!--app-html-->', appHtml)
        .replace(
          '</head>',
          `<script>window.__PINIA_STATE__=${JSON.stringify(piniaState).replace(/</g, '\\u003c')}</script></head>`
        )

      res.status(200).set({ 'Content-Type': 'text/html' }).end(rendered)
    } catch (e) {
      vite.ssrFixStacktrace(e as Error)
      next(e)
    }
  })

  app.listen(port, () => {
    console.log(`SSR server (development) running at http://localhost:${port}`)
  })
}

createServer()
