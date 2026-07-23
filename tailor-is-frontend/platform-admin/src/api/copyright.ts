import request from './request';
import type { PageResult } from './role';

export interface CopyrightInfo {
  id: number;
  workName: string;
  author: string;
  authorId: number;
  category: string;
  registrationNo: string;
  blockchainHash: string;
  status: 'pending' | 'registered' | 'rejected';
  fileUrl: string;
  description: string;
  registerTime: string;
  auditTime: string;
}

export interface InfringementInfo {
  id: number;
  workName: string;
  originalAuthor: string;
  suspectedWork: string;
  suspectedAuthor: string;
  similarity: number;
  status: 'detected' | 'pending' | 'confirmed' | 'ignored';
  detectTime: string;
  handleTime: string;
  description: string;
}

export interface CopyrightQueryParams {
  keyword?: string;
  status?: string;
  category?: string;
  current?: number;
  size?: number;
}

const mockCopyrights: CopyrightInfo[] = [
  {
    id: 1, workName: '时尚连衣裙设计', author: '张伟', authorId: 201, category: '服装设计',
    registrationNo: 'CR20260720001', blockchainHash: '0x7a3b8c9d...',
    status: 'registered', fileUrl: '', description: '原创连衣裙设计稿，含详细工艺说明',
    registerTime: '2026-07-20 10:30:00', auditTime: '2026-07-20 14:00:00',
  },
  {
    id: 2, workName: '男士衬衫纸样', author: '李明', authorId: 202, category: '纸样设计',
    registrationNo: '', blockchainHash: '',
    status: 'pending', fileUrl: '', description: '商务正装衬衫纸样设计',
    registerTime: '2026-07-23 09:15:00', auditTime: '',
  },
  {
    id: 3, workName: '童装系列设计', author: '王芳', authorId: 203, category: '服装设计',
    registrationNo: 'CR20260718001', blockchainHash: '0x5e9f2a1b...',
    status: 'registered', fileUrl: '', description: '2026秋冬童装系列，共12款设计',
    registerTime: '2026-07-18 08:00:00', auditTime: '2026-07-19 10:00:00',
  },
];

const mockInfringements: InfringementInfo[] = [
  {
    id: 1, workName: '时尚连衣裙设计', originalAuthor: '张伟', suspectedWork: '抄袭连衣裙设计',
    suspectedAuthor: '匿名用户', similarity: 95, status: 'detected',
    detectTime: '2026-07-23 10:30:00', handleTime: '',
    description: 'AI检测到高度相似设计，相似度达95%，建议人工审核',
  },
  {
    id: 2, workName: '男士衬衫纸样', originalAuthor: '李明', suspectedWork: '相似衬衫纸样',
    suspectedAuthor: '赵六', similarity: 78, status: 'pending',
    detectTime: '2026-07-23 09:15:00', handleTime: '',
    description: '核心版型结构相似度较高，需进一步判断',
  },
  {
    id: 3, workName: '风衣外套纸样', originalAuthor: '刘强', suspectedWork: '类风衣纸样',
    suspectedAuthor: '周八', similarity: 62, status: 'ignored',
    detectTime: '2026-07-20 14:00:00', handleTime: '2026-07-21 09:00:00',
    description: '经审核属于行业通用版型，不构成侵权',
  },
];

export async function listCopyrights(params: CopyrightQueryParams): Promise<PageResult<CopyrightInfo>> {
  try {
    return await request<unknown, PageResult<CopyrightInfo>>({
      url: '/admin/copyright',
      method: 'get',
      params,
      showError: false,
    });
  } catch {
    let filtered = [...mockCopyrights];
    if (params.keyword) {
      const kw = params.keyword.toLowerCase();
      filtered = filtered.filter(c =>
        c.workName.toLowerCase().includes(kw) ||
        c.author.toLowerCase().includes(kw) ||
        c.registrationNo.includes(kw)
      );
    }
    if (params.status) {
      filtered = filtered.filter(c => c.status === params.status);
    }
    if (params.category) {
      filtered = filtered.filter(c => c.category === params.category);
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

export async function registerCopyright(data: { workName: string; author: string; category: string; description: string }): Promise<CopyrightInfo> {
  try {
    return await request<unknown, CopyrightInfo>({
      url: '/admin/copyright',
      method: 'post',
      data,
      showError: false,
    });
  } catch {
    return {
      id: Date.now(),
      ...data,
      authorId: 0,
      registrationNo: `CR${Date.now()}`,
      blockchainHash: `0x${Math.random().toString(16).slice(2)}`,
      status: 'pending',
      fileUrl: '',
      registerTime: new Date().toISOString().replace('T', ' ').slice(0, 19),
      auditTime: '',
    };
  }
}

export async function approveCopyright(id: number, approved: boolean, reason?: string): Promise<void> {
  try {
    await request<unknown, void>({
      url: `/admin/copyright/${id}/audit`,
      method: 'post',
      data: { approved, reason },
      showError: false,
    });
  } catch {
    // Mock success
  }
}

export async function listInfringements(params: CopyrightQueryParams): Promise<PageResult<InfringementInfo>> {
  try {
    return await request<unknown, PageResult<InfringementInfo>>({
      url: '/admin/copyright/infringement',
      method: 'get',
      params,
      showError: false,
    });
  } catch {
    let filtered = [...mockInfringements];
    if (params.status) {
      filtered = filtered.filter(i => i.status === params.status);
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

export async function handleInfringement(id: number, action: 'confirmed' | 'ignored', remark?: string): Promise<void> {
  try {
    await request<unknown, void>({
      url: `/admin/copyright/infringement/${id}/handle`,
      method: 'post',
      data: { action, remark },
      showError: false,
    });
  } catch {
    // Mock success
  }
}

export async function detectInfringement(): Promise<InfringementInfo[]> {
  try {
    return await request<unknown, InfringementInfo[]>({
      url: '/admin/copyright/infringement/detect',
      method: 'post',
      showError: false,
    });
  } catch {
    return mockInfringements;
  }
}