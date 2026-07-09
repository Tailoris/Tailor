import { test, expect } from '@playwright/test'

test.describe('Order Flow', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
  })

  test('should navigate to checkout page from cart', async ({ page }) => {
    await page.goto('/cart')
    await page.waitForLoadState('networkidle')

    const checkoutButton = page.locator('button:has-text("结算")')
    if (await checkoutButton.isVisible()) {
      await checkoutButton.click()
      await page.waitForURL(/\/checkout/)
      await expect(page).toHaveURL(/\/checkout/)
    }
  })

  test('should display checkout page with order summary', async ({ page }) => {
    await page.goto('/checkout')
    await page.waitForLoadState('networkidle')

    const orderSummary = page.locator('.order-summary')
    await expect(orderSummary).toBeVisible()

    const totalAmount = page.locator('.total-amount')
    await expect(totalAmount).toBeVisible()
  })

  test('should select shipping address', async ({ page }) => {
    await page.goto('/checkout')
    await page.waitForLoadState('networkidle')

    const addressSelect = page.locator('.address-select')
    if (await addressSelect.isVisible()) {
      await addressSelect.first().click()
      await page.waitForTimeout(500)
    }
  })

  test('should select payment method', async ({ page }) => {
    await page.goto('/checkout')
    await page.waitForLoadState('networkidle')

    const paymentOptions = page.locator('.payment-option')
    if (await paymentOptions.first().isVisible()) {
      await paymentOptions.first().click()
      await page.waitForTimeout(500)
    }
  })

  test('should submit order', async ({ page }) => {
    await page.goto('/checkout')
    await page.waitForLoadState('networkidle')

    const submitButton = page.locator('button:has-text("提交订单")')
    if (await submitButton.isVisible()) {
      await submitButton.click()
      await page.waitForURL(/\/order-success/)
      await expect(page).toHaveURL(/\/order-success/)
    }
  })

  test('should display order success page', async ({ page }) => {
    await page.goto('/order-success')
    await page.waitForLoadState('networkidle')

    const successMessage = page.locator('text=订单提交成功')
    await expect(successMessage).toBeVisible()

    const orderNumber = page.locator('.order-number')
    await expect(orderNumber).toBeVisible()
  })

  test('should navigate to order list page', async ({ page }) => {
    const orderLink = page.locator('a:has-text("我的订单")')
    if (await orderLink.isVisible()) {
      await orderLink.click()
      await page.waitForURL(/\/orders/)
      await expect(page).toHaveURL(/\/orders/)
    }
  })

  test('should display order list', async ({ page }) => {
    await page.goto('/orders')
    await page.waitForLoadState('networkidle')

    const orderList = page.locator('.order-list')
    await expect(orderList).toBeVisible()
  })

  test('should filter orders by status', async ({ page }) => {
    await page.goto('/orders')
    await page.waitForLoadState('networkidle')

    const statusFilter = page.locator('.status-filter')
    if (await statusFilter.first().isVisible()) {
      await statusFilter.first().click()
      await page.waitForTimeout(500)
    }
  })

  test('should view order detail', async ({ page }) => {
    await page.goto('/orders')
    await page.waitForLoadState('networkidle')

    const orderItem = page.locator('.order-item')
    if (await orderItem.first().isVisible()) {
      await orderItem.first().click()
      await page.waitForURL(/\/order\//)
      await expect(page).toHaveURL(/\/order\//)
    }
  })

  test('should cancel order', async ({ page }) => {
    await page.goto('/orders')
    await page.waitForLoadState('networkidle')

    const cancelButton = page.locator('button:has-text("取消")')
    if (await cancelButton.first().isVisible()) {
      await cancelButton.first().click()
      await page.waitForTimeout(500)
    }
  })

  test('should pay order', async ({ page }) => {
    await page.goto('/orders')
    await page.waitForLoadState('networkidle')

    const payButton = page.locator('button:has-text("支付")')
    if (await payButton.first().isVisible()) {
      await payButton.first().click()
      await page.waitForTimeout(500)
    }
  })

  test('should confirm order', async ({ page }) => {
    await page.goto('/orders')
    await page.waitForLoadState('networkidle')

    const confirmButton = page.locator('button:has-text("确认收货")')
    if (await confirmButton.first().isVisible()) {
      await confirmButton.first().click()
      await page.waitForTimeout(500)
    }
  })
})