import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '../request'
import { getAddresses, createAddress, updateAddress, deleteAddress, setDefaultAddress } from '../address'

vi.mock('../request', () => ({
  default: vi.fn()
}))

describe('Address API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getAddresses', () => {
    it('should fetch addresses successfully', async () => {
      const mockAddresses = [
        { id: 1, name: '张三', phone: '13800138001', province: '广东省', city: '广州市', district: '天河区', detail: '天河路100号', isDefault: 1 },
        { id: 2, name: '李四', phone: '13900139001', province: '北京市', city: '北京市', district: '朝阳区', detail: '朝阳路200号', isDefault: 0 }
      ]
      ;(request as vi.Mock).mockResolvedValue(mockAddresses)

      const result = await getAddresses()

      expect(request).toHaveBeenCalledWith({
        url: '/user/address',
        method: 'get'
      })
      expect(result).toEqual(mockAddresses)
    })

    it('should handle fetch failure', async () => {
      ;(request as vi.Mock).mockRejectedValue(new Error('获取地址列表失败'))

      await expect(getAddresses()).rejects.toThrow('获取地址列表失败')
    })

    it('should return empty array when no addresses', async () => {
      ;(request as vi.Mock).mockResolvedValue([])

      const result = await getAddresses()

      expect(result).toEqual([])
    })
  })

  describe('createAddress', () => {
    const createData = {
      name: '王五',
      phone: '13700137001',
      province: '上海市',
      city: '上海市',
      district: '浦东新区',
      detail: '浦东大道500号',
      isDefault: 0
    }

    it('should create address successfully', async () => {
      ;(request as vi.Mock).mockResolvedValue(3)

      const result = await createAddress(createData)

      expect(request).toHaveBeenCalledWith({
        url: '/user/address',
        method: 'post',
        data: createData
      })
      expect(result).toBe(3)
    })

    it('should handle create failure - missing required fields', async () => {
      ;(request as vi.Mock).mockRejectedValue(new Error('请填写完整的地址信息'))

      await expect(createAddress({ name: '', phone: '', province: '', city: '', district: '', detail: '' })).rejects.toThrow('请填写完整的地址信息')
    })

    it('should handle create failure - invalid phone format', async () => {
      ;(request as vi.Mock).mockRejectedValue(new Error('手机号格式不正确'))

      await expect(createAddress({ ...createData, phone: 'invalid-phone' })).rejects.toThrow('手机号格式不正确')
    })

    it('should handle create failure - network error', async () => {
      ;(request as vi.Mock).mockRejectedValue(new Error('网络异常，请稍后重试'))

      await expect(createAddress(createData)).rejects.toThrow('网络异常，请稍后重试')
    })

    it('should create with default address flag', async () => {
      const dataWithDefault = { ...createData, isDefault: 1 }
      ;(request as vi.Mock).mockResolvedValue(4)

      const result = await createAddress(dataWithDefault)

      expect(request).toHaveBeenCalledWith({
        url: '/user/address',
        method: 'post',
        data: dataWithDefault
      })
      expect(result).toBe(4)
    })
  })

  describe('updateAddress', () => {
    const updateData = {
      name: '赵六',
      phone: '13600136001',
      detail: '更新后的地址'
    }

    it('should update address successfully', async () => {
      ;(request as vi.Mock).mockResolvedValue(true)

      const result = await updateAddress(1, updateData)

      expect(request).toHaveBeenCalledWith({
        url: '/user/address/1',
        method: 'put',
        data: updateData
      })
      expect(result).toBe(true)
    })

    it('should handle update failure - address not found', async () => {
      ;(request as vi.Mock).mockRejectedValue(new Error('地址不存在'))

      await expect(updateAddress(999, updateData)).rejects.toThrow('地址不存在')
    })

    it('should handle update failure - permission denied', async () => {
      ;(request as vi.Mock).mockRejectedValue(new Error('无权修改此地址'))

      await expect(updateAddress(1, updateData)).rejects.toThrow('无权修改此地址')
    })

    it('should allow partial updates', async () => {
      const partialData = { name: '孙七' }
      ;(request as vi.Mock).mockResolvedValue(true)

      const result = await updateAddress(1, partialData)

      expect(request).toHaveBeenCalledWith({
        url: '/user/address/1',
        method: 'put',
        data: partialData
      })
      expect(result).toBe(true)
    })
  })

  describe('deleteAddress', () => {
    it('should delete address successfully', async () => {
      ;(request as vi.Mock).mockResolvedValue(true)

      const result = await deleteAddress(1)

      expect(request).toHaveBeenCalledWith({
        url: '/user/address/1',
        method: 'delete'
      })
      expect(result).toBe(true)
    })

    it('should handle delete failure - address not found', async () => {
      ;(request as vi.Mock).mockRejectedValue(new Error('地址不存在'))

      await expect(deleteAddress(999)).rejects.toThrow('地址不存在')
    })

    it('should handle delete failure - cannot delete default address', async () => {
      ;(request as vi.Mock).mockRejectedValue(new Error('不能删除默认地址'))

      await expect(deleteAddress(1)).rejects.toThrow('不能删除默认地址')
    })

    it('should handle delete failure - permission denied', async () => {
      ;(request as vi.Mock).mockRejectedValue(new Error('无权删除此地址'))

      await expect(deleteAddress(1)).rejects.toThrow('无权删除此地址')
    })
  })

  describe('setDefaultAddress', () => {
    it('should set default address successfully', async () => {
      ;(request as vi.Mock).mockResolvedValue(true)

      const result = await setDefaultAddress(2)

      expect(request).toHaveBeenCalledWith({
        url: '/user/address/2/default',
        method: 'put'
      })
      expect(result).toBe(true)
    })

    it('should handle set default failure - address not found', async () => {
      ;(request as vi.Mock).mockRejectedValue(new Error('地址不存在'))

      await expect(setDefaultAddress(999)).rejects.toThrow('地址不存在')
    })

    it('should handle set default failure - same as current default', async () => {
      ;(request as vi.Mock).mockRejectedValue(new Error('已是默认地址'))

      await expect(setDefaultAddress(1)).rejects.toThrow('已是默认地址')
    })

    it('should handle set default failure - permission denied', async () => {
      ;(request as vi.Mock).mockRejectedValue(new Error('无权设置默认地址'))

      await expect(setDefaultAddress(2)).rejects.toThrow('无权设置默认地址')
    })
  })
})