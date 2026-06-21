# =============================================================
# [DEPRECATED] Tailor IS 用户服务 Dockerfile
# 此模块已被 microservice 版本替代: tailor-is/tailor-is-user/
# 生产部署请使用 docker-compose.prod.yml 中的 user-service
# 本文件保留仅用于独立测试目的
# =============================================================
# - 基础镜像: openjdk:17-slim（生产环境轻量 & JDK 版本与 pom.xml 对齐）
# - 构建产物: 由 Maven 生成的 spring-boot fat jar，路径 ./target/tailor-is-user.jar
# - 健康检查: 通过 Spring Actuator /actuator/health
# =============================================================

FROM openjdk:17-slim

LABEL maintainer="Tailor IS Team"
LABEL description="Tailor IS 用户服务 - 登录/认证 + Prometheus 业务指标"
LABEL version="1.0.0"

WORKDIR /app

# 基础镜像默认无 curl，加上便于健康检查/调试
RUN apt-get update -y \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# 时区 -> Asia/Shanghai
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 业务运行参数（生产环境可被 docker-compose 覆盖）
ENV JVM_XMX=512m
ENV JVM_XMS=256m
ENV SERVER_PORT=18080
ENV SPRING_PROFILES_ACTIVE=prod
ENV MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=*
ENV MANAGEMENT_ENDPOINT_PROMETHEUS_ENABLED=true

# 服务暴露（http/actuator/prometheus 共用同一端口）
EXPOSE 18080

# 构建产物复制（COPY 比 ADD 语义更清晰）
COPY ./target/tailor-is-user.jar /app/tailor-is-user.jar

# 非 root 运行（openjdk:17-slim 默认用户 root；为安全起见创建应用用户）
RUN groupadd --system tailor \
    && useradd --system -g tailor tailor \
    && chown -R tailor:tailor /app
USER tailor

ENTRYPOINT [ "sh", "-c", "java \
    -Duser.timezone=${TZ} \
    -Dserver.port=${SERVER_PORT} \
    -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} \
    -Dmanagement.endpoints.web.exposure.include=${MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE} \
    -Dmanagement.endpoint.prometheus.enabled=${MANAGEMENT_ENDPOINT_PROMETHEUS_ENABLED} \
    -Xmx${JVM_XMX} -Xms${JVM_XMS} \
    -jar /app/tailor-is-user.jar" ]

# 健康检查：Spring Actuator。超时 5s，间隔 15s，启动宽容期 30s，连续失败 3 次标记 unhealthy
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -fsS http://127.0.0.1:18080/actuator/health || exit 1
