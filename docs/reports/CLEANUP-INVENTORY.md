# Tailor IS 项目文件清理清单

## 一、文件分类标准

### 1.1 保留范围（不删除）
| 分类 | 描述 | 示例 |
|-----|-----|-----|
| 源代码 | 所有 .java, .ts, .vue, .js（非node_modules内）, .py | `tailor-is-order/src/main/java/**`, `tailor-is-frontend/pc-mall/src/**` |
| 配置文件 | pom.xml, package.json, vite.config.ts, pages.json, manifest.json, application.yml, bootstrap.yml, .env.* | `tailor-is/pom.xml`, `tailor-is-frontend/pc-mall/package.json` |
| 核心文档 | tailor-is/docs/**, CHANGELOG.md, README.md, CONTRIBUTING.md, LICENSE, 架构设计文档 | `tailor-is/docs/ARCHITECTURE.md` |
| 部署脚本 | docker-compose.yml, Dockerfile, deploy/nginx/**, deploy/database/**, deploy/k8s/** | `docker-compose.yml`, `Dockerfile` |
| 数据库脚本 | tailor-is/sql/** | `tailor-is/sql/01_user_system.sql` |
| 依赖管理 | node_modules/**, .m2/** | 由包管理器管理 |
| IDE配置 | .vscode/**, .obsidian/**, .claudian/** | 编辑器配置 |
| 项目跟踪 | .trae/** | 项目跟踪数据 |
| 测试文件 | e2e-tests/** | `tailor-is-frontend/e2e-tests/**` |

### 1.2 清理范围（可安全删除）
| 分类 | 描述 | 恢复方式 |
|-----|-----|---------|
| 已失效构建产物 | 旧版本JAR包 (deploy/*.jar) | `mvn package` 重新构建 |
| 构建日志 | build-gateway-prom*.log | 重新构建自动生成 |
| 重复源码目录 | mobile-app/src/（已迁移到根目录结构） | git 历史 |
| 临时性能分析文件 | profile-0.cpuprofile | 重新性能测试生成 |
| 临时截图 | *.png (根目录) | 重新截图 |
| 临时文本文件 | 编译日志.txt, 服务器终端最新执行结果.txt 等 | 重新执行命令生成 |
| 冗余图标文件 | Tailor IS-01.ico | 项目已有favicon |
| manifest清单 | *.MANIFEST.txt | 构建时自动生成 |
| dist编译目录 | 前端dist目录 | `npm run build` 重新构建 |
| target编译目录 | Java target目录 | `mvn package` 重新构建 |

## 二、预清理清单

### 2.1 已失效部署产物（deploy/目录）
```
deploy/build-gateway-prom.log                  9.5KB   构建日志，可重建
deploy/build-gateway-prom2.log                  9.6KB   构建日志，可重建
deploy/tailor-is-gateway-1.0.0-20260604-132935.jar     56.9MB  旧版本JAR包
deploy/tailor-is-gateway-1.0.0-fix-credentials-20260604-160227.jar  56.9MB  旧版本JAR包
deploy/tailor-is-gateway-1.0.0-prometheus-20260604-213850.jar  59.4MB  旧版本JAR包
deploy/tailor-is-gateway-1.0.0-prometheus-20260604-214310.jar  59.4MB  旧版本JAR包
deploy/tailor-is-gateway-1.0.0-20260604-132935.MANIFEST.txt  1.6KB  构建清单，可重建
小计: 7个文件，约232MB
```

### 2.2 重复源码结构（mobile-app/src/目录）
```
tailor-is-frontend/mobile-app/src/              1.3MB   旧版JS结构，已迁移到根目录TS结构
  ├─ src/pages/          (与根目录 pages/ 内容相同 + 1个额外的 forgot-password 页面)
  ├─ src/api/*.js        (已被根目录 api/*.ts 取代，从JavaScript迁移到TypeScript)
  ├─ src/App.vue         (与根目录 App.vue 内容相同)
  ├─ src/main.js         (入口文件，已被根目录 main.ts 取代)
  ├─ src/manifest.json   (配置文件，已被根目录 manifest.json 取代)
  ├─ src/pages.json      (配置文件，已被根目录 pages.json 取代)
  ├─ src/entry-server.js (SSR入口，已迁移)
  ├─ src/uni.scss        (样式文件，已迁移)
  └─ src/static/sw.js    (Service Worker，已迁移)
小计: 约30个文件，约1.3MB
```

### 2.3 根目录临时文件
```
手机号注册 - Tailor IS.png                   截图，可重新获取
邮箱注册 - Tailor IS.png                     截图，可重新获取
登录 - Tailor IS.png                         截图，可重新获取
Tailor IS-01.ico                            冗余图标文件
编译日志.txt                                临时编译输出
服务器终端最新执行结果.txt                    临时执行输出
项目部署 1Panel账户信息.txt                   临时配置说明
项目开发及部署情况核查 .txt                   临时核查输出
小计: 8个文件，约1-5MB
```

### 2.4 性能分析临时文件
```
tailor-is-frontend/mobile-app/src/profile-0.cpuprofile   性能分析输出，可重新生成
小计: 1个文件
```

### 2.5 前端编译产物目录（构建可恢复）
```
tailor-is-frontend/pc-mall/dist/             1.8MB   Vue构建产物，npm run build 可恢复
tailor-is-frontend/mobile-app/dist/          16KB    uni-app构建产物，npm run build 可恢复
tailor-is-frontend/merchant-admin/dist/      1.8MB   Vue构建产物，npm run build 可恢复
tailor-is-frontend/platform-admin/dist/      1.6MB   Vue构建产物，npm run build 可恢复
小计: 4个目录，约5.2MB
```

### 2.6 Java编译产物目录（构建可恢复）
```
tailor-is/tailor-is-*/target/                各微服务Java编译产物，mvn package 可恢复
  已在 .gitignore 中标记忽略
```

## 三、清理汇总

| 类别 | 文件/目录数 | 预估体积 | 恢复方式 |
|-----|-----------|---------|---------|
| 旧版本部署JAR | 4 | ~232MB | `mvn package` + 重新部署 |
| 构建日志/清单 | 3 | ~20KB | 重新构建 |
| 重复源码结构 | 30+ | ~1.3MB | git 历史 |
| 临时文本/截图 | 8 | ~5MB | 重新执行 |
| 前端dist目录 | 4 | ~5.2MB | `npm run build` |
| 总计 | ~47+ | ~243.5MB | |

## 四、团队确认状态
- [ ] 确认 deploy/ 中JAR包为旧版本可删除
- [ ] 确认 mobile-app/src/ 为旧版结构可删除
- [ ] 确认 根目录临时文件可删除
- [ ] 确认 dist/ 目录可清理（构建可恢复）
