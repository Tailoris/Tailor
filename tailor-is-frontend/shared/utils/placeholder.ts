/**
 * 占位图工具 - 修复 L-10
 *
 * 不再使用 via.placeholder.com 等外部占位图服务
 * 改用本地占位图（按需生成 SVG / Data URI）
 */

/**
 * 生成 SVG Data URI 占位图
 * @param width 宽度
 * @param height 高度
 * @param text 显示文字（可选）
 * @returns SVG data URI
 */
export function placeholderImage(
  width = 100,
  height = 100,
  text?: string
): string {
  const displayText =
    text ||
    `${width}×${height}`

  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}">
    <rect width="100%" height="100%" fill="#f0f0f0"/>
    <text x="50%" y="50%" font-family="sans-serif" font-size="14" fill="#999" text-anchor="middle" dominant-baseline="middle">${displayText}</text>
  </svg>`

  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`
}

/**
 * 通用商品占位图
 */
export const PRODUCT_PLACEHOLDER = placeholderImage(150, 150, '商品')

/**
 * 通用头像占位图
 */
export const AVATAR_PLACEHOLDER = placeholderImage(80, 80, '头像')

/**
 * 通用图片占位图
 */
export const IMAGE_PLACEHOLDER = placeholderImage(100, 100)

/**
 * 通用默认占位图
 */
export const DEFAULT_PLACEHOLDER = placeholderImage(60, 60)

/**
 * 各种尺寸的占位图常量（用于替换 via.placeholder.com）
 */
export const PLACEHOLDER_32x32 = placeholderImage(32, 32)
export const PLACEHOLDER_40x40 = placeholderImage(40, 40)
export const PLACEHOLDER_50x50 = placeholderImage(50, 50)
export const PLACEHOLDER_60x60 = placeholderImage(60, 60)
export const PLACEHOLDER_80x80 = placeholderImage(80, 80)
export const PLACEHOLDER_100x100 = placeholderImage(100, 100)
export const PLACEHOLDER_150x150 = placeholderImage(150, 150)
export const PLACEHOLDER_200x200 = placeholderImage(200, 200)
export const PLACEHOLDER_300x300 = placeholderImage(300, 300)
export const PLACEHOLDER_480x480 = placeholderImage(480, 480)
export const PLACEHOLDER_750x300 = placeholderImage(750, 300)
export const PLACEHOLDER_750x750 = placeholderImage(750, 750)
