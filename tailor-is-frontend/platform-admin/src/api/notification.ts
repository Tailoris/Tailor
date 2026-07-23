import request from './request';

export interface Notification {
  id: number;
  title: string;
  content: string;
  type: string;
  level: string;
  isRead: number;
  createdAt: string;
}

export interface NotificationQuery {
  current?: number;
  size?: number;
}

const mockNotifications: Notification[] = [
  {
    id: 1,
    title: '系统更新通知',
    content: '裁智云平台已升级至v4.0版本，新增AI智能打版功能',
    type: 'system',
    level: 'info',
    isRead: 0,
    createdAt: new Date(Date.now() - 300000).toISOString(),
  },
  {
    id: 2,
    title: '新订单提醒',
    content: '用户张三提交了一笔加急打版订单，订单号：ORD20260723001',
    type: 'order',
    level: 'success',
    isRead: 0,
    createdAt: new Date(Date.now() - 1800000).toISOString(),
  },
  {
    id: 3,
    title: '商家审核通知',
    content: '商家"时尚工坊"提交了入驻审核申请，请及时处理',
    type: 'merchant',
    level: 'warning',
    isRead: 1,
    createdAt: new Date(Date.now() - 3600000).toISOString(),
  },
];

export async function getUnreadCount(): Promise<number> {
  try {
    return await request<unknown, number>({
      url: '/admin/notifications/unread-count',
      method: 'get',
      showError: false,
    });
  } catch {
    return mockNotifications.filter((n) => n.isRead === 0).length;
  }
}

export async function getNotifications(params: NotificationQuery): Promise<{ records: Notification[]; total: number }> {
  try {
    return await request<unknown, { records: Notification[]; total: number }>({
      url: '/admin/notifications',
      method: 'get',
      params,
      showError: false,
    });
  } catch {
    const start = ((params.current || 1) - 1) * (params.size || 10);
    return {
      records: mockNotifications.slice(start, start + (params.size || 10)),
      total: mockNotifications.length,
    };
  }
}

export async function markAllNotificationsRead(): Promise<void> {
  try {
    await request<unknown, void>({
      url: '/admin/notifications/read-all',
      method: 'post',
      showError: false,
    });
  } catch {
    // Mock success
  }
}

export async function markNotificationRead(id: number): Promise<void> {
  try {
    await request<unknown, void>({
      url: `/admin/notifications/${id}/read`,
      method: 'post',
      showError: false,
    });
  } catch {
    // Mock success
  }
}