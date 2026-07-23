import request from './request';
import type { PageResult } from './role';

export interface PatternInfo {
  id: number;
  name: string;
  designer: string;
  designerId: number;
  category: string;
  price: number;
  score: number;
  format: string;
  size: string;
  status: 'pending' | 'approved' | 'rejected';
  tags: string[];
  description: string;
  uploadTime: string;
  auditTime: string;
}

export interface PatternForm {
  id?: number;
  name: string;
  designerId: number;
  category: string;
  price: number;
  format: string;
  size: string;
  tags: string[];
  description: string;
}

export interface PatternQueryParams {
  keyword?: string;
  status?: string;
  category?: string;
  current?: number;
  size?: number;
}

const mockPatterns: PatternInfo[] = [
  {
    id: 1, name: '女士连衣裙纸样', designer: '张伟', designerId: 201, category: '连衣裙',
    price: 129, score: 92, format: 'DXF', size: 'A3',
    status: 'approved', tags: ['版型精准', '工艺清晰', '适配性高'],
    description: '2026夏季新款连衣裙纸样，含S/M/L/XL四个尺码', uploadTime: '2026-07-20 10:30:00', auditTime: '2026-07-20 14:00:00',
  },
  {
    id: 2, name: '男士衬衫纸样', designer: '李明', designerId: 202, category: '衬衫',
    price: 89, score: 88, format: 'PDF', size: 'A4',
    status: 'pending', tags: ['工艺清晰', '适配性高'],
    description: '商务正装衬衫纸样，适合机构定制', uploadTime: '2026-07-22 09:15:00', auditTime: '',
  },
  {
    id: 3, name: '休闲T恤纸样', designer: '王芳', designerId: 203, category: 'T恤',
    price: 59, score: 85, format: 'DXF', size: 'A3',
    status: 'approved', tags: ['版型精准', '适配性高'],
    description: '基础款T恤纸样，多色可选', uploadTime: '2026-07-18 08:00:00', auditTime: '2026-07-19 10:00:00',
  },
  {
    id: 4, name: '风衣外套纸样', designer: '刘强', designerId: 204, category: '外套',
    price: 199, score: 95, format: 'AI', size: 'A3',
    status: 'pending', tags: ['版型精准', '工艺清晰', '适配性高'],
    description: '双排扣风衣纸样，含内衬设计', uploadTime: '2026-07-23 10:30:00', auditTime: '',
  },
];

export async function listPatterns(params: PatternQueryParams): Promise<PageResult<PatternInfo>> {
  try {
    return await request<unknown, PageResult<PatternInfo>>({
      url: '/admin/pattern',
      method: 'get',
      params,
      showError: false,
    });
  } catch {
    let filtered = [...mockPatterns];
    if (params.keyword) {
      const kw = params.keyword.toLowerCase();
      filtered = filtered.filter(p =>
        p.name.toLowerCase().includes(kw) ||
        p.designer.toLowerCase().includes(kw) ||
        p.category.toLowerCase().includes(kw)
      );
    }
    if (params.status) {
      filtered = filtered.filter(p => p.status === params.status);
    }
    if (params.category) {
      filtered = filtered.filter(p => p.category === params.category);
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

export async function getPatternById(id: number): Promise<PatternInfo> {
  try {
    return await request<unknown, PatternInfo>({
      url: `/admin/pattern/${id}`,
      method: 'get',
      showError: false,
    });
  } catch {
    return mockPatterns.find(p => p.id === id) || mockPatterns[0];
  }
}

export async function createPattern(data: PatternForm): Promise<PatternInfo> {
  try {
    return await request<unknown, PatternInfo>({
      url: '/admin/pattern',
      method: 'post',
      data,
      showError: false,
    });
  } catch {
    return {
      id: Date.now(),
      ...data,
      designer: '新设计师',
      score: 0,
      status: 'pending',
      uploadTime: new Date().toISOString().replace('T', ' ').slice(0, 19),
      auditTime: '',
    };
  }
}

export async function updatePattern(id: number, data: PatternForm): Promise<PatternInfo> {
  try {
    return await request<unknown, PatternInfo>({
      url: `/admin/pattern/${id}`,
      method: 'put',
      data,
      showError: false,
    });
  } catch {
    const existing = mockPatterns.find(p => p.id === id);
    return { ...existing!, ...data };
  }
}

export async function approvePattern(id: number, approved: boolean, reason?: string): Promise<void> {
  try {
    await request<unknown, void>({
      url: `/admin/pattern/${id}/audit`,
      method: 'post',
      data: { approved, reason },
      showError: false,
    });
  } catch {
    // Mock success
  }
}

export async function deletePattern(id: number): Promise<void> {
  try {
    await request<unknown, void>({
      url: `/admin/pattern/${id}`,
      method: 'delete',
      showError: false,
    });
  } catch {
    // Mock success
  }
}