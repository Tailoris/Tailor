import { test, expect } from '@playwright/test'

test.describe('Homepage', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
  })

  test('should load homepage successfully', async ({ page }) => {
    await expect(page).toHaveTitle(/Tailoris/)
  })

  test('should display categories section', async ({ page }) => {
    const categoriesSection = page.locator('.category-section')
    await expect(categoriesSection).toBeVisible()
    const categories = page.locator('.category-item')
    await expect(categories.first()).toBeVisible()
  })

  test('should display hot products section', async ({ page }) => {
    const hotProductsSection = page.locator('.hot-products-section')
    await expect(hotProductsSection).toBeVisible()
    const products = page.locator('.product-item')
    await expect(products.first()).toBeVisible()
  })

  test('should display new products section', async ({ page }) => {
    const newProductsSection = page.locator('.new-products-section')
    await expect(newProductsSection).toBeVisible()
  })

  test('should display seckill products section', async ({ page }) => {
    const seckillSection = page.locator('.seckill-section')
    await expect(seckillSection).toBeVisible()
  })

  test('should navigate to product detail when clicking a product', async ({ page }) => {
    const productItem = page.locator('.product-item').first()
    await productItem.click()
    await page.waitForURL(/\/product\//)
    await expect(page).toHaveURL(/\/product\//)
  })

  test('should search products', async ({ page }) => {
    const searchInput = page.locator('input[placeholder*="搜索"]')
    await searchInput.fill('test')
    await searchInput.press('Enter')
    await page.waitForURL(/\/search/)
    await expect(page).toHaveURL(/\/search/)
  })
})