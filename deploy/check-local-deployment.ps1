# Tailor IS 部署状态检查脚本 - Windows PowerShell
# 适用于通过 localhost 访问 WSL2 中的 1Panel 服务

$ErrorActionPreference = "SilentlyContinue"

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "===  Tailor IS 部署状态检查  ===" -ForegroundColor Cyan
Write-Host "===  $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')  ===" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "=== 1. 1Panel 面板状态 ===" -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri 'http://localhost:42405/5b4c869c53' -UseBasicParsing -TimeoutSec 5
    if ($response.StatusCode -eq 200) {
        Write-Host "  [OK] 1Panel 面板可访问 (HTTP $($response.StatusCode))" -ForegroundColor Green
    } else {
        Write-Host "  [WARN] 1Panel 面板 HTTP $($response.StatusCode)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  [FAIL] 1Panel 面板无法访问: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== 2. 基础设施服务端口检查 ===" -ForegroundColor Yellow
$ports = @(
    @{Port=3306; Name="MySQL"; Expected="MySQL数据库"},
    @{Port=6379; Name="Redis"; Expected="Redis缓存"},
    @{Port=5672; Name="RabbitMQ"; Expected="RabbitMQ消息队列"},
    @{Port=8848; Name="Nacos"; Expected="Nacos注册中心"},
    @{Port=15672; Name="RabbitMQ Dashboard"; Expected="RabbitMQ管理界面"}
)

foreach ($svc in $ports) {
    $result = Test-NetConnection -ComputerName localhost -Port $svc.Port -WarningAction SilentlyContinue
    if ($result.TcpTestSucceeded) {
        Write-Host "  [OK] $($svc.Name) (端口 $($svc.Port)) - $($svc.Expected)" -ForegroundColor Green
    } else {
        Write-Host "  [FAIL] $($svc.Name) (端口 $($svc.Port)) - 未响应" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "=== 3. 微服务健康检查 ===" -ForegroundColor Yellow
$services = @(
    @{Port=8080; Name="Gateway"; Path="/actuator/health"},
    @{Port=8101; Name="User"; Path="/actuator/health"},
    @{Port=8102; Name="Merchant"; Path="/actuator/health"},
    @{Port=8103; Name="Product"; Path="/actuator/health"},
    @{Port=8104; Name="Order"; Path="/actuator/health"},
    @{Port=8105; Name="Payment"; Path="/actuator/health"},
    @{Port=8106; Name="Marketing"; Path="/actuator/health"},
    @{Port=8107; Name="AI"; Path="/actuator/health"},
    @{Port=8108; Name="Copyright"; Path="/actuator/health"},
    @{Port=8109; Name="Community"; Path="/actuator/health"},
    @{Port=8110; Name="Supply"; Path="/actuator/health"},
    @{Port=8111; Name="Message"; Path="/actuator/health"}
)

foreach ($svc in $services) {
    try {
        $url = "http://localhost:$($svc.Port)$($svc.Path)"
        $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 3
        if ($response.StatusCode -eq 200) {
            Write-Host "  [OK] $($svc.Name) (端口 $($svc.Port)) - 健康" -ForegroundColor Green
        } else {
            Write-Host "  [WARN] $($svc.Name) (端口 $($svc.Port)) - HTTP $($response.StatusCode)" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "  [FAIL] $($svc.Name) (端口 $($svc.Port)) - 未响应" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "=== 4. Nacos 服务注册检查 ===" -ForegroundColor Yellow
try {
    $nacosUrl = "http://localhost:8848/nacos/v1/ns/catalog/services?pageNo=1&pageSize=20"
    $response = Invoke-WebRequest -Uri $nacosUrl -UseBasicParsing -TimeoutSec 5
    if ($response.StatusCode -eq 200) {
        $content = $response.Content | ConvertFrom-Json
        $count = $content.count
        Write-Host "  [OK] Nacos 已注册服务数量: $count" -ForegroundColor Green
        if ($content.serviceList) {
            foreach ($svc in $content.serviceList) {
                Write-Host "       - $($svc.name)" -ForegroundColor Gray
            }
        }
    }
} catch {
    Write-Host "  [FAIL] Nacos 服务列表获取失败" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== 5. Prometheus 监控检查 ===" -ForegroundColor Yellow
try {
    $promUrl = "http://localhost:9090/api/v1/status/flags"
    $response = Invoke-WebRequest -Uri $promUrl -UseBasicParsing -TimeoutSec 3
    if ($response.StatusCode -eq 200) {
        Write-Host "  [OK] Prometheus 监控服务运行中" -ForegroundColor Green
    }
} catch {
    Write-Host "  [WARN] Prometheus 监控服务未启动" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== 6. Grafana 仪表盘检查 ===" -ForegroundColor Yellow
try {
    $grafanaUrl = "http://localhost:3000/api/health"
    $response = Invoke-WebRequest -Uri $grafanaUrl -UseBasicParsing -TimeoutSec 3
    if ($response.StatusCode -eq 200) {
        Write-Host "  [OK] Grafana 仪表盘运行中" -ForegroundColor Green
    }
} catch {
    Write-Host "  [WARN] Grafana 仪表盘未启动" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "检查完成" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
