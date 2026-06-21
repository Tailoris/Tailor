#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Tailor IS - 重新生成 docker-compose.prod.yml
为所有微服务补齐 Docker Compose 配置

运行:
    cd /home/tailor/Tailoris && python3 deploy/scripts/_rewrite_compose.py
"""
import os

PROJECT_ROOT = "/home/tailor/Tailoris"

# 微服务清单: (服务名, 端口, JVM 参数
MICROSERVICES = [
    # 网关层
    ("core-gateway", 8080, "2048m", "1024m", "2.0", "1536M"),
    ("lite-gateway", 8081, "512m", "256m", "1.0", "768M"),
    # 核心业务微服务（注：user-service 已由独立的 tailor-is-user 服务替代）
    ("admin-service", 8100, "1024m", "512m", "1.5", "1024M"),
    ("product-service", 8102, "1024m", "512m", "1.5", "1024M"),
    ("order-service", 8103, "1024m", "512m", "1.5", "1024M"),
    ("payment-service", 8104, "1024m", "512m", "1.5", "1024M"),
    ("marketing-service", 8105, "1024m", "512m", "1.0", "768M"),
    ("ai-service", 8106, "2048m", "1024m", "2.0", "1536M"),
    ("copyright-service", 8107, "1024m", "512m", "1.0", "768M"),
    ("community-service", 8108, "1024m", "512m", "1.0", "768M"),
    ("supply-service", 8109, "1024m", "512m", "1.0", "768M"),
    ("merchant-service", 8110, "1024m", "512m", "1.0", "768M"),
    ("message-service", 8111, "1024m", "512m", "1.0", "768M"),
    ("academy-service", 8112, "1024m", "512m", "1.0", "768M"),
    ("analytics-service", 8113, "1024m", "512m", "1.0", "768M"),
    ("message-im-service", 8114, "1024m", "512m", "1.0", "768M"),
    ("pattern-service", 8115, "1024m", "512m", "1.0", "768M"),
]

# 服务名到 tailor-is 子模块映射 (不含 "tailor-is-user" 前缀需特殊处理)
# 注意: user-service 子模块是 tailor-is-user 但在 docker-compose 中叫 user-service
SERVICE_TO_MODULE = {
    "core-gateway": "tailor-is-core-gateway",
    "lite-gateway": "tailor-is-lite-gateway",
    "admin-service": "tailor-is-admin",
    "product-service": "tailor-is-product",
    "order-service": "tailor-is-order",
    "payment-service": "tailor-is-payment",
    "marketing-service": "tailor-is-marketing",
    "ai-service": "tailor-is-ai",
    "copyright-service": "tailor-is-copyright",
    "community-service": "tailor-is-community",
    "supply-service": "tailor-is-supply",
    "merchant-service": "tailor-is-merchant",
    "message-service": "tailor-is-message",
    "academy-service": "tailor-is-academy",
    "analytics-service": "tailor-is-analytics",
    "message-im-service": "tailor-is-message-im",
    "pattern-service": "tailor-is-pattern",
}


def gen_service_block(name, port, jvm_xmx, jvm_xms, cpu_limit, mem_limit):
    """生成单个微服务的 docker-compose 配置块"""
    module = SERVICE_TO_MODULE[name]
    # 决定数据库/Redis/RabbitMQ 的默认值（根据服务类型）
    has_db = "-service" in name and name not in ["core-gateway", "lite-gateway"]
    has_rabbit = name in ["order-service", "payment-service", "community-service"]

    env_lines = [
        "      SPRING_PROFILES_ACTIVE: prod",
        "      TZ: Asia/Shanghai",
        f"      JVM_XMX: {jvm_xmx}",
        f"      JVM_XMS: {jvm_xms}",
        f"      SERVER_PORT: \"{port}\"",
        "      MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE: \"*\"",
        "      MANAGEMENT_ENDPOINT_PROMETHEUS_ENABLED: \"true\"",
        "      SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR: 127.0.0.1:8848",
    ]

    if has_db:
        db_name_map = {
            "user-service": "tailor_is_user",
            "admin-service": "tailor_is_admin",
            "product-service": "tailor_is_product",
            "order-service": "tailor_is_order",
            "payment-service": "tailor_is_payment",
            "marketing-service": "tailor_is_marketing",
            "ai-service": "tailor_is_ai",
            "copyright-service": "tailor_is_copyright",
            "community-service": "tailor_is_community",
            "supply-service": "tailor_is_supply",
            "merchant-service": "tailor_is_merchant",
            "message-service": "tailor_is_message",
            "academy-service": "tailor_is_academy",
            "analytics-service": "tailor_is_analytics",
            "message-im-service": "tailor_is_message_im",
            "pattern-service": "tailor_is_pattern",
        }
        db = db_name_map.get(name, "tailor_is")
        env_lines.extend([
            f'      SPRING_DATASOURCE_URL: jdbc:mysql://127.0.0.1:3306/{db}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8',
            "      SPRING_DATASOURCE_USERNAME: root",
            '      SPRING_DATASOURCE_PASSWORD: ${MYSQL_PASSWORD:-mysql_ZmY2sr}',
            "      SPRING_DATA_REDIS_HOST: 127.0.0.1",
            '      SPRING_DATA_REDIS_PORT: \"6379\"',
            '      SPRING_DATA_REDIS_PASSWORD: ${REDIS_PASSWORD:-redis_jD2N8n}',
        ])
        if has_rabbit:
            env_lines.extend([
                "      SPRING_RABBITMQ_HOST: 127.0.0.1",
                '      SPRING_RABBITMQ_PORT: "5672"',
                '      SPRING_RABBITMQ_USERNAME: ${RABBITMQ_USER:-rabbitmq}',
                '      SPRING_RABBITMQ_PASSWORD: ${RABBITMQ_PASSWORD:-rabbitmq}',
            ])

    # payment-service 的特殊支付渠道配置
    if name == "payment-service":
        env_lines.extend([
            '      WECHAT_PAY_APP_ID: ${WECHAT_PAY_APP_ID:-}',
            '      WECHAT_PAY_MCH_ID: ${WECHAT_PAY_MCH_ID:-}',
            '      WECHAT_PAY_API_KEY: ${WECHAT_PAY_API_KEY:-}',
            '      ALIPAY_APP_ID: ${ALIPAY_APP_ID:-}',
            '      ALIPAY_PRIVATE_KEY: ${ALIPAY_PRIVATE_KEY:-}',
            '      ALIPAY_PUBLIC_KEY: ${ALIPAY_PUBLIC_KEY:-}',
        ])

    env_block = "\n".join(env_lines)

    return f'''  {name}:
    image: tailor-is/{name}:latest
    container_name: tailor-is-{name}
    hostname: {name}
    build:
      context: ./tailor-is
      dockerfile: ./{module}/Dockerfile
    environment:
{env_block}
    ports:
      - "{port}:{port}"
    networks: [tailor-is-network, monitor-network]
    extra_hosts:
      - "host.docker.internal:host-gateway"
    <<: *restart-policy
    logging: *default-logging
    deploy:
      resources:
        limits: {{ cpus: '{cpu_limit}', memory: {mem_limit} }}
        reservations: {{ cpus: '0.2', memory: 256M }}
    healthcheck:
      test: ["CMD", "curl", "-f", "http://127.0.0.1:{port}/actuator/health"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 60s
'''


HEADER = '''# ==============================================================================
# Tailor IS 生产环境 Docker Compose 配置
# ==============================================================================
# 版本: v2.0 (微服务集群版)
# 启动所有微服务一键拉起:
#   docker compose -f docker-compose.prod.yml up -d --build
# 停止: docker compose -f docker-compose.prod.yml down
# 查看状态: docker compose -f docker-compose.prod.yml ps
# 查看日志: docker compose -f docker-compose.prod.yml logs -f
# 单独构建某服务: docker compose -f docker-compose.prod.yml build <service>
# ==============================================================================

networks:
  tailor-is-network:
    name: tailor-is-network
    driver: bridge
  monitor-network:
    name: monitor-network
    driver: bridge

volumes:
  # ============ 数据卷: 持久化存储 ============
  prometheus-data: { name: tailor-is-prometheus-data }
  grafana-data: { name: tailor-is-grafana-data }
  nginx-logs: { name: tailor-is-nginx-logs }
  ssl-certs: { name: tailor-is-ssl-certs }

x-restart-policy: &restart-policy
  restart: unless-stopped

x-logging: &default-logging
  driver: "json-file"
  options:
    max-size: "10m"
    max-file: "5"
    tag: "{{.Name}}"

x-resources: &default-resources
  limits:
    cpus: '1.0'
    memory: 1024M
  reservations:
    cpus: '0.2'
    memory: 256M

# ==============================================================================
# 基础设施层 (Infrastructure)
# ==============================================================================
services:
  prometheus:
    image: prom/prometheus:v2.54.1
    container_name: tailor-is-prometheus
    hostname: prometheus
    volumes:
      - ./deploy/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - ./deploy/prometheus/alert-rules.yml:/etc/prometheus/alert-rules.yml:ro
      - prometheus-data:/prometheus
    command:
      - --config.file=/etc/prometheus/prometheus.yml
      - --storage.tsdb.path=/prometheus
      - --storage.tsdb.retention.time=15d
      - --web.enable-lifecycle
    ports:
      - "9090:9090"
    networks: [tailor-is-network, monitor-network]
    <<: *restart-policy
    logging: *default-logging
    user: "65534:65534"
    deploy:
      resources:
        limits: { cpus: '1.0', memory: 1024M }
        reservations: { cpus: '0.1', memory: 128M }
    healthcheck:
      test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost:9090/-/healthy"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 30s

  grafana:
    image: grafana/grafana:10.4.0
    container_name: tailor-is-grafana
    hostname: grafana
    environment:
      GF_SECURITY_ADMIN_USER: ${GRAFANA_ADMIN_USER:-admin}
      GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_PASSWORD:?GRAFANA_PASSWORD is required}
      GF_USERS_ALLOW_SIGN_UP: "false"
      GF_INSTALL_PLUGINS: "grafana-piechart-panel,grafana-clock-panel"
    volumes:
      - grafana-data:/var/lib/grafana
    ports:
      - "3001:3000"
    networks: [monitor-network]
    depends_on: [prometheus]
    <<: *restart-policy
    logging: *default-logging
    deploy:
      resources:
        limits: { cpus: '1.0', memory: 512M }
        reservations: { cpus: '0.1', memory: 128M }
    healthcheck:
      test: ["CMD-SHELL", "wget --quiet --tries=1 --spider http://localhost:3000/api/health || exit 1"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 30s

# ==============================================================================
# 认证/用户独立服务 (Auth/User Service Standalone)
# ==============================================================================
  tailor-is-user:
    image: tailor-is/user:1.0.0
    container_name: tailor-is-user
    hostname: tailor-is-user
    build:
      context: ./tailor-is-user
      dockerfile: Dockerfile
    environment:
      SPRING_PROFILES_ACTIVE: prod
      TZ: Asia/Shanghai
      SERVER_PORT: "18080"
      JVM_XMX: 512m
      JVM_XMS: 256m
      MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE: "*"
      MANAGEMENT_ENDPOINT_PROMETHEUS_ENABLED: "true"
      SPRING_DATASOURCE_URL: jdbc:mysql://127.0.0.1:3306/tailoris?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: ${MYSQL_PASSWORD:-mysql_ZmY2sr}
      SPRING_DATA_REDIS_HOST: 127.0.0.1
      SPRING_DATA_REDIS_PORT: "6379"
      SPRING_DATA_REDIS_PASSWORD: ${REDIS_PASSWORD:-redis_jD2N8n}
      SPRING_RABBITMQ_HOST: 127.0.0.1
      SPRING_RABBITMQ_PORT: "5672"
      SPRING_RABBITMQ_USERNAME: ${RABBITMQ_USER:-rabbitmq}
      SPRING_RABBITMQ_PASSWORD: ${RABBITMQ_PASSWORD:-rabbitmq}
      SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR: 127.0.0.1:8848
    ports:
      - "18080:18080"
    networks: [tailor-is-network, monitor-network]
    extra_hosts:
      - "host.docker.internal:host-gateway"
    <<: *restart-policy
    logging: *default-logging
    deploy:
      resources:
        limits: { cpus: '1.0', memory: 1024M }
        reservations: { cpus: '0.2', memory: 256M }
    healthcheck:
      test: ["CMD", "curl", "-f", "http://127.0.0.1:18080/actuator/health"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 30s

# ==============================================================================
# API 网关层 (API Gateway)
# ==============================================================================
'''

# 生成所有微服务配置
ms_blocks = []
for svc in MICROSERVICES:
    name, port, jvm_xmx, jvm_xms, cpu_limit, mem_limit = svc
    ms_blocks.append(gen_service_block(name, port, jvm_xmx, jvm_xms, cpu_limit, mem_limit))

ms_section = "\n".join(ms_blocks)

FOOTER = f'''# ==============================================================================
# 告警 Webhook 中继 - 接入钉钉/飞书/企业微信/Resend 邮件
# ==============================================================================
  alert-webhook:
    image: tailor-is/alert-webhook:1.0.0
    container_name: tailor-is-alert-webhook
    hostname: alert-webhook
    build:
      context: ./deploy/alert-webhook
      dockerfile: Dockerfile
    environment:
      TZ: Asia/Shanghai
      PORT: "8080"
      DINGTALK_WEBHOOK: ${{DINGTALK_WEBHOOK:-}}
      DINGTALK_SECRET:  ${{DINGTALK_SECRET:-}}
      FEISHU_WEBHOOK:   ${{FEISHU_WEBHOOK:-}}
      WECOM_WEBHOOK:    ${{WECOM_WEBHOOK:-}}
      RESEND_API_KEY:   ${{RESEND_API_KEY:-}}
      ALERT_FROM_EMAIL: ${{ALERT_FROM_EMAIL:-"Tailor IS <alerts@tailorbot.top>"}}
      ALERT_TO_EMAIL:   ${{ALERT_TO_EMAIL:-}}
    ports:
      - "9095:8080"
    networks: [tailor-is-network, monitor-network]
    extra_hosts:
      - "host.docker.internal:host-gateway"
    <<: *restart-policy
    logging: *default-logging
    deploy:
      resources:
        limits: {{ cpus: '0.5', memory: 256M }}
        reservations: {{ cpus: '0.1', memory: 64M }}
    healthcheck:
      test: ["CMD", "curl", "-f", "http://127.0.0.1:8080/health"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 15s
'''

full_content = HEADER + ms_section + FOOTER

output_path = os.path.join(PROJECT_ROOT, "docker-compose.prod.yml")
with open(output_path, "w", encoding="utf-8") as f:
    f.write(full_content)

print(f"✓ docker-compose.prod.yml 已生成: {output_path}")
print(f"  共 {len(MICROSERVICES)} 个微服务")
for svc in MICROSERVICES:
    print(f"    - {svc[0]:<20} (端口 {svc[1]})")
print("\n下一步:")
print("  docker compose -f docker-compose.prod.yml config  # 验证配置")
print("  docker compose -f docker-compose.prod.yml build # 构建所有镜像")
print("  docker compose -f docker-compose.prod.yml up -d  # 启动整个集群")
