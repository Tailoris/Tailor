export interface Merchant {
  id: number
  name: string
  contactName: string
  contactPhone: string
  email: string
  status: 'active' | 'inactive' | 'pending'
  createdAt: string
  updatedAt: string
}

export interface Shop {
  id: number
  name: string
  logo: string
  description: string
  businessHours: string
  announcement: string
  status: 'active' | 'inactive'
  merchantId: number
  createdAt: string
}

export interface Product {
  id: number
  name: string
  category: string
  type: 'physical' | 'virtual'
  description: string
  images: string[]
  status: 'draft' | 'pending' | 'on_sale' | 'off_sale'
  shopId: number
  createdAt: string
  updatedAt: string
}

export interface ProductSku {
  id: number
  productId: number
  skuCode: string
  name: string
  price: number
  originalPrice: number
  stock: number
  image: string
  status: 'active' | 'inactive'
}

export interface Order {
  id: number
  orderNo: string
  shopId: number
  shopName: string
  buyerName: string
  buyerPhone: string
  status: 'pending' | 'paid' | 'shipped' | 'completed' | 'cancelled'
  totalAmount: number
  discountAmount: number
  payAmount: number
  items: OrderItem[]
  logisticsCompany: string
  trackingNumber: string
  remark: string
  paidAt: string
  shippedAt: string
  completedAt: string
  createdAt: string
}

export interface OrderItem {
  id: number
  orderId: number
  productId: number
  productName: string
  productImage: string
  skuId: number
  skuName: string
  quantity: number
  price: number
  subtotal: number
}

export interface AfterSaleTicket {
  id: number
  ticketNo: string
  orderNo: string
  buyerName: string
  type: 'refund_only' | 'return_and_refund' | 'exchange'
  reason: string
  description: string
  evidenceImages: string[]
  refundAmount: number
  status: 'pending' | 'processing' | 'approved' | 'rejected' | 'completed'
  handlerRemark: string
  createdAt: string
  updatedAt: string
}

export interface Employee {
  id: number
  userId: number
  name: string
  phone: string
  role: 'admin' | 'operator' | 'viewer'
  shopId: number
  shopName: string
  status: 'active' | 'inactive'
  createdAt: string
}

export interface SettlementRecord {
  id: number
  orderNo: string
  orderAmount: number
  commissionRate: number
  commissionAmount: number
  netAmount: number
  status: 'pending' | 'settled' | 'withdrawn'
  settledAt: string
  createdAt: string
}

export interface Coupon {
  id: number
  name: string
  type: 'discount' | 'fixed' | 'percentage'
  discountValue: number
  minAmount: number
  validFrom: string
  validTo: string
  totalQuantity: number
  usedQuantity: number
  status: 'active' | 'inactive' | 'expired'
  shopId: number
  createdAt: string
}

export interface DashboardStats {
  todayOrders: number
  todayRevenue: number
  pendingOrders: number
  pendingAftersale: number
  totalProducts: number
  totalCustomers: number
  revenueTrend: { date: string; amount: number }[]
  recentOrders: Order[]
  recentAftersales: AfterSaleTicket[]
}

export interface PageResponse<T> {
  records: T[]
  total: number
  pages: number
  current: number
  size: number
}

export interface ApiResponse<T> {
  code: number
  message: string
  data?: T | null
}
