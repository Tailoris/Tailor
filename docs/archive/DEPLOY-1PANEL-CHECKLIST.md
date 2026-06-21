# Tailor IS 1Panel 部署检查清单 (Deployment Checklist)

**文档编号**: TAILOR-IS-CHECKLIST-2026-0603
**版本**: V1.0
**日期**: 2026年6月3日
**关联文档**: [DEPLOY-1PANEL-PLAN.md](file:///F:/Tailor/Tailor%20is/DEPLOY-1PANEL-PLAN.md)

---

## 使用说明

- [ ] = 待检查项
- ✅ = 通过
- ❌ = 失败
- ⚠️ = 警告（需说明）

**部署负责人**: _______________ **日期**: _______________

---

## 阶段 0: 部署前准备（1h）

### 0.1 部署评审

- [ ] 部署计划方案评审通过
- [ ] 风险评估报告评审通过
- [ ] 部署脚本测试通过（staging）
- [ ] 数据库备份脚本测试通过
- [ ] 回滚脚本测试通过

### 0.2 资源准备

- [ ] 服务器已申请（8 核 16G 最低）
- [ ] 域名已备案
- [ ] SSL 证书已准备
- [ ] 域名 DNS 已解析
- [ ] 防火墙规则已开放
- [ ] 部署包已构建并上传

### 0.3 人员准备

- [ ] 总指挥已通知
- [ ] DevOps 团队已就绪
- [ ] DBA 已就绪
- [ ] 后端团队已就绪
- [ ] 前端团队已就绪
- [ ] QA 团队已就绪
- [ ] SRE 团队已就绪
- [ ] 运营/客服已通知

### 0.4 沟通准备

- [ ] 部署窗口已确认
- [ ] 用户公告已发布
- [ ] 内部沟通群已建立
- [ ] 升级路径已确认
- [ ] 应急联系人已确认

---

## 阶段 1: 1Panel 服务确认（0.5h）

### 1.1 1Panel 面板

- [ ] 1Panel 已启动（`sudo 1pctl status`）
- [ ] 面板可访问：http://172.28.249.179:42405/5b4c869c53
- [ ] 面板用户登录成功（c0f9ba4b02 / 004db65669）

### 1.2 基础服务确认

- [ ] MySQL 端口 3306 监听中
- [ ] MySQL 连接成功（密码 mysql_CA75Yk）
- [ ] Redis 端口 6379 监听中
- [ ] Redis 连接成功（密码 redis_RSeR4G）
- [ ] RabbitMQ 端口 5672 监听中
- [ ] RabbitMQ Dashboard 端口 15672 可访问
- [ ] Nacos 端口 8080 监听中
- [ ] Nacos 端口 8848 / 9848 监听中
- [ ] Nacos 登录成功

### 1.3 环境检查

- [ ] JDK 17 已安装
- [ ] JAVA_HOME 已设置
- [ ] 主机 CPU ≥ 4 核
- [ ] 主机内存 ≥ 8GB
- [ ] 磁盘可用空间 ≥ 50GB
- [ ] 部署目录权限正确
- [ ] 端口冲突检查通过

**预检查结果**: `bash 1panel-pre-deploy-check.sh` 通过

---

## 阶段 2: 数据库初始化与迁移（1h）

### 2.1 数据库准备

- [ ] 数据库 tailor_is 已创建
- [ ] 字符集 utf8mb4
- [ ] 数据库可访问

### 2.2 SQL 脚本执行（顺序执行）

- [ ] 01_user_system.sql
- [ ] 02_merchant_system.sql
- [ ] 03_product_system.sql
- [ ] 04_order_system.sql
- [ ] 05_payment_system.sql
- [ ] 06_marketing_system.sql
- [ ] 07_copyright_system.sql
- [ ] 08_community_system.sql
- [ ] 09_supply_system.sql
- [ ] 10_message_system.sql
- [ ] V8__Sprint8_OSS_Search_CustomReview.sql
- [ ] V8_1__Sprint8_Merchant_Dashboard_Trial_Violation.sql
- [ ] V8_2__Sprint8_Marketing_Community.sql
- [ ] V8_3__Sprint8_Blockchain_Copyright.sql
- [ ] V9_1__Sprint9_QA_Index_Optimization.sql

### 2.3 数据验证

- [ ] 表数量 ≥ 50
- [ ] 关键表存在（sys_user/merchant/product/order_info/payment_record/copyright_record/community_post）
- [ ] 索引数量 ≥ 200
- [ ] 字符集正确（utf8mb4）
- [ ] 数据验证脚本通过

### 2.4 备份

- [ ] 初始数据已备份
- [ ] 备份文件存储位置：/opt/tailor-is/backup/db/
- [ ] 备份文件大小记录
- [ ] 备份可恢复验证

---

## 阶段 3: 后端服务部署（1.5h）

### 3.1 JAR 包上传

- [ ] tailor-is-gateway.jar
- [ ] tailor-is-user.jar
- [ ] tailor-is-merchant.jar
- [ ] tailor-is-product.jar
- [ ] tailor-is-order.jar
- [ ] tailor-is-payment.jar
- [ ] tailor-is-copyright.jar
- [ ] tailor-is-marketing.jar
- [ ] tailor-is-community.jar
- [ ] tailor-is-message.jar

### 3.2 Nacos 配置

- [ ] 命名空间已创建（tailor-is-prod）
- [ ] application-common.yml 已上传
- [ ] application-gateway.yml 已上传
- [ ] application-user.yml 已上传
- [ ] application-merchant.yml 已上传
- [ ] application-product.yml 已上传
- [ ] application-order.yml 已上传
- [ ] application-payment.yml 已上传
- [ ] application-copyright.yml 已上传
- [ ] application-marketing.yml 已上传
- [ ] application-community.yml 已上传
- [ ] application-message.yml 已上传

### 3.3 服务启动（按顺序）

- [ ] 业务基础服务
  - [ ] tailor-is-user (8082)
  - [ ] tailor-is-merchant (8083)
  - [ ] tailor-is-product (8084)
- [ ] 业务核心服务
  - [ ] tailor-is-order (8085)
  - [ ] tailor-is-payment (8086)
  - [ ] tailor-is-message (8090)
- [ ] 业务扩展服务
  - [ ] tailor-is-copyright (8087)
  - [ ] tailor-is-marketing (8088)
  - [ ] tailor-is-community (8089)
- [ ] 网关服务
  - [ ] tailor-is-gateway (8081)

### 3.4 启动验证

- [ ] 所有服务进程已启动
- [ ] 所有服务健康检查通过
- [ ] 服务日志无 ERROR
- [ ] 端口监听正常

---

## 阶段 4: 前端应用部署（1h）

### 4.1 前端文件上传

- [ ] PC 商城构建产物
- [ ] 移动端 H5 构建产物
- [ ] 商家后台构建产物
- [ ] 平台后台构建产物

### 4.2 Nginx 配置

- [ ] Nginx 配置文件已上传
- [ ] 静态资源目录权限正确
- [ ] 反向代理配置正确
- [ ] HTTPS 证书已部署
- [ ] Nginx 配置测试通过（`nginx -t`）
- [ ] Nginx 已重新加载

### 4.3 前端验证

- [ ] PC 商城首页可访问
- [ ] 移动端 H5 可访问
- [ ] 商家后台可访问
- [ ] 平台后台可访问
- [ ] 静态资源加载正常
- [ ] 跨域问题已解决

---

## 阶段 5: 监控与日志（可选，1h）

### 5.1 监控部署

- [ ] Prometheus 已部署
- [ ] Grafana 已部署
- [ ] AlertManager 已部署
- [ ] 应用 Prometheus 端点暴露
- [ ] Grafana Dashboard 已配置
- [ ] 告警规则已配置
- [ ] 告警渠道已配置

### 5.2 日志系统

- [ ] ELK 已部署
- [ ] Filebeat 已部署
- [ ] 日志收集正常
- [ ] Kibana 可视化正常

---

## 阶段 6: 部署验证（1h）

### 6.1 L1 - 基础设施

- [ ] MySQL 连接通过
- [ ] Redis 连接通过
- [ ] RabbitMQ 服务正常
- [ ] Nacos 服务正常

### 6.2 L2 - 服务健康

- [ ] gateway 健康检查
- [ ] user 健康检查
- [ ] merchant 健康检查
- [ ] product 健康检查
- [ ] order 健康检查
- [ ] payment 健康检查
- [ ] copyright 健康检查
- [ ] marketing 健康检查
- [ ] community 健康检查
- [ ] message 健康检查

### 6.3 L3 - 接口功能

- [ ] 用户登录接口
- [ ] 用户注册接口
- [ ] 商品列表接口
- [ ] 商品详情接口
- [ ] 购物车接口
- [ ] 订单创建接口
- [ ] 支付接口
- [ ] 版权验证接口
- [ ] 社区帖子接口
- [ ] 站内信接口

### 6.4 L4 - 业务流程

- [ ] 注册→登录流程
- [ ] 浏览→加购→下单→支付
- [ ] 版权登记流程
- [ ] 商家入驻流程
- [ ] 社区发帖流程

### 6.5 L5 - 性能

- [ ] 首页 P95 ≤ 200ms
- [ ] 商品列表 P95 ≤ 300ms
- [ ] 下单 P95 ≤ 800ms
- [ ] 支付回调 P95 ≤ 1000ms
- [ ] 版权登记 P95 ≤ 800ms
- [ ] 并发 ≥ 2000 用户

### 6.6 L6 - 安全

- [ ] HTTPS 全站
- [ ] 安全响应头已配置
- [ ] SQL 注入测试通过
- [ ] XSS 测试通过
- [ ] OWASP ZAP 0 High 漏洞
- [ ] SonarQube A 级

**验证结果**: `bash 1panel-verify.sh 6` 通过

---

## 阶段 7: 灰度发布（2h）

### 7.1 1% 灰度（30min 观察）

- [ ] 灰度配置已生效
- [ ] 1% 流量已切到新版本
- [ ] 错误率 < 0.1%
- [ ] P99 响应时间 < 3s
- [ ] 无 P0/P1 告警

### 7.2 10% 灰度（30min 观察）

- [ ] 灰度配置已生效
- [ ] 10% 流量已切到新版本
- [ ] 错误率 < 0.1%
- [ ] P99 响应时间 < 3s
- [ ] 无 P0/P1 告警

### 7.3 50% 灰度（60min 观察）

- [ ] 灰度配置已生效
- [ ] 50% 流量已切到新版本
- [ ] 错误率 < 0.1%
- [ ] P99 响应时间 < 3s
- [ ] 无 P0/P1 告警

### 7.4 100% 全量（30min 观察）

- [ ] 100% 流量切到新版本
- [ ] 错误率 < 0.1%
- [ ] P99 响应时间 < 3s
- [ ] 无 P0/P1 告警
- [ ] 业务核心指标正常

---

## 阶段 8: 收尾工作（0.5h）

### 8.1 清理

- [ ] 灰度版本容器/进程已清理
- [ ] 临时文件已清理
- [ ] 旧版本 JAR 已归档
- [ ] 旧备份已清理

### 8.2 文档

- [ ] 部署完成报告已编写
- [ ] 部署验证报告已生成
- [ ] 监控配置文档已更新
- [ ] 应急响应手册已更新

### 8.3 通知

- [ ] 内部相关方已通知
- [ ] 用户公告已发布
- [ ] 客服团队已培训
- [ ] 运维值班已交接

### 8.4 监控

- [ ] 7×24 监控已启动
- [ ] 告警值班已安排
- [ ] 性能基线已记录
- [ ] Post-Mortem 计划已安排

---

## 部署后 7 天跟踪

### 第 1 天

- [ ] 系统稳定性监控
- [ ] 用户反馈收集
- [ ] 紧急问题处理
- [ ] Post-Mortem 报告

### 第 3 天

- [ ] 性能指标评估
- [ ] 错误率统计
- [ ] 用户满意度调研
- [ ] 优化项识别

### 第 7 天

- [ ] 7 天稳定性报告
- [ ] 业务指标对比
- [ ] 经验教训总结
- [ ] 下一迭代计划

---

## 部署回滚触发条件

如出现以下任一情况，立即触发回滚：

| 触发条件 | 阈值 | 响应时间 |
|---------|------|---------|
| 错误率 | > 5% | 5min |
| P99 响应时间 | > 10s | 5min |
| 关键服务不可用 | 持续 5min | 5min |
| 数据库连接失败 | 持续 5min | 5min |
| 用户投诉 | > 50/h | 15min |
| 资金相关 Bug | 任意 | 即时 |
| 数据丢失 | 任意 | 即时 |

**回滚命令**: `bash 1panel-rollback.sh all`

---

## 签字确认

### 部署执行

| 角色 | 姓名 | 签字 | 日期 |
|------|------|------|------|
| 总指挥 | | | |
| DevOps | | | |
| DBA | | | |
| 后端 | | | |
| 前端 | | | |
| QA | | | |
| SRE | | | |

### 部署完成

- [ ] 所有阶段检查项完成
- [ ] 验证脚本全部通过
- [ ] 监控告警配置正确
- [ ] 文档归档完成
- [ ] 团队已通知

**部署完成时间**: _______________
**部署结果**: ✅ 成功 / ❌ 失败 / ⚠️ 部分成功
**备注**: _______________
