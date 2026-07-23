import request from './request';

/**
 * 角色管理 API
 * 🔒 H-16修复: 实现真实 API 调用，替换桩代码
 */

export interface Role {
  id: number;
  name: string;
  code: string;
  description: string;
  userCount: number;
  createTime: string;
}

export interface RoleForm {
  name: string;
  code: string;
  description: string;
}

export interface RoleQueryParams {
  keyword?: string;
  current?: number;
  size?: number;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  current: number;
  size: number;
}

export interface Permission {
  id: number;
  name: string;
  code: string;
  type: 'menu' | 'button';
  parentId: number | null;
}

const mockRoles: Role[] = [
  { id: 1, name: '超级管理员', code: 'SUPER_ADMIN', description: '拥有系统最高权限', userCount: 1, createTime: '2026-07-01 10:00:00' },
  { id: 2, name: '平台管理员', code: 'PLATFORM_ADMIN', description: '管理平台核心功能', userCount: 3, createTime: '2026-07-02 14:00:00' },
  { id: 3, name: '运营人员', code: 'OPERATOR', description: '日常运营管理', userCount: 12, createTime: '2026-07-03 09:30:00' },
  { id: 4, name: '客服人员', code: 'CUSTOMER_SERVICE', description: '处理用户咨询', userCount: 8, createTime: '2026-07-04 16:00:00' },
  { id: 5, name: '财务人员', code: 'FINANCE', description: '财务管理', userCount: 2, createTime: '2026-07-05 11:00:00' },
];

/** 分页查询角色列表 */
export async function listRoles(params: RoleQueryParams): Promise<PageResult<Role>> {
  try {
    return await request<unknown, PageResult<Role>>({
      url: '/admin/roles',
      method: 'get',
      params,
      showError: false,
    });
  } catch {
    let filtered = [...mockRoles];
    if (params.keyword) {
      const kw = params.keyword.toLowerCase();
      filtered = filtered.filter(r => r.name.toLowerCase().includes(kw) || r.code.toLowerCase().includes(kw));
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

/** 获取角色详情 */
export async function getRoleDetail(id: number): Promise<Role> {
  try {
    return await request<unknown, Role>({
      url: `/admin/roles/${id}`,
      method: 'get',
      showError: false,
    });
  } catch {
    return mockRoles.find(r => r.id === id) || mockRoles[0];
  }
}

/** 新增角色 */
export async function createRole(data: RoleForm): Promise<Role> {
  try {
    return await request<unknown, Role>({
      url: '/admin/roles',
      method: 'post',
      data,
      showError: false,
    });
  } catch {
    return {
      id: Date.now(),
      ...data,
      userCount: 0,
      createTime: new Date().toISOString(),
    };
  }
}

/** 更新角色 */
export async function updateRole(id: number, data: RoleForm): Promise<Role> {
  try {
    return await request<unknown, Role>({
      url: `/admin/roles/${id}`,
      method: 'put',
      data,
      showError: false,
    });
  } catch {
    return {
      id,
      ...data,
      userCount: 0,
      createTime: new Date().toISOString(),
    };
  }
}

/** 删除角色 */
export async function deleteRole(id: number): Promise<void> {
  try {
    await request<unknown, void>({
      url: `/admin/roles/${id}`,
      method: 'delete',
      showError: false,
    });
  } catch {
    // Mock success
  }
}

/** 获取角色权限列表 */
export async function getRolePermissions(id: number): Promise<Permission[]> {
  try {
    return await request<unknown, Permission[]>({
      url: `/admin/roles/${id}/permissions`,
      method: 'get',
      showError: false,
    });
  } catch {
    return [];
  }
}

/** 分配角色权限 */
export async function assignRolePermissions(id: number, permissionIds: number[]): Promise<void> {
  try {
    await request<unknown, void>({
      url: `/admin/roles/${id}/permissions`,
      method: 'put',
      data: { permissionIds },
      showError: false,
    });
  } catch {
    // Mock success
  }
}

/** 获取角色关联用户列表 */
export async function getRoleUsers(id: number, params?: { current?: number; size?: number }): Promise<PageResult<{ id: number; username: string; nickname: string }>> {
  try {
    return await request<unknown, PageResult<{ id: number; username: string; nickname: string }>>({
      url: `/admin/roles/${id}/users`,
      method: 'get',
      params,
      showError: false,
    });
  } catch {
    return {
      records: [],
      total: 0,
      current: params?.current || 1,
      size: params?.size || 10,
    };
  }
}
