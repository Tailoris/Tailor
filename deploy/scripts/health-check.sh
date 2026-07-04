#!/bin/bash

set -e

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

RESULT_FILE="/tmp/tailoris-health-check-result.log"
> "$RESULT_FILE"

PASS_COUNT=0
FAIL_COUNT=0

check() {
    local name="$1"
    local url="$2"
    local method="${3:-GET}"
    local data="${4:-}"
    
    log "Checking $name..."
    
    local response
    if [ -n "$data" ]; then
        response=$(curl -s -X "$method" -H "Content-Type: application/json" -d "$data" "$url" 2>&1)
    else
        response=$(curl -s -X "$method" "$url" 2>&1)
    fi
    
    if echo "$response" | grep -q "success\|200\|OK\|<!DOCTYPE html>"; then
        log "  ✓ $name - PASS"
        echo "✓ $name - PASS" >> "$RESULT_FILE"
        PASS_COUNT=$((PASS_COUNT + 1))
    else
        log "  ✗ $name - FAIL"
        echo "✗ $name - FAIL" >> "$RESULT_FILE"
        echo "    Response: $response" >> "$RESULT_FILE"
        FAIL_COUNT=$((FAIL_COUNT + 1))
    fi
}

check_docker() {
    local name="$1"
    local container="$2"
    
    log "Checking $name..."
    
    if docker ps --filter "name=$container" --format "{{.Status}}" | grep -q "Up"; then
        log "  ✓ $name - PASS"
        echo "✓ $name - PASS" >> "$RESULT_FILE"
        PASS_COUNT=$((PASS_COUNT + 1))
    else
        log "  ✗ $name - FAIL"
        echo "✗ $name - FAIL" >> "$RESULT_FILE"
        FAIL_COUNT=$((FAIL_COUNT + 1))
    fi
}

log "Starting Tailor IS health check..."

check "Frontend - PC Mall" "http://localhost:3001/"
check "Gateway - Login API" "http://localhost:9000/api/auth/login" "POST" '{"username":"admin","password":"admin123"}'
check "User Service - Login API" "http://localhost:8101/api/auth/login" "POST" '{"username":"admin","password":"admin123"}'
check_docker "MySQL Container" "tailor-is-mysql"
check_docker "Redis Container" "tailor-is-redis"
check_docker "Core Gateway Container" "tailor-is-core-gateway"
check_docker "User Service Container" "tailor-is-user"

log "Health check completed:"
log "  Passed: $PASS_COUNT"
log "  Failed: $FAIL_COUNT"

if [ $FAIL_COUNT -gt 0 ]; then
    log "Errors detected! Check $RESULT_FILE for details."
    exit 1
else
    log "All checks passed! Tailor IS is running normally."
    exit 0
fi