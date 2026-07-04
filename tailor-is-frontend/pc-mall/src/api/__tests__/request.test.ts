import { describe, it, expect, vi } from 'vitest'
import request from '../request'
import axios from 'axios'

vi.mock('axios')

describe('request function', () => {
  it('should extract data from successful response', async () => {
    const mockData = { id: 1, name: 'Test' }
    ;(axios.create as vi.Mock).mockReturnValue({
      interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
      request: vi.fn().mockResolvedValue({
        data: { code: 200, message: 'success', data: mockData }
      })
    })

    const result = await request({ url: '/test', method: 'get' })
    expect(result).toEqual(mockData)
  })

  it('should return data when code is 0', async () => {
    const mockData = { items: [] }
    ;(axios.create as vi.Mock).mockReturnValue({
      interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
      request: vi.fn().mockResolvedValue({
        data: { code: 0, message: 'success', data: mockData }
      })
    })

    const result = await request({ url: '/test', method: 'get' })
    expect(result).toEqual(mockData)
  })

  it('should handle null data gracefully', async () => {
    ;(axios.create as vi.Mock).mockReturnValue({
      interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
      request: vi.fn().mockResolvedValue({
        data: { code: 200, message: 'success', data: null }
      })
    })

    const result = await request({ url: '/test', method: 'get' })
    expect(result).toBeNull()
  })
})