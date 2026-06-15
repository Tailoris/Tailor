export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

export interface RequestConfig {
  noAuth?: boolean
  showLoading?: boolean
  hideError?: boolean
  /** 离线模式下是否加入请求队列（默认 true） */
  offlineQueue?: boolean
  /** 自定义重试次数（默认 3） */
  retryCount?: number
  /** 自定义超时时间（毫秒） */
  timeout?: number
  /** 是否忽略离线状态强制请求 */
  ignoreOffline?: boolean
}

export interface PaginationParams {
  pageNum?: number
  pageSize?: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar: string
  phone: string
  email: string
}

export interface AddressInfo {
  id: number
  name: string
  phone: string
  province: string
  city: string
  district: string
  detail: string
  isDefault: number
}

export interface ProductInfo {
  id: number
  name: string
  price: number
  originalPrice: number
  image: string
  images: string[]
  description: string
  categoryId: number
  stock: number
  sales: number
  status: number
}

export interface CartItem {
  id: number
  productId: number
  productName: string
  productImage: string
  price: number
  quantity: number
  skuId?: number
}

export interface OrderInfo {
  id: number
  orderNo: string
  status: number
  totalPrice: number
  payPrice: number
  payType?: number
  payTime?: string
  createTime: string
  items?: OrderItem[]
  address?: AddressInfo
}

export interface OrderItem {
  id: number
  orderId: number
  productId: number
  productName: string
  productImage: string
  price: number
  quantity: number
}

export interface PostInfo {
  id: number
  title: string
  content: string
  images: string[]
  authorId: number
  authorName: string
  authorAvatar: string
  likeCount: number
  commentCount: number
  createTime: string
  liked: boolean
}

export interface CommentInfo {
  id: number
  postId: number
  userId: number
  userName: string
  userAvatar: string
  content: string
  createTime: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginByCodeRequest {
  target: string
  code: string
  type: 'phone' | 'email'
}

export interface RegisterRequest {
  username: string
  password: string
  phone: string
  smsCode: string
}

export interface RegisterByEmailRequest {
  email: string
  code: string
  password: string
}

export interface SmsCodeRequest {
  phone: string
  type: number
}

export interface EmailCodeRequest {
  email: string
}

export interface SendCodeRequest {
  target: string
  type: 'phone' | 'email'
}

export interface MerchantApplyRequest {
  merchantName: string
  merchantType: number
  contactName: string
  contactPhone: string
  idCardFront: string
  idCardBack: string
  businessLicense: string
}

export interface AddressRequest {
  name: string
  phone: string
  province: string
  city: string
  district: string
  detail: string
  isDefault?: number
}
