# ============================================================
# Tailor IS 商户服务 (merchant) Dockerfile
# - 端口: 8110
# - 多阶段构建：Maven 编译 + JRE 运行
# ============================================================

# -------- 阶段 1: 构建
FROM maven:3.9.6-eclipse-temurin-17 AS builder
LABEL maintainer="Tailor IS Team"
WORKDIR /build

# 配置阿里云 Maven 镜像加速
COPY settings.xml /usr/share/maven/conf/settings.xml

# 先复制 pom.xml，利用 Docker 缓存依赖
# 复制所有模块的 pom.xml（父 POM 引用所有模块）
COPY pom.xml /build/pom.xml
COPY tailor-is-common/pom.xml /build/tailor-is-common/pom.xml
COPY tailor-is-common-web/pom.xml /build/tailor-is-common-web/pom.xml
COPY tailor-is-api/pom.xml /build/tailor-is-api/pom.xml
COPY tailor-is-core-gateway/pom.xml /build/tailor-is-core-gateway/pom.xml
COPY tailor-is-lite-gateway/pom.xml /build/tailor-is-lite-gateway/pom.xml
COPY tailor-is-user/pom.xml /build/tailor-is-user/pom.xml
COPY tailor-is-merchant/pom.xml /build/tailor-is-merchant/pom.xml
COPY tailor-is-product/pom.xml /build/tailor-is-product/pom.xml
COPY tailor-is-order/pom.xml /build/tailor-is-order/pom.xml
COPY tailor-is-payment/pom.xml /build/tailor-is-payment/pom.xml
COPY tailor-is-marketing/pom.xml /build/tailor-is-marketing/pom.xml
COPY tailor-is-ai/pom.xml /build/tailor-is-ai/pom.xml
COPY tailor-is-copyright/pom.xml /build/tailor-is-copyright/pom.xml
COPY tailor-is-community/pom.xml /build/tailor-is-community/pom.xml
COPY tailor-is-supply/pom.xml /build/tailor-is-supply/pom.xml
COPY tailor-is-message/pom.xml /build/tailor-is-message/pom.xml
COPY tailor-is-message-im/pom.xml /build/tailor-is-message-im/pom.xml
COPY tailor-is-academy/pom.xml /build/tailor-is-academy/pom.xml
COPY tailor-is-analytics/pom.xml /build/tailor-is-analytics/pom.xml
COPY tailor-is-pattern/pom.xml /build/tailor-is-pattern/pom.xml
COPY tailor-is-admin/pom.xml /build/tailor-is-admin/pom.xml

# 复制源码
# 复制源码（仅复制依赖模块和当前模块）
COPY tailor-is-common/src /build/tailor-is-common/src
COPY tailor-is-common-web/src /build/tailor-is-common-web/src
COPY tailor-is-merchant/src /build/tailor-is-merchant/src

# 编译商户服务（依赖会在编译时自动下载，避免 dependency:go-offline 卡死）
RUN mvn -f /build/pom.xml -pl tailor-is-merchant -am clean package -DskipTests -B -U
RUN ls -la /build/tailor-is-merchant/target/*.jar

# -------- 阶段 2: 运行
FROM openjdk:17-slim
LABEL maintainer="Tailor IS Team"
LABEL description="Tailor IS 商户服务"
LABEL version="1.0.0"

WORKDIR /app

# 基础镜像默认无 curl，加上便于健康检查/调试
RUN apt-get update -y \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 运行参数
ENV JVM_XMX=1024m
ENV JVM_XMS=512m
ENV SERVER_PORT=8110
ENV SPRING_PROFILES_ACTIVE=prod
ENV MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=*
ENV MANAGEMENT_ENDPOINT_PROMETHEUS_ENABLED=true

EXPOSE 8110

# 从构建阶段复制 JAR
COPY --from=builder /build/tailor-is-merchant/target/*.jar /app/app.jar

# 非 root 运行
RUN groupadd --system tailor \
    && useradd --system -g tailor tailor \
    && chown -R tailor:tailor /app
USER tailor

ENTRYPOINT [ "sh", "-c", "java \
    -Duser.timezone=${TZ} \
    -Dserver.port=${SERVER_PORT} \
    -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} \
    -Dcsp.sentinel.log.dir=/tmp/sentinel \
    -Dmanagement.endpoints.web.exposure.include=${MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE} \
    -Dmanagement.endpoint.prometheus.enabled=${MANAGEMENT_ENDPOINT_PROMETHEUS_ENABLED} \
    -Xmx${JVM_XMX} -Xms${JVM_XMS} \
    -jar /app/app.jar" ]

# 健康检查
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS http://127.0.0.1:8110/actuator/health || exit 1
