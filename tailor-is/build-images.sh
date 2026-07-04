#!/bin/bash
# Tailor IS - Build all Docker images from pre-built JARs
set -e

cd /home/tailor/Tailoris/tailor-is

# Service definitions: module_name:port
SERVICES=(
  "tailor-is-lite-gateway:8081"
  "tailor-is-user:8101"
  "tailor-is-product:8102"
  "tailor-is-order:8103"
  "tailor-is-payment:8104"
  "tailor-is-marketing:8105"
  "tailor-is-ai:8106"
  "tailor-is-copyright:8107"
  "tailor-is-community:8108"
  "tailor-is-supply:8109"
  "tailor-is-admin:8100"
  "tailor-is-merchant:8110"
  "tailor-is-message:8111"
  "tailor-is-message-im:8114"
  "tailor-is-academy:8112"
  "tailor-is-analytics:8113"
  "tailor-is-pattern:8115"
)

for svc_def in "${SERVICES[@]}"; do
  MODULE="${svc_def%%:*}"
  PORT="${svc_def##*:}"
  IMAGE_NAME="${MODULE}:latest"
  JAR_FILE="${MODULE}/target/${MODULE}-1.0.0.jar"

  echo "=== Building ${IMAGE_NAME} (port ${PORT}) ==="

  # Create temp Dockerfile
  cat > /tmp/Dockerfile.${MODULE} << DOCKEREOF
# 🔒 H-06修复: 统一使用 eclipse-temurin:17-jre-jammy
FROM eclipse-temurin:17-jre-jammy
LABEL maintainer="Tailor IS Team"
LABEL description="Tailor IS ${MODULE}"

WORKDIR /app

RUN apt-get update -y \\
    && apt-get install -y --no-install-recommends curl ca-certificates \\
    && rm -rf /var/lib/apt/lists/*

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/\$TZ /etc/localtime && echo \$TZ > /etc/timezone

ENV SERVER_PORT=${PORT}
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC"

EXPOSE ${PORT}

COPY ${JAR_FILE} /app/app.jar

RUN groupadd --system tailor \\
    && useradd --system -g tailor tailor \\
    && chown -R tailor:tailor /app
USER tailor

ENTRYPOINT [ "sh", "-c", "java \$JAVA_OPTS -Duser.timezone=\${TZ} -Dserver.port=\${SERVER_PORT} -Dspring.profiles.active=\${SPRING_PROFILES_ACTIVE} -jar /app/app.jar" ]

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \\
    CMD curl -fsS http://127.0.0.1:${PORT}/actuator/health || exit 1
DOCKEREOF

  sudo docker build -t "${IMAGE_NAME}" -f "/tmp/Dockerfile.${MODULE}" .
  rm -f "/tmp/Dockerfile.${MODULE}"
  echo "=== ${IMAGE_NAME} built successfully ==="
done

echo ""
echo "=== All images built successfully! ==="
sudo docker images | grep tailor-is