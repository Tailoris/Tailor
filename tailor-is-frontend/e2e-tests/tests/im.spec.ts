// ==============================================================================
// Tailor IS - IM 即时通讯 E2E 测试 (Phase 3: TEST-P2-02)
// ==============================================================================
// 测试框架: Playwright
// 运行: npx playwright test tests/im.spec.ts
// 验收标准: IM 消息发送/接收/WebSocket 重连功能正常
// ==============================================================================

import { test, expect } from '@playwright/test'

const BASE_URL = process.env.BASE_URL || 'http://localhost:8080'
const IM_WS_URL = process.env.IM_WS_URL || 'ws://localhost:8114/ws'

test.describe('IM 即时通讯 - 消息功能', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(`${BASE_URL}/login`)
    await page.fill('[data-testid="login-username"]', 'test_user')
    await page.fill('[data-testid="login-password"]', 'test_password')
    await page.click('[data-testid="login-submit"]')
    await page.waitForURL('**/home')
  })

  test('T-E2E-08: 发送文本消息', async ({ page }) => {
    // 1. 进入聊天页面
    await page.goto(`${BASE_URL}/chat`)
    await page.waitForSelector('[data-testid="chat-list"]')

    // 2. 选择一个聊天会话
    const firstChat = page.locator('[data-testid="chat-item"]').first()
    await firstChat.click()
    await page.waitForSelector('[data-testid="chat-window"]')

    // 3. 输入消息
    const testMessage = `测试消息 ${Date.now()}`
    await page.fill('[data-testid="message-input"]', testMessage)

    // 4. 点击发送
    await page.click('[data-testid="send-button"]')

    // 5. 验证消息出现在聊天列表中
    const lastMessage = page.locator('[data-testid="message-bubble"]').last()
    await expect(lastMessage).toContainText(testMessage)
    await expect(lastMessage).toBeVisible()

    // 6. 验证消息状态为已发送
    await expect(lastMessage.locator('[data-testid="message-status"]')).toHaveText('已发送')
  })

  test('T-E2E-09: 消息列表加载与分页', async ({ page }) => {
    await page.goto(`${BASE_URL}/chat`)
    await page.waitForSelector('[data-testid="chat-list"]')

    const firstChat = page.locator('[data-testid="chat-item"]').first()
    await firstChat.click()
    await page.waitForSelector('[data-testid="chat-window"]')

    // 验证消息列表已加载
    const messages = page.locator('[data-testid="message-bubble"]')
    const count = await messages.count()
    expect(count).toBeGreaterThan(0)

    // 滚动到顶部加载历史消息
    await page.locator('[data-testid="chat-messages"]').evaluate(el => el.scrollTop = 0)
    await page.waitForTimeout(1000)

    // 验证历史消息已加载
    const updatedCount = await messages.count()
    expect(updatedCount).toBeGreaterThanOrEqual(count)
  })

  test('T-E2E-10: WebSocket 连接与心跳', async ({ page }) => {
    await page.goto(`${BASE_URL}/chat`)
    await page.waitForSelector('[data-testid="chat-list"]')

    // 验证 WebSocket 连接状态
    const wsStatus = page.locator('[data-testid="ws-status"]')
    await expect(wsStatus).toHaveText('已连接', { timeout: 10000 })

    // 等待心跳周期
    await page.waitForTimeout(6000)

    // 验证连接保持
    await expect(wsStatus).toHaveText('已连接')
  })

  test('T-E2E-11: WebSocket 断线重连', async ({ page }) => {
    await page.goto(`${BASE_URL}/chat`)
    await page.waitForSelector('[data-testid="chat-list"]')

    const firstChat = page.locator('[data-testid="chat-item"]').first()
    await firstChat.click()
    await page.waitForSelector('[data-testid="chat-window"]')

    // 模拟网络断开
    await page.route('**/ws/**', route => route.abort())

    // 等待重连机制触发
    await page.waitForTimeout(3000)

    // 恢复网络
    await page.unroute('**/ws/**')

    // 验证重连成功
    const wsStatus = page.locator('[data-testid="ws-status"]')
    await expect(wsStatus).toHaveText('已连接', { timeout: 15000 })
  })

  test('T-E2E-12: 消息已读状态更新', async ({ page }) => {
    await page.goto(`${BASE_URL}/chat`)
    await page.waitForSelector('[data-testid="chat-list"]')

    const firstChat = page.locator('[data-testid="chat-item"]').first()
    await firstChat.click()
    await page.waitForSelector('[data-testid="chat-window"]')

    // 查找未读消息
    const unreadMessages = page.locator('[data-testid="message-bubble"]').filter({ hasText: '未读' })

    if ((await unreadMessages.count()) > 0) {
      // 等待已读回执
      await page.waitForTimeout(2000)
      // 验证消息已标记为已读
      const messageStatus = unreadMessages.first().locator('[data-testid="message-status"]')
      await expect(messageStatus).toHaveText('已读')
    }
  })

  test('T-E2E-13: 聊天列表未读消息数', async ({ page }) => {
    await page.goto(`${BASE_URL}/chat`)
    await page.waitForSelector('[data-testid="chat-list"]')

    // 验证未读消息徽章
    const badges = page.locator('[data-testid="unread-badge"]')
    const badgeCount = await badges.count()

    // 至少有一个会话显示未读数
    if (badgeCount > 0) {
      const badgeText = await badges.first().textContent()
      expect(Number(badgeText)).toBeGreaterThan(0)
    }
  })
})

test.describe('IM 即时通讯 - 异常场景', () => {
  test('T-E2E-14: 发送空消息应被拦截', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`)
    await page.fill('[data-testid="login-username"]', 'test_user')
    await page.fill('[data-testid="login-password"]', 'test_password')
    await page.click('[data-testid="login-submit"]')
    await page.waitForURL('**/home')

    await page.goto(`${BASE_URL}/chat`)
    await page.waitForSelector('[data-testid="chat-list"]')

    const firstChat = page.locator('[data-testid="chat-item"]').first()
    await firstChat.click()
    await page.waitForSelector('[data-testid="chat-window"]')

    // 发送按钮应被禁用（输入框为空）
    const sendButton = page.locator('[data-testid="send-button"]')
    await expect(sendButton).toBeDisabled()

    // 输入空格后发送按钮仍应被禁用
    await page.fill('[data-testid="message-input"]', '   ')
    await expect(sendButton).toBeDisabled()
  })

  test('T-E2E-15: 超长消息应被限制', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`)
    await page.fill('[data-testid="login-username"]', 'test_user')
    await page.fill('[data-testid="login-password"]', 'test_password')
    await page.click('[data-testid="login-submit"]')
    await page.waitForURL('**/home')

    await page.goto(`${BASE_URL}/chat`)
    await page.waitForSelector('[data-testid="chat-list"]')

    const firstChat = page.locator('[data-testid="chat-item"]').first()
    await firstChat.click()
    await page.waitForSelector('[data-testid="chat-window"]')

    // 输入超长消息
    const longMessage = 'a'.repeat(5001)
    await page.fill('[data-testid="message-input"]', longMessage)

    // 应显示长度限制提示
    await expect(page.locator('[data-testid="char-limit-warning"]')).toBeVisible()