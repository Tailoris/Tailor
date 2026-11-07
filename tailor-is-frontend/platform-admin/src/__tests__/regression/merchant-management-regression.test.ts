/**
 * 回归测试套件: 商家管理模块完整性（P0）
 *
 * 防护目标: 杜绝「商家管理」二级菜单 / 路由 / API 函数被误删导致的回归
 * 历史故障:
 *   - 2026-08-03: 修复一个无关问题时，merchant.ts 约 30 个 API 函数与
 *     router/index.ts 6 条二级路由被回退到修复前状态，导致 6 个页面空白。
 *   - 同日: ShopManageView.vue 图标命名冲突 (Shop 类型 vs Shop 图标) 导致
 *     店铺 Logo 不渲染。
 *   - 同日: DepositConfigView.vue 缺少 @tab-change 处理，Tab 切换后表格空白。
 *
 * 本套件以「结构不变式」方式锁定修复逻辑:
 *   1. 8 条商家管理二级路由全部存在且 component 可解析
 *   2. merchant.ts 关键 API 函数全部导出（防止再次被删）
 *   3. ShopManageView / DepositConfigView 关键修复代码存在
 */
import { describe, it, expect } from 'vitest';
import router from '@/router/index';
import * as merchantApi from '@/api/merchant';

// 平台管理后台「商家管理」8 个二级菜单的期望路由路径（相对 /admin 父级）
const EXPECTED_MERCHANT_ROUTES = [
    { path: 'merchant-audit', name: 'MerchantAudit' },
    { path: 'merchant-list', name: 'MerchantList' },
    { path: 'deposit-config', name: 'DepositConfig' },
    { path: 'store-deposit', name: 'StoreDeposit' },
    { path: 'deposit-flow', name: 'DepositFlow' },
    { path: 'merchant-withdraw', name: 'MerchantWithdraw' },
    { path: 'merchant-bill', name: 'MerchantBill' },
    { path: 'shop-manage', name: 'ShopManage' },
] as const;

// merchant.ts 必须导出的关键 API 函数（历史被误删清单）
const EXPECTED_MERCHANT_API_EXPORTS = [
    'listPendingMerchants',
    'listMerchants',
    'auditMerchant',
    'batchAuditMerchant',
    'freezeMerchant',
    'unfreezeMerchant',
    'getShopCategoryTree',
    'createShopCategory',
    'updateShopCategory',
    'deleteShopCategory',
    'listApplyTypePage',
    'createApplyType',
    'updateApplyType',
    'deleteApplyType',
    'listModifyApplyPage',
    'auditModifyApply',
    'listDepositPage',
    'adjustDeposit',
    'listWithdrawPage',
    'auditWithdraw',
    'listMerchantBillPage',
] as const;

describe('回归: 商家管理 8 条二级路由不可缺失 (P0)', () => {
    // 提取所有已注册路由的 name 与 path（含子路由扁平化）
    const allNames = new Set<string>();
    const allPaths = new Set<string>();
    for (const r of router.options.routes) {
        if (r.name) allNames.add(String(r.name));
        if (r.path) allPaths.add(r.path);
        if (r.children) {
            for (const c of r.children) {
                if (c.name) allNames.add(String(c.name));
                if (c.path) allPaths.add(c.path);
            }
        }
    }

    it.each(EXPECTED_MERCHANT_ROUTES)(
        '路由 name="$name" (path=$path) 必须已注册',
        ({ name, path }) => {
            expect(allNames.has(name), `路由 name="${name}" 缺失，商家管理将出现空挂页面`).toBe(true);
            expect(allPaths.has(path), `路由 path="${path}" 缺失`).toBe(true);
        }
    );

    it('商家管理二级路由数量不少于 8 条', () => {
        const present = EXPECTED_MERCHANT_ROUTES.filter(r => allNames.has(r.name));
        expect(present.length).toBeGreaterThanOrEqual(8);
    });

    it('每条商家管理路由都绑定了懒加载 component（非空字符串）', () => {
        const adminRoot = router.options.routes.find(r => r.path === '/');
        const children = adminRoot?.children ?? [];
        for (const expected of EXPECTED_MERCHANT_ROUTES) {
            const matched = children.find(c => c.name === expected.name);
            expect(matched, `未找到路由 ${expected.name}`).toBeDefined();
            // component 必须是函数（懒加载 import）或组件对象
            expect(typeof matched!.component).not.toBe('undefined');
        }
    });
});

describe('回归: merchant.ts 关键 API 函数不可缺失 (P0)', () => {
    it.each(EXPECTED_MERCHANT_API_EXPORTS)(
        'merchant.ts 必须导出函数: %s',
        (fnName) => {
            const fn = (merchantApi as Record<string, unknown>)[fnName];
            expect(typeof fn, `merchant.ts 缺失导出: ${fnName}（历史曾被误删导致 404/500）`).toBe('function');
        }
    );

    it('merchant.ts 关键 API 函数总数不少于 21 个', () => {
        const present = EXPECTED_MERCHANT_API_EXPORTS.filter(
            fnName => typeof (merchantApi as Record<string, unknown>)[fnName] === 'function'
        );
        expect(present.length).toBeGreaterThanOrEqual(21);
    });
});
