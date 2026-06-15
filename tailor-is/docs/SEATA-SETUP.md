# Seata 分布式事务配置指南

本文档介绍 Tailor IS 项目中 Seata 分布式事务的部署与配置。

## 概述

Seata 用于解决跨微服务的分布式事务问题，主要应用于订单-支付-库存等跨服务场景。

## 架构

项目采用 Seata AT 模式：

```
Order Service → Seata Server (TC)
Payment Service → Seata Server (TC)
Supply Service → Seata Server (TC)
```

## Seata Server 部署

### Docker 方式

```bash
docker run -d \
  --name seata-server \
  -p 8091:8091 \
  -e SEATA_CONFIG_NAME=file:/root/seata-config/registry.conf \
  -v /path/to/seata-config:/root/seata-config \
  seataio/seata-server:1.7.0
```

### 配置要点

1. **注册到 Nacos** — 修改 `registry.conf` 将 Seata Server 注册到 Nacos
2. **存储模式** — 推荐使用 `db` 模式存储事务日志
3. **事务分组** — 各微服务使用统一的事务分组名

## 微服务配置

在需要分布式事务的服务中添加依赖：

```xml
<dependency>
    <groupId>io.seata</groupId>
    <artifactId>seata-spring-boot-starter</artifactId>
</dependency>
```

`application.yml` 配置：

```yaml
seata:
  enabled: true
  application-id: ${spring.application.name}
  tx-service-group: tailor_is_tx_group
  registry:
    type: nacos
    nacos:
      server-addr: ${NACOS_ADDR}
```

## 使用方式

在需要分布式事务的方法上添加 `@GlobalTransactional` 注解：

```java
@GlobalTransactional
public void createOrder(OrderRequest request) {
    // 1. 创建订单
    orderService.create(request);
    // 2. 扣减库存（远程调用）
    supplyService.deductStock(request.getItems());
    // 3. 发起支付（远程调用）
    paymentService.createPayment(request);
}
```

## 常见问题

- **事务超时** — 检查各服务间网络延迟，适当调整 `client.tm.defaultGlobalTransactionTimeout`
- **分支事务注册失败** — 确认 Seata Server 已正确注册到 Nacos
- **undo_log 表** — 每个参与分布式事务的数据库都需要创建 `undo_log` 表
