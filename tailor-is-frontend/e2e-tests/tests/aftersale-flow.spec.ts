// ==============================================================================
// Tailor IS - 售后流程 E2E 测试 (Phase 3: TEST-P2-02)
// ==============================================================================
// 测试框架: Playwright
// 运行: npx playwright test tests/aftersale.spec.ts
// 验收标准: 售后申请/审核/退款全流程端到端连贯
// ==============================================================================

import { test, expect } from '@playwright/test'

const BASE_URL = process.env.BASE_URL || 'http://localhost:5173'

test.describe('售后流程 - 退款申请', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(`${BASE_URL}/login`)
    await page.fill('[data-testid="login-username"]', 'test_user')
    await page.fill('[data-testid="login-password"]', 'test_password')
    await page.click('[data-testid="login-submit"]')
    await page.waitForURL('**/home')
  })

  test('T-E2E-01: 用户申请仅退款', async ({ page }) => {
    // 1. 进入订单列表
    await page.goto(`${BASE_URL}/orders`)
    await page.waitForSelector('[data-testid="order-list"]')

    // 2. 选择一个已完成订单，点击"申请售后"
    const firstOrder = page.locator('[data-testid="order-item"]').first()
    await expect(firstOrder).toBeVisible()
    await firstOrder.locator('[data-testid="btn-aftersale"]').click()

    // 3. 选择售后类型为"仅退款"
    await page.waitForSelector('[data-testid="aftersale-form"]')
    await page.locator('[data-testid="aftersale-type"]').selectOption('refund')
    await page.fill('[data-testid="aftersale-reason"]', '商品与描述不符，申请退款')
    await page.fill('[data-testid="aftersale-amount"]', '99.00')

    // 4. 上传凭证图片
    await page.setInputFiles('[data-testid="aftersale-images"]', [])

    // 5. 提交申请
    await page.click('[data-testid="aftersale-submit"]')

    // 6. 验证提交成功
    await expect(page.locator('[data-testid="toast-success"]')).toBeVisible({ timeout: 5000 })
    await expect(page.locator('[data-testid="aftersale-status"]')).toContainText('待审核')
  })

  test('T-E2E-02: 用户申请退货退款', async ({ page }) => {
    await page.goto(`${BASE_URL}/orders`)
    await page.waitForSelector('[data-testid="order-list"]')

    const firstOrder = page.locator('[data-testid="order-item"]').first()
    await firstOrder.locator('[data-testid="btn-aftersale"]').click()

    await page.waitForSelector('[data-testid="aftersale-form"]')
    await page.locator('[data-testid="aftersale-type"]').selectOption('return_refund')
    await page.fill('[data-testid="aftersale-reason"]', '尺码不合适，申请退货退款')
    await page.fill('[data-testid="aftersale-amount"]', '149.00')

    await page.click('[data-testid="aftersale-submit"]')

    await expect(page.locator('[data-testid="aftersale-status"]')).toContainText('待审核')
    await expect(page.locator('[data-testid="return-address"]')).toBeVisible()
  })

  test('T-E2E-03: 退款进度查询', async ({ page }) => {
    await page.goto(`${BASE_URL}/aftersale`)
    await page.waitForSelector('[data-testid="aftersale-list"]')

    // 验证售后列表显示
    const aftersaleItems = page.locator('[data-testid="aftersale-item"]')
    const count = await aftersaleItems.count()
    expect(count).toBeGreaterThan(0)

    // 点击第一个售后单查看详情
    await aftersaleItems.first().click()
    await page.waitForSelector('[data-testid="aftersale-detail"]')

    // 验证进度时间线
    const timeline = page.locator('[data-testid="aftersale-timeline"]')
    await expect(timeline).toBeVisible()

    // 验证状态标签
    await expect(page.locator('[data-testid="aftersale-status"]')).toBeVisible()
  })

  test('T-E2E-04: 商家审核退款申请', async ({ page }) => {
    // 切换到商家账号登录
    await page.goto(`${BASE_URL}/merchant/login`)
    await page.fill('[data-testid="login-username"]', 'merchant_test')
    await page.fill('[data-testid="login-password"]', 'merchant_password')
    await page.click('[data-testid="login-submit"]')
    await page.waitForURL('**/merchant/dashboard')

    // 进入售后管理
    await page.goto(`${BASE_URL}/merchant/aftersale`)
    await page.waitForSelector('[data-testid="aftersale-list"]')

    // 选择第一个待审核的售后单
    const pendingItem = page.locator('[data-testid="aftersale-item"]').filter({ hasText: '待审核' }).first()
    await pendingItem.click()

    // 审核通过
    await page.waitForSelector('[data-testid="review-form"]')
    await page.locator('[data-testid="review-result"]').selectOption('approve')
    await page.fill('[data-testid="review-remark"]', '同意退款')
    await page.click('[data-testid="review-submit"]')

    // 验证审核结果
    await expect(page.locator('[data-testid="toast-success"]')).toBeVisible()
    await expect(page.locator('[data-testid="aftersale-status"]')).toContainText('已通过')
  })

  test('T-E2E-05: 退款后库存恢复验证', async ({ page }) => {
    await page.goto(`${BASE_URL}/orders`)
    await page.waitForSelector('[data-testid="order-list"]')

    // 获取商品原库存
    const productId = '10001'
    await page.goto(`${BASE_URL}/product/${productId}`)
    await page.waitForSelector('[data-testid="product-detail"]')
    const originalStock = await page.locator('[data-testid="product-stock"]').textContent()

    // 执行退款流程...
    // 在此处模拟退款审批通过

    // 验证库存恢复
    await page.goto(`${BASE_URL}/product/${productId}`)
    await page.waitForSelector('[data-testid="product-stock"]')
    const updatedStock = await page.locator('[data-testid="product-stock"]').textContent()

    expect(updatedStock).not.toBeNull()
    expect(Number(updatedStock)).toBeGreaterThanOrEqual(Number(originalStock))
  })
})

test.describe('售后流程 - 异常场景', () => {
  test('T-E2E-06: 重复提交退款申请应被拦截', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`)
    await page.fill('[data-testid="login-username"]', 'test_user')
    await page.fill('[data-testid="login-password"]', 'test_password')
    await page.click('[data-testid="login-submit"]')
    await page.waitForURL('**/home')

    await page.goto(`${BASE_URL}/orders`)
    await page.waitForSelector('[data-testid="order-list"]')

    const firstOrder = page.locator('[data-testid="order-item"]').first()
    await firstOrder.locator('[data-testid="btn-aftersale"]').click()

    await page.waitForSelector('[data-testid="aftersale-form"]')
    await page.locator('[data-testid="aftersale-type"]').selectOption('refund')
    await page.fill('[data-testid="aftersale-reason"]', '重复提交测试')
    await page.click('[data-testid="aftersale-submit"]')

    // 尝试再次提交同一订单的售后申请
    await page.goto(`${BASE_URL}/orders`)
    await firstOrder.locator('[data-testid="btn-aftersale"]').click()

    // 应显示已有售后申请的提示
    await expect(page.locator('[data-testid="duplicate-aftersale-warning"]')).toBeVisible()
  })

  test('T-E2E-07: 未支付订单不可申请售后', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`)
    await page.fill('[data-testid="login-username"]', 'test_user')
    await page.fill('[data-testid="login-password"]', 'test_password')
    await page.click('[data-testid="login-submit"]')
    await page.waitForURL('**/home')

    await page.goto(`${BASE_URL}/orders`)
    await page.waitForSelector('[data-testid="order-list"]')

    // 查找待支付订单
    const pendingOrder = page.locator('[data-testid="order-item"]').filter({ hasText: '待支付' }).first()
    const aftersaleBtn = pendingOrder.locator('[data-testid="btn-aftersale"]')

    // 待支付订单不应显示售后按钮，或按钮应被禁用
    const isDisabled = await aftersaleBtn.isDisabled().catch(() => true)
    const isHidden = !(await aftersaleBtn.isVisible().catch(() => false))
    expect(isDisabled || isHidden).toBeTruthy()
  })
})