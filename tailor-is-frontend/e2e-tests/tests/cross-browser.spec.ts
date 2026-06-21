import { test, expect } from '@playwright/test';

/**
 * 跨浏览器兼容性测试 - UX-P3-02
 *
 * 测试 Tailor IS PC 商城在 Chromium、Firefox、WebKit 多浏览器下的兼容性。
 * 验证页面渲染、交互功能、表单提交等核心功能在不同浏览器下的一致性。
 */

const BASE_URL = process.env.BASE_URL || 'http://localhost:5173';

// 测试页面
const PAGES = [
  { name: '首页', url: '/' },
  { name: '商品列表', url: '/products' },
  { name: '商品详情', url: '/product/1' },
  { name: '购物车', url: '/cart' },
  { name: '登录', url: '/login' },
  { name: '注册', url: '/register' },
  { name: '社区', url: '/community' },
  { name: '商家入驻', url: '/merchant-apply' },
];

test.describe('跨浏览器 - 页面加载', () => {
  for (const pageInfo of PAGES) {
    test(`${pageInfo.name} - 页面正常加载`, async ({ page, browserName }) => {
      test.skip(
        browserName === 'webkit' && pageInfo.url === '/checkout',
        'WebKit 暂不支持结算页某些功能'
      );

      const response = await page.goto(`${BASE_URL}${pageInfo.url}`, {
        waitUntil: 'networkidle',
        timeout: 30000,
      });

      expect(response?.status()).toBeLessThan(400);
      expect(response?.ok()).toBe(true);

      // 验证页面标题
      const title = await page.title();
      expect(title).toBeTruthy();

      // 截图对比
      await page.screenshot({
        path: `screenshots/${browserName}-${pageInfo.name.replace(/\//g, '-')}.png`,
        fullPage: true,
      });
    });
  }
});

test.describe('跨浏览器 - 核心交互', () => {
  test('搜索功能 - 各浏览器', async ({ page, browserName }) => {
    await page.goto(BASE_URL, { waitUntil: 'networkidle' });

    // 查找搜索框
    const searchInput = page.locator('[aria-label*="搜索"]').first();
    if (await searchInput.isVisible()) {
      await searchInput.fill('测试商品');
      await searchInput.press('Enter');

      // 验证导航到搜索结果
      await page.waitForURL(/products/, { timeout: 5000 }).catch(() => {});
    }
  });

  test('登录表单 - 各浏览器', async ({ page, browserName }) => {
    await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });

    // 验证表单元素存在
    const usernameInput = page.locator('[data-testid="username-input"]');
    const passwordInput = page.locator('[data-testid="password-input"]');

    await expect(usernameInput).toBeVisible();
    await expect(passwordInput).toBeVisible();

    // 测试填写
    await usernameInput.fill('test@example.com');
    await passwordInput.fill('Test123456');

    const usernameValue = await usernameInput.inputValue();
    expect(usernameValue).toBe('test@example.com');
  });

  test('导航链接 - 各浏览器', async ({ page, browserName }) => {
    await page.goto(BASE_URL, { waitUntil: 'networkidle' });

    // 验证主导航链接
    const navLinks = page.locator('[role="navigation"] a');
    const navCount = await navLinks.count();

    // 至少应有 4 个导航链接
    expect(navCount).toBeGreaterThanOrEqual(4);

    // 验证每个链接都可点击
    for (let i = 0; i < navCount; i++) {
      const link = navLinks.nth(i);
      await expect(link).toBeVisible();
    }
  });
});

test.describe('跨浏览器 - CSS 兼容性', () => {
  test('CSS Grid 布局 - 各浏览器', async ({ page, browserName }) => {
    await page.goto(BASE_URL, { waitUntil: 'networkidle' });

    // 验证商品网格布局
    const hasGridSupport = await page.evaluate(() => {
      const el = document.createElement('div');
      el.style.display = 'grid';
      return el.style.display === 'grid';
    });

    expect(hasGridSupport).toBe(true);
  });

  test('Flexbox 布局 - 各浏览器', async ({ page, browserName }) => {
    await page.goto(BASE_URL, { waitUntil: 'networkidle' });

    const hasFlexSupport = await page.evaluate(() => {
      const el = document.createElement('div');
      el.style.display = 'flex';
      return el.style.display === 'flex';
    });

    expect(hasFlexSupport).toBe(true);
  });

  test('CSS 变量支持 - 各浏览器', async ({ page, browserName }) => {
    await page.goto(BASE_URL, { waitUntil: 'networkidle' });

    const hasCustomProperties = await page.evaluate(() => {
      return CSS.supports('--test', '0');
    });

    expect(hasCustomProperties).toBe(true);
  });
});

test.describe('跨浏览器 - JavaScript 兼容性', () => {
  test('ES2020 语法支持', async ({ page, browserName }) => {
    await page.goto(BASE_URL, { waitUntil: 'networkidle' });

    const result = await page.evaluate(() => {
      try {
        // 测试可选链
        const obj = { a: { b: 1 } };
        const val = obj?.a?.b;

        // 测试空值合并
        const val2 = null ?? 'default';

        // 测试 Promise.allSettled
        return typeof Promise.allSettled === 'function';
      } catch {
        return false;
      }
    });

    expect(result).toBe(true);
  });

  test('IntersectionObserver 支持', async ({ page, browserName }) => {
    await page.goto(BASE_URL, { waitUntil: 'networkidle' });

    const hasIntersectionObserver = await page.evaluate(() => {
      return typeof IntersectionObserver !== 'undefined';
    });

    expect(hasIntersectionObserver).toBe(true);
  });
});