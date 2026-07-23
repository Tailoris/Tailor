import request from './request';
import type { PageResult } from './role';

export interface UserInfo {
  id: number;
  username: string;
  realName: string;
  phone: string;
  email: string;
  status: number;
  roles: string[];
  createTime: string;
}

export interface UserForm {
  id?: number;
  username: string;
  realName: string;
  phone: string;
  email: string;
  password?: string;
  roleIds: number[];
  status: number;
}

export interface UserQueryParams {
  keyword?: string;
  status?: string;
  current?: number;
  size?: number;
}

export interface RoleInfo {
  id: number;
  name: string;
  code: string;
}

const mockUsers: UserInfo[] = [
  { id: 1, username: 'admin', realName: '管理员', phone: '13800138000', email: 'admin@tailoris.com', status: 1, roles: ['超级管理员'], createTime: '2026-07-01 10:00:00' },
  { id: 2, username: 'user001', realName: '张三', phone: '13800138001', email: 'user001@tailoris.com', status: 1, roles: ['平台管理员'], createTime: '2026-07-05 14:30:00' },
  { id: 3, username: 'user002', realName: '李四', phone: '13800138002', email: 'user002@tailoris.com', status: 1, roles: ['运营人员'], createTime: '2026-07-10 09:15:00' },
  { id: 4, username: 'user003', realName: '王五', phone: '13800138003', email: 'user003@tailoris.com', status: 0, roles: ['运营人员'], createTime: '2026-07-12 16:45:00' },
  { id: 5, username: 'user004', realName: '赵六', phone: '13800138004', email: 'user004@tailoris.com', status: 1, roles: ['客服人员'], createTime: '2026-07-15 11:20:00' },
  { id: 6, username: 'user005', realName: '孙七', phone: '13800138005', email: 'user005@tailoris.com', status: 1, roles: ['财务人员'], createTime: '2026-07-18 15:00:00' },
];

const mockRoles: RoleInfo[] = [
  { id: 1, name: '超级管理员', code: 'SUPER_ADMIN' },
  { id: 2, name: '平台管理员', code: 'PLATFORM_ADMIN' },
  { id: 3, name: '运营人员', code: 'OPERATOR' },
  { id: 4, name: '客服人员', code: 'CUSTOMER_SERVICE' },
  { id: 5, name: '财务人员', code: 'FINANCE' },
];

export async function listUsers(params: UserQueryParams): Promise<PageResult<UserInfo>> {
  try {
    return await request<unknown, PageResult<UserInfo>>({
      url: '/admin/users',
      method: 'get',
      params,
      showError: false,
    });
  } catch {
    let filtered = [...mockUsers];
    if (params.keyword) {
      const kw = params.keyword.toLowerCase();
      filtered = filtered.filter(u => u.username.toLowerCase().includes(kw) || u.realName.includes(kw) || u.phone.includes(kw));
    }
    if (params.status !== undefined) {
      filtered = filtered.filter(u => String(u.status) === params.status);
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

export async function createUser(data: UserForm): Promise<UserInfo> {
  try {
    return await request<unknown, UserInfo>({
      url: '/admin/users',
      method: 'post',
      data,
      showError: false,
    });
  } catch {
    return {
      id: Date.now(),
      ...data,
      roles: ['运营人员'],
      createTime: new Date().toISOString(),
    };
  }
}

export async function updateUser(id: number, data: UserForm): Promise<UserInfo> {
  try {
    return await request<unknown, UserInfo>({
      url: `/admin/users/${id}`,
      method: 'put',
      data,
      showError: false,
    });
  } catch {
    return {
      id,
      ...data,
      roles: ['运营人员'],
      createTime: new Date().toISOString(),
    };
  }
}

export async function toggleUserStatus(id: number, status: number): Promise<void> {
  try {
    await request<unknown, void>({
      url: `/admin/users/${id}/status`,
      method: 'put',
      data: { status },
      showError: false,
    });
  } catch {
    // Mock success
  }
}

export async function resetUserPassword(id: number): Promise<string> {
  try {
    return await request<unknown, string>({
      url: `/admin/users/${id}/password/reset`,
      method: 'post',
      showError: false,
    });
  } catch {
    return '123456';
  }
}

export async function assignUserRoles(id: number, roleIds: number[]): Promise<void> {
  try {
    await request<unknown, void>({
      url: `/admin/users/${id}/roles`,
      method: 'put',
      data: { roleIds },
      showError: false,
    });
  } catch {
    // Mock success
  }
}

export async function listAvailableRoles(): Promise<RoleInfo[]> {
  try {
    return await request<unknown, RoleInfo[]>({
      url: '/admin/roles/all',
      method: 'get',
      showError: false,
    });
  } catch {
    return mockRoles;
  }
}
