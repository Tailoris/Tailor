import { test, expect } from '@playwright/test';

test.describe('Product Listing', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/products');
  });

  test('should display the product list page', async ({ page }) => {
    await expect(page).toHaveURL(/\/products/);
  });

  test('should display breadcrumb navigation', async ({ page }) => {
    const breadcrumb = page.locator('.el-breadcrumb');
    await expect(breadcrumb).toBeVisible();

    const homeLink = breadcrumb.locator('.el-breadcrumb__item', { hasText: '首页' });
    await expect(homeLink).toBeVisible();

    const listItem = breadcrumb.locator('.el-breadcrumb__item', { hasText: '商品列表' });
    await expect(listItem).toBeVisible();
  });

  test('should display category filter sidebar', async ({ page }) => {
    const categoryTitle = page.locator('.filter-section h3', { hasText: '商品分类' });
    await expect(categoryTitle).toBeVisible();
  });

  test('should display price filter section', async ({ page }) => {
    const priceTitle = page.locator('.filter-section h3', { hasText: '价格区间' });
    await expect(priceTitle).toBeVisible();
  });

  test('should display sort options', async ({ page }) => {
    const sortBar = page.locator('.sort-options');
    await expect(sortBar).toBeVisible();

    await expect(sortBar.locator('span', { hasText: '综合' })).toBeVisible();
    await expect(sortBar.locator('span', { hasText: '销量' })).toBeVisible();
    await expect(sortBar.locator('span', { hasText: '价格 ↑' })).toBeVisible();
    await expect(sortBar.locator('span', { hasText: '价格 ↓' })).toBeVisible();
    await expect(sortBar.locator('span', { hasText: '新品' })).toBeVisible();
  });

  test('should display total product count', async ({ page }) => {
    const totalText = page.locator('.total-text');
    await expect(totalText).toBeVisible();
  });

  test('should highlight active sort option on click', async ({ page }) => {
    const salesSort = page.locator('.sort-options span', { hasText: '销量' });
    await salesSort.click();
    await expect(salesSort).toHaveClass(/active/);
  });

  test('should have search functionality via header', async ({ page }) => {
    const searchInput = page.locator('.search-box input');
    await expect(searchInput).toBeVisible();
    await searchInput.fill('西装');
    await searchInput.press('Enter');
    await expect(page).toHaveURL(/\/products\?keyword=西装/);
  });

  test('should display pagination when products exist', async ({ page }) => {
    await page.waitForLoadState('networkidle');

    const pagination = page.locator('.el-pagination');
    const empty = page.locator('.el-empty');
    const hasPagination = await pagination.isVisible().catch(() => false);
    const isEmpty = await empty.isVisible().catch(() => false);

    expect(hasPagination || isEmpty).toBeTruthy();
  });
});

test.describe('Product Detail', () => {
  test('should navigate from product list to product detail', async ({ page }) => {
    await page.goto('/products');

    await page.waitForLoadState('networkidle');

    const firstProductCard = page.locator('.product-grid .product-card-item, .product-grid > *').first();
    const cardExists = await firstProductCard.isVisible().catch(() => false);

    if (cardExists) {
      await firstProductCard.click();
      await expect(page).toHaveURL(/\/product\/\d+/);
    } else {
      test.skip(true, 'No products available to navigate to detail');
    }
  });

  test('should display product detail with breadcrumb', async ({ page }) => {
    await page.goto('/product/1');

    const breadcrumb = page.locator('.el-breadcrumb');
    await expect(breadcrumb).toBeVisible();
  });

  test('should display product name on detail page', async ({ page }) => {
    await page.goto('/product/1');

    const productName = page.locator('.product-name');
    await expect(productName).toBeVisible();
  });

  test('should display "add to cart" button', async ({ page }) => {
    await page.goto('/product/1');

    const addCartBtn = page.locator('.add-cart-btn');
    await expect(addCartBtn).toBeVisible();
    await expect(addCartBtn).toHaveText('加入购物车');
  });

  test('should display "buy now" button', async ({ page }) => {
    await page.goto('/product/1');

    const buyNowBtn = page.locator('.buy-now-btn');
    await expect(buyNowBtn).toBeVisible();
    await expect(buyNowBtn).toHaveText('立即购买');
  });

  test('should display price on detail page', async ({ page }) => {
    await page.goto('/product/1');

    const price = page.locator('.current-price');
    await expect(price).toBeVisible();
  });
});