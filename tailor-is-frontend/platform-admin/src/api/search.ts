import request from './request';

export interface SearchResult {
  users: Array<{ id: number; username: string; realName: string; nickname?: string; phone?: string; email?: string; avatar?: string }>;
  products: Array<{ id: number; name: string; code: string; mainImage?: string; minPrice?: number; saleCount?: number }>;
  orders: Array<{ id: number; orderNo: string; status: string; totalAmount?: number }>;
  total: number;
}

const mockUsers = [
  { id: 1, username: 'admin', realName: '系统管理员', nickname: '管理员', phone: '13800138000', avatar: '' },
  { id: 2, username: 'zhangwei', realName: '张伟', nickname: '版师小张', phone: '13900139000', avatar: '' },
];

const mockProducts = [
  { id: 1, name: '女士连衣裙纸样', code: 'PAT001', mainImage: '', minPrice: 99, saleCount: 128 },
  { id: 2, name: '男士衬衫纸样', code: 'PAT002', mainImage: '', minPrice: 79, saleCount: 89 },
];

const mockOrders = [
  { id: 1, orderNo: 'ORD20260723001', status: 'pending', totalAmount: 299 },
  { id: 2, orderNo: 'ORD20260723002', status: 'processing', totalAmount: 158 },
];

export async function globalSearch(keyword: string): Promise<SearchResult> {
  try {
    return await request<unknown, SearchResult>({
      url: '/admin/search',
      method: 'get',
      params: { keyword },
      showError: false,
    });
  } catch {
    const lowerKeyword = keyword.toLowerCase();
    const users = mockUsers.filter(
      (u) =>
        u.username.toLowerCase().includes(lowerKeyword) ||
        u.realName.toLowerCase().includes(lowerKeyword) ||
        u.nickname?.toLowerCase().includes(lowerKeyword)
    );
    const products = mockProducts.filter(
      (p) => p.name.toLowerCase().includes(lowerKeyword) || p.code.toLowerCase().includes(lowerKeyword)
    );
    const orders = mockOrders.filter((o) => o.orderNo.toLowerCase().includes(lowerKeyword));
    return { users, products, orders, total: users.length + products.length + orders.length };
  }
}