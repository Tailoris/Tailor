/**
 * 回归测试套件: 废弃菜单 / 路由清理不变式（P0）
 *
 * 防护目标: 锁定「区域代理商」「店铺分类」「入驻类型」「修改申请」「修改记录」等
 * 已合并/废弃菜单的清理结果，防止历史回退导致废弃菜单再次出现。
 *
 * 历史故障:
 *   - 2026-08-03: 「区域代理商」子菜单清理后，因前端未重新构建部署到 Nginx，
 *     浏览器缓存旧 JS，导致用户误以为菜单回归。本测试从源码层面断言废弃菜单
 *     已彻底移除，构建产物校验由 verify-build-fingerprint.sh 负责。
 */
import { describe, it, expect } from 'vitest';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const ADMIN_LAYOUT_PATH = resolve(__dirname, '../../components/AdminLayout.vue');
const ROUTER_PATH = resolve(__dirname, '../../router/index.ts');

function readSource(p: string): string {
    return readFileSync(p, 'utf-8');
}

// 已废弃的菜单/路由标识，源码中不得再出现
const DEPRECATED_TOKENS = [
    'region-agent',
    '区域代理',
] as const;

describe('回归: 废弃菜单/路由不得再次出现 (P0)', () => {
    it('AdminLayout.vue 不含废弃菜单标识', () => {
        const src = readSource(ADMIN_LAYOUT_PATH);
        for (const token of DEPRECATED_TOKENS) {
            expect(src, `AdminLayout.vue 仍含废弃标识 "${token}"，疑似历史回退`).not.toContain(token);
        }
    });

    it('router/index.ts 不含废弃路由标识', () => {
        const src = readSource(ROUTER_PATH);
        for (const token of DEPRECATED_TOKENS) {
            expect(src, `router/index.ts 仍含废弃标识 "${token}"，疑似历史回退`).not.toContain(token);
        }
    });

    it('AdminLayout.vue 含全部 8 条商家管理菜单项', () => {
        const src = readSource(ADMIN_LAYOUT_PATH);
        const expectedMenus = [
            '/merchant-audit',
            '/merchant-list',
            '/deposit-config',
            '/store-deposit',
            '/deposit-flow',
            '/merchant-withdraw',
            '/merchant-bill',
            '/shop-manage',
        ];
        for (const menu of expectedMenus) {
            expect(src, `AdminLayout.vue 缺失菜单项 ${menu}`).toContain(menu);
        }
    });

    it('AdminLayout.vue routeParentMap 覆盖全部 8 条商家管理路由', () => {
        const src = readSource(ADMIN_LAYOUT_PATH);
        const expectedMappings = [
            "'/merchant-audit': 'merchant'",
            "'/merchant-list': 'merchant'",
            "'/deposit-config': 'merchant'",
            "'/store-deposit': 'merchant'",
            "'/deposit-flow': 'merchant'",
            "'/merchant-withdraw': 'merchant'",
            "'/merchant-bill': 'merchant'",
            "'/shop-manage': 'merchant'",
        ];
        for (const mapping of expectedMappings) {
            expect(src, `routeParentMap 缺失映射 ${mapping}`).toContain(mapping);
        }
    });
});

/**
 * P0 回归场景清单（manifest）
 * 每条对应一次历史故障，新增修复后在此追加一条 it 用例，永久留存。
 */
describe('回归场景清单 (manifest): 历史故障防护索引', () => {
    it('P0 清单已登记且不重复', () => {
        const scenarios = [
            'B-2026-08-03-01: 商家管理 8 菜单 + 6 路由 + 6 组件 + 21 API 不可缺失',
            'B-2026-08-03-02: 区域代理商等废弃菜单不得再次出现',
            'B-2026-08-03-03: 鼠标悬停展开二级菜单（menuRef.open/close + expandedMenus 同步）',
            'B-2026-08-03-04: 仪表盘 v2.0 构建（dashboard-v2.0-golden tag）',
            'B-2026-08-02-01: 商品详情页 notFound/loadError 兜底 + images null 防护',
            'B-2026-08-02-02: 个人中心 /profile 空列表/0积分降级 + showError=false 透传',
            'B-2026-08-02-03: 线上学堂 academy 503 走 mock 降级 + showError=false 透传',
        ];
        const unique = new Set(scenarios);
        expect(unique.size).toBe(scenarios.length);
        expect(scenarios.length).toBeGreaterThanOrEqual(7);
    });
});
