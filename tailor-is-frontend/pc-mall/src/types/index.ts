export interface User {
  id: number
  username: string
  phone: string
  nickname?: string
  nickName?: string
  email?: string
  avatar?: string
  realName?: string
  gender?: number
  birthday?: string
  status?: number
  createdAt?: string
}

export interface Product {
  id: number
  merchantId: number
  shopId: number
  categoryId: number
  productType: number
  name: string
  subTitle: string
  mainImage: string
  images: string[]
  description: string
  price: number
  status: number
  auditStatus: number
  copyrightFlag: number
  saleCount: number
  viewCount: number
  skus?: ProductSku[]
  createdAt: string
}

export interface ProductSku {
  id: number
  productId: number
  skuCode: string
  attributes: Record<string, string>
  price: number
  stock: number
  status: number
}

export interface ProductCategory {
  id: number
  name: string
  parentId: number
  level: number
  sort: number
  icon: string
  status: number
  children?: ProductCategory[]
}

export interface CartItem {
  id: number
  userId: number
  productId: number
  productName: string
  productImage: string
  skuId: number
  skuAttributes: Record<string, string>
  quantity: number
  price: number
  checked: boolean
}

export interface Order {
  id: number
  orderNo: string
  userId: number
  shopId: number
  merchantId: number
  productType: number
  status: number
  totalAmount: number
  discountAmount: number
  payAmount: number
  payType: number
  payStatus: number
  addressSnapshot: string
  remark: string
  createdAt: string
  paidAt: string
  items?: OrderItem[]
}

export interface OrderItem {
  id: number
  orderId: number
  productId: number
  skuId: number
  productName: string
  productImage: string
  skuAttributes: Record<string, string>
  quantity: number
  price: number
  subtotal: number
}

export interface Address {
  id: number
  userId: number
  name: string
  phone: string
  province: string
  city: string
  district: string
  detail: string
  isDefault: number
}

export interface Coupon {
  id: number
  name: string
  type: number
  discountType: number
  discountValue: number
  minAmount: number
  startTime: string
  endTime: string
  status: number
}

export interface Post {
  id: number
  userId: number
  userName: string
  userAvatar: string
  title: string
  content: string
  images: string[]
  videoUrl: string
  type: number
  viewCount: number
  likeCount: number
  commentCount: number
  status: number
  createdAt: string
}

export interface Comment {
  id: number
  postId: number
  userId: number
  userName: string
  userAvatar: string
  parentId: number
  content: string
  likeCount: number
  status: number
  createdAt: string
}

export interface Merchant {
  id: number
  userId: number
  merchantType: number
  companyName: string
  licenseNo: string
  contactName: string
  contactPhone: string
  status: number
  auditStatus: number
  depositAmount: number
  joinTime: string
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

export interface ProductReview {
  id: number
  productId: number
  skuId?: number
  userId: number
  orderId?: number
  orderItemId?: number
  rating: number
  content: string
  images?: string[]
  videoUrl?: string
  status: number
  isAnonymous: number
  isAdditional: number
  parentId?: number
  merchantReply?: string
  merchantReplyTime?: string
  likeCount: number
  imageUrls?: string[]
  tags?: string[]
  skuSpec?: string
  helpfulCount: number
  reportCount: number
  isFeatured: number
  createdAt: string
}
