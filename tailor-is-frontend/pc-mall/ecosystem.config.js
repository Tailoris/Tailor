/**
 * PM2 生态系统配置 — Tailor IS PC Mall SSR 生产部署.
 *
 * <h3>部署架构</h3>
 * <ul>
 *   <li>4 个实例以 cluster 模式运行</li>
 *   <li>每个实例内存限制 512MB</li>
 *   <li>日志轮转（保留 30 天）</li>
 *   <li>崩溃自动重启</li>
 *   <li>零停机重载</li>
 * </ul>
 *
 * <h3>使用方式</h3>
 * <pre>
 * npm run start:prod          # 启动
 * pm2 reload ecosystem.config.js  # 零停机重载
 * pm2 stop tailor-is-ssr      # 停止
 * pm2 logs tailor-is-ssr      # 查看日志
 * </pre>
 */

module.exports = {
  apps: [
    {
      name: 'tailor-is-ssr',
      script: 'server.prod.ts',
      interpreter: 'node',
      interpreter_args: '--import tsx',
      instances: 4,
      exec_mode: 'cluster',
      max_memory_restart: '512M',
      env: {
        NODE_ENV: 'production',
        PORT: 3000
      },
      // 日志配置
      log_date_format: 'YYYY-MM-DD HH:mm:ss Z',
      error_file: './logs/ssr-error.log',
      out_file: './logs/ssr-out.log',
      merge_logs: true,
      // 日志轮转
      log_type: 'json',
      max_size: '10M',
      retain: 30,
      // 自动重启
      autorestart: true,
      max_restarts: 10,
      restart_delay: 4000,
      // 优雅关闭
      kill_timeout: 30000,
      listen_timeout: 10000,
      // 监控
      watch: false,
      // Node.js 参数
      node_args: [
        '--max-old-space-size=512',
        '--experimental-specifier-resolution=node'
      ]
    }
  ]
}