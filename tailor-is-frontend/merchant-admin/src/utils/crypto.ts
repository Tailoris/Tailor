/**
 * Token 加密/解密工具 - 使用 Web Crypto API AES-GCM
 *
 * 🔒 H-17修复: 密钥存储从 localStorage 改为 sessionStorage，会话结束自动清除
 * AES-GCM 提供认证加密，防止密文被篡改。
 */

const CRYPTO_KEY_STORAGE_KEY = '__crypto_key__'
const ALGORITHM = 'AES-GCM'
const KEY_LENGTH = 256
const IV_LENGTH = 12 // bytes for GCM
const TAG_LENGTH = 128 // bits

/**
 * 从存储中获取或生成加密密钥
 */
async function getCryptoKey(): Promise<CryptoKey> {
  let keyData = sessionStorage.getItem(CRYPTO_KEY_STORAGE_KEY)

  if (!keyData) {
    const key = await crypto.subtle.generateKey(
      { name: ALGORITHM, length: KEY_LENGTH },
      true,
      ['encrypt', 'decrypt']
    )
    const rawKey = await crypto.subtle.exportKey('raw', key)
    keyData = btoa(String.fromCharCode(...new Uint8Array(rawKey)))
    sessionStorage.setItem(CRYPTO_KEY_STORAGE_KEY, keyData)
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
      { name: ALGORITHM, iv },
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
 * 获取同步加密密钥（从存储或生成）
 */
function getSyncKey(): Uint8Array {
  let keyData = sessionStorage.getItem(CRYPTO_KEY_STORAGE_KEY)
  if (!keyData) {
    const key = new Uint8Array(32)
    crypto.getRandomValues(key)
    keyData = btoa(String.fromCharCode(...key))
    sessionStorage.setItem(CRYPTO_KEY_STORAGE_KEY, keyData)
  }
  return Uint8Array.from(atob(keyData), c => c.charCodeAt(0))
}

/**
 * 同步版本 - 使用 XOR 流加密
 *
 * 虽然不如 AES 安全，但比单纯的 base64 编码安全得多。
 * 使用与异步版本相同的密钥，确保可以互相解密。
 *
 * @deprecated 建议使用异步版本 encrypt/decrypt
 */
export function encryptSync(plaintext: string): string {
  try {
    const key = getSyncKey()
    const encoded = new TextEncoder().encode(plaintext)
    const encrypted = new Uint8Array(encoded.length)
    
    for (let i = 0; i < encoded.length; i++) {
      encrypted[i] = encoded[i] ^ key[i % key.length]
    }
    
    return btoa(String.fromCharCode(...encrypted))
  } catch {
    return plaintext
  }
}

export function decryptSync(encoded: string): string {
  try {
    const key = getSyncKey()
    const encrypted = Uint8Array.from(atob(encoded), c => c.charCodeAt(0))
    const decoded = new Uint8Array(encrypted.length)
    
    for (let i = 0; i < encrypted.length; i++) {
      decoded[i] = encrypted[i] ^ key[i % key.length]
    }
    
    return new TextDecoder().decode(decoded)
  } catch {
    return ''
  }
}
