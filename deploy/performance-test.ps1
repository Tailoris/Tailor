# Tailor IS 性能测试脚本 - Windows PowerShell
# 使用 Invoke-WebRequest 进行简单的性能测试

param(
    [int]$Iterations = 100,
    [string]$TargetUrl = "http://localhost:8080/actuator/health"
)

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "===  Tailor IS 性能测试  ===" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "测试目标: $TargetUrl" -ForegroundColor Yellow
Write-Host "测试次数: $Iterations" -ForegroundColor Yellow
Write-Host ""

$results = @()
$errors = 0
$success = 0

Write-Host "开始性能测试..." -ForegroundColor Cyan

for ($i = 1; $i -le $Iterations; $i++) {
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $response = Invoke-WebRequest -Uri $TargetUrl -UseBasicParsing -TimeoutSec 10
        $sw.Stop()
        $elapsed = $sw.ElapsedMilliseconds

        $results += [PSCustomObject]@{
            Iteration = $i
            Status = $response.StatusCode
            ResponseTime = $elapsed
            Success = $true
        }
        $success++

        if ($i % 20 -eq 0) {
            Write-Host "  [$i/$Iterations] 完成 - 响应时间: ${elapsed}ms" -ForegroundColor Gray
        }
    } catch {
        $sw.Stop()
        $results += [PSCustomObject]@{
            Iteration = $i
            Status = 0
            ResponseTime = $sw.ElapsedMilliseconds
            Success = $false
        }
        $errors++
        Write-Host "  [$i/$Iterations] 失败: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "===  测试结果汇总  ===" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

$successResults = $results | Where-Object { $_.Success -eq $true }

if ($successResults.Count -gt 0) {
    $avg = ($successResults | Measure-Object -Property ResponseTime -Average).Average
    $min = ($successResults | Measure-Object -Property ResponseTime -Minimum).Minimum
    $max = ($successResults | Measure-Object -Property ResponseTime -Maximum).Maximum
    $p95Index = [math]::Floor($successResults.Count * 0.95) - 1
    $sortedResults = $successResults | Sort-Object ResponseTime
    $p95 = $sortedResults[$p95Index].ResponseTime
    $p99Index = [math]::Floor($successResults.Count * 0.99) - 1
    $p99 = $sortedResults[$p99Index].ResponseTime

    Write-Host "总请求数:     $Iterations" -ForegroundColor White
    Write-Host "成功:        $success ($([math]::Round($success/$Iterations*100, 2))%)" -ForegroundColor Green
    Write-Host "失败:        $errors ($([math]::Round($errors/$Iterations*100, 2))%)" -ForegroundColor Red
    Write-Host ""
    Write-Host "响应时间统计 (ms):" -ForegroundColor Yellow
    Write-Host "  平均 (AVG):  $avg" -ForegroundColor White
    Write-Host "  最小 (MIN):  $min" -ForegroundColor White
    Write-Host "  最大 (MAX):  $max" -ForegroundColor White
    Write-Host "  P95:         $p95" -ForegroundColor Cyan
    Write-Host "  P99:         $p99" -ForegroundColor Cyan
    Write-Host ""

    # 性能评估
    Write-Host "性能评估:" -ForegroundColor Yellow
    if ($p95 -le 200) {
        Write-Host "  [优] P95响应时间 ≤ 200ms (目标达成)" -ForegroundColor Green
    } elseif ($p95 -le 500) {
        Write-Host "  [良] P95响应时间 ≤ 500ms" -ForegroundColor Yellow
    } else {
        Write-Host "  [差] P95响应时间 > 500ms (需要优化)" -ForegroundColor Red
    }
} else {
    Write-Host "[FAIL] 所有请求均失败" -ForegroundColor Red
}

Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "测试完成" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
