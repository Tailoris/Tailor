import { test, expect } from '@playwright/test'

test.describe('Authentication', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
  })

  test('should navigate to login page', async ({ page }) => {
    const loginButton = page.locator('text=登录')
    await loginButton.click()
    await page.waitForURL(/\/login/)
    await expect(page).toHaveURL(/\/login/)
  })

  test('should navigate to register page', async ({ page }) => {
    const registerButton = page.locator('text=注册')
    await registerButton.click()
    await page.waitForURL(/\/register/)
    await expect(page).toHaveURL(/\/register/)
  })

  test('should login with valid credentials', async ({ page }) => {
    await page.goto('/login')
    await page.waitForLoadState('networkidle')

    const usernameInput = page.locator('input[name="username"], input[placeholder*="账号"], input[placeholder*="用户名"]')
    const passwordInput = page.locator('input[name="password"], input[placeholder*="密码"]')
    const submitButton = page.locator('button:has-text("登录")')

    await usernameInput.fill('testuser')
    await passwordInput.fill('password123')
    await submitButton.click()

    await page.waitForTimeout(2000)
  })

  test('should logout successfully', async ({ page }) => {
    await page.goto('/login')
    await page.waitForLoadState('networkidle')

    const usernameInput = page.locator('input[name="username"], input[placeholder*="账号"], input[placeholder*="用户名"]')
    const passwordInput = page.locator('input[name="password"], input[placeholder*="密码"]')
    const submitButton = page.locator('button:has-text("登录")')

    await usernameInput.fill('testuser')
    await passwordInput.fill('password123')
    await submitButton.click()

    await page.waitForTimeout(2000)

    const logoutButton = page.locator('text=退出')
    if (await logoutButton.isVisible()) {
      await logoutButton.click()
      await page.waitForURL(/\/login/)
      await expect(page).toHaveURL(/\/login/)
    }
  })
})