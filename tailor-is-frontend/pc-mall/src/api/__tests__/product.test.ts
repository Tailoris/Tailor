import { describe, it, expect, vi } from 'vitest'
import axios from 'axios'

vi.mock('axios')

describe('product API tests', () => {
  it('should mock axios successfully', () => {
    expect(axios.create).toBeDefined()
    expect(typeof axios.create).toBe('function')
  })

  it('should have axios mock setup correctly', () => {
    const mockInstance = {
      interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
      request: vi.fn().mockResolvedValue({ data: { code: 200, message: 'success', data: {} } })
    }
    ;(axios.create as vi.Mock).mockReturnValue(mockInstance)
    
    const instance = axios.create()
    expect(instance.interceptors).toBeDefined()
    expect(typeof instance.request).toBe('function')
  })
})