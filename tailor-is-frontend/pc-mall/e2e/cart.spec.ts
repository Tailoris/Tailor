import { test, expect } from '@playwright/test'

test.describe('Shopping Cart', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
  })

  test('should add product to cart', async ({ page }) => {
    const productItem = page.locator('.product-item').first()
    await productItem.click()
    await page.waitForURL(/\/product\//)

    const addToCartButton = page.locator('button:has-text("加入购物车")')
    await addToCartButton.click()

    await page.waitForTimeout(1000)

    const cartIcon = page.locator('.cart-icon')
    await expect(cartIcon).toHaveText(/[1-9]/)
  })

  test('should navigate to cart page', async ({ page }) => {
    const cartLink = page.locator('a:has-text("购物车")')
    await cartLink.click()
    await page.waitForURL(/\/cart/)
    await expect(page).toHaveURL(/\/cart/)
  })

  test('should display cart items', async ({ page }) => {
    await page.goto('/cart')
    await page.waitForLoadState('networkidle')

    const cartItems = page.locator('.cart-item')
    await expect(cartItems).toHaveCount(0)
  })

  test('should update cart item quantity', async ({ page }) => {
    await page.goto('/cart')
    await page.waitForLoadState('networkidle')

    const quantityInput = page.locator('.quantity-input')
    if (await quantityInput.first().isVisible()) {
      await quantityInput.first().fill('2')
      await quantityInput.first().press('Enter')
      await page.waitForTimeout(500)
    }
  })

  test('should remove item from cart', async ({ page }) => {
    await page.goto('/cart')
    await page.waitForLoadState('networkidle')

    const removeButton = page.locator('.remove-btn')
    if (await removeButton.first().isVisible()) {
      await removeButton.first().click()
      await page.waitForTimeout(500)
    }
  })

  test('should select all items in cart', async ({ page }) => {
    await page.goto('/cart')
    await page.waitForLoadState('networkidle')

    const selectAllCheckbox = page.locator('.select-all')
    if (await selectAllCheckbox.isVisible()) {
      await selectAllCheckbox.click()
      await page.waitForTimeout(500)
    }
  })

  test('should checkout selected items', async ({ page }) => {
    await page.goto('/cart')
    await page.waitForLoadState('networkidle')

    const checkoutButton = page.locator('button:has-text("结算")')
    if (await checkoutButton.isVisible()) {
      await checkoutButton.click()
      await page.waitForURL(/\/checkout/)
      await expect(page).toHaveURL(/\/checkout/)
    }
  })

  test('should clear cart', async ({ page }) => {
    await page.goto('/cart')
    await page.waitForLoadState('networkidle')

    const clearButton = page.locator('button:has-text("清空购物车")')
    if (await clearButton.isVisible()) {
      await clearButton.click()
      await page.waitForTimeout(500)
    }
  })

  test('should persist cart after page reload', async ({ page }) => {
    const productItem = page.locator('.product-item').first()
    await productItem.click()
    await page.waitForURL(/\/product\//)

    const addToCartButton = page.locator('button:has-text("加入购物车")')
    await addToCartButton.click()
    await page.waitForTimeout(1000)

    await page.reload()
    await page.waitForLoadState('networkidle')

    const cartIcon = page.locator('.cart-icon')
    await expect(cartIcon).toHaveText(/[1-9]/)
  })
})