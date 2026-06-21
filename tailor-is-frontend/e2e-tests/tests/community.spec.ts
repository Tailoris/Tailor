import { test, expect } from '@playwright/test'

/**
 * 社区功能 E2E 测试 - TEST-P2-02.
 *
 * <p>覆盖社区动态的核心功能：帖子列表、点赞、评论、发布动态、分页等。</p>
 */

test.describe('社区 - 帖子列表', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/community')
  })

  test('社区页面加载正常', async ({ page }) => {
    await expect(page).toHaveURL(/\/community/)
    await expect(page.locator('h2.page-title')).toContainText('社区动态')
  })

  test('社区页面显示面包屑导航', async ({ page }) => {
    const breadcrumb = page.locator('.el-breadcrumb')
    await expect(breadcrumb).toBeVisible()

    const homeLink = breadcrumb.locator('.el-breadcrumb__item', { hasText: '首页' })
    await expect(homeLink).toBeVisible()

    const communityItem = breadcrumb.locator('.el-breadcrumb__item', { hasText: '社区' })
    await expect(communityItem).toBeVisible()
  })

  test('社区页面显示发布动态按钮', async ({ page }) => {
    const publishBtn = page.locator('.community-header button', { hasText: '发布动态' })
    await expect(publishBtn).toBeVisible()
  })

  test('社区页面显示帖子列表或空状态', async ({ page }) => {
    await page.waitForLoadState('networkidle')

    const posts = page.locator('.post-card')
    const empty = page.locator('.el-empty')

    const hasPosts = await posts.first().isVisible().catch(() => false)
    const isEmpty = await empty.isVisible().catch(() => false)

    expect(hasPosts || isEmpty).toBeTruthy()
  })
})

test.describe('社区 - 帖子交互', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/community')
  })

  test('帖子卡片包含用户信息', async ({ page }) => {
    await page.waitForLoadState('networkidle')

    const firstPost = page.locator('.post-card').first()
    const hasPost = await firstPost.isVisible().catch(() => false)

    if (hasPost) {
      await expect(firstPost.locator('.post-user-info')).toBeVisible()
      await expect(firstPost.locator('.user-name')).toBeVisible()
      await expect(firstPost.locator('.post-time')).toBeVisible()
    }
  })

  test('帖子卡片包含标题和内容', async ({ page }) => {
    await page.waitForLoadState('networkidle')

    const firstPost = page.locator('.post-card').first()
    const hasPost = await firstPost.isVisible().catch(() => false)

    if (hasPost) {
      await expect(firstPost.locator('.post-title')).toBeVisible()
      await expect(firstPost.locator('.post-content')).toBeVisible()
    }
  })

  test('帖子卡片包含操作按钮（点赞、评论、浏览）', async ({ page }) => {
    await page.waitForLoadState('networkidle')

    const firstPost = page.locator('.post-card').first()
    const hasPost = await firstPost.isVisible().catch(() => false)

    if (hasPost) {
      await expect(firstPost.locator('.post-actions')).toBeVisible()
      const actionBtns = firstPost.locator('.post-actions .action-btn')
      const count = await actionBtns.count()
      expect(count).toBeGreaterThanOrEqual(3)
    }
  })

  test('点赞按钮可点击', async ({ page }) => {
    await page.waitForLoadState('networkidle')

    const firstPost = page.locator('.post-card').first()
    const hasPost = await firstPost.isVisible().catch(() => false)

    if (hasPost) {
      const likeBtn = firstPost.locator('.action-btn').first()
      await expect(likeBtn).toBeVisible()
      await likeBtn.click()
      await page.waitForTimeout(300)
    }
  })

  test('评论按钮展开评论区域', async ({ page }) => {
    await page.waitForLoadState('networkidle')

    const firstPost = page.locator('.post-card').first()
    const hasPost = await firstPost.isVisible().catch(() => false)

    if (hasPost) {
      const commentBtn = firstPost.locator('.action-btn', { hasText: /^\d+$/ }).first()
      // 评论按钮是第二个 action-btn
      const actionBtns = firstPost.locator('.action-btn')
      const commentActionBtn = actionBtns.nth(1)

      const hasCommentBtn = await commentActionBtn.isVisible().catch(() => false)
      if (hasCommentBtn) {
        await commentActionBtn.click()
        await page.waitForTimeout(300)

        const commentSection = firstPost.locator('.comment-section')
        const hasCommentSection = await commentSection.isVisible().catch(() => false)
        expect(hasCommentSection).toBeTruthy()
      }
    }
  })
})

test.describe('社区 - 发布动态', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/community')
  })

  test('点击发布动态按钮打开对话框', async ({ page }) => {
    const publishBtn = page.locator('.community-header button', { hasText: '发布动态' })
    await publishBtn.click()

    const dialog = page.locator('.el-dialog')
    await expect(dialog).toBeVisible()
    await expect(dialog.locator('.el-dialog__title')).toContainText('发布动态')
  })

  test('发布动态对话框包含表单字段', async ({ page }) => {
    const publishBtn = page.locator('.community-header button', { hasText: '发布动态' })
    await publishBtn.click()

    const dialog = page.locator('.el-dialog')
    await expect(dialog).toBeVisible()

    // 检查表单字段存在
    const form = dialog.locator('.el-form')
    await expect(form).toBeVisible()
  })

  test('关闭发布动态对话框', async ({ page }) => {
    const publishBtn = page.locator('.community-header button', { hasText: '发布动态' })
    await publishBtn.click()

    const dialog = page.locator('.el-dialog')
    await expect(dialog).toBeVisible()

    // 关闭对话框
    await dialog.locator('.el-dialog__close').click()
    await page.waitForTimeout(300)

    const isDialogVisible = await dialog.isVisible().catch(() => false)
    expect(isDialogVisible).toBeFalsy()
  })
})

test.describe('社区 - 分页', () => {
  test('社区页面显示分页组件', async ({ page }) => {
    await page.goto('/community')
    await page.waitForLoadState('networkidle')

    const pagination = page.locator('.el-pagination, .pagination-wrapper')
    const hasPagination = await pagination.isVisible().catch(() => false)

    const empty = page.locator('.el-empty')
    const isEmpty = await empty.isVisible().catch(() => false)

    expect(hasPagination || isEmpty).toBeTruthy()
  })
})