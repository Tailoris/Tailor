import request from './request';
import type { PageResult } from './role';

export interface CouponInfo {
  id: number;
  name: string;
  type: 'fixed' | 'percent';
  discount: number;
  minAmount: number;
  totalCount: number;
  usedCount: number;
  startTime: string;
  endTime: string;
  status: 'upcoming' | 'active' | 'ended';
  description: string;
  createTime: string;
}

export interface CampaignInfo {
  id: number;
  name: string;
  type: string;
  startTime: string;
  endTime: string;
  participants: number;
  targetCount: number;
  budget: number;
  status: 'preparing' | 'active' | 'ended';
  description: string;
  createTime: string;
}

export interface MarketingQueryParams {
  keyword?: string;
  status?: string;
  type?: string;
  current?: number;
  size?: number;
}

const mockCoupons: CouponInfo[] = [
  {
    id: 1, name: '新人专享券', type: 'fixed', discount: 50, minAmount: 200,
    totalCount: 1000, usedCount: 328, startTime: '2026-07-01', endTime: '2026-12-31',
    status: 'active', description: '新用户注册即可领取，满200减50元',
    createTime: '2026-07-01 10:00:00',
  },
  {
    id: 2, name: '满减优惠券', type: 'fixed', discount: 100, minAmount: 500,
    totalCount: 500, usedCount: 156, startTime: '2026-07-15', endTime: '2026-08-15',
    status: 'active', description: '限时满减优惠，满500减100元',
    createTime: '2026-07-15 08:00:00',
  },
  {
    id: 3, name: '八折优惠', type: 'percent', discount: 20, minAmount: 0,
    totalCount: 300, usedCount: 87, startTime: '2026-07-20', endTime: '2026-07-31',
    status: 'active', description: '全场商品八折优惠',
    createTime: '2026-07-20 09:00:00',
  },
  {
    id: 4, name: '春节大促券', type: 'fixed', discount: 200, minAmount: 1000,
    totalCount: 200, usedCount: 0, startTime: '2026-08-01', endTime: '2026-08-15',
    status: 'upcoming', description: '即将开始的春节促销活动',
    createTime: '2026-07-22 10:00:00',
  },
];

const mockCampaigns: CampaignInfo[] = [
  {
    id: 1, name: '夏季促销活动', type: '促销', startTime: '2026-07-20', endTime: '2026-07-31',
    participants: 5280, targetCount: 10000, budget: 50000,
    status: 'active', description: '夏季服装纸样全场8折，满500再减100',
    createTime: '2026-07-20 00:00:00',
  },
  {
    id: 2, name: '新品发布活动', type: '新品', startTime: '2026-08-01', endTime: '2026-08-15',
    participants: 0, targetCount: 5000, budget: 30000,
    status: 'preparing', description: '2026秋冬新品发布，注册即送50元优惠券',
    createTime: '2026-07-22 10:00:00',
  },
  {
    id: 3, name: '设计师大赛', type: '比赛', startTime: '2026-07-01', endTime: '2026-07-15',
    participants: 1250, targetCount: 2000, budget: 20000,
    status: 'ended', description: '原创设计大赛圆满结束，获奖作品已公示',
    createTime: '2026-07-01 00:00:00',
  },
];

export async function listCoupons(params: MarketingQueryParams): Promise<PageResult<CouponInfo>> {
  try {
    return await request<unknown, PageResult<CouponInfo>>({
      url: '/admin/marketing/coupon',
      method: 'get',
      params,
      showError: false,
    });
  } catch {
    let filtered = [...mockCoupons];
    if (params.keyword) {
      const kw = params.keyword.toLowerCase();
      filtered = filtered.filter(c => c.name.toLowerCase().includes(kw));
    }
    if (params.status) {
      filtered = filtered.filter(c => c.status === params.status);
    }
    const total = filtered.length;
    const start = ((params.current || 1) - 1) * (params.size || 10);
    return {
      records: filtered.slice(start, start + (params.size || 10)),
      total,
      current: params.current || 1,
      size: params.size || 10,
    };
  }
}

export async function createCoupon(data: Partial<CouponInfo>): Promise<CouponInfo> {
  try {
    return await request<unknown, CouponInfo>({
      url: '/admin/marketing/coupon',
      method: 'post',
      data,
      showError: false,
    });
  } catch {
    return {
      id: Date.now(),
      ...data,
      usedCount: 0,
      createTime: new Date().toISOString().replace('T', ' ').slice(0, 19),
    } as CouponInfo;
  }
}

export async function updateCoupon(id: number, data: Partial<CouponInfo>): Promise<CouponInfo> {
  try {
    return await request<unknown, CouponInfo>({
      url: `/admin/marketing/coupon/${id}`,
      method: 'put',
      data,
      showError: false,
    });
  } catch {
    const existing = mockCoupons.find(c => c.id === id);
    return { ...existing!, ...data };
  }
}

export async function deleteCoupon(id: number): Promise<void> {
  try {
    await request<unknown, void>({
      url: `/admin/marketing/coupon/${id}`,
      method: 'delete',
      showError: false,
    });
  } catch {
    // Mock success
  }
}

export async function listCampaigns(params: MarketingQueryParams): Promise<PageResult<CampaignInfo>> {
  try {
    return await request<unknown, PageResult<CampaignInfo>>({
      url: '/admin/marketing/campaign',
      method: 'get',
      params,
      showError: false,
    });
  } catch {
    let filtered = [...mockCampaigns];
    if (params.keyword) {
      const kw = params.keyword.toLowerCase();
      filtered = filtered.filter(c => c.name.toLowerCase().includes(kw));
    }
    if (params.status) {
      filtered = filtered.filter(c => c.status === params.status);
    }
    const total = filtered.length;
    const start = ((params.current || 1) - 1) * (params.size || 10);
    return {
      records: filtered.slice(start, start + (params.size || 10)),
      total,
      current: params.current || 1,
      size: params.size || 10,
    };
  }
}

export async function createCampaign(data: Partial<CampaignInfo>): Promise<CampaignInfo> {
  try {
    return await request<unknown, CampaignInfo>({
      url: '/admin/marketing/campaign',
      method: 'post',
      data,
      showError: false,
    });
  } catch {
    return {
      id: Date.now(),
      ...data,
      participants: 0,
      createTime: new Date().toISOString().replace('T', ' ').slice(0, 19),
    } as CampaignInfo;
  }
}

export async function updateCampaign(id: number, data: Partial<CampaignInfo>): Promise<CampaignInfo> {
  try {
    return await request<unknown, CampaignInfo>({
      url: `/admin/marketing/campaign/${id}`,
      method: 'put',
      data,
      showError: false,
    });
  } catch {
    const existing = mockCampaigns.find(c => c.id === id);
    return { ...existing!, ...data };
  }
}

export async function deleteCampaign(id: number): Promise<void> {
  try {
    await request<unknown, void>({
      url: `/admin/marketing/campaign/${id}`,
      method: 'delete',
      showError: false,
    });
  } catch {
    // Mock success
  }
}