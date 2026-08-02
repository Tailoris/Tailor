import request from './request';

/**
 * 仪表盘核心统计数据类型（与后端 DashboardStatsResponse 对齐）
 */
export interface DashboardStats {
    userCount: number;
    merchantCount: number;
    orderCount: number;
    todayRevenue: number;
    todayOrders: number;
    pendingAuditCount: number;
    todayNewUsers?: number;
    yesterdayOrders?: number;
    yesterdayRevenue?: number;
    yesterdayNewUsers?: number;
    ordersGrowth?: number;
    revenueGrowth?: number;
    userGrowth?: number;
    pendingMerchantCount?: number;
    pendingProductCount?: number;
    pendingAfterSaleCount?: number;
    totalProducts?: number;
    totalPatterns?: number;
}

/**
 * 趋势数据单日项
 */
export interface TrendItem {
    date: string;
    amount: number;
    orderCount: number;
}

/**
 * 趋势数据响应
 */
export interface DashboardTrendResponse {
    items: TrendItem[];
    days: number;
}

/**
 * 订单状态分布项
 */
export interface StatusItem {
    status: number;
    label: string;
    count: number;
}

/**
 * 排行榜项
 */
export interface RankItem {
    name: string;
    value: number;
    count: number;
}

/**
 * 概览数据响应
 */
export interface DashboardOverviewResponse {
    orderStatusDist: StatusItem[];
    topMerchants: RankItem[];
    topProducts: RankItem[];
}

// ==================== Mock 降级数据（后端不可用时前端仍可展示） ====================

const mockStats: DashboardStats = {
    userCount: 12580,
    merchantCount: 326,
    orderCount: 45280,
    todayRevenue: 19300,
    todayOrders: 156,
    pendingAuditCount: 28,
    todayNewUsers: 86,
    yesterdayOrders: 142,
    yesterdayRevenue: 16800,
    yesterdayNewUsers: 72,
    ordersGrowth: 9.86,
    revenueGrowth: 14.88,
    userGrowth: 19.44,
    pendingMerchantCount: 12,
    pendingProductCount: 16,
    pendingAfterSaleCount: 7,
    totalProducts: 2840,
    totalPatterns: 1560,
};

function buildMockTrend(days: number): DashboardTrendResponse {
    const items: TrendItem[] = [];
    const baseAmount = 15000;
    for (let i = days - 1; i >= 0; i--) {
        const date = new Date();
        date.setDate(date.getDate() - i);
        const dateStr = date.toISOString().slice(0, 10);
        const wave = Math.sin(i / 3) * 4000;
        const amount = Math.round(baseAmount + wave + Math.random() * 3000);
        items.push({
            date: dateStr,
            amount,
            orderCount: Math.round(amount / 120 + Math.random() * 10),
        });
    }
    return { items, days };
}

const mockOverview: DashboardOverviewResponse = {
    orderStatusDist: [
        { status: 0, label: '待付款', count: 320 },
        { status: 1, label: '已付款', count: 180 },
        { status: 2, label: '已发货', count: 240 },
        { status: 3, label: '已完成', count: 1860 },
        { status: 4, label: '已取消', count: 95 },
        { status: 5, label: '退款中', count: 28 },
        { status: 6, label: '已退款', count: 42 },
    ],
    topMerchants: [
        { name: '锦绣华服有限公司', value: 286000, count: 186 },
        { name: '云尚制衣厂', value: 213500, count: 142 },
        { name: '米兰裁缝铺', value: 168000, count: 98 },
        { name: '东方版型工作室', value: 142000, count: 86 },
        { name: '快捷纸样中心', value: 98000, count: 72 },
    ],
    topProducts: [
        { name: '女士修身连衣裙纸样', value: 86000, count: 320 },
        { name: '男士商务衬衫版型', value: 72000, count: 280 },
        { name: '儿童春秋外套纸样', value: 58000, count: 210 },
        { name: '高定西装套装版型', value: 45000, count: 96 },
        { name: '大码女装连衣裙', value: 38000, count: 180 },
    ],
};

// ==================== API 函数 ====================

/**
 * 获取仪表盘核心统计数据
 */
export async function getDashboardStats(): Promise<DashboardStats> {
    try {
        const data = await request<unknown, DashboardStats>({
            url: '/admin/dashboard/stats',
            method: 'get',
            showError: false,
        });
        return { ...mockStats, ...data };
    } catch {
        return { ...mockStats };
    }
}

/**
 * 获取近 N 天营收与订单趋势数据
 * @param days 天数（7 或 30，默认 7）
 */
export async function getDashboardTrend(days = 7): Promise<DashboardTrendResponse> {
    try {
        const data = await request<unknown, DashboardTrendResponse>({
            url: '/admin/dashboard/trend',
            method: 'get',
            params: { days },
            showError: false,
        });
        if (data && data.items && data.items.length > 0) {
            return data;
        }
        return buildMockTrend(days);
    } catch {
        return buildMockTrend(days);
    }
}

/**
 * 获取仪表盘概览数据（订单状态分布 + 商家/商品排行）
 */
export async function getDashboardOverview(): Promise<DashboardOverviewResponse> {
    try {
        const data = await request<unknown, DashboardOverviewResponse>({
            url: '/admin/dashboard/overview',
            method: 'get',
            showError: false,
        });
        if (data && data.orderStatusDist) {
            return { ...mockOverview, ...data };
        }
        return { ...mockOverview };
    } catch {
        return { ...mockOverview };
    }
}
