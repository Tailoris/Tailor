# Tailor IS 测试环境部署与功能验证手册（W1末）

**文档编号**: TAILOR-IS-DEPLOY-STAGING-2026-0603
**版本**: V1.0
**适用范围**: 1Panel + Ubuntu/Debian + Docker

---

## 1. 部署前置条件

### 1.1 服务器要求

| 项目 | 最低配置 | 推荐配置 |
|------|---------|---------|
| CPU | 4核 | 8核+ |
| 内存 | 8GB | 16GB+ |
| 磁盘 | 50GB | 100GB+ |
| 网络 | 100Mbps | 1Gbps |
| 系统 | Ubuntu 20.04+ | Ubuntu 22.04 LTS |
| Java | OpenJDK 17 | OpenJDK 17/21 |

### 1.2 已部署服务（1Panel）

- ✅ MySQL 8.0 (172.28.249.179:3306)
- ✅ Redis 7.2 (172.28.249.179:6379)
- ✅ RabbitMQ 3.12 (172.28.249.179:5672, 管理:15672)
- ✅ Nacos 2.3.0 (172.28.249.179:8081, API:8848)
- ✅ 1Panel 面板 (172.28.249.179:31691)

---

## 2. 部署步骤

### 2.1 准备部署目录

```bash
# 登录服务器
ssh root@172.28.249.179

# 创建部署目录
mkdir -p /opt/tailor-is/{scripts,logs,backup,apps,docker-jars,upload,source}
cd /opt/tailor-is

# 复制文件
# - deploy-staging.sh -> /opt/tailor-is/scripts/
# - e2e-test.sh       -> /opt/tailor-is/scripts/
# - deploy-and-test.sh -> /opt/tailor-is/scripts/
# - .env              -> /opt/tailor-is/
# - tailor-is/        -> /opt/tailor-is/source/
```

### 2.2 配置 .env 文件

```bash
# 复制环境变量模板
cp /opt/tailor-is/source/tailor-is/deploy/.env.example /opt/tailor-is/.env

# 编辑（关键变量）
nano /opt/tailor-is/.env
```

关键配置项：

```bash
MYSQL_HOST=172.28.249.179
MYSQL_PORT=3306
MYSQL_USERNAME=root
MYSQL_PASSWORD=<1Panel设置的MySQL密码>

REDIS_HOST=172.28.249.179
REDIS_PORT=6379
REDIS_PASSWORD=<1Panel设置的Redis密码>

RABBITMQ_HOST=172.28.249.179
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=rabbitmq
RABBITMQ_PASSWORD=rabbitmq

NACOS_ADDR=172.28.249.179:8848
```

### 2.3 上传JAR文件

方式A：从本地编译后上传

```bash
# 在Windows下编译
cd F:\Tailor\Tailor is\tailor-is
mvn clean package -DskipTests -Dcheckstyle.skip=true

# 上传所有target/*-1.0.0.jar到服务器的/opt/tailor-is/upload/目录
# 文件名需重命名为: tailor-is-{服务名}-1.0.0.jar
```

方式B：使用WinSCP/1Panel文件管理
- 将所有服务的JAR上传到 `/opt/tailor-is/upload/`

### 2.4 执行部署

```bash
# 设置脚本可执行权限
chmod +x /opt/tailor-is/scripts/*.sh

# 方式1: 仅部署
/opt/tailor-is/scripts/deploy-staging.sh

# 方式2: 部署+测试（推荐）
/opt/tailor-is/scripts/deploy-and-test.sh
```

### 2.5 部署后验证

```bash
# 1. 查看运行中的服务
ps aux | grep tailor-is

# 2. 查看日志
tail -f /opt/tailor-is/logs/tailor-is-gateway.log

# 3. 健康检查
curl http://localhost:8080/actuator/health

# 4. 注册中心查看
curl http://172.28.249.179:8081/nacos/
```

---

## 3. 完整功能验证测试

### 3.1 单独执行E2E测试

```bash
/opt/tailor-is/scripts/e2e-test.sh
```

### 3.2 测试套件说明

| 套件 | 验证项 | 对应Critical Fix |
|------|--------|-----------------|
| 套件1 认证安全 | 登录、锁定、限流、验证码 | B-C05/B-C06/B-C09 |
| 套件2 网关鉴权 | 白名单、Token验证 | TD-CR2 |
| 套件3 商品服务 | CRUD、并发控制 | B-C08 |
| 套件4 订单服务 | 幂等、库存预扣 | B-C07 |
| 套件5 基础设施 | 密码配置、安全认证 | B-C01~B-C04 |

### 3.3 测试结果示例

```
================================================
Tailor IS E2E功能验证测试
开始时间: 2026-06-03 14:30:00
目标服务: http://localhost:8080
================================================

================================================
套件1: 认证安全测试 (Critical B-C05/B-C06/B-C09)
================================================
[TEST] 1.1 正常登录
[✓] 1.1 正常登录 (code=200)
[TEST] 1.2 错误密码登录
[✓] 1.2 错误密码应失败 (code=500)
...

通过: 30
失败: 0
跳过: 5
通过率: 100.00%
```

---

## 4. 测试结果分析

### 4.1 通过标准

- ✅ 所有Critical级别验证100%通过
- ✅ 关键业务流程完整
- ✅ 性能指标在阈值内（P95 < 200ms）

### 4.2 失败处理

| 现象 | 排查方向 |
|------|---------|
| 登录失败 | 检查Nacos、Redis连接 |
| 网关401 | 检查AuthGlobalFilter白名单配置 |
| 限流未触发 | 调整RateLimit阈值或压力 |
| 订单失败 | 检查MySQL表结构、库存字段 |

### 4.3 常见问题

#### Q1: 启动卡在"Nacos注册中"
A: 检查Nacos配置、端口、命名空间

#### Q2: Redis连接失败
A: 确认密码正确，防火墙开放6379

#### Q3: 编译失败
A: 添加参数 `-Dcheckstyle.skip=true -Ddependency-check.skip=true`

---

## 5. 部署检查清单

### 5.1 部署前

- [ ] 服务器CPU/内存/磁盘充足
- [ ] 1Panel基础设施已部署并验证
- [ ] .env文件已正确配置
- [ ] JAR文件已上传到upload目录
- [ ] Java 17已安装

### 5.2 部署中

- [ ] 部署脚本执行无错误
- [ ] 数据库初始化完成
- [ ] 13个微服务全部启动
- [ ] Nacos服务注册成功
- [ ] Redis连接正常

### 5.3 部署后

- [ ] E2E测试100%通过（或通过率≥95%）
- [ ] 健康检查全绿
- [ ] 日志无ERROR级别
- [ ] 关键接口性能达标
- [ ] 备份目录生成

---

## 6. 应急回滚

如部署后发现严重问题：

```bash
# 1. 停止所有服务
ps aux | grep tailor-is | awk '{print $2}' | xargs -r kill -9

# 2. 恢复备份
LAST_BACKUP=$(cat /opt/tailor-is/backup/.last_backup)
echo "恢复备份: $LAST_BACKUP"
cp $LAST_BACKUP/*.jar /opt/tailor-is/apps/

# 3. 重启服务
/opt/tailor-is/scripts/start-services.sh

# 4. 验证
/opt/tailor-is/scripts/e2e-test.sh
```

---

## 7. 部署签字

| 角色 | 姓名 | 操作 | 时间 |
|------|------|------|------|
| 部署执行 | _________ | 部署完成 | _________ |
| 测试执行 | _________ | 测试完成 | _________ |
| 测试结果 | _________ | 通过/失败 | _________ |
| 验收签字 | _________ | 通过/驳回 | _________ |

---

**部署手册结束**
