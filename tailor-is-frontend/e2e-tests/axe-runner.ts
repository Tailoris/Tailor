/**
 * Axe-core Playwright 集成运行器 - UX-P3-01
 *
 * 封装 axe-core 的 Playwright 集成，提供自动扫描页面并报告违规的功能。
 * 支持自定义规则排除和分级报告。
 */

import AxeBuilder from '@axe-core/playwright';
import type { Page } from '@playwright/test';

export interface AxeScanOptions {
  /** 要包含的 WCAG 标签 */
  tags?: string[];
  /** 要排除的规则 ID 列表 */
  excludeRules?: string[];
  /** 是否包含 passing 的结果 */
  includePasses?: boolean;
  /** 是否在控制台输出违规详情 */
  logViolations?: boolean;
}

export interface AxeScanResult {
  /** 违规总数 */
  violationCount: number;
  /** 按严重程度分组的违规 */
  violationsByImpact: {
    critical: number;
    serious: number;
    moderate: number;
    minor: number;
  };
  /** 所有违规详情 */
  violations: ViolationSummary[];
  /** 是否通过 (无 critical 违规) */
  passed: boolean;
}

export interface ViolationSummary {
  id: string;
  impact: string;
  description: string;
  helpUrl: string;
  nodeCount: number;
  nodes: string[];
}

/**
 * 扫描页面无障碍合规性
 * @param page - Playwright Page 对象
 * @param options - 扫描选项
 * @returns 扫描结果
 */
export async function scanPageForA11y(
  page: Page,
  options: AxeScanOptions = {}
): Promise<AxeScanResult> {
  const {
    tags = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'],
    excludeRules = [],
    includePasses = false,
    logViolations = true,
  } = options;

  let builder = new AxeBuilder({ page }).withTags(tags);

  if (excludeRules.length > 0) {
    builder = builder.disableRules(excludeRules);
  }

  const results = await builder.analyze();

  const violations: ViolationSummary[] = results.violations.map((v) => ({
    id: v.id,
    impact: v.impact || 'minor',
    description: v.help,
    helpUrl: v.helpUrl,
    nodeCount: v.nodes.length,
    nodes: v.nodes.slice(0, 5).map((n) => n.target.join(', ')),
  }));

  const violationsByImpact = {
    critical: violations.filter((v) => v.impact === 'critical').length,
    serious: violations.filter((v) => v.impact === 'serious').length,
    moderate: violations.filter((v) => v.impact === 'moderate').length,
    minor: violations.filter((v) => v.impact === 'minor').length,
  };

  if (logViolations && violations.length > 0) {
    console.log(`\n=== A11y Scan: ${violations.length} violations found ===`);
    for (const v of violations) {
      console.log(`  [${v.impact.toUpperCase()}] ${v.id}: ${v.description}`);
      console.log(`    Nodes: ${v.nodeCount}, Help: ${v.helpUrl}`);
    }
  }

  return {
    violationCount: violations.length,
    violationsByImpact,
    violations,
    passed: violationsByImpact.critical === 0,
  };
}

/**
 * 多页面无障碍扫描
 * @param page - Playwright Page 对象
 * @param urls - 要扫描的页面 URL 列表
 * @param options - 扫描选项
 * @returns 各页面扫描结果
 */
export async function scanMultiplePages(
  page: Page,
  urls: { name: string; url: string }[],
  options: AxeScanOptions = {}
): Promise<Record<string, AxeScanResult>> {
  const results: Record<string, AxeScanResult> = {};

  for (const { name, url } of urls) {
    console.log(`\nScanning: ${name} (${url})`);
    await page.goto(url, { waitUntil: 'networkidle' });
    results[name] = await scanPageForA11y(page, options);
  }

  return results;
}

/**
 * 生成无障碍扫描报告摘要
 */
export function generateA11yReport(
  results: Record<string, AxeScanResult>
): string {
  let report = '# WCAG 2.1 AA 无障碍扫描报告\n\n';
  report += `## 扫描概览\n\n`;
  report += `| 页面 | 违规数 | Critical | Serious | Moderate | Minor | 状态 |\n`;
  report += `|------|--------|----------|---------|----------|-------|------|\n`;

  let totalViolations = 0;
  let totalPassed = 0;

  for (const [name, result] of Object.entries(results)) {
    const status = result.passed ? '✅' : '❌';
    if (result.passed) totalPassed++;
    totalViolations += result.violationCount;
    report += `| ${name} | ${result.violationCount} | ${result.violationsByImpact.critical} | ${result.violationsByImpact.serious} | ${result.violationsByImpact.moderate} | ${result.violationsByImpact.minor} | ${status} |\n`;
  }

  report += `\n## 总结\n`;
  report += `- 扫描页面数: ${Object.keys(results).length}\n`;
  report += `- 通过页面数: ${totalPassed}\n`;
  report += `- 总违规数: ${totalViolations}\n`;
  report += `- 通过率: ${((totalPassed / Object.keys(results).length) * 100).toFixed(1)}%\n`;

  return report;
}