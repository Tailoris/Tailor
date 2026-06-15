#!/bin/bash
for p in 8080 8101 8102 8103 8104 8105 8106 8107 8108 8109 8110 8111; do
  result=$(curl -s --noproxy "*" --max-time 3 http://localhost:$p/actuator/health 2>&1)
  status=$(echo "$result" | grep -o '"status":"[^"]*"' | head -1)
  redis=$(echo "$result" | grep -o '"redis":[^,}]*' | head -1)
  echo ":$p $status | $redis"
done
