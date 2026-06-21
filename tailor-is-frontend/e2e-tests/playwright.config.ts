import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright E2E测试配置 - UX-P3-02 多平台兼容性
 *
 * 支持多浏览器、多设备并行测试，覆盖 Chromium/Firefox/WebKit/Edge 等主流浏览器，
 * 以及 iPhone、Samsung Galaxy、iPad Pro 等移动设备。
 */
export default defineConfig({
  testDir: './tests',
  timeout: 30 * 1000,
  expect: {
    timeout: 5000
  },
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 1,
  workers: process.env.CI ? 1 : undefined,
  reporter: process.env.CI ? [['github'], ['html']] : 'list',
  use: {
    baseURL: process.env.BASE_URL || 'http://localhost:5173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    viewport: { width: 1280, height: 720 },
  },
  // UX-P3-02: 扩展多浏览器 + 多设备测试覆盖
  projects: [
    // 桌面浏览器
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    },
    {
      name: 'edge',
      use: { ...devices['Desktop Edge'] },
    },
    // 桌面视口
    {
      name: 'desktop-fullhd',
      use: { browserName: 'chromium', viewport: { width: 1920, height: 1080 } },
    },
    {
      name: 'desktop-hd',
      use: { browserName: 'chromium', viewport: { width: 1366, height: 768 } },
    },
    // 移动端 - iOS
    {
      name: 'mobile-iphone-14',
      use: { ...devices['iPhone 14'] },
    },
    {
      name: 'mobile-iphone-14-pro-max',
      use: { ...devices['iPhone 14 Pro Max'] },
    },
    {
      name: 'mobile-iphone-se',
      use: { ...devices['iPhone SE'] },
    },
    // 移动端 - Android
    {
      name: 'mobile-galaxy-s23',
      use: { ...devices['Galaxy S9+'] },  // Closest available preset
    },
    {
      name: 'mobile-pixel-7',
      use: { ...devices['Pixel 7'] },
    },
    // 平板
    {
      name: 'tablet-ipad-pro',
      use: { ...devices['iPad Pro'] },
    },
    {
      name: 'tablet-ipad-mini',
      use: { ...devices['iPad Mini'] },
    },
    {
      name: 'tablet-galaxy-tab',
      use: { ...devices['Galaxy Tab S4'] },
    },
  ],
  webServer: process.env.CI ? undefined : {
    command: 'npm run dev --prefix ../pc-mall',
    port: 3000,
    reuseExistingServer: !process.env.CI,
  },
});