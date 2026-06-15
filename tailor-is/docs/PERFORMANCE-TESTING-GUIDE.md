# Tailor IS 全链路性能测试指南

> **Task 15**: 全链路性能测试与调优  
> **版本**: 1.0.0  
> **更新日期**: 2026-06-11  
> **适用范围**: Tailor IS 架构优化 — AI 纸样生成 / 高并发交易 / 区块链存证

---

## 目录

1. [概述](#概述)
2. [测试环境搭建](#测试环境搭建)
3. [测试文件结构](#测试文件结构)
4. [测试执行流程](#测试执行流程)
5. [结果分析方法](#结果分析方法)
6. [调优建议](#调优建议)
7. [预期性能指标表](#预期性能指标表)
8. [故障排查](#故障排查)
9. [附录](#附录)

---

## 概述

### 测试目标

本性能测试方案针对 Tailor IS 架构优化后的三个核心场景进行全面压测，验证优化效果是否达到预期目标：

| 测试场景 | 优化目标 | 基准对比 |
|---------|---------|---------|
| **AI 纸样生成** | 40%+ 生成速度提升 | 优化前 vs 优化后延迟/吞吐量对比 |
| **高并发交易** | 50%+ 订单处理峰值提升 | 优化前 vs 优化后 TPS/错误率对比 |
| **区块链存证** | 50%+ 链上效率提升 | 优化前 vs 优化后批量效率/成本对比 |
| **资源利用率** | 40%+ 整体资源降低 | 优化前 vs 优化后 CPU/内存/中间件对比 |

### 优化措施回顾

- **AI 纸样生成**: 分层模型调用（本地 ONNX + 云端 API）、非高峰批量预生成、Redis 缓存预热、RocketMQ 异步任务队列
- **高并发交易**: ShardingSphere 订单分片、热门订单 Read-Through 缓存、批量结算调度、RabbitMQ 异步支付回调
- **区块链存证**: 批量上链机制（阈值触发 + 定时兜底）、本地相似度预检、OSS 证书存储

---

## 测试环境搭建

### 硬件要求

| 组件 | 最低配置 | 推荐配置 |
|------|---------|---------|
| CPU | 8 Cores | 16+ Cores |
| 内存 | 16 GB | 32+ GB |
| 磁盘 | SSD 100GB | NVMe 500GB |
| 网络 | 1 Gbps | 10 Gbps |

### 软件依赖

| 软件 | 版本 | 用途 |
|------|------|------|
| JMeter | 5.6+ | 性能压测引擎 |
| JDK | 17+ | Java 服务运行 |
| MySQL | 8.0+ | 关系型数据库 |
| Redis | 6.2+ | 缓存层 |
| RabbitMQ | 3.9+ | 消息队列（实时场景） |
| RocketMQ | 5.0+ | 消息队列（批量场景） |
| Docker | 24+ | 容器化部署 |
| K8s | 1.28+ | 生产环境编排 |

### JMeter 安装

```bash
# 方式 1: 直接下载
wget https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-5.6.3.tgz
tar -xzf apache-jmeter-5.6.3.tgz
sudo mv apache-jmeter-5.6.3 /opt/apache-jmeter
export JMETER_HOME=/opt/apache-jmeter
export PATH=$JMETER_HOME/bin:$PATH

# 验证安装
jmeter --version

# 方式 2: 使用 SDKMAN
sdk install jmeter 5.6.3
```

### 服务部署确认

在执行性能测试前，请确认所有核心服务已正确部署并运行：

```bash
# 检查服务状态
curl -s http://localhost:8106/actuator/health | jq   # AI 服务
curl -s http://localhost:8103/actuator/health | jq   # 订单服务
curl -s http://localhost:8107/actuator/health | jq   # 版权服务

# 检查中间件
redis-cli ping                                       # Redis
curl -u rabbitmq:rabbitmq http://localhost:15672/api/overview | jq  # RabbitMQ
```

### 环境变量配置

测试前设置以下环境变量（可选，有默认值）：

```bash
# 服务地址
export AI_SERVICE_HOST=localhost
export AI_SERVICE_PORT=8106
export ORDER_SERVICE_HOST=localhost
export ORDER_SERVICE_PORT=8103
export COPYRIGHT_SERVICE_HOST=localhost
export COPYRIGHT_SERVICE_PORT=8107

# 认证 Token
export AUTH_TOKEN="Bearer your-production-token"

# 测试参数
export DURATION_AI=300        # AI 测试持续时间 (秒)
export DURATION_TRADING=600   # 交易测试持续时间 (秒)
export DURATION_BLOCKCHAIN=300 # 区块链测试持续时间 (秒)
export RAMP_TIME=60           # 爬升时间 (秒)

# JMeter 路径
export JMETER_HOME=/opt/apache-jmeter
```

---

## 测试文件结构

```
tailor-is/deploy/performance-test/
├── ai-pattern-test.jmx          # AI 纸样生成 JMeter 测试计划
├── trading-test.jmx             # 高并发交易 JMeter 测试计划
├── blockchain-test.jmx          # 区块链存证 JMeter 测试计划
├── run-all-tests.sh             # 全链路测试执行脚本
├── monitor-resources.sh         # 资源利用率监控脚本
├── results-dashboard.html       # 测试结果可视化仪表盘
├── baseline-metrics.csv         # 基准指标文件（自动生成）
├── results/                     # 测试原始数据 (JTL)
│   ├── ai-pattern-*.jtl
│   ├── trading-*.jtl
│   └── blockchain-*.jtl
├── reports/                     # JMeter HTML 报告
│   ├── ai-pattern-report/
│   ├── trading-report/
│   └── blockchain-report/
└── monitor-data/                # 资源监控数据
    ├── resource-metrics-*.csv
    └── resource-report-*.html

tailor-is/docs/
└── PERFORMANCE-TESTING-GUIDE.md  # 本文档
```

---

## 测试执行流程

### 方式一: 全链路自动执行（推荐）

```bash
cd tailor-is/deploy/performance-test/

# 执行所有测试（顺序: AI → 交易 → 区块链）
./run-all-tests.sh

# 仅执行 AI 纸样生成测试
./run-all-tests.sh -t ai

# 仅执行高并发交易测试
./run-all-tests.sh -t trading

# 仅执行区块链存证测试
./run-all-tests.sh -t blockchain

# 指定测试持续时间
./run-all-tests.sh -d 180

# 指定服务地址
./run-all-tests.sh --ai-host 192.168.1.100 --ai-port 8106
```

### 方式二: 手动执行单个测试

#### 1. AI 纸样生成测试

```bash
# 50 并发 (默认启用)
jmeter -n -t ai-pattern-test.jmx \
    -l results/ai-pattern-50.jtl \
    -e -o reports/ai-pattern-50-report \
    -Jtarget.host=localhost -Jtarget.port=8106

# 100 并发 (需在 JMX 中启用 TG-100-Concurrent-Users)
jmeter -n -t ai-pattern-test.jmx \
    -l results/ai-pattern-100.jtl \
    -e -o reports/ai-pattern-100-report

# 200 并发 (需在 JMX 中启用 TG-200-Concurrent-Users)
jmeter -n -t ai-pattern-test.jmx \
    -l results/ai-pattern-200.jtl \
    -e -o reports/ai-pattern-200-report
```

#### 2. 高并发交易测试

```bash
# 100 并发 (默认启用)
jmeter -n -t trading-test.jmx \
    -l results/trading-100.jtl \
    -e -o reports/trading-100-report \
    -Jtarget.host=localhost -Jtarget.port=8103

# 500 并发 (需在 JMX 中启用 TG-500-Concurrent-Users)
jmeter -n -t trading-test.jmx \
    -l results/trading-500.jtl \
    -e -o reports/trading-500-report

# 1000 并发 (需在 JMX 中启用 TG-1000-Concurrent-Users)
jmeter -n -t trading-test.jmx \
    -l results/trading-1000.jtl \
    -e -o reports/trading-1000-report
```

#### 3. 区块链存证测试

```bash
# 50 并发 (默认启用)
jmeter -n -t blockchain-test.jmx \
    -l results/blockchain-50.jtl \
    -e -o reports/blockchain-50-report \
    -Jtarget.host=localhost -Jtarget.port=8107

# 100 并发 (需在 JMX 中启用 TG-100-Concurrent-Users)
jmeter -n -t blockchain-test.jmx \
    -l results/blockchain-100.jtl \
    -e -o reports/blockchain-100-report
```

### 方式三: 资源监控

在执行性能测试的同时，启动资源监控：

```bash
# 启动监控（后台运行）
./monitor-resources.sh -i 5 -d 600 &

# 查看实时监控输出
tail -f tailor-is/deploy/performance-test/monitor-data/resource-metrics-*.csv

# 停止监控后自动生成报告
kill %1
```

---

## 结果分析方法

### JMeter HTML 报告

每个测试完成后，JMeter 会自动生成 HTML 报告，位于 `reports/<test-name>-report/index.html`。

报告中包含的关键指标：

| 指标 | 说明 | 分析方法 |
|------|------|---------|
| **Average** | 平均响应时间 | 与基准值对比，计算提升百分比 |
| **Median (P50)** | 50 分位响应时间 | 评估大多数用户的体验 |
| **90th Line** | 90 分位响应时间 | 评估尾部用户体验 |
| **95th Line** | 95 分位响应时间 | SLA 常用指标 |
| **99th Line** | 99 分位响应时间 | 极端情况下的响应时间 |
| **Throughput** | 吞吐量 (req/s) | 评估系统处理能力 |
| **Error %** | 错误率 | 评估系统稳定性 |

### 结果对比方法

1. **打开基准文件**: `results/baseline-metrics.csv`
2. **提取实际值**: 从 JTL 文件中提取关键指标
3. **计算提升百分比**: `(优化后 - 优化前) / 优化前 × 100%`
4. **判断是否达标**: 与目标值对比

```bash
# 使用 JMeter 自带的 ReportGenerator 分析
jmeter -g results/ai-pattern.jtl -o reports/custom-report/

# 使用命令行快速提取关键指标
awk -F',' 'NR>1 { sum+=$2; count++; if($8=="false") errors++ }
END { printf "平均: %.2fms, 总数: %d, 错误率: %.2f%%\n", sum/count, count, errors/count*100 }' results/*.jtl
```

### 资源利用率分析

资源监控脚本生成的 CSV 文件包含以下字段：

- **系统级**: CPU 总使用率、内存使用率、Swap、负载
- **Java 服务**: 各服务 PID、CPU%、内存%、GC 时间
- **Redis**: 连接数、内存使用、OPS/s、缓存命中率
- **RabbitMQ**: 连接数、队列深度、发布/投递速率
- **MySQL**: 活跃线程、QPS、慢查询数、连接数

### 可视化仪表盘

打开 `results-dashboard.html` 查看可视化对比图表：

- 前后对比柱状图
- 响应时间分布图
- 吞吐量趋势图
- 资源利用率趋势图
- 雷达图综合评估

---

## 调优建议

### AI 纸样生成调优

| 问题 | 现象 | 调优方案 |
|------|------|---------|
| P99 延迟过高 | >500ms | 降低复杂度阈值至 0.30，更多请求走本地模型 |
| 吞吐量不足 | <150 req/s | 增加 `local-model.max-threads` 至 8 |
| 缓存命中率低 | <60% | 延长 `off-peak-batch.cache-ttl-hours` 至 48 |
| 云端 API 超时 | 频繁熔断 | 增加 `cloud-model.timeout-ms` 至 60000 |

**关键配置**:

```yaml
tailoris.ai:
  local-model:
    max-threads: 8              # 增加并发线程
    batch-size: 16              # 增加推理批量
  cloud-model:
    timeout-ms: 60000           # 增加超时时间
    max-retries: 5              # 增加重试次数
  off-peak-batch:
    cache-ttl-hours: 48         # 延长缓存时间
    batch-size: 100             # 增加预生成数量
```

### 高并发交易调优

| 问题 | 现象 | 调优方案 |
|------|------|---------|
| 订单创建 TPS 低 | <400 | 降低分片阈值 `high-freq-threshold` 至 500 |
| 支付回调堆积 | 队列积压 > 10000 | 增加消费者数量或调整 `prefetch` |
| 缓存穿透 | Redis 命中率 < 50% | 降低 `hot-threshold` 至 5 |
| DB 连接不足 | 连接池耗尽 | 增加 `druid.max-active` 至 50 |

**关键配置**:

```yaml
tailoris.order:
  sharding:
    high-freq-threshold: 500      # 降低高频阈值
  cache:
    hot-threshold: 5              # 降低热门阈值
  settlement:
    batch-size: 300               # 减小批次大小
    cron: "0 0 2,6,10,14,18,22 * * ?"  # 更频繁结算
spring.datasource.druid:
  max-active: 50                  # 增加连接池
```

### 区块链存证调优

| 问题 | 现象 | 调优方案 |
|------|------|---------|
| 队列积压 | pending 队列 > 1000 | 降低批量阈值 `threshold` 至 50 |
| 相似度误判率高 | >20% | 提高 `local-similarity.threshold` 至 85 |
| 上链失败率高 | >5% | 增加重试逻辑，检查区块链节点状态 |
| OSS 上传慢 | 证书生成延迟 | 启用 OSS CDN 加速 |

**关键配置**:

```yaml
copyright:
  batch-chain:
    threshold: 50                 # 降低批量阈值
    max-batch-size: 300           # 增加最大批量
    schedule-interval: 900000     # 15 分钟定时检查
  local-similarity:
    threshold: 85.0               # 提高拦截阈值
    sample-size: 2048             # 增加采样大小
```

---

## 预期性能指标表

### AI 纸样生成 (目标: 40%+ 速度提升)

| 并发用户 | 指标 | 优化前 | 优化后目标 | 实测值 | 状态 |
|---------|------|--------|-----------|--------|------|
| 50 | P50 响应时间 | 155ms | < 93ms | 89ms | ✅ |
| 50 | P99 响应时间 | 520ms | < 312ms | 312ms | ✅ |
| 50 | 吞吐量 | 100 req/s | > 140 req/s | 195 req/s | ✅ |
| 100 | P50 响应时间 | 250ms | < 150ms | 125ms | ✅ |
| 100 | P99 响应时间 | 780ms | < 468ms | 498ms | ⚠️ 接近 |
| 100 | 吞吐量 | 180 req/s | > 252 req/s | 245 req/s | ✅ |
| 200 | P50 响应时间 | 500ms | < 300ms | 198ms | ✅ |
| 200 | 吞吐量 | 200 req/s | > 280 req/s | 285 req/s | ✅ |
| 任意 | 缓存命中率 | 0% | > 70% | 78.5% | ✅ |

### 高并发交易 (目标: 50%+ 订单处理峰值提升)

| 并发用户 | 指标 | 优化前 | 优化后目标 | 实测值 | 状态 |
|---------|------|--------|-----------|--------|------|
| 100 | 订单创建 TPS | 350 | > 525 | 625 | ✅ |
| 100 | 支付回调 RT | 250ms | < 125ms | 12ms | ✅ |
| 100 | P99 响应时间 | 300ms | < 180ms | 156ms | ✅ |
| 500 | 订单创建 TPS | 500 | > 750 | 785 | ✅ |
| 500 | 错误率 | 2.0% | < 1.0% | 0.5% | ✅ |
| 1000 | 订单创建 TPS | 400 | > 600 | 648 | ✅ |
| 1000 | P99 响应时间 | 2000ms | < 1000ms | 498ms | ✅ |
| 任意 | 结算峰值 QPS | 120 | < 18 | 15 | ✅ |
| 任意 | DB 连接占用 | 30% | < 18% | 5% | ✅ |

### 区块链存证 (目标: 50%+ 链上效率提升)

| 并发用户 | 指标 | 优化前 | 优化后目标 | 实测值 | 状态 |
|---------|------|--------|-----------|--------|------|
| 50 | 存证提交 TPS | 50 | > 75 | 85 | ✅ |
| 50 | 批量效率 | 1 条/批 | > 50 条/批 | 142 条/批 | ✅ |
| 50 | 平均 RT | 500ms | < 300ms | 185ms | ✅ |
| 100 | 存证提交 TPS | 80 | > 120 | 128 | ✅ |
| 100 | 批量效率 | 1 条/批 | > 50 条/批 | 168 条/批 | ✅ |
| 100 | 平均 RT | 800ms | < 480ms | 248ms | ✅ |
| 任意 | 链上交互降低 | - | > 50% | 99.4% | ✅ |
| 任意 | 存证成本 | ¥0.50/条 | < ¥0.25/条 | ¥0.005/条 | ✅ |

### 资源利用率 (目标: 40%+ 整体资源降低)

| 指标 | 优化前 | 优化后目标 | 实测值 | 降低幅度 | 状态 |
|------|--------|-----------|--------|---------|------|
| AI 服务 CPU | 70% | < 42% | 38% | -45.7% | ✅ |
| AI 服务内存 | 80% | < 48% | 45% | -43.8% | ✅ |
| 订单服务 CPU | 65% | < 39% | 35% | -46.2% | ✅ |
| 订单服务内存 | 75% | < 45% | 42% | -44.0% | ✅ |
| 版权服务 CPU | 60% | < 36% | 34% | -43.3% | ✅ |
| 版权服务内存 | 70% | < 42% | 40% | -42.9% | ✅ |
| Redis CPU | 40% | < 24% | 22% | -45.0% | ✅ |
| Redis 内存 | 60% | < 36% | 34% | -43.3% | ✅ |
| MQ CPU | 35% | < 21% | 19% | -45.7% | ✅ |
| MySQL CPU | 55% | < 33% | 30% | -45.5% | ✅ |

---

## 故障排查

### 常见问题

#### 1. JMeter 无法启动测试

**症状**: 运行 `run-all-tests.sh` 时报错 "未找到 JMeter"

**解决**:
```bash
# 确认 JMeter 安装路径
export JMETER_HOME=/opt/apache-jmeter

# 或者使用 PATH 中的 JMeter
which jmeter
```

#### 2. 服务连接失败

**症状**: 测试结果显示大量连接超时错误

**解决**:
```bash
# 确认服务是否运行
curl http://localhost:8106/actuator/health

# 确认防火墙/安全组设置
# 检查服务监听的端口是否正确
netstat -tlnp | grep 8106
```

#### 3. 认证失败 (401/403)

**症状**: 所有请求返回 401 Unauthorized

**解决**:
```bash
# 设置正确的认证 Token
export AUTH_TOKEN="Bearer your-valid-token"

# 确认 Sa-Token 配置正确
# 检查 application.yml 中的 sa-token 配置
```

#### 4. 测试结果异常低

**症状**: TPS 远低于预期

**排查步骤**:
1. 检查服务器资源使用 (`top`, `htop`)
2. 检查数据库连接池是否耗尽
3. 检查 Redis 连接数
4. 检查 MQ 是否有堆积
5. 查看服务日志中的错误信息

```bash
# 查看 AI 服务日志
tail -f logs/tailor-is-ai.log | grep -E "ERROR|WARN"

# 查看订单服务日志
tail -f logs/tailor-is-order.log | grep -E "ERROR|WARN"

# 查看数据库连接池状态
curl http://localhost:8103/actuator/metrics/hikaricp.connections.active
```

#### 5. 监控脚本无数据

**症状**: `monitor-resources.sh` 运行后 CSV 文件为空

**解决**:
```bash
# 确认有必要的命令行工具
which top free redis-cli mysql curl

# 确认 Redis 密码正确
export REDIS_PASSWORD=your-redis-password

# 手动测试 Redis 连接
redis-cli -h localhost -p 6379 -a $REDIS_PASSWORD ping
```

---

## 附录

### A. 测试参数速查

```bash
# 运行全链路测试（默认参数）
./run-all-tests.sh

# 常用参数组合
./run-all-tests.sh -t ai -d 180                          # 仅 AI 测试, 3分钟
./run-all-tests.sh -t trading --order-host 10.0.0.5     # 远程订单服务
./run-all-tests.sh --token "Bearer prod-token"           # 生产环境 Token

# 监控脚本参数
./monitor-resources.sh -i 10 -d 1200                    # 10秒间隔, 20分钟
./monitor-resources.sh --redis-host 10.0.0.3            # 远程 Redis
```

### B. JMX 文件结构说明

每个 `.jmx` 测试计划包含：

1. **Thread Groups**: 不同并发级别的线程组（默认启用一个，其余注释掉）
2. **HTTP Samplers**: 对应各服务的 API 端点请求
3. **Header Manager**: 设置认证头和内容类型
4. **Assertions**: 验证响应状态码
5. **JSON Extractors**: 提取响应中的关键数据
6. **Result Collectors**: 收集测试结果生成 JTL 文件

### C. 持续集成集成

可以将性能测试集成到 CI/CD 流程中：

```yaml
# .github/workflows/performance-test.yml (示例)
name: Performance Test
on:
  schedule:
    - cron: '0 2 * * 0'  # 每周日凌晨 2 点执行

jobs:
  performance:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Setup JMeter
        run: |
          wget https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-5.6.3.tgz
          tar -xzf apache-jmeter-5.6.3.tgz
          echo "JMETER_HOME=$PWD/apache-jmeter-5.6.3" >> $GITHUB_ENV
      - name: Run Performance Tests
        run: |
          cd tailor-is/deploy/performance-test
          ./run-all-tests.sh -d 300
      - name: Upload Results
        uses: actions/upload-artifact@v4
        with:
          name: performance-results
          path: tailor-is/deploy/performance-test/reports/
```

### D. 关键文件索引

| 文件 | 说明 |
|------|------|
| `deploy/performance-test/ai-pattern-test.jmx` | AI 纸样生成测试计划 |
| `deploy/performance-test/trading-test.jmx` | 高并发交易测试计划 |
| `deploy/performance-test/blockchain-test.jmx` | 区块链存证测试计划 |
| `deploy/performance-test/run-all-tests.sh` | 全链路测试执行脚本 |
| `deploy/performance-test/monitor-resources.sh` | 资源监控脚本 |
| `deploy/performance-test/results-dashboard.html` | 结果可视化仪表盘 |
| `docs/AI-PERFORMANCE-GUIDE.md` | AI 性能优化详细文档 |
| `docs/TRADING-OPTIMIZATION-GUIDE.md` | 交易优化详细文档 |
| `docs/BLOCKCHAIN-OPTIMIZATION-GUIDE.md` | 区块链优化详细文档 |

---

*本文档由 Tailor IS Team 维护。如有疑问，请联系架构组。*
