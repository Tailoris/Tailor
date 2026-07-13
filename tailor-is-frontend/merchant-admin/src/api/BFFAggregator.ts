/**
 * BFF (Backend For Frontend) 聚合层
 *
 * 用途：将多个后端 API 调用聚合为单个请求，减少前端网络往返次数
 * 最佳实践：
 *   1. 优先使用后端 BFF 服务聚合（如 /dashboard/aggregated），由网关统一返回
 *   2. 若后端暂不支持，前端使用 Promise.all / Promise.allSettled 并行调用
 *   3. 聚合接口应有统一的错误处理策略（部分失败不影响整体渲染）
 *
 * 使用示例：
 *   import { getDashboardData } from '@/api/BFFAggregator'
 *   const { stats, orders, finance } = await getDashboardData()
 */

import { getDashboardStats } from './dashboard';
import { listOrders } from './order';
import { getBalance } from './finance';
import { getSettlementInfo } from './finance';
import { listProducts } from './product';
import { getCommissionDetail } from './finance';
import type { DashboardStats, Order, PageResponse, SettlementRecord } from '@/types';

// ================================================================
// 聚合结果类型定义
// ================================================================

export interface AggregatedDashboardData {
  stats: DashboardStats;
  recentOrders: PageResponse<Order>;
  balance: { withdrawableAmount: number; totalEarnings: number; totalWithdrawn: number };
  commission: { totalCommission: number; commissionRate: number; monthlyCommission: number };
  recentSettlements: PageResponse<SettlementRecord>;
  productCount: number;
}

// ================================================================
// 首页仪表板数据聚合
// 说明：并行调用多个后端接口，使用 Promise.all 同时发起请求
// 若后端提供了 BFF 聚合端点（如 GET /dashboard/aggregated），
// 请优先使用后端聚合以进一步减少网络开销
// ================================================================

/**
 * 获取仪表板聚合数据
 * 并行调用：统计概览、最近订单、余额、佣金、结算、商品数量
 */
export async function getDashboardData(): Promise<AggregatedDashboardData> {
  const [stats, recentOrders, balance, commission, recentSettlements, productResult] = await Promise.all([
    getDashboardStats(),
    listOrders({ current: 1, size: 5 }),
    getBalance(),
    getCommissionDetail(),
    getSettlementInfo({ current: 1, size: 5 }),
    listProducts({ current: 1, size: 1 }),
  ]);

  return {
    stats,
    recentOrders,
    balance,
    commission,
    recentSettlements,
    productCount: productResult.total,
  };
}

// ================================================================
// 部分失败容错示例
// 使用 Promise.allSettled 确保某个接口失败不会导致整体崩溃
// ================================================================

/**
 * 获取仪表板聚合数据（容错版本）
 * 单个接口失败不影响其他数据展示，缺失数据用默认值填充
 */
export async function getDashboardDataSafe(): Promise<Partial<AggregatedDashboardData>> {
  const results = await Promise.allSettled([
    getDashboardStats(),
    listOrders({ current: 1, size: 5 }),
    getBalance(),
    getCommissionDetail(),
    getSettlementInfo({ current: 1, size: 5 }),
    listProducts({ current: 1, size: 1 }),
  ]);

  const data: Partial<AggregatedDashboardData> = {};

  if (results[0].status === 'fulfilled') data.stats = results[0].value;
  if (results[1].status === 'fulfilled') data.recentOrders = results[1].value;
  if (results[2].status === 'fulfilled') data.balance = results[2].value;
  if (results[3].status === 'fulfilled') data.commission = results[3].value;
  if (results[4].status === 'fulfilled') data.recentSettlements = results[4].value;
  if (results[5].status === 'fulfilled') data.productCount = results[5].value.total;

  return data;
}
