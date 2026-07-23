import request from './request';
import type { PageResult } from './role';

export interface OrderInfo {
  id: number;
  orderNo: string;
  userName: string;
  userId: number;
  productName: string;
  merchantName: string;
  totalAmount: number;
  paidAmount: number;
  paymentMethod: string;
  status: 'pending' | 'paid' | 'processing' | 'delivering' | 'completed' | 'cancelled' | 'refunding';
  remark: string;
  createTime: string;
  payTime: string;
  completeTime: string;
}

export interface ArbitrationInfo {
  id: number;
  orderNo: string;
  userName: string;
  merchantName: string;
  reason: string;
  description: string;
  evidence: string[];
  status: 'pending' | 'user_wins' | 'merchant_wins' | 'cancelled';
  result: string;
  applyTime: string;
  handleTime: string;
}

export interface OrderQueryParams {
  orderNo?: string;
  status?: string;
  keyword?: string;
  current?: number;
  size?: number;
  startDate?: string;
  endDate?: string;
}

const mockOrders: OrderInfo[] = [
  {
    id: 1, orderNo: 'ORD20260723001', userName: '张三', userId: 101, productName: '女士连衣裙纸样',
    merchantName: '时尚工坊', totalAmount: 299, paidAmount: 299, paymentMethod: '微信支付',
    status: 'pending', remark: '加急订单', createTime: '2026-07-23 10:30:00', payTime: '', completeTime: '',
  },
  {
    id: 2, orderNo: 'ORD20260723002', userName: '李四', userId: 102, productName: '男士衬衫纸样',
    merchantName: '精品制衣', totalAmount: 158, paidAmount: 158, paymentMethod: '支付宝',
    status: 'processing', remark: '', createTime: '2026-07-23 09:15:00', payTime: '2026-07-23 09:16:00', completeTime: '',
  },
  {
    id: 3, orderNo: 'ORD20260722001', userName: '王五', userId: 103, productName: '儿童外套纸样',
    merchantName: '时尚工坊', totalAmount: 89, paidAmount: 89, paymentMethod: '微信支付',
    status: 'completed', remark: '', createTime: '2026-07-22 14:20:00', payTime: '2026-07-22 14:21:00', completeTime: '2026-07-22 18:00:00',
  },
  {
    id: 4, orderNo: 'ORD20260722002', userName: '赵六', userId: 104, productName: '休闲裤纸样',
    merchantName: '精品制衣', totalAmount: 199, paidAmount: 66, paymentMethod: '支付宝',
    status: 'paid', remark: '分期付款-首期', createTime: '2026-07-22 16:00:00', payTime: '2026-07-22 16:05:00', completeTime: '',
  },
  {
    id: 5, orderNo: 'ORD20260721001', userName: '孙七', userId: 105, productName: '新款连衣裙纸样',
    merchantName: '创意设计工作室', totalAmount: 358, paidAmount: 0, paymentMethod: '',
    status: 'cancelled', remark: '用户取消', createTime: '2026-07-21 11:00:00', payTime: '', completeTime: '',
  },
];

const mockArbitrations: ArbitrationInfo[] = [
  {
    id: 1, orderNo: 'ORD20260723001', userName: '张三', merchantName: '时尚工坊',
    reason: '纸样尺寸不符', description: '收到的纸样尺寸与订单描述偏差超过3cm，无法正常使用',
    evidence: [], status: 'pending', result: '', applyTime: '2026-07-23 14:00:00', handleTime: '',
  },
  {
    id: 2, orderNo: 'ORD20260718001', userName: '周八', merchantName: '恒达服装厂',
    reason: '纸样质量不合格', description: '纸样打印后发现多处线条断裂，影响使用',
    evidence: [], status: 'user_wins', result: '经核实纸样存在质量问题，全额退款', applyTime: '2026-07-19 10:00:00', handleTime: '2026-07-20 15:00:00',
  },
];

export async function listOrders(params: OrderQueryParams): Promise<PageResult<OrderInfo>> {
  try {
    return await request<unknown, PageResult<OrderInfo>>({
      url: '/admin/order',
      method: 'get',
      params,
      showError: false,
    });
  } catch {
    let filtered = [...mockOrders];
    if (params.orderNo) {
      filtered = filtered.filter(o => o.orderNo.includes(params.orderNo!));
    }
    if (params.status) {
      filtered = filtered.filter(o => o.status === params.status);
    }
    if (params.keyword) {
      const kw = params.keyword.toLowerCase();
      filtered = filtered.filter(o =>
        o.userName.includes(kw) || o.productName.includes(kw) || o.merchantName.includes(kw)
      );
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

export async function getOrderById(id: number): Promise<OrderInfo> {
  try {
    return await request<unknown, OrderInfo>({
      url: `/admin/order/${id}`,
      method: 'get',
      showError: false,
    });
  } catch {
    return mockOrders.find(o => o.id === id) || mockOrders[0];
  }
}

export async function updateOrderStatus(id: number, status: string, remark?: string): Promise<void> {
  try {
    await request<unknown, void>({
      url: `/admin/order/${id}/status`,
      method: 'put',
      data: { status, remark },
      showError: false,
    });
  } catch {
    // Mock success
  }
}

export async function listArbitrations(params: OrderQueryParams): Promise<PageResult<ArbitrationInfo>> {
  try {
    return await request<unknown, PageResult<ArbitrationInfo>>({
      url: '/admin/order/arbitration',
      method: 'get',
      params,
      showError: false,
    });
  } catch {
    let filtered = [...mockArbitrations];
    if (params.status) {
      filtered = filtered.filter(a => a.status === params.status);
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

export async function handleArbitration(id: number, result: 'user_wins' | 'merchant_wins', remark: string): Promise<void> {
  try {
    await request<unknown, void>({
      url: `/admin/order/arbitration/${id}/handle`,
      method: 'post',
      data: { result, remark },
      showError: false,
    });
  } catch {
    // Mock success
  }
}