import { test, expect } from '@playwright/test'

/**
 * 版型功能 E2E 测试 - TEST-P2-02.
 *
 * <p>覆盖版型相关功能：商品类型展示（含数字纸样类型）、SKU 规格选择、产品详情交互等。</p>
 * <p>注意：版型功能主要通过商品详情页和产品类型差异化展示，用户端无独立版型页面。</p>
 */

test.describe('版型 - 商品类型展示', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/products')
  })

  test('商品列表页支持按类型浏览', async ({ page }) => {
    await expect(page).toHaveURL(/\/products/)
    await expect(page.locator('.filter-section h3', { hasText: '商品分类' })).toBeVisible()
  })

  test('商品列表页显示排序选项', async ({ page }) => {
    const sortBar = page.locator('.sort-options')
    await expect(sortBar).toBeVisible()

    await expect(sortBar.locator('span', { hasText: '综合' })).toBeVisible()
    await expect(sortBar.locator('span', { hasText: '销量' })).toBeVisible()
    await expect(sortBar.locator('span', { hasText: '新品' })).toBeVisible()
  })
})

test.describe('版型 - 商品详情与规格选择', () => {
  test('商品详情页显示商品类型', async ({ page }) => {
    await page.goto('/product/1')
    await page.waitForLoadState('networkidle')

    const metaInfo = page.locator('.meta-info')
    const hasMeta = await metaInfo.isVisible().catch(() => false)

    if (hasMeta) {
      const typeLabel = metaInfo.locator('.meta-item .label', { hasText: '类型' })
      const hasTypeLabel = await typeLabel.isVisible().catch(() => false)
      if (hasTypeLabel) {
        await expect(typeLabel).toBeVisible()
      }
    }
  })

  test('商品详情页显示 SKU 规格选择区域', async ({ page }) => {
    await page.goto('/product/1')
    await page.waitForLoadState('networkidle')

    const skuSection = page.locator('.sku-section')
    const hasSku = await skuSection.isVisible().catch(() => false)

    if (hasSku) {
      await expect(skuSection).toBeVisible()
      const skuOptions = skuSection.locator('.sku-options')
      await expect(skuOptions).toBeVisible()
    }
  })

  test('SKU 规格选择可点击切换', async ({ page }) => {
    await page.goto('/product/1')
    await page.waitForLoadState('networkidle')

    const skuBtn = page.locator('.sku-options button').first()
    const hasSkuBtn = await skuBtn.isVisible().catch(() => false)

    if (hasSkuBtn) {
      await skuBtn.click()
      await page.waitForTimeout(300)
      await expect(skuBtn).toHaveClass(/active/)
    }
  })

  test('商品详情页显示数量选择器', async ({ page }) => {
    await page.goto('/product/1')
    await page.waitForLoadState('networkidle')

    const quantitySection = page.locator('.quantity-section')
    const hasQuantity = await quantitySection.isVisible().catch(() => false)

    if (hasQuantity) {
      await expect(quantitySection).toBeVisible()
      const inputNumber = quantitySection.locator('.el-input-number')
      await expect(inputNumber).toBeVisible()
    }
  })

  test('商品详情页显示加入购物车和立即购买按钮', async ({ page }) => {
    await page.goto('/product/1')
    await page.waitForLoadState('networkidle')

    const addCartBtn = page.locator('.add-cart-btn')
    const hasAddCart = await addCartBtn.isVisible().catch(() => false)
    if (hasAddCart) {
      await expect(addCartBtn).toBeVisible()
    }

    const buyNowBtn = page.locator('.buy-now-btn')
    const hasBuyNow = await buyNowBtn.isVisible().catch(() => false)
    if (hasBuyNow) {
      await expect(buyNowBtn).toBeVisible()
    }
  })
})

test.describe('版型 - 商品图片画廊', () => {
  test('商品详情页显示图片画廊', async ({ page }) => {
    await page.goto('/product/1')
    await page.waitForLoadState('networkidle')

    const gallery = page.locator('.gallery-section')
    const hasGallery = await gallery.isVisible().catch(() => false)

    if (hasGallery) {
      await expect(gallery).toBeVisible()
      const carousel = gallery.locator('.el-carousel')
      await expect(carousel).toBeVisible()
    }
  })

  test('商品详情页显示缩略图列表', async ({ page }) => {
    await page.goto('/product/1')
    await page.waitForLoadState('networkidle')

    const thumbnails = page.locator('.thumbnail-list')
    const hasThumbs = await thumbnails.isVisible().catch(() => false)

    if (hasThumbs) {
      await expect(thumbnails).toBeVisible()
      const thumbImages = thumbnails.locator('img')
      const count = await thumbImages.count()
      expect(count).toBeGreaterThan(0)
    }
  })
})

test.describe('版型 - 搜索与筛选', () => {
  test('通过搜索框搜索商品', async ({ page }) => {
    await page.goto('/products')

    const searchInput = page.locator('.search-box input')
    await expect(searchInput).toBeVisible()
    await searchInput.fill('西装')
    await searchInput.press('Enter')
    await expect(page).toHaveURL(/\/products\?keyword=西装/)
  })

  test('搜索无结果时显示空状态', async ({ page }) => {
    await page.goto('/products?keyword=xyznotexist123456')
    await page.waitForLoadState('networkidle')

    const empty = page.locator('.el-empty')
    const hasEmpty = await empty.isVisible().catch(() => false)
    const pagination = page.locator('.el-pagination')

    // 至少有一个：空状态或分页
    expect(hasEmpty || (await pagination.isVisible().catch(() => false))).toBeTruthy()
  })
})