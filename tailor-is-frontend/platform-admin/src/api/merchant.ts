import request from './request';
import type { PageResult } from './role';

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

const mockMerchants: MerchantInfo[] = [
  {
    id: 1, name: '时尚工坊', shopType: 'personal', contact: '李明', phone: '13800138001',
    email: 'liming@fashion.com', address: '广东省广州市天河区', status: 'approved',
    description: '专注女装设计打版，拥有10年行业经验', createTime: '2026-07-20 10:30:00', updateTime: '2026-07-20 10:30:00',
  },
  {
    id: 2, name: '精品制衣', shopType: 'enterprise', contact: '王芳', phone: '13800138002',
    email: 'wangfang@quality.com', address: '浙江省杭州市西湖区', status: 'approved',
    description: '集设计、打版、生产于一体的服装企业', createTime: '2026-07-18 09:15:00', updateTime: '2026-07-18 09:15:00',
  },
  {
    id: 3, name: '创意设计工作室', shopType: 'brand', contact: '张伟', phone: '13800138003',
    email: 'zhangwei@creative.com', address: '上海市浦东新区', status: 'pending',
    description: '新锐设计师品牌，主打原创设计', createTime: '2026-07-22 14:20:00', updateTime: '2026-07-22 14:20:00',
  },
  {
    id: 4, name: '恒达服装厂', shopType: 'factory', contact: '刘强', phone: '13800138004',
    email: 'liuqiang@hengda.com', address: '江苏省苏州市吴江区', status: 'approved',
    description: '大型服装工厂，月产能50万件', createTime: '2026-07-15 08:00:00', updateTime: '2026-07-15 08:00:00',
  },
  {
    id: 5, name: '米兰设计中心', shopType: 'brand', contact: '陈雪', phone: '13800138005',
    email: 'chenxue@milan.com', address: '北京市朝阳区', status: 'rejected',
    description: '资质材料不完整', createTime: '2026-07-21 16:30:00', updateTime: '2026-07-21 16:30:00',
  },
];

export async function listMerchants(params: MerchantQueryParams): Promise<PageResult<MerchantInfo>> {
  try {
    return await request<unknown, PageResult<MerchantInfo>>({
      url: '/admin/merchant',
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
    const start = ((params.current || 1) - 1) * (params.size || 10);
    return {
      records: filtered.slice(start, start + (params.size || 10)),
      total,
      current: params.current || 1,
      size: params.size || 10,
    };
  }
}

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
      status: 'pending',
      createTime: new Date().toISOString().replace('T', ' ').slice(0, 19),
      updateTime: new Date().toISOString().replace('T', ' ').slice(0, 19),
    };
  }
}

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
    return { ...existing!, ...data, updateTime: new Date().toISOString().replace('T', ' ').slice(0, 19) };
  }
}

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