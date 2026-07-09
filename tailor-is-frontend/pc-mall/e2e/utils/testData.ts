export const testAddress = {
  name: `TestUser_${Date.now()}`,
  phone: '13800138000',
  province: '广东省',
  city: '广州市',
  district: '天河区',
  detail: `TestAddress_${Date.now()}`,
}

export const testCoupon = {
  discount: 10,
  minAmount: 100,
  title: '测试优惠券',
}

export const testProduct = {
  name: '测试商品',
  price: 199,
}

export function generateUniqueId(): string {
  return `test_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
}

export function getTestUser(): { username: string; password: string } {
  return {
    username: 'testuser',
    password: 'test123',
  }
}
