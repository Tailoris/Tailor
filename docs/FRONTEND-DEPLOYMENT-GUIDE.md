# Tailor IS 前端部署与联通验证指南

> 对应: Phase 1 / P1-6 / H-04  
> 生成时间: 2026-06-13  
> 责任人: 前端负责人 + DevOps

## 一、目标

将 4 个前端项目构建为静态资源, 通过 Nginx 提供服务, 验证与后端 Core/Lite Gateway 的联通性。

## 二、架构

```
用户浏览器
   ↓ HTTPS
OpenResty (1Panel) :443
   ↓
Nginx (本任务部署) :80
   ├── /                    → PC 商城 (pc-mall/dist)
   ├── /merchant/           → 商户后台 (merchant-admin/dist)
   ├── /admin/              → 平台管理 (platform-admin/dist)
   ├── /api/core/**         → Core Gateway :9001
   ├── /api/lite/**         → Lite Gateway :9002
   ├── /api/**              → Core Gateway (兜底)
   └── /graphql             → Core Gateway
```

## 三、构建步骤

### 1. 环境要求
- Node.js 20.x
- npm 10.x
- 网络可访问 npm registry

### 2. 单项目构建
```bash
cd /home/tailor/Tailoris/tailor-is-frontend
./deploy/scripts/build-frontend.sh pc-mall
./deploy/scripts/build-frontend.sh merchant-admin
./deploy/scripts/build-frontend.sh platform-admin
```

### 3. 批量构建
```bash
cd /home/tailor/Tailoris
./deploy/scripts/build-frontend.sh all
```

构建产物位置:
- `tailor-is-frontend/pc-mall/dist/`
- `tailor-is-frontend/merchant-admin/dist/`
- `tailor-is-frontend/platform-admin/dist/`

## 四、部署

### 方式 A: Docker Compose (推荐)
```bash
cd /home/tailor/Tailoris/deploy
docker compose -f docker-compose.frontend.yml up -d
docker compose -f docker-compose.frontend.yml ps
docker compose -f docker-compose.frontend.yml logs -f nginx-frontend
```

### 方式 B: 1Panel OpenResty 集成
将构建产物同步到 OpenResty 静态目录:
```bash
cp -r ../tailor-is-frontend/pc-mall/dist/* /opt/1panel/apps/openresty/www/sites/tailoris/index/
cp -r ../tailor-is-frontend/merchant-admin/dist/* /opt/1panel/apps/openresty/www/sites/tailoris/merchant/
cp -r ../tailor-is-frontend/platform-admin/dist/* /opt/1panel/apps/openresty/www/sites/tailoris/admin/
```
然后应用 `deploy/nginx/frontend.conf` 中的 location 规则到 OpenResty 配置。

## 五、联通验证清单

### 1. 静态资源验证
```bash
# PC 商城首页
curl -I http://localhost:8080/
# 期望: 200 OK, Content-Type: text/html

# 静态资源
curl -I http://localhost:8080/assets/index-xxx.js
# 期望: 200 OK, Cache-Control: public, immutable

# 商户后台
curl -I http://localhost:8080/merchant/
# 期望: 200 OK

# 平台管理
curl -I http://localhost:8080/admin/
# 期望: 200 OK
```

### 2. 后端 API 联通验证
```bash
# Core Gateway 健康检查
curl -s http://localhost:8080/api/core/actuator/health
# 期望: {"status":"UP"}

# 登录接口(穿透到 user-service)
curl -X POST http://localhost:8080/api/core/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test"}'
# 期望: 200/401(账号密码错误,但说明 API 通路正常)

# 商品列表(穿透到 product-service)
curl -s http://localhost:8080/api/core/api/product/list
# 期望: 200/未授权,但响应中能看到 product 服务的 JSON 响应
```

### 3. 浏览器端验证

| 验证项 | 预期结果 | 验证方式 |
|--------|---------|---------|
| PC 商城首页加载 | < 2s, 无白屏 | 浏览器 DevTools Performance |
| Vue 路由切换 | SPA 模式, 无刷新 | 点击菜单, 观察 URL 变化 |
| 登录 API 调用 | 返回 Token + 用户信息 | Network 面板 |
| 跨域 (CORS) | 无 CORS 错误 | Console 面板 |
| 静态资源缓存 | 命中 304/200 from cache | Network 面板 |
| Gzip 压缩生效 | Content-Encoding: gzip | Network 面板 |
| HTTPS 证书 | 浏览器显示锁图标 | 地址栏 |

### 4. 移动端验证
- iOS Safari / Android Chrome 访问 `/` 应正常显示
- H5 页面字体/布局适配 (响应式)

## 六、性能基准

| 指标 | 目标 | 当前 |
|------|------|------|
| 首屏 LCP | < 2.5s | 待测 |
| 静态资源大小 | < 1MB (gzipped) | 待测 |
| 路由切换 | < 500ms | 待测 |
| API P95 | < 200ms | 待测 |

## 七、CI 集成

已在 `.github/workflows/frontend-ci.yml` 配置:
1. push 到 main → 自动构建 + 上传 artifacts
2. 每日 9:00 → Dependabot 扫描依赖
3. PR → 编译验证 (允许失败, Phase 1)

## 八、问题排查

### Q1: 静态资源 404
- 检查 `dist/` 目录是否已挂载到 Nginx
- 检查 `location /` 根路径配置

### Q2: API 502 Bad Gateway
- 检查 Core Gateway 是否运行: `docker ps | grep core-gateway`
- 检查 `host.docker.internal:9001` 是否可达
- 查看 Nginx 错误日志: `docker logs tailor-is-frontend`

### Q3: CORS 错误
- Nginx 已设置 `proxy_set_header Host $host;` 应已处理
- 如仍报错, 在 `proxy_pass` 后加 CORS 头:
  ```nginx
  add_header Access-Control-Allow-Origin $http_origin;
  ```

### Q4: Vue Router 刷新 404
- 已配置 `try_files $uri $uri/ /index.html;`, 应正常
- 如仍 404, 检查 Nginx 版本 (需 1.18+)

## 九、Phase 2 增强

- 启用 CDN 加速 (M-03)
- 启用 Brotli 压缩
- SSR 完整启用 (pc-mall)
- 移动端离线能力 (M-09)
- Lighthouse 自动化性能检测
