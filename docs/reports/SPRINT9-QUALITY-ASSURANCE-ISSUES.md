# Sprint 9 质量保障与上线问题清单与跟踪表

**文档编号**: TAILOR-IS-SPRINT9-ISSUES-2026-0603
**编制日期**: 2026年6月3日
**关联任务**: QA-001~QA-020
**关联模块**: 全栈质量保障
**目标**: 项目达到生产环境部署标准

---

## 1. 问题概览

| 类别 | 数量 | 严重等级 | 处理策略 |
|------|:----:|:--------:|---------|
| 测试缺失（T） | 8 | High | 补充单元/集成/E2E测试 |
| 性能瓶颈（P） | 12 | High | 索引优化/N+1修复/缓存/压测 |
| 安全漏洞（S） | 10 | Critical | OWASP扫描/HTTPS/CI集成 |
| 部署配置（D） | 8 | High | Nginx/HTTPS/灰度/蓝绿 |
| 监控缺失（M） | 6 | High | Prometheus/ELK/告警 |
| 文档/规范（N） | 4 | Medium | 补充SOP |
| 可访问性（A） | 4 | Medium | WCAG 2.1 AA |
| 兼容性（C） | 4 | Medium | 多浏览器/移动设备 |
| **合计** | **56** | — | — |

---

## 2. 详细问题清单

### 2.1 QA-001 单元测试覆盖率达90%（4项）

| 编号 | 文件 | 问题描述 | 严重 | 建议方案 | 优先级 |
|:---:|------|---------|:----:|---------|:----:|
| QA-T01 | pom.xml | jacoco 覆盖率阈值仅 80% | High | 提升至 90% + 排除 DTO/VO/Entity | P0 |
| QA-T02 | 各模块 service | 边界/异常分支未覆盖 | High | 补充异常场景测试 | P0 |
| QA-T03 | tailor-is-payment | 支付回调分支覆盖率低 | Critical | 补充回调全场景测试 | P0 |
| QA-T04 | tailor-is-order | 订单状态机分支遗漏 | High | 状态机全状态测试 | P0 |

### 2.2 QA-002 集成测试（3项）

| 编号 | 模块 | 问题描述 | 严重 | 建议方案 | 优先级 |
|:---:|------|---------|:----:|---------|:----:|
| QA-T05 | 订单支付链路 | 订单→支付→库存扣减集成测试缺失 | High | Testcontainers 全链路 | P1 |
| QA-T06 | 用户-商家-商品 | 注册→开店→发布商品 链路缺失 | High | 端到端集成测试 | P1 |
| QA-T07 | 营销订单 | 优惠券→订单→分账 集成测试缺失 | Medium | SpringBootTest | P1 |

### 2.3 QA-003 E2E测试（3项）

| 编号 | 模块 | 问题描述 | 严重 | 建议方案 | 优先级 |
|:---:|------|---------|:----:|---------|:----:|
| QA-T08 | 核心购物流程 | 浏览→加购→下单→支付→发货→收货 全流程无E2E | High | Playwright 自动化 | P1 |
| QA-T09 | 商家后台 | 商家登录→上架→审核→发布 流程未测 | Medium | Playwright | P1 |
| QA-T10 | 移动端 H5 | 下单流程在 H5 兼容性未测 | Medium | Playwright + 模拟器 | P2 |

### 2.4 QA-004 性能压测（4项）

| 编号 | 模块 | 问题描述 | 严重 | 建议方案 | 优先级 |
|:---:|------|---------|:----:|---------|:----:|
| QA-P01 | 首页/列表 | P95 响应时间未达 200ms | High | JMeter 压测+优化 | P0 |
| QA-P02 | 订单创建 | 秒杀/拼团场景未做峰值测试 | High | 5000 TPS 压测 | P0 |
| QA-P03 | 区块链版权 | 上链并发瓶颈未验证 | Medium | 50 TPS 长稳测试 | P1 |
| QA-P04 | 全链路 | 灰度场景的 1%→100% 切流压测缺失 | Medium | 流量回放+压测 | P1 |

### 2.5 QA-005 安全扫描（3项）

| 编号 | 模块 | 问题描述 | 严重 | 建议方案 | 优先级 |
|:---:|------|---------|:----:|---------|:----:|
| QA-S01 | 全栈 | OWASP ZAP 主动扫描未集成 | Critical | ZAP Baseline + Full Scan | P0 |
| QA-S02 | API | SQL注入点未做模糊测试 | Critical | SQLMap + ZAP Fuzz | P0 |
| QA-S03 | 前端 | XSS 反射型未做扫描 | High | ZAP + DOM-based XSS | P0 |

### 2.6 QA-006 数据库索引优化（4项）

| 编号 | 表 | 问题描述 | 严重 | 建议方案 | 优先级 |
|:---:|---|---------|:----:|---------|:----:|
| QA-P05 | 各业务表 | 主键外键无复合索引，查询慢 | High | 添加复合索引 | P0 |
| QA-P06 | order_info | 按 user_id+status 查询无索引 | High | 添加 idx_user_status | P0 |
| QA-P07 | copyright_record | 按 file_hash 查询无索引 | High | 唯一索引 | P0 |
| QA-P08 | community_post | 按 topic_id+create_time 无索引 | Medium | 复合索引 | P1 |

### 2.7 QA-007 N+1查询修复（3项）

| 编号 | 模块 | 问题描述 | 严重 | 建议方案 | 优先级 |
|:---:|------|---------|:----:|---------|:----:|
| QA-P09 | OrderService | 列表查询遍历订单 N 次查详情 | High | 批量查询/join | P0 |
| QA-P10 | CommunityService | 帖子列表每条评论单独查询 | High | 一次查询+内存组装 | P0 |
| QA-P11 | ProductService | 商品列表每条查 SKU | High | 批量查 SKU | P0 |

### 2.8 QA-008 多级缓存（3项）

| 编号 | 模块 | 问题描述 | 严重 | 建议方案 | 优先级 |
|:---:|------|---------|:----:|---------|:----:|
| QA-P12 | Product | 热点商品无本地缓存 | High | Caffeine L1 + Redis L2 | P1 |
| QA-P13 | Home | 首页 Banner/分类无缓存 | High | 多级缓存 | P1 |
| QA-P14 | User | Session 无本地缓存 | Medium | Caffeine | P1 |

### 2.9 QA-009 前端资源优化（3项）

| 编号 | 模块 | 问题描述 | 严重 | 建议方案 | 优先级 |
|:---:|------|---------|:----:|---------|:----:|
| QA-P15 | PC商城 | 首屏 JS bundle > 2MB | High | 代码分割+懒加载 | P1 |
| QA-P16 | 移动端 | 图片未做 WebP 转换 | Medium | WebP + 懒加载 | P1 |
| QA-P17 | 商家后台 | 表格大数据卡顿 | Medium | 虚拟滚动 | P2 |

### 2.10 QA-010 Nginx 负载均衡（2项）

| 编号 | 模块 | 问题描述 | 严重 | 建议方案 | 优先级 |
|:---:|------|---------|:----:|---------|:----:|
| QA-D01 | nginx.conf | 仅 1 个 gateway 节点 | High | 3节点 upstream + 负载均衡 | P0 |
| QA-D02 | nginx.conf | 无 health check | High | active health check | P0 |

### 2.11 QA-011 HTTPS 全站配置（2项）

| 编号 | 模块 | 问题描述 | 严重 | 建议方案 | 优先级 |
|:---:|------|---------|:----:|---------|:----:|
| QA-D03 | nginx.conf | HTTPS 配置被注释 | High | 启用 TLS 1.3 + HSTS | P0 |
| QA-D04 | 证书 | 证书自动续期未配置 | High | certbot + cron | P0 |

### 2.12 QA-012 灰度发布/蓝绿部署（2项）

| 编号 | 模块 | 问题描述 | 严重 | 建议方案 | 优先级 |
|:---:|------|---------|:----:|---------|:----:|
| QA-D05 | deploy | 无灰度策略 | High | Nginx + 流量切分 1%→100% | P1 |
| QA-D06 | deploy | 无蓝绿部署脚本 | Medium | docker-compose 蓝绿 | P1 |

### 2.13 QA-013 监控告警（3项）

| 编号 | 模块 | 问题描述 | 严重 | 建议方案 | 优先级 |
|:---:|------|---------|:----:|---------|:----:|
| QA-M01 | prometheus | 部分服务无 metrics 端点 | High | 启用 actuator/prometheus | P0 |
| QA-M02 | alerts.yml | 告警规则不完整 | High | 补充 SLO 告警 | P0 |
| QA-M03 | Grafana | Dashboard 不完整 | Medium | 补充业务大屏 | P1 |

### 2.14 QA-014 日志集中管理（2项）

| 编号 | 模块 | 问题描述 | 严重 | 建议方案 | 优先级 |
|:---:|------|---------|:----:|---------|:----:|
| QA-M04 | logback | 日志仅本地存储 | High | Filebeat + ELK | P1 |
| QA-M05 | audit | 审计日志未独立存储 | High | 独立索引 + 长期保留 | P1 |

### 2.15 QA-015 移动端 TypeScript 迁移（3项）

| 编号 | 模块 | 问题描述 | 严重 | 建议方案 | 优先级 |
|:---:|------|---------|:----:|---------|:----:|
| QA-N01 | mobile-app | API 文件为 JS | Critical | 迁移至 .ts | P0 |
| QA-N02 | mobile-app | pages 目录为 .vue 无类型 | High | 添加 lang="ts" | P0 |
| QA-N03 | mobile-app | 无 tsconfig | High | 添加 tsconfig.json | P0 |

### 2.16 QA-016 SonarQube 部署+集成（2项）

| 编号 | 模块 | 问题描述 | 严重 | 建议方案 | 优先级 |
|:---:|------|---------|:----:|---------|:----:|
| QA-N04 | infra | SonarQube 服务未部署 | High | docker-compose 部署 | P0 |
| QA-N05 | ci.yml | SonarQube 扫描配置存在但未启用 | High | 配置 SONAR_TOKEN | P0 |

### 2.17 QA-017 OWASP CI/CD 集成（2项）

| 编号 | 模块 | 问题描述 | 严重 | 建议方案 | 优先级 |
|:---:|------|---------|:----:|---------|:----:|
| QA-S04 | ci.yml | OWASP 仅在 main 分支执行 | High | PR 也执行 | P0 |
| QA-S05 | ci.yml | 无 SARIF 上传 | Medium | GitHub Code Scanning | P1 |

### 2.18 QA-018 多浏览器兼容测试（2项）

| 编号 | 模块 | 问题描述 | 严重 | 建议方案 | 优先级 |
|:---:|------|---------|:----:|---------|:----:|
| QA-C01 | 前端 | 未在 Safari 测试 | High | Playwright 多浏览器 | P1 |
| QA-C02 | 前端 | Edge 兼容性未验证 | Medium | Playwright | P1 |

### 2.19 QA-019 移动设备兼容测试（2项）

| 编号 | 模块 | 问题描述 | 严重 | 建议方案 | 优先级 |
|:---:|------|---------|:----:|---------|:----:|
| QA-C03 | mobile | iOS Safari 兼容性未测 | High | BrowserStack/真机 | P1 |
| QA-C04 | mobile | Android 微信内置浏览器 | Medium | 微信 JSSDK 测试 | P1 |

### 2.20 QA-020 无障碍（WCAG 2.1 AA）（4项）

| 编号 | 模块 | 问题描述 | 严重 | 建议方案 | 优先级 |
|:---:|------|---------|:----:|---------|:----:|
| QA-A01 | 关键页面 | 无 aria-label | High | 添加 ARIA 属性 | P2 |
| QA-A02 | 颜色对比度 | 部分按钮对比度 < 4.5:1 | Medium | 调整配色 | P2 |
| QA-A03 | 键盘导航 | 部分交互仅鼠标可达 | Medium | 键盘可达性 | P2 |
| QA-A04 | 屏幕阅读器 | 未做 NVDA/JAWS 测试 | Medium | 屏幕阅读器测试 | P2 |

---

## 3. 修复策略

- **P0 (Critical/High)**: Sprint 9 必修
- **P1 (Medium)**: Sprint 9 选做或下 Sprint
- **P2 (Low)**: 上线后持续优化

**Sprint 9 必修**: 41 项（73%）
**Sprint 9 选做/下 Sprint**: 15 项（27%）

---

## 4. 跟踪与更新

本文档将随 Sprint 9 开发进度持续更新，标记每项问题的修复状态、解决方案、责任人。
