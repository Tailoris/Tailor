# Tailor IS 安全审计脚本 - Windows PowerShell
# 检查常见的安全配置和潜在漏洞

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "===  Tailor IS 安全审计  ===" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

function Test-SecurityHeader {
    param([string]$Url)

    Write-Host "检查: $Url" -ForegroundColor Yellow
    try {
        $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 10
        $headers = $response.Headers

        $securityHeaders = @{
            "X-Frame-Options" = "防止点击劫持"
            "X-Content-Type-Options" = "防止MIME类型嗅探"
            "X-XSS-Protection" = "XSS防护"
            "Strict-Transport-Security" = "HSTS强制HTTPS"
            "Content-Security-Policy" = "内容安全策略"
        }

        foreach ($header in $securityHeaders.GetEnumerator()) {
            if ($headers.ContainsKey($header.Key)) {
                Write-Host "    [OK] $($header.Key): $($headers[$header.Key])" -ForegroundColor Green
            } else {
                Write-Host "    [WARN] $($header.Key): 缺失 ($($header.Value))" -ForegroundColor Yellow
            }
        }
    } catch {
        Write-Host "    [FAIL] 无法获取响应头" -ForegroundColor Red
    }
}

Write-Host "=== 1. 安全响应头检查 ===" -ForegroundColor Yellow
Test-SecurityHeader -Url "http://localhost:8080/actuator/health"
Write-Host ""

Write-Host "=== 2. 敏感端口检查 ===" -ForegroundColor Yellow
$sensitivePorts = @(
    @{ Port=3306; Name="MySQL"; Risk="高"},
    @{ Port=6379; Name="Redis"; Risk="高"},
    @{ Port=5672; Name="RabbitMQ"; Risk="中"},
    @{ Port=27017; Name="MongoDB"; Risk="高"}
)

foreach ($port in $sensitivePorts) {
    $result = Test-NetConnection -ComputerName localhost -Port $port.Port -WarningAction SilentlyContinue
    if ($result.TcpTestSucceeded) {
        Write-Host "  [WARN] $($port.Name) (端口 $($port.Port)) - 风险: $($port.Risk) - 建议仅本地访问" -ForegroundColor Yellow
    }
}
Write-Host ""

Write-Host "=== 3. Actuator 端点检查 ===" -ForegroundColor Yellow
$actuatorEndpoints = @(
    "/actuator/env"
    "/actuator/beans"
    "/actuator/configprops"
    "/actuator/secrets"
)

foreach ($endpoint in $actuatorEndpoints) {
    $url = "http://localhost:8080$endpoint"
    try {
        $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 3
        Write-Host "  [WARN] $endpoint - 公开可访问 (HTTP $($response.StatusCode))" -ForegroundColor Yellow
    } catch {
        Write-Host "  [OK] $endpoint - 需要认证或不可访问" -ForegroundColor Green
    }
}
Write-Host ""

Write-Host "=== 4. API 认证检查 ===" -ForegroundColor Yellow
$protectedEndpoints = @(
    "/api/v1/user/*"
    "/api/v1/order/*"
    "/api/v1/payment/*"
)

Write-Host "  [INFO] 检查以下接口是否需要认证:" -ForegroundColor Cyan
foreach ($endpoint in $protectedEndpoints) {
    Write-Host "    - $endpoint" -ForegroundColor Gray
}
Write-Host ""

Write-Host "=== 5. CORS 配置检查 ===" -ForegroundColor Yellow
Write-Host "  [提示] 检查是否配置了合理的 CORS 策略" -ForegroundColor Cyan
Write-Host "    - 允许的来源域名"
Write-Host "    - 允许的HTTP方法"
Write-Host "    - 允许的请求头"
Write-Host ""

Write-Host "=== 6. SQL注入风险检查 ===" -ForegroundColor Yellow
Write-Host "  [提示] 检查所有用户输入是否经过参数化查询" -ForegroundColor Cyan
Write-Host ""

Write-Host "=== 7. 密码强度检查 ===" -ForegroundColor Yellow
Write-Host "  当前配置密码强度检查:" -ForegroundColor Cyan
Write-Host "    MySQL: mysql_CA75Yk" -ForegroundColor Gray
Write-Host "    Redis: redis_RSeR4G" -ForegroundColor Gray
Write-Host "    RabbitMQ: rabbitmq" -ForegroundColor Gray
Write-Host "    [建议] 生产环境请使用更强的密码" -ForegroundColor Yellow
Write-Host ""

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "安全审计完成" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "建议:" -ForegroundColor Yellow
Write-Host "  1. 确保所有敏感端口仅本地访问" -ForegroundColor Gray
Write-Host "  2. 限制 Actuator 端点公开访问" -ForegroundColor Gray
Write-Host "  3. 启用安全响应头" -ForegroundColor Gray
Write-Host "  4. 生产环境使用更强密码" -ForegroundColor Gray
Write-Host "  5. 使用 HTTPS 加密传输" -ForegroundColor Gray
