import request from './request';
import type { PageResult } from './role';

export interface ProductInfo {
  id: number;
  name: string;
  code: string;
  category: string;
  merchantName: string;
  price: number;
  originalPrice: number;
  saleCount: number;
  stock: number;
  status: 'online' | 'offline' | 'pending' | 'rejected';
  description: string;
  mainImage: string;
  createTime: string;
  updateTime: string;
}

export interface ProductForm {
  id?: number;
  name: string;
  code: string;
  category: string;
  merchantId: number;
  price: number;
  originalPrice: number;
  stock: number;
  description: string;
  mainImage: string;
}

export interface ProductQueryParams {
  keyword?: string;
  status?: string;
  category?: string;
  current?: number;
  size?: number;
}

const mockProducts: ProductInfo[] = [
  {
    id: 1, name: '女士连衣裙纸样', code: 'PAT001', category: '女装', merchantName: '时尚工坊',
    price: 99, originalPrice: 199, saleCount: 128, stock: 999, status: 'online',
    description: '修身连衣裙纸样，含多种尺码规格', mainImage: '',
    createTime: '2026-07-20 10:30:00', updateTime: '2026-07-20 10:30:00',
  },
  {
    id: 2, name: '男士衬衫纸样', code: 'PAT002', category: '男装', merchantName: '精品制衣',
    price: 79, originalPrice: 159, saleCount: 89, stock: 500, status: 'online',
    description: '经典商务衬衫纸样，适合定制', mainImage: '',
    createTime: '2026-07-18 09:15:00', updateTime: '2026-07-18 09:15:00',
  },
  {
    id: 3, name: '儿童外套纸样', code: 'PAT003', category: '童装', merchantName: '时尚工坊',
    price: 69, originalPrice: 139, saleCount: 45, stock: 0, status: 'offline',
    description: '保暖儿童外套纸样，多年龄段可选', mainImage: '',
    createTime: '2026-07-15 14:00:00', updateTime: '2026-07-22 09:00:00',
  },
  {
    id: 4, name: '新款连衣裙纸样', code: 'PAT004', category: '女装', merchantName: '创意设计工作室',
    price: 129, originalPrice: 259, saleCount: 22, stock: 300, status: 'pending',
    description: '2026夏季新款连衣裙纸样', mainImage: '',
    createTime: '2026-07-23 10:30:00', updateTime: '2026-07-23 10:30:00',
  },
  {
    id: 5, name: '休闲裤纸样', code: 'PAT005', category: '裤装', merchantName: '精品制衣',
    price: 89, originalPrice: 179, saleCount: 56, stock: 200, status: 'pending',
    description: '宽松休闲裤纸样，舒适百搭', mainImage: '',
    createTime: '2026-07-23 09:15:00', updateTime: '2026-07-23 09:15:00',
  },
];

export async function listProducts(params: ProductQueryParams): Promise<PageResult<ProductInfo>> {
  try {
    return await request<unknown, PageResult<ProductInfo>>({
      url: '/admin/product',
      method: 'get',
      params,
      showError: false,
    });
  } catch {
    let filtered = [...mockProducts];
    if (params.keyword) {
      const kw = params.keyword.toLowerCase();
      filtered = filtered.filter(p =>
        p.name.toLowerCase().includes(kw) ||
        p.code.toLowerCase().includes(kw) ||
        p.merchantName.toLowerCase().includes(kw)
      );
    }
    if (params.status) {
      filtered = filtered.filter(p => p.status === params.status);
    }
    if (params.category) {
      filtered = filtered.filter(p => p.category === params.category);
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

export async function getProductById(id: number): Promise<ProductInfo> {
  try {
    return await request<unknown, ProductInfo>({
      url: `/admin/product/${id}`,
      method: 'get',
      showError: false,
    });
  } catch {
    return mockProducts.find(p => p.id === id) || mockProducts[0];
  }
}

export async function createProduct(data: ProductForm): Promise<ProductInfo> {
  try {
    return await request<unknown, ProductInfo>({
      url: '/admin/product',
      method: 'post',
      data,
      showError: false,
    });
  } catch {
    return {
      id: Date.now(),
      ...data,
      merchantName: '新商家',
      saleCount: 0,
      status: 'pending',
      createTime: new Date().toISOString().replace('T', ' ').slice(0, 19),
      updateTime: new Date().toISOString().replace('T', ' ').slice(0, 19),
    };
  }
}

export async function updateProduct(id: number, data: ProductForm): Promise<ProductInfo> {
  try {
    return await request<unknown, ProductInfo>({
      url: `/admin/product/${id}`,
      method: 'put',
      data,
      showError: false,
    });
  } catch {
    const existing = mockProducts.find(p => p.id === id);
    return { ...existing!, ...data, updateTime: new Date().toISOString().replace('T', ' ').slice(0, 19) };
  }
}

export async function approveProduct(id: number, approved: boolean, reason?: string): Promise<void> {
  try {
    await request<unknown, void>({
      url: `/admin/product/${id}/audit`,
      method: 'post',
      data: { approved, reason },
      showError: false,
    });
  } catch {
    // Mock success
  }
}

export async function toggleProductStatus(id: number, status: string): Promise<void> {
  try {
    await request<unknown, void>({
      url: `/admin/product/${id}/status`,
      method: 'put',
      data: { status },
      showError: false,
    });
  } catch {
    // Mock success
  }
}

export async function deleteProduct(id: number): Promise<void> {
  try {
    await request<unknown, void>({
      url: `/admin/product/${id}`,
      method: 'delete',
      showError: false,
    });
  } catch {
    // Mock success
  }
}