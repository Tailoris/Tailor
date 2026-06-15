# Tailor IS 一键部署验证脚本 - Windows PowerShell
# 运行所有检查项并生成综合报告

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "===  Tailor IS 完整部署验证  ===" -ForegroundColor Cyan
Write-Host "===  $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')  ===" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

# 1. 基础服务检查
Write-Host ">>> 步骤 1: 基础服务端口检查..." -ForegroundColor Yellow
$ports = @(3306, 6379, 5672, 8848, 15672, 8080, 8101, 8102, 8103, 8104, 8105)
$portResults = @()
foreach ($port in $ports) {
    $result = Test-NetConnection -ComputerName localhost -Port $port -WarningAction SilentlyContinue
    $portResults += [PSCustomObject]@{
        Port = $port
        Status = if($result.TcpTestSucceeded){"OK"}else{"FAIL"}
    }
}
$portResults | Format-Table -AutoSize
Write-Host ""

# 2. 健康检查
Write-Host ">>> 步骤 2: 微服务健康检查..." -ForegroundColor Yellow
$services = @(8080, 8101, 8102, 8103, 8104, 8105, 8106, 8107, 8108, 8109, 8110, 8111)
$healthResults = @()
foreach ($port in $services) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:$port/actuator/health" -UseBasicParsing -TimeoutSec 3
        $healthResults += [PSCustomObject]@{
            Port = $port
            Status = "OK"
            Code = $response.StatusCode
        }
    } catch {
        $healthResults += [PSCustomObject]@{
            Port = $port
            Status = "FAIL"
            Code = 0
        }
    }
}
$healthResults | Format-Table -AutoSize
Write-Host ""

# 3. 快速性能测试
Write-Host ">>> 步骤 3: 快速性能测试 (10次请求)..." -ForegroundColor Yellow
$times = @()
for ($i = 1; $i -le 10; $i++) {
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $null = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -UseBasicParsing -TimeoutSec 5
        $sw.Stop()
        $times += $sw.ElapsedMilliseconds
    } catch {
        $sw.Stop()
        $times += 9999
    }
}
$avgTime = ($times | Where-Object {$_ -lt 9999} | Measure-Object -Average).Average
Write-Host "  平均响应时间: $avgTime ms" -ForegroundColor $(if($avgTime -lt 200){"Green"}else{"Yellow"})
Write-Host ""

# 4. 汇总
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "===  验证结果汇总  ===" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan

$portOk = ($portResults | Where-Object {$_.Status -eq "OK"}).Count
$healthOk = ($healthResults | Where-Object {$_.Status -eq "OK"}).Count

Write-Host "端口检查: $portOk / $($portResults.Count) 正常" -ForegroundColor $(if($portOk -eq $portResults.Count){"Green"}else{"Yellow"})
Write-Host "服务健康: $healthOk / $($healthResults.Count) 正常" -ForegroundColor $(if($healthOk -eq $healthResults.Count){"Green"}else{"Yellow"})
Write-Host ""

if ($portOk -eq $portResults.Count -and $healthOk -eq $healthResults.Count) {
    Write-Host "✅ 部署验证通过!" -ForegroundColor Green
    Write-Host ""
    Write-Host "可以执行下一步操作:" -ForegroundColor Cyan
    Write-Host "  1. 性能测试: .\performance-test.ps1 -Iterations 100" -ForegroundColor Gray
    Write-Host "  2. 安全审计: .\security-audit.ps1" -ForegroundColor Gray
    Write-Host "  3. API检查: .\api-health-check.ps1" -ForegroundColor Gray
    Write-Host "  4. 灰度发布: .\gray-release.ps1 deploy" -ForegroundColor Gray
} else {
    Write-Host "⚠️  部分服务异常，请检查日志" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan
