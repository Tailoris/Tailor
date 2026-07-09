# Tailor IS（裁智云）全新无1Panel部署优化方案（根治多Java容器CPU满载缺陷完整版）

# Tailor IS（裁智云）全新无 1Panel 部署优化方案（根治多 Java 容器 CPU 满载缺陷完整版）

## 文档概述

本方案基于原有「混合微服务 \+ 无面板 Docker/Nginx 部署规范」全面重构，针对**本地开发 17 个 SpringBoot 容器同时运行、无资源隔离、无分组启停、JVM 未轻量化、缺少自动管控**导致整机 CPU 长期 99\.71% 故障做全链路修复，从**环境隔离、分组编排、容器硬限制、JVM 轻量化、自动化资源调度、监控告警**六大维度彻底杜绝同类问题复发，同时完全兼容一期全业务模块端口、域名、路由规范。

# 一、故障根源复盘（本次 CPU 满载核心缺陷）

## 1\. 现象汇总

1. 整机 CPU：75%→97%→99\.71% 持续恶化；内存 34\.44%、磁盘 17\.31% 正常；

2. 负载来源：17 个外部 SpringBoot 容器（端口 8081、8100\-8115），单服务 CPU 5%\~8% 叠加占满核心；

3. 排除项：Trae IDE 进程 CPU 均＜2%，IDE 无性能问题；

4. 历史：连续 3 次同类 CPU 过载故障，属于部署架构先天缺陷重复触发。

## 2\. 五大底层架构缺陷

1. **无分组按需启动机制**：原 Compose 无 Profile 分组，执行`up`一次性拉起全部 17 个业务服务，开发仅需 2\~5 个却全部常驻；

2. **容器无 CPU / 内存硬隔离**：所有 Java 容器未配置`deploy.resources.limits`，单服务可无限抢占宿主机算力，无上限约束；

3. **JVM 参数开发 / 生产未分离**：Java 进程默认读取宿主机全部 CPU 核心、堆内存，JIT、GC、异步线程池无上限消耗 CPU；

4. **静态部署无弹性休眠**：仅支持手动启停，闲置服务长期后台运行，无自动释放资源策略；

5. **缺少自动化监控清理体系**：无定时检测脚本，高负载、闲置容器无法自动告警 / 停机，负载持续累积。

# 二、优化整体设计原则（新增资源管控约束）

在原有 7 条部署原则基础上新增 4 条资源管控强制规范：

1. **服务分组隔离**：所有业务服务按业务域划分 Profile，禁止一次性全量启动；

2. **容器资源强制配额**：每个 Java 容器必须配置 CPU / 内存上下限，不允许无限制占用宿主机；

3. **JVM 环境两套隔离**：开发环境轻量化 JVM 参数独立`.env.dev`，生产环境独立`.env.prod`，严格区分；

4. **闲置自动回收**：接入定时调度脚本，30 分钟无访问流量服务自动优雅停机，释放 CPU；

5. **负载阈值告警**：整机 CPU＞85% 触发日志告警，输出占用 TOP 容器，及时干预；

6. **启动依赖收敛**：中间件统一`infra`分组，业务模块按需叠加，避免冗余中间件常驻。

# 三、前置环境彻底重置（不变，补充容器残留深度清理）

## 3\.1 全量备份

数据库、AI 版型素材、区块链证书、Nacos 配置、全部 yml/\.env、SSL 证书异地归档；

## 3\.2 卸载 1Panel \+ 清理全部旧容器残留

```bash
1pctl uninstall
rm -rf /opt/1panel /etc/1panel /var/lib/1panel /usr/local/bin/1pctl
# 深度清理全部容器、镜像、网络、数据卷，消除旧端口冲突
docker stop $(docker ps -a -q)
docker system prune -a --volumes -f
docker network prune -f
systemctl restart docker
# 配置Docker镜像加速
cat > /etc/docker/daemon.json <<EOF
{
  "registry-mirrors": ["https://docker.mirrors.ustc.edu.cn"],
  "log-driver": "json-file",
  "log-opts": {"max-size":"10m","max-file":"3"}
}
EOF
systemctl daemon-reload && systemctl restart docker
```

## 3\.3 基础软件安装

Docker Compose v2、原生 Nginx、JDK17、Maven、Node、FFmpeg、crontab、Prometheus、Grafana、Netdata（轻量整机监控）

# 四、统一目录结构（新增资源管控脚本目录）

根目录：`/data/tailor-is-prod`

```Plain Text
/data/tailor-is-prod
├── env/                     # 环境变量分离（dev/prod两套JVM配置）
│   ├── .env.base
│   ├── .env.core
│   ├── .env.light
│   ├── .env.dev     # 开发轻量化JVM参数（核心优化点）
│   └── .env.nginx
├── middleware/               # 中间件统一编排（infra专属profile）
├── core-services/            # 核心微服务（交易/AI/区块链/3D/商户）
│   ├── compose/
│   ├── dockerfiles/
│   └── logs/
├── light-services/           # 轻量单体（社区/学堂/知识库/数据/供应链）
├── frontend/
├── ai-resource/
├── chain-storage/
├── gateway/                  # Nginx路由（沿用原有域名/端口规范不变）
├── static-cdn/
└── script/                   # 【新增资源管控自动化脚本】
    ├── dev-start.sh          # 分组按需启动脚本
    ├── dev-stop-all.sh       # 一键关停全部业务服务
    ├── idle-auto-stop.sh     # 闲置容器自动停机脚本
    ├── resource-monitor.sh   # CPU/内存负载监控告警
    └── clean-docker-cache.sh # 定时清理镜像、缓存
```

# 五、Docker 三层隔离网络（保持原有不变）

```bash
docker network create tailor-mid-net
docker network create tailor-core-net
docker network create tailor-light-net
```

1. tailor\-mid\-net：Nacos/Redis/MQ/MySQL/TiDB 中间件

2. tailor\-core\-net：核心付费微服务

3. tailor\-light\-net：免费轻量单体服务

4. Nginx 网关同时接入三张网络作为唯一流量出口

# 六、端口与域名路由规范（完全沿用原有方案，无改动）

## 6\.1 端口分段固化（40000 中间件 / 30000 核心微 / 31000 轻量 / 443 网关 / 41000 监控）

## 6\.2 6 套业务域名分层（shop/design/admin/forum/edu/data）

## 6\.3 Nginx 路由 API 前缀分层 `/api/core`核心高优 /api/light 普通限流

## 6\.4 静态资源、回调特殊接口放行规则完全不变

> 说明：路由、域名、端口业务层逻辑不做修改，仅在**部署编排、容器资源、JVM、自动化调度**层面做优化，业务无侵入。
> 
> 

# 七、核心优化一：Docker Compose Profile 分组按需启动（根治全量拉起）

## 7\.1 服务 Profile 分组规划（匹配 17 个微服务，5 大业务分组 \+ 基础设施分组）

|Profile 分组|包含服务清单|适用开发场景|
|---|---|---|
|infra|Nacos、Redis 集群 / 单机、RabbitMQ、RocketMQ、MySQL、TiDB|所有开发必启动，基础中间件|
|core\-trade|gateway\-core、service\-trade、service\-merchant|商城、订单、支付、商家后台开发|
|core\-ai\-chain|service\-ai\-pattern、service\-chain\-proof、service\-3d\-render|AI 制版、3D 试衣、区块链版权开发|
|light\-forum|service\-community|社区、线上沙龙、供需板块调试|
|light\-edu|service\-school、service\-knowledge|学堂、课程、问答知识库|
|light\-data\-supply|service\-data\-analytics、service\-supply|行业数据报表、面料供应链模块|

规则：**所有业务服务必须绑定对应 Profile，无全局默认启动服务**，不指定分组则不会启动任何 Java 容器。

## 7\.2 Compose 标准配置模板（强制添加资源限制 \+ profile \+ 开发 JVM 环境变量）

以`service-trade`交易服务示例，所有 17 个微服务统一套用该模板：

```yaml
version: "3.8"
services:
  service-trade:
    image: tailor/service-trade:dev
    container_name: tailor-trade
    profiles: ["core-trade"] # 绑定分组
    env_file:
      - ../../env/.env.base
      - ../../env/.env.core
      - ../../env/.env.dev # 开发轻量化JVM参数注入
    ports:
      - "30001:30001"
    networks:
      - tailor-core-net
      - tailor-mid-net
    # 强制CPU/内存硬限制（核心优化，杜绝无限抢占）
    deploy:
      resources:
        limits:
          cpus: "0.8"    # 核心服务最高0.8核
          memory: "384M"
        reservations:
          cpus: "0.2"
          memory: "128M"
    healthcheck:
      test: ["CMD","curl","-f","http://127.0.0.1:30001/actuator/health"]
      interval: 30s
      timeout: 5s
      retries: 3
    depends_on:
      nacos:
        condition: service_healthy
networks:
  tailor-core-net:
  tailor-mid-net:
```

### 资源配额统一标准（全 17 个 Java 容器强制区分）

1. 核心微服务（交易 / AI/3D / 区块链 / 商户）：`cpus:0.8` 内存 384M

2. 轻量单体（社区 / 学堂 / 知识库 / 数据 / 供应链）：`cpus:0.5` 内存 256M

## 7\.3 标准化启停命令（脚本封装，禁止手动敲原生 compose 命令）

### 示例 1：仅开发订单商城（最常用场景，仅启动 5 个服务）

```bash
bash /data/tailor-is-prod/script/dev-start.sh infra core-trade
```

### 示例 2：开发 AI 制版 \+ 区块链

```bash
bash /data/tailor-is-prod/script/dev-start.sh infra core-ai-chain
```

### 示例 3：同时调试社区 \+ 学堂

```bash
bash /data/tailor-is-prod/script/dev-start.sh infra light-forum light-edu
```

### 示例 4：一键关停所有业务容器（保留中间件）

```bash
bash /data/tailor-is-prod/script/dev-stop-all.sh
```

# 八、核心优化二：开发环境轻量化 JVM 参数（彻底降低单服务 CPU 消耗）

文件路径：`/data/tailor-is-prod/env/.env.dev`，全局注入所有 Java 容器，限制 JVM 线程、CPU、堆内存上限

```env
# 适配Docker cgroup识别容器资源限制
JAVA_OPTS_DEV=-XX:+UseContainerSupport \
# 强制JVM仅识别容器分配CPU，不读取宿主机全核心
-XX:ActiveProcessorCount=2 \
# 缩小堆内存，降低GC频率
-Xms128m -Xmx256m \
# 限制元空间，防止元内存泄漏持续占用CPU
-XX:MaxMetaspaceSize=128m \
# 简化JIT编译，开发环境关闭多层编译减少CPU
-XX:-TieredCompilation \
# GC线程固定2条，不自动扩容
-XX:ParallelGCThreads=2 \
# 限制直接内存
-XX:MaxDirectMemorySize=64M \
# Spring内置Tomcat线程池压缩，减少空闲线程消耗
-Dserver.tomcat.threads.max=60 \
-Dspring.profiles.active=dev
```

关键优化点：`-XX:ActiveProcessorCount=2` 强制 JVM 最多使用 2 核，避免单服务创建数十条后台线程持续跑满 CPU。

# 九、核心优化三：自动化资源管控脚本（自动告警 \+ 闲置停机）

统一存放路径：`/data/tailor-is-prod/script/`，配置 crontab 定时执行，实现无人值守资源回收。

## 9\.1 脚本 1：\[idle\-auto\-stop\.sh\]\(idle\-auto\-stop\.sh\) 闲置 30 分钟自动停机

逻辑：调用容器健康接口，30 分钟无 200 响应流量则优雅 stop 容器，释放 CPU / 内存，重启需手动执行启动脚本。

## 9\.2 脚本 2：\[resource\-monitor\.sh\]\(resource\-monitor\.sh\) 整机 CPU 负载告警

逻辑：每小时检测整机 CPU，超过 85% 输出 TOP5 占用 Java 容器日志，写入`/data/tailor-is-prod/gateway/access-logs/cpu-alert.log`。

## 9\.3 脚本 3：\[clean\-docker\-cache\.sh\]\(clean\-docker\-cache\.sh\) 每日凌晨清理

清理停止容器、无用镜像、AI 临时版型缓存、过期日志，避免磁盘与镜像堆积间接拉高 CPU。

## 9\.4 crontab 定时任务配置

```cron
# 每30分钟检测闲置服务自动停机
*/30 * * * * /data/tailor-is-prod/script/idle-auto-stop.sh >> /data/tailor-is-prod/gateway/access-logs/idle-clean.log
# 每小时整机CPU监控告警
0 * * * * /data/tailor-is-prod/script/resource-monitor.sh
# 每日凌晨2点清理Docker缓存与临时资源
0 2 * * * /data/tailor-is-prod/script/clean-docker-cache.sh
```

# 十、分阶段落地实施计划（分紧急 / 短期 / 长效三层）

## 阶段 1：即时应急处置（当天完成，解决当前 99\.71% CPU）

1. 执行环境全清理，停止全部 17 个 Java 容器；

2. 仅保留当前开发所需分组（最多 5 个服务）启动；

3. 临时通过`docker update`给运行容器绑定 CPU / 内存上限；

4. 临时注入轻量化 JVM 参数重启服务，整机负载立刻下降 60% 以上。

## 阶段 2：架构重构改造（3 天内，根治全量启动缺陷）

1. 重构全部 core/light 目录 docker\-compose\.yml，增加 profile 分组、deploy 资源限制；

2. 新建`.env.dev`开发 JVM 轻量化配置，统一注入所有微服务；

3. 编写全套自动化启停、闲置停机、监控脚本；

4. 配置 crontab 定时调度任务。

## 阶段 3：长效监控与弹性升级（7 天内，长期稳定保障）

1. 部署 Netdata 整机轻量监控面板，可视化单容器 CPU 占用；

2. Prometheus\+Grafana 接入容器资源指标，绘制负载曲线；

3. 可选升级：单机 Docker Swarm/K3s 轻量 K8s，支持服务 0 副本弹性缩容；

4. 全链路多轮压测，验证多分组并发、闲置自动回收效果。

# 十一、风险规避配套规则

1. **中间件依赖缺失风险**
所有业务 Profile 必须搭配`infra`分组启动，脚本内置强制校验，不启动中间件则拒绝拉起业务服务，避免 Nacos / 数据库连接失败。

2. **JVM 内存过小 OOM 风险**
AI 制版服务单独放宽内存至 384M，其余轻量服务 256M，兼顾轻量化与业务运行稳定。

3. **闲置停机丢失调试数据**
Redis 开启 RDB 持久化、AI 版型素材挂载宿主机目录，容器停机数据不丢失，重启自动加载。

4. **CPU 限制导致接口卡顿**
核心交易 / AI 服务 CPU 上限 0\.8 核高于社区 0\.5 核，保障付费核心业务调试流畅，免费非核心服务适当约束算力。

5. **多分组同时启动过载**
脚本增加最大并发服务数量校验，单次最多同时启动 8 个 Java 容器，超过则提示分批启动，防止再次叠加跑满 CPU。

# 十二、优化后核心收益（针对本次 CPU 故障）

1. **杜绝一次性拉起 17 个服务**：按需分组启动，常规开发仅运行 3\~5 个 Java 容器，整机 CPU 负载稳定 30% 以内；

2. **单容器算力硬隔离**：每个服务 CPU 上限 0\.5\~0\.8 核，单个服务异常不会抢占整机全部资源；

3. **JVM 资源消耗大幅降低**：限制 JVM 识别 CPU 核心、压缩堆内存、减少 GC/JIT 开销，单服务 CPU 占用从 5%\~8% 下降至 1%\~3%；

4. **闲置资源自动释放**：30 分钟无访问服务自动停机，后台无无效进程持续消耗算力；

5. **负载提前预警**：CPU 超过 85% 自动输出告警日志，提前介入，不再出现 75%→97%→99\.71% 持续恶化；

6. **完全兼容原有业务规范**：域名、端口、Nginx 路由、一期全部业务模块无修改，仅优化部署底层，业务零改动；

7. **运维标准化可复制**：整套 Profile、脚本、资源限制配置可迁移至测试 / 生产环境，本地与生产配置逻辑统一。

# 十三、与原部署方案对比差异清单

|优化维度|旧部署缺陷|本次全新优化方案|
|---|---|---|
|服务启动|无分组，docker compose up 全量 17 个服务拉起|Profile 业务分组，仅手动指定分组才启动，无全局自动拉起|
|容器资源|无 CPU / 内存限制，进程无限抢占算力|所有 Java 容器强制配置 limits，核心 0\.8 核、轻量 0\.5 核|
|JVM 配置|开发生产共用一套，无 CPU / 堆限制|独立\.env\.dev 轻量化参数，限制 JVM 识别核心、压缩堆内存|
|闲置回收|无自动机制，服务常驻后台|crontab 定时脚本，30 分钟无访问自动停机释放资源|
|负载监控|无整机 CPU 告警，故障持续恶化|每小时负载检测，CPU＞85% 输出 TOP 容器告警日志|
|启停操作|手动输入复杂 compose 命令，易误操作|统一封装 shell 脚本，参数化指定业务分组，降低操作失误|
|资源清理|人工手动清理镜像 / 缓存|每日凌晨自动清理 Docker 冗余资源，无堆积隐性负载|

> （注：部分内容可能由 AI 生成）
