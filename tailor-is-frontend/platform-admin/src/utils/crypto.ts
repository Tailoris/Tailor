/**
 * Token 加密/解密工具 - 使用 Web Crypto API AES-GCM
 *
 * 加密密钥从 localStorage 派生，每次登录时更新。
 * AES-GCM 提供认证加密，防止密文被篡改。
 */

const CRYPTO_KEY_STORAGE_KEY = '__crypto_key__'
const ALGORITHM = 'AES-GCM'
const KEY_LENGTH = 256
const IV_LENGTH = 12 // bytes for GCM

/**
 * 从存储中获取或生成加密密钥
 */
async function getCryptoKey(): Promise<CryptoKey> {
  let keyData = localStorage.getItem(CRYPTO_KEY_STORAGE_KEY)

  if (!keyData) {
    const key = await crypto.subtle.generateKey(
      { name: ALGORITHM, length: KEY_LENGTH },
      true,
      ['encrypt', 'decrypt']
    )
    const rawKey = await crypto.subtle.exportKey('raw', key)
    keyData = btoa(String.fromCharCode(...new Uint8Array(rawKey)))
    localStorage.setItem(CRYPTO_KEY_STORAGE_KEY, keyData)
  }

  const rawKey = Uint8Array.from(atob(keyData), c => c.charCodeAt(0))
  return crypto.subtle.importKey('raw', rawKey, ALGORITHM, true, ['encrypt', 'decrypt'])
}

/**
 * 生成随机 IV
 */
function generateIV(): Uint8Array {
  const iv = new Uint8Array(IV_LENGTH)
  crypto.getRandomValues(iv)
  return iv
}

/**
 * 加密字符串
 * @param plaintext - 明文
 * @returns base64 编码的密文 (IV + ciphertext)
 */
export async function encrypt(plaintext: string): Promise<string> {
  const key = await getCryptoKey()
  const iv = generateIV()
  const encoded = new TextEncoder().encode(plaintext)

  const ciphertext = await crypto.subtle.encrypt(
    { name: ALGORITHM, iv: iv as BufferSource },
    key,
    encoded
  )

  // 拼接 IV + ciphertext，然后 base64 编码
  const combined = new Uint8Array(iv.length + ciphertext.byteLength)
  combined.set(iv)
  combined.set(new Uint8Array(ciphertext), iv.length)
  return btoa(String.fromCharCode(...combined))
}

/**
 * 解密字符串
 * @param base64Ciphertext - base64 编码的密文
 * @returns 明文
 */
export async function decrypt(base64Ciphertext: string): Promise<string> {
  try {
    const key = await getCryptoKey()
    const combined = Uint8Array.from(atob(base64Ciphertext), c => c.charCodeAt(0))

    const iv = combined.slice(0, IV_LENGTH)
    const ciphertext = combined.slice(IV_LENGTH)

    const decrypted = await crypto.subtle.decrypt(
      { name: ALGORITHM, iv: iv as BufferSource },
      key,
      ciphertext
    )

    return new TextDecoder().decode(decrypted)
  } catch {
    // 解密失败返回空字符串
    return ''
  }
}

/**
 * 同步版本 - 使用 base64 编码（非加密）
 *
 * 浏览器环境无法实现同步强加密（Web Crypto API 仅有异步接口）。
 * 此函数仅做 base64 编码以避免明文直接暴露，并非加密。
 *
 * TODO: Token 安全应依赖服务端 httpOnly cookie，前端不应持久化存储 token。
 * 迁移至 httpOnly cookie 后可移除此函数。
 */
export function encryptSync(plaintext: string): string {
  try {
    return btoa(unescape(encodeURIComponent(plaintext)))
  } catch {
    return plaintext
  }
}

export function decryptSync(encoded: string): string {
  try {
    return decodeURIComponent(escape(atob(encoded)))
  } catch {
    return ''
  }
}
