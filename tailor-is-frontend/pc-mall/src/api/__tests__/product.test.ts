import { vi, describe, it, expect, beforeEach } from 'vitest';

vi.mock('../request', () => ({
  default: vi.fn().mockResolvedValue({}),
}));

import request from '../request';
import {
  getProducts,
  getProductDetail,
  getCategories,
  searchProducts,
  getHotProducts,
  getNewProducts,
  getSeckillProducts,
} from '../product';

describe('product API', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should get product list successfully', async () => {
    const mockProducts = {
      records: [
        { id: 1, name: 'Product 1', price: 99.9 },
        { id: 2, name: 'Product 2', price: 199.9 },
      ],
      current: 1,
      size: 10,
      total: 100,
    };
    (request as vi.Mock).mockResolvedValue(mockProducts);

    const result = await getProducts({ current: 1, size: 10 });
    expect(result).toEqual(mockProducts);
    expect(result.records.length).toBe(2);
    expect(result.total).toBe(100);
  });

  it('should get product detail successfully', async () => {
    const mockProduct = {
      id: 1,
      name: 'Test Product',
      price: 99.9,
      description: 'Test description',
      images: ['image1.jpg', 'image2.jpg'],
      stock: 100,
    };
    (request as vi.Mock).mockResolvedValue(mockProduct);

    const result = await getProductDetail(1);
    expect(result).toEqual(mockProduct);
    expect(result.id).toBe(1);
    expect(result.name).toBe('Test Product');
  });

  it('should get categories successfully', async () => {
    const mockCategories = [
      { id: 1, name: 'Category 1', parentId: 0 },
      { id: 2, name: 'Category 2', parentId: 0 },
      { id: 3, name: 'Sub Category', parentId: 1 },
    ];
    (request as vi.Mock).mockResolvedValue(mockCategories);

    const result = await getCategories();
    expect(result).toEqual(mockCategories);
    expect(result.length).toBe(3);
  });

  it('should search products successfully', async () => {
    const mockProducts = {
      records: [
        { id: 1, name: 'Search Result 1', price: 59.9 },
        { id: 2, name: 'Search Result 2', price: 79.9 },
      ],
      current: 1,
      size: 10,
      total: 20,
    };
    (request as vi.Mock).mockResolvedValue(mockProducts);

    const result = await searchProducts('keyword', { current: 1, size: 10 });
    expect(result).toEqual(mockProducts);
    expect(result.records.length).toBe(2);
  });

  it('should get hot products successfully', async () => {
    const mockProducts = [
      { id: 1, name: 'Hot Product 1', price: 99.9, sales: 1000 },
      { id: 2, name: 'Hot Product 2', price: 149.9, sales: 800 },
    ];
    (request as vi.Mock).mockResolvedValue(mockProducts);

    const result = await getHotProducts();
    expect(result).toEqual(mockProducts);
    expect(result.length).toBe(2);
  });

  it('should get new products successfully', async () => {
    const mockProducts = [
      { id: 1, name: 'New Product 1', price: 89.9, createTime: '2024-01-15' },
      { id: 2, name: 'New Product 2', price: 129.9, createTime: '2024-01-14' },
    ];
    (request as vi.Mock).mockResolvedValue(mockProducts);

    const result = await getNewProducts();
    expect(result).toEqual(mockProducts);
    expect(result.length).toBe(2);
  });

  it('should get seckill products successfully', async () => {
    const mockProducts = [
      { id: 1, name: 'Seckill Product 1', price: 99.9, seckillPrice: 49.9, stock: 10 },
      { id: 2, name: 'Seckill Product 2', price: 199.9, seckillPrice: 99.9, stock: 5 },
    ];
    (request as vi.Mock).mockResolvedValue(mockProducts);

    const result = await getSeckillProducts();
    expect(result).toEqual(mockProducts);
    expect(result.length).toBe(2);
  });

  it('should handle empty product list', async () => {
    const mockProducts = {
      records: [],
      current: 1,
      size: 10,
      total: 0,
    };
    (request as vi.Mock).mockResolvedValue(mockProducts);

    const result = await getProducts({ keyword: 'nonexistent' });
    expect(result.records).toEqual([]);
    expect(result.total).toBe(0);
  });

  it('should handle empty categories', async () => {
    (request as vi.Mock).mockResolvedValue([]);

    const result = await getCategories();
    expect(result).toEqual([]);
  });
});
