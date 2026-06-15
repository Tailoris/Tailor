# =============================================================
# Alert Webhook 中继服务 Dockerfile
# - 基础镜像: python:3.11-slim
# - 脚本路径: ./server.py（从 deploy/alert-webhook 目录复制）
# - 接收 Prometheus/Alertmanager 的告警请求，转发到:
#     · 钉钉机器人 / 飞书机器人 / 企业微信
#     · 邮件 (Resend API)
# =============================================================

FROM python:3.11-slim

LABEL maintainer="Tailor IS Team"
LABEL description="Alert Webhook 中继 - 钉钉/飞书/企业微信/Resend 邮件"
LABEL version="1.0.0"

WORKDIR /app

# 基础镜像默认无 curl，加上便于健康检查
RUN apt-get update -y \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 监听端口
ENV PORT=8080
EXPOSE 8080

# 业务脚本（原生标准库，无需 pip 安装）
COPY ./server.py /app/server.py

# 以非 root 运行
RUN groupadd --system alert \
    && useradd --system -g alert alert \
    && chown -R alert:alert /app
USER alert

HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
    CMD curl -fsS http://127.0.0.1:8080/health || exit 1

ENTRYPOINT [ "python3", "/app/server.py" ]
