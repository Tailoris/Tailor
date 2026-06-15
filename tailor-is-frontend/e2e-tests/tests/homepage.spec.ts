import { test, expect } from '@playwright/test';

test.describe('Homepage', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('should load the homepage successfully', async ({ page }) => {
    await expect(page).toHaveTitle(/裁智云|Tailor IS/);
  });

  test('should display the header with logo', async ({ page }) => {
    const logo = page.locator('.logo');
    await expect(logo).toBeVisible();
    await expect(logo).toContainText('裁智云');
  });

  test('should display navigation links', async ({ page }) => {
    const navLinks = page.locator('.nav-links a');
    await expect(navLinks).toHaveCount(4);

    await expect(navLinks.nth(0)).toHaveText('首页');
    await expect(navLinks.nth(1)).toHaveText('商品');
    await expect(navLinks.nth(2)).toHaveText('社区');
    await expect(navLinks.nth(3)).toHaveText('商家入驻');
  });

  test('should display login button when not authenticated', async ({ page }) => {
    const loginBtn = page.locator('.auth-btn.login-btn');
    await expect(loginBtn).toBeVisible();
    await expect(loginBtn).toHaveText('登录');
  });

  test('should display register button when not authenticated', async ({ page }) => {
    const registerBtn = page.locator('.auth-btn.register-btn');
    await expect(registerBtn).toBeVisible();
    await expect(registerBtn).toHaveText('注册');
  });

  test('should navigate to products page via nav link', async ({ page }) => {
    await page.locator('.nav-links a', { hasText: '商品' }).click();
    await expect(page).toHaveURL(/\/products/);
  });

  test('should navigate to login page via login button', async ({ page }) => {
    await page.locator('.auth-btn.login-btn').click();
    await expect(page).toHaveURL(/\/login/);
  });

  test('should display category grid section', async ({ page }) => {
    const categorySection = page.locator('.section-title', { hasText: '商品分类' });
    await expect(categorySection).toBeVisible();
  });

  test('should display featured products section', async ({ page }) => {
    const featuredSection = page.locator('.section-title', { hasText: '精选推荐' });
    await expect(featuredSection).toBeVisible();
  });

  test('should have a search box in the header', async ({ page }) => {
    const searchInput = page.locator('.search-box input');
    await expect(searchInput).toBeVisible();
    await expect(searchInput).toHaveAttribute('placeholder', /搜索/);
  });
});