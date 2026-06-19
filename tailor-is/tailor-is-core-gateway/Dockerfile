# ============================================================
# Tailor IS 核心网关 (core-gateway) - 精简版 Dockerfile
# 前置条件: 已在 tailor-is 目录执行 `mvn clean package -DskipTests`
# 生成: tailor-is-core-gateway/target/tailor-is-core-gateway-1.0.0.jar
# ============================================================

FROM openjdk:17-slim
LABEL maintainer="Tailor IS Team"
LABEL description="Tailor IS 核心网关 - 路由/限流/鉴权"
LABEL version="1.0.0"

WORKDIR /app

# 基础镜像默认无 curl, 添加便于健康检查/调试
RUN apt-get update -y \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 运行参数
ENV SERVER_PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xmx1024m -Xms512m -XX:+UseG1GC"

EXPOSE 8080

# 从构建上下文复制 JAR (构建上下文: ../tailor-is)
COPY tailor-is-core-gateway/target/*.jar /app/app.jar

# 非 root 运行
RUN groupadd --system tailor \
    && useradd --system -g tailor tailor \
    && chown -R tailor:tailor /app
USER tailor

ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Duser.timezone=${TZ} -Dserver.port=${SERVER_PORT} -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} -jar /app/app.jar" ]

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS http://127.0.0.1:8080/actuator/health || exit 1
