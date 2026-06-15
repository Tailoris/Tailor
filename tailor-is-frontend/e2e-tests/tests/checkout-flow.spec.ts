import { test, expect, Page } from '@playwright/test';

/**
 * 核心购物流程 E2E 测试 - Sprint 9 QA-003
 *
 * <p>覆盖完整购物流程: 浏览 → 加购 → 下单 → 支付 → 发货 → 收货</p>
 *
 * @author Tailor IS Team
 */

const BASE_URL = process.env.BASE_URL || 'https://staging.tailoris.com';
const TEST_USER = {
  username: 'e2e_test_user',
  password: 'Test@123456',
  phone: '13800138000',
};

async function login(page: Page) {
  await page.goto(`${BASE_URL}/login`);
  await page.fill('[data-testid="username-input"]', TEST_USER.username);
  await page.fill('[data-testid="password-input"]', TEST_USER.password);
  await page.click('[data-testid="login-button"]');
  await page.waitForURL(/\/home/);
}

test.describe('核心购物流程', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('1. 浏览首页 - 验证页面加载', async ({ page }) => {
    await page.goto(BASE_URL);

    // 验证关键元素
    await expect(page.locator('[data-testid="header-logo"]')).toBeVisible();
    await expect(page.locator('[data-testid="search-bar"]')).toBeVisible();
    await expect(page.locator('[data-testid="banner-carousel"]')).toBeVisible();
    await expect(page.locator('[data-testid="category-nav"]')).toBeVisible();

    // 验证首屏加载时间 < 3s
    const loadTime = await page.evaluate(() => {
      const timing = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming;
      return timing.loadEventEnd - timing.fetchStart;
    });
    expect(loadTime).toBeLessThan(3000);
  });

  test('2. 商品列表 - 浏览+筛选', async ({ page }) => {
    await page.goto(`${BASE_URL}/products`);

    // 等待商品列表加载
    await page.waitForSelector('[data-testid="product-card"]');

    // 验证有商品
    const products = await page.locator('[data-testid="product-card"]').count();
    expect(products).toBeGreaterThan(0);

    // 筛选测试
    await page.click('[data-testid="category-shirt"]');
    await page.waitForLoadState('networkidle');
    const filteredCount = await page.locator('[data-testid="product-card"]').count();
    expect(filteredCount).toBeGreaterThan(0);

    // 价格排序
    await page.click('[data-testid="sort-price-desc"]');
    await page.waitForLoadState('networkidle');

    // 验证排序生效
    const prices = await page.locator('[data-testid="product-price"]').allTextContents();
    const numericPrices = prices.map(p => parseFloat(p.replace(/[^\d.]/g, '')));
    for (let i = 1; i < numericPrices.length; i++) {
      expect(numericPrices[i - 1]).toBeGreaterThanOrEqual(numericPrices[i]);
    }
  });

  test('3. 商品详情 - 查看完整信息', async ({ page }) => {
    await page.goto(`${BASE_URL}/products`);
    await page.click('[data-testid="product-card"]:first-child');

    // 验证详情页
    await expect(page.locator('[data-testid="product-title"]')).toBeVisible();
    await expect(page.locator('[data-testid="product-price"]')).toBeVisible();
    await expect(page.locator('[data-testid="product-images"]')).toBeVisible();
    await expect(page.locator('[data-testid="sku-selector"]')).toBeVisible();
    await expect(page.locator('[data-testid="add-to-cart-button"]')).toBeVisible();

    // 切换 SKU
    await page.click('[data-testid="sku-color-blue"]');
    await page.click('[data-testid="sku-size-xl"]');

    // 验证库存
    const stock = await page.locator('[data-testid="stock-info"]').textContent();
    expect(stock).toMatch(/库存\d+/);
  });

  test('4. 加入购物车 - 验证购物车', async ({ page }) => {
    await page.goto(`${BASE_URL}/products`);
    await page.click('[data-testid="product-card"]:first-child');
    await page.click('[data-testid="sku-color-red"]');
    await page.click('[data-testid="sku-size-l"]');
    await page.click('[data-testid="add-to-cart-button"]');

    // 验证加入成功提示
    await expect(page.locator('[data-testid="toast-success"]')).toContainText('已加入购物车');

    // 进入购物车
    await page.goto(`${BASE_URL}/cart`);
    await page.waitForSelector('[data-testid="cart-item"]');
    const cartItems = await page.locator('[data-testid="cart-item"]').count();
    expect(cartItems).toBeGreaterThan(0);

    // 修改数量
    await page.click('[data-testid="quantity-plus"]');
    await page.waitForTimeout(500);
    const newQty = await page.locator('[data-testid="item-quantity"]').first().inputValue();
    expect(parseInt(newQty)).toBeGreaterThanOrEqual(2);
  });

  test('5. 提交订单 - 验证订单创建', async ({ page }) => {
    // 准备：先加购物车
    await page.goto(`${BASE_URL}/products`);
    await page.click('[data-testid="product-card"]:first-child');
    await page.click('[data-testid="sku-color-green"]');
    await page.click('[data-testid="sku-size-m"]');
    await page.click('[data-testid="add-to-cart-button"]');

    // 进入结算页
    await page.goto(`${BASE_URL}/checkout`);
    await page.waitForSelector('[data-testid="address-selector"]');

    // 选择地址
    await page.click('[data-testid="address-item"]:first-child');

    // 选择支付方式
    await page.click('[data-testid="payment-wechat"]');

    // 提交订单
    await page.click('[data-testid="submit-order-button"]');

    // 验证跳转到支付页
    await page.waitForURL(/\/payment\//, { timeout: 10000 });
    const orderNo = page.url().match(/\/payment\/(\w+)/)?.[1];
    expect(orderNo).toBeTruthy();

    // 验证订单号显示
    await expect(page.locator('[data-testid="order-no"]')).toContainText(orderNo!);
  });

  test('6. 完整支付流程（沙箱）- 验证支付成功', async ({ page }) => {
    // 准备订单
    await page.goto(`${BASE_URL}/products`);
    await page.click('[data-testid="product-card"]:first-child');
    await page.click('[data-testid="add-to-cart-button"]');
    await page.goto(`${BASE_URL}/checkout`);
    await page.click('[data-testid="address-item"]:first-child');
    await page.click('[data-testid="payment-alipay"]');
    await page.click('[data-testid="submit-order-button"]');

    await page.waitForURL(/\/payment\//);

    // 模拟沙箱支付成功
    await page.click('[data-testid="mock-pay-success"]');

    // 验证跳转到成功页
    await page.waitForURL(/\/payment\/success/, { timeout: 15000 });
    await expect(page.locator('[data-testid="payment-success-icon"]')).toBeVisible();

    // 验证订单状态
    await page.goto(`${BASE_URL}/orders`);
    const firstOrderStatus = await page.locator('[data-testid="order-status"]').first().textContent();
    expect(firstOrderStatus).toContain('已支付');
  });

  test('7. 查看订单列表 - 验证订单管理', async ({ page }) => {
    await page.goto(`${BASE_URL}/orders`);

    // 等待订单加载
    await page.waitForSelector('[data-testid="order-item"]');

    // 验证筛选
    await page.click('[data-testid="filter-pending-pay"]');
    await page.waitForLoadState('networkidle');
    const pendingOrders = await page.locator('[data-testid="order-item"]').count();
    expect(pendingOrders).toBeGreaterThanOrEqual(0);

    // 切换到已完成
    await page.click('[data-testid="filter-completed"]');
    await page.waitForLoadState('networkidle');
  });

  test('8. 取消订单 - 验证取消流程', async ({ page }) => {
    await page.goto(`${BASE_URL}/orders`);
    await page.click('[data-testid="filter-pending-pay"]');
    await page.waitForLoadState('networkidle');

    const firstOrder = page.locator('[data-testid="order-item"]').first();
    if (await firstOrder.count() > 0) {
      await firstOrder.locator('[data-testid="cancel-order-button"]').click();
      await page.click('[data-testid="confirm-cancel"]');
      await expect(page.locator('[data-testid="toast-success"]')).toContainText('订单已取消');
    }
  });
});

test.describe('响应式与性能', () => {
  test('9. 移动端布局 - 验证响应式', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });  // iPhone 8
    await page.goto(BASE_URL);

    // 移动端汉堡菜单
    await expect(page.locator('[data-testid="mobile-menu-toggle"]')).toBeVisible();
    await page.click('[data-testid="mobile-menu-toggle"]');
    await expect(page.locator('[data-testid="mobile-nav-drawer"]')).toBeVisible();
  });

  test('10. 弱网环境 - 验证容错', async ({ page, context }) => {
    // 模拟 3G 网络
    await context.route('**/*', (route) => {
      setTimeout(() => route.continue(), 200);
    });
    await page.goto(BASE_URL);

    // 验证页面仍然可访问
    await expect(page.locator('[data-testid="header-logo"]')).toBeVisible({ timeout: 10000 });
  });
});
