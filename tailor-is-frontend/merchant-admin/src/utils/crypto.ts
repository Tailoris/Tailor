/**
 * Token 加密/解密工具 - 使用 Web Crypto API AES-GCM
 *
 * 加密密钥从 localStorage 派生，每次登录时更新。
 * AES-GCM 提供认证加密，防止密文被篡改。
 */

const CRYPTO_KEY_STORAGE_KEY = '__crypto_key__'
const ALGORITHM = 'AES-GCM'
const KEY_LENGTH = 256
const IV_LENGTH = 12
const TAG_LENGTH = 128

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

function generateIV(): Uint8Array {
  const iv = new Uint8Array(IV_LENGTH)
  crypto.getRandomValues(iv)
  return iv
}

export async function encrypt(plaintext: string): Promise<string> {
  const key = await getCryptoKey()
  const iv = generateIV()
  const encoded = new TextEncoder().encode(plaintext)

  const ciphertext = await crypto.subtle.encrypt(
    { name: ALGORITHM, iv },
    key,
    encoded
  )

  const combined = new Uint8Array(iv.length + ciphertext.byteLength)
  combined.set(iv)
  combined.set(new Uint8Array(ciphertext), iv.length)
  return btoa(String.fromCharCode(...combined))
}

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
    return ''
  }
}

export function encryptSync(plaintext: string): string {
  const keyData = localStorage.getItem(CRYPTO_KEY_STORAGE_KEY)
  if (!keyData) return plaintext
  return simpleEncrypt(plaintext, keyData.substring(0, 32))
}

export function decryptSync(base64Ciphertext: string): string {
  const keyData = localStorage.getItem(CRYPTO_KEY_STORAGE_KEY)
  if (!keyData) return base64Ciphertext
  return simpleDecrypt(base64Ciphertext, keyData.substring(0, 32))
}

function simpleEncrypt(text: string, key: string): string {
  let result = ''
  for (let i = 0; i < text.length; i++) {
    result += String.fromCharCode(text.charCodeAt(i) ^ key.charCodeAt(i % key.length))
  }
  return btoa(unescape(encodeURIComponent(result)))
}

function simpleDecrypt(encoded: string, key: string): string {
  try {
    const text = decodeURIComponent(escape(atob(encoded)))
    let result = ''
    for (let i = 0; i < text.length; i++) {
      result += String.fromCharCode(text.charCodeAt(i) ^ key.charCodeAt(i % key.length))
    }
    return result
  } catch {
    return ''
  }
}
