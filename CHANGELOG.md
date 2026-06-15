# Changelog

All notable changes to Tailor IS（裁智云）will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/lang/zh-CN/).

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
