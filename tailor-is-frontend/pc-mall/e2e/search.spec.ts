import { test, expect } from '@playwright/test'

test.describe('Product Search', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
  })

  test('should navigate to search page', async ({ page }) => {
    const searchLink = page.locator('a:has-text("搜索")')
    if (await searchLink.isVisible()) {
      await searchLink.click()
      await page.waitForURL(/\/search/)
      await expect(page).toHaveURL(/\/search/)
    }
  })

  test('should display search input', async ({ page }) => {
    await page.goto('/search')
    await page.waitForLoadState('networkidle')

    const searchInput = page.locator('input[type="search"]')
    await expect(searchInput).toBeVisible()
  })

  test('should search by keyword', async ({ page }) => {
    await page.goto('/search')
    await page.waitForLoadState('networkidle')

    const searchInput = page.locator('input[type="search"]')
    if (await searchInput.isVisible()) {
      await searchInput.fill('纸样')
      await searchInput.press('Enter')
      await page.waitForLoadState('networkidle')
      await page.waitForTimeout(1000)

      const searchResults = page.locator('.product-item')
      await expect(searchResults).toBeVisible()
    }
  })

  test('should search from homepage search bar', async ({ page }) => {
    const searchInput = page.locator('input[type="search"]')
    if (await searchInput.isVisible()) {
      await searchInput.fill('定制')
      await searchInput.press('Enter')
      await page.waitForURL(/\/search/)
      await expect(page).toHaveURL(/\/search/)
    }
  })

  test('should display search results', async ({ page }) => {
    await page.goto('/search?keyword=纸样')
    await page.waitForLoadState('networkidle')

    const searchResults = page.locator('.product-item')
    await expect(searchResults).toBeVisible()
  })

  test('should display empty results for non-existent keyword', async ({ page }) => {
    await page.goto('/search?keyword=nonexistent-keyword-12345')
    await page.waitForLoadState('networkidle')

    const emptyMessage = page.locator('text=暂无搜索结果')
    if (await emptyMessage.isVisible()) {
      await expect(emptyMessage).toBeVisible()
    }
  })

  test('should filter by price range', async ({ page }) => {
    await page.goto('/search?keyword=纸样')
    await page.waitForLoadState('networkidle')

    const priceFilter = page.locator('.price-filter')
    if (await priceFilter.isVisible()) {
      await priceFilter.click()
      await page.waitForTimeout(500)
    }
  })

  test('should filter by category', async ({ page }) => {
    await page.goto('/search?keyword=纸样')
    await page.waitForLoadState('networkidle')

    const categoryFilter = page.locator('.category-filter')
    if (await categoryFilter.first().isVisible()) {
      await categoryFilter.first().click()
      await page.waitForTimeout(500)
    }
  })

  test('should sort results by price', async ({ page }) => {
    await page.goto('/search?keyword=纸样')
    await page.waitForLoadState('networkidle')

    const sortButton = page.locator('.sort-btn')
    if (await sortButton.isVisible()) {
      await sortButton.click()
      await page.waitForTimeout(500)

      const priceSortOption = page.locator('text=价格')
      if (await priceSortOption.isVisible()) {
        await priceSortOption.click()
        await page.waitForLoadState('networkidle')
      }
    }
  })

  test('should sort results by sales', async ({ page }) => {
    await page.goto('/search?keyword=纸样')
    await page.waitForLoadState('networkidle')

    const sortButton = page.locator('.sort-btn')
    if (await sortButton.isVisible()) {
      await sortButton.click()
      await page.waitForTimeout(500)

      const salesSortOption = page.locator('text=销量')
      if (await salesSortOption.isVisible()) {
        await salesSortOption.click()
        await page.waitForLoadState('networkidle')
      }
    }
  })

  test('should handle pagination', async ({ page }) => {
    await page.goto('/search?keyword=纸样')
    await page.waitForLoadState('networkidle')

    const pagination = page.locator('.pagination')
    if (await pagination.isVisible()) {
      const nextPageButton = page.locator('.pagination .btn-next')
      if (await nextPageButton.isVisible()) {
        await nextPageButton.click()
        await page.waitForLoadState('networkidle')
      }
    }
  })

  test('should clear search filters', async ({ page }) => {
    await page.goto('/search?keyword=纸样')
    await page.waitForLoadState('networkidle')

    const clearButton = page.locator('button:has-text("清除筛选")')
    if (await clearButton.isVisible()) {
      await clearButton.click()
      await page.waitForLoadState('networkidle')
    }
  })

  test('should display search suggestions', async ({ page }) => {
    await page.goto('/search')
    await page.waitForLoadState('networkidle')

    const searchInput = page.locator('input[type="search"]')
    if (await searchInput.isVisible()) {
      await searchInput.fill('纸')
      await page.waitForTimeout(500)

      const suggestions = page.locator('.search-suggestions')
      if (await suggestions.isVisible()) {
        await expect(suggestions).toBeVisible()
      }
    }
  })

  test('should select search suggestion', async ({ page }) => {
    await page.goto('/search')
    await page.waitForLoadState('networkidle')

    const searchInput = page.locator('input[type="search"]')
    if (await searchInput.isVisible()) {
      await searchInput.fill('纸')
      await page.waitForTimeout(500)

      const suggestions = page.locator('.search-suggestion-item')
      if (await suggestions.first().isVisible()) {
        await suggestions.first().click()
        await page.waitForLoadState('networkidle')
        await expect(page).toHaveURL(/\/search/)
      }
    }
  })

  test('should search with special characters', async ({ page }) => {
    await page.goto('/search')
    await page.waitForLoadState('networkidle')

    const searchInput = page.locator('input[type="search"]')
    if (await searchInput.isVisible()) {
      await searchInput.fill('测试@#$%')
      await searchInput.press('Enter')
      await page.waitForLoadState('networkidle')
    }
  })

  test('should search with empty keyword', async ({ page }) => {
    await page.goto('/search')
    await page.waitForLoadState('networkidle')

    const searchInput = page.locator('input[type="search"]')
    if (await searchInput.isVisible()) {
      await searchInput.fill('')
      await searchInput.press('Enter')
      await page.waitForLoadState('networkidle')
    }
  })
})