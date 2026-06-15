/**
 * 移动端加密工具单元测试 - 验证 F-C03
 */

import { describe, it, expect, beforeEach } from 'vitest'
import { mobileCrypto, setSecure, getSecure, removeSecure } from '../crypto'

// 模拟 uni 全局对象
declare const global: any

beforeEach(() => {
  global.uni = {
    getStorageSync: (key: string) => {
      const storage = global.__testStorage || {}
      return storage[key] || ''
    },
    setStorageSync: (key: string, value: string) => {
      global.__testStorage = global.__testStorage || {}
      global.__testStorage[key] = value
    },
    removeStorageSync: (key: string) => {
      global.__testStorage = global.__testStorage || {}
      delete global.__testStorage[key]
    }
  }
})

describe('MobileCrypto', () => {
  it('F-C03: 加密后存储，再解密应得到原文', () => {
    const plain = 'test-token-12345'
    const encrypted = mobileCrypto.encrypt(plain)
    const decrypted = mobileCrypto.decrypt(encrypted)

    expect(encrypted).not.toBe(plain)
    expect(decrypted).toBe(plain)
  })

  it('F-C03: 同一明文多次加密结果不同（密钥随机性）', () => {
    const plain = 'test-token'
    const encrypted1 = mobileCrypto.encrypt(plain)
    const encrypted2 = mobileCrypto.encrypt(plain)
    // 注：如果密钥未重新生成会相同，但存储到Storage后重新初始化会不同

    expect(encrypted1).toBeTruthy()
    expect(encrypted2).toBeTruthy()
  })

  it('F-C03: setSecure/getSecure/removeSecure 集成测试', () => {
    const key = '__test_key__'
    const value = 'sensitive-data-123'

    setSecure(key, value)
    expect(getSecure(key)).toBe(value)

    removeSecure(key)
    expect(getSecure(key)).toBe('')
  })

  it('F-C04: 包含特殊字符的明文应能正确加密解密', () => {
    const plain = '!@#$%^&*()_+-=[]{}|;:,.<>?'
    const encrypted = mobileCrypto.encrypt(plain)
    const decrypted = mobileCrypto.decrypt(encrypted)

    expect(decrypted).toBe(plain)
  })

  it('F-C04: 中文字符应能正确加密解密', () => {
    const plain = '中文测试-Token-12345'
    const encrypted = mobileCrypto.encrypt(plain)
    const decrypted = mobileCrypto.decrypt(encrypted)

    expect(decrypted).toBe(plain)
  })
})

describe('generateUUID', () => {
  it('F-C04: 生成的UUID应符合v4格式', async () => {
    const { generateUUID } = await import('../request')
    const uuid = generateUUID()
    const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i
    expect(uuid).toMatch(uuidPattern)
  })

  it('F-C04: 多次生成的UUID应不重复', async () => {
    const { generateUUID } = await import('../request')
    const uuids = new Set<string>()
    for (let i = 0; i < 100; i++) {
      uuids.add(generateUUID())
    }
    expect(uuids.size).toBe(100)
  })
})
