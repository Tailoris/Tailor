#!/bin/bash

set -e

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)
PROJECT_ROOT=$(dirname "$(dirname "$SCRIPT_DIR")")

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

log "Starting Tailor IS services..."

cd "$PROJECT_ROOT"

if [ ! -f .env ]; then
    log "Error: .env file not found. Please create it from .env.example"
    exit 1
fi

log "Starting infrastructure services (MySQL, Redis, RabbitMQ)..."
docker compose up -d mysql redis rabbitmq

log "Waiting for MySQL to be ready..."
MAX_WAIT=60
COUNT=0
MYSQL_PWD=$(grep MYSQL_ROOT_PASSWORD .env | cut -d'=' -f2)
while ! docker exec 1Panel-mysql-FAZu mysqladmin ping -h localhost -uroot -p"$MYSQL_PWD" --silent 2>/dev/null; do
    sleep 2
    COUNT=$((COUNT + 2))
    if [ $COUNT -ge $MAX_WAIT ]; then
        log "Error: MySQL not ready after $MAX_WAIT seconds"
        exit 1
    fi
    log "Waiting for MySQL... $COUNT/$MAX_WAIT"
done
log "MySQL is ready"

log "Waiting for Redis to be ready..."
REDIS_PWD=$(grep REDIS_PASSWORD .env | cut -d'=' -f2)
while ! docker exec tailor-is-redis redis-cli -a "$REDIS_PWD" ping 2>/dev/null | grep -q PONG; do
    sleep 2
done
log "Redis is ready"

log "Starting core microservices..."
docker compose up -d user-service product-service order-service payment-service

log "Waiting for user-service to be ready..."
while ! curl -s http://localhost:8101/api/auth/login -X POST -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}' 2>/dev/null | grep -q "success"; do
    sleep 3
done
log "User service is ready"

log "Starting core-gateway..."
docker compose up -d core-gateway

log "Waiting for gateway to be ready..."
while ! curl -s http://localhost:9000/api/auth/login -X POST -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}' 2>/dev/null | grep -q "success"; do
    sleep 3
done
log "Gateway is ready"

log "Starting remaining services..."
docker compose up -d marketing-service ai-service copyright-service community-service
docker compose up -d supply-service academy-service message-service merchant-service
docker compose up -d admin-service pattern-service analytics-service message-im-service

log "Starting frontend..."
cd "$PROJECT_ROOT/tailor-is-frontend/pc-mall"
nohup npm run dev > /tmp/tailoris-frontend.log 2>&1 &
FRONTEND_PID=$!
echo "$FRONTEND_PID" > /tmp/tailoris-frontend.pid
log "Frontend started in background (PID: $FRONTEND_PID)"

log "Waiting for frontend to be ready..."
sleep 10
while ! curl -s http://localhost:3001/ 2>/dev/null | grep -q "<!DOCTYPE html>"; do
    sleep 3
done
log "Frontend is ready"

log ""
log "Tailor IS services started successfully!"
log ""
log "Access URLs:"
log "  - PC Mall: http://localhost:3001/"
log "  - Gateway API: http://localhost:9000/api/"
log "  - User Service: http://localhost:8101/api/"
log ""
log "Health check:"
"$PROJECT_ROOT/deploy/scripts/health-check.sh"