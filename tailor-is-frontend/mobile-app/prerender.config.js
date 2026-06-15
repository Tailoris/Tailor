/**
 * 移动端预渲染配置
 *
 * 配合 prerender-spa-plugin 或类似方案使用，在构建时预渲染
 * 关键页面为静态 HTML，提升首屏加载速度和 SEO。
 *
 * 使用方式：
 *   1. npm install prerender-spa-plugin --save-dev
 *   2. 在 vite.config.ts 中引入此配置
 *   3. 运行 build 时自动预渲染
 *
 * 注意：uni-app H5 构建可使用 vite-plugin-prerender 替代
 */

const PrerenderRoutes = {
  // 需要预渲染的页面路由列表
  routes: [
    {
      path: '/',
      meta: {
        title: '首页 - Tailor IS',
        description: 'Tailor IS 服装全产业平台首页，提供精选服装商品',
        keywords: '服装,电商,Tailor IS'
      }
    },
    {
      path: '/category',
      meta: {
        title: '商品分类 - Tailor IS',
        description: '浏览服装商品分类',
        keywords: '分类,服装'
      }
    },
    {
      path: '/product/detail',
      meta: {
        title: '商品详情 - Tailor IS',
        description: '查看商品详细信息',
        keywords: '商品,详情'
      }
    },
    {
      path: '/community/list',
      meta: {
        title: '社区 - Tailor IS',
        description: '服装搭配社区',
        keywords: '社区,搭配'
      }
    }
  ],

  // 预渲染配置选项
  options: {
    // 渲染后等待时间(ms)，确保异步数据加载完成
    renderAfterDocumentEvent: 'render-event',
    // 渲染超时时间(ms)
    renderAfterTime: 5000,
    // 是否在渲染时移除客户端 hydration 脚本
    removeScriptTags: false,
    // 是否使用 headless Chrome 渲染
    headless: true,
    // 页面渲染完成后触发的事件名
    documentEventName: 'prerender-ready'
  },

  // 预渲染中间件：在页面渲染完成后执行
  postProcess(context) {
    // 注入 SEO meta 标签
    const route = this.routes.find(r => r.path === context.route.path)
    if (route?.meta) {
      context.html = context.html
        .replace('<title>', `<title>${route.meta.title} | Tailor IS</title><!-- original:`)
        .replace('</title>', '-->')
    }

    // 注入 preload/prefetch 资源提示
    const preloadLinks = `
      <link rel="preload" href="/assets/index.css" as="style">
      <link rel="prefetch" href="/assets/vendor.js" as="script">
    `
    context.html = context.html.replace('</head>', `${preloadLinks}</head>`)

    // 移除预渲染时的加载状态
    context.html = context.html.replace(
      /<div[^>]*class="loading"[^>]*>.*?<\/div>/s,
      ''
    )

    return context.html
  }
}

/**
 * Vite 预渲染插件配置（适用于 vite-plugin-prerender）
 *
 * 在 vite.config.ts 中使用：
 * import { prerenderPlugin } from './prerender.config'
 * export default defineConfig({ plugins: [prerenderPlugin()] })
 */
export function prerenderPluginConfig() {
  return {
    routes: PrerenderRoutes.routes.map(r => r.path),
    ...PrerenderRoutes.options
  }
}

export default PrerenderRoutes
