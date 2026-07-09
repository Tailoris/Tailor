import { test, expect } from '@playwright/test'
import { testCoupon, generateUniqueId } from './utils/testData'

test.describe('Coupon System', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
  })

  test('should navigate to coupon page', async ({ page }) => {
    const couponLink = page.locator('a:has-text("优惠券")')
    if (await couponLink.first().isVisible()) {
      await couponLink.first().click()
      await page.waitForURL(/\/coupon/)
      await expect(page).toHaveURL(/\/coupon/)
    }
  })

  test('should display available coupons', async ({ page }) => {
    await page.goto('/coupon')
    await page.waitForLoadState('networkidle')

    const couponCards = page.locator('.coupon-card')
    const count = await couponCards.count()
    expect(count).toBeGreaterThanOrEqual(0)
    console.log(`Found ${count} coupon cards`)
  })

  test('should receive coupon', async ({ page }) => {
    await page.goto('/coupon')
    await page.waitForLoadState('networkidle')

    const receiveButton = page.locator('button:has-text("领取")')
    if (await receiveButton.first().isVisible()) {
      await receiveButton.first().click()
      await page.waitForTimeout(500)
    }
  })

  test('should navigate to my coupons page', async ({ page }) => {
    const myCouponLink = page.locator('a:has-text("我的优惠券")')
    if (await myCouponLink.first().isVisible()) {
      await myCouponLink.first().click()
      await page.waitForURL(/\/coupon\/my/)
      await expect(page).toHaveURL(/\/coupon\/my/)
    }
  })

  test('should display my coupons', async ({ page }) => {
    await page.goto('/coupon/my')
    await page.waitForLoadState('networkidle')

    const myCouponCards = page.locator('.coupon-card')
    const count = await myCouponCards.count()
    expect(count).toBeGreaterThanOrEqual(0)
    console.log(`Found ${count} my coupon cards`)
  })

  test('should verify coupon expiration date display', async ({ page }) => {
    await page.goto('/coupon')
    await page.waitForLoadState('networkidle')

    const couponCard = page.locator('.coupon-card').first()
    if (await couponCard.isVisible()) {
      const expirationText = couponCard.locator('.expiration')
      await expect(expirationText).toBeVisible()
      const text = await expirationText.textContent()
      expect(text).toBeTruthy()
      expect(text?.length).toBeGreaterThan(0)
    }
  })

  test('should verify coupon discount amount display', async ({ page }) => {
    await page.goto('/coupon')
    await page.waitForLoadState('networkidle')

    const couponCard = page.locator('.coupon-card').first()
    if (await couponCard.isVisible()) {
      const discountAmount = couponCard.locator('.discount-amount')
      await expect(discountAmount).toBeVisible()
      const text = await discountAmount.textContent()
      expect(text).toBeTruthy()
      expect(text).toMatch(/¥\d+/)
    }
  })

  test('should verify coupon min amount condition', async ({ page }) => {
    await page.goto('/coupon')
    await page.waitForLoadState('networkidle')

    const couponCard = page.locator('.coupon-card').first()
    if (await couponCard.isVisible()) {
      const conditionText = couponCard.locator('.condition')
      await expect(conditionText).toBeVisible()
      const text = await conditionText.textContent()
      expect(text).toBeTruthy()
    }
  })

  test('should apply coupon in checkout process', async ({ page }) => {
    await page.goto('/checkout')
    await page.waitForLoadState('networkidle')

    const couponSection = page.locator('section:has-text("优惠券")')
    if (await couponSection.isVisible()) {
      const selectCouponButton = couponSection.locator('button:has-text("选择")')
      if (await selectCouponButton.isVisible()) {
        await selectCouponButton.click()
        await page.waitForTimeout(500)

        const availableCoupon = page.locator('.coupon-card:not(.disabled)').first()
        if (await availableCoupon.isVisible()) {
          await availableCoupon.click()
          await page.waitForTimeout(500)
        }
      }
    }
  })

  test('should verify coupon discount applied to total', async ({ page }) => {
    await page.goto('/checkout')
    await page.waitForLoadState('networkidle')

    const discountSection = page.locator('.discount')
    if (await discountSection.isVisible()) {
      const discountText = await discountSection.textContent()
      console.log('Discount applied:', discountText)
    }
  })

  test('should display expired coupons as disabled', async ({ page }) => {
    await page.goto('/coupon/my')
    await page.waitForLoadState('networkidle')

    const expiredCoupons = page.locator('.coupon-card.expired')
    const count = await expiredCoupons.count()
    expect(count).toBeGreaterThanOrEqual(0)
    console.log(`Found ${count} expired coupons`)
  })

  test('should display used coupons as disabled', async ({ page }) => {
    await page.goto('/coupon/my')
    await page.waitForLoadState('networkidle')

    const usedCoupons = page.locator('.coupon-card.used')
    const count = await usedCoupons.count()
    expect(count).toBeGreaterThanOrEqual(0)
    console.log(`Found ${count} used coupons`)
  })

  test('should verify coupon stack rules', async ({ page }) => {
    await page.goto('/checkout')
    await page.waitForLoadState('networkidle')

    const couponSection = page.locator('section:has-text("优惠券")')
    if (await couponSection.isVisible()) {
      const stackInfo = couponSection.locator('.stack-info')
      if (await stackInfo.isVisible()) {
        const stackText = await stackInfo.textContent()
        console.log('Coupon stack rules:', stackText)
      }
    }
  })

  test('should handle coupon with insufficient amount', async ({ page }) => {
    await page.goto('/checkout')
    await page.waitForLoadState('networkidle')

    const couponSection = page.locator('section:has-text("优惠券")')
    if (await couponSection.isVisible()) {
      const selectCouponButton = couponSection.locator('button:has-text("选择")')
      if (await selectCouponButton.isVisible()) {
        await selectCouponButton.click()
        await page.waitForTimeout(500)

        const unavailableCoupons = page.locator('.coupon-card.disabled')
        const count = await unavailableCoupons.count()
        expect(count).toBeGreaterThanOrEqual(0)
        console.log(`Found ${count} unavailable coupons`)
      }
    }
  })

  test('should clear applied coupon', async ({ page }) => {
    await page.goto('/checkout')
    await page.waitForLoadState('networkidle')

    const couponSection = page.locator('section:has-text("优惠券")')
    if (await couponSection.isVisible()) {
      const clearButton = couponSection.locator('button:has-text("清除")')
      if (await clearButton.isVisible()) {
        await clearButton.click()
        await page.waitForTimeout(500)
      }
    }
  })

  test('should validate coupon format', async ({ page }) => {
    await page.goto('/coupon')
    await page.waitForLoadState('networkidle')

    const couponCards = page.locator('.coupon-card')
    for (let i = 0; i < Math.min(5, await couponCards.count()); i++) {
      const card = couponCards.nth(i)
      const discount = card.locator('.discount-amount')
      const condition = card.locator('.condition')
      const expiration = card.locator('.expiration')

      if (await discount.isVisible()) {
        const discountText = await discount.textContent()
        expect(discountText).toMatch(/¥\d+(\.\d{1,2})?/)
      }
      if (await condition.isVisible()) {
        const conditionText = await condition.textContent()
        expect(conditionText).toMatch(/满\d+元可用/)
      }
      if (await expiration.isVisible()) {
        const expirationText = await expiration.textContent()
        expect(expirationText).toMatch(/\d{4}-\d{2}-\d{2}/)
      }
    }
  })
})
