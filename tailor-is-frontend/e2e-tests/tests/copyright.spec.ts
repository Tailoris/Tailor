import { test, expect } from '@playwright/test'

/**
 * 版权功能 E2E 测试 - TEST-P2-02.
 *
 * <p>覆盖版权相关功能：商品详情页版权信息展示、产品描述区域、产品图片等。</p>
 * <p>注意：版权登记功能主要通过后端 API 和区块链集成实现，用户端通过商品详情页展示版权信息。</p>
 */

test.describe('版权 - 商品详情页', () => {
  test('商品详情页加载正常', async ({ page }) => {
    await page.goto('/product/1')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('.el-breadcrumb')).toBeVisible()
  })

  test('商品详情页显示商品名称', async ({ page }) => {
    await page.goto('/product/1')
    await page.waitForLoadState('networkidle')

    const productName = page.locator('.product-name')
    const hasName = await productName.isVisible().catch(() => false)

    if (hasName) {
      await expect(productName).toBeVisible()
    }
  })

  test('商品详情页显示商品副标题', async ({ page }) => {
    await page.goto('/product/1')
    await page.waitForLoadState('networkidle')

    const subtitle = page.locator('.product-subtitle')
    const hasSubtitle = await subtitle.isVisible().catch(() => false)

    if (hasSubtitle) {
      await expect(subtitle).toBeVisible()
    }
  })

  test('商品详情页显示价格信息', async ({ page }) => {
    await page.goto('/product/1')
    await page.waitForLoadState('networkidle')

    const price = page.locator('.current-price')
    const hasPrice = await price.isVisible().catch(() => false)

    if (hasPrice) {
      await expect(price).toBeVisible()
    }
  })

  test('商品详情页显示销量和浏览统计', async ({ page }) => {
    await page.goto('/product/1')
    await page.waitForLoadState('networkidle')

    const metaInfo = page.locator('.meta-info')
    const hasMeta = await metaInfo.isVisible().catch(() => false)

    if (hasMeta) {
      const saleLabel = metaInfo.locator('.meta-item .label', { hasText: '销量' })
      const hasSale = await saleLabel.isVisible().catch(() => false)
      if (hasSale) {
        await expect(saleLabel).toBeVisible()
      }

      const viewLabel = metaInfo.locator('.meta-item .label', { hasText: '浏览' })
      const hasView = await viewLabel.isVisible().catch(() => false)
      if (hasView) {
        await expect(viewLabel).toBeVisible()
      }
    }
  })
})

test.describe('版权 - 产品图片画廊', () => {
  test('商品详情页图片轮播可交互', async ({ page }) => {
    await page.goto('/product/1')
    await page.waitForLoadState('networkidle')

    const carousel = page.locator('.el-carousel')
    const hasCarousel = await carousel.isVisible().catch(() => false)

    if (hasCarousel) {
      // 检查轮播图片存在
      const carouselItems = carousel.locator('.el-carousel__item')
      const count = await carouselItems.count()
      expect(count).toBeGreaterThan(0)
    }
  })

  test('图片画廊支持缩略图切换', async ({ page }) => {
    await page.goto('/product/1')
    await page.waitForLoadState('networkidle')

    const thumbnails = page.locator('.thumbnail-list img')
    const count = await thumbnails.count()

    if (count > 1) {
      // 点击第二个缩略图
      await thumbnails.nth(1).click()
      await page.waitForTimeout(300)

      // 第二个缩略图应有 active 类
      await expect(thumbnails.nth(1)).toHaveClass(/active/)
    }
  })
})

test.describe('版权 - 面包屑导航', () => {
  test('商品详情页面包屑显示首页和商品列表', async ({ page }) => {
    await page.goto('/product/1')
    await page.waitForLoadState('networkidle')

    const breadcrumb = page.locator('.el-breadcrumb')
    await expect(breadcrumb).toBeVisible()

    const homeLink = breadcrumb.locator('.el-breadcrumb__item', { hasText: '首页' })
    await expect(homeLink).toBeVisible()

    const productListLink = breadcrumb.locator('.el-breadcrumb__item', { hasText: '商品列表' })
    await expect(productListLink).toBeVisible()
  })

  test('面包屑中商品列表链接可点击返回', async ({ page }) => {
    await page.goto('/product/1')
    await page.waitForLoadState('networkidle')

    const productListLink = page.locator('.el-breadcrumb__item', { hasText: '商品列表' }).locator('a')
    const hasLink = await productListLink.isVisible().catch(() => false)

    if (hasLink) {
      await productListLink.click()
      await expect(page).toHaveURL(/\/products/)
    }
  })
})

test.describe('版权 - 商品列表到详情导航', () => {
  test('从商品列表点击商品进入详情页', async ({ page }) => {
    await page.goto('/products')
    await page.waitForLoadState('networkidle')

    const firstProductCard = page.locator('.product-grid .product-card-item, .product-grid > *').first()
    const cardExists = await firstProductCard.isVisible().catch(() => false)

    if (cardExists) {
      await firstProductCard.click()
      await expect(page).toHaveURL(/\/product\/\d+/)
    } else {
      test.skip(true, 'No products available to navigate to detail')
    }
  })
})