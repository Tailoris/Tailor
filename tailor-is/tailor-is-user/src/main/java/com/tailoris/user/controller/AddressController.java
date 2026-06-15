package com.tailoris.user.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.tailoris.common.result.Result;
import com.tailoris.user.dto.AddressRequest;
import com.tailoris.user.entity.UserAddress;
import com.tailoris.user.service.UserAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 收货地址管理Controller.
 *
 * <p>提供收货地址的增删改查、默认地址设置等接口。</p>
 *
 * <p>关键修复：</p>
 * <ul>
 *   <li>B-M13: 移除无用导入 BusinessException</li>
 *   <li>B-M11: 补充方法级别Javadoc</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@SaCheckLogin
@Tag(name = "地址管理", description = "收货地址增删改查接口")
@RestController
@RequestMapping("/api/user/address")
@RequiredArgsConstructor
public class AddressController {

    private final UserAddressService userAddressService;

    /**
     * 获取当前用户的所有收货地址.
     *
     * @return 地址列表
     */
    @Operation(summary = "获取地址列表", description = "获取当前用户的所有收货地址")
    @GetMapping
    public Result<List<UserAddress>> listAddresses() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<UserAddress> addresses = userAddressService.listByUserId(userId);
        return Result.success(addresses);
    }

    /**
     * 获取当前用户的默认收货地址.
     *
     * @return 默认地址
     */
    @Operation(summary = "获取默认地址", description = "获取当前用户的默认收货地址")
    @GetMapping("/default")
    public Result<UserAddress> getDefaultAddress() {
        Long userId = StpUtil.getLoginIdAsLong();
        UserAddress address = userAddressService.getDefaultAddress(userId);
        return Result.success(address);
    }

    /**
     * 新增收货地址.
     *
     * @param request 地址信息
     * @return 新地址ID
     */
    @Operation(summary = "新增地址", description = "添加新的收货地址")
    @PostMapping
    public Result<Long> createAddress(@Valid @RequestBody AddressRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long addressId = userAddressService.create(userId, request);
        return Result.success(addressId);
    }

    /**
     * 更新收货地址.
     *
     * @param addressId 地址ID
     * @param request 地址信息
     */
    @Operation(summary = "更新地址", description = "修改指定收货地址信息")
    @PutMapping("/{id}")
    public Result<Void> updateAddress(@PathVariable("id") Long addressId,
                                      @Valid @RequestBody AddressRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        userAddressService.update(userId, addressId, request);
        return Result.success();
    }

    /**
     * 删除收货地址.
     *
     * @param addressId 地址ID
     */
    @Operation(summary = "删除地址", description = "删除指定收货地址")
    @DeleteMapping("/{id}")
    public Result<Void> deleteAddress(@PathVariable("id") Long addressId) {
        Long userId = StpUtil.getLoginIdAsLong();
        userAddressService.delete(userId, addressId);
        return Result.success();
    }

    /**
     * 设为默认地址.
     *
     * @param addressId 地址ID
     */
    @Operation(summary = "设为默认地址", description = "将指定地址设为默认收货地址")
    @PutMapping("/{id}/default")
    public Result<Void> setDefaultAddress(@PathVariable("id") Long addressId) {
        Long userId = StpUtil.getLoginIdAsLong();
        userAddressService.setDefault(userId, addressId);
        return Result.success();
    }
}
