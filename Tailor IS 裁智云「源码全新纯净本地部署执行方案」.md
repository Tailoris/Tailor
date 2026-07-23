# Tailor IS 裁智云「源码全新纯净本地部署执行方案」

# Tailor IS 裁智云「源码全新纯净本地部署执行方案」

## 文档目标

彻底规避三大痛点：

1. 多 Java 容器同时运行导致整机 CPU 满载；

2. 旧环境残留、新旧配置混杂，旧问题反复复现、改完又出新 Bug；

3. 部署流程无标准、无校验、无回归测试，上线后各类缺陷持续爆发。
适用场景：全量清空旧环境 → 纯净 Git 源码拉取 → 从零本地标准化部署，全程无 1Panel、统一 Docker Compose Profile 分组、资源硬隔离、自动化校验 \+ 全链路验收闭环。

# 一、前置准备：全量数据备份 \+ 旧环境彻底销毁（必做，不可逆）

## 1\.1 一键备份脚本 `full_backup.sh`

```bash
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/data/tailor_backup/$DATE"
mkdir -p $BACKUP_DIR

# 1. MySQL全库备份
docker exec tailor-is-mysql mysqldump -uroot -p --all-databases > $BACKUP_DIR/mysql_full.sql
# 2. AI版型、区块链、静态素材
tar -zcvf $BACKUP_DIR/assets.tar.gz /data/tailor-is-prod/ai-resource /data/tailor-is-prod/chain-storage /data/tailor-is-prod/static-cdn
# 3. 环境变量、Nginx证书、脚本
tar -zcvf $BACKUP_DIR/config.tar.gz /data/tailor-is-prod/env /data/tailor-is-prod/gateway/ssl /data/tailor-is-prod/script
# 4. Git稳定源码归档
git clone -b release-stable /git/tailor-is $BACKUP_DIR/source_code
echo "备份完成：$BACKUP_DIR"
```

执行：`bash full_backup.sh`，备份文件异地留存。

## 1\.2 彻底销毁旧环境一键脚本 `full_destroy.sh`

```bash
#!/bin/bash
# 停止并删除全部容器
docker stop $(docker ps -a -q) 2>/dev/null
docker rm $(docker ps -a -q) 2>/dev/null
# 删除全部数据卷、自定义网络
docker volume rm $(docker volume ls -q) 2>/dev/null
docker network rm tailor-mid-net tailor-core-net tailor-light-net tailoris-services 2>/dev/null
# 深度清理镜像、构建缓存
docker system prune -a --volumes -f
# 删除旧项目根目录
rm -rf /data/tailor-is-prod
# 清理1Panel所有残留
rm -rf /opt/1panel /etc/1panel /var/lib/1panel /usr/local/bin/1pctl
echo "旧环境全部销毁完成，无任何残留"
```

执行：`bash full_destroy.sh`，执行后确认无任何 Tailor 相关容器 / 目录 / 网络。

# 二、纯净源码拉取 \& 全新标准化目录初始化

## 2\.1 拉取稳定纯净源码（不复用旧工程）

GitHub 配置信息

- 账号名称 : Tailor IS

- 绑定邮箱 : Tailor\-is@outlook\.com

- 仓库地址 : https://github\.com/Tailoris/Tailor

- 认证方式 : SSH \(ED25519\)

```bash
# 创建全新独立部署根目录
mkdir -p /data/tailor-is-prod && cd /data/tailor-is-prod
# 拉取线上稳定发布分支（无本地历史脏修改）
git clone -b release-stable 项目Git地址 ./source
cd source
# 删除源码内废弃遗留配置
rm -f docker-compose.development.yml 1panel-install.sh old-mysql-conf.yml
```

## 2\.2 标准固定目录结构（严格统一，禁止随意修改）

```Plain Text
/data/tailor-is-prod
├── source/                  # 纯净Git源码
├── env/                     # 环境变量区分开发/生产
│   ├── .env.base           # 公共中间件配置
│   ├── .env.dev            # 开发轻量化JVM（核心控CPU）
│   └── .env.nginx          # 网关配置
├── middleware/              # 中间件统一Compose（infra分组）
│   └── docker-compose.services.yml
├── core-services/           # 核心微服务：交易/AI/区块链/用户网关
├── light-services/          # 轻量单体：社区/学堂/数据服务
├── frontend/                # PC商家后台、H5商城、设计工作台SSR
├── ai-resource/             # AI版型模型、渲染缓存
├── chain-storage/           # 区块链存证证书
├── static-cdn/              # 静态图纸、视频素材
├── gateway/                 # Nginx统一路由、分层日志、SSL
├── script/                  # 全套标准化运维脚本
│   ├── pre-deploy-check.sh  # 部署前预检（拦截残留/违规配置）
│   ├── dev-start.sh         # Profile分组按需启动
│   ├── dev-stop-all.sh      # 一键关停业务服务
│   ├── idle-auto-stop.sh    # 闲置30分钟服务自动停机
│   ├── resource-monitor.sh  # CPU负载告警（>85%输出日志）
│   └── cleanup-conflict-resources.sh
└── .trae/rules/             # 强制规范，部署自动校验
    ├── deployment-architecture-rule.md
    └── exception-handling-rule.md
```

# 三、底层核心约束：从架构杜绝 CPU 满载、故障反复复发

## 3\.1 Docker 网络统一规划（无 host 混杂、固定三层隔离网络）

```bash
# 全新创建隔离网络，仅执行一次
docker network create tailor-mid-net    # 中间件专属
docker network create tailor-core-net  # 核心付费微服务
docker network create tailor-light-net # 免费轻量服务
```

规则：

1. 中间件仅接入`tailor-mid-net`；

2. 核心服务同时接入`tailor-mid-net`\+`tailor-core-net`；

3. 轻量服务同时接入`tailor-mid-net`\+`tailor-light-net`；

4. Nginx 网关同时接入三张网络，唯一对外流量入口。

## 3\.2 Compose Profile 分组机制（根治一次性拉起全部 14 个 Java 容器）

### 分组清单（禁止无分组全局启动）

|Profile 分组|内容|使用场景|
|---|---|---|
|infra|Nacos、MySQL、Redis、RabbitMQ、RocketMQ|所有开发必须优先启动|
|core\-trade|网关、用户服务、交易、商户后台|商城 / 后台开发|
|core\-ai\-chain|AI 制版、3D 渲染、区块链版权|设计模块开发|
|light\-forum|社区服务|仅调试社区|
|light\-edu|学堂、知识库|教育模块调试|
|light\-data|数据分析、供应链|增值报表开发|

### Compose 强制资源配额（每个 Java 容器必须配置，不可省略）

1. 核心微服务：`cpus:0.8`、内存 384M

2. 轻量单体：`cpus:0.5`、内存 256M
示例片段：

```yaml
deploy:
  resources:
    limits:
      cpus: "0.8"
      memory: "384M"
    reservations:
      cpus: "0.2"
      memory: "128M"
healthcheck:
  test: ["CMD","curl","-f","http://127.0.0.1:端口/actuator/health"]
  interval: 30s
  timeout: 5s
  retries: 3
```

## 3\.3 开发轻量化 JVM（\.env\.dev 固定配置，降低单服务 CPU 消耗）

```env
JAVA_OPTS_DEV=-XX:+UseContainerSupport \
-XX:ActiveProcessorCount=2 \
-Xms128m -Xmx256m \
-XX:MaxMetaspaceSize=128m \
-XX:-TieredCompilation \
-XX:ParallelGCThreads=2 \
-Dserver.tomcat.threads.max=60 \
-Dspring.profiles.active=dev
```

关键限制：强制 JVM 最多识别 2 核，杜绝单服务抢占整机算力。

# 四、标准化分步部署执行流程（严格按顺序，禁止跳步）

## 步骤 1：部署前置预检（阻断旧残留、违规配置）

```bash
cd /data/tailor-is-prod
bash ./script/pre-deploy-check.sh
```

预检校验项：

1. 服务器无旧 tailor 容器、网络、项目目录；

2. Compose 无 1Panel 旧容器名、废弃 yml 依赖；

3. \.trae/rules 两套规范文件完整；

4. 环境变量 dev 轻量化 JVM 参数存在；
预检失败直接终止部署，必须先执行 full\\\[\_destroy\.sh\]\(\_destroy\.sh\) 清理。

## 步骤 2：启动中间件 infra 分组（强制先行，解决 MySQL 未就绪登录报错）

```bash
bash ./script/dev-start.sh infra
```

等待全部中间件状态 healthy 后校验：

1. tailor\-is\-mysql 正常监听 40300 端口；

2. 导入备份 mysql\_full\.sql；

3. 校验 sys\_user 表完整字段：nick\_name/id\_card\_front/id\_card\_back/certification/wx\_openid；
**中间件未就绪，禁止启动任何 Java 业务服务**。

## 步骤 3：按需启动业务分组（禁止一次性启动全部 14 个服务）

### 场景 1：仅开发商家后台、订单（日常高频）

```bash
bash ./script/dev-start.sh infra core-trade
```

### 场景 2：开发 AI 制版、版权模块

```bash
bash ./script/dev-start.sh infra core-ai-chain
```

### 场景 3：调试社区学堂

```bash
bash ./script/dev-start.sh infra light-forum light-edu
```

约束：单次同时运行 Java 容器≤8 个，脚本内置拦截，防止 CPU 叠加满载。

## 步骤 4：Nginx 网关启动，加载统一路由规范

端口、域名、API 前缀严格沿用标准化方案：

- 核心接口`/api/core/*`高优先级限流；

- 轻量接口`/api/light/*`普通限流；

- 支付、区块链回调接口关闭限流；

- 前端区分 shop/design/admin/forum/edu 多域名隔离。

# 五、代码层强制约束（杜绝登录报错、异常弹窗重复故障）

## 5\.1 后端异常规范（\.trae/rules/\[exception\-handling\-rule\.md\]\(exception\-handling\-rule\.md\) 强制校验）

1. `GlobalExceptionHandler` 添加 `@Order(HIGHEST_PRECEDENCE)`；

2. `AdminGlobalExceptionHandler` 指定 basePackages，移除通用 Exception 兜底；

3. `NotLoginException` 返回标准 401；数据库 / 中间件故障返回独立 503 错误码；

4. 禁止所有异常统一抛出 “账号密码错误” 误导文案。

## 5\.2 前端 LoginView 修复（解决双重弹窗、错误提示混淆）

1. 响应拦截器与登录页面统一一套 Toast 提示逻辑，去除重复弹窗；

2. 分支判断：

    - 503 → 仅提示「服务暂时不可用」；

    - 401 / 账号校验失败 → 仅提示「账号或密码错误」；

3. 模拟 MySQL 宕机场景单独回归测试。

# 六、部署完成强制全链路验收清单（全部通过才算部署成功）

## 6\.1 基础设施验收

1. 所有容器命名统一`tailor-is-xxx`，无 \- dev/1Panel 残留后缀；

2. 三层 Docker 网络正常，无 host 模式混用；

3. 整机 CPU 稳定＜60%，无持续走高趋势；

4. 闲置脚本、监控脚本执行无报错。

## 6\.2 数据库校验

1. sys\_user 全部字段齐全；

2. 历史备份账号可正常查询、密码加密校验正常。

## 6\.3 登录核心链路专项回归（解决历史重复故障）

1. 正确账号密码：返回 200\+token，正常进入后台；

2. 错误密码：仅弹出账号错误，无服务不可用提示；

3. 关闭 MySQL 模拟故障：仅弹出服务不可用，无账号错误误导；

4. 无双重弹窗、页面跳转正常；

5. 未登录访问后台自动跳转登录页。

## 6\.4 架构规范验收

1. 所有 Java 容器配置 CPU / 内存 limits；

2. 服务均绑定 Profile，无全局自动启动；

3. 无废弃 compose、1Panel 硬编码配置；

4. 异常处理器完全符合规则文档要求。

# 七、长效运维机制：杜绝后续问题反复出现

## 7\.1 定时自动资源管控（crontab 写入系统定时）

```cron
# 每30分钟自动关停30分钟无访问的Java服务
*/30 * * * * /data/tailor-is-prod/script/idle-auto-stop.sh
# 每小时检测整机CPU，超过85%输出告警日志
0 * * * * /data/tailor-is-prod/script/resource-monitor.sh
# 每日凌晨2点清理Docker镜像、AI临时缓存
0 2 * * * /data/tailor-is-prod/script/cleanup-conflict-resources.sh
```

## 7\.2 Git 提交前置校验（pre\-commit 钩子）

每次代码提交自动校验：

1. 异常处理器是否符合规范；

2. 前端登录页面错误提示逻辑；

3. Compose 脚本无旧 1Panel 命名、废弃配置；
校验不通过直接阻断提交，从源头产生违规代码。

## 7\.3 迭代变更流程规范

1. 任何代码 / 部署脚本修改完成后，必须执行完整全链路验收；

2. 出现重复历史故障，同步更新\.trae/rules 强制规则固化解决方案；

3. 禁止在运行容器内直接修改配置，全部通过源码 \+ 重新构建发布。

# 八、本方案彻底解决混乱问题的核心保障

1. **环境绝对纯净**：全量销毁旧容器、卷、网络、目录，不存在新旧配置混杂，根除旧问题复现土壤；

2. **资源硬隔离 \+ 按需启动**：禁止一次性拉起所有 Java 服务，单容器 CPU 上限锁死，杜绝整机 CPU 99% 满载；

3. **流程强校验闭环**：部署预检→分层启动→全链路回归，隐性缺陷部署阶段直接暴露，不会上线后持续爆发；

4. **规范自动化落地**：规则文件 \+ Git 钩子 \+ 预检脚本三重强制，不靠人工记忆遵守架构标准；

5. **故障分层隔离**：核心交易 / AI 与社区轻量服务网络、限流、资源完全隔离，轻量业务异常不会影响核心登录、交易链路；

6. **可复现标准化流程**：整套脚本、目录、配置完全标准化，下次重装直接复用，不会出现每次部署环境不一致的混乱。

> （注：部分内容可能由 AI 生成）
