# Tailor IS k6 压测脚本执行说明

## 环境准备

### 安装 k6
```bash
# macOS
brew install k6

# Linux (Debian/Ubuntu)
sudo apt-get install k6

# Docker
docker pull grafana/k6:latest
```

## 脚本列表

| 脚本 | 用途 | 目标 QPS |
|------|------|----------|
| `load-test.js` | 基础负载测试 | 1000 |
| `stress-test.js` | 压力测试（阶梯递增） | 5000 |
| `spike-test.js` | 峰值测试（突发流量） | 10000 |
| `soak-test.js` | 浸泡测试（长时间运行） | 500 |

## 执行方式

### 本地执行
```bash
# 基础负载测试（100 并发，持续 60 秒）
k6 run --vus 100 --duration 60s load-test.js

# 指定环境变量
k6 run -e API_BASE_URL=http://localhost:9000 -e AUTH_TOKEN=xxx load-test.js

# 输出 JSON 报告
k6 run --out json=results.json load-test.js

# 输出到 InfluxDB + Grafana
k6 run --out influxdb=http://localhost:8086/k6 load-test.js
```

### Docker 执行
```bash
# 基础负载测试
docker run --rm -i grafana/k6 run - < load-test.js

# 挂载脚本目录
docker run --rm -v $(pwd):/scripts grafana/k6 run /scripts/load-test.js
```

### CI/CD 集成
```yaml
# GitHub Actions 示例
- name: Run k6 load test
  uses: grafana/k6-action@v0.3
  with:
    filename: k6-scripts/load-test.js
    flags: --vus 50 --duration 30s
```

## 测试场景说明

### 1. 基础负载测试 (load-test.js)
- 模拟正常流量：100 VUs，持续 5 分钟
- 包含：首页访问、商品列表、商品详情、搜索
- 关键指标：P95 延迟 < 500ms，错误率 < 0.1%

### 2. 压力测试 (stress-test.js)
- 阶梯递增：10 → 50 → 100 → 200 → 500 VUs
- 每个阶段持续 2 分钟
- 目的：找到系统瓶颈和拐点

### 3. 峰值测试 (spike-test.js)
- 模拟秒杀/大促：短时间内从 0 → 1000 VUs
- 持续 30 秒高并发后回落
- 目的：验证系统弹性伸缩能力

### 4. 浸泡测试 (soak-test.js)
- 长时间运行：50 VUs，持续 4 小时
- 目的：检测内存泄漏、连接池耗尽等长期问题

## 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `API_BASE_URL` | 后端 API 地址 | `http://localhost:9000` |
| `GRAPHQL_URL` | GraphQL 网关地址 | `http://localhost:4000/graphql` |
| `AUTH_TOKEN` | 测试账号 Token | 需手动设置 |
| `TARGET_QPS` | 目标 QPS | 1000 |

## 结果解读

### 关键指标
- `http_req_duration` - 请求延迟（关注 P95/P99）
- `http_req_failed` - 请求失败率（应 < 1%）
- `http_reqs` - 总请求数
- `vus` - 活跃虚拟用户数
- `iterations` - 总迭代次数

### 性能阈值
| 指标 | 黄色告警 | 红色告警 |
|------|----------|----------|
| P95 延迟 | > 500ms | > 1000ms |
| 错误率 | > 1% | > 5% |
| P99 延迟 | > 1000ms | > 3000ms |