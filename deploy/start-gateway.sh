#!/bin/bash
cd /opt/tailor-is/jars
nohup java -Xms512m -Xmx1024m \
  -Dspring.application.name=tailor-is-gateway \
  -Dspring.cloud.nacos.discovery.server-addr=localhost:8848 \
  -Dspring.cloud.nacos.config.enabled=false \
  -Dseata.enabled=false \
  -Dspring.main.allow-circular-references=true \
  -Dspring.data.redis.host=172.18.0.2 \
  -Dspring.data.redis.port=6379 \
  -Dspring.data.redis.password=redis_RSeR4G \
  -Dspring.config.additional-location=file:./application.yml \
  -Dmanagement.endpoints.web.exposure.include=health,info,prometheus,metrics,gateway \
  -jar tailor-is-gateway-1.0.0.jar > /tmp/tailor-is-logs/gateway.log 2>&1 &
echo "Gateway started PID: $!"
sleep 2
