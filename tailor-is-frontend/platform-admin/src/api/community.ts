import request from './request';
import type { PageResult } from './role';

export interface PostInfo {
  id: number;
  title: string;
  author: string;
  authorId: number;
  content: string;
  board: string;
  status: 'pending' | 'approved' | 'rejected';
  viewCount: number;
  likeCount: number;
  commentCount: number;
  createTime: string;
  auditTime: string;
}

export interface ReportInfo {
  id: number;
  postId: number;
  postTitle: string;
  reporter: string;
  reporterId: number;
  reason: string;
  description: string;
  status: 'pending' | 'confirmed' | 'ignored';
  result: string;
  reportTime: string;
  handleTime: string;
}

export interface CommunityQueryParams {
  keyword?: string;
  status?: string;
  board?: string;
  current?: number;
  size?: number;
}

const mockPosts: PostInfo[] = [
  {
    id: 1, title: '新款连衣裙设计分享', author: '张三', authorId: 101,
    content: '今天分享一款2026夏季新款连衣裙的设计思路和制作过程。从灵感来源到成品展示，每一个细节都经过精心打磨...',
    board: '设计交流', status: 'pending', viewCount: 1250, likeCount: 89, commentCount: 23,
    createTime: '2026-07-23 10:30:00', auditTime: '',
  },
  {
    id: 2, title: '纸样制作技巧分享', author: '李四', authorId: 102,
    content: '分享一些纸样制作的实用技巧，包括如何快速绘制基础版型、如何处理复杂曲线等...',
    board: '技术分享', status: 'approved', viewCount: 3580, likeCount: 256, commentCount: 67,
    createTime: '2026-07-22 14:20:00', auditTime: '2026-07-22 16:00:00',
  },
  {
    id: 3, title: '面辅料选购指南', author: '王五', authorId: 103,
    content: '新手设计师如何选择合适的面辅料？这里分享一些选购经验和注意事项...',
    board: '资源对接', status: 'pending', viewCount: 890, likeCount: 45, commentCount: 12,
    createTime: '2026-07-23 08:00:00', auditTime: '',
  },
  {
    id: 4, title: '工厂合作经验分享', author: '赵六', authorId: 104,
    content: '与工厂合作是服装设计变现的关键环节，分享一些与工厂沟通协作的经验...',
    board: '资源对接', status: 'rejected', viewCount: 0, likeCount: 0, commentCount: 0,
    createTime: '2026-07-21 16:00:00', auditTime: '2026-07-21 18:00:00',
  },
];

const mockReports: ReportInfo[] = [
  {
    id: 1, postId: 1, postTitle: '新款连衣裙设计分享', reporter: '王五', reporterId: 103,
    reason: '涉及广告宣传', description: '帖子中包含推广链接，疑似商业广告',
    status: 'pending', result: '', reportTime: '2026-07-23 14:00:00', handleTime: '',
  },
  {
    id: 2, postId: 3, postTitle: '面辅料选购指南', reporter: '用户A', reporterId: 105,
    reason: '内容不当', description: '帖子中存在不文明用语',
    status: 'confirmed', result: '已删除不当内容，警告用户', reportTime: '2026-07-23 10:00:00', handleTime: '2026-07-23 12:00:00',
  },
];

export async function listPosts(params: CommunityQueryParams): Promise<PageResult<PostInfo>> {
  try {
    return await request<unknown, PageResult<PostInfo>>({
      url: '/admin/community',
      method: 'get',
      params,
      showError: false,
    });
  } catch {
    let filtered = [...mockPosts];
    if (params.keyword) {
      const kw = params.keyword.toLowerCase();
      filtered = filtered.filter(p =>
        p.title.toLowerCase().includes(kw) ||
        p.content.toLowerCase().includes(kw) ||
        p.author.includes(kw)
      );
    }
    if (params.status) {
      filtered = filtered.filter(p => p.status === params.status);
    }
    if (params.board) {
      filtered = filtered.filter(p => p.board === params.board);
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

export async function getPostById(id: number): Promise<PostInfo> {
  try {
    return await request<unknown, PostInfo>({
      url: `/admin/community/${id}`,
      method: 'get',
      showError: false,
    });
  } catch {
    return mockPosts.find(p => p.id === id) || mockPosts[0];
  }
}

export async function approvePost(id: number, approved: boolean, reason?: string): Promise<void> {
  try {
    await request<unknown, void>({
      url: `/admin/community/${id}/audit`,
      method: 'post',
      data: { approved, reason },
      showError: false,
    });
  } catch {
    // Mock success
  }
}

export async function deletePost(id: number): Promise<void> {
  try {
    await request<unknown, void>({
      url: `/admin/community/${id}`,
      method: 'delete',
      showError: false,
    });
  } catch {
    // Mock success
  }
}

export async function listReports(params: CommunityQueryParams): Promise<PageResult<ReportInfo>> {
  try {
    return await request<unknown, PageResult<ReportInfo>>({
      url: '/admin/community/reports',
      method: 'get',
      params,
      showError: false,
    });
  } catch {
    let filtered = [...mockReports];
    if (params.status) {
      filtered = filtered.filter(r => r.status === params.status);
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

export async function handleReport(id: number, action: 'confirmed' | 'ignored', remark?: string): Promise<void> {
  try {
    await request<unknown, void>({
      url: `/admin/community/reports/${id}/handle`,
      method: 'post',
      data: { action, remark },
      showError: false,
    });
  } catch {
    // Mock success
  }
}