# ============================================================
# Tailor IS 用户服务 (user) - 精简版 Dockerfile
# 前置条件: 已在 tailor-is 目录执行 `mvn clean package -DskipTests`
# 生成: tailor-is-user/target/tailor-is-user-1.0.0.jar
# ============================================================

FROM openjdk:17-slim
LABEL maintainer="Tailor IS Team"
LABEL description="Tailor IS 用户服务"
LABEL version="1.0.0"

WORKDIR /app

RUN apt-get update -y \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

ENV SERVER_PORT=8101
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xmx1024m -Xms512m -XX:+UseG1GC"

EXPOSE 8101

COPY tailor-is-user/target/*.jar /app/app.jar

RUN groupadd --system tailor \
    && useradd --system -g tailor tailor \
    && chown -R tailor:tailor /app
USER tailor

ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Duser.timezone=${TZ} -Dserver.port=${SERVER_PORT} -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} -jar /app/app.jar" ]

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS http://127.0.0.1:8101/actuator/health || exit 1
