# Tailor IS API 健康检查脚本 - Windows PowerShell
# 测试核心API接口的可用性

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "===  Tailor IS API 健康检查  ===" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8080"

$apiTests = @(
    @{ Method="GET"; Path="/actuator/health"; Name="健康检查"; ExpectedCode=200 },
    @{ Method="GET"; Path="/actuator/info"; Name="应用信息"; ExpectedCode=200 },
    @{ Method="GET"; Path="/actuator/prometheus"; Name="Prometheus指标"; ExpectedCode=200 },
    @{ Method="GET"; Path="/api/v1/user/health"; Name="用户服务"; ExpectedCode=200 },
    @{ Method="GET"; Path="/api/v1/product/health"; Name="商品服务"; ExpectedCode=200 },
    @{ Method="GET"; Path="/api/v1/order/health"; Name="订单服务"; ExpectedCode=200 },
    @{ Method="GET"; Path="/api/v1/payment/health"; Name="支付服务"; ExpectedCode=200 },
    @{ Method="GET"; Path="/api/v1/community/health"; Name="社区服务"; ExpectedCode=200 },
    @{ Method="GET"; Path="/api/v1/copyright/health"; Name="版权服务"; ExpectedCode=200 },
    @{ Method="GET"; Path="/api/v1/ai/health"; Name="AI服务"; ExpectedCode=200 }
)

$passed = 0
$failed = 0

foreach ($test in $apiTests) {
    $url = "$baseUrl$($test.Path)"
    try {
        $response = Invoke-WebRequest -Uri $url -Method $test.Method -UseBasicParsing -TimeoutSec 5
        $status = $response.StatusCode

        if ($status -eq $test.ExpectedCode) {
            Write-Host "  [OK] $($test.Name) - HTTP $status" -ForegroundColor Green
            $passed++
        } else {
            Write-Host "  [WARN] $($test.Name) - HTTP $status (期望: $($test.ExpectedCode))" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "  [FAIL] $($test.Name) - $($_.Exception.Message)" -ForegroundColor Red
        $failed++
    }
}

Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "检查完成: $passed 通过, $failed 失败" -ForegroundColor $(if($failed -eq 0){"Green"}else{"Yellow"})
Write-Host "================================================" -ForegroundColor Cyan
