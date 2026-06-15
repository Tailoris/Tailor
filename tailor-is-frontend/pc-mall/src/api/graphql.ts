import { ref, computed } from 'vue'

// GraphQL endpoint
const GRAPHQL_ENDPOINT = import.meta.env.VITE_GRAPHQL_ENDPOINT || '/graphql'

// Generic GraphQL response types
interface GraphQLError {
  message: string
  path?: string[]
  extensions?: Record<string, unknown>
}

interface GraphQLResponse<T> {
  data?: T
  errors?: GraphQLError[]
}

/**
 * Generic GraphQL query executor
 */
async function graphqlQuery<T>(query: string, variables: Record<string, unknown> = {}): Promise<T | null> {
  try {
    const token = localStorage.getItem('token')
    const headers: Record<string, string> = {
      'Content-Type': 'application/json'
    }
    if (token) {
      headers.Authorization = `Bearer ${token}`
    }

    const response = await fetch(GRAPHQL_ENDPOINT, {
      method: 'POST',
      headers,
      body: JSON.stringify({ query, variables })
    })

    const result: GraphQLResponse<T> = await response.json()

    if (result.errors && result.errors.length > 0) {
      console.error('[GraphQL] Query errors:', result.errors)
      return null
    }

    return result.data || null
  } catch (error) {
    console.error('[GraphQL] Request failed:', error)
    return null
  }
}

// ========== GraphQL Queries ==========

/**
 * Product detail query - fetches product with SKUs in a single request
 */
export const PRODUCT_DETAIL_QUERY = /* GraphQL */ `
  query ProductDetail($id: ID!) {
    product(id: $id) {
      id
      name
      subTitle
      mainImage
      images
      description
      price
      saleCount
      viewCount
      productType
      status
      skus {
        id
        skuCode
        attributes {
          key
          value
        }
        price
        stock
        status
      }
      createdAt
    }
  }
`

/**
 * Product list query with pagination and filters
 */
export const PRODUCTS_QUERY = /* GraphQL */ `
  query Products(
    $categoryId: Int
    $keyword: String
    $sort: String
    $minPrice: Float
    $maxPrice: Float
    $current: Int
    $size: Int
  ) {
    products(
      categoryId: $categoryId
      keyword: $keyword
      sort: $sort
      minPrice: $minPrice
      maxPrice: $maxPrice
      current: $current
      size: $size
    ) {
      records {
        id
        name
        subTitle
        mainImage
        images
        price
        saleCount
        viewCount
        productType
      }
      total
      pages
      current
      size
    }
  }
`

/**
 * Order detail query with items
 */
export const ORDER_DETAIL_QUERY = /* GraphQL */ `
  query OrderDetail($id: ID!) {
    order(id: $id) {
      id
      orderNo
      status
      totalAmount
      discountAmount
      payAmount
      payType
      payStatus
      remark
      createdAt
      paidAt
      items {
        id
        productId
        productName
        productImage
        skuAttributes {
          key
          value
        }
        quantity
        price
        subtotal
      }
    }
  }
`

/**
 * Hot products query
 */
export const HOT_PRODUCTS_QUERY = /* GraphQL */ `
  query HotProducts($limit: Int) {
    hotProducts(limit: $limit) {
      id
      name
      mainImage
      price
      saleCount
    }
  }
`

// ========== Composable for Product Detail ==========

export interface ProductSkuAttribute {
  key: string
  value: string
}

export interface ProductSku {
  id: number
  skuCode: string
  attributes: ProductSkuAttribute[]
  price: number
  stock: number
  status: number
}

export interface GraphQLProduct {
  id: number
  name: string
  subTitle: string
  mainImage: string
  images: string[]
  description: string
  price: number
  saleCount: number
  viewCount: number
  productType: number
  status: number
  skus?: ProductSku[]
  createdAt: string
}

/**
 * Composable for fetching product detail via GraphQL
 */
export function useProductGraphQL(id: number) {
  const product = ref<GraphQLProduct | null>(null)
  const loading = ref(true)
  const error = ref<string | null>(null)

  async function fetch() {
    loading.value = true
    error.value = null

    try {
      const data = await graphqlQuery<{ product: GraphQLProduct }>(
        PRODUCT_DETAIL_QUERY,
        { id: String(id) }
      )

      if (data?.product) {
        product.value = data.product
      } else {
        error.value = 'Product not found'
      }
    } catch (e) {
      error.value = (e as Error).message
    } finally {
      loading.value = false
    }
  }

  return { product, loading, error, fetch }
}

/**
 * Composable for fetching order detail via GraphQL
 */
export interface OrderItem {
  id: number
  productId: number
  productName: string
  productImage: string
  skuAttributes: ProductSkuAttribute[]
  quantity: number
  price: number
  subtotal: number
}

export interface GraphQLOrder {
  id: number
  orderNo: string
  status: number
  totalAmount: number
  discountAmount: number
  payAmount: number
  payType: number
  payStatus: number
  remark: string
  createdAt: string
  paidAt: string
  items: OrderItem[]
}

export function useOrderGraphQL(id: number) {
  const order = ref<GraphQLOrder | null>(null)
  const loading = ref(true)
  const error = ref<string | null>(null)

  async function fetch() {
    loading.value = true
    error.value = null

    try {
      const data = await graphqlQuery<{ order: GraphQLOrder }>(
        ORDER_DETAIL_QUERY,
        { id: String(id) }
      )

      if (data?.order) {
        order.value = data.order
      } else {
        error.value = 'Order not found'
      }
    } catch (e) {
      error.value = (e as Error).message
    } finally {
      loading.value = false
    }
  }

  return { order, loading, error, fetch }
}

export default graphqlQuery
