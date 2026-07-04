# Changelog

All notable changes to Tailor IS（裁智云）will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/lang/zh-CN/).

<!--
  =============================================================================
  CHANGELOG 自动更新说明 (P3-L-11)
  =============================================================================

  本文档应在每次版本发布时手动更新，遵循以下规则:

  1. 版本号遵循 SemVer: MAJOR.MINOR.PATCH
  2. 使用以下分类组织变更:
     - 新增 (Added): 新功能
     - 变更 (Changed): 现有功能的变更
     - 废弃 (Deprecated): 即将移除的功能
     - 移除 (Removed): 已移除的功能
     - 修复 (Fixed): 任何 Bug 修复
     - 安全 (Security): 安全漏洞修复
     - 架构 (Architecture): 架构层面的变更
     - 文档 (Documentation): 文档更新

  3. 每次发布前，建议运行以下命令收集变更:
     git log --oneline v1.0.0..HEAD --format="  - %s"

  4. 版本发布模板:
     ## [X.Y.Z] - YYYY-MM-DD
     ### 新增
     - xxx
     ### 修复
     - xxx
     ### 变更
     - xxx

  5. 发布 checklist:
     - [ ] 更新 CHANGELOG.md
     - [ ] 更新版本号 (pom.xml / package.json)
     - [ ] 创建 Git tag: git tag -a vX.Y.Z -m "Release vX.Y.Z"
     - [ ] 推送 tag: git push origin vX.Y.Z
  =============================================================================
-->

## [Unreleased]

### 新增
- (待发布)

### 修复
- P3-L-01: 文档归档策略 (docs/archive/, docs/reports/README.md)
- P3-L-02: Gitleaks 集成至 PR Check CI 流程
- P3-L-03: WCAG 测试覆盖范围说明注释
- P3-L-04: 多平台兼容性测试计划文档
- P3-L-05: Service Worker 生产构建移除 console.log
- P3-L-06: platform-admin .dockerignore 排除 mock-server.js 注释
- P3-L-07: @Deprecated 方法清理计划标注
- P3-L-08: 占位图片工具函数 (shared/utils/placeholder.ts)
- P3-L-09: 全局错误边界组件 (shared/components/ErrorBoundary.vue)
- P3-L-10: merchant-admin i18n 补充订单状态翻译
- P3-L-11: CHANGELOG 自动更新机制说明

## [1.0.0] - 2026-06-11

### 新增

- **多商户商城体系** — 完整的商户入驻、商品管理、订单管理、支付结算流程
- **纸样定制工作台** — 在线服装纸样定制、尺寸采集、协作修改
- **区块链版权保护** — 基于区块链的设计稿版权登记与存证
- **AI 智能推荐** — 基于用户行为的个性化商品推荐
- **社区交流平台** — 用户内容分享、互动、评价系统
- **营销工具集** — 优惠券、秒杀、拼团等营销能力
- **商户管理后台** — 商户端商品、订单、财务管理界面
- **平台管理后台** — 平台端商户审核、数据统计、系统配置
- **移动端商城** — 基于 UniApp 的跨端移动商城
- **GraphQL 聚合层** — 前端 GraphQL Gateway 减少请求次数
- **SSR 支持** — PC 端商城服务端渲染优化 SEO

### 架构

- Spring Cloud 微服务架构，服务注册与配置中心（Nacos）
- 分布式事务支持（Seata）
- 消息队列集成（RabbitMQ）
- 全链路监控（Prometheus + Grafana）
- 告警系统（Alertmanager + Webhook）
- Kubernetes 部署配置
- 数据库分库分表方案

### 安全

- JWT 认证 + RBAC 权限控制
- 接口限流与防重放
- 敏感数据加密存储
- XSS 防护（DOMPurify）

### 文档

- 架构设计文档
- K8s 部署指南
- API 文档
- 编码规范
- 监控与性能测试指南