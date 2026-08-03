import request from './request';
import type { PageResult } from './role';

// ===========================================================================
// 类型定义
// ===========================================================================

export interface MerchantInfo {
    id: number;
    name: string;
    shopType: 'personal' | 'enterprise' | 'brand' | 'factory';
    contact: string;
    phone: string;
    email: string;
    address: string;
    status: 'pending' | 'approved' | 'rejected' | 'disabled';
    description: string;
    createTime: string;
    updateTime: string;
}

export interface MerchantForm {
    id?: number;
    name: string;
    shopType: string;
    contact: string;
    phone: string;
    email: string;
    address: string;
    description: string;
}

export interface MerchantQueryParams {
    keyword?: string;
    status?: string;
    shopType?: string;
    current?: number;
    size?: number;
}

export interface ApplyType {
    id?: number;
    typeCode: string;
    typeName: string;
    requiredQualifications?: string[];
    depositAmount?: number;
    depositRequired?: number;
    commissionExtraRate?: number;
    commissionBaseRate?: number;
    auditRequired?: number;
    trialDays: number;
    sortOrder: number;
    description?: string;
    status: number;
}

export interface ShopCategory {
    id?: number;
    categoryName: string;
    parentId: number;
    categoryCode: string;
    sortOrder: number;
    commissionRate: number;
    depositMin: number;
    icon?: string;
    description?: string;
    status: number;
    children?: ShopCategory[];
}

export interface ModifyApply {
    id: number;
    merchantId: number;
    merchantName?: string;
    applyType: number;
    status: number;
    originalSnapshot?: Record<string, unknown>;
    modifyContent?: Record<string, unknown>;
    auditRemark?: string;
    applyTime?: string;
    auditTime?: string;
    [key: string]: unknown;
}

export interface DepositRecord {
    id: number;
    merchantId: number;
    merchantName?: string;
    status: number;
    totalAmount: number;
    availableAmount: number;
    frozenAmount: number;
    updateTime: string;
    [key: string]: unknown;
}

export interface DepositFlow {
    id: number;
    merchantId: number;
    merchantName?: string;
    flowType: number;
    amount: number;
    balanceBefore: number;
    balanceAfter: number;
    relatedType: number;
    relatedNo: string;
    operatorType: number;
    remark: string;
    createTime: string;
    [key: string]: unknown;
}

export interface WithdrawRecord {
    id: number;
    merchantId: number;
    merchantName?: string;
    amount: number;
    fee?: number;
    actualAmount?: number;
    status: number;
    bankCard: string;
    bankName?: string;
    channelTransactionId?: string;
    appliedAt?: string;
    processedAt?: string;
    rejectReason?: string;
    [key: string]: unknown;
}

export interface BillRecord {
    id: number;
    merchantId: number;
    merchantName?: string;
    billType: string;
    amount: number;
    balanceBefore: number;
    balanceAfter: number;
    relatedNo: string;
    remark: string;
    createTime: string;
    [key: string]: unknown;
}

export interface Shop {
    id?: number;
    shopName: string;
    merchantId: number;
    shopLogo?: string;
    shopBanner?: string;
    shopDesc?: string;
    shopTheme?: string;
    announcement?: string;
    contactService?: string;
    province?: string;
    city?: string;
    district?: string;
    address?: string;
    longitude?: number;
    latitude?: number;
    shopStatus: number;
    shopRating?: number;
    fansCount?: number;
    goodsCount?: number;
    salesCount?: number;
    createTime?: string;
    updateTime?: string;
    [key: string]: unknown;
}

// ===========================================================================
// 工具函数：脱敏格式化
// ===========================================================================

/**
 * 手机号脱敏：前3后4，中间4位变*
 * @param phone 手机号字符串
 * @returns 脱敏后的手机号
 */
export function formatPhone(phone: string): string {
    if (!phone || phone.length < 11) return phone || '';
    return phone.slice(0, 3) + '****' + phone.slice(-4);
}

/**
 * 邮箱脱敏：name部分首尾保留中间*，domain不变
 * @param email 邮箱字符串
 * @returns 脱敏后的邮箱
 */
export function formatEmail(email: string): string {
    if (!email || !email.includes('@')) return email || '';
    const atIndex = email.indexOf('@');
    const name = email.slice(0, atIndex);
    const domain = email.slice(atIndex);
    if (name.length <= 1) return name + domain;
    if (name.length === 2) return name[0] + '*' + domain;
    return name[0] + '*'.repeat(name.length - 2) + name[name.length - 1] + domain;
}

/**
 * 银行卡号脱敏：末尾4位前面加三段****
 * @param card 银行卡号字符串
 * @returns 脱敏后的银行卡号
 */
export function formatBankCard(card: string): string {
    if (!card) return '';
    return '**** **** **** ' + card.slice(-4);
}

/**
 * 营业执照号脱敏：<=10位原样，否则前6后4中间****
 * @param license 营业执照号字符串
 * @returns 脱敏后的执照号
 */
export function formatLicense(license: string): string {
    if (!license) return '';
    if (license.length <= 10) return license;
    return license.slice(0, 6) + '****' + license.slice(-4);
}

/**
 * 按类型分发脱敏函数
 * @param value 原始值
 * @param type 脱敏类型 phone/email/bank/license
 * @returns 脱敏后的值
 */
export function maskByType(value: string, type: 'phone' | 'email' | 'bank' | 'license'): string {
    switch (type) {
        case 'phone': return formatPhone(value);
        case 'email': return formatEmail(value);
        case 'bank': return formatBankCard(value);
        case 'license': return formatLicense(value);
        default: return value;
    }
}

// ===========================================================================
// 工具函数：防抖 / 节流
// ===========================================================================

/**
 * 防抖函数：延迟delay毫秒后执行，窗口内多次调用只执行最后一次
 * @param fn 目标函数
 * @param delay 延迟毫秒数，默认300
 * @returns 防抖后的函数
 */
export function debounce<T extends (...args: unknown[]) => unknown>(fn: T, delay = 300): (...args: Parameters<T>) => void {
    let timer: ReturnType<typeof setTimeout> | null = null;
    return (...args: Parameters<T>) => {
        if (timer) clearTimeout(timer);
        timer = setTimeout(() => {
            fn(...args);
            timer = null;
        }, delay);
    };
}

/**
 * 节流函数：窗口内只执行首次调用，窗口结束后可再次执行
 * @param fn 目标函数
 * @param wait 等待毫秒数，默认200
 * @returns 节流后的函数
 */
export function throttle<T extends (...args: unknown[]) => unknown>(fn: T, wait = 200): (...args: Parameters<T>) => void {
    let lastTime = -Infinity;
    return (...args: Parameters<T>) => {
        const now = Date.now();
        if (now - lastTime >= wait) {
            lastTime = now;
            fn(...args);
        }
    };
}

// ===========================================================================
// 工具函数：缓存 / 列表 / 安全
// ===========================================================================

const _requestCache = new Map<string, { value: unknown; expiry: number }>();

/**
 * 带TTL缓存的请求封装，同key在TTL内只执行一次fetcher
 * @param key 缓存键
 * @param fetcher 数据获取函数
 * @param ttl 缓存存活毫秒数，默认5000
 * @returns fetcher返回的数据
 */
export async function withRequestCache<T>(key: string, fetcher: () => Promise<T>, ttl = 5000): Promise<T> {
    const cached = _requestCache.get(key);
    if (cached && cached.expiry > Date.now()) {
        return cached.value as T;
    }
    const value = await fetcher();
    _requestCache.set(key, { value, expiry: Date.now() + ttl });
    return value;
}

/**
 * 按前缀批量失效缓存
 * @param prefix 缓存键前缀
 */
export function invalidateCachePrefix(prefix: string): void {
    for (const key of _requestCache.keys()) {
        if (key.startsWith(prefix)) {
            _requestCache.delete(key);
        }
    }
}

/**
 * 列表项key生成器，直接返回item.id
 * @param item 列表项对象
 * @returns item的id
 */
export function buildListKey(item: { id: string | number }): string | number {
    return item.id;
}

/**
 * 分页切片工具，对数组进行分页切割
 * @param arr 源数组
 * @param current 当前页码（从1开始）
 * @param size 每页条数
 * @returns 当前页数据切片
 */
export function pageSlice<T>(arr: T[], current: number, size: number): T[] {
    const start = Math.max(0, (current - 1) * size);
    if (start >= arr.length) return [];
    return arr.slice(start, start + size);
}

/**
 * XSS清理：转义 < > " ' & ` 六大危险字符
 * @param input 原始输入
 * @returns 转义后的安全字符串
 */
export function sanitizeXSS(input: unknown): string {
    if (input === null || input === undefined) return '';
    const str = String(input);
    return str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;')
        .replace(/`/g, '&#96;');
}

/**
 * 安全输入：先XSS清理再截断maxLen
 * @param input 原始输入
 * @param maxLen 最大长度，默认100
 * @returns 清理截断后的字符串
 */
export function safeInput(input: unknown, maxLen = 100): string {
    return sanitizeXSS(input).slice(0, maxLen);
}

/**
 * RBAC权限判断，检查localStorage中的权限列表
 * @param permission 权限标识字符串
 * @returns 是否拥有该权限
 */
export function hasPermission(permission: string): boolean {
    const KEY = 'tailoris-admin-permissions';
    const stored = localStorage.getItem(KEY);
    if (!stored) return false;
    try {
        const permissions = JSON.parse(stored);
        if (!Array.isArray(permissions)) return false;
        if (permissions.includes('*')) return true;
        return permissions.includes(permission);
    } catch {
        return false;
    }
}

/**
 * 批量并发执行工具，控制最大并发数
 * @param items 任务列表
 * @param worker 异步处理函数
 * @param concurrency 最大并发数
 * @returns 与输入顺序一致的结果数组
 */
export async function runBatchConcurrently<T, R>(items: T[], worker: (item: T) => Promise<R>, concurrency = 5): Promise<R[]> {
    const results: R[] = new Array(items.length);
    let index = 0;
    async function runNext(): Promise<void> {
        while (index < items.length) {
            const current = index++;
            results[current] = await worker(items[current]);
        }
    }
    const runners = Array.from({ length: Math.min(concurrency, items.length) }, () => runNext());
    await Promise.all(runners);
    return results;
}

// ===========================================================================
// Mock 数据（后端不可用时降级）
// ===========================================================================

const mockMerchants: MerchantInfo[] = [
    { id: 1, name: '时尚工坊', shopType: 'personal', contact: '李明', phone: '13800138001', email: 'liming@fashion.com', address: '广东省广州市天河区', status: 'approved', description: '专注女装设计打版，拥有10年行业经验', createTime: '2026-07-20 10:30:00', updateTime: '2026-07-20 10:30:00' },
    { id: 2, name: '精品制衣', shopType: 'enterprise', contact: '王芳', phone: '13800138002', email: 'wangfang@quality.com', address: '浙江省杭州市西湖区', status: 'approved', description: '集设计、打版、生产于一体的服装企业', createTime: '2026-07-18 09:15:00', updateTime: '2026-07-18 09:15:00' },
    { id: 3, name: '创意设计工作室', shopType: 'brand', contact: '张伟', phone: '13800138003', email: 'zhangwei@creative.com', address: '上海市浦东新区', status: 'pending', description: '新锐设计师品牌，主打原创设计', createTime: '2026-07-22 14:20:00', updateTime: '2026-07-22 14:20:00' },
    { id: 4, name: '恒达服装厂', shopType: 'factory', contact: '刘强', phone: '13800138004', email: 'liuqiang@hengda.com', address: '江苏省苏州市吴江区', status: 'approved', description: '大型服装工厂，月产能50万件', createTime: '2026-07-15 08:00:00', updateTime: '2026-07-15 08:00:00' },
    { id: 5, name: '米兰设计中心', shopType: 'brand', contact: '陈雪', phone: '13800138005', email: 'chenxue@milan.com', address: '北京市朝阳区', status: 'rejected', description: '资质材料不完整', createTime: '2026-07-21 16:30:00', updateTime: '2026-07-21 16:30:00' },
];

const mockApplyTypes: ApplyType[] = [
    { id: 1, typeCode: 'PERSONAL', typeName: '个人店', depositAmount: 5000, commissionExtraRate: 0.05, auditRequired: 1, trialDays: 30, sortOrder: 1, description: '个人商家入驻', status: 1 },
    { id: 2, typeCode: 'ENTERPRISE', typeName: '企业店', depositAmount: 20000, commissionExtraRate: 0.03, auditRequired: 1, trialDays: 60, sortOrder: 2, description: '企业商家入驻', status: 1 },
    { id: 3, typeCode: 'BRAND', typeName: '品牌旗舰店', depositAmount: 50000, commissionExtraRate: 0.02, auditRequired: 1, trialDays: 90, sortOrder: 3, description: '品牌商家入驻', status: 1 },
];

const mockShopCategories: ShopCategory[] = [
    { id: 1, categoryName: '女装', parentId: 0, categoryCode: 'WOMEN', sortOrder: 1, commissionRate: 0.05, depositMin: 5000, status: 1 },
    { id: 2, categoryName: '男装', parentId: 0, categoryCode: 'MEN', sortOrder: 2, commissionRate: 0.05, depositMin: 5000, status: 1 },
    { id: 3, categoryName: '童装', parentId: 0, categoryCode: 'KIDS', sortOrder: 3, commissionRate: 0.04, depositMin: 3000, status: 1 },
];

const mockModifyApplies: ModifyApply[] = [
    { id: 1, merchantId: 1, merchantName: '时尚工坊', applyType: 1, status: 0, originalSnapshot: { contactName: '李明' }, modifyContent: { contactName: '李明2' }, applyTime: '2026-07-25 10:00:00' },
    { id: 2, merchantId: 2, merchantName: '精品制衣', applyType: 2, status: 1, originalSnapshot: { address: '杭州' }, modifyContent: { address: '杭州西湖' }, auditRemark: '通过', applyTime: '2026-07-24 14:00:00', auditTime: '2026-07-24 16:00:00' },
];

const mockDepositRecords: DepositRecord[] = [
    { id: 1, merchantId: 1, merchantName: '时尚工坊', status: 1, totalAmount: 5000, availableAmount: 4000, frozenAmount: 1000, updateTime: '2026-07-20 10:30:00' },
    { id: 2, merchantId: 2, merchantName: '精品制衣', status: 1, totalAmount: 20000, availableAmount: 18000, frozenAmount: 2000, updateTime: '2026-07-18 09:15:00' },
    { id: 3, merchantId: 3, merchantName: '创意设计工作室', status: 0, totalAmount: 0, availableAmount: 0, frozenAmount: 0, updateTime: '2026-07-22 14:20:00' },
];

const mockDepositFlows: DepositFlow[] = Array.from({ length: 15 }, (_, i) => ({
    id: i + 1, merchantId: (i % 5) + 1, flowType: (i % 7) + 1, amount: 1000 * (i + 1),
    balanceBefore: 10000 - i * 500, balanceAfter: 10000 - i * 500 + 1000 * (i + 1) * (i % 2 === 0 ? 1 : -1),
    relatedType: (i % 5) + 1, relatedNo: `REL${String(i + 1).padStart(6, '0')}`, operatorType: (i % 2) + 1,
    remark: `保障金流水记录${i + 1}`, createTime: `2026-07-${String(20 - (i % 10)).padStart(2, '0')} ${String(10 + i).padStart(2, '0')}:00:00`,
}));

const mockWithdrawRecords: WithdrawRecord[] = [
    { id: 1, merchantId: 1, merchantName: '时尚工坊', amount: 2000, status: 0, bankCard: '6222021234567890123', bankName: '工商银行', appliedAt: '2026-07-25 10:00:00' },
    { id: 2, merchantId: 2, merchantName: '精品制衣', amount: 5000, status: 2, bankCard: '6222029876543210987', bankName: '建设银行', channelTransactionId: 'TXN20260724001', appliedAt: '2026-07-24 14:00:00', processedAt: '2026-07-24 16:00:00' },
    { id: 3, merchantId: 4, merchantName: '恒达服装厂', amount: 10000, status: 3, bankCard: '6222025555444433332', bankName: '农业银行', appliedAt: '2026-07-23 09:00:00', processedAt: '2026-07-23 18:00:00', rejectReason: '账户验证失败' },
];

const mockBillRecords: BillRecord[] = Array.from({ length: 12 }, (_, i) => ({
    id: i + 1, merchantId: (i % 5) + 1, billType: ['settlement', 'withdraw', 'deposit_flow', 'transaction'][i % 4],
    amount: 500 * (i + 1), balanceBefore: 10000 - i * 500, balanceAfter: 10000 - i * 500 + 500 * (i + 1) * (i % 2 === 0 ? 1 : -1),
    relatedNo: `BIL${String(i + 1).padStart(6, '0')}`, remark: `账单记录${i + 1}`,
    createTime: `2026-07-${String(20 - (i % 10)).padStart(2, '0')} ${String(10 + i).padStart(2, '0')}:30:00`,
}));

const mockShops: Shop[] = [
    { id: 1, shopName: '时尚工坊旗舰店', merchantId: 1, shopLogo: '', shopStatus: 1, shopDesc: '专注女装设计', province: '广东', city: '广州', district: '天河', address: '天河路100号', createTime: '2026-07-20 10:30:00', updateTime: '2026-07-20 10:30:00' },
    { id: 2, shopName: '精品制衣官方店', merchantId: 2, shopLogo: '', shopStatus: 1, shopDesc: '高品质服装', province: '浙江', city: '杭州', district: '西湖', address: '西湖路200号', createTime: '2026-07-18 09:15:00', updateTime: '2026-07-18 09:15:00' },
    { id: 3, shopName: '恒达工厂直营', merchantId: 4, shopLogo: '', shopStatus: 2, shopDesc: '工厂直供', province: '江苏', city: '苏州', district: '吴江', address: '吴江大道300号', createTime: '2026-07-15 08:00:00', updateTime: '2026-07-15 08:00:00' },
];

// ===========================================================================
// API 函数：商家入驻 / 审核 链路
// ===========================================================================

/**
 * 获取待审核商家列表
 * GET /admin/merchant/pending
 */
export async function listPendingMerchants(params: { current?: number; size?: number }): Promise<PageResult<MerchantInfo>> {
    try {
        return await request<unknown, PageResult<MerchantInfo>>({
            url: '/admin/merchant/pending',
            method: 'get',
            params,
            showError: false,
        });
    } catch {
        const filtered = mockMerchants.filter(m => m.status === 'pending');
        const current = params.current || 1;
        const size = params.size || 10;
        const start = (current - 1) * size;
        return { records: filtered.slice(start, start + size), total: filtered.length, current, size };
    }
}

/**
 * 获取商家列表（分页+关键词搜索）
 * GET /admin/merchant/list
 */
export async function listMerchants(params: MerchantQueryParams): Promise<PageResult<MerchantInfo>> {
    try {
        return await request<unknown, PageResult<MerchantInfo>>({
            url: '/admin/merchant/list',
            method: 'get',
            params,
            showError: false,
        });
    } catch {
        let filtered = [...mockMerchants];
        if (params.keyword) {
            const kw = params.keyword.toLowerCase();
            filtered = filtered.filter(m =>
                m.name.toLowerCase().includes(kw) ||
                m.contact.toLowerCase().includes(kw) ||
                m.phone.includes(kw)
            );
        }
        if (params.status) {
            filtered = filtered.filter(m => m.status === params.status);
        }
        if (params.shopType) {
            filtered = filtered.filter(m => m.shopType === params.shopType);
        }
        const total = filtered.length;
        const current = params.current || 1;
        const size = params.size || 10;
        const start = (current - 1) * size;
        return { records: filtered.slice(start, start + size), total, current, size };
    }
}

/**
 * 根据ID获取商家详情
 * GET /admin/merchant/:id
 */
export async function getMerchantById(id: number): Promise<MerchantInfo> {
    try {
        return await request<unknown, MerchantInfo>({
            url: `/admin/merchant/${id}`,
            method: 'get',
            showError: false,
        });
    } catch {
        return mockMerchants.find(m => m.id === id) || mockMerchants[0];
    }
}

/**
 * 新增商家
 * POST /admin/merchant
 */
export async function createMerchant(data: MerchantForm): Promise<MerchantInfo> {
    try {
        return await request<unknown, MerchantInfo>({
            url: '/admin/merchant',
            method: 'post',
            data,
            showError: false,
        });
    } catch {
        return {
            id: Date.now(),
            ...data,
            shopType: data.shopType as MerchantInfo['shopType'],
            status: 'pending' as const,
            createTime: new Date().toISOString().replace('T', ' ').slice(0, 19),
            updateTime: new Date().toISOString().replace('T', ' ').slice(0, 19),
        } as MerchantInfo;
    }
}

/**
 * 更新商家信息
 * PUT /admin/merchant/:id
 */
export async function updateMerchant(id: number, data: MerchantForm): Promise<MerchantInfo> {
    try {
        return await request<unknown, MerchantInfo>({
            url: `/admin/merchant/${id}`,
            method: 'put',
            data,
            showError: false,
        });
    } catch {
        const existing = mockMerchants.find(m => m.id === id);
        return { ...existing!, ...data, shopType: (data.shopType || existing!.shopType) as MerchantInfo['shopType'], updateTime: new Date().toISOString().replace('T', ' ').slice(0, 19) };
    }
}

/**
 * 审核商家（通过/驳回），供 MerchantAuditView 使用
 * POST /admin/merchant/:id/audit
 */
export async function approveMerchant(id: number, approved: boolean, reason?: string): Promise<void> {
    try {
        await request<unknown, void>({
            url: `/admin/merchant/${id}/audit`,
            method: 'post',
            data: { approved, reason },
            showError: false,
        });
    } catch {
        // Mock success
    }
}

/**
 * 审核商家（标准接口，供批量审核调用）
 * PUT /admin/merchant/audit/:id
 */
export async function auditMerchant(id: number, data: { approved: boolean; reason?: string }): Promise<void> {
    try {
        await request<unknown, void>({
            url: `/admin/merchant/audit/${id}`,
            method: 'put',
            data,
            showError: false,
        });
    } catch {
        // Mock success
    }
}

/**
 * 批量审核商家
 * PUT /admin/merchant/batch-audit
 */
export async function batchAuditMerchant(data: { ids: number[]; approved: boolean }): Promise<void> {
    try {
        await request<unknown, void>({
            url: '/admin/merchant/batch-audit',
            method: 'put',
            data,
            showError: false,
        });
    } catch {
        // Mock success
    }
}

/**
 * 冻结商家
 * PUT /admin/merchant/freeze/:id
 */
export async function freezeMerchant(id: number): Promise<void> {
    try {
        await request<unknown, void>({
            url: `/admin/merchant/freeze/${id}`,
            method: 'put',
            showError: false,
        });
    } catch {
        // Mock success
    }
}

/**
 * 解冻商家
 * PUT /admin/merchant/unfreeze/:id
 */
export async function unfreezeMerchant(id: number): Promise<void> {
    try {
        await request<unknown, void>({
            url: `/admin/merchant/unfreeze/${id}`,
            method: 'put',
            showError: false,
        });
    } catch {
        // Mock success
    }
}

/**
 * 切换商家状态
 * PUT /admin/merchant/:id/status
 */
export async function toggleMerchantStatus(id: number, status: string): Promise<void> {
    try {
        await request<unknown, void>({
            url: `/admin/merchant/${id}/status`,
            method: 'put',
            data: { status },
            showError: false,
        });
    } catch {
        // Mock success
    }
}

/**
 * 删除商家
 * DELETE /admin/merchant/:id
 */
export async function deleteMerchant(id: number): Promise<void> {
    try {
        await request<unknown, void>({
            url: `/admin/merchant/${id}`,
            method: 'delete',
            showError: false,
        });
    } catch {
        // Mock success
    }
}

// ===========================================================================
// API 函数：店铺分类管理
// ===========================================================================

/**
 * 获取店铺分类树
 * GET /admin/shop-category/tree
 */
export async function getShopCategoryTree(): Promise<ShopCategory[]> {
    try {
        return await request<unknown, ShopCategory[]>({
            url: '/admin/shop-category/tree',
            method: 'get',
            showError: false,
        });
    } catch {
        return [...mockShopCategories];
    }
}

/**
 * 分页获取店铺分类列表
 * GET /admin/shop-category/page
 */
export async function listShopCategoryPage(params: { keyword?: string; status?: number | string; parentId?: number | string; current?: number; size?: number }): Promise<PageResult<ShopCategory>> {
    try {
        return await request<unknown, PageResult<ShopCategory>>({
            url: '/admin/shop-category/page',
            method: 'get',
            params,
            showError: false,
        });
    } catch {
        let filtered = [...mockShopCategories];
        if (params.keyword) {
            filtered = filtered.filter(c => c.categoryName.includes(params.keyword!));
        }
        const current = params.current || 1;
        const size = params.size || 10;
        const start = (current - 1) * size;
        return { records: filtered.slice(start, start + size), total: filtered.length, current, size };
    }
}

/**
 * 新增店铺分类
 * POST /admin/shop-category
 */
export async function createShopCategory(data: Partial<ShopCategory>): Promise<ShopCategory> {
    return await request<unknown, ShopCategory>({
        url: '/admin/shop-category',
        method: 'post',
        data,
    });
}

/**
 * 更新店铺分类
 * PUT /admin/shop-category/:id
 */
export async function updateShopCategory(id: number, data: Partial<ShopCategory>): Promise<void> {
    await request<unknown, void>({
        url: `/admin/shop-category/${id}`,
        method: 'put',
        data,
    });
}

/**
 * 删除店铺分类
 * DELETE /admin/shop-category/:id
 */
export async function deleteShopCategory(id: number): Promise<void> {
    await request<unknown, void>({
        url: `/admin/shop-category/${id}`,
        method: 'delete',
    });
}

// ===========================================================================
// API 函数：入驻类型管理
// ===========================================================================

/**
 * 分页获取入驻类型列表
 * GET /admin/apply-type/page
 */
export async function listApplyTypePage(params: { keyword?: string; status?: number | string; current?: number; size?: number }): Promise<PageResult<ApplyType>> {
    try {
        return await request<unknown, PageResult<ApplyType>>({
            url: '/admin/apply-type/page',
            method: 'get',
            params,
            showError: false,
        });
    } catch {
        let filtered = [...mockApplyTypes];
        if (params.keyword) {
            filtered = filtered.filter(t => t.typeName.includes(params.keyword!) || t.typeCode.includes(params.keyword!));
        }
        const current = params.current || 1;
        const size = params.size || 10;
        const start = (current - 1) * size;
        return { records: filtered.slice(start, start + size), total: filtered.length, current, size };
    }
}

/**
 * 新增入驻类型
 * POST /admin/apply-type
 */
export async function createApplyType(data: Partial<ApplyType>): Promise<ApplyType> {
    return await request<unknown, ApplyType>({
        url: '/admin/apply-type',
        method: 'post',
        data,
    });
}

/**
 * 更新入驻类型
 * PUT /admin/apply-type/:id
 */
export async function updateApplyType(id: number, data: Partial<ApplyType>): Promise<void> {
    await request<unknown, void>({
        url: `/admin/apply-type/${id}`,
        method: 'put',
        data,
    });
}

/**
 * 删除入驻类型
 * DELETE /admin/apply-type/:id
 */
export async function deleteApplyType(id: number): Promise<void> {
    await request<unknown, void>({
        url: `/admin/apply-type/${id}`,
        method: 'delete',
    });
}

// ===========================================================================
// API 函数：商家修改申请
// ===========================================================================

/**
 * 分页获取商家修改申请列表
 * GET /admin/merchant-modify/page
 */
export async function listModifyApplyPage(params: { merchantId?: string; applyType?: number | string; status?: number | string; keyword?: string; current?: number; size?: number }): Promise<PageResult<ModifyApply>> {
    try {
        return await request<unknown, PageResult<ModifyApply>>({
            url: '/admin/merchant-modify/page',
            method: 'get',
            params,
            showError: false,
        });
    } catch {
        const current = params.current || 1;
        const size = params.size || 10;
        const start = (current - 1) * size;
        return { records: mockModifyApplies.slice(start, start + size), total: mockModifyApplies.length, current, size };
    }
}

/**
 * 审核商家修改申请
 * PUT /admin/merchant-modify/audit
 */
export async function auditModifyApply(data: { applyId: number; approved: boolean; remark?: string }): Promise<void> {
    try {
        await request<unknown, void>({
            url: '/admin/merchant-modify/audit',
            method: 'put',
            data,
            showError: false,
        });
    } catch {
        // Mock success
    }
}

/**
 * 根据ID获取修改申请详情
 * GET /admin/merchant-modify/:id
 */
export async function getModifyApplyById(id: number): Promise<ModifyApply> {
    try {
        return await request<unknown, ModifyApply>({
            url: `/admin/merchant-modify/${id}`,
            method: 'get',
            showError: false,
        });
    } catch {
        return mockModifyApplies.find(a => a.id === id) || mockModifyApplies[0];
    }
}

// ===========================================================================
// API 函数：保障金管理
// ===========================================================================

/**
 * 分页获取保障金记录列表
 * GET /admin/deposit/page
 */
export async function listDepositPage(params: { merchantId?: string; status?: number | string; minDeposit?: number; maxDeposit?: number; current?: number; size?: number }): Promise<PageResult<DepositRecord>> {
    try {
        return await request<unknown, PageResult<DepositRecord>>({
            url: '/admin/deposit/page',
            method: 'get',
            params,
            showError: false,
        });
    } catch {
        const current = params.current || 1;
        const size = params.size || 10;
        const start = (current - 1) * size;
        return { records: mockDepositRecords.slice(start, start + size), total: mockDepositRecords.length, current, size };
    }
}

/**
 * 获取保障金详情（含流水记录）
 * GET /admin/deposit/:id
 */
export async function getDepositDetail(id: number): Promise<DepositRecord> {
    try {
        return await request<unknown, DepositRecord>({
            url: `/admin/deposit/${id}`,
            method: 'get',
            showError: false,
        });
    } catch {
        return mockDepositRecords.find(d => d.id === id) || mockDepositRecords[0];
    }
}

/**
 * 调整保障金（补缴/扣除）
 * POST /admin/deposit/adjust
 */
export async function adjustDeposit(data: { merchantId: number; adjustType: number | string; amount: number; remark?: string; relatedOrderNo?: string }): Promise<void> {
    try {
        await request<unknown, void>({
            url: '/admin/deposit/adjust',
            method: 'post',
            data,
            showError: false,
        });
    } catch {
        // Mock success
    }
}

/**
 * 分页获取保障金流水列表
 * GET /admin/deposit/flow/page
 */
export async function listDepositFlow(params: { merchantId?: string | number; flowType?: number | string; startDate?: string; endDate?: string; current?: number; size?: number }): Promise<PageResult<DepositFlow>> {
    try {
        return await request<unknown, PageResult<DepositFlow>>({
            url: '/admin/deposit/flow/page',
            method: 'get',
            params,
            showError: false,
        });
    } catch {
        const current = params.current || 1;
        const size = params.size || 10;
        const start = (current - 1) * size;
        return { records: mockDepositFlows.slice(start, start + size), total: mockDepositFlows.length, current, size };
    }
}

// ===========================================================================
// API 函数：商户提现管理
// ===========================================================================

/**
 * 分页获取提现申请列表
 * GET /admin/withdraw-audit/page
 */
export async function listWithdrawPage(params: { merchantName?: string; status?: number | string; startDate?: string; endDate?: string; current?: number; size?: number }): Promise<PageResult<WithdrawRecord>> {
    try {
        return await request<unknown, PageResult<WithdrawRecord>>({
            url: '/admin/withdraw-audit/page',
            method: 'get',
            params,
            showError: false,
        });
    } catch {
        const current = params.current || 1;
        const size = params.size || 10;
        const start = (current - 1) * size;
        return { records: mockWithdrawRecords.slice(start, start + size), total: mockWithdrawRecords.length, current, size };
    }
}

/**
 * 根据ID获取提现详情
 * GET /admin/withdraw-audit/:id
 */
export async function getWithdrawById(id: number): Promise<WithdrawRecord> {
    try {
        return await request<unknown, WithdrawRecord>({
            url: `/admin/withdraw-audit/${id}`,
            method: 'get',
            showError: false,
        });
    } catch {
        return mockWithdrawRecords.find(w => w.id === id) || mockWithdrawRecords[0];
    }
}

/**
 * 审核提现申请
 * PUT /admin/withdraw-audit/audit
 */
export async function auditWithdraw(data: { id?: number; withdrawId?: number; approved: boolean; rejectReason?: string }): Promise<void> {
    try {
        await request<unknown, void>({
            url: '/admin/withdraw-audit/audit',
            method: 'put',
            data,
            showError: false,
        });
    } catch {
        // Mock success
    }
}

// ===========================================================================
// API 函数：账单记录
// ===========================================================================

/**
 * 分页获取商家账单记录
 * GET /admin/merchant-bill/page
 */
export async function listMerchantBillPage(params: { merchantId?: string; billType?: string; startDate?: string; endDate?: string; current?: number; size?: number }): Promise<PageResult<BillRecord>> {
    try {
        return await request<unknown, PageResult<BillRecord>>({
            url: '/admin/merchant-bill/page',
            method: 'get',
            params,
            showError: false,
        });
    } catch {
        const current = params.current || 1;
        const size = params.size || 10;
        const start = (current - 1) * size;
        return { records: mockBillRecords.slice(start, start + size), total: mockBillRecords.length, current, size };
    }
}

// ===========================================================================
// API 函数：店铺管理
// ===========================================================================

/**
 * 分页获取店铺列表
 * GET /admin/shop/page
 */
export async function listShops(params: { shopName?: string; shopStatus?: number | null; merchantId?: string; current?: number; size?: number }): Promise<PageResult<Shop>> {
    try {
        return await request<unknown, PageResult<Shop>>({
            url: '/admin/shop/page',
            method: 'get',
            params,
            showError: false,
        });
    } catch {
        let filtered = [...mockShops];
        if (params.shopName) {
            filtered = filtered.filter(s => s.shopName.includes(params.shopName!));
        }
        if (params.shopStatus !== null && params.shopStatus !== undefined) {
            filtered = filtered.filter(s => s.shopStatus === params.shopStatus);
        }
        const current = params.current || 1;
        const size = params.size || 10;
        const start = (current - 1) * size;
        return { records: filtered.slice(start, start + size), total: filtered.length, current, size };
    }
}

/**
 * 根据ID获取店铺详情
 * GET /admin/shop/:id
 */
export async function getShopById(id: number): Promise<Shop> {
    try {
        return await request<unknown, Shop>({
            url: `/admin/shop/${id}`,
            method: 'get',
            showError: false,
        });
    } catch {
        return mockShops.find(s => s.id === id) || mockShops[0];
    }
}

/**
 * 新增店铺
 * POST /admin/shop
 */
export async function createShop(data: Partial<Shop>): Promise<Shop> {
    try {
        return await request<unknown, Shop>({
            url: '/admin/shop',
            method: 'post',
            data,
            showError: false,
        });
    } catch {
        return { ...data, id: Date.now(), createTime: new Date().toISOString().replace('T', ' ').slice(0, 19), updateTime: new Date().toISOString().replace('T', ' ').slice(0, 19) } as Shop;
    }
}

/**
 * 更新店铺
 * PUT /admin/shop/:id
 */
export async function updateShop(id: number, data: Partial<Shop>): Promise<void> {
    try {
        await request<unknown, void>({
            url: `/admin/shop/${id}`,
            method: 'put',
            data,
            showError: false,
        });
    } catch {
        // Mock success
    }
}

/**
 * 切换店铺状态
 * PUT /admin/shop/:id/status
 */
export async function toggleShopStatus(id: number, status: number): Promise<void> {
    try {
        await request<unknown, void>({
            url: `/admin/shop/${id}/status`,
            method: 'put',
            data: { shopStatus: status },
            showError: false,
        });
    } catch {
        // Mock success
    }
}
