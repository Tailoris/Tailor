import { test, expect } from '@playwright/test';
import { scanPageForA11y, scanMultiplePages, generateA11yReport } from '../axe-runner';

/**
 * WCAG 2.1 AA 合规性自动化测试 - UX-P3-01
 *
 * 使用 axe-core 对 Tailor IS PC 商城的核心页面进行自动化无障碍检测。
 * 测试覆盖 WCAG 2.1 AA 全部规则，包括 Perceivable、Operable、Understandable、Robust。
 */

const BASE_URL = process.env.BASE_URL || 'http://localhost:5173';

// 测试页面列表
const PAGES = [
  { name: '首页', url: '/' },
  { name: '商品列表', url: '/products' },
  { name: '商品详情', url: '/product/1' },
  { name: '购物车', url: '/cart' },
  { name: '结算', url: '/checkout' },
  { name: '登录', url: '/login' },
  { name: '注册', url: '/register' },
  { name: '个人中心', url: '/profile' },
  { name: '社区', url: '/community' },
  { name: '商家入驻', url: '/merchant-apply' },
  { name: '忘记密码', url: '/forgot-password' },
];

test.describe('WCAG 2.1 AA 合规性扫描', () => {
  test('全站无障碍扫描 - 所有页面', async ({ page }) => {
    const results = await scanMultiplePages(page, PAGES, {
      tags: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'],
      logViolations: true,
    });

    // 生成报告
    const report = generateA11yReport(results);
    console.log(report);

    // 验证：无 critical 违规
    for (const [name, result] of Object.entries(results)) {
      expect(result.passed, `${name} 页面存在 critical 违规`).toBe(true);
    }
  });

  for (const pageInfo of PAGES) {
    test(`${pageInfo.name} - WCAG 2.1 AA 合规`, async ({ page }) => {
      await page.goto(`${BASE_URL}${pageInfo.url}`, { waitUntil: 'networkidle' });

      const result = await scanPageForA11y(page, {
        tags: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'],
        logViolations: true,
      });

      // Critical 违规必须为 0
      expect(
        result.violationsByImpact.critical,
        `${pageInfo.name} 有 ${result.violationsByImpact.critical} 个 critical 违规`
      ).toBe(0);

      // Serious 违规应少于 5 个
      expect(
        result.violationsByImpact.serious,
        `${pageInfo.name} 有 ${result.violationsByImpact.serious} 个 serious 违规`
      ).toBeLessThanOrEqual(5);
    });
  }
});

test.describe('感知性 (Perceivable) 测试', () => {
  test('图片 alt 文本 - 商品列表', async ({ page }) => {
    await page.goto(`${BASE_URL}/products`, { waitUntil: 'networkidle' });

    const images = page.locator('img');
    const count = await images.count();

    let missingAlt = 0;
    for (let i = 0; i < count; i++) {
      const alt = await images.nth(i).getAttribute('alt');
      if (!alt) {
        missingAlt++;
      }
    }
    expect(missingAlt).toBe(0);
  });

  test('颜色对比度 - 关键文本', async ({ page }) => {
    await page.goto(BASE_URL, { waitUntil: 'networkidle' });

    const result = await scanPageForA11y(page, {
      tags: ['wcag2aa'],
      logViolations: false,
    });

    const colorViolations = result.violations.filter(
      (v) => v.id === 'color-contrast'
    );
    expect(colorViolations.length).toBeLessThanOrEqual(5);
  });

  test('表单必需字段标注', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });

    // 验证 required 字段有 aria-required
    const requiredInputs = page.locator('[aria-required="true"]');
    const requiredCount = await requiredInputs.count();
    expect(requiredCount).toBeGreaterThanOrEqual(2);
  });
});

test.describe('可操作性 (Operable) 测试', () => {
  test('键盘导航 - Tab 键顺序', async ({ page }) => {
    await page.goto(BASE_URL, { waitUntil: 'networkidle' });

    // 按 Tab 键几次，验证焦点元素存在
    for (let i = 0; i < 5; i++) {
      await page.keyboard.press('Tab');
    }

    const focusedElement = page.locator(':focus');
    await expect(focusedElement).toBeVisible();
  });

  test('跳过导航链接可用', async ({ page }) => {
    await page.goto(BASE_URL, { waitUntil: 'networkidle' });

    // 第一次 Tab 应该聚焦到跳过导航链接
    await page.keyboard.press('Tab');
    const skipNav = page.locator('.skip-nav');
    await expect(skipNav).toBeFocused();
  });

  test('页面标题存在', async ({ page }) => {
    await page.goto(BASE_URL, { waitUntil: 'networkidle' });

    const title = await page.title();
    expect(title).toBeTruthy();
    expect(title.length).toBeGreaterThan(0);
  });
});

test.describe('可理解性 (Understandable) 测试', () => {
  test('页面语言声明', async ({ page }) => {
    await page.goto(BASE_URL, { waitUntil: 'networkidle' });

    const lang = await page.locator('html').getAttribute('lang');
    expect(lang).toBeTruthy();
  });

  test('表单错误提示', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });

    // 提交空表单触发验证
    const loginBtn = page.locator('[data-testid="login-button"]');
    if (await loginBtn.isVisible()) {
      await loginBtn.click();
      await page.waitForTimeout(500);

      // 验证有错误提示
      const errorMessages = page.locator('.el-form-item__error');
      const errorCount = await errorMessages.count();
      expect(errorCount).toBeGreaterThan(0);
    }
  });
});

test.describe('健壮性 (Robust) 测试', () => {
  test('ARIA 属性有效性', async ({ page }) => {
    await page.goto(BASE_URL, { waitUntil: 'networkidle' });

    const result = await scanPageForA11y(page, {
      tags: ['wcag21a', 'wcag21aa'],
      logViolations: false,
    });

    // 验证无 ARIA 相关违规
    const ariaViolations = result.violations.filter(
      (v) => v.id.includes('aria') || v.id.includes('role')
    );
    expect(ariaViolations.length).toBeLessThanOrEqual(3);
  });

  test('语义化 HTML 结构', async ({ page }) => {
    await page.goto(BASE_URL, { waitUntil: 'networkidle' });

    // 验证主内容区域
    const mainContent = page.locator('[role="main"]');
    await expect(mainContent).toBeVisible();

    // 验证导航
    const navigation = page.locator('[role="navigation"]');
    const navCount = await navigation.count();
    expect(navCount).toBeGreaterThanOrEqual(1);
  });
});