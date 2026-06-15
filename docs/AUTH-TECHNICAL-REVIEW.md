# Tailor IS 认证方案技术评审报告

**文档编号**: TR-2026-0530-001  
**评审日期**: 2026-05-30  
**评审人**: 技术架构组  

---

## 一、评审背景

### 1.1 当前认证架构

```
前端(Authorization Header)
    │
    ▼
Spring Cloud Gateway (AuthGlobalFilter)
    │ Sa-Token StpUtil.getLoginIdByToken()
    ▼
微服务 (Sa-Token StpUtil)
    │ @SaCheckRole / @SaCheckPermission
    ▼
业务逻辑层
```

### 1.2 文档规划 vs 实际实现

| 维度 | 技术方案文档 | 实际实现 | 偏差评估 |
|------|------------|---------|---------|
| 安全框架 | Spring Security + JWT | Sa-Token 1.37.0 | 重大偏差 |
| Token类型 | 双Token(Access 30min + Refresh 7d) | 单Token(已改为30min) | 功能缺失 |
| Token格式 | JWT (jjwt库) | Sa-Token UUID格式 | 格式偏差 |
| Session存储 | Redis (Spring Session) | Sa-Token Redis集成 | 等效 |
| 网关认证 | Spring Security OAuth2 | Sa-Token Reactor | 等效 |
| 权限模型 | RBAC (Spring Security ACL) | Sa-Token RBAC | 等效 |

---

## 二、方案对比分析

### 2.1 方案A：保持Sa-Token（当前实现）

**优势**:
- 代码已实现，无需重构
- API简洁（StpUtil一行搞定登录/认证/权限）
- 文档丰富，中文社区活跃
- 支持注解鉴权、路由鉴权、Session会话
- 集成简单，无需复杂的Security配置

**劣势**:
- 非行业标准JWT格式，Token为UUID不可自解析
- 每次API调用需查询Redis获取Session（性能开销）
- 缺少Refresh Token自动续期机制
- 与技术方案文档不一致
- 无法被第三方系统解析Token内容

**安全性评估**: ⭐⭐⭐⭐ (4/5)
- Token存储在Redis，支持主动踢出
- 支持二级认证、同端互斥
- UUID格式Token不可被外部解析，防泄露

**性能评估**: ⭐⭐⭐ (3/5)
- 每次请求需Redis查Session（约1-3ms）
- 高并发下Redis可能成为瓶颈
- 可使用二级缓存优化

### 2.2 方案B：回退Spring Security + JWT

**优势**:
- 行业标准，生态成熟
- JWT可自解析（无需每次查Redis）
- 双Token机制更安全（Access短+Refresh长）
- 与文档一致
- Spring Security提供完整安全链

**劣势**:
- 需要大幅度重构（预估5-8人天）
- JWT无法主动踢出（需黑名单机制）
- 配置复杂，学习曲线陡峭
- 已投入的Sa-Token开发工作需废弃

**安全性评估**: ⭐⭐⭐⭐⭐ (5/5)
- Spring Security CSRF/XSS/CORS一体化
- JWT签名防止篡改
- SecurityFilterChain提供完整防护

**性能评估**: ⭐⭐⭐⭐ (4/5)
- JWT自解析无需Redis（减少网络开销）
- Access Token 30min短期 + Refresh Token 7天双Token

### 2.3 方案C：混合方案（Sa-Token + JWT风格Token）

**优势**:
- 保留现有Sa-Token代码
- Sa-Token支持自定义Token生成策略
- 可实现类似JWT的Token格式
- 渐进式迁移，风险可控

**劣势**:
- 增加系统复杂度
- 需自行实现Token解析逻辑

**安全性评估**: ⭐⭐⭐⭐ (4/5)
**性能评估**: ⭐⭐⭐⭐ (4/5)

---

## 三、综合评估与建议

### 3.1 推荐方案：方案A - 保持Sa-Token + 增强

**综合得分**: 安全性4/5 + 性能3/5 + 开发效率5/5 + 维护成本4/5 = **16/20**

**理由**:
1. **当前代码已稳定**：所有14+个微服务模块均已基于Sa-Token实现，回退代价大
2. **安全等级达标**：Redis Token存储支持主动踢出，优于纯JWT
3. **性能可通过优化提升**：引入Caffeine本地缓存减少Redis查询
4. **功能完整性**：RBAC、注解鉴权、Session管理均已实现

### 3.2 增强建议

| 增强项 | 优先级 | 实现方案 | 预期效果 |
|--------|-------|---------|---------|
| **Refresh Token** | High | Sa-Token `setActivityTimeout(1800)` + `setTimeout(604800)` | 实现30min活跃超时+7天总超时 |
| **Token风格统一** | Medium | `token-style: simple-uuid` | 简化Token格式 |
| **二级缓存** | Medium | Sa-Token二级缓存(Redis+Caffeine) | 减少Redis查询50%+ |
| **Token刷新API** | Medium | 新增`/api/auth/refresh`端点 | 前端无感续期 |
| **日志审计** | Low | 添加Sa-Token登录日志监听器 | 安全审计追溯 |

### 3.3 迁移路径（如选择方案B）

| 阶段 | 工作内容 | 工作量 |
|------|---------|-------|
| Phase 1 | 替换依赖：sa-token → spring-security + jjwt | 1天 |
| Phase 2 | 实现JwtTokenProvider + SecurityConfig | 2天 |
| Phase 3 | 迁移所有Controller注解 (@SaCheck → @PreAuthorize) | 2天 |
| Phase 4 | 迁移Gateway Filter (SaToken → JWT验证) | 1天 |
| Phase 5 | 集成测试 + 回归测试 | 2天 |
| **总计** | | **8天** |

---

## 四、结论

**建议保持Sa-Token方案**，通过配置优化和功能增强达到文档规划的安全目标。如需与文档严格一致，建议**更新技术方案文档**以反映当前Sa-Token实现，而非回退代码。

**下一步行动**:
1. ✅ 更新Sa-Token timeout为1800s（已完成）
2. ⏳ 配置Sa-Token activity-timeout (30min活跃超时)
3. ⏳ 更新技术方案文档认证章节
4. ⏳ 续期API端点开发

---

*本报告基于代码审查和架构分析生成，建议技术团队评审后确定最终方案。*