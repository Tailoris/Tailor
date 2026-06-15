import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

/**
 * 无障碍 (Accessibility) 测试 - Sprint 9 QA-020
 *
 * <p>使用 axe-core 检测 WCAG 2.1 AA 标准合规性，
 * 覆盖以下类别：Perceivable、Operable、Understandable、Robust。</p>
 *
 * @author Tailor IS Team
 */

const BASE_URL = process.env.BASE_URL || 'https://staging.tailoris.com';

// 需要测试的关键页面
const PAGES_TO_TEST = [
  { name: '首页', url: '/' },
  { name: '商品列表', url: '/products' },
  { name: '商品详情', url: '/product/1' },
  { name: '购物车', url: '/cart' },
  { name: '登录页', url: '/login' },
  { name: '注册页', url: '/register' },
  { name: '个人中心', url: '/user' },
  { name: '版权登记', url: '/copyright/register' },
  { name: '订单列表', url: '/orders' },
  { name: '社区首页', url: '/community' },
];

test.describe('WCAG 2.1 AA 无障碍测试', () => {
  for (const pageInfo of PAGES_TO_TEST) {
    test(`${pageInfo.name} - 无障碍合规性检测`, async ({ page }) => {
      // 登录（除公开页面外）
      if (pageInfo.url !== '/' && pageInfo.url !== '/login' && pageInfo.url !== '/register') {
        await page.goto(`${BASE_URL}/login`);
        await page.fill('[data-testid="username-input"]', 'e2e_test_user');
        await page.fill('[data-testid="password-input"]', 'Test@123456');
        await page.click('[data-testid="login-button"]');
        await page.waitForURL(/\/home/);
      }

      await page.goto(`${BASE_URL}${pageInfo.url}`);
      await page.waitForLoadState('networkidle');

      // 使用 axe-core 进行无障碍检测
      const accessibilityScanResults = await new AxeBuilder({ page })
        .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
        .analyze();

      // 输出违规详情
      if (accessibilityScanResults.violations.length > 0) {
        console.log(`\n=== ${pageInfo.name} 无障碍违规 ===`);
        for (const violation of accessibilityScanResults.violations) {
          console.log(`\n规则: ${violation.id}`);
          console.log(`影响: ${violation.impact}`);
          console.log(`帮助: ${violation.help}`);
          console.log(`帮助URL: ${violation.helpUrl}`);
          console.log(`元素数: ${violation.nodes.length}`);
          for (const node of violation.nodes.slice(0, 3)) {
            console.log(`  目标: ${node.target.join(', ')}`);
            console.log(`  HTML: ${node.html.substring(0, 200)}`);
          }
        }
      }

      // 严重违规必须为 0
      const criticalViolations = accessibilityScanResults.violations.filter(
        v => v.impact === 'critical'
      );
      expect(criticalViolations).toHaveLength(0);

      // 高级违规应少于 3 个
      const seriousViolations = accessibilityScanResults.violations.filter(
        v => v.impact === 'serious'
      );
      expect(seriousViolations.length).toBeLessThanOrEqual(3);
    });
  }
});

test.describe('键盘可访问性', () => {
  test('Tab 键导航 - 关键页面', async ({ page }) => {
    await page.goto(BASE_URL);

    // 第一次 Tab 跳到 skip-nav
    await page.keyboard.press('Tab');
    const skipNav = page.locator('[data-testid="skip-nav"]');
    await expect(skipNav).toBeFocused();

    // 继续 Tab 跳转到主导航
    await page.keyboard.press('Tab');
    await page.keyboard.press('Tab');
    const firstNavItem = page.locator('[data-testid="main-nav-item"]:first-child');
    await expect(firstNavItem).toBeFocused();
  });

  test('Enter 键 - 按钮触发', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`);
    await page.keyboard.press('Tab');  // 用户名
    await page.keyboard.press('Tab');  // 密码
    await page.keyboard.press('Tab');  // 登录按钮
    await page.keyboard.press('Tab');  // 忘记密码
    await page.keyboard.press('Enter');

    // 验证导航到忘记密码页
    await page.waitForURL(/forgot-password/);
  });

  test('ESC 键 - 关闭弹窗', async ({ page }) => {
    await page.goto(BASE_URL);

    // 打开搜索弹窗
    await page.click('[data-testid="search-button"]');
    await expect(page.locator('[data-testid="search-modal"]')).toBeVisible();

    // ESC 关闭
    await page.keyboard.press('Escape');
    await expect(page.locator('[data-testid="search-modal"]')).not.toBeVisible();
  });
});

test.describe('屏幕阅读器兼容性', () => {
  test('ARIA 标签 - 关键交互元素', async ({ page }) => {
    await page.goto(BASE_URL);

    // 验证 aria-label
    const searchButton = page.locator('[data-testid="search-button"]');
    await expect(searchButton).toHaveAttribute('aria-label', /.+/);

    // 验证 aria-current
    const activeNav = page.locator('[data-testid="main-nav-item"][aria-current="page"]');
    await expect(activeNav).toHaveCount(1);
  });

  test('图片 alt 文本', async ({ page }) => {
    await page.goto(`${BASE_URL}/products`);

    // 验证所有产品图片都有 alt
    const productImages = page.locator('[data-testid="product-card"] img');
    const count = await productImages.count();

    for (let i = 0; i < Math.min(count, 10); i++) {
      const img = productImages.nth(i);
      const alt = await img.getAttribute('alt');
      expect(alt, `产品图 ${i} 缺少 alt 文本`).toBeTruthy();
    }
  });

  test('表单字段 label', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`);

    // 验证每个 input 都有 label
    const usernameInput = page.locator('[data-testid="username-input"]');
    const passwordInput = page.locator('[data-testid="password-input"]');

    const usernameLabelId = await usernameInput.getAttribute('aria-labelledby');
    const passwordLabelId = await passwordInput.getAttribute('aria-labelledby');

    expect(usernameLabelId).toBeTruthy();
    expect(passwordLabelId).toBeTruthy();
  });
});
