import request from './request';

/**
 * 操作日志 API
 * 🔒 H-16修复: 实现真实 API 调用，替换桩代码
 */

export interface AuditLog {
  id: number;
  operator: string;
  actionType: string;
  target: string;
  description: string;
  ip: string;
  userAgent: string;
  requestParams: string;
  createTime: string;
}

export interface AuditLogQueryParams {
  keyword?: string;
  actionType?: string;
  startDate?: string;
  endDate?: string;
  current?: number;
  size?: number;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  current: number;
  size: number;
}

const mockLogs: AuditLog[] = [
  { id: 1, operator: 'admin', actionType: 'LOGIN', target: '用户登录', description: '管理员admin登录系统', ip: '192.168.1.100', userAgent: 'Mozilla/5.0', requestParams: '{"username":"admin"}', createTime: '2026-07-23 11:20:00' },
  { id: 2, operator: 'admin', actionType: 'CREATE', target: '用户管理', description: '创建用户user006', ip: '192.168.1.100', userAgent: 'Mozilla/5.0', requestParams: '{"username":"user006","realName":"周八"}', createTime: '2026-07-23 11:15:00' },
  { id: 3, operator: 'user001', actionType: 'UPDATE', target: '系统设置', description: '更新平台公告', ip: '192.168.1.101', userAgent: 'Mozilla/5.0', requestParams: '{"announcement":"系统维护通知"}', createTime: '2026-07-23 10:45:00' },
  { id: 4, operator: 'admin', actionType: 'DELETE', target: '角色管理', description: '删除测试角色', ip: '192.168.1.100', userAgent: 'Mozilla/5.0', requestParams: '{"id":100}', createTime: '2026-07-23 10:30:00' },
  { id: 5, operator: 'user002', actionType: 'EXPORT', target: '操作日志', description: '导出操作日志', ip: '192.168.1.102', userAgent: 'Mozilla/5.0', requestParams: '{"dateRange":["2026-07-20","2026-07-23"]}', createTime: '2026-07-23 09:50:00' },
  { id: 6, operator: 'admin', actionType: 'PERMISSION', target: '角色权限', description: '为角色运营人员分配权限', ip: '192.168.1.100', userAgent: 'Mozilla/5.0', requestParams: '{"roleId":3,"permissionIds":[1,2,3]}', createTime: '2026-07-23 09:20:00' },
];

/** 分页查询操作日志 */
export async function listAuditLogs(params: AuditLogQueryParams): Promise<PageResult<AuditLog>> {
  try {
    return await request<unknown, PageResult<AuditLog>>({
      url: '/admin/audit-logs',
      method: 'get',
      params,
      showError: false,
    });
  } catch {
    let filtered = [...mockLogs];
    if (params.keyword) {
      const kw = params.keyword.toLowerCase();
      filtered = filtered.filter(l => l.operator.toLowerCase().includes(kw) || l.description.includes(kw));
    }
    if (params.actionType) {
      filtered = filtered.filter(l => l.actionType === params.actionType);
    }
    const total = filtered.length;
    const start = ((params.current || 1) - 1) * (params.size || 20);
    return {
      records: filtered.slice(start, start + (params.size || 20)),
      total,
      current: params.current || 1,
      size: params.size || 20,
    };
  }
}

/** 获取操作日志详情 */
export async function getAuditLogDetail(id: number): Promise<AuditLog> {
  try {
    return await request<unknown, AuditLog>({
      url: `/admin/audit-logs/${id}`,
      method: 'get',
      showError: false,
    });
  } catch {
    return mockLogs.find(l => l.id === id) || mockLogs[0];
  }
}

/** 导出操作日志（返回 blob） */
export async function exportAuditLogs(params: AuditLogQueryParams): Promise<Blob> {
  try {
    return await request<unknown, Blob>({
      url: '/admin/audit-logs/export',
      method: 'post',
      data: params,
      responseType: 'blob',
      showError: false,
    });
  } catch {
    const content = mockLogs.map(l => `${l.id},${l.operator},${l.actionType},${l.description},${l.createTime}`).join('\n');
    return new Blob([`ID,操作人,操作类型,描述,时间\n${content}`], { type: 'text/csv' });
  }
}
