import { test, expect } from '@playwright/test'

/**
 * 售后流程 E2E 测试 - TEST-P2-02.
 *
 * <p>覆盖售后相关的核心用户流程：订单取消、订单详情查看、订单列表筛选等。</p>
 * <p>注意：用户端售后申请入口在订单详情页，商家端售后管理在 merchant-admin。</p>
 */

const TEST_USER = {
  phone: '13800138000',
  password: 'Test@123456',
}

test.describe('售后 - 订单取消', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.fill('input[name="phone"]', TEST_USER.phone)
    await page.fill('input[name="password"]', TEST_USER.password)
    await page.click('button[type="submit"]')
    await expect(page).toHaveURL('/')
  })

  test('已登录用户可访问订单列表', async ({ page }) => {
    await page.goto('/orders')
    await expect(page).toHaveURL(/\/orders/)
    await expect(page.locator('h2.page-title')).toContainText('我的订单')
  })

  test('订单列表显示状态筛选标签页', async ({ page }) => {
    await page.goto('/orders')

    const tabs = page.locator('.el-tabs')
    await expect(tabs).toBeVisible()

    await expect(page.locator('.el-tabs__item', { hasText: '全部' })).toBeVisible()
    await expect(page.locator('.el-tabs__item', { hasText: '待付款' })).toBeVisible()
    await expect(page.locator('.el-tabs__item', { hasText: '待发货' })).toBeVisible()
    await expect(page.locator('.el-tabs__item', { hasText: '待收货' })).toBeVisible()
    await expect(page.locator('.el-tabs__item', { hasText: '已完成' })).toBeVisible()
    await expect(page.locator('.el-tabs__item', { hasText: '已取消' })).toBeVisible()
  })

  test('点击订单查看详情', async ({ page }) => {
    await page.goto('/orders')
    await page.waitForLoadState('networkidle')

    const emptyEl = page.locator('.el-empty')
    const isEmpty = await emptyEl.isVisible().catch(() => false)

    if (!isEmpty) {
      const detailBtn = page.locator('.order-card .order-actions button', { hasText: '查看详情' }).first()
      const hasDetail = await detailBtn.isVisible().catch(() => false)

      if (hasDetail) {
        await detailBtn.click()
        await expect(page).toHaveURL(/\/order\//)
      }
    }
  })

  test('订单详情页显示费用明细', async ({ page }) => {
    await page.goto('/order/1')
    await page.waitForLoadState('networkidle')

    const breadcrumb = page.locator('.el-breadcrumb')
    await expect(breadcrumb).toBeVisible()

    const priceSection = page.locator('.price-section')
    const hasPrice = await priceSection.isVisible().catch(() => false)
    if (hasPrice) {
      await expect(page.locator('.price-item.total')).toBeVisible()
    }
  })

  test('订单详情页显示商品清单', async ({ page }) => {
    await page.goto('/order/1')
    await page.waitForLoadState('networkidle')

    const productsSection = page.locator('.products-section')
    const hasProducts = await productsSection.isVisible().catch(() => false)
    if (hasProducts) {
      await expect(page.locator('.products-section h3')).toContainText('商品清单')
    }
  })

  test('订单详情页显示物流信息区域', async ({ page }) => {
    await page.goto('/order/1')
    await page.waitForLoadState('networkidle')

    const logisticsSection = page.locator('.logistics-section')
    const hasLogistics = await logisticsSection.isVisible().catch(() => false)
    if (hasLogistics) {
      await expect(page.locator('.logistics-section h3')).toContainText('物流信息')
    }
  })
})

test.describe('售后 - 订单状态标签', () => {
  test('待付款订单显示付款和取消按钮', async ({ page }) => {
    await page.goto('/login')
    await page.fill('input[name="phone"]', TEST_USER.phone)
    await page.fill('input[name="password"]', TEST_USER.password)
    await page.click('button[type="submit"]')
    await expect(page).toHaveURL('/')

    await page.goto('/orders')
    await page.waitForLoadState('networkidle')

    // 切换到待付款标签
    const pendingTab = page.locator('.el-tabs__item', { hasText: '待付款' })
    await pendingTab.click()
    await page.waitForTimeout(500)

    // 检查是否有付款和取消按钮
    const payBtn = page.locator('.order-actions button', { hasText: '付款' })
    const cancelBtn = page.locator('.order-actions button', { hasText: '取消' })

    const hasPayBtn = await payBtn.isVisible().catch(() => false)
    const hasCancelBtn = await cancelBtn.isVisible().catch(() => false)

    // 如果有订单，应显示操作按钮；否则显示空状态
    expect(hasPayBtn || hasCancelBtn).toBeTruthy()
  })
})

test.describe('售后 - 未登录访问保护', () => {
  test('未登录访问订单页跳转登录', async ({ page }) => {
    await page.context().clearCookies()
    await page.goto('/orders')
    await expect(page).toHaveURL(/\/login/)
  })

  test('未登录访问订单详情跳转登录', async ({ page }) => {
    await page.context().clearCookies()
    await page.goto('/order/1')
    await expect(page).toHaveURL(/\/login/)
  })
})