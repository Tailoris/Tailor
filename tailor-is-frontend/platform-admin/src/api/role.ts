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

/** 分页查询角色列表 */
export function listRoles(params: RoleQueryParams) {
  return request<unknown, PageResult<Role>>({
    url: '/admin/roles',
    method: 'get',
    params,
  });
}

/** 获取角色详情 */
export function getRoleDetail(id: number) {
  return request<unknown, Role>({
    url: `/admin/roles/${id}`,
    method: 'get',
  });
}

/** 新增角色 */
export function createRole(data: RoleForm) {
  return request<unknown, Role>({
    url: '/admin/roles',
    method: 'post',
    data,
  });
}

/** 更新角色 */
export function updateRole(id: number, data: RoleForm) {
  return request<unknown, Role>({
    url: `/admin/roles/${id}`,
    method: 'put',
    data,
  });
}

/** 删除角色 */
export function deleteRole(id: number) {
  return request<unknown, void>({
    url: `/admin/roles/${id}`,
    method: 'delete',
  });
}

/** 获取角色权限列表 */
export function getRolePermissions(id: number) {
  return request<unknown, Permission[]>({
    url: `/admin/roles/${id}/permissions`,
    method: 'get',
  });
}

/** 分配角色权限 */
export function assignRolePermissions(id: number, permissionIds: number[]) {
  return request<unknown, void>({
    url: `/admin/roles/${id}/permissions`,
    method: 'put',
    data: { permissionIds },
  });
}

/** 获取角色关联用户列表 */
export function getRoleUsers(id: number, params?: { current?: number; size?: number }) {
  return request<unknown, PageResult<{ id: number; username: string; nickname: string }>>({
    url: `/admin/roles/${id}/users`,
    method: 'get',
    params,
  });
}
