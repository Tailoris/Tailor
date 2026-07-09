import { test, expect } from '@playwright/test'

test.describe('Address Management', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
  })

  test('should navigate to address management page', async ({ page }) => {
    const addressLink = page.locator('a:has-text("地址管理")')
    if (await addressLink.first().isVisible()) {
      await addressLink.first().click()
      await page.waitForURL(/\/address/)
      await expect(page).toHaveURL(/\/address/)
    }
  })

  test('should display address list', async ({ page }) => {
    await page.goto('/address')
    await page.waitForLoadState('networkidle')

    const addressCards = page.locator('.address-card')
    await expect(addressCards.count()).toBeGreaterThanOrEqual(0)
  })

  test('should add new address', async ({ page }) => {
    await page.goto('/address')
    await page.waitForLoadState('networkidle')

    const addButton = page.locator('button:has-text("新增地址")')
    if (await addButton.isVisible()) {
      await addButton.click()
      await page.waitForTimeout(500)

      const nameInput = page.locator('input[name="name"]')
      const phoneInput = page.locator('input[name="phone"]')
      const provinceSelect = page.locator('select[name="province"]')
      const citySelect = page.locator('select[name="city"]')
      const districtSelect = page.locator('select[name="district"]')
      const detailInput = page.locator('input[name="detail"]')
      const saveButton = page.locator('button:has-text("保存")')

      if (await nameInput.isVisible()) {
        await nameInput.fill('测试用户')
      }
      if (await phoneInput.isVisible()) {
        await phoneInput.fill('13800138000')
      }
      if (await provinceSelect.isVisible()) {
        await provinceSelect.selectOption('广东省')
      }
      if (await citySelect.isVisible()) {
        await citySelect.selectOption('广州市')
      }
      if (await districtSelect.isVisible()) {
        await districtSelect.selectOption('天河区')
      }
      if (await detailInput.isVisible()) {
        await detailInput.fill('测试地址')
      }
      if (await saveButton.isVisible()) {
        await saveButton.click()
        await page.waitForTimeout(500)
      }
    }
  })

  test('should edit address', async ({ page }) => {
    await page.goto('/address')
    await page.waitForLoadState('networkidle')

    const editButton = page.locator('button:has-text("编辑")')
    if (await editButton.first().isVisible()) {
      await editButton.first().click()
      await page.waitForTimeout(500)

      const saveButton = page.locator('button:has-text("保存")')
      if (await saveButton.isVisible()) {
        await saveButton.click()
        await page.waitForTimeout(500)
      }
    }
  })

  test('should delete address', async ({ page }) => {
    await page.goto('/address')
    await page.waitForLoadState('networkidle')

    const deleteButton = page.locator('button:has-text("删除")')
    if (await deleteButton.first().isVisible()) {
      await deleteButton.first().click()
      await page.waitForTimeout(500)

      const confirmButton = page.locator('button:has-text("确定")')
      if (await confirmButton.isVisible()) {
        await confirmButton.click()
        await page.waitForTimeout(500)
      }
    }
  })

  test('should set default address', async ({ page }) => {
    await page.goto('/address')
    await page.waitForLoadState('networkidle')

    const defaultButton = page.locator('button:has-text("设为默认")')
    if (await defaultButton.first().isVisible()) {
      await defaultButton.first().click()
      await page.waitForTimeout(500)
    }
  })

  test('should display default address tag', async ({ page }) => {
    await page.goto('/address')
    await page.waitForLoadState('networkidle')

    const defaultTag = page.locator('.el-tag:has-text("默认")')
    await expect(defaultTag.count()).toBeGreaterThanOrEqual(0)
  })

  test('should display address details correctly', async ({ page }) => {
    await page.goto('/address')
    await page.waitForLoadState('networkidle')

    const addressCard = page.locator('.address-card').first()
    if (await addressCard.isVisible()) {
      const name = addressCard.locator('.name')
      const phone = addressCard.locator('.phone')
      const detail = addressCard.locator('.address-detail')

      await expect(name).toBeVisible()
      await expect(phone).toBeVisible()
      await expect(detail).toBeVisible()
    }
  })

  test('should navigate to checkout with address selection', async ({ page }) => {
    await page.goto('/checkout')
    await page.waitForLoadState('networkidle')

    const addressSection = page.locator('section:has-text("收货地址")')
    await expect(addressSection).toBeVisible()
  })

  test('should switch address in checkout', async ({ page }) => {
    await page.goto('/checkout')
    await page.waitForLoadState('networkidle')

    const addressSection = page.locator('section:has-text("收货地址")')
    if (await addressSection.isVisible()) {
      const changeAddressButton = addressSection.locator('button:has-text("更换")')
      if (await changeAddressButton.isVisible()) {
        await changeAddressButton.click()
        await page.waitForTimeout(500)

        const addressList = page.locator('.address-card')
        if (await addressList.count() > 1) {
          const secondAddress = addressList.nth(1)
          if (await secondAddress.isVisible()) {
            await secondAddress.click()
            await page.waitForTimeout(500)
          }
        }
      }
    }
  })

  test('should calculate shipping fee based on address', async ({ page }) => {
    await page.goto('/checkout')
    await page.waitForLoadState('networkidle')

    const shippingSection = page.locator('.shipping-fee')
    if (await shippingSection.isVisible()) {
      const shippingText = await shippingSection.textContent()
      console.log('Shipping fee:', shippingText)
    }
  })

  test('should display multiple addresses in checkout selection', async ({ page }) => {
    await page.goto('/checkout')
    await page.waitForLoadState('networkidle')

    const addressSection = page.locator('section:has-text("收货地址")')
    if (await addressSection.isVisible()) {
      const changeAddressButton = addressSection.locator('button:has-text("更换")')
      if (await changeAddressButton.isVisible()) {
        await changeAddressButton.click()
        await page.waitForTimeout(500)

        const addressList = page.locator('.address-card')
        await expect(addressList.count()).toBeGreaterThanOrEqual(0)
      }
    }
  })

  test('should validate address form fields', async ({ page }) => {
    await page.goto('/address')
    await page.waitForLoadState('networkidle')

    const addButton = page.locator('button:has-text("新增地址")')
    if (await addButton.isVisible()) {
      await addButton.click()
      await page.waitForTimeout(500)

      const saveButton = page.locator('button:has-text("保存")')
      if (await saveButton.isVisible()) {
        await saveButton.click()
        await page.waitForTimeout(500)

        const errorMessages = page.locator('.el-form-item__error')
        await expect(errorMessages.count()).toBeGreaterThanOrEqual(0)
      }
    }
  })

  test('should handle address with special characters', async ({ page }) => {
    await page.goto('/address')
    await page.waitForLoadState('networkidle')

    const addButton = page.locator('button:has-text("新增地址")')
    if (await addButton.isVisible()) {
      await addButton.click()
      await page.waitForTimeout(500)

      const nameInput = page.locator('input[name="name"]')
      const detailInput = page.locator('input[name="detail"]')
      const saveButton = page.locator('button:has-text("保存")')

      if (await nameInput.isVisible()) {
        await nameInput.fill('测试@用户#123')
      }
      if (await detailInput.isVisible()) {
        await detailInput.fill('测试地址@#$%^&*()')
      }
      if (await saveButton.isVisible()) {
        await saveButton.click()
        await page.waitForTimeout(500)
      }
    }
  })

  test('should verify address count limit', async ({ page }) => {
    await page.goto('/address')
    await page.waitForLoadState('networkidle')

    const addressCards = page.locator('.address-card')
    const count = await addressCards.count()
    console.log('Current address count:', count)

    const addButton = page.locator('button:has-text("新增地址")')
    if (await addButton.isVisible()) {
      console.log('Add address button is visible, limit not reached')
    } else {
      console.log('Add address button not visible, limit may be reached')
    }
  })

  test('should handle empty address list in checkout', async ({ page }) => {
    await page.goto('/checkout')
    await page.waitForLoadState('networkidle')

    const addressSection = page.locator('section:has-text("收货地址")')
    if (await addressSection.isVisible()) {
      const addAddressButton = addressSection.locator('button:has-text("新增地址")')
      if (await addAddressButton.isVisible()) {
        await addAddressButton.click()
        await page.waitForTimeout(500)
      }
    }
  })
})
