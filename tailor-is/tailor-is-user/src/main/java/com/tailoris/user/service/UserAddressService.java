package com.tailoris.user.service;

import com.tailoris.user.dto.AddressRequest;
import com.tailoris.user.entity.UserAddress;

import java.util.List;

/**
 * 收货地址服务接口.
 *
 * <p>提供用户收货地址的增删改查和默认地址管理。</p>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
public interface UserAddressService {

    /**
     * 查询用户所有收货地址.
     *
     * @param userId 用户ID
     * @return 地址列表
     */
    List<UserAddress> listByUserId(Long userId);

    /**
     * 根据地址ID查询地址.
     *
     * @param addressId 地址ID
     * @return 地址信息
     */
    UserAddress getById(Long addressId);

    /**
     * 新增收货地址.
     *
     * @param userId 用户ID
     * @param request 地址信息
     * @return 新地址ID
     */
    Long create(Long userId, AddressRequest request);

    /**
     * 更新收货地址.
     *
     * @param userId 用户ID
     * @param addressId 地址ID
     * @param request 地址信息
     */
    void update(Long userId, Long addressId, AddressRequest request);

    /**
     * 删除收货地址.
     *
     * @param userId 用户ID
     * @param addressId 地址ID
     */
    void delete(Long userId, Long addressId);

    /**
     * 设置默认收货地址.
     *
     * @param userId 用户ID
     * @param addressId 地址ID
     */
    void setDefault(Long userId, Long addressId);

    /**
     * 获取默认收货地址.
     *
     * @param userId 用户ID
     * @return 默认地址信息，未设置时返回null
     */
    UserAddress getDefaultAddress(Long userId);
}
