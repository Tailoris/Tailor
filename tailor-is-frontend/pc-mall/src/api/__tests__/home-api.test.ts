import { describe, it, expect } from 'vitest'
import axios from 'axios'

const apiBase = 'http://localhost'

describe('Home Page API Integration Tests', () => {
  it('should fetch categories successfully', async () => {
    const response = await axios.get(`${apiBase}/api/v1/products/categories`)
    expect(response.status).toBe(200)
    expect(response.data.code).toBe(200)
    expect(Array.isArray(response.data.data)).toBe(true)
    if (response.data.data.length > 0) {
      expect(response.data.data[0]).toHaveProperty('id')
      expect(response.data.data[0]).toHaveProperty('name')
    }
  })

  it('should fetch hot products successfully', async () => {
    const response = await axios.get(`${apiBase}/api/v1/products/hot`)
    expect(response.status).toBe(200)
    expect(response.data.code).toBe(200)
    expect(Array.isArray(response.data.data)).toBe(true)
    if (response.data.data.length > 0) {
      expect(response.data.data[0]).toHaveProperty('id')
      expect(response.data.data[0]).toHaveProperty('name')
    }
  })

  it('should fetch new products successfully', async () => {
    const response = await axios.get(`${apiBase}/api/v1/products/new`)
    expect(response.status).toBe(200)
    expect(response.data.code).toBe(200)
    expect(Array.isArray(response.data.data)).toBe(true)
    if (response.data.data.length > 0) {
      expect(response.data.data[0]).toHaveProperty('id')
      expect(response.data.data[0]).toHaveProperty('name')
    }
  })

  it('should fetch seckill products successfully', async () => {
    const response = await axios.get(`${apiBase}/api/v1/products/seckill`)
    expect(response.status).toBe(200)
    expect(response.data.code).toBe(200)
    expect(Array.isArray(response.data.data)).toBe(true)
  })

  it('should fetch community posts successfully', async () => {
    const response = await axios.get(`${apiBase}/api/v1/community/posts`)
    expect(response.status).toBe(200)
    expect(response.data.code).toBe(200)
    const data = response.data.data
    expect(data).toBeDefined()
    if (data && Array.isArray(data.list)) {
      expect(data.list.length).toBeGreaterThanOrEqual(0)
      if (data.list.length > 0) {
        expect(data.list[0]).toHaveProperty('id')
        expect(data.list[0]).toHaveProperty('title')
      }
    }
  })
})