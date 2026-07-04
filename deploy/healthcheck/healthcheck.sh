#!/bin/bash

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

API_BASE="${API_BASE:-http://localhost}"
TIMEOUT=10
RETRIES=3

FAILED_CHECKS=()

log_info() {
  echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
  echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
  echo -e "${RED}[ERROR]${NC} $1"
  FAILED_CHECKS+=("$1")
}

check_http() {
  local url="$1"
  local name="$2"
  local expected_code="${3:-200}"
  
  log_info "Checking $name: $url"
  
  for i in $(seq 1 $RETRIES); do
    local status_code=$(curl -s -o /dev/null -w "%{http_code}" --max-time $TIMEOUT "$url" 2>/dev/null)
    
    if [ "$status_code" = "$expected_code" ]; then
      log_info "✓ $name is healthy (HTTP $status_code)"
      return 0
    fi
    
    log_warn "Attempt $i/$RETRIES: $name returned HTTP $status_code (expected $expected_code)"
    sleep 2
  done
  
  log_error "✗ $name is UNHEALTHY (HTTP $status_code)"
  return 1
}

check_json_api() {
  local url="$1"
  local name="$2"
  
  log_info "Checking $name: $url"
  
  for i in $(seq 1 $RETRIES); do
    local response=$(curl -s --max-time $TIMEOUT "$url" 2>/dev/null)
    local status_code=$(curl -s -o /dev/null -w "%{http_code}" --max-time $TIMEOUT "$url" 2>/dev/null)
    
    if [ "$status_code" = "200" ] && echo "$response" | python3 -c "import sys,json; d=json.load(sys.stdin); exit(0 if d.get('code') in (0,200) else 1)" 2>/dev/null; then
      log_info "✓ $name is healthy (HTTP 200, API code OK)"
      return 0
    fi
    
    log_warn "Attempt $i/$RETRIES: $name returned HTTP $status_code"
    sleep 2
  done
  
  log_error "✗ $name is UNHEALTHY"
  return 1
}

check_port() {
  local port="$1"
  local name="$2"
  
  log_info "Checking $name port $port"
  
  if ss -tlnp | grep -q ":$port"; then
    log_info "✓ $name port $port is open"
    return 0
  else
    log_error "✗ $name port $port is NOT open"
    return 1
  fi
}

check_docker_container() {
  local container="$1"
  local name="$2"
  
  log_info "Checking $name container: $container"
  
  if docker inspect "$container" >/dev/null 2>&1 && docker ps --filter "name=$container" --filter "status=running" -q | grep -q .; then
    log_info "✓ $name container is running"
    return 0
  else
    log_error "✗ $name container is NOT running"
    return 1
  fi
}

echo -e "\n${YELLOW}=============================================${NC}"
echo -e "${YELLOW}Tailor IS 系统健康检查 (Health Check)${NC}"
echo -e "${YELLOW}=============================================${NC}\n"

log_info "开始服务健康检查..."

echo -e "\n${YELLOW}--- 容器状态检查 ---${NC}"
check_docker_container "tailor-is-core-gateway" "Core Gateway"
check_docker_container "tailor-is-product-service" "Product Service"
check_docker_container "tailor-is-community-service" "Community Service"
check_docker_container "tailor-is-user" "User Service"
check_docker_container "tailor-is-order-service" "Order Service"
check_docker_container "tailor-is-frontend" "Frontend (Nginx)"

echo -e "\n${YELLOW}--- 端口监听检查 ---${NC}"
check_port "9000" "Core Gateway"
check_port "8101" "User Service"
check_port "8102" "Product Service"
check_port "8103" "Order Service"
check_port "8108" "Community Service"
check_port "80" "Frontend Nginx"

echo -e "\n${YELLOW}--- API端点检查 ---${NC}"
check_json_api "$API_BASE/api/products/categories" "Product Categories API"
check_json_api "$API_BASE/api/products/hot" "Hot Products API"
check_json_api "$API_BASE/api/products/new" "New Products API"
check_json_api "$API_BASE/api/community/posts" "Community Posts API"
check_http "$API_BASE/api/user/info" "User Info API" 401

echo -e "\n${YELLOW}--- 网关健康检查 ---${NC}"
check_http "http://localhost:9000/actuator/health" "Core Gateway Actuator" 200
check_http "http://localhost:8081/actuator/health" "Lite Gateway Actuator" 200

echo -e "\n${YELLOW}=============================================${NC}"

if [ ${#FAILED_CHECKS[@]} -eq 0 ]; then
  echo -e "${GREEN}✅ 所有健康检查通过！系统运行正常。${NC}"
  echo -e "${YELLOW}=============================================${NC}\n"
  exit 0
else
  echo -e "${RED}❌ 发现 ${#FAILED_CHECKS[@]} 个健康检查失败：${NC}"
  for check in "${FAILED_CHECKS[@]}"; do
    echo -e "${RED}  - $check${NC}"
  done
  echo -e "${YELLOW}=============================================${NC}\n"
  exit 1
fi