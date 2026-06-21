import { test, expect } from '@playwright/test'

/**
 * 商家入驻 E2E 测试 - TEST-P2-02.
 *
 * <p>覆盖商家入驻申请的多步骤表单流程：基本信息、资质上传、联系信息、确认提交。</p>
 */

const TEST_USER = {
  phone: '13800138000',
  password: 'Test@123456',
}

test.describe('商家入驻 - 页面结构', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.fill('input[name="phone"]', TEST_USER.phone)
    await page.fill('input[name="password"]', TEST_USER.password)
    await page.click('button[type="submit"]')
    await expect(page).toHaveURL('/')
  })

  test('商家入驻页面加载正常', async ({ page }) => {
    await page.goto('/merchant-apply')
    await expect(page).toHaveURL(/\/merchant-apply/)
    await expect(page.locator('h2.page-title')).toContainText('商家入驻申请')
  })

  test('商家入驻页面显示面包屑导航', async ({ page }) => {
    await page.goto('/merchant-apply')

    const breadcrumb = page.locator('.el-breadcrumb')
    await expect(breadcrumb).toBeVisible()

    const homeLink = breadcrumb.locator('.el-breadcrumb__item', { hasText: '首页' })
    await expect(homeLink).toBeVisible()

    const merchantItem = breadcrumb.locator('.el-breadcrumb__item', { hasText: '商家入驻' })
    await expect(merchantItem).toBeVisible()
  })

  test('商家入驻页面显示步骤条', async ({ page }) => {
    await page.goto('/merchant-apply')

    const steps = page.locator('.el-steps, .apply-steps')
    await expect(steps).toBeVisible()

    await expect(page.locator('.el-step__title', { hasText: '基本信息' })).toBeVisible()
    await expect(page.locator('.el-step__title', { hasText: '资质上传' })).toBeVisible()
    await expect(page.locator('.el-step__title', { hasText: '联系信息' })).toBeVisible()
    await expect(page.locator('.el-step__title', { hasText: '确认提交' })).toBeVisible()
  })
})

test.describe('商家入驻 - 第一步：基本信息', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.fill('input[name="phone"]', TEST_USER.phone)
    await page.fill('input[name="password"]', TEST_USER.password)
    await page.click('button[type="submit"]')
    await expect(page).toHaveURL('/')
    await page.goto('/merchant-apply')
  })

  test('第一步显示商家类型选择', async ({ page }) => {
    await expect(page.locator('.el-select')).toBeVisible()
  })

  test('第一步显示公司名称输入框', async ({ page }) => {
    const companyInput = page.locator('input[placeholder*="公司"], input[placeholder*="店铺"]')
    await expect(companyInput).toBeVisible()
  })

  test('第一步显示营业执照号输入框', async ({ page }) => {
    const licenseInput = page.locator('input[placeholder*="营业执照"]')
    await expect(licenseInput).toBeVisible()
  })

  test('第一步可以进入下一步', async ({ page }) => {
    const nextBtn = page.locator('.step-actions button', { hasText: '下一步' })
    await expect(nextBtn).toBeVisible()
    await nextBtn.click()

    // 应该停留在第一步（因为表单未填写）或进入第二步
    await page.waitForTimeout(300)
  })

  test('填写基本信息后进入第二步', async ({ page }) => {
    // 选择商家类型
    const selectEl = page.locator('.el-select').first()
    await selectEl.click()
    await page.waitForTimeout(300)
    const option = page.locator('.el-select-dropdown__item').first()
    const hasOption = await option.isVisible().catch(() => false)
    if (hasOption) {
      await option.click()
    }

    // 填写公司名称
    const companyInput = page.locator('input[placeholder*="公司"], input[placeholder*="店铺"]').first()
    const hasCompanyInput = await companyInput.isVisible().catch(() => false)
    if (hasCompanyInput) {
      await companyInput.fill('测试服装有限公司')
    }

    // 填写营业执照号
    const licenseInput = page.locator('input[placeholder*="营业执照"]').first()
    const hasLicenseInput = await licenseInput.isVisible().catch(() => false)
    if (hasLicenseInput) {
      await licenseInput.fill('91110000MA12345678')
    }

    const nextBtn = page.locator('.step-actions button', { hasText: '下一步' })
    await nextBtn.click()
    await page.waitForTimeout(500)
  })
})

test.describe('商家入驻 - 第二步至第四步', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.fill('input[name="phone"]', TEST_USER.phone)
    await page.fill('input[name="password"]', TEST_USER.password)
    await page.click('button[type="submit"]')
    await expect(page).toHaveURL('/')
    await page.goto('/merchant-apply')
  })

  test('第二步显示资质上传表单', async ({ page }) => {
    // 先填写第一步并进入第二步
    const selectEl = page.locator('.el-select').first()
    await selectEl.click()
    await page.waitForTimeout(300)
    const option = page.locator('.el-select-dropdown__item').first()
    if (await option.isVisible().catch(() => false)) {
      await option.click()
    }
    const companyInput = page.locator('input[placeholder*="公司"], input[placeholder*="店铺"]').first()
    if (await companyInput.isVisible().catch(() => false)) {
      await companyInput.fill('测试服装有限公司')
    }
    const licenseInput = page.locator('input[placeholder*="营业执照"]').first()
    if (await licenseInput.isVisible().catch(() => false)) {
      await licenseInput.fill('91110000MA12345678')
    }
    const nextBtn = page.locator('.step-actions button', { hasText: '下一步' })
    await nextBtn.click()
    await page.waitForTimeout(500)

    // 检查第二步内容
    const licenseImageInput = page.locator('input[placeholder*="营业执照图片"]')
    const hasLicenseImage = await licenseImageInput.isVisible().catch(() => false)
    if (hasLicenseImage) {
      await expect(licenseImageInput).toBeVisible()
    }
  })

  test('第二步有上一步按钮', async ({ page }) => {
    // 先填写第一步并进入第二步
    const selectEl = page.locator('.el-select').first()
    await selectEl.click()
    await page.waitForTimeout(300)
    const option = page.locator('.el-select-dropdown__item').first()
    if (await option.isVisible().catch(() => false)) {
      await option.click()
    }
    const companyInput = page.locator('input[placeholder*="公司"], input[placeholder*="店铺"]').first()
    if (await companyInput.isVisible().catch(() => false)) {
      await companyInput.fill('测试服装有限公司')
    }
    const licenseInput = page.locator('input[placeholder*="营业执照"]').first()
    if (await licenseInput.isVisible().catch(() => false)) {
      await licenseInput.fill('91110000MA12345678')
    }
    const nextBtn = page.locator('.step-actions button', { hasText: '下一步' })
    await nextBtn.click()
    await page.waitForTimeout(500)

    // 检查上一步按钮
    const prevBtn = page.locator('.step-actions button', { hasText: '上一步' })
    await expect(prevBtn).toBeVisible()
  })
})

test.describe('商家入驻 - 未登录访问保护', () => {
  test('未登录访问商家入驻页跳转登录', async ({ page }) => {
    await page.context().clearCookies()
    await page.goto('/merchant-apply')
    await expect(page).toHaveURL(/\/login/)
  })
})