# ============================================================
# Tailor IS 多阶段构建 Dockerfile
# 修复 L-012: 使用多阶段构建减小镜像体积
# 🔒 H-06修复: 统一使用 JDK 17（与 Spring Boot 3.x 兼容）
# ============================================================

# 构建参数：指定要构建的模块
ARG MODULE=tailor-is-core-gateway

# ── Stage 1: 构建阶段 ──────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# 复制父 POM 和所有子模块 POM（利用 Docker 缓存层）
COPY pom.xml .
COPY tailor-is-common/pom.xml tailor-is-common/
COPY tailor-is-common-web/pom.xml tailor-is-common-web/
COPY tailor-is-api/pom.xml tailor-is-api/
COPY tailor-is-core-gateway/pom.xml tailor-is-core-gateway/
COPY tailor-is-lite-gateway/pom.xml tailor-is-lite-gateway/
COPY tailor-is-user/pom.xml tailor-is-user/
COPY tailor-is-merchant/pom.xml tailor-is-merchant/
COPY tailor-is-product/pom.xml tailor-is-product/
COPY tailor-is-order/pom.xml tailor-is-order/
COPY tailor-is-payment/pom.xml tailor-is-payment/
COPY tailor-is-marketing/pom.xml tailor-is-marketing/
COPY tailor-is-ai/pom.xml tailor-is-ai/
COPY tailor-is-copyright/pom.xml tailor-is-copyright/
COPY tailor-is-community/pom.xml tailor-is-community/
COPY tailor-is-supply/pom.xml tailor-is-supply/
COPY tailor-is-message/pom.xml tailor-is-message/
COPY tailor-is-message-im/pom.xml tailor-is-message-im/
COPY tailor-is-academy/pom.xml tailor-is-academy/
COPY tailor-is-analytics/pom.xml tailor-is-analytics/
COPY tailor-is-pattern/pom.xml tailor-is-pattern/
COPY tailor-is-admin/pom.xml tailor-is-admin/

# 下载依赖（利用 Docker 缓存层）

# 复制源代码并构建指定模块
COPY . .
ARG MODULE
RUN mvn clean package -pl ${MODULE} -am -Dmaven.test.skip=true -B

# ── Stage 2: 运行阶段 ──────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy

# 重新声明构建参数（Stage 2 需要重新声明）
ARG MODULE=tailor-is-core-gateway

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 创建非root用户（Debian/Ubuntu 风格）
RUN groupadd --system --gid 1001 appgroup && \
    useradd --system --uid 1001 --gid appgroup appuser

WORKDIR /app

# 从构建阶段复制指定模块的jar包
COPY --from=builder /app/${MODULE}/target/*.jar app.jar

# 创建日志目录（Sentinel需要）
RUN mkdir -p /home/appuser/logs/csp && \
    chown -R appuser:appgroup /app /home/appuser/logs

# 切换到非root用户
USER appuser

# 暴露端口（由环境变量覆盖）
EXPOSE ${SERVER_PORT:-8080}

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${SERVER_PORT:-8080}/actuator/health || exit 1

# JVM 参数优化
ENV JAVA_OPTS="-Xms256m -Xmx512m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/tmp/heapdump.hprof \
  -Djava.security.egd=file:/dev/./urandom \
  -Dfile.encoding=UTF-8 \
  -Dsun.jnu.encoding=UTF-8"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
