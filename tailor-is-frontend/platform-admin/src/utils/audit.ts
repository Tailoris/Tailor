import type { AxiosInstance } from 'axios';
import request from '@/api/request';

export interface AuditLog {
  id?: string;
  userId: string;
  userName: string;
  action: string;
  module: string;
  targetId?: string;
  targetName?: string;
  detail?: string;
  ip?: string;
  createdAt?: string;
}

export const auditActions = {
  SETTLEMENT_APPROVE: 'settlement_approve',
  SETTLEMENT_REJECT: 'settlement_reject',
  SETTLEMENT_BATCH: 'settlement_batch',
  REFUND_APPROVE: 'refund_approve',
  REFUND_REJECT: 'refund_reject',
  REPORT_GENERATE: 'report_generate',
  REPORT_EXPORT: 'report_export',
  FEE_CONFIG_CREATE: 'fee_config_create',
  FEE_CONFIG_UPDATE: 'fee_config_update',
  FEE_CONFIG_DELETE: 'fee_config_delete',
  TRANSACTION_EXPORT: 'transaction_export',
};

export async function recordAuditLog(log: AuditLog): Promise<void> {
  try {
    await request({
      url: '/admin/audit-logs',
      method: 'post',
      data: {
        ...log,
        ip: await getClientIp(),
        createdAt: new Date().toISOString(),
      },
      showError: false,
    });
  } catch {
    console.warn('Failed to record audit log:', log);
  }
}

async function getClientIp(): Promise<string> {
  try {
    const response = await fetch('https://api.ipify.org?format=json');
    const data = await response.json();
    return data.ip || 'unknown';
  } catch {
    return 'unknown';
  }
}

export function getCurrentUserInfo(): { userId: string; userName: string } {
  const userInfo = localStorage.getItem('userInfo');
  if (userInfo) {
    const parsed = JSON.parse(userInfo);
    return {
      userId: parsed.id || 'system',
      userName: parsed.name || 'System',
    };
  }
  return { userId: 'system', userName: 'System' };
}

export function createFinanceAuditLog(
  action: string,
  module: string,
  targetId?: string,
  targetName?: string,
  detail?: string
): AuditLog {
  const user = getCurrentUserInfo();
  return {
    userId: user.userId,
    userName: user.userName,
    action,
    module,
    targetId,
    targetName,
    detail,
  };
}
