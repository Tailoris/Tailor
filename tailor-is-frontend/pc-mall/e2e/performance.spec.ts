import { test, expect } from '@playwright/test'
import fs from 'fs'
import path from 'path'

const PERFORMANCE_THRESHOLDS = {
  FCP: 1800,
  LCP: 2500,
  CLS: 0.25,
  FID: 100,
  TTI: 3500,
  loadTime: 3000,
  totalSize: 2000 * 1024,
  avgDuration: 500
}

const BASELINE_FILE = path.join(__dirname, './baselines/performance-baseline.json')
const CURRENT_RESULTS_FILE = path.join(__dirname, '../test-results/performance-current.json')

interface PerformanceMetrics {
  page: string
  fcp: number
  lcp: number
  cls: number
  fid: number
  tti: number
  loadTime: number
  totalRequests: number
  totalSizeKB: number
  avgDurationMs: number
  timestamp: string
}

async function getPerformanceMetrics(page: any): Promise<{ fcp: number; lcp: number; cls: number; fid: number; tti: number }> {
  return page.evaluate(() => {
    return new Promise((resolve) => {
      const observer = new PerformanceObserver((entryList) => {
        const entries = entryList.getEntries()
        const lcpEntry = entries.find((e: any) => e.entryType === 'largest-contentful-paint')
        if (lcpEntry) {
          observer.disconnect()
          
          const perfData: any = {
            fcp: performance.getEntriesByType('paint').find((e: any) => e.name === 'first-contentful-paint')?.startTime || 0,
            lcp: lcpEntry.startTime || 0,
            cls: 0,
            fid: 0,
            tti: performance.now()
          }
          
          const clsEntries = performance.getEntriesByType('layout-shift')
          if (clsEntries.length > 0) {
            perfData.cls = clsEntries.reduce((sum: number, entry: any) => sum + entry.value, 0)
          }
          
          const fidEntries = performance.getEntriesByType('first-input')
          if (fidEntries.length > 0) {
            perfData.fid = fidEntries[0].startTime || 0
          }
          
          resolve(perfData)
        }
      })
      
      observer.observe({ type: 'largest-contentful-paint', buffered: true })
      
      setTimeout(() => {
        observer.disconnect()
        const perfData: any = {
          fcp: performance.getEntriesByType('paint').find((e: any) => e.name === 'first-contentful-paint')?.startTime || 0,
          lcp: performance.getEntriesByType('largest-contentful-paint').pop()?.startTime || 0,
          cls: 0,
          fid: 0,
          tti: performance.now()
        }
        
        const clsEntries = performance.getEntriesByType('layout-shift')
        if (clsEntries.length > 0) {
          perfData.cls = clsEntries.reduce((sum: number, entry: any) => sum + entry.value, 0)
        }
        
        const fidEntries = performance.getEntriesByType('first-input')
        if (fidEntries.length > 0) {
          perfData.fid = fidEntries[0].startTime || 0
        }
        
        resolve(perfData)
      }, 8000)
    })
  })
}

async function getNetworkMetrics(page: any): Promise<{ totalRequests: number; totalSizeKB: number; avgDurationMs: number }> {
  const requests: Array<{ duration: number; size: number }> = []
  
  page.on('requestfinished', (request: any) => {
    const response = request.response()
    if (response) {
      requests.push({
        duration: response.request().timing()?.responseEnd - (response.request().timing()?.requestStart || 0) || 0,
        size: response.headers()['content-length'] ? parseInt(response.headers()['content-length'] || '0') : 0
      })
    }
  })
  
  return new Promise((resolve) => {
    setTimeout(() => {
      const totalSize = requests.reduce((sum, r) => sum + r.size, 0)
      const avgDuration = requests.length > 0 ? requests.reduce((sum, r) => sum + r.duration, 0) / requests.length : 0
      
      resolve({
        totalRequests: requests.length,
        totalSizeKB: totalSize / 1024,
        avgDurationMs: avgDuration
      })
    }, 5000)
  })
}

function saveResults(results: PerformanceMetrics[], filePath: string) {
  const dir = path.dirname(filePath)
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true })
  }
  fs.writeFileSync(filePath, JSON.stringify(results, null, 2))
}

function loadBaseline(): PerformanceMetrics[] {
  if (fs.existsSync(BASELINE_FILE)) {
    return JSON.parse(fs.readFileSync(BASELINE_FILE, 'utf-8'))
  }
  return []
}

function compareWithBaseline(current: PerformanceMetrics[], baseline: PerformanceMetrics[]): void {
  const degradationThreshold = 0.2
  
  console.log('\n=== Performance Comparison Report ===')
  
  for (const currentResult of current) {
    const baselineResult = baseline.find(b => b.page === currentResult.page)
    
    if (baselineResult) {
      console.log(`\nPage: ${currentResult.page}`)
      console.log('Metric\t\t\tBaseline\tCurrent\t\tChange\t\tStatus')
      
      const metrics: Array<{ name: string; current: number; baseline: number; unit: string }> = [
        { name: 'FCP', current: currentResult.fcp, baseline: baselineResult.fcp, unit: 'ms' },
        { name: 'LCP', current: currentResult.lcp, baseline: baselineResult.lcp, unit: 'ms' },
        { name: 'CLS', current: currentResult.cls, baseline: baselineResult.cls, unit: '' },
        { name: 'FID', current: currentResult.fid, baseline: baselineResult.fid, unit: 'ms' },
        { name: 'TTI', current: currentResult.tti, baseline: baselineResult.tti, unit: 'ms' },
        { name: 'LoadTime', current: currentResult.loadTime, baseline: baselineResult.loadTime, unit: 'ms' },
      ]
      
      let hasDegradation = false
      
      for (const metric of metrics) {
        const change = ((metric.current - metric.baseline) / metric.baseline * 100).toFixed(1)
        const isDegraded = metric.current > metric.baseline * (1 + degradationThreshold) && metric.baseline > 0
        const status = isDegraded ? '⚠️ DEGRADED' : '✓ OK'
        
        if (isDegraded) hasDegradation = true
        
        console.log(`${metric.name.padEnd(15)}\t${metric.baseline.toFixed(2).padEnd(12)}${metric.unit}\t${metric.current.toFixed(2).padEnd(12)}${metric.unit}\t${change.padEnd(8)}%\t${status}`)
      }
      
      if (hasDegradation) {
        console.log(`\n⚠️ WARNING: Performance degradation detected for ${currentResult.page}!`)
      }
    } else {
      console.log(`\nNo baseline data for ${currentResult.page}. Creating new baseline.`)
    }
  }
}

test.describe('Performance Benchmark', () => {
  const allResults: PerformanceMetrics[] = []

  test.afterAll(() => {
    saveResults(allResults, CURRENT_RESULTS_FILE)
    
    const baseline = loadBaseline()
    if (baseline.length === 0) {
      console.log('Creating initial performance baseline...')
      saveResults(allResults, BASELINE_FILE)
    } else {
      compareWithBaseline(allResults, baseline)
    }
  })

  test('homepage performance metrics', async ({ page }) => {
    const startTime = Date.now()
    await page.goto('/', { waitUntil: 'networkidle' })
    
    const [metrics, networkMetrics] = await Promise.all([
      getPerformanceMetrics(page),
      getNetworkMetrics(page)
    ])
    
    const result: PerformanceMetrics = {
      page: 'Homepage',
      ...metrics,
      loadTime: Date.now() - startTime,
      ...networkMetrics,
      timestamp: new Date().toISOString()
    }
    
    allResults.push(result)
    
    console.log('Homepage Performance Metrics:', {
      FCP: `${metrics.fcp.toFixed(2)}ms`,
      LCP: `${metrics.lcp.toFixed(2)}ms`,
      CLS: metrics.cls.toFixed(3),
      FID: `${metrics.fid.toFixed(2)}ms`,
      TTI: `${metrics.tti.toFixed(2)}ms`,
      LoadTime: `${result.loadTime.toFixed(2)}ms`,
      TotalRequests: networkMetrics.totalRequests,
      TotalSizeKB: networkMetrics.totalSizeKB.toFixed(2),
      AvgDurationMs: networkMetrics.avgDurationMs.toFixed(2)
    })

    expect(metrics.fcp).toBeLessThan(PERFORMANCE_THRESHOLDS.FCP)
    expect(metrics.lcp).toBeLessThan(PERFORMANCE_THRESHOLDS.LCP)
    expect(metrics.cls).toBeLessThan(PERFORMANCE_THRESHOLDS.CLS)
  })

  test('product page performance metrics', async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    
    const productItem = page.locator('.product-item').first()
    if (await productItem.isVisible()) {
      const startTime = Date.now()
      await productItem.click()
      await page.waitForURL(/\/product\//)
      
      const [metrics, networkMetrics] = await Promise.all([
        getPerformanceMetrics(page),
        getNetworkMetrics(page)
      ])
      
      const result: PerformanceMetrics = {
        page: 'ProductPage',
        ...metrics,
        loadTime: Date.now() - startTime,
        ...networkMetrics,
        timestamp: new Date().toISOString()
      }
      
      allResults.push(result)
      
      console.log('Product Page Performance Metrics:', {
        FCP: `${metrics.fcp.toFixed(2)}ms`,
        LCP: `${metrics.lcp.toFixed(2)}ms`,
        CLS: metrics.cls.toFixed(3),
        TTI: `${metrics.tti.toFixed(2)}ms`,
        LoadTime: `${result.loadTime.toFixed(2)}ms`
      })

      expect(metrics.fcp).toBeLessThan(PERFORMANCE_THRESHOLDS.FCP)
      expect(metrics.lcp).toBeLessThan(PERFORMANCE_THRESHOLDS.LCP)
      expect(metrics.cls).toBeLessThan(PERFORMANCE_THRESHOLDS.CLS)
    }
  })

  test('category page performance metrics', async ({ page }) => {
    const startTime = Date.now()
    await page.goto('/products')
    await page.waitForLoadState('networkidle')
    
    const [metrics, networkMetrics] = await Promise.all([
      getPerformanceMetrics(page),
      getNetworkMetrics(page)
    ])
    
    const result: PerformanceMetrics = {
      page: 'CategoryPage',
      ...metrics,
      loadTime: Date.now() - startTime,
      ...networkMetrics,
      timestamp: new Date().toISOString()
    }
    
    allResults.push(result)
    
    console.log('Category Page Performance Metrics:', {
      FCP: `${metrics.fcp.toFixed(2)}ms`,
      LCP: `${metrics.lcp.toFixed(2)}ms`,
      CLS: metrics.cls.toFixed(3),
      TTI: `${metrics.tti.toFixed(2)}ms`,
      LoadTime: `${result.loadTime.toFixed(2)}ms`
    })

    expect(metrics.fcp).toBeLessThan(PERFORMANCE_THRESHOLDS.FCP)
    expect(metrics.lcp).toBeLessThan(PERFORMANCE_THRESHOLDS.LCP)
    expect(metrics.cls).toBeLessThan(PERFORMANCE_THRESHOLDS.CLS)
  })

  test('cart page performance metrics', async ({ page }) => {
    const startTime = Date.now()
    await page.goto('/cart')
    await page.waitForLoadState('networkidle')
    
    const [metrics, networkMetrics] = await Promise.all([
      getPerformanceMetrics(page),
      getNetworkMetrics(page)
    ])
    
    const result: PerformanceMetrics = {
      page: 'CartPage',
      ...metrics,
      loadTime: Date.now() - startTime,
      ...networkMetrics,
      timestamp: new Date().toISOString()
    }
    
    allResults.push(result)
    
    console.log('Cart Page Performance Metrics:', {
      FCP: `${metrics.fcp.toFixed(2)}ms`,
      LCP: `${metrics.lcp.toFixed(2)}ms`,
      CLS: metrics.cls.toFixed(3),
      TTI: `${metrics.tti.toFixed(2)}ms`,
      LoadTime: `${result.loadTime.toFixed(2)}ms`
    })

    expect(metrics.fcp).toBeLessThan(PERFORMANCE_THRESHOLDS.FCP)
    expect(metrics.lcp).toBeLessThan(PERFORMANCE_THRESHOLDS.LCP)
    expect(metrics.cls).toBeLessThan(PERFORMANCE_THRESHOLDS.CLS)
  })

  test('checkout page performance metrics', async ({ page }) => {
    const startTime = Date.now()
    await page.goto('/checkout')
    await page.waitForLoadState('networkidle')
    
    const [metrics, networkMetrics] = await Promise.all([
      getPerformanceMetrics(page),
      getNetworkMetrics(page)
    ])
    
    const result: PerformanceMetrics = {
      page: 'CheckoutPage',
      ...metrics,
      loadTime: Date.now() - startTime,
      ...networkMetrics,
      timestamp: new Date().toISOString()
    }
    
    allResults.push(result)
    
    console.log('Checkout Page Performance Metrics:', {
      FCP: `${metrics.fcp.toFixed(2)}ms`,
      LCP: `${metrics.lcp.toFixed(2)}ms`,
      CLS: metrics.cls.toFixed(3),
      TTI: `${metrics.tti.toFixed(2)}ms`,
      LoadTime: `${result.loadTime.toFixed(2)}ms`
    })

    expect(metrics.fcp).toBeLessThan(PERFORMANCE_THRESHOLDS.FCP)
    expect(metrics.lcp).toBeLessThan(PERFORMANCE_THRESHOLDS.LCP)
    expect(metrics.cls).toBeLessThan(PERFORMANCE_THRESHOLDS.CLS)
  })

  test('network requests should be efficient', async ({ page }) => {
    const requests: Array<{ url: string; duration: number; size: number }> = []
    
    page.on('requestfinished', (request: any) => {
      const response = request.response()
      if (response) {
        requests.push({
          url: request.url(),
          duration: response.request().timing()?.responseEnd - (response.request().timing()?.requestStart || 0) || 0,
          size: response.headers()['content-length'] ? parseInt(response.headers()['content-length'] || '0') : 0
        })
      }
    })
    
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    
    const totalSize = requests.reduce((sum, r) => sum + r.size, 0)
    const avgDuration = requests.length > 0 ? requests.reduce((sum, r) => sum + r.duration, 0) / requests.length : 0
    
    console.log('Network Performance:', {
      totalRequests: requests.length,
      totalSizeKB: (totalSize / 1024).toFixed(2),
      avgDurationMs: avgDuration.toFixed(2)
    })
    
    expect(totalSize).toBeLessThan(PERFORMANCE_THRESHOLDS.totalSize)
    expect(avgDuration).toBeLessThan(PERFORMANCE_THRESHOLDS.avgDuration)
  })
})
