#!/bin/bash
exec 2>&1
echo "=== 1Panel Nacos docker-compose.yml ==="
cat /opt/1panel/apps/nacos/nacos/docker-compose.yml 2>&1
