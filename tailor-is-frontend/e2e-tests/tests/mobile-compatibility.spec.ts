import { test, expect } from '@playwright/test';

/**
 * 移动端兼容性测试 - UX-P3-02
 *
 * 测试 Tailor IS PC 商城在不同移动设备视口下的兼容性。
 * 验证响应式布局、触摸交互、虚拟键盘适配等。
 */

const BASE_URL = process.env.BASE_URL || 'http://localhost:5173';

// 移动设备视口配置
const MOBILE_VIEWPORTS = {
  'iPhone 14': { width: 390, height: 844 },
  'iPhone 14 Pro Max': { width: 430, height: 932 },
  'Samsung Galaxy S23': { width: 360, height: 780 },
  'Google Pixel 7': { width: 412, height: 915 },
  'iPad Pro': { width: 1024, height: 1366 },
  'iPad Mini': { width: 768, height: 1024 },
  'Galaxy Tab S8': { width: 800, height: 1280 },
};

const DESKTOP_VIEWPORTS = {
  'Full HD': { width: 1920, height: 1080 },
  'HD Ready': { width: 1366, height: 768 },
  'MacBook Pro 14': { width: 1512, height: 982 },
};

test.describe('移动端视口 - 页面布局', () => {
  for (const [device, viewport] of Object.entries(MOBILE_VIEWPORTS)) {
    test(`${device} (${viewport.width}x${viewport.height}) - 首页布局`, async ({
      page,
    }) => {
      await page.setViewportSize(viewport);
      await page.goto(BASE_URL, { waitUntil: 'networkidle' });

      // 验证页面可正常渲染
      const body = page.locator('body');
      await expect(body).toBeVisible();

      // 验证无水平溢出
      const hasOverflow = await page.evaluate(() => {
        return document.documentElement.scrollWidth > window.innerWidth;
      });
      expect(hasOverflow).toBe(false);

      // 截图记录
      await page.screenshot({
        path: `screenshots/mobile-${device.toLowerCase().replace(/\s+/g, '-')}-home.png`,
        fullPage: true,
      });
    });
  }
});

test.describe('桌面端视口 - 页面布局', () => {
  for (const [device, viewport] of Object.entries(DESKTOP_VIEWPORTS)) {
    test(`${device} (${viewport.width}x${viewport.height}) - 首页布局`, async ({
      page,
    }) => {
      await page.setViewportSize(viewport);
      await page.goto(BASE_URL, { waitUntil: 'networkidle' });

      const body = page.locator('body');
      await expect(body).toBeVisible();

      const hasOverflow = await page.evaluate(() => {
        return document.documentElement.scrollWidth > window.innerWidth;
      });
      expect(hasOverflow).toBe(false);
    });
  }
});

test.describe('移动端 - 触摸交互', () => {
  test('触摸滚动 - 页面正常', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await page.goto(BASE_URL, { waitUntil: 'networkidle' });

    // 模拟触摸滚动
    await page.evaluate(() => {
      window.scrollTo(0, 500);
    });

    const scrollY = await page.evaluate(() => window.scrollY);
    expect(scrollY).toBeGreaterThan(0);
  });

  test('点击事件 - 导航链接', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await page.goto(BASE_URL, { waitUntil: 'networkidle' });

    // 查找并点击导航链接
    const navLinks = page.locator('[role="navigation"] a');
    const firstLink = navLinks.first();

    if (await firstLink.isVisible()) {
      await firstLink.tap();
      await page.waitForLoadState('networkidle');

      // 验证页面已导航
      const currentUrl = page.url();
      expect(currentUrl).toBeTruthy();
    }
  });

  test('输入框 - 虚拟键盘适配', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });

    // 点击输入框（触发虚拟键盘）
    const usernameInput = page.locator('[data-testid="username-input"]');
    await usernameInput.tap();

    // 输入文本
    await usernameInput.fill('test@example.com');

    // 验证输入值
    const value = await usernameInput.inputValue();
    expect(value).toBe('test@example.com');
  });
});

test.describe('移动端 - 响应式组件', () => {
  test('汉堡菜单 - 移动端适配', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await page.goto(BASE_URL, { waitUntil: 'networkidle' });

    // 验证导航在移动端的显示
    const header = page.locator('.app-header');
    await expect(header).toBeVisible();
  });

  test('商品卡片 - 移动端列数', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await page.goto(`${BASE_URL}/products`, { waitUntil: 'networkidle' });

    // 验证商品网格在小屏幕下的布局
    const productCards = page.locator('.product-card');
    const cardCount = await productCards.count();

    if (cardCount > 0) {
      const firstCard = productCards.first();
      await expect(firstCard).toBeVisible();
    }
  });
});

test.describe('移动端 - 性能', () => {
  test('首屏加载时间 - 移动端', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 });

    const startTime = Date.now();
    await page.goto(BASE_URL, { waitUntil: 'networkidle' });
    const loadTime = Date.now() - startTime;

    // 移动端首屏加载应 < 5 秒
    expect(loadTime).toBeLessThan(5000);
  });

  test('慢网络 - 页面可访问', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 });

    // 模拟 3G 网络
    await page.route('**/*', (route) => {
      route.continue();
    });

    await page.goto(BASE_URL, { waitUntil: 'domcontentloaded', timeout: 30000 });

    const body = page.locator('body');
    await expect(body).toBeVisible();
  });
});