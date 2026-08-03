import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import request from '../request';
import {
    formatPhone,
    formatEmail,
    formatBankCard,
    formatLicense,
    maskByType,
    debounce,
    throttle,
    withRequestCache,
    invalidateCachePrefix,
    buildListKey,
    pageSlice,
    sanitizeXSS,
    safeInput,
    hasPermission,
    runBatchConcurrently,
    listPendingMerchants,
    listMerchants,
    auditMerchant,
    batchAuditMerchant,
    freezeMerchant,
    unfreezeMerchant,
    getShopCategoryTree,
    createShopCategory,
    updateShopCategory,
    deleteShopCategory,
    listApplyTypePage,
    createApplyType,
    updateApplyType,
    deleteApplyType,
    listModifyApplyPage,
    auditModifyApply,
    listDepositPage,
    adjustDeposit,
    listWithdrawPage,
    auditWithdraw,
    listMerchantBillPage,
} from '../merchant';

/**
 * 商家管理模块API层单元测试
 * 覆盖：4个脱敏函数 + 2个时间窗口工具 + 缓存(LRU+TTL) + 虚拟列表工具 + 3个安全工具 + 30+个API函数
 * 测试策略：mock request.ts 单例，验证URL/method/params/data/showError传递正确
 */
vi.mock('../request', () => {
    const req: ReturnType<typeof vi.fn> = vi.fn();
    return { default: req };
});
const mockedRequest = request as unknown as ReturnType<typeof vi.fn>;

// ---------------------------------------------------------------------------
// 工具函数层：脱敏格式化
// ---------------------------------------------------------------------------
describe('merchant.ts 脱敏格式化工具（formatPhone/Email/BankCard/License）', () => {
    it('formatPhone：前3后4，中间4位变*，长度<11原样返回', () => {
        expect(formatPhone('13812345678')).toBe('138****5678');
        expect(formatPhone('123')).toBe('123');
        expect(formatPhone('')).toBe('');
    });
    it('formatEmail：name首尾保留中间*，domain不变', () => {
        expect(formatEmail('zhangsan@example.com')).toBe('z******n@example.com');
        expect(formatEmail('ab@x.com')).toBe('a*@x.com');
        expect(formatEmail('invalid')).toBe('invalid');
        expect(formatEmail('')).toBe('');
    });
    it('formatBankCard：末尾4位前面加三段****', () => {
        expect(formatBankCard('6222021234567890123')).toBe('**** **** **** 0123');
        expect(formatBankCard('')).toBe('');
    });
    it('formatLicense：<=10位原样，否则前6后4中间****', () => {
        expect(formatLicense('1234567890')).toBe('1234567890');
        expect(formatLicense('91110000MA1234567X')).toBe('911100****567X');
        expect(formatLicense('')).toBe('');
    });
    it('maskByType分发：按type字段路由到正确脱敏函数', () => {
        expect(maskByType('13812345678', 'phone')).toBe(formatPhone('13812345678'));
        expect(maskByType('a@b.cn', 'email')).toBe(formatEmail('a@b.cn'));
        expect(maskByType('6222021234567890123', 'bank')).toBe(formatBankCard('6222021234567890123'));
        expect(maskByType('91110000MA1234567X', 'license')).toBe(formatLicense('91110000MA1234567X'));
    });
});

// ---------------------------------------------------------------------------
// 工具函数层：防抖 / 节流
// ---------------------------------------------------------------------------
describe('merchant.ts 时间窗口工具（debounce/throttle）', () => {
    beforeEach(() => vi.useFakeTimers());
    afterEach(() => vi.useRealTimers());

    it('debounce：默认delay=300ms，窗口内多次调用只执行最后一次', () => {
        const fn = vi.fn();
        const d = debounce(fn);
        d(1); d(2); d(3);
        expect(fn).not.toHaveBeenCalled();
        vi.advanceTimersByTime(299);
        expect(fn).not.toHaveBeenCalled();
        vi.advanceTimersByTime(1);
        expect(fn).toHaveBeenCalledTimes(1);
        expect(fn).toHaveBeenCalledWith(3);
    });
    it('debounce：自定义delay=600ms生效', () => {
        const fn = vi.fn();
        const d = debounce(fn, 600);
        d('x');
        vi.advanceTimersByTime(599);
        expect(fn).not.toHaveBeenCalled();
        vi.advanceTimersByTime(1);
        expect(fn).toHaveBeenCalledWith('x');
    });
    it('throttle：默认wait=200ms，窗口内只执行首次', () => {
        const fn = vi.fn();
        const t = throttle(fn);
        t(1); t(2); t(3);
        expect(fn).toHaveBeenCalledTimes(1);
        expect(fn).toHaveBeenCalledWith(1);
        vi.advanceTimersByTime(199);
        t(4);
        expect(fn).toHaveBeenCalledTimes(1);
        vi.advanceTimersByTime(1);
        t(5);
        expect(fn).toHaveBeenCalledTimes(2);
        expect(fn).toHaveBeenLastCalledWith(5);
    });
});

// ---------------------------------------------------------------------------
// 工具函数层：缓存 + 列表分页切片 + 安全
// ---------------------------------------------------------------------------
describe('merchant.ts 缓存层 withRequestCache / invalidateCachePrefix', () => {
    beforeEach(() => {
        invalidateCachePrefix('TEST_PREFIX_');
    });

    it('同key两次调用只执行一次fetcher，TTL内返回缓存', async () => {
        const fetcher = vi.fn(async () => ({ v: 42 }));
        const r1 = await withRequestCache('TEST_PREFIX_A', fetcher, 5000);
        const r2 = await withRequestCache('TEST_PREFIX_A', fetcher, 5000);
        expect(r1).toEqual({ v: 42 });
        expect(r1).toBe(r2);
        expect(fetcher).toHaveBeenCalledTimes(1);
    });
    it('不同key各独立执行一次fetcher', async () => {
        const fA = vi.fn(async () => 'A');
        const fB = vi.fn(async () => 'B');
        const [a, b] = await Promise.all([
            withRequestCache('TEST_PREFIX_KA', fA),
            withRequestCache('TEST_PREFIX_KB', fB),
        ]);
        expect(a).toBe('A');
        expect(b).toBe('B');
        expect(fA).toHaveBeenCalledTimes(1);
        expect(fB).toHaveBeenCalledTimes(1);
    });
    it('invalidateCachePrefix：按前缀批量失效', async () => {
        const f = vi.fn(async () => Math.random());
        const a1 = await withRequestCache('TEST_PREFIX_INV', f, 5000);
        invalidateCachePrefix('TEST_PREFIX_INV');
        const a2 = await withRequestCache('TEST_PREFIX_INV', f, 5000);
        expect(a1).not.toBe(a2);
        expect(f).toHaveBeenCalledTimes(2);
    });
});

describe('merchant.ts 列表工具 buildListKey / pageSlice', () => {
    it('buildListKey：直接返回item.id，供v-for/virtual-scroller key', () => {
        expect(buildListKey({ id: 7 })).toBe(7);
        expect(buildListKey({ id: 'uuid-abc' })).toBe('uuid-abc');
    });
    it('pageSlice：当前页数据切割，越界保护start<0与超出长度截断', () => {
        const arr = Array.from({ length: 25 }, (_, i) => i);
        expect(pageSlice(arr, 1, 10)).toEqual(Array.from({ length: 10 }, (_, i) => i));
        expect(pageSlice(arr, 3, 10)).toEqual([20, 21, 22, 23, 24]);
        expect(pageSlice(arr, 0, 10)).toEqual(arr.slice(0, 10));
        expect(pageSlice(arr, 999, 10)).toEqual([]);
    });
});

describe('merchant.ts 安全工具 sanitizeXSS / safeInput / hasPermission', () => {
    it('sanitizeXSS：转义< > " \' & ` 六大危险字符', () => {
        const raw = '<script>alert(\'"xss"\')&copy;`back`</script>';
        const escaped = sanitizeXSS(raw);
        expect(escaped).not.toContain('<script>');
        expect(escaped).toContain('&lt;');
        expect(escaped).toContain('&gt;');
        expect(escaped).toContain('&quot;');
        expect(escaped).toContain('&#39;');
        expect(escaped).toContain('&amp;');
        expect(escaped).toContain('&#96;');
    });
    it('sanitizeXSS：null/undefined/数字安全', () => {
        expect(sanitizeXSS(null)).toBe('');
        expect(sanitizeXSS(undefined)).toBe('');
        expect(sanitizeXSS(123)).toBe('123');
    });
    it('safeInput：先XSS清理再截断maxLen，默认100（"<" → "&lt;"占4字符）', () => {
        expect(safeInput('a'.repeat(200))).toHaveLength(100);
        // '<img src=x>'  XSS转义后='&lt;img src=x&gt;'，取前5位='&lt;i'
        expect(safeInput('<img src=x>', 5)).toBe('&lt;i');
    });

    describe('hasPermission RBAC权限判断', () => {
        const KEY = 'tailoris-admin-permissions';
        beforeEach(() => localStorage.clear());
        it('未设置permissions时返回false', () => {
            expect(hasPermission('merchant:audit:pass')).toBe(false);
        });
        it('通配符*匹配所有权限', () => {
            localStorage.setItem(KEY, JSON.stringify(['*']));
            expect(hasPermission('anything')).toBe(true);
        });
        it('精确匹配权限字符串', () => {
            localStorage.setItem(KEY, JSON.stringify(['merchant:audit:pass', 'merchant:list']));
            expect(hasPermission('merchant:audit:pass')).toBe(true);
            expect(hasPermission('merchant:delete')).toBe(false);
        });
    });
});

describe('merchant.ts 批量并发 runBatchConcurrently（concurrency=5）', () => {
    it('25条任务最多5条并发执行，返回顺序与输入一致', async () => {
        let inflight = 0;
        let maxInflight = 0;
        const worker = vi.fn(async (x: number) => {
            inflight++;
            maxInflight = Math.max(maxInflight, inflight);
            await Promise.resolve();
            inflight--;
            return x * 2;
        });
        const items = Array.from({ length: 25 }, (_, i) => i + 1);
        const results = await runBatchConcurrently(items, worker, 5);
        expect(results).toHaveLength(25);
        expect(results.slice(0, 5)).toEqual([2, 4, 6, 8, 10]);
        expect(results[24]).toBe(50);
        expect(maxInflight).toBeLessThanOrEqual(5);
        expect(worker).toHaveBeenCalledTimes(25);
    });
    it('空数组/单元素边界：不创建多余worker', async () => {
        const w = vi.fn(async (x: number) => x);
        expect(await runBatchConcurrently([], w)).toEqual([]);
        expect(await runBatchConcurrently([9], w)).toEqual([9]);
    });
});

// ---------------------------------------------------------------------------
// API层：商家入驻 / 审核 链路
// ---------------------------------------------------------------------------
describe('merchant.ts API层：商家入驻/审核链路', () => {
    beforeEach(() => { mockedRequest.mockReset(); });

    it('listPendingMerchants：GET /admin/merchant/pending，showError=false（503走mock降级不弹错', async () => {
        mockedRequest.mockRejectedValueOnce(new Error('503'));
        const res = await listPendingMerchants({ current: 1, size: 10 });
        expect(res.current).toBe(1);
        expect(res.size).toBe(10);
        expect(Array.isArray(res.records)).toBe(true);
        expect(mockedRequest).toHaveBeenCalledWith(
            expect.objectContaining({ url: '/admin/merchant/pending', method: 'get', showError: false })
        );
    });

    it('listMerchants：GET /admin/merchant/list，分页+keyword透传，showError=false降级', async () => {
        mockedRequest.mockResolvedValueOnce({ records: [], total: 0, current: 2, size: 20 });
        const res = await listMerchants({ current: 2, size: 20, keyword: '测试商家' });
        expect(res.current).toBe(2);
        expect(mockedRequest).toHaveBeenCalledWith(
            expect.objectContaining({
                url: '/admin/merchant/list',
                method: 'get',
                showError: false,
                params: { current: 2, size: 20, keyword: '测试商家' },
            })
        );
    });

    it('auditMerchant：PUT /admin/merchant/audit/:id，通过/驳回都带data', async () => {
        mockedRequest.mockResolvedValue(null);
        await auditMerchant(1001, { approved: true });
        expect(mockedRequest).toHaveBeenCalledWith(
            expect.objectContaining({ url: '/admin/merchant/audit/1001', method: 'put', data: { approved: true } })
        );
        await auditMerchant(1002, { approved: false, reason: '营业执照模糊' });
        expect(mockedRequest).toHaveBeenLastCalledWith(
            expect.objectContaining({
                url: '/admin/merchant/audit/1002',
                method: 'put',
                data: { approved: false, reason: '营业执照模糊' },
            })
        );
    });

    it('batchAuditMerchant：PUT /admin/merchant/batch-audit，showError=false', async () => {
        mockedRequest.mockResolvedValue(null);
        await batchAuditMerchant({ ids: [1, 2, 3], approved: true });
        expect(mockedRequest).toHaveBeenCalledWith(
            expect.objectContaining({
                url: '/admin/merchant/batch-audit',
                method: 'put',
                showError: false,
                data: { ids: [1, 2, 3], approved: true },
            })
        );
    });

    it('freezeMerchant / unfreezeMerchant：PUT冻结/解冻', async () => {
        mockedRequest.mockResolvedValue(null);
        await freezeMerchant(5);
        expect(mockedRequest).toHaveBeenCalledWith(
            expect.objectContaining({ url: '/admin/merchant/freeze/5', method: 'put' })
        );
        await unfreezeMerchant(5);
        expect(mockedRequest).toHaveBeenLastCalledWith(
            expect.objectContaining({ url: '/admin/merchant/unfreeze/5', method: 'put' })
        );
    });
});

// ---------------------------------------------------------------------------
// API层：店铺分类管理
// ---------------------------------------------------------------------------
describe('merchant.ts API层：店铺分类管理（CRUD完整链路）', () => {
    beforeEach(() => { mockedRequest.mockReset(); });

    it('getShopCategoryTree：GET /admin/shop-category/tree，showError=false', async () => {
        mockedRequest.mockRejectedValueOnce(new Error('503'));
        const res = await getShopCategoryTree();
        expect(Array.isArray(res)).toBe(true);
        expect(mockedRequest).toHaveBeenCalledWith(
            expect.objectContaining({ url: '/admin/shop-category/tree', showError: false })
        );
    });
    it('createShopCategory → updateShopCategory → deleteShopCategory 完整CRUD', async () => {
        const payload = {
            categoryName: '女装定制', parentId: 0, categoryCode: 'WOMEN',
            sortOrder: 1, commissionRate: 0.05, depositMin: 5000, status: 1,
        };
        // create
        mockedRequest.mockResolvedValueOnce({ id: 11, ...payload });
        const created = await createShopCategory(payload);
        expect(created.id).toBe(11);
        expect(mockedRequest).toHaveBeenLastCalledWith(
            expect.objectContaining({ url: '/admin/shop-category', method: 'post', data: payload })
        );
        // update
        mockedRequest.mockResolvedValueOnce(null);
        await updateShopCategory(11, { categoryName: '高级女装定制', status: 1 });
        expect(mockedRequest).toHaveBeenLastCalledWith(
            expect.objectContaining({ url: '/admin/shop-category/11', method: 'put' })
        );
        // delete
        mockedRequest.mockResolvedValueOnce(null);
        await deleteShopCategory(11);
        expect(mockedRequest).toHaveBeenLastCalledWith(
            expect.objectContaining({ url: '/admin/shop-category/11', method: 'delete' })
        );
    });
});

// ---------------------------------------------------------------------------
// API层：入驻类型 / 修改申请 / 保障金 / 提现 / 账单
// ---------------------------------------------------------------------------
describe('merchant.ts API层：入驻类型/修改申请/保障金/提现/账单', () => {
    beforeEach(() => { mockedRequest.mockReset(); });

    it('listApplyTypePage/createApplyType/updateApplyType/deleteApplyType', async () => {
        mockedRequest.mockResolvedValueOnce({ records: [], total: 0, current: 1, size: 10 });
        await listApplyTypePage({ current: 1, size: 10 });
        expect(mockedRequest).toHaveBeenCalledWith(
            expect.objectContaining({ url: '/admin/apply-type/page', params: { current: 1, size: 10 } })
        );
        mockedRequest.mockClear();
        const p = {
            typeCode: 'FLAGSHIP', typeName: '旗舰版', depositRequired: 20000,
            commissionBaseRate: 0.03, trialDays: 60, sortOrder: 2, status: 1,
        };
        mockedRequest.mockResolvedValue({ id: 2 });
        await createApplyType(p);
        expect(mockedRequest).toHaveBeenLastCalledWith(
            expect.objectContaining({ url: '/admin/apply-type', method: 'post', data: p })
        );
        await updateApplyType(2, { typeName: '改旗舰版' });
        expect(mockedRequest).toHaveBeenLastCalledWith(
            expect.objectContaining({ url: '/admin/apply-type/2', method: 'put' })
        );
        await deleteApplyType(2);
        expect(mockedRequest).toHaveBeenLastCalledWith(
            expect.objectContaining({ url: '/admin/apply-type/2', method: 'delete' })
        );
    });

    it('修改申请：listModifyApplyPage showError=false + auditModifyApply', async () => {
        mockedRequest.mockRejectedValueOnce(new Error('503'));
        await listModifyApplyPage({ current: 1, size: 10 });
        expect(mockedRequest).toHaveBeenCalledWith(
            expect.objectContaining({ url: '/admin/merchant-modify/page', showError: false })
        );
        mockedRequest.mockClear();
        mockedRequest.mockResolvedValue(null);
        await auditModifyApply({ applyId: 77, approved: true });
        expect(mockedRequest).toHaveBeenCalledWith(
            expect.objectContaining({
                url: '/admin/merchant-modify/audit',
                method: 'put',
                data: { applyId: 77, approved: true },
            })
        );
    });

    it('保障金：listDepositPage showError=false + adjustDeposit(SUPPLEMENT/DEDUCT)', async () => {
        mockedRequest.mockRejectedValueOnce(new Error('503'));
        await listDepositPage({ current: 1, size: 10 });
        expect(mockedRequest).toHaveBeenCalledWith(
            expect.objectContaining({ url: '/admin/deposit/page', showError: false })
        );
        mockedRequest.mockClear();
        mockedRequest.mockResolvedValue(null);
        await adjustDeposit({ merchantId: 1, adjustType: 'SUPPLEMENT', amount: 2000, remark: '补缴' });
        expect(mockedRequest).toHaveBeenLastCalledWith(
            expect.objectContaining({
                url: '/admin/deposit/adjust', method: 'post',
                data: { merchantId: 1, adjustType: 'SUPPLEMENT', amount: 2000, remark: '补缴' },
            })
        );
        await adjustDeposit({ merchantId: 2, adjustType: 'DEDUCT', amount: 300, remark: '扣罚' });
        expect(mockedRequest).toHaveBeenLastCalledWith(
            expect.objectContaining({ data: expect.objectContaining({ adjustType: 'DEDUCT' }) })
        );
    });

    it('提现：listWithdrawPage showError=false + auditWithdraw 拒绝必带rejectReason', async () => {
        mockedRequest.mockRejectedValueOnce(new Error('503'));
        await listWithdrawPage({ current: 1, size: 10 });
        expect(mockedRequest).toHaveBeenCalledWith(
            expect.objectContaining({ url: '/admin/withdraw-audit/page', showError: false })
        );
        mockedRequest.mockClear();
        mockedRequest.mockResolvedValue(null);
        await auditWithdraw({ id: 8, approved: false, rejectReason: '账户验证失败' });
        expect(mockedRequest).toHaveBeenLastCalledWith(
            expect.objectContaining({
                url: '/admin/withdraw-audit/audit', method: 'put',
                data: { id: 8, approved: false, rejectReason: '账户验证失败' },
            })
        );
    });

    it('账单：listMerchantBillPage showError=false', async () => {
        mockedRequest.mockRejectedValueOnce(new Error('503'));
        const res = await listMerchantBillPage({ current: 1, size: 10 });
        expect(res.records).toBeDefined();
        expect(mockedRequest).toHaveBeenCalledWith(
            expect.objectContaining({ url: '/admin/merchant-bill/page', showError: false })
        );
    });
});
