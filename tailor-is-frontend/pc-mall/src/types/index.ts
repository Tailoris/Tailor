// 公共类型统一从 shared/types 引入，消除多处重复定义
export type { ApiResponse, PageResponse, User } from '@shared/types';
import {
  StatusEnum,
  AuditStatusEnum,
  CopyrightFlagEnum,
} from '@shared/types';

export interface Product {
  id: number;
  merchantId: number;
  shopId: number;
  categoryId: number;
  productType: number;
  name: string;
  subTitle: string;
  mainImage: string;
  images: string[];
  description: string;
  price: number;
  /** 商品状态: 0=下架, 1=上架, 2=审核中, 3=已拒绝 */
  status: StatusEnum;
  /** 审核状态: 0=待审核, 1=审核通过, 2=审核拒绝 */
  auditStatus: AuditStatusEnum;
  /** 版权标记: 0=未检测, 1=已存证, 2=疑似侵权, 3=已确认侵权 */
  copyrightFlag: CopyrightFlagEnum;
  saleCount: number;
  viewCount: number;
  skus?: ProductSku[];
  createdAt: string;
}

export interface ProductSku {
  id: number;
  productId: number;
  skuCode: string;
  attributes: Record<string, string>;
  price: number;
  stock: number;
  /** SKU 状态: 0=禁用, 1=启用 */
  status: StatusEnum;
}

export interface ProductCategory {
  id: number;
  name: string;
  parentId: number;
  level: number;
  sort: number;
  icon: string;
  /** 分类状态: 0=禁用, 1=启用 */
  status: StatusEnum;
  children?: ProductCategory[];
}

export interface CartItem {
  id: number;
  userId: number;
  productId: number;
  productName: string;
  productImage: string;
  skuId: number;
  skuAttributes: Record<string, string>;
  quantity: number;
  price: number;
  checked: boolean;
}

export interface Order {
  id: number;
  orderNo: string;
  userId: number;
  shopId: number;
  merchantId: number;
  productType: number;
  /** 订单状态: 0=已取消, 1=待支付, 2=待发货, 3=已发货, 4=已完成 */
  status: StatusEnum;
  totalAmount: number;
  discountAmount: number;
  payAmount: number;
  payType: number;
  payStatus: number;
  addressSnapshot: string;
  remark: string;
  createdAt: string;
  paidAt: string;
  items?: OrderItem[];
}

export interface OrderItem {
  id: number;
  orderId: number;
  productId: number;
  skuId: number;
  productName: string;
  productImage: string;
  skuAttributes: Record<string, string>;
  quantity: number;
  price: number;
  subtotal: number;
}

export interface Address {
  id: number;
  userId: number;
  name: string;
  phone: string;
  province: string;
  city: string;
  district: string;
  detail: string;
  isDefault: number;
}

export interface Coupon {
  id: number;
  name: string;
  type: number;
  discountType: number;
  discountValue: number;
  minAmount: number;
  startTime: string;
  endTime: string;
  status: number;
}

export interface Post {
  id: number;
  userId: number;
  userName: string;
  userAvatar: string;
  title: string;
  content: string;
  images: string[];
  videoUrl: string;
  type: number;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  /** 帖子状态: 0=已删除, 1=正常, 2=审核中 */
  status: StatusEnum;
  createdAt: string;
}

export interface Comment {
  id: number;
  postId: number;
  userId: number;
  userName: string;
  userAvatar: string;
  parentId: number;
  content: string;
  likeCount: number;
  /** 评论状态: 0=已删除, 1=正常 */
  status: StatusEnum;
  createdAt: string;
}

export interface Merchant {
  id: number;
  userId: number;
  merchantType: number;
  companyName: string;
  licenseNo: string;
  contactName: string;
  contactPhone: string;
  /** 商家状态: 0=禁用, 1=正常 */
  status: StatusEnum;
  /** 审核状态: 0=待审核, 1=审核通过, 2=审核拒绝 */
  auditStatus: AuditStatusEnum;
  depositAmount: number;
  joinTime: string;
}

export interface ProductReview {
  id: number;
  productId: number;
  skuId?: number;
  userId: number;
  orderId?: number;
  orderItemId?: number;
  rating: number;
  content: string;
  images?: string[];
  videoUrl?: string;
  /** 评价状态: 0=已删除, 1=正常, 2=审核中 */
  status: StatusEnum;
  isAnonymous: number;
  isAdditional: number;
  parentId?: number;
  merchantReply?: string;
  merchantReplyTime?: string;
  likeCount: number;
  imageUrls?: string[];
  tags?: string[];
  skuSpec?: string;
  helpfulCount: number;
  reportCount: number;
  isFeatured: number;
  createdAt: string;
}
