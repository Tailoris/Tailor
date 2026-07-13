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

/** 分页查询操作日志 */
export function listAuditLogs(params: AuditLogQueryParams) {
  return request<unknown, PageResult<AuditLog>>({
    url: '/admin/audit-logs',
    method: 'get',
    params,
  });
}

/** 获取操作日志详情 */
export function getAuditLogDetail(id: number) {
  return request<unknown, AuditLog>({
    url: `/admin/audit-logs/${id}`,
    method: 'get',
  });
}

/** 导出操作日志（返回 blob） */
export function exportAuditLogs(params: AuditLogQueryParams) {
  return request<unknown, Blob>({
    url: '/admin/audit-logs/export',
    method: 'post',
    data: params,
    responseType: 'blob',
  });
}
