import request from './request';
import type { PageResult } from './role';

export interface FabricInfo {
  id: number;
  name: string;
  code: string;
  category: string;
  composition: string;
  width: string;
  weight: string;
  price: number;
  stock: number;
  unit: string;
  supplier: string;
  supplierId: number;
  status: 'active' | 'inactive';
  description: string;
  createTime: string;
}

export interface SupplierInfo {
  id: number;
  name: string;
  contact: string;
  phone: string;
  email: string;
  address: string;
  rating: number;
  products: string[];
  status: 'active' | 'inactive';
  createTime: string;
}

export interface PurchaseOrderInfo {
  id: number;
  orderNo: string;
  supplier: string;
  supplierId: number;
  totalAmount: number;
  items: { name: string; quantity: number; price: number }[];
  status: 'pending' | 'confirmed' | 'shipped' | 'completed' | 'cancelled';
  remark: string;
  createTime: string;
  confirmTime: string;
}

export interface SupplyQueryParams {
  keyword?: string;
  status?: string;
  category?: string;
  current?: number;
  size?: number;
}

const mockFabrics: FabricInfo[] = [
  {
    id: 1, name: '纯棉平纹面料', code: 'FAB001', category: '棉', composition: '100%棉',
    width: '150cm', weight: '180g/m²', price: 25, stock: 5000, unit: '米',
    supplier: '华纺集团', supplierId: 301, status: 'active',
    description: '优质纯棉面料，手感柔软，透气性好', createTime: '2026-07-20 10:30:00',
  },
  {
    id: 2, name: '涤纶梭织面料', code: 'FAB002', category: '化纤', composition: '100%涤纶',
    width: '145cm', weight: '120g/m²', price: 15, stock: 8000, unit: '米',
    supplier: '恒力集团', supplierId: 302, status: 'active',
    description: '抗皱耐磨，适合制作外套和裤装', createTime: '2026-07-18 09:15:00',
  },
  {
    id: 3, name: '真丝缎面', code: 'FAB003', category: '丝绸', composition: '100%桑蚕丝',
    width: '114cm', weight: '60g/m²', price: 120, stock: 500, unit: '米',
    supplier: '华纺集团', supplierId: 301, status: 'active',
    description: '高档真丝面料，光泽亮丽，适合礼服制作', createTime: '2026-07-15 14:00:00',
  },
  {
    id: 4, name: '牛仔布', code: 'FAB004', category: '牛仔', composition: '98%棉+2%氨纶',
    width: '155cm', weight: '350g/m²', price: 45, stock: 3000, unit: '米',
    supplier: '恒力集团', supplierId: 302, status: 'inactive',
    description: '弹力牛仔面料，适合休闲裤装', createTime: '2026-07-12 08:00:00',
  },
];

const mockSuppliers: SupplierInfo[] = [
  {
    id: 1, name: '华纺集团', contact: '王经理', phone: '13800138001',
    email: 'wang@huafang.com', address: '上海市浦东新区张江高科技园区',
    rating: 4.8, products: ['棉', '丝绸', '麻'], status: 'active',
    createTime: '2026-07-01 10:00:00',
  },
  {
    id: 2, name: '恒力集团', contact: '李经理', phone: '13800138002',
    email: 'li@hengli.com', address: '江苏省苏州市吴江区盛泽镇',
    rating: 4.5, products: ['化纤', '牛仔', '混纺'], status: 'active',
    createTime: '2026-07-05 09:00:00',
  },
  {
    id: 3, name: '新丝路纺织', contact: '张经理', phone: '13800138003',
    email: 'zhang@newsilk.com', address: '浙江省绍兴市柯桥区',
    rating: 4.2, products: ['丝绸', '棉', '麻'], status: 'inactive',
    createTime: '2026-07-10 14:00:00',
  },
];

const mockPurchaseOrders: PurchaseOrderInfo[] = [
  {
    id: 1, orderNo: 'PO20260723001', supplier: '华纺集团', supplierId: 301,
    totalAmount: 50000, items: [{ name: '纯棉平纹面料', quantity: 2000, price: 25 }],
    status: 'pending', remark: '急单，请尽快处理', createTime: '2026-07-23 10:30:00', confirmTime: '',
  },
  {
    id: 2, orderNo: 'PO20260722001', supplier: '恒力集团', supplierId: 302,
    totalAmount: 30000, items: [{ name: '涤纶梭织面料', quantity: 2000, price: 15 }],
    status: 'confirmed', remark: '', createTime: '2026-07-22 14:20:00', confirmTime: '2026-07-22 16:00:00',
  },
];

export async function listFabrics(params: SupplyQueryParams): Promise<PageResult<FabricInfo>> {
  try {
    return await request<unknown, PageResult<FabricInfo>>({
      url: '/admin/supply/fabric',
      method: 'get',
      params,
      showError: false,
    });
  } catch {
    let filtered = [...mockFabrics];
    if (params.keyword) {
      const kw = params.keyword.toLowerCase();
      filtered = filtered.filter(f =>
        f.name.toLowerCase().includes(kw) ||
        f.code.toLowerCase().includes(kw) ||
        f.supplier.toLowerCase().includes(kw)
      );
    }
    if (params.status) {
      filtered = filtered.filter(f => f.status === params.status);
    }
    if (params.category) {
      filtered = filtered.filter(f => f.category === params.category);
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

export async function createFabric(data: Partial<FabricInfo>): Promise<FabricInfo> {
  try {
    return await request<unknown, FabricInfo>({
      url: '/admin/supply/fabric',
      method: 'post',
      data,
      showError: false,
    });
  } catch {
    return {
      id: Date.now(),
      ...data,
      status: 'active',
      createTime: new Date().toISOString().replace('T', ' ').slice(0, 19),
    } as FabricInfo;
  }
}

export async function updateFabric(id: number, data: Partial<FabricInfo>): Promise<FabricInfo> {
  try {
    return await request<unknown, FabricInfo>({
      url: `/admin/supply/fabric/${id}`,
      method: 'put',
      data,
      showError: false,
    });
  } catch {
    const existing = mockFabrics.find(f => f.id === id);
    return { ...existing!, ...data };
  }
}

export async function deleteFabric(id: number): Promise<void> {
  try {
    await request<unknown, void>({
      url: `/admin/supply/fabric/${id}`,
      method: 'delete',
      showError: false,
    });
  } catch {
    // Mock success
  }
}

export async function listSuppliers(params: SupplyQueryParams): Promise<PageResult<SupplierInfo>> {
  try {
    return await request<unknown, PageResult<SupplierInfo>>({
      url: '/admin/supply/supplier',
      method: 'get',
      params,
      showError: false,
    });
  } catch {
    let filtered = [...mockSuppliers];
    if (params.keyword) {
      const kw = params.keyword.toLowerCase();
      filtered = filtered.filter(s =>
        s.name.toLowerCase().includes(kw) ||
        s.contact.includes(kw) ||
        s.phone.includes(kw)
      );
    }
    if (params.status) {
      filtered = filtered.filter(s => s.status === params.status);
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

export async function createSupplier(data: Partial<SupplierInfo>): Promise<SupplierInfo> {
  try {
    return await request<unknown, SupplierInfo>({
      url: '/admin/supply/supplier',
      method: 'post',
      data,
      showError: false,
    });
  } catch {
    return {
      id: Date.now(),
      ...data,
      status: 'active',
      createTime: new Date().toISOString().replace('T', ' ').slice(0, 19),
    } as SupplierInfo;
  }
}

export async function updateSupplier(id: number, data: Partial<SupplierInfo>): Promise<SupplierInfo> {
  try {
    return await request<unknown, SupplierInfo>({
      url: `/admin/supply/supplier/${id}`,
      method: 'put',
      data,
      showError: false,
    });
  } catch {
    const existing = mockSuppliers.find(s => s.id === id);
    return { ...existing!, ...data };
  }
}

export async function deleteSupplier(id: number): Promise<void> {
  try {
    await request<unknown, void>({
      url: `/admin/supply/supplier/${id}`,
      method: 'delete',
      showError: false,
    });
  } catch {
    // Mock success
  }
}

export async function listPurchaseOrders(params: SupplyQueryParams): Promise<PageResult<PurchaseOrderInfo>> {
  try {
    return await request<unknown, PageResult<PurchaseOrderInfo>>({
      url: '/admin/supply/purchase-order',
      method: 'get',
      params,
      showError: false,
    });
  } catch {
    let filtered = [...mockPurchaseOrders];
    if (params.keyword) {
      const kw = params.keyword.toLowerCase();
      filtered = filtered.filter(o =>
        o.orderNo.includes(kw) || o.supplier.includes(kw)
      );
    }
    if (params.status) {
      filtered = filtered.filter(o => o.status === params.status);
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

export async function createPurchaseOrder(data: Partial<PurchaseOrderInfo>): Promise<PurchaseOrderInfo> {
  try {
    return await request<unknown, PurchaseOrderInfo>({
      url: '/admin/supply/purchase-order',
      method: 'post',
      data,
      showError: false,
    });
  } catch {
    return {
      id: Date.now(),
      ...data,
      status: 'pending',
      createTime: new Date().toISOString().replace('T', ' ').slice(0, 19),
      confirmTime: '',
    } as PurchaseOrderInfo;
  }
}

export async function confirmPurchaseOrder(id: number): Promise<void> {
  try {
    await request<unknown, void>({
      url: `/admin/supply/purchase-order/${id}/confirm`,
      method: 'post',
      showError: false,
    });
  } catch {
    // Mock success
  }
}

export async function cancelPurchaseOrder(id: number): Promise<void> {
  try {
    await request<unknown, void>({
      url: `/admin/supply/purchase-order/${id}/cancel`,
      method: 'post',
      showError: false,
    });
  } catch {
    // Mock success
  }
}