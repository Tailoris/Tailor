# Tailor IS v4.0 Admin服务中心化迁移指南

## 一、迁移背景

### 1.1 问题现状
- Admin Controller散落在各业务服务中（order、product、community、merchant）
- 同一管理功能存在多份实现，维护成本高
- 路由 `/api/v1/admin/**` 同时指向多个服务，可能导致数据不一致

### 1.2 迁移目标
- 所有Admin管理功能统一由 `tailor-is-admin` 服务提供
- 业务服务仅保留业务逻辑，删除Admin Controller
- 通过Feign Client实现服务间解耦

---

## 二、已完成迁移的模块

| 模块 | 原位置 | 目标位置 | 迁移状态 |
|------|-------|---------|---------|
| AdminOrderController | tailor-is-order | tailor-is-admin | ✅ 已完成 |
| AdminProductController | tailor-is-product | tailor-is-admin | ✅ 已完成 |
| AdminCommunityController | tailor-is-community | tailor-is-admin | ✅ 已完成 |
| AdminMerchantController | tailor-is-merchant | tailor-is-admin | ✅ 已完成 |

---

## 三、迁移步骤

### 3.1 创建Feign Client接口

在 `tailor-is-admin` 模块中创建Feign Client：

```java
@FeignClient(name = "tailor-is-order")
public interface OrderFeignClient {
    
    @GetMapping("/api/v1/order/{id}")
    ApiResponse<OrderDTO> getOrderById(@PathVariable Long id);
    
    @GetMapping("/api/v1/order")
    ApiResponse<PageResponse<OrderDTO>> getOrderList(@RequestParam Map<String, Object> params);
    
    @PutMapping("/api/v1/order/{id}/status")
    ApiResponse<Void> updateOrderStatus(@PathVariable Long id, @RequestBody StatusUpdateRequest request);
}
```

### 3.2 实现Admin Controller

在 `tailor-is-admin` 中实现统一的管理Controller：

```java
@RestController
@RequestMapping(RoutePortConstant.ROUTE_ADMIN + "/order")
public class AdminOrderController {
    
    private final OrderFeignClient orderFeignClient;
    private final OrderAdminService orderAdminService;
    
    @GetMapping
    public ApiResponse<PageResponse<OrderDTO>> listOrders(OrderQueryParams params) {
        return orderAdminService.listOrders(params);
    }
    
    @GetMapping("/{id}")
    public ApiResponse<OrderDTO> getOrder(@PathVariable Long id) {
        return orderAdminService.getOrder(id);
    }
    
    @PutMapping("/{id}/status")
    @PreAuthorize("hasPermission('order:update')")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestBody StatusUpdateRequest request) {
        return orderAdminService.updateStatus(id, request);
    }
}
```

### 3.3 删除业务服务中的Admin Controller

```bash
# 删除业务服务中的Admin Controller
rm tailor-is/tailor-is-order/src/main/java/com/tailoris/order/controller/AdminOrderController.java
rm tailor-is/tailor-is-product/src/main/java/com/tailoris/product/controller/AdminProductController.java
rm tailor-is/tailor-is-community/src/main/java/com/tailoris/community/controller/AdminCommunityController.java
rm tailor-is/tailor-is-merchant/src/main/java/com/tailoris/merchant/controller/AdminMerchantController.java
```

### 3.4 更新网关路由配置

确保网关路由 `/api/v1/admin/**` 统一指向 `tailor-is-admin` 服务：

```java
RouteDefinition adminRoute = RouteDefinition.builder()
    .id("admin-route")
    .predicate(PredicateDefinition.builder()
        .name("Path")
        .args(Map.of("_genkey_0", "/api/v1/admin/**"))
        .build())
    .uri("lb://tailor-is-admin")
    .build();
```

### 3.5 更新前端API调用

将前端API调用从业务服务路由改为统一的admin路由：

```typescript
// 旧方式（直接调用业务服务）
GET /api/v1/order/admin/list

// 新方式（统一通过admin服务）
GET /api/v1/admin/order
```

---

## 四、迁移验证清单

### 4.1 功能验证

| 验证项 | 验证方法 | 预期结果 |
|--------|---------|---------|
| 订单列表查询 | 访问 `/api/v1/admin/order` | 返回订单列表数据 |
| 订单详情查询 | 访问 `/api/v1/admin/order/{id}` | 返回订单详情 |
| 订单状态更新 | PUT `/api/v1/admin/order/{id}/status` | 状态更新成功 |
| 商品管理 | 访问 `/api/v1/admin/product` | 返回商品列表 |
| 权限控制 | 使用非管理员账号访问 | 返回403禁止访问 |

### 4.2 路由验证

```bash
# 验证所有admin路由统一指向admin-service
curl -v http://localhost:9000/api/v1/admin/order
# 预期: X-Forwarded-To: tailor-is-admin
```

### 4.3 性能验证

```bash
# 验证接口响应时间
bash deploy/scripts/performance-test.sh
```

---

## 五、迁移风险与应对

### 5.1 风险列表

| 风险ID | 风险描述 | 概率 | 影响 | 应对策略 |
|-------|---------|------|-----|---------|
| RISK-001 | 迁移期间双写导致数据不一致 | 中 | 高 | 迁移前做好数据同步验证 |
| RISK-002 | 前端API路径未更新 | 高 | 中 | 全局搜索 `/api/v1/*/admin` 路径 |
| RISK-003 | 权限配置缺失 | 中 | 高 | 迁移后验证所有接口权限 |
| RISK-004 | Feign调用超时 | 低 | 中 | 配置合理的超时时间 |

### 5.2 回退方案

```bash
# 回退步骤
# 1. 恢复业务服务中的Admin Controller
# 2. 恢复网关路由配置
# 3. 恢复前端API路径
# 4. 验证所有功能正常
```

---

## 六、后续工作

### 6.1 待迁移模块

| 模块 | 优先级 | 计划时间 |
|------|-------|---------|
| AdminAnalyticsController | P1 | 第3周 |
| AdminAcademyController | P2 | 第4周 |
| AdminSupplyController | P2 | 第4周 |

### 6.2 优化建议

1. **统一异常处理**：在admin-service中实现统一的异常处理和错误码管理
2. **日志追踪**：添加全链路日志追踪，便于问题排查
3. **缓存策略**：对高频查询接口添加缓存，提升性能
4. **批量操作**：支持批量更新、批量删除等批量操作接口

---

## 七、参考文档

- [RoutePortConstant.java](file:///home/tailor/Tailoris/tailor-is/tailor-is-common/src/main/java/com/tailoris/common/constant/RoutePortConstant.java)
- [CoreGatewayRouteConfig.java](file:///home/tailor/Tailoris/tailor-is/tailor-is-core-gateway/src/main/java/com/tailoris/coregateway/config/CoreGatewayRouteConfig.java)
- [AdminOrderController.java](file:///home/tailor/Tailoris/tailor-is/tailor-is-admin/src/main/java/com/tailoris/admin/controller/AdminOrderController.java)