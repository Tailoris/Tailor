package com.tailoris.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tailoris.merchant.entity.MerchantCurrentShop;
import com.tailoris.merchant.entity.MerchantEmployee;
import com.tailoris.merchant.entity.MerchantShop;
import com.tailoris.merchant.exception.MerchantBusinessException;
import com.tailoris.merchant.mapper.MerchantCurrentShopMapper;
import com.tailoris.merchant.mapper.MerchantEmployeeMapper;
import com.tailoris.merchant.mapper.MerchantShopMapper;
import com.tailoris.merchant.service.IMerchantCurrentShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商家多店铺切换服务实现 - MER-003.
 *
 * <h3>切换流程</h3>
 * <ol>
 *   <li>校验用户是否属于该商家（员工关系）</li>
 *   <li>校验目标店铺属于该商家</li>
 *   <li>检查用户是否被授权访问该店铺</li>
 *   <li>更新/插入用户当前店铺记录</li>
 * </ol>
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantCurrentShopServiceImpl
        extends ServiceImpl<MerchantCurrentShopMapper, MerchantCurrentShop>
        implements IMerchantCurrentShopService {

    private final MerchantEmployeeMapper employeeMapper;
    private final MerchantShopMapper shopMapper;

    @Override
    public Long getCurrentShopId(Long userId, Long merchantId) {
        MerchantCurrentShop current = baseMapper.selectByUserAndMerchant(userId, merchantId);
        return current == null ? null : current.getCurrentShopId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean switchTo(Long userId, Long merchantId, Long targetShopId) {
        // 1. 校验用户属于该商家
        LambdaQueryWrapper<MerchantEmployee> empCheck = new LambdaQueryWrapper<>();
        empCheck.eq(MerchantEmployee::getUserId, userId)
                .eq(MerchantEmployee::getMerchantId, merchantId)
                .eq(MerchantEmployee::getStatus, 1)
                .last("LIMIT 1");
        MerchantEmployee employee = employeeMapper.selectOne(empCheck);
        if (employee == null) {
            throw new MerchantBusinessException("用户不属于该商家或已禁用");
        }

        // 2. 校验店铺
        MerchantShop shop = shopMapper.selectById(targetShopId);
        if (shop == null) {
            throw new MerchantBusinessException("店铺不存在");
        }
        if (!merchantId.equals(shop.getMerchantId())) {
            throw new MerchantBusinessException("店铺不属于该商家");
        }

        // 3. 校验员工有该店铺权限（如果设了限定）
        if (employee.getShopIds() != null && !employee.getShopIds().isEmpty()) {
            List<Long> allowed = parseShopIds(employee.getShopIds());
            if (!allowed.contains(targetShopId)) {
                throw new MerchantBusinessException("无该店铺访问权限");
            }
        }

        // 4. 更新当前店铺
        MerchantCurrentShop current = baseMapper.selectByUserAndMerchant(userId, merchantId);
        if (current == null) {
            current = new MerchantCurrentShop();
            current.setUserId(userId);
            current.setMerchantId(merchantId);
            current.setCurrentShopId(targetShopId);
            current.setLastSwitchTime(LocalDateTime.now());
            save(current);
        } else {
            current.setCurrentShopId(targetShopId);
            current.setLastSwitchTime(LocalDateTime.now());
            updateById(current);
        }

        log.info("店铺切换成功: userId={}, merchantId={}, shopId={}", userId, merchantId, targetShopId);
        return true;
    }

    @Override
    public List<MerchantShop> listUserShops(Long userId, Long merchantId) {
        // 获取员工记录
        LambdaQueryWrapper<MerchantEmployee> empQuery = new LambdaQueryWrapper<>();
        empQuery.eq(MerchantEmployee::getUserId, userId)
                .eq(MerchantEmployee::getMerchantId, merchantId)
                .eq(MerchantEmployee::getStatus, 1)
                .last("LIMIT 1");
        MerchantEmployee employee = employeeMapper.selectOne(empQuery);
        if (employee == null) {
            return Collections.emptyList();
        }

        // 平台管理员/店长可见全部门店
        boolean isAll = "shop_manager".equals(employee.getRoleCode())
                || "merchant_owner".equals(employee.getRoleCode());
        if (isAll || employee.getShopIds() == null || employee.getShopIds().isEmpty()) {
            LambdaQueryWrapper<MerchantShop> q = new LambdaQueryWrapper<>();
            q.eq(MerchantShop::getMerchantId, merchantId)
             .eq(MerchantShop::getShopStatus, 1)
             .orderByDesc(MerchantShop::getId);
            return shopMapper.selectList(q);
        }

        // 仅可见限定店铺
        List<Long> allowedIds = parseShopIds(employee.getShopIds());
        if (allowedIds.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<MerchantShop> q = new LambdaQueryWrapper<>();
        q.in(MerchantShop::getId, allowedIds)
         .eq(MerchantShop::getShopStatus, 1)
         .orderByDesc(MerchantShop::getId);
        return shopMapper.selectList(q);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean clear(Long userId, Long merchantId) {
        MerchantCurrentShop current = baseMapper.selectByUserAndMerchant(userId, merchantId);
        if (current == null) {
            return true;
        }
        return removeById(current.getId());
    }

    // ============================================================
    // 私有方法
    // ============================================================

    private List<Long> parseShopIds(String shopIdsStr) {
        if (shopIdsStr == null || shopIdsStr.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return java.util.Arrays.stream(shopIdsStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("店铺ID列表解析失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
